package org.forgerock.openidm.salesforce.internal.metadata;

import static org.forgerock.openidm.util.ResourceUtil.getUriTemplateVariables;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.util.JsonUtil;
import org.forgerock.openidm.util.ResourceUtil;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sforce.soap.metadata.AsyncRequestState;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.Package;
import com.sforce.soap.metadata.PackageTypeMembers;
import com.sforce.soap.metadata.RetrieveRequest;
import com.sforce.soap.metadata.RetrieveResult;
import com.sforce.soap.metadata.SamlSsoConfig;
import com.sforce.soap.metadata.SessionHeader_element;
import com.sforce.soap.metadata.UpdateMetadata;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import com.sforce.ws.SessionRenewer;
import com.sforce.ws.bind.TypeMapper;
import com.sforce.ws.parser.PullParserException;
import com.sforce.ws.parser.XmlInputStream;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class MetadataResourceProvider implements CollectionResourceProvider {

    /**
     * Setup logging for the {@link MetadataResourceProvider}.
     */
    private static final Logger logger = LoggerFactory.getLogger(MetadataResourceProvider.class);

    private static final ObjectMapper OBJECT_MAPPER = JsonUtil.build();

    static {
        OBJECT_MAPPER.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
    }

    // one second in milliseconds
    private static final long ONE_SECOND = 1000;
    // maximum number of attempts to retrieve the results
    private static final int MAX_NUM_POLL_REQUESTS = 50;
    private static final TypeMapper TYPE_MAPPER = new TypeMapper();

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
            this.connection.getConfig().setSessionRenewer(new SessionRenewer() {
                @Override
                public SessionRenewalHeader renewSession(final ConnectorConfig config)
                        throws ConnectionException {
                    try {
                        if (!sfconnection.refreshAccessToken(config)) {
                            final ServiceUnavailableException cause =
                                    new ServiceUnavailableException(
                                            "Session is expired and can not be renewed");
                            throw new ConnectionException(cause.getMessage(), cause);
                        }
                    } catch (final ResourceException e) {
                        throw new ConnectionException(e.getMessage(), e);
                    }

                    SessionRenewalHeader header = new SessionRenewalHeader();
                    header.name =
                            new javax.xml.namespace.QName(
                                    "http://soap.sforce.com/2006/04/metadata", "SessionHeader");
                    header.headerElement = new SessionHeader_element();
                    ((SessionHeader_element) header.headerElement).setSessionId(config
                            .getSessionId());
                    return header;
                }
            });
        } catch (ConnectionException e) {
            throw new ComponentException("Failed to initiate the Metadata service", e);
        }
    }

    protected MetadataConnection getConnection() throws ResourceException {
        return connection;
    }

    @Override
    public void createInstance(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        try {
            if (StringUtils.isBlank(request.getNewResourceId())) {
                Class<? extends Metadata> metaClass = getMetadataClass(context);
                Metadata metadata =
                        OBJECT_MAPPER.convertValue(request.getContent().asMap(), metaClass);

                AsyncResult[] results = getConnection().create(new Metadata[] { metadata });
                getAsyncResult(results[0]);

                Resource metadataResource = getMetadataResource(metadata.getFullName(), metaClass);
                if (null != metadataResource) {
                    handler.handleResult(metadataResource);
                } else {
                    handler.handleResult(new Resource(metadata.getFullName(), null, null));
                }
            } else {
                handler.handleError(new NotSupportedException(
                        "Create metadata with client provided ID is not supported"));
            }
        } catch (final Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void updateInstance(final ServerContext context, final String resourceId,
            final UpdateRequest request, final ResultHandler<Resource> handler) {
        try {
            Class<? extends Metadata> metaClass = getMetadataClass(context);
            Metadata metadata =
                    OBJECT_MAPPER.convertValue(request.getNewContent().asMap(), metaClass);

            UpdateMetadata updateMetadata = new UpdateMetadata();
            updateMetadata.setCurrentName(resourceId);
            updateMetadata.setMetadata(metadata);

            AsyncResult[] results = getConnection().update(new UpdateMetadata[] { updateMetadata });
            getAsyncResult(results[0]);

            Resource metadataResource = getMetadataResource(metadata.getFullName(), metaClass);
            if (null != metadataResource) {
                handler.handleResult(metadataResource);
            } else {
                handler.handleResult(new Resource(metadata.getFullName(), null, null));
            }
        } catch (final Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void readInstance(final ServerContext context, final String resourceId,
            final ReadRequest request, ResultHandler<Resource> handler) {
        try {
            Class<? extends Metadata> metaClass = getMetadataClass(context);

            Resource metadataResource = getMetadataResource(resourceId, metaClass);
            if (null != metadataResource) {
                handler.handleResult(metadataResource);
            } else {
                handler.handleError(new NotFoundException());
            }
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void queryCollection(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        try {
            if (ServerConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                Class<? extends Metadata> metaClass = getMetadataClass(context);
                ListMetadataQuery query = new ListMetadataQuery();
                query.setType(metaClass.getSimpleName());
                query.setFolder("*");

                FileProperties[] results =
                        getConnection().listMetadata(new ListMetadataQuery[] { query }, 28.0);

                for (FileProperties file : results) {
                    handler.handleResource(new Resource(file.getFullName(), "", new JsonValue(
                            OBJECT_MAPPER.convertValue(file, Map.class))));
                }
                handler.handleResult(new QueryResult());
            } else {
                handler.handleError(new BadRequestException("Not supported queryId"));
            }
        } catch (final Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void deleteInstance(final ServerContext context, final String resourceId,
            final DeleteRequest request, final ResultHandler<Resource> handler) {
        try {
            Class<? extends Metadata> metaClass = getMetadataClass(context);
            Metadata metadata = metaClass.newInstance();
            metadata.setFullName(resourceId);

            AsyncResult[] results = getConnection().delete(new Metadata[] { metadata });
            getAsyncResult(results[0]);

            handler.handleResult(new Resource(metadata.getFullName(), null, null));
        } catch (final Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void actionInstance(final ServerContext context, final String resourceId,
            final ActionRequest request, final ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceUtil.notSupportedOnInstance(request));
    }

    @Override
    public void actionCollection(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceUtil.notSupportedOnCollection(request));
    }

    @Override
    public void patchInstance(final ServerContext context, final String resourceId,
            final PatchRequest request, final ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupportedOnInstance(request));
    }

    protected String getPartition(ServerContext context) throws ResourceException {
        Map<String, String> variables = getUriTemplateVariables(context);
        if (null != variables && variables.containsKey("metadataType")) {
            return variables.get("metadataType");
        }
        throw new ForbiddenException("Direct access without Router to this service is forbidden.");
    }

    private Class<? extends Metadata> getMetadataClass(final ServerContext context)
            throws ResourceException {
        Class<? extends Metadata> metaClass;
        if ("samlssoconfig".equalsIgnoreCase(getPartition(context))) {
            metaClass = SamlSsoConfig.class;
        } else {
            throw new NotFoundException();
        }
        return metaClass;
    }

    private AsyncResult getAsyncResult(AsyncResult result) throws ResourceException,
            InterruptedException, ConnectionException {
        // Wait for the retrieve to complete
        AsyncResult asyncResult = result;
        int poll = 0;
        long waitTimeMilliSecs = ONE_SECOND;
        while (!asyncResult.isDone()) {
            Thread.sleep(waitTimeMilliSecs);
            // double the wait time for the next iteration
            waitTimeMilliSecs *= 2;
            if (poll++ > MAX_NUM_POLL_REQUESTS) {
                throw new InternalServerErrorException(
                        "Request timed out.  If this is a large set "
                                + "of metadata components, check that the time allowed "
                                + "by MAX_NUM_POLL_REQUESTS is sufficient.");
            }
            asyncResult = getConnection().checkStatus(new String[] { asyncResult.getId() })[0];
            logger.trace("Status is: {}", asyncResult.getState());
        }

        if (asyncResult.getState() != AsyncRequestState.Completed) {
            final ResourceException resourceException =
                    ResourceException.getException(ResourceException.INTERNAL_ERROR, asyncResult
                            .getStatusCode()
                            + " msg: " + asyncResult.getMessage());
            try {
                resourceException.setDetail(new JsonValue(OBJECT_MAPPER.convertValue(asyncResult,
                        Map.class)));
            } catch (Exception e) {
                /* ignore */
            }
            throw resourceException;
        }
        return asyncResult;
    }

    private Resource getMetadataResource(String resourceId, Class<? extends Metadata> metaClass)
            throws ConnectionException, ResourceException, InterruptedException, IOException {
        PackageTypeMembers packageTypeMembers = new PackageTypeMembers();
        packageTypeMembers.setName(metaClass.getSimpleName());
        packageTypeMembers.setMembers(new String[] { "*" });
        Package p = new Package();
        p.setVersion("28.0");
        p.setTypes(new PackageTypeMembers[] { packageTypeMembers });
        RetrieveRequest retrieveRequest = new RetrieveRequest();
        retrieveRequest.setApiVersion(28.0);
        retrieveRequest.setUnpackaged(p);

        AsyncResult asyncResult = getConnection().retrieve(retrieveRequest);
        asyncResult = getAsyncResult(asyncResult);

        RetrieveResult result = getConnection().checkRetrieveStatus(asyncResult.getId());

        return getMetadataResource(result.getZipFile(), metaClass.getSimpleName(), resourceId);
    }

    static Resource getMetadataResource(byte[] zipFile, String type, String id)
            throws ResourceException, IOException {
        String entryName = null;
        Metadata metadata = null;
        if ("samlssoconfig".equalsIgnoreCase(type)) {
            metadata = new SamlSsoConfig();
            entryName = "unpackaged/samlssoconfigs/" + id + ".samlssoconfig";
        } else {
            throw new NotFoundException("Unknown Metadata type: " + type);
        }

        logger.trace("Load metadata file from {}", entryName);
        // open the zip file stream
        ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(zipFile));
        try {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                logger.trace("Entry in ZIP: {}", entry);
                if (entryName.equals(entry.getName())) {
                    XmlInputStream xin = new XmlInputStream();
                    try {
                        xin.setInput(stream, "utf-8");
                        metadata.load(xin, TYPE_MAPPER);
                        return new Resource(null != metadata.getFullName() ? metadata.getFullName()
                                : id, null, new JsonValue(OBJECT_MAPPER.convertValue(metadata,
                                Map.class)));
                    } catch (PullParserException e) {
                        /* ignore */
                    } catch (ConnectionException e) {
                        logger.error("Failed to load the Metadata from ZIP", e);
                        throw new InternalServerErrorException(e);
                    }
                }
            }
        } finally {
            stream.close();
        }
        return null;
    }
}
