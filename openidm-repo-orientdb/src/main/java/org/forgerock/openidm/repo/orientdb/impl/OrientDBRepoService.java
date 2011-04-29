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

import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.repo.QueryConstants;
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
@Component(name = "org.forgerock.repo.orientdb", immediate=true)
@Service(value = RepositoryService.class) 
@Properties({
    @Property(name = "service.description", value = "Repository Service using OrientDB"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
    @Property(name = "db.type", value = "OrientDB")
})
public class OrientDBRepoService implements RepositoryService {
    final static Logger logger = LoggerFactory.getLogger(OrientDBRepoService.class);

    // Keys in the JSON configuration
    final static String CONFIG_QUERIES = "queries";
    
    ODatabaseDocumentPool pool;

    // TODO make configurable
    String dbURL = "local:./db/openidm"; 
    String user = "admin";
    String password = "admin";
    int poolMinSize = 5; 
    int poolMaxSize = 20;

    // TODO: evaluate use of Guice instead
    PredefinedQueries predefinedQueries = new PredefinedQueries();
    Queries queries = new Queries();
    EnhancedConfig enhancedConfig = new JSONEnhancedConfig();
    
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
    public Map<String, Object> read(String fullId) throws ObjectSetException {
        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);
        
        if (fullId == null || localId == null) {
            throw new NotFoundException("The repository requires clients to supply an identifier for the object to create. Full identifier: " + fullId + " local identifier: " + localId);
        } else if (type == null) {
            throw new NotFoundException("The object identifier did not include sufficient information to determine the object type: " + fullId);
        }
        
        Map<String, Object> result = null;
        ODatabaseDocumentTx db = pool.acquire(dbURL, user, password);
        try {
            ODocument doc = predefinedQueries.getByID(localId, type, db);
            if (doc == null) {
                throw new NotFoundException("Object " + fullId + " not found in " + type);
            }
            result = DocumentUtil.toMap(doc);
            logger.trace("get for id: {} result: {}", fullId, result);        
        } finally {
            if (db != null) {
                db.close();
                pool.release(db);
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
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);
 
        if (fullId == null || localId == null) {
            throw new NotFoundException("The repository requires clients to supply an identifier for the object to create. Full identifier: " + fullId + " local identifier: " + localId);
        } else if (type == null) {
            throw new NotFoundException("The object identifier did not include sufficient information to determine the object type: " + fullId);
        }
        
        obj.put(DocumentUtil.TAG_ID, localId);
        
        ODatabaseDocumentTx db = pool.acquire(dbURL, user, password);
        try{
            // Rather than using MVCC for insert, rely on primary key uniqueness constraints to detect duplicate create
            ODocument newDoc = DocumentUtil.toDocument(obj, null, db, type);
            logger.trace("Created doc for id: {} to save {}", fullId, newDoc);
            newDoc.save();
            obj.put(DocumentUtil.TAG_REV, Integer.toString(newDoc.getVersion()));
            logger.debug("create for id: {} doc: {}", fullId, newDoc);
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
                pool.release(db);
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
    public void update(String fullId, String rev, Map<String, Object> obj) throws ObjectSetException {
        
        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);
        
        if (rev == null) {
            throw new ConflictException("Object passed into update does not have revision it expects set.");
        } else {
            obj.put(DocumentUtil.TAG_REV, rev);
        }
        
        ODatabaseDocumentTx db = pool.acquire(dbURL, user, password);
        try{
            db.begin();
            ODocument existingDoc = predefinedQueries.getByID(localId, type, db);
            if (existingDoc == null) {
                throw new NotFoundException("Update on object " + fullId + " could not find existing object.");
            }
            ODocument updatedDoc = DocumentUtil.toDocument(obj, existingDoc, db, type);
            logger.trace("Updated doc for id {} to save {}", fullId, updatedDoc);
            updatedDoc.save();
            db.commit();

            obj.put(DocumentUtil.TAG_REV, Integer.toString(updatedDoc.getVersion()));
            logger.debug("update for id: {} saved doc: {}", fullId, updatedDoc);
        } catch (OConcurrentModificationException ex) {
            db.rollback();
            throw new PreconditionFailedException("Update rejected as current Object revision is different than expected by caller, the object has changed since retrieval: " + ex.getMessage(), ex);
        } catch (RuntimeException e){
            db.rollback();
            throw e;
        } finally {
            if (db != null) {
                db.close();
                pool.release(db);
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
    public void delete(String fullId, String rev) throws ObjectSetException {
        String localId = getLocalId(fullId);
        String type = getObjectType(fullId);

        if (rev == null) {
            throw new ConflictException("Object passed into delete does not have revision it expects set.");
        } 
        
        int ver = DocumentUtil.parseVersion(rev); // This throws ConflictException if parse fails
        
        ODatabaseDocumentTx db = pool.acquire(dbURL, user, password);
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
                pool.release(db);
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
    public Map<String, Object> query(String fullId, Map<String, Object> params) throws ObjectSetException {
        // TODO: replace with common utility
        String type = fullId; 
        if (fullId != null && fullId.startsWith("/")) {
            type = fullId.substring(1);
            logger.trace("Full id: {} Extracted type: {}", fullId, type);
        }
        
        Map<String, Object> result = new HashMap<String, Object>();
        ODatabaseDocumentTx db = pool.acquire(dbURL, user, password);
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
                        new Object[] {((Map) result.get(QueryConstants.QUERY_RESULT)).size(),
                        result.get(QueryConstants.STATISTICS_QUERY_TIME),
                        result.get(QueryConstants.STATISTICS_CONVERSION_TIME)});
            }
        } finally {
            if (db != null) {
                db.close();
                pool.release(db);
            }
        }
        
        return result;
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
    private String getObjectType(String id) {
        String type = null;
        int lastSlashPos = id.lastIndexOf("/");
        if (lastSlashPos > -1 && id.startsWith("/")) {
            int secondLastSlashPos = id.lastIndexOf("/", lastSlashPos - 1);
            type = id.substring(1, lastSlashPos);
            logger.trace("Full id: {} Extracted type: {}", id, type);
        }
        return type;
    }
    
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

    @Activate
    void activate(ComponentContext compContext) { 
        logger.debug("Activating Service with configuration {}", compContext);
        
        Map<String, Object> config = enhancedConfig.getConfiguration(compContext);
        Map<String, String> queryMap = (Map<String, String>) config.get(CONFIG_QUERIES);
        queries.setConfiguredQueries(queryMap);

        try {
            pool = DBHelper.initPool(dbURL, user, password, poolMinSize, poolMaxSize);
            logger.info("Repository started.");
        } catch (RuntimeException ex) {
            logger.warn("Initializing database pool failed", ex);
            throw ex;
        } 
    }

    /* Currently rely on deactivate/activate to be called by DS if config changes instead
    //@Modified
    void modified(ComponentContext compContext) {
        logger.trace("Modifying service {}", compContext);
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
        logger.info("Repository stopped.");
    }
}