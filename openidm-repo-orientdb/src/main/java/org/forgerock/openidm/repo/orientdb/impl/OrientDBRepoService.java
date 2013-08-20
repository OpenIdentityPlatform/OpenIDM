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
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.repo.RepoBootService;
import org.forgerock.openidm.repo.RepositoryService; 
import org.forgerock.openidm.repo.orientdb.impl.query.PredefinedQueries;
import org.forgerock.openidm.repo.orientdb.impl.query.Queries;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.index.OIndexException;
import com.orientechnologies.orient.core.record.impl.ODocument;

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
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.PropertyUtil;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.util.ResourceUtil;
/**
 * Repository service implementation using OrientDB
 * @author aegloff
 */
@Component(name = OrientDBRepoService.PID, immediate=true, policy=ConfigurationPolicy.REQUIRE, enabled=true)
@Service (value = {RepositoryService.class, RequestHandler.class}) // Omit the RepoBootService interface from the managed service
@Properties({
    @Property(name = "service.description", value = "Repository Service using OrientDB"),
    @Property(name = "service.vendor", value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/repo/*") }) // "/repo/{partition}*") })
public class OrientDBRepoService implements RequestHandler, RepositoryService, RepoBootService {

    final static Logger logger = LoggerFactory.getLogger(OrientDBRepoService.class);
    public static final String PID = "org.forgerock.openidm.repo.orientdb";
    
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
    
    ODatabaseDocumentPool pool;

    String dbURL; 
    String user;
    String password;
    int poolMinSize = 5; 
    int poolMaxSize = 20;

    // Current configuration
    JsonValue existingConfig;
    
    // TODO: evaluate use of Guice instead
    PredefinedQueries predefinedQueries = new PredefinedQueries();
    Queries queries = new Queries();
    EnhancedConfig enhancedConfig = new JSONEnhancedConfig();

    EmbeddedOServerService embeddedServer;
    
    @Override
    public void handleRead(final ServerContext context, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        try {
        	handleRead2(context, request, handler);
        } catch (Exception ex) {  
            handler.handleError(adapt(ex));
        }
    }

    @Override
    public void handleCreate(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
    	
        try {
        	handleCreate2(context, request, handler);
        } catch (Exception ex) {  
            handler.handleError(adapt(ex));
        }
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        try {
        	handleUpdate2(context, request, handler);
        } catch (Exception ex) {  
            handler.handleError(adapt(ex));
        }
    }


    @Override
    public void handleDelete(final ServerContext context, final DeleteRequest request,
            final ResultHandler<Resource> handler) {
        try {
        	handleDelete2(context, request, handler);
        } catch (Exception ex) {  
            handler.handleError(adapt(ex));
        }
    }
    
    @Override
    public void handleQuery(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
    	try {
        	handleQuery2(context, request, handler);
        } catch (Exception ex) {  
            handler.handleError(adapt(ex));
        }
    }
    
    /**
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
    //@Override
    public void handleRead2(final ServerContext context, final ReadRequest request,
            final ResultHandler<Resource> handler) throws ResourceException {
        Resource result = read(request);
        handler.handleResult(result);
    }
    
    public Resource read(ReadRequest request) throws ResourceException {
    	String fullId = request.getResourceName();
    	String localId = getLocalId(fullId);
        String type = getObjectType(fullId);
        
        if (fullId == null || localId == null) {
            throw new NotFoundException("The repository requires clients to supply an identifier for the object to create. Full identifier: " + fullId + " local identifier: " + localId);
        } else if (type == null) {
            throw new NotFoundException("The object identifier did not include sufficient information to determine the object type: " + fullId);
        }
        
        Resource result = null;
        ODatabaseDocumentTx db = getConnection();
        try {
            ODocument doc = predefinedQueries.getByID(localId, type, db);
            if (doc == null) {
                throw new NotFoundException("Object " + fullId + " not found in " + type);
            }
            result = DocumentUtil.toResource(doc);
            logger.trace("Completed get for id: {} result: {}", fullId, result); 
            return result;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * Creates a new object in the object set.
     * <p>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version (For optimistic concurrency)
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
    //@Override
    public void handleCreate2(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) throws ResourceException {
        Resource result = create(request);
        handler.handleResult(result);
    }
    
    public Resource create(CreateRequest request) throws ResourceException {
    	String localId = request.getNewResourceId();//getLocalId(fullId);
        // TODO: should CREST support server side generation of ID itself?
        if (localId == null) {
        	localId = UUID.randomUUID().toString(); // Generate ID server side.
        }
        String type = stripSlash(request.getResourceName()); //getObjectType(fullId);
        // Used currently for logging
        String fullId = request.getResourceName() + localId;
        
        String orientClassName = typeToOrientClassName(type);
        JsonValue obj = request.getContent();
 
        if (fullId == null || localId == null) {
            throw new NotFoundException("The repository requires clients to supply an identifier for the object to create. Full identifier: " + fullId + " local identifier: " + localId);
        } else if (type == null) {
            throw new NotFoundException("The object identifier did not include sufficient information to determine the object type: " + fullId);
        }
        
        obj.put(DocumentUtil.TAG_ID, localId);
        
        ODatabaseDocumentTx db = getConnection();
        try{
            // Rather than using MVCC for insert, rely on primary key uniqueness constraints to detect duplicate create
            ODocument newDoc = DocumentUtil.toDocument(obj, null, db, orientClassName);
            logger.trace("Created doc for id: {} to save {}", fullId, newDoc);
            newDoc.save();
            
            obj.put(DocumentUtil.TAG_REV, Integer.toString(newDoc.getVersion()));
            logger.debug("Completed create for id: {} revision: {}", fullId, newDoc.getVersion());
            logger.trace("Create payload for id: {} doc: {}", fullId, newDoc);
            return new Resource(obj.get(DocumentUtil.TAG_ID).asString(), 
    				obj.get(DocumentUtil.TAG_REV).asString(), obj);
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
    //@Override
    public void handleUpdate2(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) throws ResourceException {
        Resource result = update(request);
        handler.handleResult(result);
    }
    
    public Resource update(UpdateRequest request) throws ResourceException {
    	String fullId = request.getResourceName();
        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);
        String orientClassName = typeToOrientClassName(type);
        JsonValue obj = request.getNewContent();
        String rev = request.getRevision();
        
        if (rev == null) {
            throw new ConflictException("Object passed into update does not have revision it expects set.");
        } else {
            obj.put(DocumentUtil.TAG_REV, rev);
        }
        
        ODatabaseDocumentTx db = getConnection();
        try{
            db.begin();
            ODocument existingDoc = predefinedQueries.getByID(localId, type, db);
            if (existingDoc == null) {
                throw new NotFoundException("Update on object " + fullId + " could not find existing object.");
            }
            ODocument updatedDoc = DocumentUtil.toDocument(obj, existingDoc, db, orientClassName);
            logger.trace("Updated doc for id {} to save {}", fullId, updatedDoc);
            
            updatedDoc.save();
            db.commit();

            obj.put(DocumentUtil.TAG_REV, Integer.toString(updatedDoc.getVersion()));
            // Set ID to return to caller
            obj.put(DocumentUtil.TAG_ID, updatedDoc.field(DocumentUtil.ORIENTDB_PRIMARY_KEY));
            logger.debug("Committed update for id: {} revision: {}", fullId, updatedDoc.getVersion());
            logger.trace("Update payload for id: {} doc: {}", fullId, updatedDoc);
            return new Resource(obj.get(DocumentUtil.TAG_ID).asString(), 
    				obj.get(DocumentUtil.TAG_REV).asString(), obj);
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
    //@Override
    public void handleDelete2(final ServerContext context, final DeleteRequest request,
            final ResultHandler<Resource> handler) throws ResourceException {
        Resource result = delete(request);
        handler.handleResult(result);
    }
    
    public Resource delete(DeleteRequest request) throws ResourceException {
    	String fullId = request.getResourceName();
        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);
        String rev = request.getRevision();

        if (rev == null) {
            throw new ConflictException("Object passed into delete does not have revision it expects set.");
        } 
        
        int ver = DocumentUtil.parseVersion(rev); // This throws ConflictException if parse fails
        
        ODatabaseDocumentTx db = getConnection();
        try {
            db.begin();
            ODocument existingDoc = predefinedQueries.getByID(localId, type, db);
            if (existingDoc == null) {
                throw new NotFoundException("Object does not exist for delete on: " + fullId);
            }
            
            existingDoc.setVersion(ver); // State the version we expect to delete for MVCC check

            db.delete(existingDoc); 
            db.commit();
            logger.debug("delete for id succeeded: {} revision: {}", localId, rev);
            return new Resource(localId, null, new JsonValue(null));
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
    
    @Override
    public void handlePatch(final ServerContext context, final PatchRequest request,
            final ResultHandler<Resource> handler) {
    	// TODO: impl
            handler.handleError(new NotSupportedException("Patch not supported yet"));
    }
    
    @Override
    public void handleAction(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException("Action not supported"));
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
    //@Override
    public void handleQuery2(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) throws ResourceException {
        List<Resource> results = query(request);
        for (Resource result : results) {
        	handler.handleResource(result);
        }
        handler.handleResult(new QueryResult());        
    }
    
    public List<Resource> query(QueryRequest request) throws ResourceException {
    	List<Resource> results = new ArrayList<Resource>();
    	// TODO: replace with common utility
    	String fullId = request.getResourceName(); 
        String type = fullId;
        // Whilst the URI starts with a slash, but consider relative URI
        if (fullId != null && fullId.startsWith("/")) {
            type = fullId.substring(1);
        }
        logger.trace("Full id: {} Extracted type: {}", fullId, type);
        
        // TODO: Statistics is not returned in result anymore
        // TODO: result is not needed in map form anymore
        Map<String, Object> result = new HashMap<String, Object>();
        ODatabaseDocumentTx db = getConnection();
        try {
            //List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
            //result.put(QueryConstants.QUERY_RESULT, docs);
            long start = System.currentTimeMillis();
            List<ODocument> queryResult = queries.query(type, request, db); 
            long end = System.currentTimeMillis();
            if (queryResult != null) {
                long convStart = System.currentTimeMillis();
                for (ODocument entry : queryResult) {
                    Map<String, Object> convertedEntry = DocumentUtil.toMap(entry);
                    //docs.add(convertedEntry);
                    results.add(new Resource(
                    		(String) convertedEntry.get(DocumentUtil.TAG_ID), 
                    	    (String) convertedEntry.get(DocumentUtil.TAG_REV), 
                    	    new JsonValue(convertedEntry)));
                }
                long convEnd = System.currentTimeMillis();
                result.put(QueryConstants.STATISTICS_CONVERSION_TIME, Long.valueOf(convEnd-convStart));
            }
            result.put(QueryConstants.STATISTICS_QUERY_TIME, Long.valueOf(end-start));
            
            if (logger.isDebugEnabled()) {
                logger.debug("Query result contains {} records, took {} ms and took {} ms to convert result.",
                        new Object[] {((List) result.get(QueryConstants.QUERY_RESULT)).size(),
                        result.get(QueryConstants.STATISTICS_QUERY_TIME),
                        result.get(QueryConstants.STATISTICS_CONVERSION_TIME)});
            }
            return results;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
    
    /**
     * @return A connection from the pool. Call close on the connection when done to return to the pool.
     * @throws org.forgerock.openidm.objset.InternalServerErrorException
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
    
    private static String stripSlash(String id) {
        String type = null;

        int startPos = 0;
        // This should not be necessary as relative URI should not start with slash
        if (id.startsWith("/")) {
            startPos = 1;
        }
        type = id.substring(startPos);
        logger.info("Full id: {}, extracted type: {}", id, type);

        return type;
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
            existingConfig = enhancedConfig.getConfigurationAsJson(compContext);
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

            Map map = config.get(CONFIG_QUERIES).asMap();
            Map<String, String> queryMap = (Map<String, String>) map;
            queries.setConfiguredQueries(queryMap);
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
            newConfig = enhancedConfig.getConfigurationAsJson(compContext);
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
}
