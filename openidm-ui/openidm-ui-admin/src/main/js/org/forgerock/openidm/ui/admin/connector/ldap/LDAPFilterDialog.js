/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global define, window, $, _ , Handlebars*/

define("org/forgerock/openidm/ui/admin/connector/ldap/LDAPFilterDialog", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "ldapjs-filter"
], function (AbstractView, conf, uiUtils, ldapjs) {
    var isRootFilter = true,
        ActiveDirectoryFilterDialog = AbstractView.extend({
            template: "templates/admin/connector/ldap/SetupFilter.html",
            el: "#dialogs",
            data: {},
            events: {
                "click input[type=submit]": "returnLdapString",
                "change .expressionTree :input": "updateNodeValue",
                "click .expressionTree .add-btn": "addNode",
                "click .expressionTree .remove-btn": "removeNode"
            },
            returnLdapString: function (e) {
                e.preventDefault();
                if (_.has(this.data.filter, "op") && this.data.filter.op === "none") {
                    this.updatePromise.resolve("");
                } else {
                    this.updatePromise.resolve(ldapjs.serializeFilterTree(this.data.filter));
                }
                this.close();
            },
            close: function () {
                if(this.currentDialog) {
                    try {
                        this.currentDialog.dialog('destroy').remove();
                    } catch(e) {
                        // perhaps the dialog hasn't been initialized ?
                    }
                }
            },
            getExpressionContext: function (e) {
                e.preventDefault();
                var objectPath =   _.map(
                        $(e.target).parentsUntil(".expressionTree fieldset>div", ".node[index]"),
                        function(li) {
                            return $(li).attr("index");
                        }
                    ).reverse(),
                    previousNode = null,
                    node = this.data.filter;


                _.each(objectPath, function (index) {
                    if (index.length) {
                        previousNode = node;
                        node = node.children[index];
                    }

                });

                return {current: node, parent: previousNode, path: objectPath};
            },
            removeNode: function (e) {
                var context = this.getExpressionContext(e);

                if (!_.isNull(context.parent)) {
                    context.parent.children = _.reject(context.parent.children, function (c) { return c === context.current; });
                    if (context.parent.children.length !== 0) {
                        this.data.filterString = ldapjs.serializeFilterTree(this.data.filter);
                    } else {
                        e.target = $(":input:first", $(e.target).parents(".node[index]")[1])[0];
                        return this.removeNode(e);
                    }
                } else {
                    this.data.filter = { "op": "none", "children": []};
                    this.data.filterString = "";
                }

                this.renderExpressionTree();
            },
            addNode: function (e) {
                var context = this.getExpressionContext(e),
                    node = context.current;

                node.children.push({name: "", value: "", tag: "equalityMatch", children: [], op: "expr"});

                this.data.filterString = ldapjs.serializeFilterTree(this.data.filter);

                this.renderExpressionTree();
            },
            updateNodeValue: function (e) {
                var context = this.getExpressionContext(e),
                    node = context.current,
                    field = $(e.target),
                    redrawContainer = false;

                if (field.hasClass("op")) {
                    redrawContainer = true;
                    node.op = field.val();
                    if (node.op === "expr") {
                        node.name = "";
                        node.value = "";
                        node.tag = "equalityMatch";
                        node.children = [];
                    } else if (node.op === "none") {
                        node.children = [];
                    } else if (!node.children || !node.children.length) {
                        node.children = [{name: "", value: "", tag: "equalityMatch", children: [], op: "expr"}];
                    }
                } else if (field.hasClass("name")) {

                    if (field.siblings(".tag").val() === "extensibleMatchAND") {
                        node.extensible.matchType=field.val();
                        node.name = field.val() + ":1.2.840.113556.1.4.803";
                    } else if (field.siblings(".tag").val() === "extensibleMatchOR") {
                        node.extensible.matchType = field.val();
                        node.name = field.val() + ":1.2.840.113556.1.4.804";
                    } else {
                        node.name = field.val();
                    }

                } else if (field.hasClass("tag")) {

                    if (field.val() === "extensibleMatchAND") {
                        node.tag = "extensibleMatch";
                        node.extensible = {
                            matchType: field.siblings(".name").val(),
                            rule: "1.2.840.113556.1.4.803",
                            value: field.siblings(".value").val(),
                            type: 169 // comes from ldapjs protocol definition for FILTER_EXT
                        };
                        node.name = field.siblings(".name").val() + ":1.2.840.113556.1.4.803";
                    } else if (field.val() === "extensibleMatchOR") {
                        node.tag = "extensibleMatch";
                        node.extensible = {
                            matchType: field.siblings(".name").val(),
                            rule: "1.2.840.113556.1.4.804",
                            value: field.siblings(".value").val(),
                            type: 169 // comes from ldapjs protocol definition for FILTER_EXT
                        };
                        node.name = field.siblings(".name").val() + ":1.2.840.113556.1.4.804";
                    } else {
                        delete node.extensible;
                        node.name = field.siblings(".name").val();
                        node.tag = field.val();
                    }

                } else if (field.hasClass("value")) {

                    if (field.siblings(".tag").val().match(/^extensibleMatch/)) {
                        node.extensible.value = field.val();
                    }

                    node.value = field.val();
                }

                if (node.op !== "none") {
                    this.data.filterString = ldapjs.serializeFilterTree(this.data.filter);
                } else {
                    this.data.filterString = "";
                }

                if (redrawContainer) {
                    this.renderExpressionTree();
                } else {
                    this.$el.find(".filter").text(this.data.filterString);
                }
            },

            renderExpressionTree: function () {
                isRootFilter = true;
                uiUtils.renderTemplate(this.template, this.$el, _.extend(conf.globalData, this.data), $.noop(), "replace");
            },
            render: function (params) {
                var _this = this;
                if (typeof params.filterString === "string" && params.filterString.length) {
                    this.data.filter = ldapjs.buildFilterTree(params.filterString);
                } else {
                    this.data.filter = { "op": "none", "children": []};
                }
                this.data.filterString = params.filterString;
                this.updatePromise = params.promise;

                this.currentDialog = $('<div id="attributeDialog"></div>');
                this.setElement(this.currentDialog);
                $('#dialogs').append(this.currentDialog);

                this.currentDialog.dialog({
                    title: $.t("templates.connector.ldapConnector.filter.title.dialog", {type: params.type}),
                    modal: true,
                    resizable: false,
                    width:'770px',
                    position: { my: "center", at: "center", of: window },
                    close: function () {
                        if(_this.currentDialog) {
                            try {
                                _this.currentDialog.dialog('destroy').remove();
                            } catch(e) {
                                // perhaps the dialog hasn't been initialized ?
                            }
                        }
                    },
                    open: _.bind(this.renderExpressionTree, this)
                });
            }
        }),
        filterDisplayClosure;

    Handlebars.registerHelper("filterDisplay", function (rules, options) {
        var returnVal = '',
            ops = ["and","or","not","expr"],
            tags = ["equalityMatch","greaterOrEqual","lessOrEqual","extensibleMatchAND","extensibleMatchOR"];

        if (isRootFilter) {
            ops.unshift("none");
            isRootFilter = false;
        }

        if (options.fn !== undefined) {
            filterDisplayClosure = options.fn;
        }

        if (rules.children && rules.children.length) {
            rules.children = _.map(rules.children, function (c, index) { return _.extend(c, {index: index, lastChild: false, hasMultiple: (rules.children.length > 1)}); });
            rules.children[rules.children.length-1].lastChild = true;
        }

        returnVal += filterDisplayClosure(_.extend(rules, {
            "options":_.map(ops, function (o) {
                return {
                    "value": o,
                    "label": $.t("templates.connector.ldapConnector.filter.options." + o),
                    "selected": rules.op === o
                };
            }),
            "tags": _.map(tags, function (o) {
                if (o === "extensibleMatchAND") {

                    return {
                        "value": o,
                        "label": $.t("templates.connector.ldapConnector.filter.tags.extensible.rules.bitAnd"),
                        "selected": rules.extensible && rules.extensible.rule === "1.2.840.113556.1.4.803"
                    };

                } else if (o === "extensibleMatchOR") {

                    return {
                        "value": o,
                        "label": $.t("templates.connector.ldapConnector.filter.tags.extensible.rules.bitOr"),
                        "selected": rules.extensible && rules.extensible.rule === "1.2.840.113556.1.4.804"
                    };

                } else {

                    return {
                        "value": o,
                        "label": $.t("templates.connector.ldapConnector.filter.tags." + o),
                        "selected": rules.tag === o
                    };

                }
            }),
            "delimiter": $.t("Delimiter: " + $.t("templates.connector.ldapConnector.filter.delimiters." + rules.op)), // will only be valid for and and or
            "isExpr": rules.op === "expr",
            "isMultiValueType": (rules.op === "and" || rules.op === "or")
        }));

        return returnVal;
    });


    return new ActiveDirectoryFilterDialog();

});
