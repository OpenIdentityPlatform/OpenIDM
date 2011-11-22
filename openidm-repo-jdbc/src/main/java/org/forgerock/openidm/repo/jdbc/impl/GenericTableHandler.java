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
package org.forgerock.openidm.repo.jdbc.impl;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.repo.jdbc.ErrorType;
import org.forgerock.openidm.repo.jdbc.TableHandler;
import org.forgerock.openidm.repo.jdbc.impl.query.GenericTableQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Handling of tables in a generic (not object specific) layout
 *
 * @author aegloff
 */
public class GenericTableHandler implements TableHandler {
    final static Logger logger = LoggerFactory.getLogger(GenericTableHandler.class);

    GenericTableConfig cfg;
    
    final String mainTableName;
    String propTableName;
    final String dbSchemaName;

    ObjectMapper mapper = new ObjectMapper();
    final GenericTableQueries queries;

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

    public GenericTableHandler(JsonValue tableConfig, String dbSchemaName, JsonValue queriesConfig, int maxBatchSize) {
        cfg = GenericTableConfig.parse(tableConfig);
        
        this.mainTableName = cfg.mainTableName;
        this.propTableName = cfg.propertiesTableName;
        this.dbSchemaName = dbSchemaName;
        if (maxBatchSize < 1) {
            this.maxBatchSize = 1;
        } else {
            this.maxBatchSize = maxBatchSize;
        }

        queries = new GenericTableQueries();
        queryMap = Collections.unmodifiableMap(initializeQueryMap());
        queries.setConfiguredQueries(mainTableName, propTableName, dbSchemaName, queriesConfig, queryMap);
        
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
        result.put(QueryDefinition.PROPDELETEQUERYSTR, "DELETE FROM " + propertyTable + " WHERE " + mainTableName + "_id = (SELECT obj.id FROM " + mainTable + " obj, " + typeTable + " objtype WHERE obj.objecttypes_id = objtype.id AND objtype.objecttype = ? AND obj.objectid  = ?)");

        // Default object queries
        String tableVariable =  dbSchemaName == null ? "${_mainTable}" : "${_dbSchema}.${_mainTable}";
        result.put(QueryDefinition.QUERYALLIDS, "SELECT obj.objectid FROM " + tableVariable + " obj INNER JOIN " + typeTable + " objtype ON obj.objecttypes_id = objtype.id WHERE objtype.objecttype = ${_resource}");

        return result;
    }

    /* (non-Javadoc)
    * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#read(java.lang.String, java.lang.String, java.lang.String, java.sql.Connection)
    */
    @Override
    public Map<String, Object> read(String fullId, String type, String localId, Connection connection)
            throws NotFoundException, SQLException, IOException {

        Map<String, Object> result = null;
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
                ObjectMapper mapper = new ObjectMapper();
                result = (Map<String, Object>) mapper.readValue(objString, Map.class);
                result.put("_rev", rev);
                logger.debug(" full id: {}, rev: {}, obj {}", new Object[]{fullId, rev, result});
            } else {
                throw new NotFoundException("Object " + fullId + " not found in " + type);
            }
        } finally {
            CleanupHelper.loggedClose(rs);
            CleanupHelper.loggedClose(readStatement);
        }

        return result;
    }

    /* (non-Javadoc)
    * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#create(java.lang.String, java.lang.String, java.lang.String, java.util.Map, java.sql.Connection)
    */
    @Override
    public void create(String fullId, String type, String localId, Map<String, Object> obj, Connection connection)
            throws SQLException, IOException, InternalServerErrorException {
        // Do this outside of the main tx.
        connection.setAutoCommit(true);
        long typeId = getTypeId(type, connection);

        connection.setAutoCommit(false);

        PreparedStatement createStatement = null;
        try {
            createStatement = queries.getPreparedStatement(connection, queryMap.get(QueryDefinition.CREATEQUERYSTR), true);
    
            logger.debug("Create with fullid {}", fullId);
            String rev = "0";
            obj.put("_id", localId); // Save the id in the object
            obj.put("_rev", rev); // Save the rev in the object, and return the changed rev from the create.
            String objString = mapper.writeValueAsString(obj);
    
            logger.trace("Populating statement {} with params {}, {}, {}, {}",
                    new Object[]{createStatement, typeId, localId, rev, objString});
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
        if (cfg.searchableDefault) {
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
                    Object val = entry.getValue();
                    if (val != null) {
                        propvalue = val.toString(); // TODO: proper type conversions?
                    }
                    String proptype = null;
                    if (propvalue != null) {
                        proptype = entry.getValue().getClass().getName(); // TODO: proper type info
                    }
                    if (logger.isTraceEnabled()) {
                        logger.trace("Populating statement {} with params {}, {}, {}, {}, {}",
                                new Object[]{propCreateStatement, dbId, localId, propkey, proptype, propvalue});
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
                        logger.trace("Inserting objectproperty id: {} propkey: {} proptype: {}, propvalue: {}", new Object[]{fullId, propkey, proptype, propvalue});
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
        return XOpenErrorMapping.isErrorType(ex, errorType);
    }
    
    public boolean isRetryable(SQLException ex, Connection connection) {
        if (isErrorType(ex, ErrorType.CONNECTION_FAILURE) || isErrorType(ex, ErrorType.DEADLOCK_OR_TIMEOUT)) {
            // TODO: this is known retry-able for MySQL, review good defaults in general
            return true;
        } else {
            return false;
        }
    }
    
    // Ensure type is in objecttypes table and get its assigned id
    long getTypeId(String type, Connection connection) throws SQLException, InternalServerErrorException {
        Exception detectedEx = null;
        long typeId = readTypeId(type, connection);
        if (typeId < 0) {
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
    
            logger.trace("Populating prepared statement {} for {}", readTypeStatement, type);
            readTypeStatement.setString(1, type);
    
            logger.debug("Executing: {}", readTypeStatement);
            rs = readTypeStatement.executeQuery();
            if (rs.next()) {
                typeId = rs.getLong("id");
                logger.debug("Type: {}, id: {}", new Object[]{type, typeId});
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
     * @param fullId
     * @param type
     * @param localId
     * @param connection
     * @return the row for the requested object, selected FOR UPDATE
     * @throws NotFoundException if the requested object was not found in the DB
     * @throws java.sql.SQLException
     */
    public ResultSet readForUpdate(String fullId, String type, String localId, Connection connection)
            throws NotFoundException, SQLException {

        PreparedStatement readForUpdateStatement = null; // Statement currently implicitly closed when rs closes
        readForUpdateStatement = getPreparedStatement(connection, QueryDefinition.READFORUPDATEQUERYSTR);
        logger.trace("Populating prepared statement {} for {}", readForUpdateStatement, fullId);
        readForUpdateStatement.setString(1, type);
        readForUpdateStatement.setString(2, localId);

        logger.debug("Executing: {}", readForUpdateStatement);
        ResultSet rs = readForUpdateStatement.executeQuery();
        if (rs.next()) {
            logger.debug("Read for update full id: {}", fullId);
            return rs;
        } else {
            throw new NotFoundException("Object " + fullId + " not found in " + type);
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
            logger.debug("Update existing object {} rev: {} db id: {}, object type db id: {}", new Object[]{fullId, existingRev, dbId, objectTypeDbId});
    
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
    
            logger.trace("Populating prepared statement {} for {} {} {} {} {}", new Object[]{updateStatement, fullId, newLocalId, newRev, objString, dbId});
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
            logger.trace("Populating prepared statement {} for {} {} {}", new Object[]{deletePropStatement, fullId, type, localId});
            deletePropStatement.setString(1, type);
            deletePropStatement.setString(2, localId);
            logger.debug("Update properties del statement: {}", deletePropStatement);
            int deleteCount = deletePropStatement.executeUpdate();
            logger.trace("Deleted child rows: {} for: {}", deleteCount, fullId);
            writeValueProperties(fullId, dbId, localId, jv, connection);
        } finally {
            CleanupHelper.loggedClose(rs);
            CleanupHelper.loggedClose(updateStatement);
            CleanupHelper.loggedClose(deletePropStatement);
        }
    }

    /* (non-Javadoc)
     * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#delete(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.sql.Connection)
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
            logger.trace("Populating prepared statement {} for {} {} {} {}", new Object[]{deleteStatement, fullId, type, localId, rev});
    
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
            CleanupHelper.loggedClose(existing);
            CleanupHelper.loggedClose(deleteStatement);
        }
    }

    /* (non-Javadoc)
     * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#delete(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.sql.Connection)
     */
    @Override
    public List<Map<String, Object>> query(String type, Map<String, Object> params, Connection connection)
            throws ObjectSetException {
        return queries.query(type, params, connection);
    }

    @Override
    public String toString() {
        return "Generic handler mapped to [" + mainTableName + ", " + propTableName + "]";
    }

    protected PreparedStatement getPreparedStatement(Connection connection, QueryDefinition queryDefinition) throws SQLException {
        return queries.getPreparedStatement(connection, queryMap.get(queryDefinition));
    }
}

class GenericTableConfig {
    public String mainTableName;
    public String propertiesTableName;
    public boolean searchableDefault;
    public GenericPropertiesConfig properties;
    
    public boolean isSearchable(JsonPointer propPointer) {
        Boolean explicit = properties.explicitlySearchable.get(propPointer);
        if (explicit != null) {
            return explicit.booleanValue();
        } else {
            return searchableDefault;
        }
    }
    
    public static GenericTableConfig parse(JsonValue tableConfig) {
        GenericTableConfig cfg = new GenericTableConfig();
        tableConfig.required();
        cfg.mainTableName = tableConfig.get("mainTable").required().asString();
        cfg.propertiesTableName = tableConfig.get("propertiesTable").required().asString();
        cfg.searchableDefault = tableConfig.get("searchableDefault").defaultTo(Boolean.TRUE).asBoolean().booleanValue();
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
    
    public static GenericPropertiesConfig parse(JsonValue propsConfig) {
        GenericPropertiesConfig cfg = new GenericPropertiesConfig();
        if (!propsConfig.isNull()) {
            for (String propName : propsConfig.keys()) {
                JsonValue detail = propsConfig.get(propName);
                cfg.explicitlySearchable.put(new JsonPointer(propName), detail.get("searchable").asBoolean());
            }
        }
        return cfg;
    }
}
