/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2014 ForgeRock AS. All rights reserved.
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

import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import java.util.Map;

import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Request;
import org.forgerock.openidm.smartevent.Name;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Configured and ad-hoc query support on OrientDB
 * 
 * Queries can contain tokens of the format ${token-name}
 *
 * @param <Q> The query object type
 * @param <R> The request type
 * @param <U> The result type
 *
 */
public abstract class ConfiguredQueries<Q extends OCommandRequest, R extends Request, U> {

    final static Logger logger = LoggerFactory.getLogger(ConfiguredQueries.class);
    
    // Monitoring event name prefix
    static final String EVENT_RAW_QUERY_PREFIX = "openidm/internal/repo/orientdb/raw/query/";

    private final TokenHandler tokenHandler = new TokenHandler();
    
    // Pre-configured queries, key is query id
    private final Map<String, QueryInfo<Q>> configuredQueries;

    ConfiguredQueries(Map<String, QueryInfo<Q>> configuredQueries) {
        this.configuredQueries = configuredQueries;
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
        configuredQueries.clear();
        
        // Populate/Override with Queries configured
        if (queries != null) {
            for (Map.Entry<String, String> entry : queries.entrySet()) {
                String queryId = entry.getKey();
                String queryString = entry.getValue();
                QueryInfo<Q> queryInfo = prepareQuery(queryString);
                configuredQueries.put(queryId, queryInfo);
            }
        }
    }

    /**
     * Check if a {@code queryId} is present in the set of configured queries.
     *
     * @param queryId Id of the query to check for
     *
     * @return true if the queryId is present in the set of configured queries.
     */
    public boolean queryIdExists(final String queryId) {
        return configuredQueries.containsKey(queryId);
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
    private Q resolveQuery(String queryString, Map<String, String> params)
            throws BadRequestException {
        String resolvedQueryString = tokenHandler.replaceTokensWithValues(queryString, params);
        return createQueryObject(resolvedQueryString);
    }

    /**
     * Populate and prepare the query information with the query expression passed in the parameters
     *
     * @param type the relative/local resource name
     * @param queryExpression the parameters with the query expression and token replacement key/values
     * @return the populated query info
     */
    private QueryInfo<Q> resolveInlineQuery(final String type, String queryExpression) {
        // TODO: LRU cache
        return prepareQuery(queryExpression);
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
    private QueryInfo<Q> prepareQuery(String queryString) {
        try {
            String replacedQueryString = tokenHandler.replaceTokensWithOrientToken(queryString);
            Q query = createQueryObject(replacedQueryString);
            return new QueryInfo<Q>(true, query, queryString);
        } catch (PrepareNotSupported ex) {
            // Statement not in a format that it can be converted into prepared statement
            return new QueryInfo<Q>(false, null, queryString);
        } catch (com.orientechnologies.orient.core.exception.OQueryParsingException ex) {
            // With current OrientDB impl parsing will actually only fail on first use,
            // hence unless the implementation changes this is unlikely to trigger
            return new QueryInfo<Q>(false, null, queryString);
        }
    }

    /**
     * Create the prepared query object type according to the implementation.
     *
     * @param queryString the query expression, including tokens to replace
     * @return the prepared query object
     */
    protected abstract Q createQueryObject(String queryString);

    /**
     * @return the smartevent Name for a given query
     */
    Name getEventName(String queryId, String queryExpression) {
        if (queryId == null) {
            return Name.get(EVENT_RAW_QUERY_PREFIX + "_query_expression");
        } else {
            return Name.get(EVENT_RAW_QUERY_PREFIX + queryId);
        }
    }

    /**
     * Find the QueryInfo according to the commandId or commandExpression.
     *
     * @param type the type/resource to query
     * @param queryId the queryId parameter
     * @param queryExpression the queryExpression parameter
     * @return
     * @throws NullPointerException if neither queryId or queryExpression are provided
     * @throws IllegalArgumentException if the queryId is not known/configured
     */
    QueryInfo<Q> findQueryInfo(String type, String queryId, String queryExpression) {

        if (queryId == null && queryExpression == null) {
            throw new NullPointerException();
        }
        if (queryExpression != null) {
            return resolveInlineQuery(type, queryExpression);
        }
        if (queryIdExists(queryId)) {
            return configuredQueries.get(queryId);
        }
        throw new IllegalArgumentException();
    }

    /**
     * Execute a query, either a pre-configured query by using the query ID, or a query expression passed as 
     * part of the params.
     * 
     * The keys for the input parameters as well as the return map entries are in QueryConstants.
     * 
     * @param type the relative/local resource name, which needs to be converted to match the OrientDB document class name
     * @param request the query request, including parameters which include the query id, or the query expression, as well as the 
     *        token key/value pairs to replace in the query
     * @param database a handle to a database connection instance for exclusive use by the query method whilst it is executing.
     * @return The query result, which includes meta-data about the query, and the result set itself.
     * @throws BadRequestException if the passed request parameters are invalid, e.g. missing query id or query expression or tokens.
     */
    public abstract U query(final String type, R request, ODatabaseDocumentTx database) throws BadRequestException;

    U doTokenSubsitutionQuery(QueryInfo<Q> queryInfo, Map<String, String> params, ODatabaseDocumentTx database)
            throws BadRequestException {
        // Substitute tokens manually, which supports replacing any part of the query
        Q query = resolveQuery(queryInfo.getQueryString(), params);
        logger.debug("Manual token substitution for {} resulted in {}", queryInfo.getQueryString(), query);
        return database.command(query).execute(params);
    }

    U doPreparedQuery(QueryInfo<Q> queryInfo, Map<String, String> params, ODatabaseDocumentTx database)
            throws BadRequestException {
        // Try to use the prepared statement, which supports token substitution of where clause
        Q query = queryInfo.getPreparedQuery();
        logger.debug("Prepared query {} ", query);
        return database.command(query).execute(params);
    }

}
