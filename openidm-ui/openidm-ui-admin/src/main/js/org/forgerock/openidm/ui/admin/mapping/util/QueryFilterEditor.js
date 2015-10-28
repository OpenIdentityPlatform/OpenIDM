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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/mapping/util/QueryFilterEditor", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/FilterEditor",
    "org/forgerock/openidm/ui/admin/util/QueryFilterUtils"
], function ($, _, FilterEditor, queryFitlerUtils) {
    var tagMap = {
                    "equalityMatch" : "eq",
                    "greaterOrEqual" : "ge",
                    "lessOrEqual" : "le",
                    "approxMatch" : "sw"
                },
        invertedTagMap = _.invert(tagMap),
        QueryFilterEditor = FilterEditor.extend({
            transform: function (queryFilterTree) {
                if (_.isArray(queryFilterTree)) {
                    if (queryFilterTree.length === 1) {
                        return {
                            "op": "and",
                            "children": [this.transform(queryFilterTree[0])]
                        };
                    } else {
                        return {
                            "op" : queryFilterTree[1],
                            "children" :   _.chain(queryFilterTree)
                                            .filter(function (n) {
                                                return typeof n === "object";
                                            })
                                            .map(this.transform, this)
                                            .value()
                        };
                    }
                } else if (_.isObject(queryFilterTree)) {
                    return {
                        "name" : queryFilterTree.field,
                        "op" : "expr",
                        "tag" : invertedTagMap[queryFilterTree.operator] || queryFilterTree.operator,
                        "value" : queryFilterTree.value
                    };
                }
            },
            serialize: function (node) {
                switch (node.op) {
                    case "expr":
                        if (node.tag === "pr") {
                            return node.name + ' pr';
                        } else {
                            return node.name + ' ' + (tagMap[node.tag] || node.tag) + ' "' + node.value + '"';
                        }
                    case "none":
                        return "";
                    default:
                        return "(" + _.map(node.children, this.serialize, this).join(" " + node.op + " ") + ")";
                }
            },
            getFilterString: function () {
                return this.serialize(this.data.filter);
            },
            render: function (args) {
                this.setElement(args.element);

                this.data = {
                    config: {
                        ops: [
                            "and",
                            "or",
                            "expr"
                        ],
                        tags: [
                            "equalityMatch",
                            "ne",
                            "approxMatch",
                            "co",
                            "greaterOrEqual",
                            "gt",
                            "lessOrEqual",
                            "lt"
                        ]
                    },
                    showSubmitButton: false
                };

                this.data.filterString = args.queryFilter;
                if (this.data.filterString !== "") {
                    this.data.queryFilterTree = queryFitlerUtils.convertFrom(this.data.filterString);
                    if (_.isArray(this.data.queryFilterTree) && this.data.queryFilterTree.length === 1) {
                        this.data.filter = this.transform(this.data.queryFilterTree[0]);
                    } else {
                        this.data.filter = this.transform(this.data.queryFilterTree);
                    }
                } else {
                    this.data.filter = { "op": "none", "children": []};
                }

                this.delegateEvents(this.events);

                this.renderExpressionTree();
            }
        });

    return QueryFilterEditor;

});
