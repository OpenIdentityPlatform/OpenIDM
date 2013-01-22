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

package org.forgerock.openidm.repo.orientdb.impl;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;


import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OQueryParsingException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.query.nativ.ONativeSynchQuery;
import com.orientechnologies.orient.core.query.nativ.OQueryContextNative;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;
import com.orientechnologies.orient.graph.gremlin.OCommandGremlin;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONUtility;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jettison.json.JSONObject;
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
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.repo.orientdb.impl.query.PrepareNotSupported;
import org.forgerock.openidm.repo.orientdb.impl.query.TokenHandler;
import org.forgerock.openidm.util.ContextUtil;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.repo.QueryConstants;


import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.index.OIndexException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Vertex;


/**
 * Repository service implementation using OrientDB
 * @author aegloff
 */
@Component(name = OrientDBRepoService.PID, immediate=true, policy=ConfigurationPolicy.REQUIRE, enabled=true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Repository Service using OrientDB"),
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = "/repo/{partition}*"),
    @Property(name = "db.type", value = "OrientDB")
})
public class OrientDBRepoService implements RequestHandler{

    public static final String PID = "org.forgerock.openidm.repo.orientdb";

    /**
     * Setup logging for the {@link OrientDBRepoService}.
     */
    final static Logger logger = LoggerFactory.getLogger(OrientDBRepoService.class);

    // Monitoring event name prefix
    private static final String EVENT_RAW_QUERY_PREFIX = "openidm/internal/repo/orientdb/raw/query/";

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
       Map<String,String> variables = ContextUtil.getUriTemplateVariables(context);
       if (null != variables && variables.containsKey("partition")) {
         return variables.get("partition");
       }
        throw new ForbiddenException("Direct access without Router to this service is forbidden.");
    }


    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        final ResourceException e = new NotSupportedException("Action operations are not supported");
        handler.handleError(e);
    }


    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }


    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            String partition = getPartition(context);
            if (PARTITION_LINKS.equals(partition)) {
                //Use the Graph Database

            }  else {
                //Use the Document Database
                handler.handleResult(read(partition,request));
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
     * @param partition
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
    public Resource read(String partition, ReadRequest request) throws ResourceException {
        //Parse the remaining resourceName
        String[] resourceName = ContextUtil.parseResourceName(request.getResourceName());
        if (resourceName == null) {
            throw new BadRequestException("The repository requires clients to supply an identifier for the object to read.");
        }
        //At this point we don't know if the last segment of resourceName is meant to be id or type
        // The https://bugster.forgerock.org/jira/browse/OPENIDM-739 is not fixed at this level
        // the partition=ui but resourceName will be parsed to {"notification"} or {"notification","7"}

        //partition and optional sub-type together defines the OrientDB Document type!?
        String localType = resourceName.length > 1 ? partition + "/" + resourceName[0] : partition;
        String localId = resourceName.length > 1 ? resourceName[1] : resourceName[0];

        Resource result = null;
        ODatabaseDocumentTx db = getConnection();
        try {
            ODocument doc = getByID(localType, localId, db);
            if (doc == null) {
                throw new NotFoundException("Object " + localId + " not found in " + localType);
            }
            result = DocumentUtil.toMap(doc, request.getFieldFilters());
            logger.trace("Completed get for partition: {}, resourceName: {}, result: {}", new Object[]{partition, resourceName, result});
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return result;
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        try {
            String partition = getPartition(context);
            if (PARTITION_LINKS.equals(partition)) {
                //Use the Graph Database

                /*List<Vertex> r = embeddedServer.orientGraph.getRawGraph().command(new OCommandGremlin("g.V[1].out.out.in")).execute();

                JsonValue result = new JsonValue(new LinkedHashMap<String,Object>());

                embeddedServer.orientGraph.getEdge(1)

                for (Vertex v:r) {
                result.put(v.getId(),GraphSONUtility.);
                }
*/
                OGraphDatabase gdb = embeddedServer.getOGraph();

                Random r = new Random();

                try {
                    if (gdb. getVertexType("MyV") == null) {
                        OClass c1 = gdb.createVertexType("MyV");
                        c1.createProperty("id", OType.LONG).createIndex(OClass.INDEX_TYPE.UNIQUE);

                        OClass c2 = gdb.createEdgeType("MyE");
                        c2.createProperty("label", OType.STRING);
                    }

                    ODocument v1 = gdb.createVertex("MyV");
                    v1.field("id", System.currentTimeMillis());
                    v1.field("name", "parent");

                    ODocument v2 = gdb.createVertex("MyV");
                    int id =       r.nextInt();
                    v2.field("id", id);
                    v2.field("name", "child");

                    ODocument e1 = gdb.createEdge(v1, v2, "MyE");
                    e1.field("label", "edge");

                    v1.save();

                    OrientGraph g2 = new OrientGraph(gdb);

                    String vid =  String.format("%d:%d", v2
                            .getRecord().getIdentity().getClusterId(), v2.getRecord()
                            .getIdentity().getClusterPosition().intValue());


                    ObjectNode node = (new GraphSONUtility(GraphSONMode.EXTENDED, null, null, null)).objectNodeFromElement(g2.getVertex(vid));


                    JsonValue target = new JsonValue(node);

                    handler.handleResult(new Resource(vid,null, target));
                } catch (OException e) {
                    logger.error("OrientDB Exception: " + e.toString());
                } finally {
                    gdb.close();
                }

            }  else {
                //Use the Document Database
                handler.handleResult(create(partition, request));
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
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version (For optimistic concurrency)
     *
     * @param fullId the client-generated identifier to use, or {@code null} if server-generated identifier is requested.
     * @param obj the contents of the object to create in the object set.
     * @throws NotFoundException if the specified id could not be resolved. 
     * @throws ForbiddenException if access to the object or object set is forbidden.
     * @throws PreconditionFailedException if an object with the same ID already exists.
     */
    public Resource create(String partition, CreateRequest request) throws ResourceException {
        //Parse the remaining resourceName
        String[] resourceName = ContextUtil.parseResourceName(request.getResourceName());
        if (resourceName == null) {
            throw new BadRequestException("The repository requires clients to supply an identifier for the object to create.");
        }

        //At this point we don't know if the last segment of resourceName is meant to be
        // The https://bugster.forgerock.org/jira/browse/OPENIDM-739 is not fixed at this level
        // the partition=ui but resourceName will be {"notification"} or {"notification","7"}

        //partition and optional sub-type together defines the OrientDB Document type!?
        String localType = resourceName.length > 1 ? partition + "/" + resourceName[0] : partition;
        String localId = resourceName.length > 1 ? resourceName[1] : resourceName[0];

        //----------------
        // Use this way
        
        localType = partition + request.getResourceName();
        localId = request.getNewResourceId();
        if (StringUtils.isBlank(localId)) {
            localId = UUID.randomUUID().toString();
        }                
        String orientClassName = typeToOrientClassName(localType);
 
//        if (fullId == null || localId == null) {
//            throw new NotFoundException("The repository requires clients to supply an identifier for the object to create. Full identifier: " + fullId + " local identifier: " + localId);
//        } else if (type == null) {
//            throw new NotFoundException("The object identifier did not include sufficient information to determine the object type: " + fullId);
//        }
        
        //request.getContent().put(ServerConstants.OBJECT_PROPERTY_ID, localId);

        //TODO ODocument instances always refer to the thread-local database
        ODatabaseDocumentTx db = getConnection();
        try{
            // Rather than using MVCC for insert, rely on primary key uniqueness constraints to detect duplicate create
            ODocument newDoc = DocumentUtil.toDocument(request.getContent().asMap(), null, orientClassName);
            //TODO Fix the logging
            //logger.trace("Created doc for id: {} to save {}", fullId, newDoc);
            newDoc.save();

            //request.getContent().put(ServerConstants.OBJECT_PROPERTY_REV, Integer.toString(newDoc.getVersion()));
            if (logger.isTraceEnabled()) {
                logger.trace(
                        "Create payload for partition: {}, resourceName: {}, resourceId: {}, doc: {}",
                        new Object[] { partition, request.getResourceName(),
                            request.getNewResourceId(), newDoc });
            } else {
                logger.debug(
                        "Completed create for partition: {}, resourceName: {}, resourceId: {}, revision: {}",
                        new Object[] { partition, request.getResourceName(),
                            request.getNewResourceId(), newDoc.getVersion() });
            }
            return new Resource(localId, Integer.toString(newDoc.getVersion()),null);
        } catch (OIndexException ex) {
            // Because the OpenIDM ID is defined as unique, duplicate inserts must fail
            throw new PreconditionFailedException("Create rejected as Object with same ID already exists. " + ex.getMessage(), ex);
        } catch (com.orientechnologies.orient.core.exception.ODatabaseException ex) {
            // Because the OpenIDM ID is defined as unique, duplicate inserts must fail. 
            // OrientDB may wrap the IndexException root cause.
            if (isCauseIndexException(ex, 10)) {
                throw new PreconditionFailedException("Create rejected as Object with same ID already exists and was detected. " 
                        + ex.getMessage(), ex);
            } else {
                throw ex;
            }
        } catch (RuntimeException e){
            throw e;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        try {
            String partition = getPartition(context);
            if (PARTITION_LINKS.equals(partition)) {
                //Use the Graph Database

            }  else {
                //Use the Document Database
                handler.handleResult(update(partition, request));
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
     * This implementation requires MVCC and hence enforces that clients state what revision they expect 
     * to be updating
     * 
     * If successful, this method updates metadata properties within the passed object,
     * including: a new {@code _rev} value for the revised object's version
     *
     * @param fullId the identifier of the object to be put, or {@code null} to request a generated identifier.
     * @param rev the version of the object to update; or {@code null} if not provided.
     * @param obj the contents of the object to put in the object set.
     * @throws ConflictException if version is required but is {@code null}.
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     * @throws BadRequestException if the passed identifier is invalid
     */
    public Resource update(String partition, UpdateRequest request) throws ResourceException {
        //Parse the remaining resourceName
        String[] resourceName = ContextUtil.parseResourceName(request.getResourceName());
        if (resourceName == null) {
            throw new BadRequestException("The repository requires clients to supply an identifier for the object to update.");
        }

        //At this point we don't know if the last segment of resourceName is meant to be id or type
        // The https://bugster.forgerock.org/jira/browse/OPENIDM-739 is not fixed at this level
        // the partition=ui but resourceName will be parsed to {"notification"} or {"notification","7"}

        //partition and optional sub-type together defines the OrientDB Document type!?
        String localType = resourceName.length > 1 ? partition + "/" + resourceName[0] : partition;
        String localId = resourceName.length > 1 ? resourceName[1] : resourceName[0];

        String orientClassName = typeToOrientClassName(localType);
        
        if (StringUtils.isBlank(request.getRevision())) {
            throw new ConflictException("Object passed into update does not have revision it expects set.");
        } else {
            request.getNewContent().put(ServerConstants.OBJECT_PROPERTY_REV, request.getRevision());
        }
        //TODO http://code.google.com/p/orient/wiki/JavaMultiThreading
        ODatabaseDocumentTx db = getConnection();
        try{
            db.begin();
            ODocument existingDoc = getByID(localType, localId, db);
            if (existingDoc == null) {
                throw new NotFoundException("Update on object " + localType + "/" + localId
                        + " could not find existing object.");
            }
            ODocument updatedDoc = DocumentUtil.toDocument(request.getNewContent().asMap(), existingDoc, orientClassName);
            logger.trace("Updated doc for partition: {}, resourceName: {}, to save {}",
                    new Object[] { partition, request.getResourceName(), updatedDoc });

            updatedDoc.save();
            db.commit();

            //obj.put(DocumentUtil.TAG_REV, Integer.toString(updatedDoc.getVersion()));
            // Set ID to return to caller
            //obj.put(DocumentUtil.TAG_ID, updatedDoc.field(DocumentUtil.ORIENTDB_PRIMARY_KEY));
            //TODO Fix the logging
            //logger.debug("Committed update for id: {} revision: {}", fullId, updatedDoc.getVersion());
            //logger.trace("Update payload for id: {} doc: {}", fullId, updatedDoc);
            return new Resource(localId,Integer.toString(updatedDoc.getVersion()),null);
        } catch (OConcurrentModificationException ex) {
            db.rollback();
            throw new PreconditionFailedException("Update rejected as current Object revision is different than expected by caller, the object has changed since retrieval: " + ex.getMessage(), ex);
        } catch (RuntimeException e){
            db.rollback();
            throw e;
        } finally {
            if (db != null) {
                db.close();
            } 
        }
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        try {
            String partition = getPartition(context);
            if (PARTITION_LINKS.equals(partition)) {
                //Use the Graph Database

            }  else {
                //Use the Document Database
                handler.handleResult(delete(partition, request));
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
     * @param fullId the identifier of the object to be deleted.
     * @param rev the version of the object to delete or {@code null} if not provided.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws ConflictException if version is required but is {@code null}.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    public Resource delete(String partition, DeleteRequest request) throws ResourceException {
        //Parse the remaining resourceName
        String[] resourceName = ContextUtil.parseResourceName(request.getResourceName());
        if (resourceName == null) {
            throw new BadRequestException("The repository requires clients to supply an identifier for the object to update.");
        }

        //At this point we don't know if the last segment of resourceName is meant to be id or type
        // The https://bugster.forgerock.org/jira/browse/OPENIDM-739 is not fixed at this level
        // the partition=ui but resourceName will be parsed to {"notification"} or {"notification","7"}

        //partition and optional sub-type together defines the OrientDB Document type!?
        String localType = resourceName.length > 1 ? partition + "/" + resourceName[0] : partition;
        String localId = resourceName.length > 1 ? resourceName[1] : resourceName[0];

        String orientClassName = typeToOrientClassName(localType);

        if (StringUtils.isBlank(request.getRevision())) {
            throw new ConflictException("Object passed into delete does not have revision it expects set.");
        }

        // This throws ConflictException if parse fails
        int ver = DocumentUtil.parseVersion(request.getRevision());
        
        ODatabaseDocumentTx db = getConnection();
        try {
            db.begin();
            ODocument existingDoc = getByID(localType, localId, db);
            if (existingDoc == null) {
                throw new NotFoundException("Object does not exist for delete on: " + localType + "/" + localId);
            }
            
            existingDoc.setVersion(ver); // State the version we expect to delete for MVCC check

            db.delete(existingDoc); 
            db.commit();
            logger.debug("delete for id succeeded: {} revision: {}", localId, request.getRevision());
            return DocumentUtil.toMap(existingDoc, request.getFieldFilters());
        } catch (OConcurrentModificationException ex) {  
            db.rollback();
            throw new PreconditionFailedException("Delete rejected as current Object revision is different than expected by caller, the object has changed since retrieval.", ex);
        } catch (RuntimeException e){
            db.rollback();
            throw e;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * Performs the query on the specified object and returns the associated results.
     * <p>
     * Queries are parametric; a set of named parameters is provided as the query criteria.
     * The query result is a JSON object structure composed of basic Java types.
     *
     * The returned map is structured as follow:
     * - The top level map contains meta-data about the query, plus an entry with the actual result records.
     * - The <code>QueryConstants</code> defines the map keys, including the result records (QUERY_RESULT)
     *
     * @param fullId identifies the object to query.
     * @param params the parameters of the query to perform.
     * @return the query results, which includes meta-data and the result records in JSON object structure format.
     * @throws NotFoundException if the specified object could not be found.
     * @throws BadRequestException if the specified params contain invalid arguments, e.g. a query id that is not
     * configured, a query expression that is invalid, or missing query substitution tokens.
     * @throws ForbiddenException if access to the object or specified query is forbidden.
     */
    @Override
    public void handleQuery(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        try {
            String queryExpression = null;
            if (request.getQueryFilter() != null) {
                handler.handleError(new NotSupportedException("Query by Filter not supported"));
                return;
            } else if (StringUtils.isNotBlank(request.getQueryExpression())) {
                queryExpression = prepareQuery(request.getQueryExpression());
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
                String type = partition + "/" + request.getResourceName();
                logger.trace("Partition: {} resourceName: {}", partition, request.getResourceName());

                Map<String, String> params =
                        new HashMap<String, String>(request.getAdditionalQueryParameters());
                params.put(QueryConstants.RESOURCE_NAME, typeToOrientClassName(type));

                ODatabaseDocumentTx database = getConnection();
                try {
                    final AtomicReference<OIdentifiable> lastRecord =
                            new AtomicReference<OIdentifiable>();

                    //This blocks the thread and wait until the query is finished to fetch the last record.
                    //TODO How to submit and make is async if the DB supports it?
                    OSQLAsynchQuery<ODocument> query =
                            new OSQLAsynchQuery(TokenHandler.replaceTokensWithValues(
                                    queryExpression, params)) {
                                @Override
                                public List run(Object... iArgs) {
                                    super.run(iArgs);
                                    final OIdentifiable i = lastRecord.get();

                                    if (null != i) {
                                        final ORID lastRid = i.getIdentity();
                                        handler.handleResult(new QueryResult(lastRid.next(), -1));
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
                                    DocumentUtil.toMap((ODocument) iRecord, request
                                            .getFieldFilters());
                            boolean accepted = handler.handleResource(r);
                            if (accepted) {
                                lastRecord.set((OIdentifiable) iRecord);
                            }
                            return accepted;
                        }
                    });
                    database.command(query).execute(params);
                } catch (OQueryParsingException firstTryEx) {
                    // TODO: consider differentiating between bad configuration
                    // and bad request
                    handler.handleError( new BadRequestException("Failed to resolve and parse the query "
                            + queryExpression + " with params: " + params, firstTryEx));
                } catch (IllegalArgumentException ex) {
                    // TODO: consider differentiating between bad configuration
                    // and bad request
                    handler.handleError( new BadRequestException("Query is invalid: " + queryExpression + " "
                            + ex.getMessage(), ex));
                } catch (RuntimeException ex) {
                    logger.warn("Unexpected failure during DB query: {}", ex.getMessage());
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
     * @return A connection from the pool. Call close on the connection when done to return to the pool.
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
                    logger.info("Succeeded in acquiring connection from pool in retry attempt {}", retryCount);
                }
                retryCount = maxRetry;
            } catch (com.orientechnologies.orient.core.exception.ORecordNotFoundException ex) {
                // TODO: remove work-around once OrientDB resolves this condition
                if (retryCount == maxRetry) {
                    logger.warn("Failure reported acquiring connection from pool, retried {} times before giving up.", retryCount, ex);
                    throw new InternalServerErrorException(
                            "Failure reported acquiring connection from pool, retried " + retryCount + " times before giving up: " 
                            + ex.getMessage(), ex);
                } else {
                    logger.info("Pool acquire reported failure, retrying - attempt {}", retryCount);
                    logger.trace("Pool acquire failure detail ", ex);
                    try {
                        Thread.sleep(100); // Give the DB time to complete what it's doing before retrying
                    } catch (InterruptedException iex) {
                        // ignore that sleep was interrupted
                    }
                }
            }
        }
        return db;
    }

    // TODO: replace with common utility to handle ID, this is temporary
    private String getLocalId(String id) {
        String localId = null;
        int lastSlashPos = id.lastIndexOf("/");
        if (lastSlashPos > -1) {
            localId = id.substring(id.lastIndexOf("/") + 1);
        }
        logger.trace("Full id: {} Extracted local id: {}", id, localId);
        return localId;
    }
    
    // TODO: replace with common utility to handle ID, this is temporary
    private static String getObjectType(String id) {
        String type = null;
        int lastSlashPos = id.lastIndexOf("/");
        if (lastSlashPos > -1) {
            int startPos = 0;
            // This should not be necessary as relative URI should not start with slash
            if (id.startsWith("/")) {
                startPos = 1;
            }
            type = id.substring(startPos, lastSlashPos);
            logger.trace("Full id: {} Extracted type: {}", id, type);
        }
        return type;
    }
    
    public static String typeToOrientClassName(String type) {
        return type.replace("/", "_");
    }
    
    //public static String idToOrientClassName(String id) {
    //    String type = getObjectType(id);
    //    return typeToOrientClassName(type);
    //}
    
    /**
     * Detect if the root cause of the exception is an index constraint violation
     * This is necessary as the database may wrap this root cause in further exceptions,
     * masking the underlying cause
     * @param ex The throwable to check
     * @param maxLevels the maximum level of causes to check, avoiding the cost
     * of checking recursiveness
     * @return
     */
    private boolean isCauseIndexException(Throwable ex, int maxLevels) {
        if (maxLevels > 0) {
            Throwable cause = ex.getCause();
            if (cause != null) {
                return cause instanceof OIndexException || isCauseIndexException(cause, maxLevels - 1);
            }
        }    
        return false;
    }

    /**
     * Populate and return a repository service that knows how to query and manipulate configuration.
     *
     * @param repoConfig the bootstrap configuration
     * @return the boot repository service. This instance is not managed by SCR and needs to be manually registered.
     */
    static OrientDBRepoService getRepoBootService(Map repoConfig) {
        OrientDBRepoService bootRepo = new OrientDBRepoService();
        JsonValue cfg = new JsonValue(repoConfig);
        bootRepo.init(cfg);
        return bootRepo;
    }
    
    @Activate
    void activate(ComponentContext compContext) throws Exception { 
        logger.debug("Activating Service with configuration {}", compContext.getProperties());
        
        try {
            existingConfig = JSONEnhancedConfig.newInstance().getConfigurationAsJson(compContext);
        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid and could not be parsed, can not start OrientDB repository: " 
                    + ex.getMessage(), ex);
            throw ex;
        }
        embeddedServer = new EmbeddedOServerService();
        embeddedServer.activate(existingConfig);
        
        init(existingConfig);
        
        logger.info("Repository started.");
    }
    
    /**
     * Initialize the instnace with the given configuration.
     * 
     * This can configure managed (DS/SCR) instances, as well as explicitly instantiated
     * (bootstrap) instances.
     * 
     * @param config the configuration
     */
    void init (JsonValue config) {
        try {
            dbURL = getDBUrl(config);
            logger.info("Use DB at dbURL: {}", dbURL);
            user = getUser(config);
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
        orientDbFolder = orientDbFolder.replace('\\', '/'); // OrientDB does not handle backslashes well
        return config.get(OrientDBRepoService.CONFIG_DB_URL).defaultTo("local:" + orientDbFolder).asString();
    }
    
    private String getUser(JsonValue config) {
        return config.get(CONFIG_USER).defaultTo("admin").asString();
    }
    
    private String getPassword(JsonValue config) {
        return config.get(CONFIG_PASSWORD).defaultTo("admin").asString();
    }
    
    /**
     * Handle an existing activated service getting changed; 
     * e.g. configuration changes or dependency changes
     * 
     * @param compContext THe OSGI component context
     * @throws Exception if handling the modified event failed
     */
    @Modified
    void modified(ComponentContext compContext) throws Exception {
        logger.debug("Handle repository service modified notification");
        JsonValue newConfig = null;
        try {
            newConfig = JSONEnhancedConfig.newInstance().getConfigurationAsJson(compContext);
        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid and could not be parsed, can not start OrientDB repository", ex); 
            throw ex;
        }
        if (existingConfig != null 
                && dbURL.equals(getDBUrl(newConfig))
                && user.equals(getUser(newConfig))
                && password.equals(getPassword(newConfig))) {
            // If the DB pool settings don't change keep the existing pool
            logger.info("(Re-)initialize repository with latest configuration.");
            init(newConfig);
        } else {
            // If the DB pool settings changed do a more complete re-initialization
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
        cleanup();
        if (embeddedServer != null) {
            embeddedServer.deactivate();
        }
        logger.info("Repository stopped.");
    }

    /**
     * Cleanup and close the repository
     */
    void cleanup() {
        DBHelper.closePools();
    }


    /**
     * Construct a prepared statement from the query String
     *
     * The constructed prepared query can only be verified at query execution time
     * and hence the query execution may later fall back onto non-prepared execution
     *
     * @param queryString the query expression, including tokens to replace
     * @return the constructed (but not validated) prepared statement
     */
    protected String prepareQuery(String queryString) {
        try {
            return TokenHandler.replaceTokensWithOrientToken(queryString);
        } catch (PrepareNotSupported ex) {
            // Statement not in a format that it can be converted into prepared statement
            logger.debug("Statement not in a format that it can be converted into prepared statement: {}", queryString);
        } catch (com.orientechnologies.orient.core.exception.OQueryParsingException ex) {
            // With current OrientDB impl parsing will actually only fail on first use,
            // hence unless the implementation changes this is unlikely to trigger
        }
        return queryString;
    }

    /**
     * Set the pre-configured queries, which are identified by a query identifier and can be
     * invoked using this identifier
     *
     * Success to set the queries does not mean they are valid as some can only be validated at
     * query execution time.
     *
     * @param queries the complete list of configured queries, mapping from query id to the
     * query expression which may optionally contain tokens in the form ${token-name}.
     * @throws JsonValueException if the {@code queries} is not {@code Map}
     */
    protected void setConfiguredQueries(JsonValue queries) {
        Map<String, String> prepQueries = new HashMap<String, String>();

        //TODO Should we allow to overwrite this?
        // Query all IDs is a mandatory query, default it and allow override.
        prepQueries.put(QueryConstants.QUERY_ALL_IDS, prepareQuery("select _openidm_id from ${unquoted:_resource}"));

        // Populate/Override with Queries configured
        if (queries != null && !queries.isNull()) {
            for (Map.Entry<String, Object> entry : queries.asMap().entrySet()) {
                if (entry.getValue() instanceof String) {
                    prepQueries.put(entry.getKey(), prepareQuery((String) entry.getValue()));
                }
            }
        }
        predefinedQueries = Collections.unmodifiableMap(prepQueries);
    }

    /**
     * Query by primary key, the OpenIDM identifier. This identifier is different from the OrientDB internal record id.
     *
     * @param id the OpenIDM identifier for an object
     * @param type the OrientDB class
     * @param database a handle to the OrientDB database object. No other thread must operate on this concurrently.
     * @return The ODocument if found, null if not found.
     * @throws BadRequestException if the passed identifier or type are invalid
     */
    public ODocument getByID(final String type, final String id,  ODatabaseDocumentTx database) throws BadRequestException {
        String orientClassName = OrientDBRepoService.typeToOrientClassName(type);
        if (id == null) {
            throw new BadRequestException("Query by id the passed id was null.");
        } else if (type == null) {
            throw new BadRequestException("Query by id the passed type was null.");
        }


//        OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(
//                "select * from " + orientClassName + " where " + DocumentUtil.ORIENTDB_PRIMARY_KEY + " = ? ");
//        List<ODocument> result = database.query(query,id);

        List<ODocument> result =
                new ONativeSynchQuery<OQueryContextNative>(database, orientClassName,
                        new OQueryContextNative()) {

                    @Override
                    public boolean filter(OQueryContextNative iRecord) {
                        //TODO return false after the first match
                        //TODO This should be use only locally
                        return iRecord.field(DocumentUtil.ORIENTDB_PRIMARY_KEY).eq(id).go() ;
                    };

                }.execute();
        ODocument first = null;
        if (result.size() > 0) {
            // ID is of type unique index, there must only be one at most
            first = result.get(0);
        }
        return first;
    }



}
