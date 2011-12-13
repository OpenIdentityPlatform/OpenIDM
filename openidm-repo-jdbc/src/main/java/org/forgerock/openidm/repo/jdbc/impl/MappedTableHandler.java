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
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.repo.jdbc.ErrorType;
import org.forgerock.openidm.repo.jdbc.SQLExceptionHandler;
import org.forgerock.openidm.repo.jdbc.TableHandler;
import org.forgerock.openidm.repo.jdbc.impl.GenericTableHandler.QueryDefinition;
import org.forgerock.openidm.repo.jdbc.impl.query.TableQueries;
import org.forgerock.openidm.repo.jdbc.impl.query.QueryResultMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handling of tables in a generic (not object specific) layout
 * @author aegloff
 */
public class MappedTableHandler implements TableHandler {
    final static Logger logger = LoggerFactory.getLogger(MappedTableHandler.class);

    SQLExceptionHandler sqlExceptionHandler;
    
    final String tableName;
    String dbSchemaName;

    final LinkedHashMap<String, String> rawMappingConfig;
    Mapping explicitMapping;
    
    // The json pointer (used as names) of the properties to replace the ? tokens in the prepared statement, 
    // in the order they need populating in create and update queries
    List<JsonPointer> tokenReplacementPropPointers = new ArrayList<JsonPointer>();
    
    ObjectMapper mapper = new ObjectMapper();
    final TableQueries queries;

    String readQueryStr;
    String readForUpdateQueryStr;
    String createQueryStr;
    String updateQueryStr;
    String deleteQueryStr;
    
    public MappedTableHandler(String tableName, Map mapping, String dbSchemaName, JsonValue queriesConfig, SQLExceptionHandler sqlExceptionHandler) {
        this.tableName = tableName;
        this.dbSchemaName = dbSchemaName;
        // Maintain a stable ordering
        this.rawMappingConfig = new LinkedHashMap<String, String>();
        this.rawMappingConfig.putAll(mapping);
        
        explicitMapping = new Mapping(tableName, new JsonValue(rawMappingConfig));
        logger.debug("Explicit mapping: {}", explicitMapping);
        
        if (sqlExceptionHandler == null) {
            this.sqlExceptionHandler = new DefaultSQLExceptionHandler();
        } else {
            this.sqlExceptionHandler = sqlExceptionHandler;
        }
        
        queries = new TableQueries(new ExplicitQueryResultMapper(explicitMapping));
        queries.setConfiguredQueries(tableName, dbSchemaName, queriesConfig, null);
        
        String mainTable = dbSchemaName == null ? tableName : dbSchemaName + "." + tableName;
        
        StringBuffer colNames = new StringBuffer();
        StringBuffer tokenNames = new StringBuffer();
        StringBuffer prepTokens = new StringBuffer();
        StringBuffer updateAssign = new StringBuffer();
        boolean isFirst = true;

        for (ColumnMapping colMapping : explicitMapping.columnMappings) {
            if (!isFirst) {
                colNames.append(", ");
                tokenNames.append(",");
                prepTokens.append(",");
                updateAssign.append(", ");
            }
            colNames.append(colMapping.dbColName);
            tokenNames.append("${").append(colMapping.objectColName).append("}");
            prepTokens.append("?");
            tokenReplacementPropPointers.add(colMapping.objectColPointer);
            //updateAssign.append(colMapping.dbColName).append(" = ${").append(colMapping.objectColName).append("}");
            updateAssign.append(colMapping.dbColName).append(" = ?");
            isFirst = false;
        }
        
        readQueryStr = "SELECT * FROM " + mainTable + " WHERE objectid = ?";
        readForUpdateQueryStr = "SELECT * FROM " + mainTable + " WHERE objectid = ? FOR UPDATE";
        createQueryStr = "INSERT INTO " + mainTable + " (" + colNames + ") VALUES ( " + prepTokens + ")";
        updateQueryStr = "UPDATE " + mainTable + " SET " + updateAssign + " WHERE objectid = ?";
        deleteQueryStr = "DELETE FROM " + mainTable + " WHERE objectid = ? AND rev = ?";
        
        logger.debug("Unprepared query strings {} {} {} {} {}", new Object[] {readQueryStr, createQueryStr, updateQueryStr, deleteQueryStr});
        
    }
    
    /**
     * @see org.forgerock.openidm.repo.jdbc.TableHandler#read(java.lang.String, java.lang.String, java.lang.String, java.sql.Connection)
     */
    @Override
    public Map<String, Object> read(String fullId, String type, String localId, Connection connection) 
                    throws NotFoundException, SQLException, IOException {
        JsonValue result = null;
        PreparedStatement readStatement = queries.getPreparedStatement(connection, readQueryStr);

        logger.debug("Populating prepared statement {} for {}", readStatement, fullId);
        readStatement.setString(1, localId);
        
        logger.debug("Executing: {}", readStatement);
        ResultSet rs = readStatement.executeQuery();
        if (rs.next()) {
            result = explicitMapping.mapToJsonValue(rs);
            Object rev = result.get("_rev");  
            logger.debug(" full id: {}, rev: {}, obj {}", new Object[] {fullId, rev, result});  
        } else {
            throw new NotFoundException("Object " + fullId + " not found in " + type);
        }
        
        return result.asMap();
    }
    
    /**
     * Reads and locks a record
     * @param fullId the object id to retrieve
     * @param type the object set type context
     * @param localId
     * @param connection
     * @return the row for the requested object, selected FOR UPDATE
     * @throws NotFoundException if the requested object was not found in the DB
     * @throws java.sql.SQLException if a database failure occurred
     */
    ResultSet readForUpdate(String fullId, String type, String localId, Connection connection)
            throws NotFoundException, SQLException {

        PreparedStatement readForUpdateStatement = null; // Statement currently implicitly closed when rs closes
        readForUpdateStatement = queries.getPreparedStatement(connection, readForUpdateQueryStr);
        logger.trace("Populating prepared statement {} for {}", readForUpdateStatement, fullId);
        readForUpdateStatement.setString(1, localId);

        logger.debug("Executing: {}", readForUpdateStatement);
        ResultSet rs = readForUpdateStatement.executeQuery();
        if (rs.next()) {
            logger.debug("Read for update full id: {}", fullId);
            return rs;
        } else {
            throw new NotFoundException("Object " + fullId + " not found in " + type);
        }
    }

    /**
     * @see org.forgerock.openidm.repo.jdbc.TableHandler#create(java.lang.String, java.lang.String, java.lang.String, java.util.Map, java.sql.Connection)
     */    
    @Override
    public void create(String fullId, String type, String localId, Map<String, Object> obj, Connection connection) 
                throws SQLException, IOException {
        connection.setAutoCommit(false);
        PreparedStatement createStatement = queries.getPreparedStatement(connection, createQueryStr);

        logger.debug("Create with fullid {}", fullId);
        String rev = "0";
        obj.put("_id", localId); // Save the id in the object
        obj.put("_rev", rev); // Save the rev in the object, and return the changed rev from the create.
        JsonValue objVal = new JsonValue(obj);

        logger.debug("Preparing statement {} with {}, {}, {}", 
                new Object[]{ createStatement, type, localId, rev });
        populatePrepStatementColumns(createStatement, objVal, tokenReplacementPropPointers);
        
        logger.debug("Executing: {}", createStatement);
        int val = createStatement.executeUpdate();
        
        logger.debug("Created object for id {} with rev {}", fullId, rev);
    }
    
    /**
     * Populates the create or update statement with the token replacement values in the appropriate order.
     * For update the final objectid is not part of the declarative mapping and needs to be populated separately.
     * @param prepStatement the update or create prepared statement
     * @param tokenPointers the token replacement pointers pointing into the object set to extract the relevant values
     * @return the next column position if further populating is desired
     */
    int populatePrepStatementColumns(PreparedStatement prepStatement, JsonValue objVal, List<JsonPointer> tokenPointers) 
            throws SQLException{
        int colPos = 1;
        for (JsonPointer propPointer : tokenPointers) {
            // TODO: support explicit column types/conversion specified in column mapping
            // This is currently limited to STRING handling
            JsonValue rawValue = objVal.get(propPointer);
            String propValue = null;
            if (null == rawValue) {
                propValue = null;
            } else if (rawValue.isString() || rawValue.isNull()) {
                propValue = rawValue.asString();
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("Value for col {} from {} is getting Stringified from type {} to store in a STRING column as value: {}", new Object[] {colPos, propPointer, rawValue.getClass(), rawValue});
                }
                propValue = rawValue.getObject().toString(); 
            }

            prepStatement.setString(colPos, propValue);
            colPos++;
        }
        return colPos;
    }

    /**
     * @see org.forgerock.openidm.repo.jdbc.TableHandler#update(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Map, java.sql.Connection)
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
         try {
             rs = readForUpdate(fullId, type, localId, connection);
             String existingRev = explicitMapping.getRev(rs);
             logger.debug("Update existing object {} rev: {} ", fullId, existingRev);
     
             if (!existingRev.equals(rev)) {
                 throw new PreconditionFailedException("Update rejected as current Object revision " + existingRev + " is different than expected by caller (" + rev + "), the object has changed since retrieval.");
             }
             updateStatement = queries.getPreparedStatement(connection, updateQueryStr);
     
             // Support changing object identifier
             String newLocalId = (String) obj.get("_id");
             if (newLocalId != null && !localId.equals(newLocalId)) {
                 logger.debug("Object identifier is changing from " + localId + " to " + newLocalId);
             } else {
                 newLocalId = localId; // If it hasn't changed, use the existing ID
                 obj.put("_id", newLocalId); // Ensure the ID is saved in the object
             }
             
             JsonValue objVal = new JsonValue(obj);
             logger.trace("Populating prepared statement {} for {} {} {}", new Object[]{updateStatement, fullId, newLocalId, newRev});
             int nextCol = populatePrepStatementColumns(updateStatement, objVal, tokenReplacementPropPointers);
             updateStatement.setString(nextCol, localId);
             logger.debug("Update statement: {}", updateStatement);
             int updateCount = updateStatement.executeUpdate();
             logger.trace("Updated rows: {} for {}", updateCount, fullId);
             if (updateCount != 1) {
                 throw new InternalServerErrorException("Update execution did not result in updating 1 row as expected. Updated rows: " + updateCount);
             }
         } finally {
             CleanupHelper.loggedClose(rs);
             CleanupHelper.loggedClose(updateStatement);
         }
     }
     
    /**
     * @see org.forgerock.openidm.repo.jdbc.TableHandler#delete(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.sql.Connection)
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
            String existingRev = explicitMapping.getRev(existing);
            if (!"*".equals(rev) && !rev.equals(existingRev)) {
                throw new PreconditionFailedException("Delete rejected as current Object revision " + existingRev + " is different than "
                        + "expected by caller " + rev + ", the object has changed since retrieval.");
            }
    
            // Proceed with the valid delete
            deleteStatement = queries.getPreparedStatement(connection, deleteQueryStr);
            logger.trace("Populating prepared statement {} for {} {} {} {}", new Object[]{deleteStatement, fullId, type, localId, rev});
    
            deleteStatement.setString(1, localId);
            deleteStatement.setString(2, rev);
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

    /**
     * @see org.forgerock.openidm.repo.jdbc.TableHandler#delete(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.sql.Connection)
     */ 
    @Override
    public List<Map<String, Object>> query(String type, Map<String, Object> params, Connection connection) 
                throws ObjectSetException {
        return queries.query(type, params, connection); 
    } 

    // TODO: make common to generic and explicit handlers
    /**
     * @inheritDoc
     */
    public boolean isErrorType(SQLException ex, ErrorType errorType) {
        return sqlExceptionHandler.isErrorType(ex, errorType);
    }
    
    // TODO: make common to generic and explicit handlers
    /**
     * InheritDoc
     */
    public boolean isRetryable(SQLException ex, Connection connection) {
        return sqlExceptionHandler.isRetryable(ex, connection);
    }
    
    
    @Override
    public String toString() {
        return "Generic handler mapped to " + tableName + " and mapping " + rawMappingConfig; 
    }
}

/**
 * Handle the conversion of query results to the object set model
 */
class ExplicitQueryResultMapper implements QueryResultMapper {
    final static Logger logger = LoggerFactory.getLogger(ExplicitQueryResultMapper.class);
    Mapping explicitMapping;
    
    public ExplicitQueryResultMapper(Mapping explicitMapping) {
        this.explicitMapping = explicitMapping;
    }
    
    public List<Map<String, Object>> mapQueryToObject(ResultSet rs, String queryId, String type, Map<String, Object> params,  TableQueries tableQueries) 
            throws SQLException {

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        while (rs.next()) {
            JsonValue obj = explicitMapping.mapToJsonValue(rs);
            result.add(obj.asMap());
        }
        return result;
    }
}

/**
 *  Parsed Config handling
 */
class Mapping {
    
    final static Logger logger = LoggerFactory.getLogger(Mapping.class);

    String tableName;
    List<ColumnMapping> columnMappings = new ArrayList();
    ColumnMapping revMapping; // Quick access to mapping for MVCC revision 
    
    public Mapping(String tableName, JsonValue mappingConfig) {
        this.tableName = tableName;
        for (Map.Entry<String, Object> entry : mappingConfig.asMap().entrySet()) {
            String key = entry.getKey();
            JsonValue value = mappingConfig.get(key);
            ColumnMapping colMapping = new ColumnMapping(key, value);
            columnMappings.add(colMapping);
            if ("_rev".equals(colMapping.objectColName)) {
                revMapping = colMapping;
            }
        }
    }
    
    public JsonValue mapToJsonValue(ResultSet rs) throws SQLException {
        JsonValue mappedResult = new JsonValue(new LinkedHashMap<String, Object>());
        
        for (ColumnMapping entry: columnMappings) {
            Object value = null;
            if ("STRING".equals(entry.dbColType)) { 
                value = rs.getString(entry.dbColName);
            } else {
                // TODO: support for more complex type conversions
                value = rs.getObject(entry.dbColName);
            }
            mappedResult.put(entry.objectColPointer, value);
        }
        logger.debug("Mapped rs {} to {}", rs, mappedResult);
        return mappedResult; 
    }
    
    public String getRev(ResultSet rs) throws SQLException {
        return rs.getString(revMapping.dbColName);
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Explicit table mapping for " + tableName + " :\n");
        for (ColumnMapping entry: columnMappings) {
            sb.append(entry.toString());
        }
        return sb.toString();
    }
}

/**
 *  Parsed Config handling
 */
class ColumnMapping {
    public JsonPointer objectColPointer;
    public String objectColName; // String representation of the column name/path
    public String dbColName;
    public String dbColType;
    
    public ColumnMapping(String objectColName, JsonValue dbColMappingConfig) {
        this.objectColName = objectColName;
        this.objectColPointer = new JsonPointer(objectColName);
        if (dbColMappingConfig.required().isList()) {
            if (dbColMappingConfig.asList().size() != 2) {
                throw new InvalidException("Explicit table mapping has invalid entry for " + objectColName 
                        + ", expecting column name and type but contains " + dbColMappingConfig.asList());
            }
            dbColName = dbColMappingConfig.get(0).required().asString();
            dbColType = dbColMappingConfig.get(1).required().asString();
        } else {
            dbColName = dbColMappingConfig.asString();
            dbColType = "STRING";
        }
    }
    
    public String toString() {
        return "object column : " + objectColName + " -> " + dbColName + ":" + dbColType + "\n";
    }
}
