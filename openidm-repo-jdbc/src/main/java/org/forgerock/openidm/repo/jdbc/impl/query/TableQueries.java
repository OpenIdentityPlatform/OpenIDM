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

package org.forgerock.openidm.repo.jdbc.impl.query;

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.openidm.repo.QueryConstants.PAGED_RESULTS_OFFSET;
import static org.forgerock.openidm.repo.QueryConstants.PAGE_SIZE;
import static org.forgerock.openidm.repo.QueryConstants.QUERY_EXPRESSION;
import static org.forgerock.openidm.repo.QueryConstants.QUERY_FILTER;
import static org.forgerock.openidm.repo.QueryConstants.QUERY_ID;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.repo.jdbc.TableHandler;
import org.forgerock.openidm.repo.jdbc.impl.CleanupHelper;
import org.forgerock.openidm.repo.jdbc.impl.GenericTableHandler.QueryDefinition;
import org.forgerock.openidm.repo.util.TokenHandler;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configured and add-hoc query support on tables in generic (non-object
 * specific) layout
 *
 * Queries can contain tokens of the format ${token-name}
 *
 */
public class TableQueries {

    final static Logger logger = LoggerFactory.getLogger(TableQueries.class);

    public static final String PREFIX_INT = "int";
    
    public static final String PREFIX_LIST = "list";
    
    // Monitoring event name prefix
    static final String EVENT_RAW_QUERY_PREFIX = "openidm/internal/repo/jdbc/raw/query/";

    /**
     * Helper class to wrap configured queries/commands.
     */
    class ConfiguredQueries {
        private Map<String, QueryInfo> configured = new HashMap<String, QueryInfo>();

        void setConfiguredQueries(Map<String, String> replacements, JsonValue queriesConfig) {
            configured.clear();
            for (String queryName : queriesConfig.keys()) {
                String rawQuery = queriesConfig.get(queryName).required().asString();

                TokenHandler tokenHandler = new TokenHandler();
                // Replace the table name tokens.
                String tempQueryString = tokenHandler.replaceSomeTokens(rawQuery, replacements);

                // Convert to ? for prepared statement, populate token replacement info
                List<String> tokenNames = tokenHandler.extractTokens(tempQueryString);
                String queryString = tokenHandler.replaceTokens(tempQueryString, "?", PREFIX_LIST);

                QueryInfo queryInfo = new QueryInfo(queryString, tokenNames);
                configured.put(queryName, queryInfo);
                logger.info("Configured query converted to JDBC query {} and tokens {}", queryString,
                        tokenNames);
            }
        }

        /**
         * Returns the QueryInfo for a queryId.
         *
         * @param queryId the unique identifier of the parameterized, pre-defined query
         * @return the QueryInfo
         */
        QueryInfo getQueryInfo(String queryId) {
            return configured.get(queryId);
        }

        /**
         * Gets and resolves a query by id, using token substitution
         *
         * @param con The db connection
         * @param queryId the unique identifier of the paramteerized, pre-defined query
         * @param type the resource component name targeted by the URI
         * @param params the parameters passed into the query call
         * @return The statement
         * @throws SQLException if resolving the statement failed
         * @throws BadRequestException if no query is defined for the given identifier
         */
        PreparedStatement getQuery(Connection con, String queryId, String type,
                Map<String, Object> params) throws SQLException, ResourceException {

            QueryInfo foundInfo = getQueryInfo(queryId);
            if (foundInfo == null) {
                throw new BadRequestException("No query defined/configured for requested queryId " + queryId);
            }
            return resolveQuery(foundInfo, con, params);
        }

        /**
         * Check if a {@code queryId} is present in the set of configured configured.
         *
         * @param queryId Id of the query to check for
         *
         * @return true if the queryId is present in the set of configured configured.
         */
        public boolean queryIdExists(final String queryId) {
            return configured.containsKey(queryId);
        }
    }

    /** Configured queries */
    final ConfiguredQueries queries = new ConfiguredQueries();

    /** Configured commands */
    final ConfiguredQueries commands = new ConfiguredQueries();

    final String mainTableName;
    final String propTableName;
    final String dbSchemaName;

    /** Max length of a property. Used for trimming incoming query values */
    final int maxPropLen;

    final QueryResultMapper resultMapper;
    
    private TableHandler tableHandler;

    /**
     * Constructor.
     *
     * @param tableHandler
     * @param mainTableName
     * @param propTableName
     * @param dbSchemaName
     * @param maxPropLen Max length of propvalues. Used for trimming values if > 0.
     * @param resultMapper
     */
    public TableQueries(TableHandler tableHandler, String mainTableName, String propTableName, String dbSchemaName, int maxPropLen,
            QueryResultMapper resultMapper) {
        this.tableHandler = tableHandler;
        this.mainTableName = mainTableName;
        this.propTableName = propTableName;
        this.dbSchemaName = dbSchemaName;
        this.maxPropLen = maxPropLen;
        this.resultMapper = resultMapper;
    }

    /**
     * Get a prepared statement for the given connection and SQL. May come from
     * a cache (either local or the host container)
     *
     * @param connection
     *            db connection to get a prepared statement for
     * @param sql
     *            the prepared statement SQL
     * @return the prepared statement
     * @throws SQLException
     *             if parsing or retrieving the prepared statement failed
     */
    public PreparedStatement getPreparedStatement(Connection connection, String sql)
            throws SQLException {
        return getPreparedStatement(connection, sql, false);
    }

    /**
     * Get a prepared statement for the given connection and SQL. May come from
     * a cache (either local or the host container)
     *
     * @param connection
     *            db connection to get a prepared statement for
     * @param sql
     *            the prepared statement SQL
     * @param autoGeneratedKeys
     *            whether to return auto-generated keys by the DB
     * @return the prepared statement
     * @throws SQLException
     *             if parsing or retrieving the prepared statement failed
     */
    public PreparedStatement getPreparedStatement(Connection connection, String sql,
            boolean autoGeneratedKeys) throws SQLException {
        // This is where local prepared statement caching could be added for
        // stand-alone operation.

        // In the context of a (JavaEE) container rely on its built-in prepared
        // statement caching
        // rather than doing it explicitly here.
        if (autoGeneratedKeys) {
            return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        } else {
            return connection.prepareStatement(sql);
        }
    }

    /**
     * Get a prepared statement for the given connection and SQL. Returns the
     * generated Key This is a function used by OracleTableHandler. Since ORACLE
     * does not return the auto incremented key but the ROWID on using
     * getGeneratedKeys(), we have to pass a string array containing the column
     * that has been auto incremented. I.E. passing 'id' as the only entry of
     * this array to this method will return the value of the id-column instead
     * of the ROWID
     *
     * @param connection
     *            db connection to get a prepared statement for
     * @param sql
     *            the prepared statement SQL
     * @param columns
     *            which column shall be returned as the value of
     *            PreparedStatement.getGeneratedKeys()
     * @return the prepared statement
     * @throws SQLException
     *             if parsing or retrieving the prepared statement failed
     */
    public PreparedStatement getPreparedStatement(Connection connection, String sql, String[] columns)
            throws SQLException {
        return connection.prepareStatement(sql, columns);
    }

    /**
     * Execute a query, either a pre-configured query by using the query ID, or
     * a query expression passed as part of the params.
     *
     * The keys for the input parameters as well as the return map entries are
     * in QueryConstants.
     *
     * @param type
     *            the resource component name targeted by the URI
     * @param params
     *            the parameters which include the query id, or the query
     *            expression, as well as the token key/value pairs to replace in
     *            the query
     * @param con
     *            a handle to a database connection newBuilder for exclusive use
     *            by the query method whilst it is executing.
     * @return The query result, which includes meta-data about the query, and
     *         the result set itself.
     * @throws BadRequestException
     *             if the passed request parameters are invalid, e.g. missing
     *             query id or query expression or tokens.
     * @throws InternalServerErrorException
     *             if the preparing or executing the query fails because of
     *             configuration or DB issues
     */
    public List<Map<String, Object>> query(final String type, Map<String, Object> params, Connection con)
            throws ResourceException {

        List<Map<String, Object>> result = null;
        params.put(ServerConstants.RESOURCE_NAME, type);

        // If paged results are requested then decode the cookie in order to determine
        // the index of the first result to be returned.
        final int requestPageSize = (Integer) params.get(PAGE_SIZE);

        final String offsetParam;
        final String pageSizeParam;

        if (requestPageSize > 0) {
            offsetParam = String.valueOf((Integer) params.get(PAGED_RESULTS_OFFSET));
            pageSizeParam = String.valueOf(requestPageSize);
        } else {
            offsetParam = "0";
            pageSizeParam = String.valueOf(Integer.MAX_VALUE);
        }

        params.put(PAGED_RESULTS_OFFSET, offsetParam);
        params.put(PAGE_SIZE, pageSizeParam);
        QueryFilter queryFilter = (QueryFilter) params.get(QUERY_FILTER);
        String queryExpression = (String) params.get(QUERY_EXPRESSION);
        String queryId = (String) params.get(QUERY_ID);
        if (queryId == null && queryExpression == null && queryFilter == null) {
            throw new BadRequestException("Either " + QUERY_ID + ", " + QUERY_EXPRESSION + ", or "
                    + QUERY_FILTER + " to identify/define a query must be passed in the parameters. " + params);
        }
        logger.debug("Querying " + params);
        final PreparedStatement foundQuery;
        try {
            if (queryFilter != null) {
                foundQuery = parseQueryFilter(con, queryFilter, params);
            } else if (queryExpression != null) {
                foundQuery = resolveInlineQuery(con, queryExpression, params);
            } else if (queries.queryIdExists(queryId)) {
                foundQuery = queries.getQuery(con, queryId, type, params);
            } else {
                throw new BadRequestException("The passed query identifier " + queryId
                        + " does not match any configured queries on the JDBC repository service.");
            }
        } catch (SQLException ex) {
            final String queryDescription;
            if (queryFilter != null) {
                queryDescription = queryFilter.toString();
            } else if (queryExpression != null) {
                queryDescription = queryExpression;
            } else {
                queryDescription = queries.getQueryInfo(queryId).getQueryString();
            }
            throw new InternalServerErrorException("DB reported failure preparing query: "
                    + queryDescription
                    + " with params: " + params + " error code: " + ex.getErrorCode()
                    + " sqlstate: " + ex.getSQLState() + " message: " + ex.getMessage(), ex);
        }

        Name eventName = getEventName(queryId);
        EventEntry measure = Publisher.start(eventName, foundQuery, null);
        ResultSet rs = null;
        try {
            rs = foundQuery.executeQuery();
            result = resultMapper.mapQueryToObject(rs, queryId, type, params, this);
            measure.setResult(result);
        } catch (SQLException ex) {
            throw new InternalServerErrorException("DB reported failure executing query "
                    + foundQuery.toString() + " with params: " + params + " error code: "
                    + ex.getErrorCode() + " sqlstate: " + ex.getSQLState() + " message: "
                    + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new InternalServerErrorException("Failed to convert result objects for query "
                    + foundQuery.toString() + " with params: " + params + " message: "
                    + ex.getMessage(), ex);
        } finally {
            CleanupHelper.loggedClose(rs);
            CleanupHelper.loggedClose(foundQuery);
            measure.end();
        }
        return result;
    }

    public Integer command(final String type, Map<String, Object> params, Connection con)
            throws ResourceException {

        Integer result = null;
        params.put(ServerConstants.RESOURCE_NAME, type);

        String queryExpression = (String) params.get("commandExpression");
        String queryId = (String) params.get("commandId");
        if (queryId == null && queryExpression == null) {
            throw new BadRequestException("Either " + "commandId" + " or " + "commandExpression"
                    + " to identify/define a query must be passed in the parameters. " + params);
        }
        final PreparedStatement foundQuery;
        try {
            if (queryExpression != null) {
                foundQuery = resolveInlineQuery(con, queryExpression, params);
            } else if (commands.queryIdExists(queryId)) {
                foundQuery = commands.getQuery(con, queryId, type, params);
            } else {
                throw new BadRequestException("The passed command identifier " + queryId
                        + " does not match any configured commands on the JDBC repository service.");
            }
        } catch (SQLException ex) {
            throw new InternalServerErrorException("DB reported failure preparing command: "
                    + (queryExpression != null ? queryExpression : commands.getQueryInfo(queryId).getQueryString())
                    + " with params: " + params + " error code: " + ex.getErrorCode()
                    + " sqlstate: " + ex.getSQLState() + " message: " + ex.getMessage(), ex);
        }

        Name eventName = getEventName(queryId);
        EventEntry measure = Publisher.start(eventName, foundQuery, null);
        ResultSet rs = null;
        try {
            result = foundQuery.executeUpdate();
            measure.setResult(result);
        } catch (SQLException ex) {
            throw new InternalServerErrorException("DB reported failure executing query "
                    + foundQuery.toString() + " with params: " + params + " error code: "
                    + ex.getErrorCode() + " sqlstate: " + ex.getSQLState() + " message: "
                    + ex.getMessage(), ex);
        } finally {
            CleanupHelper.loggedClose(rs);
            CleanupHelper.loggedClose(foundQuery);
            measure.end();
        }
        return result;
    }

    /**
     * Whether a result set contains a given column
     *
     * @param rsMetaData
     *            result set meta data
     * @param columnName
     *            name of the column to look for
     * @return true if it is present
     * @throws SQLException
     *             if meta data inspection failed
     */
    public boolean hasColumn(ResultSetMetaData rsMetaData, String columnName) throws SQLException {
        for (int colPos = 1; colPos <= rsMetaData.getColumnCount(); colPos++) {
            if (columnName.equalsIgnoreCase(rsMetaData.getColumnName(colPos))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves a query filter.
     *
     * @param con
     *            The db connection
     * @param filter
     *            the query filter to parse
     * @return A resolved statement
     */
    PreparedStatement parseQueryFilter(Connection con, QueryFilter filter, Map<String, Object> params)
            throws SQLException, ResourceException {
        Map<String, Object> replacementTokens = new LinkedHashMap<String, Object>();

        String rawQuery = tableHandler.renderQueryFilter(filter, replacementTokens, params);

        Map<String, String> replacements = new LinkedHashMap<String, String>();
        replacements.put("_mainTable", mainTableName);
        replacements.put("_propTable", propTableName);
        replacements.put("_dbSchema", dbSchemaName);

        TokenHandler tokenHandler = new TokenHandler();
        // Replace the table name tokens.
        String tempQueryString = tokenHandler.replaceSomeTokens(rawQuery, replacements);

        logger.debug("Tokenized statement: {} with replacementTokens: {}", rawQuery, replacementTokens);

        // Convert to ? for prepared statement, populate token replacement info
        List<String> tokenNames = tokenHandler.extractTokens(tempQueryString);
        String queryString = tokenHandler.replaceTokens(tempQueryString, "?", PREFIX_LIST);

        QueryInfo queryInfo = new QueryInfo(queryString, tokenNames);
        return resolveQuery(queryInfo, con, replacementTokens);
    }

    /**
     * Resolves a full query expression Currently does not support token
     * replacement
     *
     * @param con
     *            The db connection
     * @param queryExpression
     *            the native query string
     * @param params
     *            parameters passed to the resource query
     * @return A resolved statement
     */
    PreparedStatement resolveInlineQuery(Connection con, String queryExpression,
            Map<String, Object> params) throws SQLException, ResourceException {
        // No token replacement on expressions for now
        List<String> tokenNames = new ArrayList<String>();
        QueryInfo info = new QueryInfo(queryExpression, tokenNames);
        return resolveQuery(info, con, params);
    }

    /**
     * Check if a {@code queryId} is present in the set of configured queries.
     *
     * @param queryId Id of the query to check for
     *
     * @return true if the queryId is present in the set of configured queries.
     */
    public boolean queryIdExists(final String queryId) {
        return queries.queryIdExists(queryId);
    }

    /**
     * Resolves a query, given a QueryInfo
     *
     * @param info
     *            The info encapsulating the query information
     * @param con
     *            the db connection
     * @param params
     *            the parameters passed to query
     * @return the resolved query
     * @throws SQLException
     *             if resolving the query failed
     */
    PreparedStatement resolveQuery(QueryInfo info, Connection con, Map<String, Object> params)
            throws SQLException, ResourceException {
        String queryStr = info.getQueryString();
        List<String> tokenNames = info.getTokenNames();

        // replace ${list:variable} tokens with the correct number of bind variables
        Map<String, Integer> listReplacements = new HashMap<String, Integer>();
        for (String tokenName : tokenNames) {
            String[] tokenParts = tokenName.split(":", 2);
            if (PREFIX_LIST.equals(tokenParts[0]) && params.containsKey(tokenParts[1])) {
                listReplacements.put(tokenName, ((String) params.get(tokenParts[1])).split(",").length);
            }
        }
        if (listReplacements.size() > 0) {
            TokenHandler tokenHandler = new TokenHandler();
            queryStr = tokenHandler.replaceListTokens(queryStr, listReplacements, "?");
        }

        // now prepare the statement using the correct number of bind variables
        PreparedStatement statement = getPreparedStatement(con, queryStr);
        int count = 1; // DB column count starts at 1
        for (String tokenName : tokenNames) {
            String[] tokenParts = tokenName.split(":", 2);
            if (tokenParts.length == 1) {
                // handle single value - assume String
                Object objValue =  params.get(tokenName);
                String value = null;
                if (objValue != null) {
                    value = trimValue(objValue);
                } else {
                    // fail with an exception if token not found
                    throw new BadRequestException("Missing entry in params passed to query for token " + tokenName);
                }
                statement.setString(count, value);
                count++;
            }
            else {
                Object objValue =  params.get(tokenParts[1]);
                if (objValue == null) {
                    // fail with an exception if token not found
                    throw new BadRequestException("Missing entry in params passed to query for token " + tokenName);
                }
                if (PREFIX_INT.equals(tokenParts[0])) {
                    // handle single integer value
                    Integer int_value = null;
                    if (objValue != null) {
                        int_value = Integer.parseInt(objValue.toString());
                    }
                    statement.setInt(count, int_value);
                    count++;
                } else if (PREFIX_LIST.equals(tokenParts[0])) {
                    // handle list of values - presently assumes Strings, TODO support integer lists
                    if (objValue != null) {
                        for (String list_value : objValue.toString().split(",")) {
                            // if list value is surrounded by single quotes remove them
                            if (list_value != null && list_value.startsWith("'") && list_value.endsWith("'")) {
                                list_value = list_value.substring(1, list_value.length()-1);
                            }
                            statement.setString(count, trimValue(list_value));
                            count++;
                        }
                    }
                    else {
                        statement.setString(count, null);
                        count++;
                    }
                }
            }
        }
        logger.debug("Prepared statement: {}", statement);

        return statement;
    }

    /**
     * Set the pre-configured queries/commands for generic tables, which are identified
     * by a query identifier and can be invoked using this identifier
     *
     * Success to set the queries does not mean they are valid as some can only
     * be validated at query execution time.
     *
     * @param queriesConfig
     *            queries configured in configuration (files)
     * @param defaultQueryMap
     *            static default queries already defined for handling this table
     *            type
     *
     *            query details
     */
    public void setConfiguredQueries(
            JsonValue queriesConfig, JsonValue commandsConfig, Map<QueryDefinition, String> defaultQueryMap) {
        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put("_mainTable", mainTableName);
        replacements.put("_propTable", propTableName);
        replacements.put("_dbSchema", dbSchemaName);

        setConfiguredQueries(replacements, queriesConfig, commandsConfig, defaultQueryMap);
    }

    /**
     * Set the pre-configured queries/commands for explicitly mapped tables, which are
     * identified by a query identifier and can be invoked using this identifier
     *
     * Success to set the queries does not mean they are valid as some can only
     * be validated at query execution time.
     *
     * @param tableName
     *            name of the explicitly mapped table
     * @param dbSchemaName
     *            the database scheme the table is in
     * @param queriesConfig
     *            queries configured in configuration (files)
     * @param defaultQueryMap
     *            static default queries already defined for handling this table
     *            type
     *
     *            query details
     */
    public void setConfiguredQueries(String tableName, String dbSchemaName,
            JsonValue queriesConfig, JsonValue commandsConfig, Map<QueryDefinition, String> defaultQueryMap) {
        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put("_table", tableName);
        replacements.put("_dbSchema", dbSchemaName);

        setConfiguredQueries(replacements, queriesConfig, commandsConfig, defaultQueryMap);
    }

    private void setConfiguredQueries(Map<String, String> replacements, JsonValue queriesConfig, JsonValue commandsConfig,
            Map<QueryDefinition, String> defaultQueryMap) {

        if (queriesConfig == null || queriesConfig.isNull()) {
            queriesConfig = json(object());
        }
        if (commandsConfig == null || commandsConfig.isNull()) {
            commandsConfig = json(object());
        }

        // Default query-all-ids to allow bootstrapping of configuration
        if (!queriesConfig.isDefined(ServerConstants.QUERY_ALL_IDS) && defaultQueryMap != null) {
            queriesConfig.put(ServerConstants.QUERY_ALL_IDS, defaultQueryMap.get(QueryDefinition.QUERYALLIDS));
        }

        queries.setConfiguredQueries(replacements, queriesConfig);
        commands.setConfiguredQueries(replacements, commandsConfig);
    }

    /**
     * @return the smartevent Name for a given query
     */
    Name getEventName(String queryId) {
        if (queryId == null) {
            return Name.get(EVENT_RAW_QUERY_PREFIX + "_query_expression");
        } else {
            return Name.get(EVENT_RAW_QUERY_PREFIX + queryId);
        }
    }

    private String trimValue(Object param) {
        return maxPropLen <= 0 ? param.toString() : StringUtils.left(param.toString(), maxPropLen);
    }
}
