/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2016 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.util;

import static org.forgerock.json.resource.QueryRequest.FIELD_QUERY_EXPRESSION;
import static org.forgerock.json.resource.QueryRequest.FIELD_QUERY_FILTER;
import static org.forgerock.json.resource.QueryRequest.FIELD_QUERY_ID;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_QUERY_EXPRESSION;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_QUERY_FILTER;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_QUERY_ID;
import static org.forgerock.json.resource.http.HttpUtils.PARAM_FIELDS;

import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.Requests;


/**
 * Utility methods for helping build/manipulate CREST request objects.
 */
public class RequestUtil {

    /**
     * Create a QueryRequest from a resource name and a map of parameters.  The parameters may contain "special"
     * CREST-recognized parameters such as _queryId, _queryFilter, and _queryExpression--if so, set these attributes
     * of the QueryRequest specifically.  All other parameters are considered additional parameters.
     *
     * @param resourceContainer the resource to query
     * @param parameters a map of query parameters
     * @return the QueryRequest
     * @throws BadRequestException if a additional parameter is disallowed by CREST
     */
    public static QueryRequest buildQueryRequestFromParameterMap(String resourceContainer, Map<String, Object> parameters)
            throws BadRequestException {

        final QueryRequest request = Requests.newQueryRequest(resourceContainer);
        for (Map.Entry<String, Object> e: parameters.entrySet()) {
            if (PARAM_QUERY_ID.equals(e.getKey()) || FIELD_QUERY_ID.equals(e.getKey())) {
                request.setQueryId(String.valueOf(e.getValue()));
            } else if (PARAM_QUERY_EXPRESSION.equals(e.getKey()) || FIELD_QUERY_EXPRESSION.equals(e.getKey())) {
                request.setQueryExpression(String.valueOf(e.getValue()));
            } else if (PARAM_QUERY_FILTER.equals(e.getKey()) || FIELD_QUERY_FILTER.equals(e.getKey())) {
                request.setQueryFilter(QueryFilters.parse(String.valueOf(e.getValue())));
            } else if (PARAM_FIELDS.equals(e.getKey())) {
                request.addField(String.valueOf(e.getValue()).split(","));
            } else {
                request.setAdditionalParameter(e.getKey(), String.valueOf(e.getValue()));
            }
        }

        return request;
    }

    /**
     * @param queryCfg the query configuration
     * @return true if the query configuration explicitly defines the query to execute, false if not
     */
    public static boolean hasQueryId(JsonValue queryCfg) {
        return queryCfg.isDefined(PARAM_QUERY_ID) || queryCfg.isDefined(FIELD_QUERY_ID);
    }

    /**
     * @param queryCfg The query configuration
     * @return true if the query configuration explicitly defines the query to execute, false if not
     */
    public static boolean hasQueryExpression(JsonValue queryCfg) {
        return queryCfg.isDefined(PARAM_QUERY_EXPRESSION) || queryCfg.isDefined(FIELD_QUERY_EXPRESSION);
    }

    /**
     * @param queryCfg The query configuration
     * @return true if the query configuration explicitly defines the query to execute, false if not
     */
    public static boolean hasQueryFilter(JsonValue queryCfg) {
        return queryCfg.isDefined(PARAM_QUERY_FILTER) || queryCfg.isDefined(FIELD_QUERY_FILTER);
    }

}
