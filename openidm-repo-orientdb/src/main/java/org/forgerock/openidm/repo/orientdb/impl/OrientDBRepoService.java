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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.ConnectionFactory;
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
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.repo.RepoBootService;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.repo.orientdb.impl.query.PredefinedQueries;
import org.forgerock.openidm.repo.orientdb.impl.query.Queries;
import org.forgerock.openidm.router.RouteService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.index.OIndexException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;

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
    
    public static final String ACTION_UPDATE_CREDENTIALS = "updateDbCredentials";

    /** The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    protected ConnectionFactory connectionFactory;

    ODatabaseDocumentPool pool;

    String dbURL; 
    String user;
    String password;
    int poolMinSize = 5; 
    int poolMaxSize = 20;
    
    // Used to synchronize operations on the DB that require user/password credentials
    private static Object dbLock = new Object();

    private static OrientDBRepoService bootRepo = null;

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
        if (request.getResourceNameObject().size() < 2) {
            throw new NotFoundException("The object identifier did not include sufficient information to determine the object type and identifier of the object to read: " + request.getResourceName());
        }

        final String type = request.getResourceNameObject().parent().toString();
        final String localId = request.getResourceNameObject().leaf();
        Resource result = null;
        ODatabaseDocumentTx db = getConnection();
        try {
            ODocument doc = predefinedQueries.getByID(localId, type, db);
            if (doc == null) {
                throw new NotFoundException("Object " + localId + " not found in " + type);
            }
            result = DocumentUtil.toResource(doc);
            logger.trace("Completed get for id: {} result: {}", request.getResourceName(), result);
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
        if (request.getResourceNameObject().isEmpty()) {
            throw new NotFoundException("The object identifier did not include sufficient information to determine the object type: " + request.getResourceName());
        }

        final String type = request.getResourceName();
        // TODO: should CREST support server side generation of ID itself?
        final String localId = (request.getNewResourceId() == null || "".equals(request.getNewResourceId()))
                ? UUID.randomUUID().toString() // Generate ID server side.
                : request.getNewResourceId();

        // Used currently for logging
        String fullId = request.getResourceName() + "/" + localId;

        String orientClassName = typeToOrientClassName(type);
        JsonValue obj = request.getContent();
 
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
            return new Resource(obj.get(DocumentUtil.TAG_ID).asString(), obj.get(DocumentUtil.TAG_REV).asString(),  obj);
        } catch (ORecordDuplicatedException ex) {
            // Because the OpenIDM ID is defined as unique, duplicate inserts must fail
            throw new PreconditionFailedException("Create rejected as Object with same ID already exists. " + ex.getMessage(), ex);
        } catch (OIndexException ex) {
            // Because the OpenIDM ID is defined as unique, duplicate inserts must fail
            throw new PreconditionFailedException("Create rejected as Object with same ID already exists. " + ex.getMessage(), ex);
        } catch (ODatabaseException ex) {
            // Because the OpenIDM ID is defined as unique, duplicate inserts must fail. 
            // OrientDB may wrap the IndexException root cause.
            if (isCauseIndexException(ex, 10) || isCauseRecordDuplicatedException(ex, 10)) {
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
     * This implementation does not require MVCC and uses the current revision if no revision
     * is specified in the request.
     * <p>
     * If successful, this method updates metadata properties within the passed object,
     * including: a new {@code _rev} value for the revised object's version
     *
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
        if (request.getResourceNameObject().size() < 2) {
            throw new NotFoundException("The object identifier did not include sufficient information to determine the object type and identifier of the object to update: " + request.getResourceName());
        }
        if (StringUtils.isBlank(request.getRevision())) {
            throw new ConflictException("The repository requires clients to supply a revision for the object to update.");
        }

        final String type = request.getResourceNameObject().parent().toString();
        final String localId = request.getResourceNameObject().leaf();

        String orientClassName = typeToOrientClassName(type);
        JsonValue obj = request.getContent();

        if (request.getRevision() != null && !"".equals(request.getRevision())) {
            obj.put(DocumentUtil.TAG_REV, request.getRevision());
        }

        ODatabaseDocumentTx db = getConnection();
        try{
            ODocument existingDoc = predefinedQueries.getByID(localId, type, db);
            if (existingDoc == null) {
                throw new NotFoundException("Update on object " + request.getResourceName() + " could not find existing object.");
            }
            ODocument updatedDoc = DocumentUtil.toDocument(obj, existingDoc, db, orientClassName);
            logger.trace("Updated doc for id {} to save {}", request.getResourceName(), updatedDoc);
            
            updatedDoc.save();

            obj.put(DocumentUtil.TAG_REV, Integer.toString(updatedDoc.getVersion()));
            // Set ID to return to caller
            obj.put(DocumentUtil.TAG_ID, updatedDoc.field(DocumentUtil.ORIENTDB_PRIMARY_KEY));
            logger.debug("Committed update for id: {} revision: {}", request.getResourceName(), updatedDoc.getVersion());
            logger.trace("Update payload for id: {} doc: {}", request.getResourceName(), updatedDoc);
            return new Resource(obj.get(DocumentUtil.TAG_ID).asString(), 
                    obj.get(DocumentUtil.TAG_REV).asString(), obj);
        } catch (ODatabaseException ex) {
            // Without transaction the concurrent modification exception gets nested instead
            if (isCauseConcurrentModificationException(ex, 10)) {
                throw new PreconditionFailedException(
                        "Update rejected as current Object revision is different than expected by caller, the object has changed since retrieval: " 
                        + ex.getMessage(), ex);
            } else {
                throw ex;
            }
        } catch (OConcurrentModificationException ex) {
            throw new PreconditionFailedException("Update rejected as current Object revision is different than expected by caller, the object has changed since retrieval: " + ex.getMessage(), ex);
        } catch (RuntimeException e){
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
        if (request.getResourceNameObject().size() < 2) {
            throw new NotFoundException("The object identifier did not include sufficient information to determine the object type and identifier of the object to update: " + request.getResourceName());
        }

        if (StringUtils.isBlank(request.getRevision())) {
            throw new ConflictException("The repository requires clients to supply a revision for the object to delete.");
        }

        final String type = request.getResourceNameObject().parent().toString();
        final String localId = request.getResourceNameObject().leaf();

        int ver = DocumentUtil.parseVersion(request.getRevision()); // This throws ConflictException if parse fails
        
        ODatabaseDocumentTx db = getConnection();
        try {
            ODocument existingDoc = predefinedQueries.getByID(localId, type, db);
            if (existingDoc == null) {
                throw new NotFoundException("Object does not exist for delete on: " + request.getResourceName());
            }
            
            existingDoc.setVersion(ver); // State the version we expect to delete for MVCC check

            db.delete(existingDoc); 
            logger.debug("delete for id succeeded: {} revision: {}", localId, request.getRevision());
            return DocumentUtil.toResource(existingDoc);
        } catch (ODatabaseException ex) {
            // Without transaction the concurrent modification exception gets nested instead
            if (isCauseConcurrentModificationException(ex, 10)) {
                throw new PreconditionFailedException(
                        "Delete rejected as current Object revision is different than expected by caller, the object has changed since retrieval. "
                        + ex.getMessage(), ex);
            } else {
                throw ex;
            }

        } catch (OConcurrentModificationException ex) {  
            throw new PreconditionFailedException(
                    "Delete rejected as current Object revision is different than expected by caller, the object has changed since retrieval."
                    + ex.getMessage(), ex);
        } catch (RuntimeException e){
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
        String action = request.getAction();
        Map<String, String> params = request.getAdditionalParameters();
        try {
            if (ACTION_UPDATE_CREDENTIALS.equals(action)) {
                String newUser = params.get("user");
                String newPassword = params.get("password");
                if (newUser == null || newPassword == null) {
                    throw new BadRequestException("Expecting 'user' and 'password' parameters");
                }
                synchronized (dbLock) {
                    DBHelper.updateDbCredentials(dbURL, user, password, newUser, newPassword);
                    JsonValue config = connectionFactory.getConnection().read(context, Requests.newReadRequest("config", PID)).getContent();
                    config.put("user", newUser);
                    config.put("password", newPassword);
                    UpdateRequest updateRequest = Requests.newUpdateRequest("config/" + PID, config);
                    connectionFactory.getConnection().update(context, updateRequest);
                    handler.handleResult(new JsonValue(params));
                }
            } else {
                handler.handleError(new BadRequestException("Unknown action: " + action));
            }
        } catch (ResourceException e) {
            handler.handleError(new BadRequestException("Error updating DB credentials: " + e.getMessage(), e));
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

        // If paged results are requested then decode the cookie in order to determine
        // the index of the first result to be returned.
        final int requestPageSize = request.getPageSize();

        // Cookie containing offset of last request
        final String pagedResultsCookie = request.getPagedResultsCookie();

        final boolean pagedResultsRequested = requestPageSize > 0;

        // index of first record (used for SKIP/OFFSET)
        final int firstResultIndex;

        if (pagedResultsRequested) {
            if (StringUtils.isNotEmpty(pagedResultsCookie)) {
                try {
                    firstResultIndex = Integer.parseInt(pagedResultsCookie);
                } catch (final NumberFormatException e) {
                    throw new BadRequestException("Invalid paged results cookie");
                }
            } else {
                firstResultIndex = Math.max(0, request.getPagedResultsOffset());
            }
        } else {
            firstResultIndex = 0;
        }

        // Once cookie is processed Queries.query() can rely on the offset.
        request.setPagedResultsOffset(firstResultIndex);

        List<Resource> results = query(request);
        for (Resource result : results) {
            handler.handleResource(result);
        }

        /*
         * Execute additional -count query if we are paging
         */

        final QueryResult result;
        final String nextCookie;
        final int remainingResults;

        if (pagedResultsRequested) {
            final String countQueryId = request.getQueryId() + "-count";

            Integer resultCount = null;

            // Get total if -count query is available
            if (queries.queryIdExists(countQueryId)) {
                QueryRequest countRequest = Requests.copyOfQueryRequest(request);
                countRequest.setQueryId(countQueryId);

                // Strip pagination parameters
                countRequest.setPageSize(0);
                countRequest.setPagedResultsOffset(0);
                countRequest.setPagedResultsCookie(null);

                List<Resource> countResult = query(countRequest);

                if (countResult != null && !countResult.isEmpty()) {
                    resultCount = countResult.get(0).getContent().get("total").asInteger();
                }
            }

            boolean unknownCount = resultCount == null;

            if (results.size() < requestPageSize) {
                remainingResults = 0;
            } else {
                remainingResults = unknownCount ? -1 : resultCount - (firstResultIndex + results.size());
            }

            if (remainingResults > 0 || unknownCount) {
                nextCookie = String.valueOf(firstResultIndex + requestPageSize);
            } else {
                nextCookie = null;
            }
        } else {
            nextCookie = null;
            remainingResults = -1;
        }

        handler.handleResult(new QueryResult(nextCookie, remainingResults));
    }

    public List<Resource> query(QueryRequest request) throws ResourceException {
        List<Resource> results = new ArrayList<Resource>();

        logger.trace("Full id: {} Extracted type: {}", request.getResourceName(), request.getResourceName());
        
        // TODO: Statistics is not returned in result anymore
        // TODO: result is not needed in map form anymore
        Map<String, Object> result = new HashMap<String, Object>();
        ODatabaseDocumentTx db = getConnection();
        try {
            //List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
            //result.put(QueryConstants.QUERY_RESULT, docs);
            long start = System.currentTimeMillis();
            List<ODocument> queryResult = queries.query(request.getResourceName(), request, db);
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
                        new Object[] {results.size(), 
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
     * @throws org.forgerock.json.resource.InternalServerErrorException
     */
    ODatabaseDocumentTx getConnection() throws InternalServerErrorException {
        ODatabaseDocumentTx db = null;
        int maxRetry = 100; // give it up to approx 10 seconds to recover
        int retryCount = 0;
        
        synchronized (dbLock) {
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
        }
        return db;
    }

    public static String typeToOrientClassName(String type) {
        return type.replace("/", "_");
    }
    
    //public static String idToOrientClassName(String id) {
    //    String type = getObjectType(id);
    //    return typeToOrientClassName(type);
    //}

    /**
     * Detect if the root cause of the exception is a duplicate record.
     * This is necessary as the database may wrap this root cause in further exceptions,
     * masking the underlying cause
     * @param ex The throwable to check
     * @param maxLevels the maximum level of causes to check, avoiding the cost
     * of checking recursiveness
     * @return
     */
    private boolean isCauseRecordDuplicatedException(Throwable ex, int maxLevels) {
        return isCauseException (ex, ORecordDuplicatedException.class, maxLevels);
    }
    
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
        return isCauseException (ex, OIndexException.class, maxLevels);
    }
    
    /**
     * Detect if the root cause of the exception is an index constraint violation
     * This is necessary as the database may wrap this root cause in further exceptions,
     * masking the underlying cause
     * @param ex The throwable to check
     * @param maxLevels the maximum level of causes to check, avoiding the cost
     * of checking recursiveness
     * @return
     */
    private boolean isCauseConcurrentModificationException(Throwable ex, int maxLevels) {
        return isCauseException (ex, OConcurrentModificationException.class, maxLevels);
    }
    
    /**
     * Detect if the root cause of the exception is a specific OrientDB exception
     * This is necessary as the database may wrap this root cause in further exceptions,
     * masking the underlying cause
     * @param ex The throwable to check
     * @param clazz the specific OrientDB exception to check for
     * @param maxLevels the maximum level of causes to check, avoiding the cost
     * of checking recursiveness
     * @return whether the root cause is the specified exception
     */
    private boolean isCauseException(Throwable ex, Class clazz, int maxLevels) {
        if (maxLevels > 0) {
            Throwable cause = ex.getCause();
            if (cause != null) {
                return clazz.isInstance(cause) || isCauseException(cause, clazz, maxLevels - 1);
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
        if (bootRepo == null) {
            bootRepo = new OrientDBRepoService();
        }
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
        synchronized (dbLock) {
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
                && dbURL.equals(getDBUrl(newConfig))) {
            // If the DB pool settings don't change keep the existing pool
            logger.info("(Re-)initialize repository with latest configuration.");
            init(newConfig);
            if (bootRepo != null) {
                bootRepo.init(newConfig);
            }
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
