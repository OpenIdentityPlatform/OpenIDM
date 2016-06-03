/**
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
 * Copyright 2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/common/delegates/SearchDelegate", [
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate"
], function(_, constants, AbstractDelegate) {

    var obj = new AbstractDelegate(constants.host + "/openidm");

    obj.searchResults = function (resource, props, searchString, comparisonOperator, additionalQuery) {
        var maxPageSize = 10;

        return this.serviceCall({
            "type": "GET",
            "url":  "/" + resource + "?_sortKeys=" + props[0] + "&_pageSize=" + maxPageSize + "&_queryFilter=" + obj.generateQueryFilter(props, searchString, additionalQuery, comparisonOperator)// [a,b] => "a or (b)"; [a,b,c] => "a or (b or (c))"
        }).then(
            function (qry) {
                return _.take(qry.result, maxPageSize);//we never want more than 10 results from search in case _pageSize does not work
            },
            function (error){
                console.error(error);
            }
        );
    };

    obj.generateQueryFilter = function(props, searchString, additionalQuery, comparisonOperator) {
        var operator = (comparisonOperator) ? comparisonOperator : "sw",
            queryFilter,
            conditions = _(props)
            .reject(function(p){ return !p; })
            .map(function(p){
                var op = operator;

                if(p === "_id" && op !== "neq"){
                    op = "eq";
                }

                if(op !== "pr") {
                    return p + ' ' + op + ' "' + encodeURIComponent(searchString) + '"';
                } else {
                    return p + ' pr';
                }
            })
            .value();

        queryFilter = "(" + conditions.join(" or (") + new Array(conditions.length).join(")") +")";

        if(additionalQuery) {
            queryFilter = "(" +queryFilter+" and (" +additionalQuery +"))";
        }

        return queryFilter;
    };

    return obj;
});
