/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
 */

/*global define */

define("org/forgerock/openidm/ui/common/delegates/SearchDelegate", [
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate"
], function(_, constants, AbstractDelegate) {

    var obj = new AbstractDelegate(constants.host + "/openidm");

    obj.searchResults = function (resource, props, searchString, comparisonOperator) {
        var operator = (comparisonOperator) ? comparisonOperator : "sw",
            maxPageSize = 10,
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

        return this.serviceCall({
            "type": "GET",
            "url":  "/" + resource + "?_sortKeys=" + props[0] + "&_pageSize=" + maxPageSize + "&_queryFilter=" + conditions.join(" or (") + new Array(conditions.length).join(")")// [a,b] => "a or (b)"; [a,b,c] => "a or (b or (c))"
        }).then(
            function (qry) {
                return _.first(qry.result,maxPageSize);//we never want more than 10 results from search in case _pageSize does not work
            },
            function (error){
                console.error(error);
            }
        );
    };

    return obj;
});
