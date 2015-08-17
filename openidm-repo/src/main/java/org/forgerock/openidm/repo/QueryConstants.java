/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS.
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
package org.forgerock.openidm.repo;

import org.forgerock.json.resource.http.HttpUtils;

/**
 * Map key constants for Repository Queries
 *
 */
public final class QueryConstants {
    private QueryConstants() {}

    // Keys for the query input
    
    /**
     * Query input key.
     * 
     * Query expression key for the query param map to supply a Query expression in-line. 
     *
     * The alternative is to specify a query identifier for a pre-configured query. 
     * 
     * The query can contain tokens in the form of ${<param-map-key>} which will 
     * get substituted from the param map passed to the query.
     * 
     * When both an expression and ID are present, the query expression takes precedent
     */
    public final static String QUERY_EXPRESSION = "_queryExpression";
    
    /**
     * Query input key.
     * 
     * Query identifier for the query param map. The query identifier must match a
     * configured query. 
     * 
     * The alternative is to specify a query expression for an in-line query.
     *
     * The configured query can contain tokens in the form of ${<param-map-key>}
     * which will get substituted from the param map passed to the query.
     * 
     * When both an expression and ID are present, the query expression takes precedent
     */    
    public final static String QUERY_ID = "_queryId";
    
    /**
     * Query filter.
     */
    public final static String QUERY_FILTER = "_queryFilter";
    
    /**
     * System populated query input key.
     * 
     * Key available for token substitution inside the query expression, identifying the
     * resource queried by name. Example use: select * from ${_resource} where ...
     */
    public final static String RESOURCE_NAME = "_resource";


    // Keys in the query output
    
    /**
     * Query output key.
     * 
     * Key for record results in JSON object model format, value format List<Map<String, Object>>
     */
    public final static String QUERY_RESULT = "result";    
    
    /**
     * Query output key.
     * 
     * Key for query result meta-data, indicating how long the query took in ms
     * Value format is Long.
     */
    public final static String STATISTICS_QUERY_TIME = "query-time-ms";
    
    /**
     * Query output key.
     * 
     * Key for query result meta-data, indicating how long the conversion of the results records took in ms
     * Value format is Long.
     */
    public final static String STATISTICS_CONVERSION_TIME = "conversion-time-ms";
    
    
    // Well known values for the query map input
    
    /**
     * Query input value for the QUERY_ID input key.
     * 
     * Querying with this query id results in querying all object IDs for a given ObjectSet.
     * 
     * All ObjectSets supporting query must support the query corresponding to this ID
     */    
    public final static String QUERY_ALL_IDS = "query-all-ids";

    /**
     * Pagination offset requested. Generally used in an OFFSET clause.
     */
    public final static String PAGED_RESULTS_OFFSET = HttpUtils.PARAM_PAGED_RESULTS_OFFSET;

    /**
     * Page size requested. Generally used in a LIMIT clause.
     */
    public static final String PAGE_SIZE = HttpUtils.PARAM_PAGE_SIZE;
    
    /**
     * Sort keys used for sorting the results of the query.
     */
    public static final String SORT_KEYS = HttpUtils.PARAM_SORT_KEYS;
    
    /**
     * The clause at the end of the query expressing that provides paging details.
     */
    public static final String PAGE_CLAUSE = "pageClause";
}
