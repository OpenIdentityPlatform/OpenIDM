/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS. All rights reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.SortKey;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.repo.orientdb.impl.DocumentUtil;
import org.forgerock.openidm.repo.orientdb.impl.OrientDBRepoService;
import org.forgerock.openidm.repo.util.StringSQLQueryFilterVisitor;
import org.forgerock.openidm.repo.util.StringSQLRenderer;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OQueryParsingException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

/**
 * Configured and add-hoc query support on OrientDB
 * 
 * Queries can contain tokens of the format ${token-name}
 */
public class Queries extends ConfiguredQueries<OSQLSynchQuery<ODocument>, QueryRequest, List<ODocument>> {

    final static Logger logger = LoggerFactory.getLogger(Queries.class);

    private class OrientQueryFilterVisitor extends StringSQLQueryFilterVisitor<Map<String, String>> {
        int objectNumber = 0;
        @Override
        public StringSQLRenderer visitValueAssertion(Map<String, String> objects, String operand, JsonPointer field, Object valueAssertion) {
            ++objectNumber;
            String value = "v"+objectNumber;
            if (ResourceUtil.RESOURCE_FIELD_CONTENT_ID_POINTER.equals(field)) {
                objects.put(field.toString(), DocumentUtil.ORIENTDB_PRIMARY_KEY);
            } else {
                objects.put(field.toString(), field.toString());
            }
            objects.put(value, String.valueOf(valueAssertion));
            return new StringSQLRenderer("(${dotnotation:" + field.toString() + "} " + operand + " ${" + value + "})");
        }

        @Override
        public StringSQLRenderer visitPresentFilter(Map<String, String> objects, JsonPointer field) {
            if (ResourceUtil.RESOURCE_FIELD_CONTENT_ID_POINTER.equals(field)) {
                objects.put(field.toString(), DocumentUtil.ORIENTDB_PRIMARY_KEY);
            } else {
                objects.put(field.toString(), field.toString());
            }
            return new StringSQLRenderer("(${dotnotation:" + field.toString() + "} IS NOT NULL)");
        }

        @Override
        public StringSQLRenderer visitNotFilter(Map<String, String> objects, QueryFilter subFilter) {
            return new StringSQLRenderer("(NOT " + subFilter.accept(this, objects) + ")");
        }

        @Override
        public StringSQLRenderer visitStartsWithFilter(Map<String, String> parameters, JsonPointer field, Object valueAssertion) {
            // OrientDB needs double % for "like anything"
            return "".equals(valueAssertion)
                ? visitValueAssertion(parameters, "LIKE", field, "%%")
                : visitValueAssertion(parameters, "LIKE", field, valueAssertion + "%");
        }
    }

    public Queries() {
        super(new HashMap<String, QueryInfo<OSQLSynchQuery<ODocument>>>());
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
    @Override
    public void setConfiguredQueries(Map<String, String> queries) {
        // Query all IDs is a mandatory query, default it and allow override.
        if (!queries.containsKey("query-all-ids")) {
            queries.put("query-all-ids", "select _openidm_id from ${unquoted:_resource}");
        }

        super.setConfiguredQueries(queries);
    }

    /**
     * Create an SQL synchronous query returning an ODocument containing the query result.
     *
     * @param queryString the query expression, including tokens to replace
     * @return the prepared query object
     * */
    protected OSQLSynchQuery<ODocument> createQueryObject(String queryString) {
        return new OSQLSynchQuery<ODocument>(queryString);
    }

    private QueryInfo<OSQLSynchQuery<ODocument>> findQueryInfo(String type, Map<String, String> params,
            String queryId, String queryExpression, QueryFilter filter) {
        String queryString = queryExpression == null ? null : queryExpression + params.get("pageClause");
        if (filter != null) {
            // If there is a filter, use it's query string
            queryString = "SELECT * FROM ${unquoted:_resource} WHERE "
                    + filter.accept(new OrientQueryFilterVisitor(), params).toSQL()
                    + " " + params.get(QueryConstants.PAGE_CLAUSE);
        }
        // treat the query created by the filter as a queryExpression
        return findQueryInfo(type, queryId, queryString);
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
    public List<ODocument> query(final String type, QueryRequest request, final ODatabaseDocumentTx database)
            throws BadRequestException {

        final Map<String, String> params = new HashMap<String, String>(request.getAdditionalParameters());
        params.put(QueryConstants.RESOURCE_NAME, OrientDBRepoService.typeToOrientClassName(type));
        params.putAll(getPagingParameters(request));

        if (request.getQueryId() == null 
                && request.getQueryExpression() == null 
                && request.getQueryFilter() == null) {
            throw new BadRequestException("Either " + QueryConstants.QUERY_ID + ", " 
                    + QueryConstants.QUERY_EXPRESSION + ", or " + QueryConstants.QUERY_FILTER
                    + " to identify/define a query must be passed in the parameters. " + params);
        }

        final QueryInfo<OSQLSynchQuery<ODocument>> queryInfo;
        try {
            queryInfo = findQueryInfo(type, params, request.getQueryId(), request.getQueryExpression(), request.getQueryFilter());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("The passed identifier " + request.getQueryId()
                    + " does not match any configured queries on the OrientDB repository service.");
        }

        List<ODocument> result = null;

        logger.debug("Evaluate query {}", queryInfo.getQueryString());
        Name eventName = getEventName(request.getQueryId(), request.getQueryExpression());
        EventEntry measure = Publisher.start(eventName, queryInfo, null);

        // Disabled prepared statements until pooled usage is clarified
        boolean tryPrepared = false;

        try {
            if (tryPrepared /* queryInfo.isUsePrepared() */ ) {
                result = doPreparedQuery(queryInfo, params, database);
            } else {
                result = doTokenSubsitutionQuery(queryInfo, params, database);
            }
            measure.setResult(result);
        } catch (OQueryParsingException firstTryEx) {
            if (tryPrepared /* queryInfo.isUsePrepared() */ ) {
                // Prepared query is invalid, fall back onto add-hoc resolved query
                try {
                    logger.debug("Prepared version not valid, trying manual substitution");
                    result = doTokenSubsitutionQuery(queryInfo, params, database);
                    measure.setResult(result);
                    // Disable use of the prepared statement as manually resolved works
                    logger.debug("Manual substitution valid, mark not to use prepared statement");
                    queryInfo.setUsePrepared(false);
                } catch (OQueryParsingException secondTryEx) {
                    // TODO: consider differentiating between bad configuration and bad request
                    throw new BadRequestException("Failed to resolve and parse the query "
                            + queryInfo.getQueryString() + " with params: " + params, secondTryEx);
                }
            } else {
                // TODO: consider differentiating between bad configuration and bad request
                throw new BadRequestException("Failed to resolve and parse the query "
                        + queryInfo.getQueryString() + " with params: " + params, firstTryEx);
            }
        } catch (IllegalArgumentException ex) {
            // TODO: consider differentiating between bad configuration and bad request
            throw new BadRequestException("Query is invalid: "
                    + queryInfo.getQueryString() + " " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            logger.warn("Unexpected failure during DB query: {}", ex.getMessage());
            throw ex;
        } finally {
            measure.end();
        }

        return result;
    }

    /**
     * Return the page size and offset parameters as requested.
     *
     * @param request the QueryRequest
     * @return the Map of page size and results offset
     */
    private Map<String, String> getPagingParameters(QueryRequest request) {
        final Map<String, String> params = new HashMap<String, String>(2);

        // If paged results are requested then decode the cookie in order to determine
        // the index of the first result to be returned.
        final int requestPageSize = request.getPageSize();

        final String offsetParam;
        final String pageSizeParam;
        final List<SortKey> sortKeys;
        String pageClause;

        if (requestPageSize > 0) {
            offsetParam = String.valueOf(request.getPagedResultsOffset());
            pageSizeParam = String.valueOf(requestPageSize);
            sortKeys = request.getSortKeys();
            pageClause = "SKIP " + offsetParam + " LIMIT " + pageSizeParam;
            // Add sort keys, if any
            if (sortKeys != null && sortKeys.size() > 0) {
                List<String> keys = new ArrayList<String>();
                for (SortKey sortKey : sortKeys) {
                    String field = sortKey.getField().toString().substring(1);
                    keys.add(field + (sortKey.isAscendingOrder() ? " ASC" : " DESC"));
                }
                pageClause += " ORDER BY " + StringUtils.join(keys, ", ");
            }
        } else {
            offsetParam = "0";
            pageSizeParam = "-1"; // unlimited in Orient
            pageClause = "";
        }

        params.put(QueryConstants.PAGED_RESULTS_OFFSET, offsetParam);
        params.put(QueryConstants.PAGE_SIZE, pageSizeParam);
        params.put(QueryConstants.PAGE_CLAUSE, pageClause);

        return params;
    }

}
