/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2015 ForgeRock AS.
 */
package org.forgerock.openidm.provisioner.salesforce.internal.metadata;

import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.provisioner.salesforce.internal.SalesforceConnectorUtil.adapt;
import static org.forgerock.openidm.util.ResourceUtil.getUriTemplateVariables;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
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
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.provisioner.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.util.JsonUtil;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
        OBJECT_MAPPER.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    // one second in milliseconds
    private static final long ONE_SECOND = 1000;
    // maximum number of attempts to retrieve the results
    private static final int MAX_NUM_POLL_REQUESTS = 50;
    private static final TypeMapper TYPE_MAPPER = new TypeMapper();

    private final SalesforceConnection sfconnection;

    private MetadataConnection connection;

    public MetadataResourceProvider(final SalesforceConnection connection) {
        this.sfconnection = connection;
    }

    private void init() throws ResourceException {
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
                    MetadataResourceProvider.this.connection.getSessionHeader().setSessionId(
                            config.getSessionId());
                    return header;
                }
            });
        } catch (ConnectionException e) {
            throw new ComponentException("Failed to initiate the Metadata service", e);
        }
    }

    protected MetadataConnection getConnection() throws ResourceException {
        if (null == connection) {
            synchronized (this) {
                if (null == connection) {
                    init();
                }
            }
        }
        return connection;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(final Context context, final CreateRequest request) {
        try {
            if (StringUtils.isBlank(request.getNewResourceId())) {
                Class<? extends Metadata> metaClass = getMetadataClass(context);
                Metadata metadata =
                        OBJECT_MAPPER.convertValue(request.getContent().asMap(), metaClass);

                AsyncResult[] results = getConnection().create(new Metadata[] { metadata });
                getAsyncResult(results[0]);

                ResourceResponse metadataResource = getMetadataResource(metadata.getFullName(), metaClass);
                if (null != metadataResource) {
                    return metadataResource.asPromise();
                } else {
                    return newResourceResponse(metadata.getFullName(), null, null).asPromise();
                }
            } else {
                return new NotSupportedException("Create metadata with client provided ID is not supported").asPromise();
            }
        } catch (final Throwable t) {
            return adapt(t).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(final Context context, final String resourceId,
            final UpdateRequest request) {
        try {
            Class<? extends Metadata> metaClass = getMetadataClass(context);
            Metadata metadata =
                    OBJECT_MAPPER.convertValue(request.getContent().asMap(), metaClass);

            UpdateMetadata updateMetadata = new UpdateMetadata();
            updateMetadata.setCurrentName(resourceId);
            updateMetadata.setMetadata(metadata);

            AsyncResult[] results = getConnection().update(new UpdateMetadata[] { updateMetadata });
            getAsyncResult(results[0]);

            ResourceResponse metadataResource = getMetadataResource(metadata.getFullName(), metaClass);
            if (null != metadataResource) {
                return metadataResource.asPromise();
            } else {
                return newResourceResponse(metadata.getFullName(), null, null).asPromise();
            }
        } catch (final Throwable t) {
            return adapt(t).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(final Context context, final String resourceId,
            final ReadRequest request) {
        try {
            Class<? extends Metadata> metaClass = getMetadataClass(context);

            ResourceResponse metadataResource = getMetadataResource(resourceId, metaClass);
            if (null != metadataResource) {
                return metadataResource.asPromise();
            } else {
                return new NotFoundException().asPromise();
            }
        } catch (Throwable t) {
            return adapt(t).asPromise();
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(final Context context, final QueryRequest request,
            final QueryResourceHandler handler) {
        try {
            if (ServerConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                Class<? extends Metadata> metaClass = getMetadataClass(context);
                ListMetadataQuery query = new ListMetadataQuery();
                query.setType(metaClass.getSimpleName());
                query.setFolder("*");

                FileProperties[] results =
                        getConnection().listMetadata(new ListMetadataQuery[] { query },
                                sfconnection.getAPIVersion());

                for (FileProperties file : results) {
                    handler.handleResource(newResourceResponse(
                            file.getFullName(), "", new JsonValue(OBJECT_MAPPER.convertValue(file, Map.class))));
                }
                return newQueryResponse().asPromise();
            } else {
                return new BadRequestException("Not supported queryId").asPromise();
            }
        } catch (final Throwable t) {
            return adapt(t).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context, final String resourceId,
            final DeleteRequest request) {
        try {
            Class<? extends Metadata> metaClass = getMetadataClass(context);
            Metadata metadata = metaClass.newInstance();
            metadata.setFullName(resourceId);

            AsyncResult[] results = getConnection().delete(new Metadata[] { metadata });
            getAsyncResult(results[0]);

            return newResourceResponse(metadata.getFullName(), null, null).asPromise();
        } catch (final Throwable t) {
            return adapt(t).asPromise();
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(final Context context, final String resourceId,
            final ActionRequest request) {
        return ResourceUtil.notSupportedOnInstance(request).asPromise();
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(final Context context, final ActionRequest request) {
        return ResourceUtil.notSupportedOnCollection(request).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(final Context context, final String resourceId,
            final PatchRequest request) {
        return ResourceUtil.notSupportedOnInstance(request).asPromise();
    }

    protected String getPartition(Context context) throws ResourceException {
        Map<String, String> variables = getUriTemplateVariables(context);
        if (null != variables && variables.containsKey("metadataType")) {
            return variables.get("metadataType");
        }
        throw new ForbiddenException("Direct access without Router to this service is forbidden.");
    }

    private Class<? extends Metadata> getMetadataClass(final Context context)
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

    private ResourceResponse getMetadataResource(String resourceId, Class<? extends Metadata> metaClass)
            throws ConnectionException, ResourceException, InterruptedException, IOException {
        PackageTypeMembers packageTypeMembers = new PackageTypeMembers();
        packageTypeMembers.setName(metaClass.getSimpleName());
        packageTypeMembers.setMembers(new String[] { "*" });
        Package p = new Package();
        p.setVersion(Double.toString(sfconnection.getAPIVersion()));
        p.setTypes(new PackageTypeMembers[] { packageTypeMembers });
        RetrieveRequest retrieveRequest = new RetrieveRequest();
        retrieveRequest.setApiVersion(sfconnection.getAPIVersion());
        retrieveRequest.setUnpackaged(p);

        AsyncResult asyncResult = getConnection().retrieve(retrieveRequest);
        asyncResult = getAsyncResult(asyncResult);

        RetrieveResult result = getConnection().checkRetrieveStatus(asyncResult.getId());

        return getMetadataResource(result.getZipFile(), metaClass.getSimpleName(), resourceId);
    }

    public static ResourceResponse getMetadataResource(byte[] zipFile, String type, String id)
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
                        return newResourceResponse(
                                null != metadata.getFullName() ? metadata.getFullName() : id,
                                null,
                                new JsonValue(OBJECT_MAPPER.convertValue(metadata, Map.class)));
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
