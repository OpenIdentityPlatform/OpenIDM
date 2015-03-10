/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All Rights Reserved
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

import static org.forgerock.openidm.repo.QueryConstants.PAGED_RESULTS_OFFSET;
import static org.forgerock.openidm.repo.QueryConstants.PAGE_SIZE;
import static org.forgerock.openidm.repo.QueryConstants.SORT_KEYS;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryFilterVisitor;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.SortKey;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.repo.jdbc.ErrorType;
import org.forgerock.openidm.repo.jdbc.SQLExceptionHandler;
import org.forgerock.openidm.repo.jdbc.TableHandler;
import org.forgerock.openidm.repo.jdbc.impl.query.QueryResultMapper;
import org.forgerock.openidm.repo.jdbc.impl.query.TableQueries;
import org.forgerock.openidm.repo.util.StringSQLQueryFilterVisitor;
import org.forgerock.openidm.repo.util.StringSQLRenderer;
import org.forgerock.openidm.util.Accessor;
import org.forgerock.openidm.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handling of tables in a generic (not object specific) layout
 */
public class MappedTableHandler implements TableHandler {
    final static Logger logger = LoggerFactory.getLogger(MappedTableHandler.class);

    SQLExceptionHandler sqlExceptionHandler;

    final String tableName;
    String dbSchemaName;

    final LinkedHashMap<String, Object> rawMappingConfig;
    final Mapping explicitMapping;
    private final QueryFilterVisitor<StringSQLRenderer, Map<String, Object>> queryFilterVisitor;

    // The json pointer (used as names) of the properties to replace the ?
    // tokens in the prepared statement,
    // in the order they need populating in create and update queries
    List<JsonPointer> tokenReplacementPropPointers = new ArrayList<JsonPointer>();

    ObjectMapper mapper = new ObjectMapper();
    final TableQueries queries;

    String readQueryStr;
    String readForUpdateQueryStr;
    String createQueryStr;
    String updateQueryStr;
    String deleteQueryStr;

    public MappedTableHandler(String tableName, Map<String, Object> mapping, String dbSchemaName,
            JsonValue queriesConfig, JsonValue commandsConfig, SQLExceptionHandler sqlExceptionHandler,
            Accessor<CryptoService> cryptoServiceAccessor) throws InternalServerErrorException {

        // TODO Replace this with a "guarantee" somewhere when/if the provision of this accessor becomes more automatic
        if (cryptoServiceAccessor == null)
            throw new InternalServerErrorException("No CryptoServiceAccessor found!");

        this.tableName = tableName;
        this.dbSchemaName = dbSchemaName;
        // Maintain a stable ordering
        this.rawMappingConfig = new LinkedHashMap<String, Object>();
        this.rawMappingConfig.putAll(mapping);

        explicitMapping =
                new Mapping(tableName, new JsonValue(rawMappingConfig), cryptoServiceAccessor);
        logger.debug("Explicit mapping: {}", explicitMapping);

        if (sqlExceptionHandler == null) {
            this.sqlExceptionHandler = new DefaultSQLExceptionHandler();
        } else {
            this.sqlExceptionHandler = sqlExceptionHandler;
        }

        queryFilterVisitor =
                new StringSQLQueryFilterVisitor<Map<String, Object>>() {
                    // value number for each value placeholder
                    int objectNumber = 0;
                    @Override
                    public StringSQLRenderer visitValueAssertion(Map<String, Object> objects, String operand, JsonPointer field, Object valueAssertion) {
                        ++objectNumber;
                        String value = "v"+objectNumber;
                        objects.put(value, valueAssertion);
                        return new StringSQLRenderer(
                                explicitMapping.getDbColumnName(field) + " " + operand + " ${" + value + "}");
                    }

                    @Override
                    public StringSQLRenderer visitPresentFilter(Map<String, Object> objects, JsonPointer field) {
                        return new StringSQLRenderer(explicitMapping.getDbColumnName(field) + " IS NOT NULL");
                    }
                };

        queries = new TableQueries(this, tableName, null, dbSchemaName, 0, new ExplicitQueryResultMapper(explicitMapping));
        queries.setConfiguredQueries(tableName, dbSchemaName, queriesConfig, commandsConfig, null);

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
            // updateAssign.append(colMapping.dbColName).append(" = ${").append(colMapping.objectColName).append("}");
            updateAssign.append(colMapping.dbColName).append(" = ?");
            isFirst = false;
        }

        readQueryStr = "SELECT * FROM " + mainTable + " WHERE objectid = ?";
        readForUpdateQueryStr = "SELECT * FROM " + mainTable + " WHERE objectid = ? FOR UPDATE";
        createQueryStr =
                "INSERT INTO " + mainTable + " (" + colNames + ") VALUES ( " + prepTokens + ")";
        updateQueryStr = "UPDATE " + mainTable + " SET " + updateAssign + " WHERE objectid = ?";
        deleteQueryStr = "DELETE FROM " + mainTable + " WHERE objectid = ? AND rev = ?";

        logger.debug("Unprepared query strings {} {} {} {} {}",
                readQueryStr, createQueryStr, updateQueryStr, deleteQueryStr);

    }

    /**
     * @see org.forgerock.openidm.repo.jdbc.TableHandler#read(java.lang.String,
     *      java.lang.String, java.lang.String, java.sql.Connection)
     */
    @Override
    public Resource read(String fullId, String type, String localId, Connection connection)
            throws NotFoundException, SQLException, IOException, InternalServerErrorException {
        JsonValue resultValue = null;
        Resource result = null;
        PreparedStatement readStatement = null;
        ResultSet rs = null;
        try {
            readStatement = queries.getPreparedStatement(connection, readQueryStr);

            logger.debug("Populating prepared statement {} for {}", readStatement, fullId);
            readStatement.setString(1, localId);

            logger.debug("Executing: {}", readStatement);
            rs = readStatement.executeQuery();

            if (rs.next()) {
                resultValue = explicitMapping.mapToJsonValue(rs, Mapping.getColumnNames(rs));
                JsonValue rev = resultValue.get("_rev");
                logger.debug(" full id: {}, rev: {}, obj {}", fullId, rev, resultValue);
                result = new Resource(localId, rev.asString(), resultValue);
            } else {
                throw new NotFoundException("Object " + fullId + " not found in " + type);
            }
        } finally {
            CleanupHelper.loggedClose(rs);
            CleanupHelper.loggedClose(readStatement);
        }

        return result;
    }

    /**
     * Reads an object with for update locking applied
     *
     * Note: statement associated with the returned resultset is not closed upon
     * return. Aside from taking care to close the resultset it also is the
     * responsibility of the caller to close the associated statement. Although
     * the specification specifies that drivers/pools should close the statement
     * automatically, not all do this reliably.
     *
     * @param fullId
     *            qualified id of component type and id
     * @param type
     *            the component type
     * @param localId
     *            the id of the object within the component type
     * @param connection
     *            the connection to use
     * @return the row for the requested object, selected FOR UPDATE
     * @throws NotFoundException
     *             if the requested object was not found in the DB
     * @throws java.sql.SQLException
     *             for general DB issues
     */
    ResultSet readForUpdate(String fullId, String type, String localId, Connection connection)
            throws NotFoundException, SQLException {

        PreparedStatement readForUpdateStatement = null;
        ResultSet rs = null;
        try {
            readForUpdateStatement =
                    queries.getPreparedStatement(connection, readForUpdateQueryStr);
            logger.trace("Populating prepared statement {} for {}", readForUpdateStatement, fullId);
            readForUpdateStatement.setString(1, localId);

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

    /**
     * @see org.forgerock.openidm.repo.jdbc.TableHandler#create(java.lang.String,
     *      java.lang.String, java.lang.String, java.util.Map,
     *      java.sql.Connection)
     */
    @Override
    public void create(String fullId, String type, String localId, Map<String, Object> obj,
            Connection connection) throws SQLException, IOException {
        PreparedStatement createStatement =
                queries.getPreparedStatement(connection, createQueryStr);
        try {
            create(fullId, type, localId, obj, connection, createStatement, false);
        } finally {
            CleanupHelper.loggedClose(createStatement);
        }
    }

    /**
     * Adds the option to batch more than one create statement
     *
     * @param batchCreate
     *            if true just adds create to batched statements, does not
     *            execute. false the statement is executed directly
     * @see org.forgerock.openidm.repo.jdbc.TableHandler#create(java.lang.String,
     *      java.lang.String, java.lang.String, java.util.Map,
     *      java.sql.Connection) for the other parameters
     */
    protected void create(String fullId, String type, String localId, Map<String, Object> obj,
            Connection connection, PreparedStatement createStatement, boolean batchCreate)
            throws SQLException, IOException {

        logger.debug("Create with fullid {}", fullId);
        String rev = "0";
        obj.put("_id", localId); // Save the id in the object
        obj.put("_rev", rev); // Save the rev in the object, and return the
                              // changed rev from the create.
        JsonValue objVal = new JsonValue(obj);

        logger.debug("Preparing statement {} with {}, {}, {}", createStatement, type, localId, rev);
        populatePrepStatementColumns(createStatement, objVal, tokenReplacementPropPointers);

        if (!batchCreate) {
            logger.debug("Executing: {}", createStatement);
            int val = createStatement.executeUpdate();
            logger.debug("Created object for id {} with rev {}", fullId, rev);
        } else {
            createStatement.addBatch();
            logger.debug("Added create for object id {} with rev {} to batch", fullId, rev);
        }
    }

    /**
     * Populates the create or update statement with the token replacement
     * values in the appropriate order. For update the final objectid is not
     * part of the declarative mapping and needs to be populated separately.
     *
     * @param prepStatement
     *            the update or create prepared statement
     * @param tokenPointers
     *            the token replacement pointers pointing into the object set to
     *            extract the relevant values
     * @return the buildNext column position if further populating is desired
     */
    int populatePrepStatementColumns(PreparedStatement prepStatement, JsonValue objVal,
            List<JsonPointer> tokenPointers) throws IOException, SQLException {
        int colPos = 1;
        for (JsonPointer propPointer : tokenPointers) {
            // TODO: support explicit column types/conversion specified in
            // column mapping
            // This is currently limited to STRING handling
            JsonValue rawValue = objVal.get(propPointer);
            String propValue = null;
            if (null == rawValue) {
                propValue = null;
            } else if (rawValue.isString() || rawValue.isNull()) {
                propValue = rawValue.asString();
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace(
                            "Value for col {} from {} is getting Stringified from type {} to store in a STRING column as value: {}",
                            colPos, propPointer, rawValue.getClass(), rawValue);
                }
                propValue = mapper.writeValueAsString(rawValue.getObject());
            }

            prepStatement.setString(colPos, propValue);
            colPos++;
        }
        return colPos;
    }

    /**
     * @see org.forgerock.openidm.repo.jdbc.TableHandler#update(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String, java.util.Map,
     *      java.sql.Connection)
     */
    @Override
    public void update(String fullId, String type, String localId, String rev,
            Map<String, Object> obj, Connection connection) throws SQLException, IOException,
            PreconditionFailedException, NotFoundException, InternalServerErrorException {
        logger.debug("Update with fullid {}", fullId);

        int revInt = Integer.parseInt(rev);
        ++revInt;
        String newRev = Integer.toString(revInt);
        obj.put("_rev", newRev); // Save the rev in the object, and return the
                                 // changed rev from the create.

        ResultSet rs = null;
        PreparedStatement updateStatement = null;
        try {
            rs = readForUpdate(fullId, type, localId, connection);
            String existingRev = explicitMapping.getRev(rs);
            logger.debug("Update existing object {} rev: {} ", fullId, existingRev);

            if (!existingRev.equals(rev)) {
                throw new PreconditionFailedException("Update rejected as current Object revision "
                        + existingRev + " is different than expected by caller (" + rev
                        + "), the object has changed since retrieval.");
            }
            updateStatement = queries.getPreparedStatement(connection, updateQueryStr);

            // Support changing object identifier
            String newLocalId = (String) obj.get("_id");
            if (newLocalId != null && !localId.equals(newLocalId)) {
                logger.debug("Object identifier is changing from " + localId + " to " + newLocalId);
            } else {
                newLocalId = localId; // If it hasn't changed, use the existing
                                      // ID
                obj.put("_id", newLocalId); // Ensure the ID is saved in the
                                            // object
            }

            JsonValue objVal = new JsonValue(obj);
            logger.trace("Populating prepared statement {} for {} {} {}", updateStatement, fullId, newLocalId, newRev);
            int nextCol =
                    populatePrepStatementColumns(updateStatement, objVal,
                            tokenReplacementPropPointers);
            updateStatement.setString(nextCol, localId);
            logger.debug("Update statement: {}", updateStatement);
            int updateCount = updateStatement.executeUpdate();
            logger.trace("Updated rows: {} for {}", updateCount, fullId);
            if (updateCount != 1) {
                throw new InternalServerErrorException(
                        "Update execution did not result in updating 1 row as expected. Updated rows: "
                                + updateCount);
            }
        } finally {
            if (rs != null) {
                // Ensure associated statement also is closed
                Statement rsStatement = rs.getStatement();
                CleanupHelper.loggedClose(rs);
                CleanupHelper.loggedClose(rsStatement);
            }
            CleanupHelper.loggedClose(updateStatement);
        }
    }

    /**
     * @see org.forgerock.openidm.repo.jdbc.TableHandler#delete(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String,
     *      java.sql.Connection)
     */
    @Override
    public void delete(String fullId, String type, String localId, String rev, Connection connection)
            throws PreconditionFailedException, InternalServerErrorException, NotFoundException,
            SQLException, IOException {
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
                throw new PreconditionFailedException("Delete rejected as current Object revision "
                        + existingRev + " is different than " + "expected by caller " + rev
                        + ", the object has changed since retrieval.");
            }

            // Proceed with the valid delete
            deleteStatement = queries.getPreparedStatement(connection, deleteQueryStr);
            logger.trace("Populating prepared statement {} for {} {} {} {}", deleteStatement, fullId, type, localId, rev);

            deleteStatement.setString(1, localId);
            deleteStatement.setString(2, rev);
            logger.debug("Delete statement: {}", deleteStatement);

            int deletedRows = deleteStatement.executeUpdate();
            logger.trace("Deleted {} rows for id : {} {}", deletedRows, localId);
            if (deletedRows < 1) {
                throw new InternalServerErrorException("Deleting object for " + fullId
                        + " failed, DB reported " + deletedRows + " rows deleted");
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

    /**
     * @see org.forgerock.openidm.repo.jdbc.TableHandler#delete(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String,
     *      java.sql.Connection)
     */
    @Override
    public List<Map<String, Object>> query(String type, Map<String, Object> params,
            Connection connection) throws ResourceException {
        return queries.query(type, params, connection);
    }

    @Override
    public Integer command(String type, Map<String, Object> params, Connection connection) throws SQLException, ResourceException {
        return queries.command(type, params, connection);
    }

    @Override
    public boolean queryIdExists(String queryId) {
        return queries.queryIdExists(queryId);
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

    @Override
    public String renderQueryFilter(QueryFilter filter, Map<String, Object> replacementTokens, Map<String, Object> params) {
        final String offsetParam = (String) params.get(PAGED_RESULTS_OFFSET);
        final String pageSizeParam = (String) params.get(PAGE_SIZE);
        String pageClause = " LIMIT " + pageSizeParam + " OFFSET " + offsetParam;

        // JsonValue-cheat to avoid an unchecked cast
        final List<SortKey> sortKeys = new JsonValue(params).get(SORT_KEYS).asList(SortKey.class);
        // Check for sort keys and build up order-by syntax
        if (sortKeys != null && sortKeys.size() > 0) {
            List<String> keys = new ArrayList<String>();
            for (int i = 0; i < sortKeys.size(); i++) {
                SortKey sortKey = sortKeys.get(i);
                String tokenName = "sortKey" + i;
                keys.add("${" + tokenName + "}" + (sortKey.isAscendingOrder() ? " ASC" : " DESC"));
                replacementTokens.put(tokenName, sortKey.getField().toString().substring(1));
            }
            pageClause = " ORDER BY " + StringUtils.join(keys, ", ") + pageClause;
        }
        
        return "SELECT obj.* FROM ${_dbSchema}.${_mainTable} obj"
                + getFilterString(filter, replacementTokens)
                + pageClause;
    }
    
    /**
     * Loops through sort keys constructing the key statements.
     * 
     * @param sortKeys  a {@link List} of sort keys
     * @param keys a {@link List} to store ORDER BY keys
     * @param replacementTokens a {@link Map} containing replacement tokens for the {@link PreparedStatement}
     */
    protected void prepareSortKeyStatements(List<SortKey> sortKeys, List<String> keys, Map<String, Object> replacementTokens) {
        for (int i = 0; i < sortKeys.size(); i++) {
            SortKey sortKey = sortKeys.get(i);
            keys.add(explicitMapping.getDbColumnName(sortKey.getField()) + (sortKey.isAscendingOrder() ? " ASC" : " DESC"));
        }
    }
    
    /**
     * Returns a query string representing the supplied filter.
     * 
     * @param filter the {@link QueryFilter} object
     * @param replacementTokens replacement tokens for the query string
     * @return a query string
     */
    protected String getFilterString(QueryFilter filter, Map<String, Object> replacementTokens) {
        return " WHERE " + filter.accept(queryFilterVisitor, replacementTokens).toSQL();
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

    public List<Map<String, Object>> mapQueryToObject(ResultSet rs, String queryId, String type,
            Map<String, Object> params, TableQueries tableQueries) throws SQLException,
            InternalServerErrorException {

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Set<String> names = Mapping.getColumnNames(rs);
        while (rs.next()) {
            JsonValue obj = explicitMapping.mapToJsonValue(rs, names);
            result.add(obj.asMap());
        }
        return result;
    }
}

/**
 * Parsed Config handling
 */
class Mapping {

    final static Logger logger = LoggerFactory.getLogger(Mapping.class);

    String tableName;
    Accessor<CryptoService> cryptoServiceAccessor;
    List<ColumnMapping> columnMappings = new ArrayList<ColumnMapping>();
    ColumnMapping revMapping; // Quick access to mapping for MVCC revision
    ObjectMapper mapper = new ObjectMapper();

    public Mapping(String tableName, JsonValue mappingConfig, Accessor<CryptoService> cryptoServiceAccessor) {
        this.cryptoServiceAccessor = cryptoServiceAccessor;
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

    public JsonValue mapToJsonValue(ResultSet rs, Set<String> columnNames) throws SQLException,
    InternalServerErrorException {
        JsonValue mappedResult = new JsonValue(new LinkedHashMap<String, Object>());

        for (ColumnMapping entry : columnMappings) {
            Object value = null;
            if (columnNames.contains(entry.dbColName)) {
                if (ColumnMapping.TYPE_STRING.equals(entry.dbColType)) {
                    value = rs.getString(entry.dbColName);
                    if (cryptoServiceAccessor == null || cryptoServiceAccessor.access() == null) {
                        throw new InternalServerErrorException("CryptoService unavailable");
                    }
                    if (JsonUtil.isEncrypted((String) value)) {
                        value = convertToJson(entry.dbColName, "encrypted", (String)value, Map.class).asMap();
                    }
                } else if (ColumnMapping.TYPE_JSON_MAP.equals(entry.dbColType)) {
                    value = convertToJson(entry.dbColName, entry.dbColType, rs.getString(entry.dbColName), Map.class).asMap();
                } else if (ColumnMapping.TYPE_JSON_LIST.equals(entry.dbColType)) {
                    value = convertToJson(entry.dbColName, entry.dbColType, rs.getString(entry.dbColName), List.class).asList();
                } else {
                    throw new InternalServerErrorException("Unsupported DB column type " + entry.dbColType);
                }
                mappedResult.put(entry.objectColPointer, value);
            }
        }
        logger.debug("Mapped rs {} to {}", rs, mappedResult);
        return mappedResult;
    }

    private <T> JsonValue convertToJson(String name, String nameType, String value, Class<T> valueType) throws InternalServerErrorException {
        if (value != null) {
            try {
                return new JsonValue(mapper.readValue(value, valueType));
            } catch (IOException e) {
                throw new InternalServerErrorException("Unable to map " + nameType + " value for " + name, e);
            }
        }
        return new JsonValue(null);
    }

    public String getRev(ResultSet rs) throws SQLException {
        return rs.getString(revMapping.dbColName);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Explicit table mapping for " + tableName + " :\n");
        for (ColumnMapping entry : columnMappings) {
            sb.append(entry.toString());
        }
        return sb.toString();
    }

    public static Set<String> getColumnNames(ResultSet rs) throws SQLException {
        TreeSet<String> set = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            set.add(rs.getMetaData().getColumnName(i));
        }
        return set;
    }

    public String getDbColumnName(JsonPointer fieldName) {
        for (ColumnMapping column : columnMappings) {
            if (column.isJsonPointer(fieldName)) {
                return column.dbColName;
            }
        }
        throw new IllegalArgumentException("Unknown object field: " + fieldName.toString());
    }
}

/**
 * Parsed Config handling
 */
class ColumnMapping {
    public static final String DB_COLUMN_NAME = "column";
    public static final String DB_COLUMN_TYPE = "type";

    public static final String TYPE_STRING = "STRING";
    public static final String TYPE_JSON_MAP = "JSON_MAP";
    public static final String TYPE_JSON_LIST = "JSON_LIST";
   
    
    public JsonPointer objectColPointer;
    public String objectColName; // String representation of the column
                                 // name/path
    public String dbColName;
    public String dbColType;

    public ColumnMapping(String objectColName, JsonValue dbColMappingConfig) {
        this.objectColName = objectColName;
        this.objectColPointer = new JsonPointer(objectColName);
        if (dbColMappingConfig.required().isList()) {
            if (dbColMappingConfig.asList().size() != 2) {
                throw new InvalidException("Explicit table mapping has invalid entry for "
                        + objectColName + ", expecting column name and type but contains "
                        + dbColMappingConfig.asList());
            }
            dbColName = dbColMappingConfig.get(0).required().asString();
            dbColType = dbColMappingConfig.get(1).required().asString();
        } else if (dbColMappingConfig.isMap()) {
            dbColName = dbColMappingConfig.asMap().get(DB_COLUMN_NAME).toString();
            dbColType = dbColMappingConfig.asMap().get(DB_COLUMN_TYPE).toString();
        } else {
            dbColName = dbColMappingConfig.asString();
            dbColType = TYPE_STRING;
        }
    }

    public boolean isJsonPointer(JsonPointer fieldPointer) {
        return objectColPointer.equals(fieldPointer);
    }

    public String toString() {
        return "object column : " + objectColName + " -> " + dbColName + ":" + dbColType + "\n";
    }
}
