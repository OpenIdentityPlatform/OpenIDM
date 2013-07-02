/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openidm.repo.orientdb.internal;

import static org.forgerock.openidm.util.ResourceUtil.getUriTemplateVariables;
import static org.forgerock.openidm.util.ResourceUtil.notSupported;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.PreconditionRequiredException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.PropertyUtil;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.openidm.util.ResourceUtil.URLParser;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.client.remote.OEngineRemote;
import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.exception.OQueryParsingException;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndexException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.core.tx.OTransaction;

/**
 * Repository service implementation using OrientDB
 *
 * @author aegloff
 */
@Component(name = OrientDBRepoService.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE,
        enabled = true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Repository Service using OrientDB"),
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/repo/{partition}*") })
public class OrientDBRepoService implements RequestHandler {

    public static final String PID = "org.forgerock.openidm.repo.orientdb";

    /**
     * Setup logging for the {@link OrientDBRepoService}.
     */
    final static Logger logger = LoggerFactory.getLogger(OrientDBRepoService.class);

    // Monitoring event name prefix
    private static final String EVENT_RAW_QUERY_PREFIX =
            "openidm/internal/repo/orientdb/raw/query/";

    private static final String PARTITION_LINKS = "link";

    // Keys in the JSON configuration
    public static final String CONFIG_QUERIES = "queries";
    public static final String CONFIG_DB_URL = "dbUrl";
    public static final String CONFIG_USER = "user";
    public static final String CONFIG_PASSWORD = "password";
    public static final String CONFIG_DB_STRUCTURE = "dbStructure";
    public static final String CONFIG_ORIENTDB_CLASS = "orientdbClass";
    public static final String CONFIG_INDEX = "index";
    public static final String CONFIG_PROPERTY_NAME = "propertyName";
    public static final String CONFIG_PROPERTY_NAMES = "propertyNames";
    public static final String CONFIG_PROPERTY_TYPE = "propertyType";
    public static final String CONFIG_INDEX_TYPE = "indexType";

    final EmbeddedOServerService embeddedServer = new EmbeddedOServerService();

    private Map<String, String> predefinedQueries = null;

    @Activate
    void activate(ComponentContext context) throws Exception {
        JsonValue configuration = JSONEnhancedConfig.newInstance().getConfigurationAsJson(context);

        embeddedServer.activate(configuration).init(configuration);

        setConfiguredQueries(configuration.get(CONFIG_QUERIES));

        logger.info("Repository service is activated.");
    }

    /**
     * Handle an existing activated service getting changed; e.g. configuration
     * changes or dependency changes
     *
     * @param compContext
     *            THe OSGI component context
     * @throws Exception
     *             if handling the modified event failed
     */
    // @Modified
    // void modified(ComponentContext compContext) throws Exception {
    // logger.debug("Handle repository service modified notification");
    //
    // JsonValue configuration =
    // JSONEnhancedConfig.newInstance().getConfigurationAsJson(compContext);
    //
    // if (existingConfig != null &&
    // dbURL.equals(embeddedServer.getDBUrl(newConfig))
    // && user.equals(getUser(newConfig)) &&
    // password.equals(getPassword(newConfig))) {
    // // If the DB pool settings don't change keep the existing pool
    // logger.info("(Re-)initialize repository with latest configuration.");
    //
    // } else {
    // // If the DB pool settings changed do a more complete
    // // re-initialization
    // logger.info("Re-initialize repository with latest configuration - including DB pool setting changes.");
    // deactivate(compContext);
    // activate(compContext);
    // }
    // setConfiguredQueries(configuration.get(CONFIG_QUERIES));
    //
    // logger.debug("Repository service id modified");
    // }

    @Deactivate
    void deactivate(ComponentContext context) {
        embeddedServer.deactivate();
        logger.info("Repository service is deactivated.");
    }

    protected String getPartition(ServerContext context) throws ResourceException {
        Map<String, String> variables = getUriTemplateVariables(context);
        if (null != variables && variables.containsKey("partition")) {
            return variables.get("partition");
        }
        throw new ForbiddenException("Direct access without Router to this service is forbidden.");
    }

    // ----- Implementation of RequestHandler interface

    @Override
    public void handleAction(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        handler.handleError(notSupported(request));
    }

    /**
     * Gets an object from the repository by identifier. The returned object is
     * not validated against the current schema and may need processing to
     * conform to an updated schema.
     * <p>
     * The object will contain metadata properties, including object identifier
     * {@code _id}, and object version {@code _rev} to enable optimistic
     * concurrency supported by OrientDB and OpenIDM.
     *
     * @param request
     *            the identifier of the object to retrieve from the object set.
     * @throws NotFoundException
     *             if the specified object could not be found.
     * @throws ForbiddenException
     *             if access to the object is forbidden.
     * @throws BadRequestException
     *             if the passed identifier is invalid
     * @return the requested object.
     */
    @Override
    public void handleRead(final ServerContext context, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        ODatabaseDocumentTx db = null;
        try {
            db = getConnection();

            final String partition = getPartition(context);
            if (PARTITION_LINKS.equals(partition)) {
                // Use the Graph Database

            } else {
                // Use the Document Database
                Pair<String, Object> id = parseResourceName(request, partition);
                ODocument doc = getByID(id, request.getFields(), db);

                logger.trace("Completed get for orientType: {}, id: {}", id.getLeft(), id
                        .getRight());
                handler.handleResult(DocumentUtil.toResource(doc));
            }
        } catch (final Throwable t) {
            handler.handleError(adapt(t));
        } finally {
            if (null != db) {
                try {
                    db.close();
                } catch (Throwable t) {
                    logger.error("TODO: Debug this case. Should never happen!", t);
                }
            }
        }
    }

    /**
     * Creates a new object in the object set.
     * <p>
     * This method sets the {@code _id} property to the assigned identifier for
     * the object, and the {@code _rev} property to the revised object version
     * (For optimistic concurrency)
     *
     * @param context
     *            the client-generated identifier to use, or {@code null} if
     *            server-generated identifier is requested.
     * @param request
     *            the contents of the object to create in the object set.
     * @throws NotFoundException
     *             if the specified id could not be resolved.
     * @throws ForbiddenException
     *             if access to the object or object set is forbidden.
     * @throws ConflictException
     *             if an object with the same ID already exists.
     */
    @Override
    public void handleCreate(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        ODatabaseDocumentTx db = null;
        // The try-with-resources Statement JAVA 7
        // try (ODatabaseDocumentTx db = getConnection()) {
        try {
            db = getConnection();

            final String partition = getPartition(context);
            if (PARTITION_LINKS.equals(partition)) {
                // Edges

                OGraphDatabase gdb = (OGraphDatabase) getConnection();// OGraphDatabasePool.global().acquire(dbURL,
                                                                      // user,
                                                                      // password);
                try {

                    final ORecordId iSourceVertexRid =
                            new ORecordId(request.getContent().get("firstId").required().asString());
                    final ORecordId iDestVertexRid =
                            new ORecordId(request.getContent().get("secondId").required()
                                    .asString());

                    ODocument edge = null;

                    if ("/".equals(request.getResourceName())) {
                        edge = gdb.createEdge(iSourceVertexRid, iDestVertexRid);
                    } else {
                        // Use the Document Database
                        URLParser url = URLParser.parse(request.getResourceName()).last();
                        String iClassName =
                                resourceCollectionToOrientClassName(url.resourceName(), null);
                        if (gdb.getVertexType(iClassName) == null) {
                            OClass ce = gdb.createEdgeType(iClassName);
                            ce.createProperty("label", OType.STRING);
                        }
                        edge = gdb.createEdge(iSourceVertexRid, iDestVertexRid, iClassName);
                    }

                    for (Map.Entry<String, Object> entry : request.getContent().asMap().entrySet()) {
                        if (entry.getKey().startsWith("_") || "firstId".equals(entry.getKey())
                                || "secondId".equals(entry.getKey())) {
                            continue;
                        }
                        if (entry.getValue() instanceof String) {
                            edge.field(entry.getKey(), entry.getValue());
                        }
                    }
                    edge.field("label", edge.getClassName());
                    edge.save();

                    // handler.handleResult(getResource(edge.getIdentity().toString(),
                    // edge));
                } catch (OException e) {
                    logger.error("OrientDB Exception: " + e.toString());
                } finally {
                    gdb.close();
                }

            } else {
                // Vertexes

                Pair<OClass, String> id =
                        parseResourceName(db.getMetadata().getSchema(), request, partition);

                final ODocument newDocument = new ODocument(id.getLeft());

                DocumentUtil.toDocument(id.getRight(), null, request.getContent(), newDocument);

                if (logger.isTraceEnabled()) {
                    logger.trace("Created ODocument for id: {} to save {}", id.getRight(),
                            newDocument.toJSON());
                }

                try {
                    db.save(newDocument);
                    if (null == id.getRight()) {
                        try {

                            newDocument.field(DocumentUtil.ORIENTDB_PRIMARY_KEY, newDocument
                                    .getIdentity().toString().substring(1));

                            db.save(newDocument);
                        } catch (Throwable t) {
                            logger.warn("Failed to save the generated Id of object {}", newDocument
                                    .getIdentity());
                        }
                    }

                    if (logger.isTraceEnabled()) {
                        logger.trace(
                                "Create payload for orientClass: {}, resourceName: {}, resourceId: {}, doc: {}",
                                new Object[] { id.getLeft(), request.getResourceName(),
                                    request.getNewResourceId(), newDocument });
                    } else {
                        logger.debug(
                                "Completed create for partition: {}, resourceName: {}, resourceId: {}, revision: {}",
                                new Object[] { id.getLeft(), request.getResourceName(),
                                    request.getNewResourceId(), newDocument.getVersion() });
                    }

                    // The ODocument is saved we don't care about the
                    // further exceptions
                    handler.handleResult(DocumentUtil.toResource(newDocument));

                } catch (OIndexException ex) {
                    // Because the OpenIDM ID is defined as unique,
                    // duplicate inserts must fail
                    if (ex.getMessage().contains(DBHelper.UNIQUE_PRIMARY_IDX)) {
                        handler.handleError(new ConflictException(
                                "Create rejected as Object with same ID already exists. "
                                        + ex.getMessage(), ex));
                    } else {
                        handler.handleError(new ConflictException(ex));
                    }
                } catch (ODatabaseException ex) {
                    // Because the OpenIDM ID is defined as unique,
                    // duplicate inserts must fail
                    // OrientDB may wrap the IndexException root cause.
                    if (isCauseIndexException(ex, 10)) {
                        handler.handleError(new PreconditionFailedException(
                                "Create rejected as Object with same ID already exists and was detected. "
                                        + ex.getMessage(), ex));
                    } else {
                        handler.handleError(adapt(ex));
                    }
                }
            }
        } catch (final Throwable t) {
            handler.handleError(adapt(t));
        } finally {
            if (null != db) {
                try {
                    db.close();
                } catch (Throwable t) {
                    logger.error("TODO: Debug this case. Should never happen!", t);
                }
            }
        }
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        ODatabaseDocumentTx db = null;
        try {
            db = getConnection();

            final String partition = getPartition(context);
            if (PARTITION_LINKS.equals(partition)) {
                // Use the Graph Database

            } else {
                // Use the Document Database

                final Pair<String, Object> id = parseResourceName(request, partition);
                final ODocument existingODocument =
                        getODocumentForUpdate(id, request.getRevision(), db);

                handler.handleResult(update(existingODocument, id, request, db));
            }
        } catch (final Throwable t) {
            handler.handleError(adapt(t));
        } finally {
            if (null != db) {
                try {
                    db.close();
                } catch (Throwable t) {
                    logger.error("TODO: Debug this case. Should never happen!", t);
                }
            }
        }
    }

    @Override
    public void handlePatch(final ServerContext context, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        ODatabaseDocumentTx db = null;
        try {
            db = getConnection();

            final String partition = getPartition(context);
            if (PARTITION_LINKS.equals(partition)) {
                // Use the Graph Database

            } else {
                // Use the Document Database
                final Pair<String, Object> id = parseResourceName(request, partition);
                final ODocument existingODocument =
                        getODocumentForUpdate(id, request.getRevision(), db);

                final Resource existingResource = DocumentUtil.toResource(existingODocument);
                final JsonValue newContent = existingResource.getContent();

                if (ResourceUtil.applyPatchOperations(request.getPatchOperations(), newContent)) {
                    // Update
                    // TODO Fix the ID patch
                    handler.handleResult(update(DocumentUtil.toDocument(null, null, newContent,
                            existingODocument), id, null, db));
                } else {
                    // not modified;
                    handler.handleResult(existingResource);
                }
            }
        } catch (final Throwable t) {
            handler.handleError(adapt(t));
        } finally {
            if (null != db) {
                try {
                    db.close();
                } catch (Throwable t) {
                    logger.error("TODO: Debug this case. Should never happen!", t);
                }
            }
        }
    }

    /**
     * Updates the specified object in the object set.
     * <p>
     * This implementation requires MVCC and hence enforces that clients state
     * what revision they expect to be updating
     *
     * If successful, this method updates metadata properties within the passed
     * object, including: a new {@code _rev} value for the revised object's
     * version
     *
     * @param existingODocument
     *            the existing document in database identified by {@code id}
     *            parameter.
     * @param id
     *            the identifier was used to read the {@code existingODocument}.
     * @param request
     *            the contents of the object to put in the object set.
     * @throws ConflictException
     *             if version is required but is {@code null}.
     * @throws ForbiddenException
     *             if access to the object is forbidden.
     * @throws NotFoundException
     *             if the specified object could not be found.
     * @throws PreconditionFailedException
     *             if version did not match the existing object in the set.
     * @throws BadRequestException
     *             if the passed identifier is invalid
     */
    public Resource update(final ODocument existingODocument, final Pair<String, Object> id,
            final UpdateRequest request, final ODatabaseDocumentTx database)
            throws ResourceException {

        // At this point we don't know if the last segment of resourceName is
        // meant to be id or type
        // The https://bugster.forgerock.org/jira/browse/OPENIDM-739 is not
        // fixed at this level
        // the partition=ui but resourceName will be parsed to {"notification"}
        // or {"notification","7"}

        // partition and optional sub-type together defines the OrientDB
        // Document type!?

        try {
            database.begin();
            ODocument updatedODocument = null;
            if (null != request) {
                updatedODocument =
                        DocumentUtil.toDocument(null, request.getRevision(), request
                                .getNewContent(), existingODocument);

            } else {
                updatedODocument = existingODocument;
            }

            logger.trace("Updated doc for orientType: {}, resourceName: {}, to save {}",
                    new Object[] { id.getLeft(), id.getRight(), updatedODocument });

            database.save(updatedODocument);
            database.commit();
            return DocumentUtil.toResource(updatedODocument);
        } catch (OConcurrentModificationException ex) {
            database.rollback();
            throw new PreconditionFailedException(
                    "Update rejected as current Object revision is different than expected by caller, the object has changed since retrieval: "
                            + ex.getMessage(), ex);
        } catch (RuntimeException e) {
            database.rollback();
            throw e;
        }
    }

    /**
     * Deletes the specified object from the object set.
     *
     * {@inheritDoc}
     *
     * @throws NotFoundException
     *             if the specified object could not be found.
     * @throws ForbiddenException
     *             if access to the object is forbidden.
     * @throws ConflictException
     *             if version is required but is {@code null}.
     * @throws PreconditionFailedException
     *             if version did not match the existing object in the set.
     */
    @Override
    public void handleDelete(final ServerContext context, final DeleteRequest request,
            final ResultHandler<Resource> handler) {
        ODatabaseDocumentTx db = null;
        try {
            db = getConnection();

            final String partition = getPartition(context);
            if (PARTITION_LINKS.equals(partition)) {
                // Use the Graph Database

            } else {
                // Use the Document Database

                Pair<String, Object> id = parseResourceName(request, partition);

                final ODocument existingODocument =
                        getODocumentForUpdate(id, request.getRevision(), db);
                if (db.isMVCC()) {
                    try {
                        db.delete(existingODocument);
                    } catch (OConcurrentModificationException ex) {
                        throw new PreconditionFailedException(
                                "Delete rejected as current Object revision is different than expected by caller, the object has changed since retrieval.",
                                ex);
                    }
                    /*
                     * catch (OException e) { // TODO: investigate if the
                     * document is deleted in another thread }
                     */
                } else {
                    // How to delete now
                    db.begin(OTransaction.TXTYPE.OPTIMISTIC);
                    try {
                        db.delete(existingODocument);
                        db.commit();
                    } catch (Exception e) {
                        db.rollback();
                        throw e;
                    }
                }
                handler.handleResult(getResource(null, existingODocument));
            }
        } catch (final Throwable t) {
            handler.handleError(adapt(t));
        } finally {
            if (null != db) {
                try {
                    db.close();
                } catch (Throwable t) {
                    logger.error("TODO: Debug this case. Should never happen!", t);
                }
            }
        }
    }

    public Resource _delete(String orientClassName, String localId, DeleteRequest request)
            throws ResourceException {
        // Parse the remaining resourceName
        String[] resourceName = ResourceUtil.parseResourceName(request.getResourceName());
        if (resourceName == null) {
            throw new BadRequestException(
                    "The repository requires clients to supply an identifier for the object to update.");
        }

        // At this point we don't know if the last segment of resourceName is
        // meant to be id or type
        // The https://bugster.forgerock.org/jira/browse/OPENIDM-739 is not
        // fixed at this level
        // the partition=ui but resourceName will be parsed to {"notification"}
        // or {"notification","7"}

        // partition and optional sub-type together defines the OrientDB
        // Document type!?

        // This throws ConflictException if parse fails
        int ver = DocumentUtil.parseVersion(request.getRevision());

        ODatabaseDocumentTx db = getConnection();
        try {
            db.begin();
            ODocument existingDoc = null;// getByID(orientClassName, localId,
                                         // db);
            if (existingDoc == null) {
                throw new NotFoundException("Object does not exist for delete on: "
                        + orientClassName + "/" + localId);
            }

            if (!StringUtils.isBlank(request.getRevision())) {
                // State the version we expect to delete for MVCC check
                existingDoc.setVersion(DocumentUtil.parseVersion(request.getRevision()));
                // request.getNewContent().put(Resource.FIELD_CONTENT_REVISION,
                // request.getRevision());
            }

            db.delete(existingDoc);
            db.commit();
            logger.debug("delete for id succeeded: {} revision: {}", localId, request.getRevision());
            return DocumentUtil.toResource(existingDoc);
        } catch (OConcurrentModificationException ex) {
            db.rollback();
            throw new PreconditionFailedException(
                    "Delete rejected as current Object revision is different than expected by caller, the object has changed since retrieval.",
                    ex);
        } catch (RuntimeException e) {
            db.rollback();
            throw e;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * Performs the query on the specified object and returns the associated
     * results.
     * <p>
     * Queries are parametric; a set of named parameters is provided as the
     * query criteria. The query result is a JSON object structure composed of
     * basic Java types.
     *
     * The returned map is structured as follow: - The top level map contains
     * meta-data about the query, plus an entry with the actual result records.
     * - The <code>QueryConstants</code> defines the map keys, including the
     * result records (QUERY_RESULT)
     *
     * @param context
     *            identifies the object to query.
     * @param request
     *            the parameters of the query to perform.
     * @return the query results, which includes meta-data and the result
     *         records in JSON object structure format.
     * @throws NotFoundException
     *             if the specified object could not be found.
     * @throws BadRequestException
     *             if the specified params contain invalid arguments, e.g. a
     *             query id that is not configured, a query expression that is
     *             invalid, or missing query substitution tokens.
     * @throws ForbiddenException
     *             if access to the object or specified query is forbidden.
     */
    @Override
    public void handleQuery(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        ODatabaseDocumentTx db = null;
        try {
            db = getConnection();

            String queryExpression = null;
            if (request.getQueryFilter() != null) {
                handler.handleError(new NotSupportedException("Query by Filter not supported"));
                return;
            } else if (StringUtils.isNotBlank(request.getQueryId())) {
                if (!predefinedQueries.containsKey(request.getQueryId())) {
                    handler.handleError(new BadRequestException(
                            "The passed query identifier "
                                    + request.getQueryId()
                                    + " does not match any configured queries on the OrientDB repository service."));
                    return;
                }
                queryExpression = predefinedQueries.get(request.getQueryId());
            }

            final String partition = getPartition(context);
            if (PARTITION_LINKS.equals(partition)) {
                // Use the Graph Database

            } else {
                // Use the Document Database
                URLParser url = URLParser.parse(request.getResourceName()).last();
                logger.trace("Partition: {} resourceName: {}", partition, url.resourceName());

                Map<String, String> params =
                        new HashMap<String, String>(request.getAdditionalQueryParameters());
                params.put(ServerConstants.RESOURCE_NAME, resourceCollectionToOrientClassName(
                        partition, url.resourceName()));

                try {
                    final AtomicReference<OIdentifiable> lastRecord =
                            new AtomicReference<OIdentifiable>();

                    if (db.getURL().startsWith(OEngineRemote.NAME)) {
                        OSQLAsynchQuery<ODocument> query =
                                new OSQLAsynchQuery(((String) PropertyUtil.substVars(
                                        queryExpression, new OrientSQLPropertyAccessor(params),
                                        PropertyUtil.Delimiter.DOLLAR, true)));
                        query.setResultListener(new OCommandResultListener() {
                            @Override
                            public boolean result(Object iRecord) {
                                final Resource r = DocumentUtil.toResource((ODocument) iRecord);
                                boolean accepted = handler.handleResource(r);
                                if (accepted) {
                                    lastRecord.set((OIdentifiable) iRecord);
                                }
                                return accepted;
                            }

                            public void end() {
                                /* there is nothing to clean up */
                            }
                        });
                        logger.debug("Manual token substitution for {} resulted in {}",
                                queryExpression, query);
                        db.query(query, params);
                    } else {
                        OSQLSynchQuery<ODocument> query =
                                new OSQLSynchQuery(((String) PropertyUtil.substVars(
                                        queryExpression, new OrientSQLPropertyAccessor(params),
                                        PropertyUtil.Delimiter.DOLLAR, true)));
                        logger.debug("Manual token substitution for {} resulted in {}",
                                queryExpression, query);
                        for (Object document :  db.query(query, params)) {
                            handler.handleResource(DocumentUtil.toResource((ODocument)document));
                        }
                    }
                    handler.handleResult(new QueryResult());
                } catch (OQueryParsingException firstTryEx) {
                    // TODO: consider differentiating between bad
                    // configuration
                    // and bad request
                    handler.handleError(new BadRequestException(
                            "Failed to resolve and parse the query " + queryExpression
                                    + " with params: " + params, firstTryEx));
                } catch (IllegalArgumentException ex) {
                    // TODO: consider differentiating between bad
                    // configuration
                    // and bad request
                    handler.handleError(new BadRequestException("Query is invalid: "
                            + queryExpression + " " + ex.getMessage(), ex));
                } catch (RuntimeException ex) {
                    logger.warn("Unexpected failure during DB query: {}", ex.getMessage(), ex);
                    handler.handleError(new InternalServerErrorException(ex));
                } finally {
                    // measure.end();
                }
            }
        } catch (final Throwable t) {
            handler.handleError(adapt(t));
        } finally {
            if (null != db) {
                try {
                    db.close();
                } catch (Throwable t) {
                    logger.error("TODO: Debug this case. Should never happen!", t);
                }
            }
        }
    }

    /**
     * @return A connection from the pool. Call close on the connection when
     *         done to return to the pool.
     * @throws InternalServerErrorException
     */
    ODatabaseDocumentTx getConnection() throws ResourceException {
        return embeddedServer.getConnection();
    }

    String resourceCollectionToOrientClassName(String partition, String resourceName) {
        if ("/".equals(resourceName)) {
            return partition;
        } else {
            return (partition + resourceName).replace("/", "_");
        }
    }

    // public static String idToOrientClassName(String id) {
    // String type = getObjectType(id);
    // return typeToOrientClassName(type);
    // }

    /**
     * Detect if the root cause of the exception is an index constraint
     * violation This is necessary as the database may wrap this root cause in
     * further exceptions, masking the underlying cause
     *
     * @param ex
     *            The throwable to check
     * @param maxLevels
     *            the maximum level of causes to check, avoiding the cost of
     *            checking recursiveness
     * @return
     */
    private boolean isCauseIndexException(Throwable ex, int maxLevels) {
        if (maxLevels > 0) {
            Throwable cause = ex.getCause();
            if (cause != null) {
                return cause instanceof OIndexException
                        || isCauseIndexException(cause, maxLevels - 1);
            }
        }
        return false;
    }

    // private ServiceRegistration<GraphConnectionFactory>
    // graphServiceRegistration = null;

    // private String getDBUrl(JsonValue config) {
    // File dbFolder = IdentityServer.getFileForWorkingPath("db/openidm");
    // String orientDbFolder = dbFolder.getAbsolutePath();
    // orientDbFolder = orientDbFolder.replace('\\', '/'); // OrientDB does not
    // // handle
    // // backslashes well
    // return config.get(OrientDBRepoService.CONFIG_DB_URL).defaultTo("local:" +
    // orientDbFolder)
    // .asString();
    // }

//    private String getUser(JsonValue config) {
//        return config.get(CONFIG_USER).defaultTo("admin").asString();
//    }
//
//    private String getPassword(JsonValue config) {
//        return config.get(CONFIG_PASSWORD).defaultTo("admin").asString();
//    }

    /**
     * Set the pre-configured queries, which are identified by a query
     * identifier and can be invoked using this identifier
     *
     * Success to set the queries does not mean they are valid as some can only
     * be validated at query execution time.
     *
     * @param queries
     *            the complete list of configured queries, mapping from query id
     *            to the query expression which may optionally contain tokens in
     *            the form ${token-name}.
     * @throws JsonValueException
     *             if the {@code queries} is not {@code Map}
     */
    protected void setConfiguredQueries(JsonValue queries) {
        Map<String, String> prepQueries = new HashMap<String, String>();

        // Query all IDs is a mandatory query, default it and allow override.
        prepQueries.put(ServerConstants.QUERY_ALL_IDS, "select _openidm_id from ${_resource}");

        // Populate/Override with Queries configured
        if (queries != null && !queries.isNull()) {
            for (Map.Entry<String, Object> entry : queries.asMap().entrySet()) {
                if (entry.getValue() instanceof String) {
                    prepQueries.put(entry.getKey(), (String) entry.getValue());
                }
            }
        }
        predefinedQueries = Collections.unmodifiableMap(prepQueries);
    }

    /**
     * Query by primary key, the OpenIDM identifier. This identifier is
     * different from the OrientDB internal record id.
     *
     * @param id
     *            the OpenIDM identifier for an object
     * @param fieldFilters
     *            the OrientDB class
     * @param database
     *            a handle to the OrientDB database object. No other thread must
     *            operate on this concurrently.
     * @return The ODocument if found, null if not found.
     * @throws NotFoundException
     *             if the ODocument not exits
     */
    public ODocument getByID(final Pair<String, Object> id, final List<JsonPointer> fieldFilters,
            final ODatabaseDocumentTx database) throws NotFoundException,
            InternalServerErrorException {
        ODocument oDocument = null;

        if (id.getRight() instanceof ORecordId) {
            try {
                ORecordId RID = (ORecordId) id.getRight();
                Object o = database.getRecord(RID); // database.getRecord(new
                                                    // ORecordId("#12:1"))
                if (o instanceof ODocument) {
                    oDocument = (ODocument) o;
                } else {
                    logger.warn("The requested record is not a Document {}", o);
                }
            } catch (ODatabaseException e) {
                logger.error("Invalid id {}", id, e);
            }
        }

        // TODO build the fileds from the fieldFilters
        OSQLSynchQuery<ODocument> query =
                new OSQLSynchQuery<ODocument>("select @rid from " + id.getLeft() + " where "
                        + DocumentUtil.ORIENTDB_PRIMARY_KEY + " = ? ");
        try {
            List<ODocument> result = database.query(query, String.valueOf(id.getRight()));

            if (result.size() == 1) {
                // ID is of type unique index, there must only be one at most

                // This trick gets the full oDocument and loads the other
                // attributes
                oDocument = result.get(0).field("rid");
            } else if (result.size() > 1) {
                throw new InternalServerErrorException("The resource with ID '"
                        + String.valueOf(id.getRight()) + "is not unique");
            }
        } catch (final OQueryParsingException e) {
            if (e.getCause().getMessage().equalsIgnoreCase(
                    "Class '" + id.getLeft() + "' was not found in current database")) {
                throw new NotFoundException();
            } else {
                throw e;
            }
        }

        if (oDocument == null) {
            throw new NotFoundException("The resource with ID '" + String.valueOf(id.getRight())
                    + "' could not be read because it does not exist");
        }
        return oDocument;
    }

    private Resource getResource(String resourceId, ODocument newDoc) {
        JsonValue content = new JsonValue(new LinkedHashMap<String, Object>(3));
        // _ID
        content.put(Resource.FIELD_CONTENT_ID, resourceId);
        // _REV
        String rev = Integer.toString(newDoc.getVersion());
        content.put(Resource.FIELD_CONTENT_REVISION, Integer.toString(newDoc.getVersion()));
        // _VERTEX
        // String vid = String.format("#%d:%d",
        // newDoc.getIdentity().getClusterId(),
        // newDoc.getIdentity().getClusterPosition().intValue());
        content.put("_vertex", newDoc.getIdentity().toString());
        return new Resource(resourceId, rev, content);
    }

    private ODocument getODocumentForUpdate(final Pair<String, Object> id, final String rev,
            final ODatabaseDocumentTx database) throws ResourceException {
        if (rev != null && StringUtils.isBlank(rev)) {
            throw new PreconditionRequiredException();
        }

        final ODocument document = getByID(id, null, database);
        if (rev != null && !"*".equals(rev) && !rev.equals(Integer.toString(document.getVersion()))) {
            throw new PreconditionFailedException("The resource with ID '" + id
                    + "' could not be updated because it does not have the required version");
        }
        return document;
    }

    /**
     * Adapts a {@code Throwable} to a {@code ResourceException}. If the
     * {@code Throwable} is an JSON {@code JsonValueException} then an
     * appropriate {@code ResourceException} is returned, otherwise an
     * {@code InternalServerErrorException} is returned.
     *
     * @param t
     *            The {@code Throwable} to be converted.
     * @return The equivalent resource exception.
     */
    public ResourceException adapt(final Throwable t) {
        int resourceResultCode;
        try {
            throw t;
        } catch (OConcurrentModificationException ex) {
            resourceResultCode = ResourceException.VERSION_MISMATCH;
        } catch (final ResourceException e) {
            return e;
        } catch (final JsonValueException e) {
            resourceResultCode = ResourceException.BAD_REQUEST;
        } catch (final Throwable tmp) {
            resourceResultCode = ResourceException.INTERNAL_ERROR;
        }
        return ResourceException.getException(resourceResultCode, t.getMessage(), t);
    }

    private Pair<OClass, String> parseResourceName(final OSchema schema,
            final CreateRequest request, final String partition) throws ResourceException {

        Pair<String, Object> pair = parseResourceName(request, partition);

        OClass iClass = schema.getClass(pair.getLeft());
        if (null == iClass) {

            // TODO Move this to the Util class
            OClass superClass = schema.getClass(DBHelper.CLASS_JSON_RESOURCE);

            iClass = schema.createClass(pair.getLeft(), superClass);
            schema.save();
        }
        return Pair.of(iClass, request.getNewResourceId());
    }

    /*
     * This method has to be checked in a distributed environment to check what
     * is the disadvantages of getting the schema in advance. The Pair<String,
     * Object> may be better.
     */
    private Pair<String, Object> parseResourceName(final Request request, final String partition)
            throws ResourceException {

        if (request instanceof CreateRequest) {
            final CreateRequest cr = (CreateRequest) request;
            String resourceName = null;
            if (StringUtils.isNotBlank(cr.getNewResourceId())) {
                try {
                    ORecordId rid = new ORecordId(cr.getNewResourceId());
                    throw new BadRequestException("The id can not have the format %d:%d " + rid);
                } catch (IllegalArgumentException e) {
                    /* expected */
                    resourceName = cr.getNewResourceId();
                }
            }

            String iClassName =
                    resourceCollectionToOrientClassName(partition, request.getResourceName());

            return Pair.of(iClassName, (Object) resourceName);
        } else {
            URLParser url = URLParser.parse(request.getResourceName()).last();
            String iClassName = partition;

            if (url.index() < 0) {
                throw new BadRequestException(
                        "The repository requires clients to supply an identifier for the object to read.");
            } else if (url.index() > 0) {
                iClassName =
                        resourceCollectionToOrientClassName(partition, url.resourceCollection());
            }

            try {
                ORecordId RID = new ORecordId(url.value());
                return Pair.of(iClassName, (Object) RID);
            } catch (IllegalArgumentException e) {
                return Pair.of(iClassName, (Object) url.value());
            }

        }
    }
}
