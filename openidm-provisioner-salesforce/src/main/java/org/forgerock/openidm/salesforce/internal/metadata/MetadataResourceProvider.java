package org.forgerock.openidm.salesforce.internal.metadata;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.salesforce.internal.ResultHandler;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.salesforce.internal.ServerContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class MetadataResourceProvider extends SimpleJsonResource {

    /**
     * Setup logging for the {@link MetadataResourceProvider}.
     */
    private static final Logger logger = LoggerFactory.getLogger(MetadataResourceProvider.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.setVisibility(JsonMethod.IS_GETTER, JsonAutoDetect.Visibility.NONE).disable(
                DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    // one second in milliseconds
    private static final long ONE_SECOND = 1000;
    // maximum number of attempts to retrieve the results
    private static final int MAX_NUM_POLL_REQUESTS = 50;
    private static final TypeMapper TYPE_MAPPER = new TypeMapper();

    private final SalesforceConnection sfconnection;

    private final MetadataConnection connection;

    public MetadataResourceProvider(final SalesforceConnection connection) {
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
                            final JsonResourceException cause =
                                    new JsonResourceException(JsonResourceException.UNAVAILABLE,
                                            "Session is expired and can not be renewed");
                            throw new ConnectionException(cause.getMessage(), cause);
                        }
                    } catch (final JsonResourceException e) {
                        throw new ConnectionException(e.getMessage(), e);
                    }

                    SessionRenewalHeader header = new SessionRenewalHeader();
                    header.name =
                            new javax.xml.namespace.QName(
                                    "http://soap.sforce.com/2006/04/metadata", "SessionHeader");
                    header.headerElement = new SessionHeader_element();
                    ((SessionHeader_element) header.headerElement).setSessionId(config
                            .getSessionId());
                    MetadataResourceProvider.this.connection.getSessionHeader().setSessionId(
                            config.getSessionId());
                    return header;
                }
            });
        } catch (ConnectionException e) {
            throw new ComponentException("Failed to initiate the Metadata service", e);
        }
    }

    protected MetadataConnection getConnection() throws JsonResourceException {
        return connection;
    }

    public void createInstance(final ServerContext context, final JsonValue request,
            final ResultHandler handler) {
        try {
            if (context.getMatcher().groupCount() > 2) {
                Class<? extends Metadata> metaClass = getMetadataClass(context);
                Metadata metadata = OBJECT_MAPPER.convertValue(request.asMap(), metaClass);

                AsyncResult[] results = getConnection().create(new Metadata[] { metadata });
                getAsyncResult(results[0]);

                JsonValue metadataResource = getMetadataResource(metadata.getFullName(), metaClass);
                if (null != metadataResource) {
                    handler.handleResult(metadataResource);
                } else {
                    metadataResource = new JsonValue(new HashMap<String, ObjectMapper>());
                    metadataResource.put("_id", metadata.getFullName());
                    handler.handleResult(metadataResource);
                }
            } else {
                handler.handleError(new JsonResourceException(501,
                        "Create metadata with client provided ID is not supported"));
            }
        } catch (final Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    public void updateInstance(final ServerContext context, final String resourceId,
            final JsonValue request, final ResultHandler handler) {
        try {
            Class<? extends Metadata> metaClass = getMetadataClass(context);
            Metadata metadata = OBJECT_MAPPER.convertValue(request.asMap(), metaClass);

            UpdateMetadata updateMetadata = new UpdateMetadata();
            updateMetadata.setCurrentName(resourceId);
            updateMetadata.setMetadata(metadata);

            AsyncResult[] results = getConnection().update(new UpdateMetadata[] { updateMetadata });
            getAsyncResult(results[0]);

            JsonValue metadataResource = getMetadataResource(metadata.getFullName(), metaClass);
            if (null != metadataResource) {
                handler.handleResult(metadataResource);
            } else {
                metadataResource = new JsonValue(new HashMap<String, ObjectMapper>());
                metadataResource.put("_id", metadata.getFullName());
                handler.handleResult(metadataResource);
            }
        } catch (final Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    public void readInstance(final ServerContext context, final String resourceId,
            ResultHandler handler) {
        try {
            Class<? extends Metadata> metaClass = getMetadataClass(context);

            JsonValue metadataResource = getMetadataResource(resourceId, metaClass);
            if (null != metadataResource) {
                handler.handleResult(metadataResource);
            } else {
                handler.handleError(new JsonResourceException(JsonResourceException.NOT_FOUND));
            }
        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    public void queryCollection(final ServerContext context, final String queryId,
            final ResultHandler handler) {
        try {
            if ("query-all-ids".equals(queryId)) {
                Class<? extends Metadata> metaClass = getMetadataClass(context);
                ListMetadataQuery query = new ListMetadataQuery();
                query.setType(metaClass.getSimpleName());
                query.setFolder("*");

                FileProperties[] results =
                        getConnection().listMetadata(new ListMetadataQuery[] { query }, 28.0);

                for (FileProperties file : results) {
                    JsonValue metadataResource =
                            new JsonValue(OBJECT_MAPPER.convertValue(file, Map.class));
                    metadataResource.put("_id", file.getFullName());
                    handler.handleResource(metadataResource);
                }
            } else {
                handler.handleError(new JsonResourceException(JsonResourceException.BAD_REQUEST,
                        "Not supported queryId"));
            }
        } catch (final Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    public void deleteInstance(final ServerContext context, final String resourceId,
            final ResultHandler handler) {
        try {
            Class<? extends Metadata> metaClass = getMetadataClass(context);
            Metadata metadata = metaClass.newInstance();
            metadata.setFullName(resourceId);

            AsyncResult[] results = getConnection().delete(new Metadata[] { metadata });
            getAsyncResult(results[0]);

            JsonValue metadataResource = new JsonValue(new HashMap<String, ObjectMapper>());
            metadataResource.put("_id", metadata.getFullName());
            handler.handleResult(metadataResource);
        } catch (final Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    protected String getPartition(ServerContext context) throws JsonResourceException {
        return context.getMatcher().group(1);
    }

    private Class<? extends Metadata> getMetadataClass(final ServerContext context)
            throws JsonResourceException {
        Class<? extends Metadata> metaClass;
        if ("samlssoconfig".equalsIgnoreCase(getPartition(context))) {
            metaClass = SamlSsoConfig.class;
        } else {
            throw new JsonResourceException(JsonResourceException.NOT_FOUND, "");
        }
        return metaClass;
    }

    private AsyncResult getAsyncResult(AsyncResult result) throws JsonResourceException,
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
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR,
                        "Request timed out.  If this is a large set "
                                + "of metadata components, check that the time allowed "
                                + "by MAX_NUM_POLL_REQUESTS is sufficient.");
            }
            asyncResult = getConnection().checkStatus(new String[] { asyncResult.getId() })[0];
            logger.trace("Status is: {}", asyncResult.getState());
        }

        if (asyncResult.getState() != AsyncRequestState.Completed) {
            final JsonResourceException resourceException =
                    new JsonResourceException(JsonResourceException.INTERNAL_ERROR, asyncResult
                            .getStatusCode()
                            + " msg: " + asyncResult.getMessage());
            try {
                resourceException.setDetail(OBJECT_MAPPER.convertValue(asyncResult, Map.class));
            } catch (Exception e) {
                /* ignore */
            }
            throw resourceException;
        }
        return asyncResult;
    }

    private JsonValue getMetadataResource(String resourceId, Class<? extends Metadata> metaClass)
            throws ConnectionException, JsonResourceException, InterruptedException, IOException {
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

    static JsonValue getMetadataResource(byte[] zipFile, String type, String id)
            throws JsonResourceException, IOException {
        String entryName = null;
        Metadata metadata = null;
        if ("samlssoconfig".equalsIgnoreCase(type)) {
            metadata = new SamlSsoConfig();
            entryName = "unpackaged/samlssoconfigs/" + id + ".samlssoconfig";
        } else {
            throw new JsonResourceException(JsonResourceException.NOT_FOUND,
                    "Unknown Metadata type: " + type);
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
                        JsonValue resource =
                                new JsonValue(OBJECT_MAPPER.convertValue(metadata, Map.class));
                        resource.put("_id", null != metadata.getFullName() ? metadata.getFullName()
                                : id);
                        return resource;
                    } catch (PullParserException e) {
                        /* ignore */
                    } catch (ConnectionException e) {
                        logger.error("Failed to load the Metadata from ZIP", e);
                        throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e);
                    }
                }
            }
        } finally {
            stream.close();
        }
        return null;
    }

    @Override
    protected JsonValue create(JsonValue request) throws JsonResourceException {
        final ServerContext context = ServerContext.get();
        final ResultHandler handler = new ResultHandler();
        JsonValue content = request.get("value");
        createInstance(context, content, handler);
        return handler.getResult();
    }

    @Override
    protected JsonValue update(JsonValue request) throws JsonResourceException {
        final ServerContext context = ServerContext.get();
        if (context.getMatcher().groupCount() == 3) {
            final ResultHandler handler = new ResultHandler();
            String id = context.getMatcher().group(2);
            updateInstance(context, id, request.get("value"), handler);
            return handler.getResult();
        } else {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                    "Update collection is not supported");
        }
    }

    @Override
    protected JsonValue read(JsonValue request) throws JsonResourceException {
        final ServerContext context = ServerContext.get();
        if (context.getMatcher().groupCount() == 3) {
            final ResultHandler handler = new ResultHandler();
            String id = context.getMatcher().group(2);
            readInstance(context, id, handler);
            return handler.getResult();
        } else {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                    "Read collection is not supported");
        }
    }

    @Override
    protected JsonValue query(JsonValue request) throws JsonResourceException {
        final ServerContext context = ServerContext.get();
        if (context.getMatcher().groupCount() == 2) {
            final ResultHandler handler = new ResultHandler();
            handler.handleResource(null);
            String queryId = request.get("params").get("_queryId").required().asString();
            queryCollection(context, queryId, handler);
            return handler.getResult();
        } else {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                    "Query instance is not supported");
        }
    }

    @Override
    protected JsonValue delete(JsonValue request) throws JsonResourceException {
        final ServerContext context = ServerContext.get();
        if (context.getMatcher().groupCount() == 3) {
            final ResultHandler handler = new ResultHandler();
            String id = context.getMatcher().group(2);
            deleteInstance(context, id, handler);
            return handler.getResult();
        } else {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                    "Delete collection is not supported");
        }
    }

    public static JsonResourceException adapt(final Throwable t) {
        int resourceResultCode;
        Map<String, Object> detail = null;
        try {
            throw t;
        } catch (final JsonResourceException e) {
            return e;
        } catch (final JsonValueException e) {
            resourceResultCode = JsonResourceException.BAD_REQUEST;
        } catch (ConnectionException e) {
            resourceResultCode = JsonResourceException.INTERNAL_ERROR;
        } catch (final Throwable tmp) {
            resourceResultCode = JsonResourceException.INTERNAL_ERROR;
        }
        JsonResourceException e = new JsonResourceException(resourceResultCode, t.getMessage(), t);
        e.setDetail(detail);
        return e;
    }
}
