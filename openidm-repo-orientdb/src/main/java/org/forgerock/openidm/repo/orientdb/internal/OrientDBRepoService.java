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

import java.io.File;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.FutureResult;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.PropertyUtil;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.graph.GraphConnectionFactory;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.openidm.util.ResourceUtil.URLParser;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.command.OCommandManager;
import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.graph.OGraphDatabasePool;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.exception.OQueryParsingException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndexException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandExecutorSQLDelegate;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

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
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/repo/{partition}*"),
    @Property(name = "db.type", value = "OrientDB") })
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

    private ODatabaseDocumentPool pool;

    private String dbURL;
    private String user;
    private String password;
    private int poolMinSize = 5;
    private int poolMaxSize = 20;

    // Current configuration
    private JsonValue existingConfig;

    private EmbeddedOServerService embeddedServer;

    private Map<String, String> predefinedQueries = null;

    protected String getPartition(ServerContext context) throws ResourceException {
        Map<String, String> variables = ResourceUtil.getUriTemplateVariables(context);
        if (null != variables && variables.containsKey("partition")) {
            return variables.get("partition");
        }
        throw new ForbiddenException("Direct access without Router to this service is forbidden.");
    }

    @Override
    public void handleAction(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Action operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void handleRead(ServerContext context, ReadRequest request,
            ResultHandler<Resource> handler) {
        try {
            String partition = getPartition(context);
            if (PARTITION_LINKS.equals(partition)) {
                // Use the Graph Database

            } else {
                // Use the Document Database
                URLParser url = URLParser.parse(request.getResourceName()).last();
                String orientClassName =
                        resourceCollectionToOrientClassName(partition, url.resourceCollection());

                handler.handleResult(read(orientClassName, url.value(), request));
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
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
     * @param orientClassName
     *            the identifier of the object set to retrieve from.
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
    public Resource read(String orientClassName, String localId, ReadRequest request)
            throws ResourceException {
        // Parse the remaining resourceName
        if (localId == null) {
            throw new BadRequestException(
                    "The repository requires clients to supply an identifier for the object to read.");
        }
        // At this point we don't know if the last segment of resourceName is
        // meant to be id or type
        // The https://bugster.forgerock.org/jira/browse/OPENIDM-739 is not
        // fixed at this level
        // the partition=ui but resourceName will be parsed to {"notification"}
        // or {"notification","7"}

        // partition and optional sub-type together defines the OrientDB
        // Document type!?

        Resource result = null;
        ODatabaseDocumentTx db = getConnection();
        try {
            ODocument doc = getByID(orientClassName, localId, db);
            if (doc == null) {
                throw new NotFoundException("Object repo/" + localId + " not found in "
                        + orientClassName);
            }
            result = DocumentUtil.toMap(doc, request.getFields());
            logger.trace("Completed get for orientType: {}, id: {}, result: {}", new Object[] {
                orientClassName, localId, result });
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return result;
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request,
            ResultHandler<Resource> handler) {
        try {
            String partition = getPartition(context);
            if (PARTITION_LINKS.equals(partition)) {
                // Use the Graph Database
                OGraphDatabase gdb = OGraphDatabasePool.global().acquire(dbURL, user, password);
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

                    handler.handleResult(getResource(edge.getIdentity().toString(), edge));
                } catch (OException e) {
                    logger.error("OrientDB Exception: " + e.toString());
                } finally {
                    gdb.close();
                }

            } else {
                // Use the Document Database
                URLParser url = URLParser.parse(request.getResourceName());
                String orientClassName =
                        resourceCollectionToOrientClassName(partition, url.last().resourceName());
                handler.handleResult(create(orientClassName, request));
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    /**
     * Creates a new object in the object set.
     * <p>
     * This method sets the {@code _id} property to the assigned identifier for
     * the object, and the {@code _rev} property to the revised object version
     * (For optimistic concurrency)
     *
     * @param orientClassName
     *            the client-generated identifier to use, or {@code null} if
     *            server-generated identifier is requested.
     * @param request
     *            the contents of the object to create in the object set.
     * @throws NotFoundException
     *             if the specified id could not be resolved.
     * @throws ForbiddenException
     *             if access to the object or object set is forbidden.
     * @throws PreconditionFailedException
     *             if an object with the same ID already exists.
     */
    public Resource create(String orientClassName, CreateRequest request) throws ResourceException {
        // It's a POST _action=create
        if (StringUtils.isBlank(request.getNewResourceId())) {
            request.setNewResourceId(UUID.randomUUID().toString());
        }
        // The ResourceName may be "/" only

        // if (fullId == null || localId == null) {
        // throw new
        // NotFoundException("The repository requires clients to supply an identifier for the object to create. Full identifier: "
        // + fullId + " local identifier: " + localId);
        // } else if (type == null) {
        // throw new
        // NotFoundException("The object identifier did not include sufficient information to determine the object type: "
        // + fullId);
        // }

        // request.getContent().put(Resource.FIELD_CONTENT_ID,
        // localId);

        // TODO ODocument instances always refer to the thread-local database
        ODatabaseDocumentTx db = getConnection();
        try {
            // Rather than using MVCC for insert, rely on primary key uniqueness
            // constraints to detect duplicate create
            Map<String, Object> newContent = request.getContent().asMap();
            newContent.put(Resource.FIELD_CONTENT_ID, request.getNewResourceId());
            ODocument newDoc =
                    DocumentUtil.toDocument(request.getContent().asMap(), null, orientClassName);
            // TODO Fix the logging
            // logger.trace("Created doc for id: {} to save {}", fullId,
            // newDoc);
            newDoc.save();

            // request.getContent().put(Resource.FIELD_CONTENT_REVISION,
            // Integer.toString(newDoc.getVersion()));
            if (logger.isTraceEnabled()) {
                logger.trace(
                        "Create payload for orientClass: {}, resourceName: {}, resourceId: {}, doc: {}",
                        new Object[] { orientClassName, request.getResourceName(),
                            request.getNewResourceId(), newDoc });
            } else {
                logger.debug(
                        "Completed create for partition: {}, resourceName: {}, resourceId: {}, revision: {}",
                        new Object[] { orientClassName, request.getResourceName(),
                            request.getNewResourceId(), newDoc.getVersion() });
            }

            return getResource(request.getNewResourceId(), newDoc);
        } catch (OIndexException ex) {
            // Because the OpenIDM ID is defined as unique, duplicate inserts
            // must fail
            throw new PreconditionFailedException(
                    "Create rejected as Object with same ID already exists. " + ex.getMessage(), ex);
        } catch (com.orientechnologies.orient.core.exception.ODatabaseException ex) {
            // Because the OpenIDM ID is defined as unique, duplicate inserts
            // must fail.
            // OrientDB may wrap the IndexException root cause.
            if (isCauseIndexException(ex, 10)) {
                throw new PreconditionFailedException(
                        "Create rejected as Object with same ID already exists and was detected. "
                                + ex.getMessage(), ex);
            } else {
                throw ex;
            }
        } catch (RuntimeException e) {
            throw e;
        } finally {
            if (db != null) {
                db.close();
            }
        }
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

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        try {
            String partition = getPartition(context);
            if (PARTITION_LINKS.equals(partition)) {
                // Use the Graph Database

            } else {
                // Use the Document Database
                URLParser url = URLParser.parse(request.getResourceName());
                String orientClassName =
                        resourceCollectionToOrientClassName(partition, url.last()
                                .resourceCollection());

                handler.handleResult(update(orientClassName, url.last().value(), request));
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
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
     * @param orientClassName
     *            the identifier of the object to be put, or {@code null} to
     *            request a generated identifier.
     * @param localId
     *            the version of the object to update; or {@code null} if not
     *            provided.
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
    public Resource update(String orientClassName, String localId, UpdateRequest request)
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

        // TODO http://code.google.com/p/orient/wiki/JavaMultiThreading
        ODatabaseDocumentTx db = getConnection();
        try {
            db.begin();
            ODocument existingDoc = getByID(orientClassName, localId, db);
            if (existingDoc == null) {
                throw new NotFoundException("Update on object " + orientClassName + "/" + localId
                        + " could not find existing object.");
            }
            ODocument updatedDoc =
                    DocumentUtil.toDocument(request.getNewContent().asMap(), existingDoc,
                            orientClassName);

            if (!StringUtils.isBlank(request.getRevision())) {
                updatedDoc.setVersion(DocumentUtil.parseVersion(request.getRevision()));
                // request.getNewContent().put(Resource.FIELD_CONTENT_REVISION,
                // request.getRevision());
            }
            logger.trace("Updated doc for orientType: {}, resourceName: {}, to save {}",
                    new Object[] { orientClassName, request.getResourceName(), updatedDoc });

            updatedDoc.save();
            db.commit();

            // obj.put(DocumentUtil.TAG_REV,
            // Integer.toString(updatedDoc.getVersion()));
            // Set ID to return to caller
            // obj.put(DocumentUtil.TAG_ID,
            // updatedDoc.field(DocumentUtil.ORIENTDB_PRIMARY_KEY));
            // TODO Fix the logging
            // logger.debug("Committed update for id: {} revision: {}", fullId,
            // updatedDoc.getVersion());
            // logger.trace("Update payload for id: {} doc: {}", fullId,
            // updatedDoc);
            return getResource(localId, updatedDoc);
        } catch (OConcurrentModificationException ex) {
            db.rollback();
            throw new PreconditionFailedException(
                    "Update rejected as current Object revision is different than expected by caller, the object has changed since retrieval: "
                            + ex.getMessage(), ex);
        } catch (RuntimeException e) {
            db.rollback();
            throw e;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request,
            ResultHandler<Resource> handler) {
        try {
            String partition = getPartition(context);
            if (PARTITION_LINKS.equals(partition)) {
                // Use the Graph Database

            } else {
                // Use the Document Database
                URLParser url = URLParser.parse(request.getResourceName());
                String orientClassName =
                        resourceCollectionToOrientClassName(partition, url.last()
                                .resourceCollection());

                handler.handleResult(delete(orientClassName, url.last().value(), request));
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    /**
     * Deletes the specified object from the object set.
     *
     * @param orientClassName
     *            the identifier of the object to be deleted.
     * @param localId
     *            the version of the object to delete or {@code null} if not
     *            provided.
     * @throws NotFoundException
     *             if the specified object could not be found.
     * @throws ForbiddenException
     *             if access to the object is forbidden.
     * @throws ConflictException
     *             if version is required but is {@code null}.
     * @throws PreconditionFailedException
     *             if version did not match the existing object in the set.
     */
    public Resource delete(String orientClassName, String localId, DeleteRequest request)
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
            ODocument existingDoc = getByID(orientClassName, localId, db);
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
            return DocumentUtil.toMap(existingDoc, request.getFields());
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
        try {
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

            String partition = getPartition(context);
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

                ODatabaseDocumentTx database = getConnection();
                try {
                    final AtomicReference<OIdentifiable> lastRecord =
                            new AtomicReference<OIdentifiable>();

                    // This blocks the thread and wait until the query is
                    // finished to fetch the last record.
                    // TODO How to submit and make is async if the DB supports
                    // it?
                    OSQLAsynchQuery<ODocument> query =
                            new OSQLAsynchQuery(((String) PropertyUtil.substVars(queryExpression,
                                    new OrientSQLPropertyAccessor(params),
                                    PropertyUtil.Delimiter.DOLLAR, true))) {
                                @Override
                                public List run(Object... iArgs) {
                                    super.run(iArgs);
                                    final OIdentifiable i = lastRecord.get();

                                    if (null != i) {
                                        final ORID lastRid = i.getIdentity();
                                        handler.handleResult(new QueryResult(lastRid.next(), -1));
                                    } else {
                                        // TODO How to handle empty result???
                                        handler.handleResult(new QueryResult(null, -1));
                                    }

                                    return Collections.emptyList();
                                }
                            };
                    logger.debug("Manual token substitution for {} resulted in {}",
                            queryExpression, query);
                    query.setResultListener(new OCommandResultListener() {
                        @Override
                        public boolean result(Object iRecord) {
                            final Resource r =
                                    DocumentUtil.toMap((ODocument) iRecord, request.getFields());
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
                    OCommandManager.instance().registerExecutor(query.getClass(),
                            OCommandExecutorSQLDelegate.class);
                    database.command(query).execute(params);
                } catch (OQueryParsingException firstTryEx) {
                    // TODO: consider differentiating between bad configuration
                    // and bad request
                    handler.handleError(new BadRequestException(
                            "Failed to resolve and parse the query " + queryExpression
                                    + " with params: " + params, firstTryEx));
                } catch (IllegalArgumentException ex) {
                    // TODO: consider differentiating between bad configuration
                    // and bad request
                    handler.handleError(new BadRequestException("Query is invalid: "
                            + queryExpression + " " + ex.getMessage(), ex));
                } catch (RuntimeException ex) {
                    logger.warn("Unexpected failure during DB query: {}", ex.getMessage(), ex);
                    handler.handleError(new InternalServerErrorException(ex));
                } finally {
                    // measure.end();
                    if (database != null) {
                        database.close();
                    }
                }
            }
        } catch (final ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    /**
     * @return A connection from the pool. Call close on the connection when
     *         done to return to the pool.
     * @throws InternalServerErrorException
     */
    ODatabaseDocumentTx getConnection() throws InternalServerErrorException {
        ODatabaseDocumentTx db = null;
        int maxRetry = 100; // give it up to approx 10 seconds to recover
        int retryCount = 0;

        while (db == null && retryCount < maxRetry) {
            retryCount++;
            try {
                db = pool.acquire(dbURL, user, password);
                if (retryCount > 1) {
                    logger.info("Succeeded in acquiring connection from pool in retry attempt {}",
                            retryCount);
                }
                retryCount = maxRetry;
            } catch (com.orientechnologies.orient.core.exception.ORecordNotFoundException ex) {
                // TODO: remove work-around once OrientDB resolves this
                // condition
                if (retryCount == maxRetry) {
                    logger.warn(
                            "Failure reported acquiring connection from pool, retried {} times before giving up.",
                            retryCount, ex);
                    throw new InternalServerErrorException(
                            "Failure reported acquiring connection from pool, retried "
                                    + retryCount + " times before giving up: " + ex.getMessage(),
                            ex);
                } else {
                    logger.info("Pool acquire reported failure, retrying - attempt {}", retryCount);
                    logger.trace("Pool acquire failure detail ", ex);
                    try {
                        Thread.sleep(100); // Give the DB time to complete what
                                           // it's doing before retrying
                    } catch (InterruptedException iex) {
                        // ignore that sleep was interrupted
                    }
                }
            }
        }
        return db;
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

    private ServiceRegistration<GraphConnectionFactory> graphServiceRegistration = null;

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating Service with configuration {}", compContext.getProperties());

        try {
            existingConfig = JSONEnhancedConfig.newInstance().getConfigurationAsJson(compContext);
        } catch (RuntimeException ex) {
            logger.warn(
                    "Configuration invalid and could not be parsed, can not start OrientDB repository: "
                            + ex.getMessage(), ex);
            throw ex;
        }
        embeddedServer = new EmbeddedOServerService();
        embeddedServer.activate(existingConfig);

        init(existingConfig);

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_DESCRIPTION, "Repository Service using OrientDB");
        properties.put(Constants.SERVICE_VENDOR, ServerConstants.SERVER_VENDOR_NAME);

        graphServiceRegistration =
                compContext.getBundleContext().registerService(GraphConnectionFactory.class,
                        new GraphConnectionFactory() {
                            @Override
                            public Graph getConnection() throws ResourceException {
                                return new OrientGraph(OGraphDatabasePool.global().acquire(dbURL,
                                        user, password));
                            }

                            @Override
                            public FutureResult<Graph> getConnectionAsync(
                                    ResultHandler<Graph> handler) {
                                return null;
                            }
                        }, properties);

        logger.info("Repository started.");
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
    @Modified
    void modified(ComponentContext compContext) throws Exception {
        logger.debug("Handle repository service modified notification");
        JsonValue newConfig = null;
        try {
            newConfig = JSONEnhancedConfig.newInstance().getConfigurationAsJson(compContext);
        } catch (RuntimeException ex) {
            logger.warn(
                    "Configuration invalid and could not be parsed, can not start OrientDB repository",
                    ex);
            throw ex;
        }
        if (existingConfig != null && dbURL.equals(getDBUrl(newConfig))
                && user.equals(getUser(newConfig)) && password.equals(getPassword(newConfig))) {
            // If the DB pool settings don't change keep the existing pool
            logger.info("(Re-)initialize repository with latest configuration.");
            init(newConfig);
        } else {
            // If the DB pool settings changed do a more complete
            // re-initialization
            logger.info("Re-initialize repository with latest configuration - including DB pool setting changes.");
            deactivate(compContext);
            activate(compContext);
        }

        existingConfig = newConfig;
        logger.debug("Repository service modified");
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext);
        if (null != graphServiceRegistration) {
            graphServiceRegistration.unregister();
            graphServiceRegistration = null;
        }

        cleanup();
        if (embeddedServer != null) {
            embeddedServer.deactivate();
        }
        logger.info("Repository stopped.");
    }

    /**
     * Initialize the instnace with the given configuration.
     *
     * This can configure managed (DS/SCR) instances, as well as explicitly
     * instantiated (bootstrap) instances.
     *
     * @param config
     *            the configuration
     */
    void init(JsonValue config) {
        try {
            dbURL = getDBUrl(config);
            user = getUser(config);
            logger.info("OObjectDatabasePool.global().acquire(\"{}\", \"{}\", \"****\");", dbURL,
                    user);
            password = getPassword(config);
            setConfiguredQueries(config.get(CONFIG_QUERIES));
        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid, can not start OrientDB repository", ex);
            throw ex;
        }

        try {
            pool = DBHelper.getPool(dbURL, user, password, poolMinSize, poolMaxSize, config, true);
            logger.debug("Obtained pool {}", pool);
        } catch (RuntimeException ex) {
            logger.warn("Initializing database pool failed", ex);
            throw ex;
        }
    }

    private String getDBUrl(JsonValue config) {
        File dbFolder = IdentityServer.getFileForWorkingPath("db/openidm");
        String orientDbFolder = dbFolder.getAbsolutePath();
        orientDbFolder = orientDbFolder.replace('\\', '/'); // OrientDB does not
                                                            // handle
                                                            // backslashes well
        return config.get(OrientDBRepoService.CONFIG_DB_URL).defaultTo("local:" + orientDbFolder)
                .asString();
    }

    private String getUser(JsonValue config) {
        return config.get(CONFIG_USER).defaultTo("admin").asString();
    }

    private String getPassword(JsonValue config) {
        return config.get(CONFIG_PASSWORD).defaultTo("admin").asString();
    }

    /**
     * Cleanup and close the repository
     */
    void cleanup() {
        DBHelper.closePools();
    }

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
     * @param orientClassName
     *            the OrientDB class
     * @param database
     *            a handle to the OrientDB database object. No other thread must
     *            operate on this concurrently.
     * @return The ODocument if found, null if not found.
     * @throws BadRequestException
     *             if the passed identifier or type are invalid
     */
    public ODocument getByID(final String orientClassName, final String id,
            ODatabaseDocumentTx database) throws BadRequestException {
        if (id == null) {
            throw new BadRequestException("Query by id the passed id was null.");
        } else if (orientClassName == null) {
            throw new BadRequestException("Query by id the passed type was null.");
        }

        try {
            ORecordId RID = new ORecordId(id);
            Object o = database.getRecord(RID);
            if (o instanceof ODocument) {
                return (ODocument) o;
            }
        } catch (IllegalArgumentException e) {
            logger.trace("Invalid id {}", id, e);
        } catch (ODatabaseException e) {
            logger.error("Invalid id {}", id, e);
        }

        OSQLSynchQuery<ODocument> query =
                new OSQLSynchQuery<ODocument>("select * from " + orientClassName + " where "
                        + DocumentUtil.ORIENTDB_PRIMARY_KEY + " = ? ");
        List<ODocument> result = database.query(query, id);

        ODocument first = null;
        if (result.size() > 0) {
            // ID is of type unique index, there must only be one at most
            first = result.get(0);
        }
        return first;
    }

}
