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
package org.forgerock.openidm.repo.orientdb.impl.query;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.exception.OQueryParsingException;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.repo.orientdb.impl.OrientDBRepoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configured and add-hoc query support on OrientDB
 * 
 * Queries can contain tokens of the format ${token-name}
 * 
 * @author aegloff
 *
 */
public class Queries {

    final static Logger logger = LoggerFactory.getLogger(Queries.class);
    
    TokenHandler tokenHandler = new TokenHandler();
    
    // Pre-configured queries, key is query id
    Map<String, QueryInfo> configuredQueries = new HashMap<String, QueryInfo>();

    /**
     * Execute a query, either a pre-configured query by using the query ID, or a query expression passed as 
     * part of the params.
     * 
     * The keys for the input parameters as well as the return map entries are in QueryConstants.
     * 
     * @param type the relative/local resource name, which needs to be converted to match the OrientDB document class name
     * @param params the parameters which include the query id, or the query expression, as well as the 
     *        token key/value pairs to replace in the query
     * @param database a handle to a database connection instance for exclusive use by the query method whilst it is executing.
     * @return The query result, which includes meta-data about the query, and the result set itself.
     * @throws BadRequestException if the passed request parameters are invalid, e.g. missing query id or query expression or tokens.
     */
    public List<ODocument> query(final String type, Map<String, Object> params, ODatabaseDocumentTx database) 
            throws BadRequestException {
        
        String orientClassName = OrientDBRepoService.typeToOrientClassName(type);
        
        List<ODocument> result = null;
        QueryInfo foundQueryInfo = null;
        params.put(QueryConstants.RESOURCE_NAME, orientClassName); 
        
        String queryExpression = (String) params.get(QueryConstants.QUERY_EXPRESSION);
        if (queryExpression != null) {
            foundQueryInfo = resolveInlineQuery(type, params, database);
        } else {
            String queryId = (String) params.get(QueryConstants.QUERY_ID);
            if (queryId == null) {
                throw new BadRequestException("Either " + QueryConstants.QUERY_ID + " or " + QueryConstants.QUERY_EXPRESSION
                        + " to identify/define a query must be passed in the parameters. " + params);
            }
            foundQueryInfo = configuredQueries.get(queryId);
            if (foundQueryInfo == null) {
                throw new BadRequestException("The passed query identifier " + queryId 
                        + " does not match any configured queries on the OrientDB repository service.");
            }
        }
        
        if (foundQueryInfo != null) {
            OSQLSynchQuery<ODocument> query = null;
            boolean tryPrepared = foundQueryInfo.isUsePrepared();
            if (tryPrepared) {
                // Try to use the prepared statement, which supports token substitution of where clause
                query = foundQueryInfo.getPreparedQuery();                
                logger.debug("Prepared query {} ", query);
            } else {
                // Substitute tokens manually, which supports replacing any part of the query
                String queryString = foundQueryInfo.getQueryString();
                query = resolveQuery(queryString, params);
                logger.debug("Manual token substitution for {} resulted in {}", queryString, query);
            }

            // TODO: Simplify the below
            try {
                logger.debug("Evaluate query {}", query);
                result = database.command(query).execute(params);
            } catch (OQueryParsingException firstTryEx) {
                if (tryPrepared) {
                    // Prepared query is invalid, fall back onto add-hoc resolved query
                    try {
                        String queryString = foundQueryInfo.getQueryString();
                        query = resolveQuery(queryString, params);
                        logger.debug("Prepared version not valid, manual token substitution for {} resulted in {}",
                                queryString, query);
                        result = database.command(query).execute(params);
                        // Disable use of the prepared statement as manually resolved works
                        logger.debug("Manual substition valid, mark not to use prepared statement");
                        foundQueryInfo.setUsePrepared(false);
                    } catch (OQueryParsingException secondTryEx) {
                        // TODO: consider differentiating between bad configuration and bad request
                        throw new BadRequestException("Failed to resolve and parse the query " 
                                + foundQueryInfo.getQueryString() + " with params: " + params, secondTryEx);
                    }
                } else {
                    // TODO: consider differentiating between bad configuration and bad request
                    throw new BadRequestException("Failed to resolve and parse the query " 
                            + foundQueryInfo.getQueryString() + " with params: " + params, firstTryEx);
                }
            } catch (IllegalArgumentException ex) {
                // TODO: consider differentiating between bad configuration and bad request
                throw new BadRequestException("Query is invalid: " 
                        + foundQueryInfo.getQueryString() + " " + ex.getMessage(), ex);
            }
        }
        return result;
    }
    
    /**
     * Set the pre-configured queries, which are identified by a query identifier and can be
     * invoked using this identifier
     * 
     * Success to set the queries does not mean they are valid as some can only be validated at
     * query execution time.
     * 
     * @param queries the complete list of configured queries, mapping from query id to the 
     * query expression which may optionally contain tokens in the form ${token-name}.
     */
    public void setConfiguredQueries(Map<String, String> queries) {
        Map<String, QueryInfo> prepQueries = new HashMap<String, QueryInfo>();
        
        // Query all IDs is a mandatory query, default it and allow override.
        QueryInfo defaultAllIdsQuery = prepareQuery("select _openidm_id from ${_resource}");
        prepQueries.put("query-all-ids", defaultAllIdsQuery);
        
        // Populate/Override with Queries configured
        if (queries != null) {
            for (Map.Entry<String, String> entry : queries.entrySet()) {
                String queryId = entry.getKey();
                String queryString = entry.getValue();
                QueryInfo queryInfo = prepareQuery(queryString);
                prepQueries.put(queryId, queryInfo);
            }
        }
        configuredQueries = prepQueries;
    }    
    
    /**
     * Resolve the query string which can contain %{token} tokens to a fully resolved query
     * Doing the resolution ourselves means it can not be a prepared statement with tokens
     * that gets re-used, but it allows us to replace more parts of the query than just
     * the where clause.
     *  
     * @param queryString The query with tokens
     * @param params THe parameters to replace the tokens with
     * @return the query with any found tokens replaced
     * @throws BadRequestException if the queryString contains token missing from params
     */
    protected OSQLSynchQuery<ODocument> resolveQuery(String queryString, Map<String,Object> params)
            throws BadRequestException {
        String resolvedQueryString = tokenHandler.replaceTokensWithValues(queryString, params);
        return new OSQLSynchQuery<ODocument>(resolvedQueryString);
    }
    
    /**
     * Populate and prepare the query information with the query expression passed in the parameters
     *
     * @param type the relative/local resource name
     * @param params the parameters with the query expression and token replacement key/values
     * @param database a handle to a OrientDB db that is available for exclusive use during the invocation of this method
     * @return the populated query info
     */
    protected QueryInfo resolveInlineQuery(final String type, Map<String, Object> params, ODatabaseDocumentTx database ) {
        // TODO: LRU cache
        String queryString = (String) params.get(QueryConstants.QUERY_EXPRESSION);
        return  prepareQuery(queryString);
    }

    /**
     * Construct a prepared statement from the query String
     * 
     * The constructed prepared query can only be verified at query execution time
     * and hence the query execution may later fall back onto non-prepared execution
     * 
     * @param queryString the query expression, including tokens to replace
     * @return the constructed (but not validated) prepared statement
     */
    protected QueryInfo prepareQuery(String queryString) {
        QueryInfo queryInfo = null;
        try {
            String replacedQueryString = tokenHandler.replaceTokensWithOrientToken(queryString);
            OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(replacedQueryString);
            queryInfo = new QueryInfo(true, query, queryString);
        } catch (com.orientechnologies.orient.core.exception.OQueryParsingException ex) {
            // With current OrientDB impl parsing will actually only fail on first use, 
            // hence unless the implementation changes this is unlikely to trigger
            queryInfo = new QueryInfo(false, null, queryString);
        }
        return queryInfo;
    }
}
