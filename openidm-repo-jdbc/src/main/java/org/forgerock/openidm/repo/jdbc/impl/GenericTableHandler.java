/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2015 ForgeRock AS. All rights reserved.
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

import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.repo.QueryConstants.PAGED_RESULTS_OFFSET;
import static org.forgerock.openidm.repo.QueryConstants.PAGE_SIZE;
import static org.forgerock.openidm.repo.QueryConstants.SORT_KEYS;
import static org.forgerock.openidm.repo.util.Clauses.where;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.SortKey;
import org.forgerock.openidm.repo.jdbc.ErrorType;
import org.forgerock.openidm.repo.jdbc.SQLExceptionHandler;
import org.forgerock.openidm.repo.jdbc.TableHandler;
import org.forgerock.openidm.repo.jdbc.impl.query.QueryResultMapper;
import org.forgerock.openidm.repo.jdbc.impl.query.TableQueries;
import org.forgerock.util.query.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handling of tables in a generic (not object specific) layout
 *
 */
public class GenericTableHandler implements TableHandler {
    final static Logger logger = LoggerFactory.getLogger(GenericTableHandler.class);

    /**
     * Maximum length of searchable properties.
     * This is used to trim values due to database index size limitations.
     */
    protected static final int SEARCHABLE_LENGTH = 2000;

    SQLExceptionHandler sqlExceptionHandler;

    GenericTableConfig cfg;

    final String mainTableName;
    final String propTableName;
    final String dbSchemaName;

    // Jackson parser
    final ObjectMapper mapper = new ObjectMapper();
    // Type information for the Jackson parser
    final TypeReference<LinkedHashMap<String,Object>> typeRef = new TypeReference<LinkedHashMap<String,Object>>() {};

    final TableQueries queries;
    
    Map<QueryDefinition, String> queryMap;

    final boolean enableBatching; // Whether to use JDBC statement batching.
    int maxBatchSize;       // The maximum number of statements to batch together. If max batch size is 1, do not use batching.

    public enum QueryDefinition {
        READTYPEQUERYSTR,
        CREATETYPEQUERYSTR,
        READFORUPDATEQUERYSTR,
        READQUERYSTR,
        CREATEQUERYSTR,
        UPDATEQUERYSTR,
        DELETEQUERYSTR,
        PROPCREATEQUERYSTR,
        PROPDELETEQUERYSTR,
        QUERYALLIDS
    }

    @Override
    public boolean queryIdExists(String queryId) {
        return queries.queryIdExists(queryId);
    }

    /**
     * Create a generic table handler using a QueryFilterVisitor that uses generic object property tables to process
     * query filters.
     *
     * @param tableConfig the table config
     * @param dbSchemaName the schem name
     * @param queriesConfig a map of named queries
     * @param commandsConfig a map of named commands
     * @param maxBatchSize the maximum batch size
     * @param sqlExceptionHandler a handler for SQLExceptions
     */
    public GenericTableHandler(JsonValue tableConfig,
            String dbSchemaName,
            JsonValue queriesConfig,
            JsonValue commandsConfig,
            int maxBatchSize,
            SQLExceptionHandler sqlExceptionHandler) {

        cfg = GenericTableConfig.parse(tableConfig);

        this.mainTableName = cfg.mainTableName;
        this.propTableName = cfg.propertiesTableName;
        this.dbSchemaName = dbSchemaName;
        if (maxBatchSize < 1) {
            this.maxBatchSize = 1;
        } else {
            this.maxBatchSize = maxBatchSize;
        }

        if (sqlExceptionHandler == null) {
            this.sqlExceptionHandler = new DefaultSQLExceptionHandler();
        } else {
            this.sqlExceptionHandler = sqlExceptionHandler;
        }

        queries = new TableQueries(this, mainTableName, propTableName, dbSchemaName, getSearchableLength(), new GenericQueryResultMapper());
        queryMap = Collections.unmodifiableMap(initializeQueryMap());
        queries.setConfiguredQueries(queriesConfig, commandsConfig, queryMap);

        // TODO: Consider taking into account DB meta-data rather than just configuration
        //DatabaseMetaData metadata = connection.getMetaData();
        //boolean isBatchingSupported = metadata.supportsBatchUpdates();
        //if (!isBatchingSupported) {
        //    maxBatchSize = 1;
        //}
        enableBatching = (this.maxBatchSize > 1);
        if (enableBatching) {
            logger.info("JDBC statement batching enabled, maximum batch size {}", this.maxBatchSize);
        } else {
            logger.info("JDBC statement batching disabled.");
        }
    }

    /**
     * Get the length of the searchable index.
     */
    int getSearchableLength() {
        return SEARCHABLE_LENGTH;
    }

    protected Map<QueryDefinition, String> initializeQueryMap() {
        Map<QueryDefinition, String> result = new EnumMap<QueryDefinition, String>(QueryDefinition.class);

        String typeTable = dbSchemaName == null ? "objecttypes" : dbSchemaName + ".objecttypes";
        String mainTable = dbSchemaName == null ? mainTableName : dbSchemaName + "." + mainTableName;
        String propertyTable = dbSchemaName == null ? propTableName : dbSchemaName + "." + propTableName;

        // objecttypes table
        result.put(QueryDefinition.CREATETYPEQUERYSTR, "INSERT INTO " + typeTable + " (objecttype) VALUES (?)");
        result.put(QueryDefinition.READTYPEQUERYSTR, "SELECT id FROM " + typeTable + " objtype WHERE objtype.objecttype = ?");

        // Main object table
        result.put(QueryDefinition.READFORUPDATEQUERYSTR, "SELECT obj.* FROM " + mainTable + " obj INNER JOIN " + typeTable + " objtype ON obj.objecttypes_id = objtype.id AND objtype.objecttype = ? WHERE obj.objectid  = ? FOR UPDATE");
        result.put(QueryDefinition.READQUERYSTR, "SELECT obj.rev, obj.fullobject FROM " + typeTable + " objtype, " + mainTable + " obj WHERE obj.objecttypes_id = objtype.id AND objtype.objecttype = ? AND obj.objectid  = ?");
        result.put(QueryDefinition.CREATEQUERYSTR, "INSERT INTO " + mainTable + " (objecttypes_id, objectid, rev, fullobject) VALUES (?,?,?,?)");
        result.put(QueryDefinition.UPDATEQUERYSTR, "UPDATE " + mainTable + " obj SET obj.objectid = ?, obj.rev = ?, obj.fullobject = ? WHERE obj.id = ?");
        result.put(QueryDefinition.DELETEQUERYSTR, "DELETE obj FROM " + mainTable + " obj INNER JOIN " + typeTable + " objtype ON obj.objecttypes_id = objtype.id AND objtype.objecttype = ? WHERE obj.objectid = ? AND obj.rev = ?");

        /* DB2 Script
        deleteQueryStr = "DELETE FROM " + dbSchemaName + "." + mainTableName + " obj WHERE EXISTS (SELECT 1 FROM " + dbSchemaName + ".objecttypes objtype WHERE obj.objecttypes_id = objtype.id AND objtype.objecttype = ?) AND obj.objectid = ? AND obj.rev = ?";
        */

        // Object properties table
        result.put(QueryDefinition.PROPCREATEQUERYSTR, "INSERT INTO " + propertyTable + " ( " + mainTableName + "_id, propkey, proptype, propvalue) VALUES (?,?,?,?)");
        result.put(QueryDefinition.PROPDELETEQUERYSTR, "DELETE prop FROM " + propertyTable + " prop INNER JOIN " + mainTable + " obj ON prop." + mainTableName + "_id = obj.id INNER JOIN " + typeTable + " objtype ON obj.objecttypes_id = objtype.id WHERE objtype.objecttype = ? AND obj.objectid = ?");
        // Default object queries
        String tableVariable =  dbSchemaName == null ? "${_mainTable}" : "${_dbSchema}.${_mainTable}";
        result.put(QueryDefinition.QUERYALLIDS, "SELECT obj.objectid FROM " + tableVariable + " obj INNER JOIN " + typeTable + " objtype ON obj.objecttypes_id = objtype.id WHERE objtype.objecttype = ${_resource}");

        return result;
    }

    /* (non-Javadoc)
    * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#read(java.lang.String, java.lang.String, java.lang.String, java.sql.Connection)
    */
    @Override
    public ResourceResponse read(String fullId, String type, String localId, Connection connection)
            throws ResourceException, SQLException, IOException {

        ResourceResponse result = null;
        Map<String, Object> resultMap = null;
        PreparedStatement readStatement = null;
        ResultSet rs = null;
        try {
            readStatement = getPreparedStatement(connection, QueryDefinition.READQUERYSTR);
            logger.trace("Populating prepared statement {} for {}", readStatement, fullId);
            readStatement.setString(1, type);
            readStatement.setString(2, localId);

            logger.debug("Executing: {}", readStatement);
            rs = readStatement.executeQuery();
            if (rs.next()) {
                String rev = rs.getString("rev");
                String objString = rs.getString("fullobject");
                resultMap = mapper.readValue(objString, typeRef);
                resultMap.put("_rev", rev);
                logger.debug(" full id: {}, rev: {}, obj {}", fullId, rev, resultMap);
                return newResourceResponse(localId, rev, new JsonValue(resultMap));
            } else {
                throw ResourceException.getException(ResourceException.NOT_FOUND,
                        "Object " + fullId + " not found in " + type);
            }
        } finally {
            CleanupHelper.loggedClose(rs);
            CleanupHelper.loggedClose(readStatement);
        }
    }

    /* (non-Javadoc)
    * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#create(java.lang.String, java.lang.String, java.lang.String, java.util.Map, java.sql.Connection)
    */
    @Override
    public void create(String fullId, String type, String localId, Map<String, Object> obj, Connection connection)
            throws SQLException, IOException, InternalServerErrorException {

        long typeId = getTypeId(type, connection); // Note this call can commit and start a new transaction in some cases

        PreparedStatement createStatement = null;
        try {
            createStatement = queries.getPreparedStatement(connection, queryMap.get(QueryDefinition.CREATEQUERYSTR), true);

            logger.debug("Create with fullid {}", fullId);
            String rev = "0";
            obj.put("_id", localId); // Save the id in the object
            obj.put("_rev", rev); // Save the rev in the object, and return the changed rev from the create.
            String objString = mapper.writeValueAsString(obj);

            logger.trace("Populating statement {} with params {}, {}, {}, {}",
                    queryMap.get(QueryDefinition.CREATEQUERYSTR), typeId, localId, rev, objString);
            createStatement.setLong(1, typeId);
            createStatement.setString(2, localId);
            createStatement.setString(3, rev);
            createStatement.setString(4, objString);
            logger.debug("Executing: {}", createStatement);
            int val = createStatement.executeUpdate();

            ResultSet keys = createStatement.getGeneratedKeys();
            boolean validKeyEntry = keys.next();
            if (!validKeyEntry) {
                throw new InternalServerErrorException("Object creation for " + fullId + " failed to retrieve an assigned ID from the DB.");
            }
            long dbId = keys.getLong(1);

            logger.debug("Created object for id {} with rev {}", fullId, rev);
            JsonValue jv = new JsonValue(obj);
            writeValueProperties(fullId, dbId, localId, jv, connection);
        } finally {
            CleanupHelper.loggedClose(createStatement);
        }
    }

    /**
     * Writes all properties of a given resource to the properties table and links them to the main table record.
     *
     * @param fullId the full URI of the resource the belongs to
     * @param dbId the generated identifier to link the properties table with the main table (foreign key)
     * @param localId the local identifier of the resource these properties belong to
     * @param value the JSON value with the properties to write
     * @param connection the DB connection
     * @throws SQLException if the insert failed
     */
    void writeValueProperties(String fullId, long dbId, String localId, JsonValue value, Connection connection) throws SQLException {
        if (cfg.hasPossibleSearchableProperties()) {
            Integer batchingCount = 0;
            PreparedStatement propCreateStatement = getPreparedStatement(connection, QueryDefinition.PROPCREATEQUERYSTR);
            try {
                batchingCount = writeValueProperties(fullId, dbId, localId, value, connection, propCreateStatement, batchingCount);
                if (enableBatching && batchingCount > 0) {
                    int[] numUpdates = propCreateStatement.executeBatch();
                    logger.debug("Batch update of objectproperties updated: {}", numUpdates);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Writing batch of objectproperties, updated: {}", Arrays.asList(numUpdates));
                    }
                    propCreateStatement.clearBatch();
                }
            } finally {
                CleanupHelper.loggedClose(propCreateStatement);
            }
        }
    }
    /**
     * Internal recursive function to add/write properties.
     * If batching is enabled, prepared statements are added to the batch and only executed if they hit the max limit.
     * After completion returns the number of properties that have only been added to the batch but not yet executed.
     * The caller is responsible for executing the batch on remaining items when it deems the batch complete.
     *
     * If batching is not enabled, prepared statements are immediately executed.
     *
     * @param fullId the full URI of the resource the belongs to
     * @param dbId the generated identifier to link the properties table with the main table (foreign key)
     * @param localId the local identifier of the resource these properties belong to
     * @param value the JSON value with the properties to write
     * @param connection the DB connection
     * @param propCreateStatement the prepared properties insert statement
     * @param batchingCount the current number of statements that have been batched and not yet executed on the prepared statement
     * @return status of the current batchingCount, i.e. how many statements are not yet executed in the PreparedStatement
     * @throws SQLException if the insert failed
     */
    private int writeValueProperties(String fullId, long dbId, String localId, JsonValue value, Connection connection,
            PreparedStatement propCreateStatement, int batchingCount) throws SQLException {

        for (JsonValue entry : value) {
            JsonPointer propPointer = entry.getPointer();
            if (cfg.isSearchable(propPointer)) {
                String propkey = propPointer.toString();
                if (entry.isMap() || entry.isList()) {
                    batchingCount = writeValueProperties(fullId, dbId, localId, entry, connection, propCreateStatement, batchingCount);
                } else {
                    String propvalue = null;
                    Object val = entry.getObject();
                    if (val != null) {
                        propvalue = StringUtils.left(val.toString(), getSearchableLength());
                    }
                    String proptype = null;
                    if (propvalue != null) {
                        proptype = entry.getObject().getClass().getName(); // TODO: proper type info
                    }
                    if (logger.isTraceEnabled()) {
                        logger.trace("Populating statement {} with params {}, {}, {}, {}, {}",
                                queryMap.get(QueryDefinition.PROPCREATEQUERYSTR), dbId, localId, propkey, proptype, propvalue);
                    }
                    propCreateStatement.setLong(1, dbId);
                    propCreateStatement.setString(2, propkey);
                    propCreateStatement.setString(3, proptype);
                    propCreateStatement.setString(4, propvalue);
                    logger.debug("Executing: {}", propCreateStatement);
                    if (enableBatching) {
                        propCreateStatement.addBatch();
                        batchingCount++;
                    } else {
                        int numUpdate = propCreateStatement.executeUpdate();
                    }
                    if (logger.isTraceEnabled()) {
                        logger.trace("Inserting objectproperty id: {} propkey: {} proptype: {}, propvalue: {}", fullId, propkey, proptype, propvalue);
                    }
                }
                if (enableBatching && batchingCount >= maxBatchSize) {
                    int[] numUpdates = propCreateStatement.executeBatch();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Batch limit reached, update of objectproperties updated: {}", Arrays.asList(numUpdates));
                    }
                    propCreateStatement.clearBatch();
                    batchingCount = 0;
                }
            }
        }

        return batchingCount;
    }

    /**
     * @inheritDoc
     */
    public boolean isErrorType(SQLException ex, ErrorType errorType) {
        return sqlExceptionHandler.isErrorType(ex, errorType);
    }

    /**
     * @inheritDoc
     */
    public boolean isRetryable(SQLException ex, Connection connection) {
        return sqlExceptionHandler.isRetryable(ex, connection);
    }

    // Ensure type is in objecttypes table and get its assigned id
    // Callers should note that this may commit a transaction and start a new one if a new type gets added
    long getTypeId(String type, Connection connection) throws SQLException, InternalServerErrorException {
        Exception detectedEx = null;
        long typeId = readTypeId(type, connection);
        if (typeId < 0) {
            connection.setAutoCommit(true); // Commit the new type right away, and have no transaction isolation for read
            try {
                createTypeId(type, connection);

            } catch (SQLException ex) {
                // Rather than relying on DB specific ignore if exists functionality handle it here
                // Could extend this in the future to more explicitly check for duplicate key error codes, but these again can be DB specific
                detectedEx = ex;
            }
            typeId = readTypeId(type, connection);
            if (typeId < 0) {
                throw new InternalServerErrorException("Failed to populate and look up objecttypes table, no id could be retrieved for " + type, detectedEx);
            }
            connection.setAutoCommit(false); // Start another transaction
        }
        return typeId;
    }

    /**
     * @param type       the object type URI
     * @param connection the DB connection
     * @return the typeId for the given type if exists, or -1 if does not exist
     * @throws java.sql.SQLException
     */
    long readTypeId(String type, Connection connection) throws SQLException {
        long typeId = -1;

        Map<String, Object> result = null;
        ResultSet rs = null;
        PreparedStatement readTypeStatement = null;
        try {
            readTypeStatement = getPreparedStatement(connection, QueryDefinition.READTYPEQUERYSTR);

            logger.trace("Populating prepared statement {} for {}",
                    queryMap.get(QueryDefinition.READTYPEQUERYSTR), type);
            readTypeStatement.setString(1, type);

            logger.debug("Executing: {}", readTypeStatement);
            rs = readTypeStatement.executeQuery();
            if (rs.next()) {
                typeId = rs.getLong("id");
                logger.debug("Type: {}, id: {}", type, typeId);
            }
        } finally {
            CleanupHelper.loggedClose(rs);
            CleanupHelper.loggedClose(readTypeStatement);
        }
        return typeId;
    }

    /**
     * @param type       the object type URI
     * @param connection the DB connection
     * @return true if a type was inserted
     * @throws SQLException if the insert failed (e.g. concurrent insert by another thread)
     */
    boolean createTypeId(String type, Connection connection) throws SQLException {
        PreparedStatement createTypeStatement = getPreparedStatement(connection, QueryDefinition.CREATETYPEQUERYSTR);
        try {
            logger.debug("Create objecttype {}", type);
            createTypeStatement.setString(1, type);
            logger.debug("Executing: {}", createTypeStatement);
            int val = createTypeStatement.executeUpdate();
            return (val == 1);
        } finally {
            CleanupHelper.loggedClose(createTypeStatement);
        }
    }

    /**
     * Reads an object with for update locking applied
     *
     * Note: statement associated with the returned resultset
     * is not closed upon return.
     * Aside from taking care to close the resultset it also is
     * the responsibility of the caller to close the associated
     * statement. Although the specification specifies that drivers/pools
     * should close the statement automatically, not all do this reliably.
     *
     * @param fullId qualified id of component type and id
     * @param type the component type
     * @param localId the id of the object within the component type
     * @param connection the connection to use
     * @return the row for the requested object, selected FOR UPDATE
     * @throws NotFoundException if the requested object was not found in the DB
     * @throws java.sql.SQLException for general DB issues
     */
    public ResultSet readForUpdate(String fullId, String type, String localId, Connection connection)
            throws NotFoundException, SQLException {

        PreparedStatement readForUpdateStatement = null;
        ResultSet rs = null;
        try {
            readForUpdateStatement = getPreparedStatement(connection, QueryDefinition.READFORUPDATEQUERYSTR);
            logger.trace("Populating prepared statement {} for {}", readForUpdateStatement, fullId);
            readForUpdateStatement.setString(1, type);
            readForUpdateStatement.setString(2, localId);

            logger.debug("Executing: {}", readForUpdateStatement);
            rs = readForUpdateStatement.executeQuery();
            if (rs.next()) {
                logger.debug("Read for update full id: {}", fullId);
                return rs;
            } else {
                CleanupHelper.loggedClose(rs);
                CleanupHelper.loggedClose(readForUpdateStatement);
                throw new NotFoundException("Object " + fullId + " not found in " + type);
            }
        } catch (SQLException ex) {
            CleanupHelper.loggedClose(rs);
            CleanupHelper.loggedClose(readForUpdateStatement);
            throw ex;
        }
    }

    /* (non-Javadoc)
    * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#update(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Map, java.sql.Connection)
    */
    @Override
    public void update(String fullId, String type, String localId, String rev, Map<String, Object> obj, Connection connection)
            throws SQLException, IOException, PreconditionFailedException, NotFoundException, InternalServerErrorException {
        logger.debug("Update with fullid {}", fullId);

        int revInt = Integer.parseInt(rev);
        ++revInt;
        String newRev = Integer.toString(revInt);
        obj.put("_rev", newRev); // Save the rev in the object, and return the changed rev from the create.

        ResultSet rs = null;
        PreparedStatement updateStatement = null;
        PreparedStatement deletePropStatement = null;
        try {
            rs = readForUpdate(fullId, type, localId, connection);
            String existingRev = rs.getString("rev");
            long dbId = rs.getLong("id");
            long objectTypeDbId = rs.getLong("objecttypes_id");
            logger.debug("Update existing object {} rev: {} db id: {}, object type db id: {}", fullId, existingRev, dbId, objectTypeDbId);

            if (!existingRev.equals(rev)) {
                throw new PreconditionFailedException("Update rejected as current Object revision " + existingRev + " is different than expected by caller (" + rev + "), the object has changed since retrieval.");
            }
            updateStatement = getPreparedStatement(connection, QueryDefinition.UPDATEQUERYSTR);
            deletePropStatement = getPreparedStatement(connection, QueryDefinition.PROPDELETEQUERYSTR);

            // Support changing object identifier
            String newLocalId = (String) obj.get("_id");
            if (newLocalId != null && !localId.equals(newLocalId)) {
                logger.debug("Object identifier is changing from " + localId + " to " + newLocalId);
            } else {
                newLocalId = localId; // If it hasn't changed, use the existing ID
                obj.put("_id", newLocalId); // Ensure the ID is saved in the object
            }
            String objString = mapper.writeValueAsString(obj);

            logger.trace("Populating prepared statement {} for {} {} {} {} {}", updateStatement, fullId, newLocalId, newRev, objString, dbId);
            updateStatement.setString(1, newLocalId);
            updateStatement.setString(2, newRev);
            updateStatement.setString(3, objString);
            updateStatement.setLong(4, dbId);
            logger.debug("Update statement: {}", updateStatement);
            int updateCount = updateStatement.executeUpdate();
            logger.trace("Updated rows: {} for {}", updateCount, fullId);
            if (updateCount != 1) {
                throw new InternalServerErrorException("Update execution did not result in updating 1 row as expected. Updated rows: " + updateCount);
            }

            JsonValue jv = new JsonValue(obj);
            // TODO: only update what changed?
            logger.trace("Populating prepared statement {} for {} {} {}", deletePropStatement, fullId, type, localId);
            deletePropStatement.setString(1, type);
            deletePropStatement.setString(2, localId);
            logger.debug("Update properties del statement: {}", deletePropStatement);
            int deleteCount = deletePropStatement.executeUpdate();
            logger.trace("Deleted child rows: {} for: {}", deleteCount, fullId);
            writeValueProperties(fullId, dbId, localId, jv, connection);
        } finally {
            if (rs != null) {
                // Ensure associated statement also is closed
                Statement rsStatement = rs.getStatement();
                CleanupHelper.loggedClose(rs);
                CleanupHelper.loggedClose(rsStatement);
            }
            CleanupHelper.loggedClose(updateStatement);
            CleanupHelper.loggedClose(deletePropStatement);
        }
    }

    /**
     * @see org.forgerock.openidm.repo.jdbc.impl.GenericTableHandler#delete(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.sql.Connection)
     */
    @Override
    public void delete(String fullId, String type, String localId, String rev, Connection connection)
            throws PreconditionFailedException, InternalServerErrorException, NotFoundException, SQLException, IOException {
        logger.debug("Delete with fullid {}", fullId);

        // First check if the revision matches and select it for UPDATE
        ResultSet existing = null;
        PreparedStatement deleteStatement = null;
        try {
            try {
                existing = readForUpdate(fullId, type, localId, connection);
            } catch (NotFoundException ex) {
                throw new NotFoundException("Object does not exist for delete on: " + fullId);
            }
            String existingRev = existing.getString("rev");
            if (!"*".equals(rev) && !rev.equals(existingRev)) {
                throw new PreconditionFailedException("Delete rejected as current Object revision " + existingRev + " is different than "
                        + "expected by caller " + rev + ", the object has changed since retrieval.");
            }

            // Proceed with the valid delete
            deleteStatement = getPreparedStatement(connection, QueryDefinition.DELETEQUERYSTR);
            logger.trace("Populating prepared statement {} for {} {} {} {}", deleteStatement, fullId, type, localId, rev);

            // Rely on ON DELETE CASCADE for connected object properties to be deleted
            deleteStatement.setString(1, type);
            deleteStatement.setString(2, localId);
            deleteStatement.setString(3, rev);
            logger.debug("Delete statement: {}", deleteStatement);

            int deletedRows = deleteStatement.executeUpdate();
            logger.trace("Deleted {} rows for id : {} {}", deletedRows, localId);
            if (deletedRows < 1) {
                throw new InternalServerErrorException("Deleting object for " + fullId + " failed, DB reported " + deletedRows + " rows deleted");
            } else {
                logger.debug("delete for id succeeded: {} revision: {}", localId, rev);
            }
        } finally {
            if (existing != null) {
                // Ensure associated statement also is closed
                Statement existingStatement = existing.getStatement();
                CleanupHelper.loggedClose(existing);
                CleanupHelper.loggedClose(existingStatement);
            }
            CleanupHelper.loggedClose(deleteStatement);
        }
    }

    /* (non-Javadoc)
     * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#delete(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.sql.Connection)
     */
    @Override
    public List<Map<String, Object>> query(String type, Map<String, Object> params, Connection connection)
            throws ResourceException {
        return queries.query(type, params, connection);
    }

    @Override
    public Integer command(String type, Map<String, Object> params, Connection connection) throws SQLException, ResourceException {
        return queries.command(type, params, connection);
    }

    @Override
    public String toString() {
        return "Generic handler mapped to [" + mainTableName + ", " + propTableName + "]";
    }

    protected PreparedStatement getPreparedStatement(Connection connection, QueryDefinition queryDefinition) throws SQLException {
        return queries.getPreparedStatement(connection, queryMap.get(queryDefinition));
    }

    /**
     * Render and SQL SELECT statement with placeholders for the given query filter.
     *
     * @param filter the query filter
     * @param replacementTokens a map to store any replacement tokens
     * @param params a map containing query parameters
     * @return an SQL SELECT statement
     */
    @Override
    public String renderQueryFilter(QueryFilter<JsonPointer> filter, Map<String, Object> replacementTokens, Map<String, Object> params) {
        final int offsetParam = Integer.parseInt((String) params.get(PAGED_RESULTS_OFFSET));
        final int pageSizeParam = Integer.parseInt((String) params.get(PAGE_SIZE));

        SQLBuilder builder = new SQLBuilder() {
            @Override
            public String toSQL() {
                return "SELECT " + getColumns().toSQL()
                        + getFromClause().toSQL()
                        + getJoinClause().toSQL()
                        + getWhereClause().toSQL()
                        + getOrderByClause().toSQL()
                        + " LIMIT " + pageSizeParam
                        + " OFFSET " + offsetParam;
            }
        };

        // "SELECT obj.* FROM mainTable obj..."
        builder.addColumn("obj.*")
                .from("${_dbSchema}.${_mainTable} obj")

                // join objecttypes to fix OPENIDM-2773
                .join("${_dbSchema}.objecttypes", "objecttypes")
                .on(where("obj.objecttypes_id = objecttypes.id")
                        .and("objecttypes.objecttype = ${otype}"))

                // construct where clause by visiting filter
                .where(filter.accept(new GenericSQLQueryFilterVisitor(SEARCHABLE_LENGTH, builder), replacementTokens));

        // other half of OPENIDM-2773 fix
        replacementTokens.put("otype", params.get("_resource"));

        // JsonValue-cheat to avoid an unchecked cast
        final List<SortKey> sortKeys = new JsonValue(params).get(SORT_KEYS).asList(SortKey.class);
        // Check for sort keys and build up order-by syntax
        prepareSortKeyStatements(builder, sortKeys, replacementTokens);

        return builder.toSQL();
    }

    /**
     * Loops through sort keys constructing the inner join and key statements.
     *
     * @param builder the SQL builder
     * @param sortKeys a {@link java.util.List} of sort keys
     * @param replacementTokens a {@link java.util.Map} containing replacement tokens for the {@link java.sql.PreparedStatement}
     */
    protected void prepareSortKeyStatements(SQLBuilder builder, List<SortKey> sortKeys, Map<String, Object> replacementTokens) {
        if (sortKeys == null) {
            return;
        }
        for (int i = 0; i < sortKeys.size(); i++) {
            final SortKey sortKey = sortKeys.get(i);
            final String tokenName = "sortKey" + i;
            final String tableAlias = "orderby" + i;
            builder.join("${_dbSchema}.${_propTable}", tableAlias)
                    .on(where(tableAlias + ".${_mainTable}_id = obj.id").and(tableAlias + ".propkey = ${" + tokenName + "}"))
                    .orderBy(tableAlias + ".propvalue", sortKey.isAscendingOrder());

            replacementTokens.put(tokenName, sortKey.getField().toString());
        }
    }
}

class GenericQueryResultMapper implements QueryResultMapper {
    final static Logger logger = LoggerFactory.getLogger(GenericQueryResultMapper.class);

    // Jackson parser
    ObjectMapper mapper = new ObjectMapper();
    // Type information for the Jackson parser
    TypeReference<LinkedHashMap<String,Object>> typeRef = new TypeReference<LinkedHashMap<String,Object>>() {};

    public List<Map<String, Object>> mapQueryToObject(ResultSet rs, String queryId, String type, Map<String, Object> params,  TableQueries tableQueries)
            throws SQLException, IOException {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        ResultSetMetaData rsMetaData = rs.getMetaData();
        boolean hasFullObject = tableQueries.hasColumn(rsMetaData, "fullobject");
        boolean hasId = false;
        boolean hasRev = false;
        boolean hasPropKey = false;
        boolean hasPropValue = false;
        boolean hasTotal = false;
        if (!hasFullObject) {
            hasId = tableQueries.hasColumn(rsMetaData, "objectid");
            hasRev = tableQueries.hasColumn(rsMetaData, "rev");
            hasPropKey = tableQueries.hasColumn(rsMetaData, "propkey");
            hasPropValue = tableQueries.hasColumn(rsMetaData, "propvalue");
            hasTotal = tableQueries.hasColumn(rsMetaData, "total");
        }
        while (rs.next()) {
            if (hasFullObject) {
                String objString = rs.getString("fullobject");
                Map<String, Object> obj = mapper.readValue(objString, typeRef);

                // TODO: remove data logging
                logger.trace("Query result for queryId: {} type: {} converted obj: {}", new Object[] {queryId, type, obj});

                result.add(obj);
            } else {
                Map<String, Object> obj = new HashMap<String, Object>();
                if (hasId) {
                    obj.put("_id", rs.getString("objectid"));
                }
                if (hasRev) {
                    obj.put("_rev", rs.getString("rev"));
                }
                if (hasTotal) {
                    obj.put("total", rs.getInt("total"));
                }
                // Results from query on individual searchable property
                if (hasPropKey && hasPropValue) {
                    String propKey = rs.getString("propkey");
                    Object propValue = rs.getObject("propvalue");
                    JsonPointer pointer = new JsonPointer(propKey);
                    JsonValue wrapped = new JsonValue(obj);
                    wrapped.put(pointer, propValue);
                }
                result.add(obj);
            }
        }
        return result;
    }
}

class GenericTableConfig {
    public String mainTableName;
    public String propertiesTableName;
    public boolean searchableDefault;
    public GenericPropertiesConfig properties;

    public boolean isSearchable(JsonPointer propPointer) {

        // More specific configuration takes precedence
        Boolean explicit = null;
        while (!propPointer.isEmpty() && explicit == null) {
            explicit = properties.explicitlySearchable.get(propPointer);
            propPointer = propPointer.parent();
        }
        
        if (explicit != null) {
            return explicit.booleanValue();
        } else {
            return searchableDefault;
        }
    }
    
    /**  
     * @return Approximation on whether this may have searchable properties
     * It is only an approximation as we do not have an exhaustive list of possible properties
     * to consider against a default setting of searchable.
     */
    public boolean hasPossibleSearchableProperties() {
        return ((searchableDefault) ? true : properties.explicitSearchableProperties);
    }

    public static GenericTableConfig parse(JsonValue tableConfig) {
        GenericTableConfig cfg = new GenericTableConfig();
        tableConfig.required();
        cfg.mainTableName = tableConfig.get("mainTable").required().asString();
        cfg.propertiesTableName = tableConfig.get("propertiesTable").required().asString();
        cfg.searchableDefault = tableConfig.get("searchableDefault").defaultTo(Boolean.TRUE).asBoolean();
        cfg.properties = GenericPropertiesConfig.parse(tableConfig.get("properties"));

        return cfg;
    }
}

class GenericPropertiesConfig {
    public Map<JsonPointer, Boolean> explicitlySearchable = new HashMap<JsonPointer, Boolean>();
    public String mainTableName;
    public String propertiesTableName;
    public boolean searchableDefault;
    public GenericPropertiesConfig properties;
    // Whether there are any properties explicitly set to searchable true
    public boolean explicitSearchableProperties;

    public static GenericPropertiesConfig parse(JsonValue propsConfig) {
        
        GenericPropertiesConfig cfg = new GenericPropertiesConfig();
        if (!propsConfig.isNull()) {
            for (String propName : propsConfig.keys()) {
                JsonValue detail = propsConfig.get(propName);
                boolean propSearchable = detail.get("searchable").asBoolean();
                cfg.explicitlySearchable.put(new JsonPointer(propName), propSearchable);
                if (propSearchable) {
                    cfg.explicitSearchableProperties = true;
                }
            }
        }

        return cfg;
    }
}
