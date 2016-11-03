/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.openidm.repo.jdbc.impl;

import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.repo.QueryConstants.PAGED_RESULTS_OFFSET;
import static org.forgerock.openidm.repo.QueryConstants.PAGE_SIZE;
import static org.forgerock.openidm.repo.QueryConstants.SORT_KEYS;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.audit.util.JsonValueUtils;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.SortKey;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.repo.jdbc.Constants;
import org.forgerock.openidm.repo.jdbc.ErrorType;
import org.forgerock.openidm.repo.jdbc.SQLExceptionHandler;
import org.forgerock.openidm.repo.jdbc.TableHandler;
import org.forgerock.openidm.repo.jdbc.impl.query.TableQueries;
import org.forgerock.openidm.repo.util.StringSQLQueryFilterVisitor;
import org.forgerock.openidm.repo.util.StringSQLRenderer;
import org.forgerock.openidm.util.Accessor;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handling of tables in a generic (not object specific) layout
 */
public class MappedTableHandler implements TableHandler {
    final static Logger logger = LoggerFactory.getLogger(MappedTableHandler.class);

    SQLExceptionHandler sqlExceptionHandler;

    final String tableName;
    String dbSchemaName;

    final LinkedHashMap<String, Object> rawMappingConfig;
    final ExplicitResultSetMapper explicitMapping;
    private final QueryFilterVisitor<StringSQLRenderer, Map<String, Object>, JsonPointer> queryFilterVisitor;

    // The json pointer (used as names) of the properties to replace the ?
    // tokens in the prepared statement,
    // in the order they need populating in create and update queries
    List<JsonPointer> tokenReplacementPropPointers = new ArrayList<>();

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
        this.rawMappingConfig = new LinkedHashMap<>();
        this.rawMappingConfig.putAll(mapping);

        explicitMapping =
                new ExplicitResultSetMapper(tableName, new JsonValue(rawMappingConfig), cryptoServiceAccessor);
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

        queries = new TableQueries(this, tableName, null, dbSchemaName, 0, explicitMapping);
        queries.setConfiguredQueries(tableName, dbSchemaName, queriesConfig, commandsConfig, null);

        initializeQueries();
    }

    protected void initializeQueries() {
        final String mainTable = dbSchemaName == null ? tableName : dbSchemaName + "." + tableName;
        final StringBuffer colNames = new StringBuffer();
        final StringBuffer tokenNames = new StringBuffer();
        final StringBuffer prepTokens = new StringBuffer();
        final StringBuffer updateAssign = new StringBuffer();
        boolean isFirst = true;

        for (ColumnMapping colMapping : explicitMapping.getColumnMappings()) {
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
    public ResourceResponse read(String fullId, String type, String localId, Connection connection)
            throws NotFoundException, SQLException, InternalServerErrorException {

        PreparedStatement readStatement = null;
        ResultSet rs = null;
        try {
            readStatement = queries.getPreparedStatement(connection, readQueryStr);

            logger.debug("Populating prepared statement {} for {}", readStatement, fullId);
            readStatement.setString(1, localId);

            logger.debug("Executing: {}", readStatement);
            rs = readStatement.executeQuery();

            if (rs.next()) {
                JsonValue resultValue = explicitMapping.mapToJsonValue(rs, ExplicitResultSetMapper.getColumnNames(rs));
                JsonValue rev = resultValue.get(Constants.OBJECT_REV);
                logger.debug(" full id: {}, rev: {}, obj {}", fullId, rev, resultValue);
                return newResourceResponse(localId, rev.asString(), resultValue);
            } else {
                throw new NotFoundException("Object " + fullId + " not found in " + type);
            }
        } finally {
            CleanupHelper.loggedClose(rs);
            CleanupHelper.loggedClose(readStatement);
        }
    }

    /**
     * Reads an object with for update locking applied
     *
     * @param fullId
     *            qualified id of component type and id
     * @param type
     *            the component type
     * @param localId
     *            the id of the object within the component type
     * @param connection
     *            the connection to use
     * @return the row as a map of column name/value pairs for the requested object, selected FOR UPDATE
     * @throws NotFoundException
     *             if the requested object was not found in the DB
     * @throws java.sql.SQLException
     *             for general DB issues
     */
    Map<String, Object> readForUpdate(String fullId, String type, String localId, Connection connection)
            throws NotFoundException, InternalServerErrorException, SQLException {

        PreparedStatement readForUpdateStatement = null;
        ResultSet rs = null;
        try {
            readForUpdateStatement =
                    queries.getPreparedStatement(connection, readForUpdateQueryStr);
            logger.trace("Populating prepared statement {} for {}", readForUpdateStatement, fullId);
            readForUpdateStatement.setString(1, localId);

            logger.debug("Executing: {}", readForUpdateStatement);
            rs = readForUpdateStatement.executeQuery();
            if (rs.isBeforeFirst()) {
                return explicitMapping.mapToRawObject(rs).get(0);
            } else {
                throw new NotFoundException("Object " + fullId + " not found in " + type);
            }
        } finally {
            CleanupHelper.loggedClose(rs);
            CleanupHelper.loggedClose(readForUpdateStatement);
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
        obj.put(Constants.OBJECT_ID, localId); // Save the id in the object
        obj.put(Constants.OBJECT_REV, rev); // Save the rev in the object, and return the
                              // changed rev from the create.
        JsonValue objVal = new JsonValue(obj);

        logger.debug("Preparing statement {} with {}, {}, {}", createStatement, type, localId, rev);
        populatePrepStatementColumns(type, createStatement, objVal, tokenReplacementPropPointers);

        if (!batchCreate) {
            logger.debug("Executing: {}", createStatement);
            createStatement.executeUpdate();
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
     * @param type
     *            qualifier of the object to create/update
     * @param prepStatement
     *            the update or create prepared statement
     * @param objVal
     *            fields to map to the {@code prepStatement}
     * @param tokenPointers
     *            the token replacement pointers pointing into the object set to
     *            extract the relevant values
     * @return the buildNext column position if further populating is desired
     */
    int populatePrepStatementColumns(String type, PreparedStatement prepStatement, JsonValue objVal,
            List<JsonPointer> tokenPointers) throws IOException, SQLException {
        final JsonValue unmappedObjFields = objVal.copy();
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
                unmappedObjFields.remove(propPointer);
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace(
                            "Value for col {} from {} is getting Stringified from type {} to store in a STRING column as value: {}",
                            colPos, propPointer, rawValue.getClass(), rawValue);
                }
                propValue = mapper.writeValueAsString(rawValue.getObject());
                unmappedObjFields.remove(propPointer);
            }

            prepStatement.setString(colPos, propValue);
            colPos++;
        }
        if (!unmappedObjFields.asMap().isEmpty()) {
            // some tables don't map _id and _rev (e.g., audit)
            unmappedObjFields.remove(Constants.OBJECT_ID);
            unmappedObjFields.remove(Constants.OBJECT_REV);
            final Set<String> unmappedObjKeys = JsonValueUtils.flatten(unmappedObjFields).keySet();
            if (!unmappedObjKeys.isEmpty()) {
                // found unmapped fields in create/update request
                throw new BadRequestException("Unmapped fields " + unmappedObjKeys + " for type " + type
                        + " and table " + dbSchemaName + "." + tableName);
            }
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
        obj.put(Constants.OBJECT_REV, newRev); // Save the rev in the object, and return the
                                 // changed rev from the create.

        PreparedStatement updateStatement = null;
        try {
            JsonValue result = new JsonValue(readForUpdate(fullId, type, localId, connection));
            String existingRev = result.get(Constants.RAW_OBJECT_REV).asString();
            logger.debug("Update existing object {} rev: {} ", fullId, existingRev);

            if (!existingRev.equals(rev)) {
                throw new PreconditionFailedException("Update rejected as current Object revision "
                        + existingRev + " is different than expected by caller (" + rev
                        + "), the object has changed since retrieval.");
            }
            updateStatement = queries.getPreparedStatement(connection, updateQueryStr);

            // Support changing object identifier
            String newLocalId = (String) obj.get(Constants.OBJECT_ID);
            if (newLocalId != null && !localId.equals(newLocalId)) {
                logger.debug("Object identifier is changing from " + localId + " to " + newLocalId);
            } else {
                newLocalId = localId; // If it hasn't changed, use the existing
                                      // ID
                obj.put(Constants.OBJECT_ID, newLocalId); // Ensure the ID is saved in the
                                            // object
            }

            JsonValue objVal = new JsonValue(obj);
            logger.trace("Populating prepared statement {} for {} {} {}", updateStatement, fullId, newLocalId, newRev);
            int nextCol =
                    populatePrepStatementColumns(type, updateStatement, objVal, tokenReplacementPropPointers);
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
        PreparedStatement deleteStatement = null;
        try {
            String existingRev;
            try {
                JsonValue result = new JsonValue(readForUpdate(fullId, type, localId, connection));
                existingRev = result.get(Constants.RAW_OBJECT_REV).asString();
            } catch (NotFoundException ex) {
                throw new NotFoundException("Object does not exist for delete on: " + fullId, ex);
            }
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
    public String renderQueryFilter(QueryFilter<JsonPointer> filter, Map<String, Object> replacementTokens, Map<String, Object> params) {
        final String offsetParam = (String) params.get(PAGED_RESULTS_OFFSET);
        final String pageSizeParam = (String) params.get(PAGE_SIZE);
        String pageClause = " LIMIT " + pageSizeParam + " OFFSET " + offsetParam;

        // JsonValue-cheat to avoid an unchecked cast
        final List<SortKey> sortKeys = new JsonValue(params).get(SORT_KEYS).asList(SortKey.class);
        // Check for sort keys and build up order-by syntax
        if (sortKeys != null && sortKeys.size() > 0) {
            pageClause = " ORDER BY " + StringUtils.join(prepareSortKeyStatements(sortKeys), ", ") + pageClause;
        }

        return "SELECT obj.* FROM ${_dbSchema}.${_mainTable} obj"
                + getFilterString(filter, replacementTokens)
                + pageClause;
    }

    /**
     * Loops through sort keys constructing the key statements.
     *
     * @param sortKeys  a {@link List} of sort keys
     * @return a {@link List} to store ORDER BY keys
     */
    protected List<String> prepareSortKeyStatements(List<SortKey> sortKeys) {
        List<String> keys = new ArrayList<String>();
        for (int i = 0; i < sortKeys.size(); i++) {
            SortKey sortKey = sortKeys.get(i);
            keys.add(explicitMapping.getDbColumnName(sortKey.getField()) + (sortKey.isAscendingOrder() ? " ASC" : " DESC"));
        }
        return keys;
    }

    /**
     * Returns a query string representing the supplied filter.
     *
     * @param filter the {@link QueryFilter} object
     * @param replacementTokens replacement tokens for the query string
     * @return a query string
     */
    protected String getFilterString(QueryFilter<JsonPointer> filter, Map<String, Object> replacementTokens) {
        return " WHERE " + filter.accept(queryFilterVisitor, replacementTokens).toSQL();
    }
}
