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
package org.forgerock.openidm.repo.jdbc.impl.query;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.repo.util.TokenHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configured and add-hoc query support on tables in generic (non-object specific) layout
 * 
 * Queries can contain tokens of the format ${token-name}
 * 
 * @author aegloff
 *
 */
public class GenericTableQueries {

    final static Logger logger = LoggerFactory.getLogger(GenericTableQueries.class);
    
    // Pre-configured queries, key is query id
    Map<String, QueryInfo> queries = new HashMap<String, QueryInfo>();

    /**
     * Get a prepared statement for the given connection and SQL. May come from a cache 
     * (either local or the host container)
     * 
     * @param connection db connection to get a prepared statement for
     * @param sql the prepared statement SQL
     * @return the prepared statement
     * @throws SQLException if parsing or retrieving the prepared statement failed
     */
    public PreparedStatement getPreparedStatement(Connection connection, String sql) throws SQLException {
        PreparedStatement statement = null;
        // This is where local prepared statement caching could be added for stand-alone operation.

        // In the context of a (JavaEE) container rely on its built-in prepared statement caching 
        // rather than doing it explicitly here.
        statement = connection.prepareStatement(sql);

        return statement;
    }
    
    /**
     * Execute a query, either a pre-configured query by using the query ID, or a query expression passed as 
     * part of the params.
     * 
     * The keys for the input parameters as well as the return map entries are in QueryConstants.
     * 
     * @param type the relative/local resource name, which matches the OrientDB document class name
     * @param params the parameters which include the query id, or the query expression, as well as the 
     *        token key/value pairs to replace in the query
     * @param database a handle to a database connection instance for exclusive use by the query method whilst it is executing.
     * @return The query result, which includes meta-data about the query, and the result set itself.
     * @throws BadRequestException if the passed request parameters are invalid, e.g. missing query id or query expression or tokens.
     * @throws InternalServerException if the preparing or executing the query fails because of configuration or DB issues
     */
    public List<Map<String, Object>> query(final String type, Map<String, Object> params, Connection con) 
            throws BadRequestException, InternalServerErrorException {
        
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();;
        params.put(QueryConstants.RESOURCE_NAME, type); 
        
        //TODO: support QUERY_EXPRESSION
        //String queryExpression = (String) params.get(QueryConstants.QUERY_EXPRESSION);
        //if (queryExpression != null) {
        //    foundQueryInfo = resolveInlineQuery(type, params, database);
        //} else {
        
        String queryId = (String) params.get(QueryConstants.QUERY_ID);
        if (queryId == null) {
            throw new BadRequestException("Either " + QueryConstants.QUERY_ID + " or " + QueryConstants.QUERY_EXPRESSION
                    + " to identify/define a query must be passed in the parameters. " + params);
        }
        PreparedStatement foundQuery = null;
        try {
            foundQuery = getQuery(con, queryId, type, params); //configuredQueries.get(queryId);
        } catch (SQLException ex) {
            throw new InternalServerErrorException("DB reported failure preparing query: " 
                    + (foundQuery == null ? "null" : foundQuery.toString()) + " with params: " + params + " error code: " + ex.getErrorCode() 
                    + " sqlstate: " + ex.getSQLState() + " message: " + ex.getMessage(), ex);
        }
        if (foundQuery == null) {
            throw new BadRequestException("The passed query identifier " + queryId 
                    + " does not match any configured queries on the OrientDB repository service.");
        }
        
        try {
            ResultSet rs = foundQuery.executeQuery();
            ResultSetMetaData rsMetaData = rs.getMetaData();
            boolean hasFullObject = hasColumn(rsMetaData, "fullobject");
            boolean hasId = hasColumn(rsMetaData, "openidmid");
            boolean hasRev = hasColumn(rsMetaData, "rev");
            while (rs.next()) {
                if (hasFullObject) {
                    String objString = rs.getString("fullobject");
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> obj = (Map<String, Object>) mapper.readValue(objString, Map.class);

                    // TODO: remove data logging            
                    logger.debug("Query result for queryId: {} type: {} converted obj: {}", new Object[] {queryId, type, obj});  
                    
                    result.add(obj);
                } else {
                    Map<String, Object> obj = new HashMap<String, Object>();
                    if (hasId) {
                        obj.put("_id", rs.getString("openidmid"));
                    }
                    if (hasRev) {
                        obj.put("_rev", rs.getString("rev"));
                    }
                    result.add(obj);
                }
            }
        } catch (SQLException ex) {
            throw new InternalServerErrorException("DB reported failure executing query " 
                    + foundQuery.toString() + " with params: " + params + " error code: " + ex.getErrorCode() 
                    + " sqlstate: " + ex.getSQLState() + " message: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new InternalServerErrorException("Failed to convert result objects for query " 
                    + foundQuery.toString() + " with params: " + params + " message: " + ex.getMessage(), ex);
        }
        return result;
    }
    
    boolean hasColumn (ResultSetMetaData rsMetaData, String columnName) throws SQLException {
        for (int colPos = 1 ; colPos <= rsMetaData.getColumnCount(); colPos++) {
            if (columnName.equals(rsMetaData.getColumnName(colPos))) {
                return true;
            }
        }
        return false;
    }
    
    PreparedStatement getQuery(Connection con, String queryId, String type, Map<String, Object> params) 
                throws SQLException, BadRequestException{
        
        QueryInfo info = queries.get(queryId);
        if (info == null) {
            throw new BadRequestException("No query defined/configured for requested queryId " + queryId);
        }
        
        String queryStr = info.getQueryString();
        List<String> tokenNames = info.getTokenNames();
        PreparedStatement statement = getPreparedStatement(con, queryStr); 
        int count = 1; // DB column count starts at 1
        for (String tokenName : tokenNames) {
            Object objValue =  params.get(tokenName);
            String value = null;
            if (objValue != null) {
                value = objValue.toString();
            }
            statement.setString(count, value);
            count++;
        }
        logger.debug("Prepared statement: {}", statement);

        return statement;
    }
    
    /**
     * Set the pre-configured queries, which are identified by a query identifier and can be
     * invoked using this identifier
     * 
     * Success to set the queries does not mean they are valid as some can only be validated at
     * query execution time.
     * 
     * @param queries the complete list of configured queries, mapping from query id to the 
     * query details
     */
    public void setConfiguredQueries(String mainTableName, String propTableName, String dbSchemaName, JsonNode queriesConfig) {
        queries = new HashMap<String, QueryInfo>();
        for (String queryName : queriesConfig.keys()) {
            String rawQuery = queriesConfig.get(queryName).required().asString();
            
            Map<String, String> replacements = new HashMap<String, String>();
            replacements.put("_mainTable", mainTableName);
            replacements.put("_propTable", propTableName);
            replacements.put("_dbSchema", dbSchemaName);
            
            TokenHandler tokenHandler = new TokenHandler();
            // Replace the table name tokens.
            String tempQueryString = tokenHandler.replaceSomeTokens(rawQuery, replacements);
            
            // Convert to ? for prepared statement, populate token replacement info
            List<String> tokenNames = tokenHandler.extractTokens(tempQueryString);
            String queryString = tokenHandler.replaceTokens(tempQueryString, "?");
        
            QueryInfo queryInfo = new QueryInfo(queryString, tokenNames);
            queries.put(queryName, queryInfo);
            logger.info("Configured query converted to JDBC query {} and tokens {}", queryString, tokenNames );
        }
    }    
    
}
