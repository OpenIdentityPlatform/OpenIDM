/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.PreconditionFailedException;
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

/**
 * Repository service implementation using OrientDB
 * @author aegloff
 */
@Component(name = "org.forgerock.openidm.repo.orientdb", immediate=true, policy=ConfigurationPolicy.REQUIRE, enabled=true)
@Service (value = {RepositoryService.class, ObjectSet.class}) // Omit the RepoBootService interface from the managed service
@Properties({
    @Property(name = "service.description", value = "Repository Service using OrientDB"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
    @Property(name = "openidm.router.prefix", value = "repo"),
    @Property(name = "db.type", value = "OrientDB")
})
public class OrientDBRepoService implements RepositoryService, RepoBootService {
    final static Logger logger = LoggerFactory.getLogger(OrientDBRepoService.class);

    // Keys in the JSON configuration
    public static final String CONFIG_QUERIES = "queries";
    public static final String CONFIG_DB_URL = "dbUrl";
    public static final String CONFIG_USER = "user";
    public static final String CONFIG_PASSWORD = "password";
    public static final String CONFIG_DB_STRUCTURE = "dbStructure";
    public static final String CONFIG_ORIENTDB_CLASS = "orientdbClass";
    public static final String CONFIG_INDEX = "index";
    public static final String CONFIG_PROPERTY_NAME = "propertyName";
    public static final String CONFIG_PROPERTY_TYPE = "propertyType";
    public static final String CONFIG_INDEX_TYPE = "indexType";
    
    ODatabaseDocumentPool pool;

    String dbURL; 
    String user;
    String password;
    int poolMinSize = 5; 
    int poolMaxSize = 20;

    // TODO: evaluate use of Guice instead
    PredefinedQueries predefinedQueries = new PredefinedQueries();
    Queries queries = new Queries();
    EnhancedConfig enhancedConfig = new JSONEnhancedConfig();

    EmbeddedOServerService embeddedServer;
    
    
    /**
     * Gets an object from the repository by identifier. The returned object is not validated 
     * against the current schema and may need processing to conform to an updated schema.
     * <p>
     * The object will contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} to enable optimistic concurrency supported by OrientDB and OpenIDM.
     *
     * @param id the identifier of the object to retrieve from the object set.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws BadRequestException if the passed identifier is invalid
     * @return the requested object.
     */
    @Override
    public Map<String, Object> read(String fullId) throws ObjectSetException {
        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);
        
        if (fullId == null || localId == null) {
            throw new NotFoundException("The repository requires clients to supply an identifier for the object to create. Full identifier: " + fullId + " local identifier: " + localId);
        } else if (type == null) {
            throw new NotFoundException("The object identifier did not include sufficient information to determine the object type: " + fullId);
        }
        
        Map<String, Object> result = null;
        ODatabaseDocumentTx db = getConnection();
        try {
            ODocument doc = predefinedQueries.getByID(localId, type, db);
            if (doc == null) {
                throw new NotFoundException("Object " + fullId + " not found in " + type);
            }
            result = DocumentUtil.toMap(doc);
            logger.trace("Completed get for id: {} result: {}", fullId, result);        
        } finally {
            if (db != null) {
                db.close();
            }
        }

        return result;
    }

    /**
     * Creates a new object in the object set.
     * <p>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version (For optimistic concurrency)
     *
     * @param id the client-generated identifier to use, or {@code null} if server-generated identifier is requested.
     * @param object the contents of the object to create in the object set.
     * @throws NotFoundException if the specified id could not be resolved. 
     * @throws ForbiddenException if access to the object or object set is forbidden.
     * @throws PreconditionFailedException if an object with the same ID already exists.
     */
    @Override
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);
        String orientClassName = typeToOrientClassName(type);
 
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
     * @param id the identifier of the object to be put, or {@code null} to request a generated identifier.
     * @param rev the version of the object to update; or {@code null} if not provided.
     * @param object the contents of the object to put in the object set.
     * @throws ConflictException if version is required but is {@code null}.
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     * @throws BadRequestException if the passed identifier is invalid
     */
    @Override
    public void update(String fullId, String rev, Map<String, Object> obj) throws ObjectSetException {
        
        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);
        String orientClassName = typeToOrientClassName(type);
        
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
            logger.debug("Committed update for id: {} revision: {}", fullId, updatedDoc.getVersion());
            logger.trace("Update payload for id: {} doc: {}", fullId, updatedDoc);
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
     * @param id the identifier of the object to be deleted.
     * @param rev the version of the object to delete or {@code null} if not provided.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws ConflictException if version is required but is {@code null}.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    @Override
    public void delete(String fullId, String rev) throws ObjectSetException {
        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);

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
     * Currently not supported by this implementation.
     * 
     * Applies a patch (partial change) to the specified object in the object set.
     *
     * @param id the identifier of the object to be patched.
     * @param rev the version of the object to patch or {@code null} if not provided.
     * @param patch the partial change to apply to the object.
     * @throws ConflictException if patch could not be applied object state or if version is required.
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new UnsupportedOperationException();
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
     * @param id identifies the object to query.
     * @param params the parameters of the query to perform.
     * @return the query results, which includes meta-data and the result records in JSON object structure format.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws BadRequestException if the specified params contain invalid arguments, e.g. a query id that is not
     * configured, a query expression that is invalid, or missing query substitution tokens.
     * @throws ForbiddenException if access to the object or specified query is forbidden.
     */
    @Override
    public Map<String, Object> query(String fullId, Map<String, Object> params) throws ObjectSetException {
        // TODO: replace with common utility
        String type = fullId; 
        // This should not be necessary as relative URI should not start with slash
        if (fullId != null && fullId.startsWith("/")) {
            type = fullId.substring(1);
        }
        logger.trace("Full id: {} Extracted type: {}", fullId, type);
        
        Map<String, Object> result = new HashMap<String, Object>();
        ODatabaseDocumentTx db = getConnection();
        try {
            List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
            result.put(QueryConstants.QUERY_RESULT, docs);
            long start = System.currentTimeMillis();
            List<ODocument> queryResult = queries.query(type, params, db); 
            long end = System.currentTimeMillis();
            if (queryResult != null) {
                long convStart = System.currentTimeMillis();
                for (ODocument entry : queryResult) {
                    Map<String, Object> convertedEntry = DocumentUtil.toMap(entry);
                    docs.add(convertedEntry);
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
        } finally {
            if (db != null) {
                db.close();
            }
        }
        
        return result;
    }

    @Override
    public Map<String, Object> action(String id, Map<String, Object> params) throws ObjectSetException {
        throw new UnsupportedOperationException();
    }
    
    /**
     * @return A connection from the pool. Call close on the connection when done to return to the pool.
     */
    ODatabaseDocumentTx getConnection() throws InternalServerErrorException {
        ODatabaseDocumentTx db = null;
        int maxRetry = 100; // give it up to approx 10 seconds to recover
        int retryCount = 0;
        
        while (db == null && retryCount < maxRetry) {
            retryCount++;
            try {
                db = pool.acquire(dbURL, user, password);
                retryCount = maxRetry;
            } catch (com.orientechnologies.orient.core.exception.ORecordNotFoundException ex) {
                // TODO: remove work-around once OrientDB resolves this condition
                if (retryCount == maxRetry) {
                    logger.warn("Failure reported acquiring connection from pool, retried {} times before giving up.", retryCount, ex);
                    throw new InternalServerErrorException(
                            "Failure reported acquiring connection from pool, retried " + retryCount + " times before giving up: " 
                            + ex.getMessage(), ex);
                } else {
                    logger.info("Pool acquire reported failure, retrying");
                    logger.debug("Pool acquire failure detail ", ex);
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
     */
    private boolean isCauseIndexException(Throwable ex, int maxLevels) {
        if (maxLevels > 0) {
            Throwable cause = ex.getCause();
            if (cause != null) { 
                if (cause instanceof OIndexException) {
                    return true;
                } else {
                    return isCauseIndexException(cause, maxLevels-1);
                }
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
    static RepoBootService getRepoBootService(Map repoConfig) {
        OrientDBRepoService bootRepo = new OrientDBRepoService();
        JsonNode cfg = new JsonNode(repoConfig);
        bootRepo.init(cfg);
        return bootRepo;
    }
    
    @Activate
    void activate(ComponentContext compContext) throws Exception { 
        logger.debug("Activating Service with configuration {}", compContext.getProperties());

        JsonNode config = null;
        try {
            config = enhancedConfig.getConfigurationAsJson(compContext);
        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid and could not be parsed, can not start OrientDB repository: " 
                    + ex.getMessage(), ex);
            throw ex;
        }
        embeddedServer = new EmbeddedOServerService();
        embeddedServer.activate(config);
        
        init(config);
        
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
    void init (JsonNode config) {
        try {
            dbURL = config.get(CONFIG_DB_URL).defaultTo("local:./db/openidm").asString();
            user = config.get(CONFIG_USER).defaultTo("admin").asString();
            password = config.get(CONFIG_PASSWORD).defaultTo("admin").asString();

            Map map = config.get(CONFIG_QUERIES).asMap();
            Map<String, String> queryMap = (Map<String, String>) map;
            queries.setConfiguredQueries(queryMap);
        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid, can not start OrientDB repository: " 
                    + ex.getMessage(), ex);
            throw ex;
        }

        try {
            pool = DBHelper.initPool(dbURL, user, password, poolMinSize, poolMaxSize, config);
        } catch (RuntimeException ex) {
            logger.warn("Initializing database pool failed: " + ex.getMessage(), ex);
            throw ex;
        }
    }

    /* Currently rely on deactivate/activate to be called by DS if config changes instead
    @Modified
    void modified(ComponentContext compContext) {
        logger.info("Configuration of repository changed.");
        deactivate(compContext);
        activate(compContext);
    }
    */

    
    @Deactivate
    void deactivate(ComponentContext compContext) { 
        logger.debug("Deactivating Service {}", compContext);
        if (pool != null) {
            try {
                pool.close();
            } catch (Exception ex) {
                logger.warn("Closing pool reported exception ", ex);
            }
        }
        if (embeddedServer != null) {
            embeddedServer.deactivate();
        }
        logger.info("Repository stopped.");
    }
}
