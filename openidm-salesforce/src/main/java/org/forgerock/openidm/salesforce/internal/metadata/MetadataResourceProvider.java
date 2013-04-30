package org.forgerock.openidm.salesforce.internal.metadata;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.util.ResourceUtil;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sforce.soap.metadata.AsyncRequestState;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.RetrieveMessage;
import com.sforce.soap.metadata.RetrieveRequest;
import com.sforce.soap.metadata.RetrieveResult;
import com.sforce.ws.ConnectionException;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class MetadataResourceProvider implements CollectionResourceProvider {

    /**
     * Setup logging for the {@link MetadataResourceProvider}.
     */
    protected final static Logger logger = LoggerFactory.getLogger(MetadataResourceProvider.class);

    // one second in milliseconds
    private static final long ONE_SECOND = 1000;
    // maximum number of attempts to retrieve the results
    private static final int MAX_NUM_POLL_REQUESTS = 50;

    protected final static ObjectMapper mapper = new ObjectMapper();

    private final SalesforceConnection sfconnection;

    private final MetadataConnection connection;

    protected MetadataResourceProvider(final SalesforceConnection connection) {
        this.sfconnection = connection;
        try {
            this.connection = new MetadataConnection(sfconnection.getConnectorConfig("Soap/m"));
            this.connection.getConfig().setTraceMessage(logger.isTraceEnabled());
            try {
                if (this.connection.getConfig().isTraceMessage()) {
                    this.connection.getConfig().setTraceFile(
                            IdentityServer.getFileForWorkingPath("logs/SF-meta.log")
                                    .getAbsolutePath());
                }
            } catch (FileNotFoundException e) {
                this.connection.getConfig().setTraceMessage(false);
            }
        } catch (ConnectionException e) {
            logger.error("", e);
            throw new ComponentException("Failed to initiate the Async service", e);
        }
    }

    protected MetadataConnection getConnection(boolean refresh) throws ResourceException {
        if (refresh) {
            if (!sfconnection.refreshAccessToken(connection.getConfig())) {
                throw new ServiceUnavailableException("Session is expired and can not be renewed");
            }
        }
        return connection;
    }

    @Override
    public void createInstance(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    @Override
    public void updateInstance(final ServerContext context, final String resourceId,
            final UpdateRequest request, final ResultHandler<Resource> handler) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    @Override
    public void deleteInstance(final ServerContext context, final String resourceId,
            final DeleteRequest request, final ResultHandler<Resource> handler) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    @Override
    public void readInstance(final ServerContext context, final String resourceId,
            final ReadRequest request, ResultHandler<Resource> handler) {
        try {

            RetrieveRequest retrieveRequest = new RetrieveRequest();
            retrieveRequest.setApiVersion(28.0);
            retrieveRequest.setPackageNames(new String[] { "SamlSsoConfig" });

            AsyncResult asyncResult = getConnection(true).retrieve(retrieveRequest);
            // Wait for the retrieve to complete
            int poll = 0;
            long waitTimeMilliSecs = ONE_SECOND;
            while (!asyncResult.isDone()) {
                Thread.sleep(waitTimeMilliSecs);
                // double the wait time for the next iteration
                waitTimeMilliSecs *= 2;
                if (poll++ > MAX_NUM_POLL_REQUESTS) {
                    throw new Exception("Request timed out.  If this is a large set "
                            + "of metadata components, check that the time allowed "
                            + "by MAX_NUM_POLL_REQUESTS is sufficient.");
                }
                asyncResult =
                        getConnection(true).checkStatus(new String[] { asyncResult.getId() })[0];
                logger.trace("Status is: {}", asyncResult.getState());
            }

            if (asyncResult.getState() != AsyncRequestState.Completed) {
                throw new Exception(asyncResult.getStatusCode() + " msg: "
                        + asyncResult.getMessage());
            }

            RetrieveResult result = getConnection(true).checkRetrieveStatus(asyncResult.getId());

            // Print out any warning messages
            StringBuilder buf = new StringBuilder();
            if (result.getMessages() != null) {
                for (RetrieveMessage rm : result.getMessages()) {
                    buf.append(rm.getFileName() + " - " + rm.getProblem());
                }
            }
            if (buf.length() > 0) {
                logger.warn("Retrieve warnings:\n {}", buf);
            }

            // Write the zip to the file system
            logger.trace("Writing results to zip file");
            File resultsFile =
                    IdentityServer.getFileForWorkingPath("salesforce/retrieveResults.zip");

            FileUtils.writeByteArrayToFile(resultsFile, result.getZipFile());
            logger.trace("Results written to " + resultsFile.getAbsolutePath());

            handler.handleResult(new Resource(resourceId, null, new JsonValue(null)));

        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void actionInstance(final ServerContext context, final String resourceId,
            final ActionRequest request, final ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instance");
        handler.handleError(e);
    }

    @Override
    public void actionCollection(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource collection");
        handler.handleError(e);
    }

    @Override
    public void patchInstance(final ServerContext context, final String resourceId,
            final PatchRequest request, final ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void queryCollection(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        final ResourceException e = new NotSupportedException("Query operations are not supported");
        handler.handleError(e);
    }
}
