/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS.
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

package org.forgerock.openidm.repo.jdbc.impl;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.QueryResponse.NO_COUNT;
import static org.forgerock.json.resource.ResourceException.newInternalServerErrorException;
import static org.forgerock.json.resource.ResourceException.newNotSupportedException;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_REVISION;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.repo.QueryConstants.PAGED_RESULTS_OFFSET;
import static org.forgerock.openidm.repo.QueryConstants.PAGE_SIZE;
import static org.forgerock.openidm.repo.QueryConstants.QUERY_EXPRESSION;
import static org.forgerock.openidm.repo.QueryConstants.QUERY_FILTER;
import static org.forgerock.openidm.repo.QueryConstants.QUERY_ID;
import static org.forgerock.openidm.repo.QueryConstants.SORT_KEYS;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.jolbox.bonecp.BoneCPDataSource;
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
import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.osgi.OsgiName;
import org.forgerock.openidm.osgi.ServiceUtil;
import org.forgerock.openidm.repo.RepoBootService;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.repo.jdbc.DatabaseType;
import org.forgerock.openidm.repo.jdbc.ErrorType;
import org.forgerock.openidm.repo.jdbc.TableHandler;
import org.forgerock.openidm.repo.jdbc.impl.pool.DataSourceFactory;
import org.forgerock.openidm.util.Accessor;
import org.forgerock.openidm.patch.JsonPatch;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository service implementation using JDBC.
 */
@Component(name = JDBCRepoService.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE,
        enabled = true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Repository Service using JDBC"),
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/repo/*"),
    @Property(name = "db.type", value = "JDBC") })
public class JDBCRepoService implements RequestHandler, RepoBootService, RepositoryService {

    final static Logger logger = LoggerFactory.getLogger(JDBCRepoService.class);

    public static final String PID = "org.forgerock.openidm.repo.jdbc";
    private static final String ACTION_COMMAND = "command";

    private static ServiceRegistration sharedDataSource = null;

    // Keys in the JSON configuration
    public static final String CONFIG_CONNECTION = "connection";
    public static final String CONFIG_JNDI_NAME = "jndiName";
    public static final String CONFIG_JTA_NAME = "jtaName";
    public static final String CONFIG_DB_TYPE = "dbType";
    public static final String CONFIG_DB_DRIVER = "driverClass";
    public static final String CONFIG_DB_URL = "jdbcUrl";
    public static final String CONFIG_USER = "username";
    public static final String CONFIG_PASSWORD = "password";
    public static final String CONFIG_DB_SCHEMA = "defaultCatalog";
    public static final String CONFIG_MAX_BATCH_SIZE = "maxBatchSize";

    private boolean useDataSource;
    private String jndiName;
    private DataSource ds;
    private String dbDriver;
    private String dbUrl;
    private String user;
    private String password;

    private int maxTxRetry = 5;

    Map<String, TableHandler> tableHandlers;
    TableHandler defaultTableHandler;

    JsonValue config;
    
    /** CryptoService for detecting whether a value is encrypted */
    @Reference
    protected CryptoService cryptoService;

    /**
     * Enhanced configuration service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private EnhancedConfig enhancedConfig;

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        try {
            return newResultPromise(read(request));
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        } catch (Exception e) {
            return newExceptionPromise(newInternalServerErrorException("Read failed", e));
        }
    }

    @Override
    public ResourceResponse read(ReadRequest request) throws ResourceException {
        if (request.getResourcePathObject().size() < 2) {
            throw new BadRequestException(
                    "The repository requires clients to supply an identifier for the object to read.");
        }
        // Parse the remaining resourceName
        final String type = request.getResourcePathObject().parent().toString();
        final String localId = request.getResourcePathObject().leaf();

        Connection connection = null;
        ResourceResponse result = null;
        try {
            connection = getConnection();
            connection.setAutoCommit(true); // Ensure this does not get
                                            // transaction isolation handling
            TableHandler handler = getTableHandler(type);
            if (handler == null) {
                throw ResourceException.getException(ResourceException.INTERNAL_ERROR,
                        "No handler configured for resource type " + type);
            }
            result = handler.read(request.getResourcePath(), type, localId, connection);
            return result;
        } catch (SQLException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("SQL Exception in read of {} with error code {}, sql state {}",
                        request.getResourcePath(), ex.getErrorCode(), ex.getSQLState(), ex);
            }
            throw new InternalServerErrorException("Reading object failed " + ex.getMessage(), ex);
        } catch (ResourceException ex) {
            logger.debug("ResourceException in read of {}", request.getResourcePath(), ex);
            throw ex;
        } catch (IOException ex) {
            logger.debug("IO Exception in read of {}", request.getResourcePath(), ex);
            throw new InternalServerErrorException("Conversion of read object failed", ex);
        } finally {
            CleanupHelper.loggedClose(connection);
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        try {
            return newResultPromise(create(request));
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        } catch (Exception e) {
            return newExceptionPromise(newInternalServerErrorException("Failed to create resource", e));
        }
    }

    @Override
    public ResourceResponse create(CreateRequest request) throws ResourceException {
        if (request.getResourcePathObject().isEmpty()) {
            throw new BadRequestException(
                    "The respository requires clients to supply a type for the object to create.");
        }
        // Parse the remaining resourceName
        final String type = request.getResourcePath();
        final String localId = (request.getNewResourceId() == null || request.getNewResourceId().isEmpty())
                ? UUID.randomUUID().toString() // Generate ID server side.
                : request.getNewResourceId();
        final String fullId = type + "/" + localId;

        final JsonValue obj = request.getContent();

        Connection connection = null;
        boolean retry = false;
        int tryCount = 0;
        do {
            TableHandler handler = getTableHandler(type);
            if (handler == null) {
                throw ResourceException.getException(ResourceException.INTERNAL_ERROR,
                        "No handler configured for resource type " + type);
            }
            retry = false;
            ++tryCount;
            try {
                connection = getConnection();
                connection.setAutoCommit(false);

                handler.create(fullId, type, localId, obj.asMap(), connection);

                connection.commit();
                logger.debug("Commited created object for id: {}", fullId);

            } catch (SQLException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("SQL Exception in create of {} with error code {}, sql state {}",
                            fullId, ex.getErrorCode(), ex.getSQLState(), ex);
                }
                rollback(connection);
                boolean alreadyExisted = handler.isErrorType(ex, ErrorType.DUPLICATE_KEY);
                if (alreadyExisted) {
                    throw new PreconditionFailedException(
                            "Create rejected as Object with same ID already exists and was detected. "
                                    + "(" + ex.getErrorCode() + "-" + ex.getSQLState() + ")"
                                    + ex.getMessage(), ex);
                }
                if (handler.isRetryable(ex, connection)) {
                    if (tryCount <= maxTxRetry) {
                        retry = true;
                        logger.debug("Retryable exception encountered, retry {}", ex.getMessage());
                    }
                }
                if (!retry) {
                    throw new InternalServerErrorException("Creating object failed " + "("
                            + ex.getErrorCode() + "-" + ex.getSQLState() + ")" + ex.getMessage(),
                            ex);
                }
            } catch (ResourceException ex) {
                logger.debug("ResourceException in create of {}", fullId, ex);
                rollback(connection);
                throw ex;
            } catch (java.io.IOException ex) {
                logger.debug("IO Exception in create of {}", fullId, ex);
                rollback(connection);
                throw new InternalServerErrorException("Conversion of object to create failed", ex);
            } catch (RuntimeException ex) {
                logger.debug("Runtime Exception in create of {}", fullId, ex);
                rollback(connection);
                throw new InternalServerErrorException(
                        "Creating object failed with unexpected failure: " + ex.getMessage(), ex);
            } finally {
                CleanupHelper.loggedClose(connection);
            }
        } while (retry);

        // Return the newly created resource
        return newResourceResponse(obj.get(FIELD_CONTENT_ID).asString(), obj.get(FIELD_CONTENT_REVISION).asString(), obj);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        try {
            return newResultPromise(update(request));
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        } catch (Exception e) {
            return newExceptionPromise(newInternalServerErrorException("Update failed", e));
        }
    }

    @Override
    public ResourceResponse update(UpdateRequest request) throws ResourceException {
        if (request.getResourcePathObject().size() < 2) {
            throw new BadRequestException(
                    "The repository requires clients to supply an identifier for the object to update.");
        }
        // Parse the remaining resourceName
        final String type = request.getResourcePathObject().parent().toString();
        final String localId = request.getResourcePathObject().leaf();

        Map<String, Object> obj = request.getContent().asMap();
        String rev = request.getRevision() != null && !"".equals(request.getRevision())
                ? request.getRevision()
                : read(Requests.newReadRequest(request.getResourcePath())).getRevision();

        Connection connection = null;
        Integer previousIsolationLevel = null;
        boolean retry = false;
        int tryCount = 0;
        do {
            TableHandler handler = getTableHandler(type);
            if (handler == null) {
                throw ResourceException.getException(ResourceException.INTERNAL_ERROR,
                        "No handler configured for resource type " + type);
            }
            retry = false;
            ++tryCount;
            try {
                connection = getConnection();
                previousIsolationLevel = new Integer(connection.getTransactionIsolation());
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                connection.setAutoCommit(false);

                handler.update(request.getResourcePath(), type, localId, rev, obj, connection);

                connection.commit();
                logger.debug("Commited updated object for id: {}", request.getResourcePath());
            } catch (SQLException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("SQL Exception in update of {} with error code {}, sql state {}",
                            request.getResourcePath(), ex.getErrorCode(), ex.getSQLState(), ex);
                }
                rollback(connection);
                if (handler.isRetryable(ex, connection)) {
                    if (tryCount <= maxTxRetry) {
                        retry = true;
                        logger.debug("Retryable exception encountered, retry {}", ex.getMessage());
                    }
                }
                if (!retry) {
                    throw new InternalServerErrorException("Updating object failed "
                            + ex.getMessage(), ex);
                }
            } catch (ResourceException ex) {
                logger.debug("ResourceException in update of {}", request.getResourcePath(), ex);
                rollback(connection);
                throw ex;
            } catch (java.io.IOException ex) {
                logger.debug("IO Exception in update of {}", request.getResourcePath(), ex);
                rollback(connection);
                throw new InternalServerErrorException("Conversion of object to update failed", ex);
            } catch (RuntimeException ex) {
                logger.debug("Runtime Exception in update of {}", request.getResourcePath(), ex);
                rollback(connection);
                throw new InternalServerErrorException(
                        "Updating object failed with unexpected failure: " + ex.getMessage(), ex);
            } finally {
                if (connection != null) {
                    try {
                        if (previousIsolationLevel != null) {
                            connection.setTransactionIsolation(previousIsolationLevel.intValue());
                        }
                    } catch (SQLException ex) {
                        logger.warn("Failure in resetting connection isolation level ", ex);
                    }
                    CleanupHelper.loggedClose(connection);
                }
            }
        } while (retry);

        // Return the newly created resource
        return read(Requests.newReadRequest(request.getResourcePath()));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        try {
            return newResultPromise(delete(request));
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        } catch (Exception e) {
            return newExceptionPromise(newInternalServerErrorException("Failed to delete", e));
        }
    }

    @Override
    public ResourceResponse delete(DeleteRequest request) throws ResourceException {
        if (request.getResourcePathObject().size() < 2) {
            throw new BadRequestException(
                    "The repository requires clients to supply an identifier for the object to update.");
        }
        if (request.getRevision() == null) {
            throw new ConflictException(
                    "Object passed into delete does not have revision it expects set.");
        }

        // Parse the remaining resourceName
        final String type = request.getResourcePathObject().parent().toString();
        final String localId = request.getResourcePathObject().leaf();

        ResourceResponse result = null;
        Connection connection = null;
        boolean retry = false;
        int tryCount = 0;
        do {
            TableHandler handler = getTableHandler(type);
            if (handler == null) {
                throw ResourceException.getException(ResourceException.INTERNAL_ERROR,
                        "No handler configured for resource type " + type);
            }

            retry = false;
            ++tryCount;
            try {
                connection = getConnection();
                connection.setAutoCommit(false);

                // Read in the resource before deleting
                result = handler.read(request.getResourcePath(), type, localId, connection);

                handler.delete(request.getResourcePath(), type, localId, request.getRevision(), connection);

                connection.commit();
                logger.debug("Commited deleted object for id: {}", request.getResourcePath());
            } catch (ResourceException ex) {
                logger.debug("ResourceException in delete of {}", request.getResourcePath(), ex);
                rollback(connection);
                throw ex;
            } catch (IOException ex) {
                logger.debug("IO Exception in delete of {}", request.getResourcePath(), ex);
                rollback(connection);
                throw new InternalServerErrorException("Deleting object failed " + ex.getMessage(),
                        ex);
            } catch (SQLException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("SQL Exception in delete of {} with error code {}, sql state {}",
                            request.getResourcePath(), ex.getErrorCode(), ex.getSQLState(), ex);
                }
                rollback(connection);
                if (handler.isRetryable(ex, connection)) {
                    if (tryCount <= maxTxRetry) {
                        retry = true;
                        logger.debug("Retryable exception encountered, retry {}", ex.getMessage());
                    }
                }
                if (!retry) {
                    throw new InternalServerErrorException("Deleting object failed "
                            + ex.getMessage(), ex);
                }
            } catch (RuntimeException ex) {
                logger.debug("Runtime Exception in delete of {}", request.getResourcePath(), ex);
                rollback(connection);
                throw new InternalServerErrorException(
                        "Deleting object failed with unexpected failure: " + ex.getMessage(), ex);
            } finally {
                CleanupHelper.loggedClose(connection);
            }
        } while (retry);

        return result;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return newExceptionPromise(newNotSupportedException("Patch operations are not supported"));
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request, QueryResourceHandler handler) {
        try {

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

            List<ResourceResponse> results = query(request);
            for (ResourceResponse result : results) {
                handler.handleResource(result);
            }

            /*
             * Execute additional -count query if we are paging
             */
            final String nextCookie;

            // The number of results (if known)
            final int resultCount;

            if (pagedResultsRequested) {
                TableHandler tableHandler = getTableHandler(trimStartingSlash(request.getResourcePath()));

                // Get total if -count query is available
                final String countQueryId = request.getQueryId() + "-count";
                if (tableHandler.queryIdExists(countQueryId)) {
                    QueryRequest countRequest = Requests.copyOfQueryRequest(request);
                    countRequest.setQueryId(countQueryId);

                    // Strip pagination parameters
                    countRequest.setPageSize(0);
                    countRequest.setPagedResultsOffset(0);
                    countRequest.setPagedResultsCookie(null);

                    List<ResourceResponse> countResult = query(countRequest);

                    if (countResult != null && !countResult.isEmpty()) {
                        resultCount = countResult.get(0).getContent().get("total").asInteger();
                    } else {
                        logger.warn("Count query {} failed", countQueryId);
                        resultCount = NO_COUNT;
                    }
                } else {
                    logger.warn("Count query with id {} not found", countQueryId);
                    resultCount = NO_COUNT;
                }

                if (results.size() < requestPageSize) {
                    nextCookie = null;
                } else {
                    final int remainingResults = resultCount - (firstResultIndex + results.size());
                    if (remainingResults == 0) {
                        nextCookie = null;
                    } else {
                        nextCookie = String.valueOf(firstResultIndex + requestPageSize);
                    }
                }
            } else {
                nextCookie = null;
                resultCount = NO_COUNT;
            }

            // TODO-crest3 Check for count policy in the request
            if (resultCount == NO_COUNT) {
                return newResultPromise(newQueryResponse(nextCookie));
            } else {
                return newResultPromise(newQueryResponse(nextCookie, CountPolicy.EXACT, resultCount));
            }
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        } catch (Exception e) {
            return newExceptionPromise(newInternalServerErrorException("Query failed", e));
        }
    }

    @Override
    public List<ResourceResponse> query(QueryRequest request) throws ResourceException {
        String fullId = request.getResourcePath();
        String type = trimStartingSlash(fullId);
        logger.trace("Full id: {} Extracted type: {}", fullId, type);
        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(request.getAdditionalParameters());
        params.put(QUERY_ID, request.getQueryId());
        params.put(QUERY_EXPRESSION, request.getQueryExpression());
        params.put(QUERY_FILTER, request.getQueryFilter());
        params.put(PAGE_SIZE, request.getPageSize());
        params.put(PAGED_RESULTS_OFFSET, request.getPagedResultsOffset());
        params.put(SORT_KEYS, request.getSortKeys());  

        Connection connection = null;
        try {
            TableHandler tableHandler = getTableHandler(type);
            if (tableHandler == null) {
                throw ResourceException.getException(ResourceException.INTERNAL_ERROR,
                        "No handler configured for resource type " + type);
            }
            connection = getConnection();
            connection.setAutoCommit(true); // Ensure we do not implicitly
                                            // start transaction isolation

            List<Map<String, Object>> docs = tableHandler.query(type, params, connection);
            List<ResourceResponse> results = new ArrayList<ResourceResponse>();
            for (Map<String, Object> resultMap : docs) {
                String id = (String) resultMap.get("_id");
                String rev = (String) resultMap.get("_rev");
                JsonValue value = new JsonValue(resultMap);
                ResourceResponse resultResource = newResourceResponse(id, rev, value);
                results.add(resultResource);
            }
            return results;
        } catch (SQLException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("SQL Exception in query of {} with error code {}, sql state {}",
                        fullId, ex.getErrorCode(), ex.getSQLState(), ex);
            }
            throw new InternalServerErrorException("Querying failed: " + ex.getMessage(), ex);
        } catch (ResourceException ex) {
            logger.debug("ResourceException in query of {}", fullId, ex);
            throw ex;
        } finally {
            CleanupHelper.loggedClose(connection);
        }
    }
    
    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        try {
            if (ACTION_COMMAND.equalsIgnoreCase(request.getAction())) {
                return newResultPromise(command(request));
            } else {
                throw new NotSupportedException("Action operations are not supported");
            }
        } catch (final ResourceException e) {
            return newExceptionPromise(e);
        } catch (Exception e) {
            return newExceptionPromise(newInternalServerErrorException("Action failed", e));
        }
    }

    /**
     * Performs the repo command defined by the {@code request).
     *
     * @param request the request specifying the commandId or commandExpression and command parameters
     * @return the number of records affected
     * @throws ResourceException on failure to execute the command query
     */
    private ActionResponse command(ActionRequest request) throws ResourceException {
        final String type = request.getResourcePath();

        JsonValue result = null;
        Connection connection = null;
        boolean retry;
        int tryCount = 0;
        do {
            TableHandler handler = getTableHandler(type);
            if (handler == null) {
                throw ResourceException.getException(ResourceException.INTERNAL_ERROR,
                        "No handler configured for resource type " + type);
            }

            retry = false;
            ++tryCount;
            try {
                connection = getConnection();
                connection.setAutoCommit(false);

                result = new JsonValue(handler.command(type, new HashMap<String, Object>(request.getAdditionalParameters()), connection));

                connection.commit();
            } catch (SQLException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("SQL Exception in command on {} with error code {}, sql state {}",
                            request.getResourcePath(), ex.getErrorCode(), ex.getSQLState(), ex);
                }
                rollback(connection);
                if (handler.isRetryable(ex, connection)) {
                    if (tryCount <= maxTxRetry) {
                        retry = true;
                        logger.debug("Retryable exception encountered, retry {}", ex.getMessage());
                    }
                }
                if (!retry) {
                    throw new InternalServerErrorException("Command failed " + ex.getMessage(), ex);
                }
            } catch (ResourceException ex) {
                logger.debug("ResourceException in command on {}", request.getResourcePath(), ex);
                rollback(connection);
                throw ex;
            } catch (RuntimeException ex) {
                logger.debug("Runtime Exception in command on {}", request.getResourcePath(), ex);
                rollback(connection);
                throw new InternalServerErrorException(
                        "Command query failed with unexpected failure: " + ex.getMessage(), ex);
            } finally {
                CleanupHelper.loggedClose(connection);
            }
        } while (retry);

        return newActionResponse(result);
    }

    // Utility method to cleanly roll back including logging
    private void rollback(Connection connection) {
        if (connection != null) {
            try {
                logger.debug("Rolling back transaction.");
                connection.rollback();
            } catch (SQLException ex) {
                logger.warn("Rolling back transaction reported failure ", ex);
            }
        }
    }
    
    private String trimStartingSlash(String id) {
        if (id.startsWith("/") && id.length() > 1) {
            return id.substring(1);
        }
        return id;
    }

    Connection getConnection() throws SQLException {
        if (useDataSource) {
            return ds.getConnection();
        } else {
            return DriverManager.getConnection(dbUrl, user, password);
        }
    }

    TableHandler getTableHandler(String type) {
        TableHandler handler = tableHandlers.get(type);
        if (handler != null) {
            return handler;
        } else {
            handler = defaultTableHandler;
            for (String key : tableHandlers.keySet()) {
                if (type.startsWith(key)) {
                    handler = tableHandlers.get(key);
                    logger.debug("Use table handler configured for {} for type {} ", key, type);
                }
            }
            // For future lookups remember the handler determined for this
            // specific type
            tableHandlers.put(type, handler);
            return handler;
        }
    }

    /**
     * Populate and return a repository service that knows how to query and
     * manipulate configuration.
     *
     * @param repoConfig
     *            the bootstrap configuration
     * @param context
     * @return the boot repository service. This newBuilder is not managed by
     *         SCR and needs to be manually registered.
     */
    static RepoBootService getRepoBootService(JsonValue repoConfig, BundleContext context) { 
        JDBCRepoService bootRepo = new JDBCRepoService();
        bootRepo.init(repoConfig, context); 
        return bootRepo; 
    }

    /**
     * Activates the JDBC Repository Service
     *
     * @param compContext
     *            The component context
     */
    @Activate
    void activate(ComponentContext compContext) {
        logger.debug("Activating Service with configuration {}", compContext.getProperties());
        try {
            config = enhancedConfig.getConfigurationAsJson(compContext);
        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid and could not be parsed, can not start JDBC repository: "
                    + ex.getMessage(), ex);
            throw ex;
        }
        init(config, compContext.getBundleContext());
        logger.info("Repository started.");
    }

    /**
     * Deactivates the JDBC Repository Service
     *
     * @param compContext
     *            the component context
     */
    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext);
        shutdownDatabaseConnectionPool();
        logger.info("Repository stopped.");
    }

    /**
     * Handles configuration updates without interrupting the service
     *
     * @param compContext
     *            the component context
     */
    @Modified
    void modified(ComponentContext compContext) throws Exception {
        logger.debug("Reconfiguring the JDBC Repository Service with configuration {}", compContext
                .getProperties());
        try {
            JsonValue newConfig = enhancedConfig.getConfigurationAsJson(compContext);
            if (hasConfigChanged(config, newConfig)) {
                deactivate(compContext);
                activate(compContext);
                logger.info("Reconfigured the JDBC Repository Service {}", compContext
                        .getProperties());
            }
        } catch (Exception ex) {
            logger.warn("Configuration invalid, can not reconfigure the JDBC Repository Service.",
                    ex);
            throw ex;
        }
    }

    /**
     * Compares the current configuration with a new configuration to determine
     * if the configuration has changed
     *
     * @param existingConfig
     *            the current configuration object
     * @param newConfig
     *            the new configuration object
     * @return true if the configurations differ, false otherwise
     */
    private boolean hasConfigChanged(JsonValue existingConfig, JsonValue newConfig) {
        return JsonPatch.diff(existingConfig, newConfig).size() > 0;
    }

    /**
     * Initializes the JDBC Repository Service with the supplied configuration
     *
     * @param config
     *            the configuration object
     * @param bundleContext
     *            the bundle context
     * @throws InvalidException
     */
    void init(JsonValue config, BundleContext bundleContext) throws InvalidException {
        try {
            String enabled = config.get("enabled").asString();
            if ("false".equals(enabled)) {
                logger.debug("JDBC repository not enabled");
                throw new RuntimeException("JDBC repository not enabled.");
            }

            JsonValue connectionConfig =
                    config.get(CONFIG_CONNECTION).isNull() ? config : config.get(CONFIG_CONNECTION);

            maxTxRetry = connectionConfig.get("maxTxRetry").defaultTo(5).asInteger().intValue();

            ds = getDataSource(connectionConfig, bundleContext);

            // Table handling configuration
            String dbSchemaName = connectionConfig.get(CONFIG_DB_SCHEMA).defaultTo(null).asString();
            JsonValue genericQueries = config.get("queries").get("genericTables");
            JsonValue genericCommands = config.get("commands").get("genericTables");
            int maxBatchSize =
                    connectionConfig.get(CONFIG_MAX_BATCH_SIZE).defaultTo(100).asInteger();

            tableHandlers = new HashMap<String, TableHandler>();
            // TODO Make safe the database type detection
            DatabaseType databaseType =
                    DatabaseType.valueOf(connectionConfig.get(CONFIG_DB_TYPE).defaultTo(
                            DatabaseType.ANSI_SQL99.name()).asString());

            JsonValue defaultMapping = config.get("resourceMapping").get("default");
            if (!defaultMapping.isNull()) {
                defaultTableHandler =
                        getGenericTableHandler(databaseType, defaultMapping, dbSchemaName,
                                genericQueries, genericCommands, maxBatchSize);
                logger.debug("Using default table handler: {}", defaultTableHandler);
            } else {
                logger.warn("No default table handler configured");
            }

            // Default the configuration table for bootstrap
            JsonValue defaultTableProps = json(object(
                    field("mainTable", "configobjects"),
                    field("propertiesTable", "configobjectproperties"),
                    field("searchableDefault", Boolean.TRUE)));

            GenericTableHandler defaultConfigHandler =
                    getGenericTableHandler(databaseType, defaultTableProps, dbSchemaName,
                            genericQueries, genericCommands, 1);
            tableHandlers.put("config", defaultConfigHandler);

            JsonValue genericMapping = config.get("resourceMapping").get("genericMapping");
            if (!genericMapping.isNull()) {
                for (String key : genericMapping.keys()) {
                    JsonValue value = genericMapping.get(key);
                    if (key.endsWith("/*")) {
                        // For matching purposes strip the wildcard at the end
                        key = key.substring(0, key.length() - 1);
                    }
                    TableHandler handler =
                            getGenericTableHandler(databaseType, value, dbSchemaName,
                                    genericQueries, genericCommands, maxBatchSize);

                    tableHandlers.put(key, handler);
                    logger.debug("For pattern {} added handler: {}", key, handler);
                }
            }

            JsonValue explicitQueries = config.get("queries").get("explicitTables");
            JsonValue explicitCommands = config.get("commands").get("explicitTables");
            JsonValue explicitMapping = config.get("resourceMapping").get("explicitMapping");
            if (!explicitMapping.isNull()) {
                for (Object keyObj : explicitMapping.keys()) {
                    JsonValue value = explicitMapping.get((String) keyObj);
                    String key = (String) keyObj;
                    if (key.endsWith("/*")) {
                        // For matching purposes strip the wildcard at the end
                        key = key.substring(0, key.length() - 1);
                    }
                    TableHandler handler =
                            getMappedTableHandler(databaseType, value,
                                    value.get("table").required().asString(),
                                    value.get("objectToColumn").required().asMap(),
                                    dbSchemaName, explicitQueries, explicitCommands, maxBatchSize);

                    tableHandlers.put(key, handler);
                    logger.debug("For pattern {} added handler: {}", key, handler);
                }
            }

        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid, can not start JDBC repository.", ex);
            throw new InvalidException("Configuration invalid, can not start JDBC repository.", ex);
        } catch (InternalServerErrorException ex) {
            throw new InvalidException(
                    "Could not initialize mapped table handler, can not start JDBC repository.", ex);
        }

        Connection testConn = null;
        try {
            // Check if we can get a connection
            testConn = getConnection();
            testConn.setAutoCommit(true); // Ensure we do not implicitly start
                                          // transaction isolation
        } catch (Exception ex) {
            logger.warn(
                    "JDBC Repository start-up experienced a failure getting a DB connection: "
                            + ex.getMessage()
                            + ". If this is not temporary or resolved, Repository operation will be affected.",
                    ex);
        } finally {
            if (testConn != null) {
                try {
                    testConn.close();
                } catch (SQLException ex) {
                    logger.warn("Failure during test connection close ", ex);
                }
            }
        }
    }

    private DataSource getDataSource(JsonValue connectionConfig, BundleContext bundleContext) {
        try {
            // Data Source configuration
            jndiName = connectionConfig.get(CONFIG_JNDI_NAME).asString();
            String jtaName = connectionConfig.get(CONFIG_JTA_NAME).asString();
            if (jndiName != null && jndiName.trim().length() > 0) {
                // Get DB connection via JNDI
                logger.info("Using DB connection configured via Driver Manager");
                InitialContext ctx = null;
                try {
                    ctx = new InitialContext();
                } catch (NamingException ex) {
                    logger.warn("Getting JNDI initial context failed: " + ex.getMessage(), ex);
                }
                if (ctx == null) {
                    throw new InvalidException(
                            "Current platform context does not support lookup of repository DB via JNDI. "
                                    + " Configure DB initialization via direct " + CONFIG_DB_DRIVER
                                    + " configuration instead.");
                }

                useDataSource = true;
                return (DataSource) ctx.lookup(jndiName); // e.g.
                // "java:comp/env/jdbc/MySQLDB"
            } else if (!StringUtils.isBlank(jtaName)) {
                // e.g.
                // osgi:service/javax.sql.DataSource/(osgi.jndi.service.name=jdbc/openidm)
                OsgiName lookupName = OsgiName.parse(jtaName);
                Object service = ServiceUtil.getService(bundleContext, lookupName, null, true);
                if (service instanceof DataSource) {
                    useDataSource = true;
                    return (DataSource) service;
                } else {
                    throw new RuntimeException("DataSource can not be retrieved for: " + jtaName);
                }
            } else {
                // Get DB Connection via Driver Manager
                dbDriver = connectionConfig.get(CONFIG_DB_DRIVER).asString();
                if (dbDriver == null || dbDriver.trim().length() == 0) {
                    throw new InvalidException("Either a JNDI name (" + CONFIG_JNDI_NAME + "), "
                            + "or a DB driver lookup (" + CONFIG_DB_DRIVER
                            + ") needs to be configured to connect to a DB.");
                }
                dbUrl = connectionConfig.get(CONFIG_DB_URL).required().asString();
                user = connectionConfig.get(CONFIG_USER).required().asString();
                password = connectionConfig.get(CONFIG_PASSWORD).defaultTo("").asString();
                logger.info(
                        "Using DB connection configured via Driver Manager with Driver {} and URL",
                        dbDriver, dbUrl);
                try {
                    Class.forName(dbDriver);
                } catch (ClassNotFoundException ex) {
                    logger.error("Could not find configured database driver " + dbDriver
                            + " to start repository ", ex);
                    throw new InvalidException("Could not find configured database driver "
                            + dbDriver + " to start repository ", ex);
                }
                Boolean enableConnectionPool =
                        connectionConfig.get("enableConnectionPool").defaultTo(Boolean.FALSE)
                                .asBoolean();
                if (null == sharedDataSource) {
                    Dictionary<String, String> serviceParams = new Hashtable<String, String>(1);
                    serviceParams.put("osgi.jndi.service.name", "jdbc/openidm");
                    sharedDataSource =
                            bundleContext.registerService(DataSource.class.getName(),
                                    DataSourceFactory.newInstance(connectionConfig), serviceParams);
                }
                if (enableConnectionPool) {
                    logger.info("DataSource connection pool enabled.");
                    useDataSource = true;
                    return DataSourceFactory.newInstance(connectionConfig);
                } else {
                    logger.info("No DataSource connection pool enabled.");
                    return null;
                }
            }
        } catch (NamingException ex) {
            throw new InvalidException("Could not find configured jndiName " + jndiName
                    + " to start repository ", ex);
        }
    }

    GenericTableHandler getGenericTableHandler(DatabaseType databaseType, JsonValue tableConfig,
            String dbSchemaName, JsonValue queries, JsonValue commands, int maxBatchSize) {

        // TODO: make pluggable
        switch (databaseType) {
        case DB2:
            return
                    new DB2TableHandler(tableConfig, dbSchemaName, queries, commands, maxBatchSize,
                            new DB2SQLExceptionHandler());
        case ORACLE:
            return
                    new OracleTableHandler(tableConfig, dbSchemaName, queries, commands, maxBatchSize,
                            new DefaultSQLExceptionHandler());
        case POSTGRESQL:
            return
                    new PostgreSQLTableHandler(tableConfig, dbSchemaName, queries, commands, maxBatchSize,
                            new DefaultSQLExceptionHandler());
        case MYSQL:
            return
                    new GenericTableHandler(tableConfig, dbSchemaName, queries, commands, maxBatchSize,
                            new MySQLExceptionHandler());
        case SQLSERVER:
            return
                    new MSSQLTableHandler(tableConfig, dbSchemaName, queries, commands, maxBatchSize,
                            new DefaultSQLExceptionHandler());
        case H2:
            return
                    new H2TableHandler(tableConfig, dbSchemaName, queries, commands, maxBatchSize,
                            new DefaultSQLExceptionHandler());
        default:
            return
                    new GenericTableHandler(tableConfig, dbSchemaName, queries, commands, maxBatchSize,
                            new DefaultSQLExceptionHandler());
        }
    }

    MappedTableHandler getMappedTableHandler(DatabaseType databaseType, JsonValue tableConfig,
            String table, Map<String, Object> objectToColumn, String dbSchemaName,
            JsonValue explicitQueries, JsonValue explicitCommands, int maxBatchSize)
            throws InternalServerErrorException {

        final Accessor<CryptoService> cryptoServiceAccessor = new Accessor<CryptoService>() {
            public CryptoService access() {
                return cryptoService;
            }
        };

        // TODO: make pluggable
        switch (databaseType) {
        case DB2:
            return
                    new MappedTableHandler(table, objectToColumn, dbSchemaName, explicitQueries, explicitCommands,
                            new DB2SQLExceptionHandler(), cryptoServiceAccessor);
        case ORACLE:
            return
                    new OracleMappedTableHandler(table, objectToColumn, dbSchemaName, explicitQueries, explicitCommands,
                            new DefaultSQLExceptionHandler(), cryptoServiceAccessor);
        case POSTGRESQL:
            return
                    new MappedTableHandler(table, objectToColumn, dbSchemaName, explicitQueries, explicitCommands,
                            new DefaultSQLExceptionHandler(), cryptoServiceAccessor);
        case MYSQL:
            return
                    new MappedTableHandler(table, objectToColumn, dbSchemaName, explicitQueries, explicitCommands,
                            new MySQLExceptionHandler(), cryptoServiceAccessor);
        case SQLSERVER:
            return
                    new MSSQLMappedTableHandler(table, objectToColumn, dbSchemaName,
                            explicitQueries, explicitCommands, new DefaultSQLExceptionHandler(),
                            cryptoServiceAccessor);
        default:
            return
                    new MappedTableHandler(table, objectToColumn, dbSchemaName, explicitQueries, explicitCommands,
                            new DefaultSQLExceptionHandler(), cryptoServiceAccessor);
        }
    }

    private void shutdownDatabaseConnectionPool() {
        //set the shared datasource to null so it is reinitialized
        sharedDataSource = null;

        //close the datasource connection pool
        if (this.ds instanceof BoneCPDataSource) {
            ((BoneCPDataSource) ds).close();
        }
    }
}
