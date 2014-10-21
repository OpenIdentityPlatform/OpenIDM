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

/*global define, $, _, Handlebars */

define("org/forgerock/openidm/ui/admin/sync/CorrelationQueryView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/util/ScriptEditor",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function(AdminAbstractView,
            eventManager,
            constants,
            ConfigDelegate,
            ScriptEditor,
            uiUtils,
            configDelegate) {

    var CorrelationQueryView = AdminAbstractView.extend({
            template: "templates/admin/sync/CorrelationQueryTemplate.html",
            element: "#correlationQueryView",
            noBaseTemplate: true,
            events: {
                "click .expressionTree button.remove": "deleteField",
                "click .expressionTree button.add": "showExpressionMenu",
                "click .expressionMenu .addField": "addExpressionField",
                "click .expressionMenu .addGroup": "addExpressionGroup",
                "click .expressionMenu .parentMenuChoice": _.noop(),
                "change .expressionTree select": "updateType",
                "click .saveCorrelationQuery": "saveQuery",
                "change .queryType": "changeQueryType"
            },
            data: {},
            dataModel: {},

            /**
             * The properties following "reRender" are only passed is when the expression tree is requires re-rendering.
             *
             * @param args {
             *      sync
             *      mapping
             *      mappingName
             *      reRender
             *      expressionTree
             *      script
             *      isAny
             *      selected
             *  }
             */
            render: function(args) {
                var hasEmptyGroups,
                    expressionTree;

                this.dataModel.sync = args.sync;
                this.dataModel.mapping = args.mapping;
                this.dataModel.mappingName = args.mappingName;

                this.data.expressionTree = {"any" : []};

                if (_.isUndefined(args.isAny)){
                    this.data.isAny = true;
                } else {
                    this.data.isAny = args.isAny;
                }

                this.data.fieldNames = _.chain(this.dataModel.mapping.properties)
                    .pluck("target")
                    .sortBy(function (name) { return name; })
                    .value();

                // This is the first load and there is a correlationQuery property
                if (!args.reRender && _.has(this.dataModel.mapping, "correlationQuery")) {
                    if (_.has(this.dataModel.mapping.correlationQuery, "expressionTree")) {
                        this.data.expressionTree = expressionTree = this.dataModel.mapping.correlationQuery.expressionTree;

                    } else if (_.has(this.dataModel.mapping.correlationQuery, "type")) {
                        this.dataModel.scriptData = this.dataModel.mapping.correlationQuery;
                    }

                } else if (args.reRender && args.selected === "expression") {
                    this.data.expressionTree = expressionTree = args.expressionTree;

                } else if (args.reRender && args.selected === "script") {
                    this.dataModel.scriptData = args.script;
                }

                this.parentRender(function () {
                    function emptyGroups(node) {
                        var returnVal = true,
                            arrayCheck = function (v) {
                                if (_.isObject(v)) {
                                    returnVal = emptyGroups(v);
                                } else {
                                    returnVal = false;
                                }
                            };
                        if (_.has(node, "any")) {
                            _.each(node.any, arrayCheck);
                        } else {
                            _.each(node.all, arrayCheck);
                        }

                        return returnVal;
                    }

                    this.dataModel.scriptEditor = ScriptEditor.generateScriptEditor({
                        "element": $(".queryScript"),
                        "eventName": " ",
                        "deleteElement": false,
                        "deleteCallback": _.bind(function() {
                            this.dataModel.scriptEditor.clearScriptHook();
                            this.showHideWarning();
                        }, this),
                        "scriptData": this.dataModel.scriptData,
                        "saveCallback": _.bind(this.showHideWarning, this)
                    });

                    // There is a correlationQuery
                    if (args.reRender || _.has(this.dataModel.mapping, "correlationQuery")) {
                        if (_.has(this.dataModel.mapping.correlationQuery, "expressionTree") || args.selected === "expression") {
                            hasEmptyGroups = emptyGroups(expressionTree);
                            this.$el.find("#expressionTreeQueryRadio").prop("checked", true);
                            this.$el.find(".queryScript").hide();

                        } else if (_.has(this.dataModel.mapping.correlationQuery, "type") || args.selected === "script") {
                            this.$el.find("#scriptQueryRadio").prop("checked", true);
                            this.$el.find(".expressionTree").hide();
                        }

                        // This is the fall through in case there is no correlation query for an initial render or a re-render
                    } else {
                        this.$el.find("#noCorrelationQueryRadio").prop("checked", true).change();
                        this.$el.find(".expressionTree").hide();
                        this.$el.find(".queryScript").hide();
                    }

                    this.$el.find(".expressionTree .expressionMenu").menu().hide();
                    this.$el.find(".expressionTree .remove:first").prop('disabled', true);
                    this.showHideWarning();
                });
            },

            showHideWarning: function() {
                // If the no correlation radio is selected or the script is selected and has a script hook or the expression is selected and has a field
                if (this.$el.find("#noCorrelationQueryRadio").prop("checked") ||
                    (this.$el.find("#scriptQueryRadio").prop("checked") && this.dataModel.scriptEditor.getScriptHook().script !== null) ||
                    (this.$el.find("#expressionTreeQueryRadio").prop("checked") && this.$el.find(".expressionTree li[field]").length > 0)) {

                    this.$el.find(".saveCorrelationQuery").prop('disabled', false);
                    this.$el.find("#correlationQueryError").hide();

                } else {
                    this.$el.find(".saveCorrelationQuery").prop('disabled', true);
                    this.$el.find("#correlationQueryError").show();
                }
            },

            renderExpressionTree: function() {
                this.render({
                    sync: this.dataModel.sync,
                    mapping: this.dataModel.mapping,
                    mappingName: this.dataModel.mappingName,
                    expressionTree: this.data.expressionTree,
                    script: this.dataModel.scriptEditor.getScriptHook().script,
                    isAny: _.has(this.data.expressionTree, "any") || false,
                    reRender: true,
                    selected: this.$el.find(".queryType:checked").val()
                });

                this.showHideWarning();
            },

            changeQueryType: function() {
                _.each(this.$el.find(".queryType"), function(which) {
                    if ($(which).prop("checked")) {
                        $(which).parent().find(".queryContainer").slideDown();
                    } else {
                        $(which).parent().find(".queryContainer").slideUp();
                    }
                });

                this.showHideWarning();
            },

            saveQuery: function (e) {
                e.preventDefault();

                var query = {};

                switch (this.$el.find(".queryType:checked").val()) {
                    case "none":
                        if (_.has(this.dataModel.mapping, "correlationQuery")) {
                            delete this.dataModel.mapping.correlationQuery;
                        }
                        break;

                    case "expression":
                        this.dataModel.mapping.correlationQuery = {
                            expressionTree: this.data.expressionTree,
                            mapping: this.dataModel.mappingName,
                            type: "text/javascript",
                            file: "ui/correlateTreeToQueryFilter.js"
                        };
                        break;

                    case "script":
                        this.dataModel.mapping.correlationQuery = this.dataModel.scriptEditor.getScriptHook().script;
                        break;
                }

                _.each(this.dataModel.sync.mappings, function(map, key) {
                    if (map.name === this.dataModel.mappingName) {
                        this.dataModel.sync.mappings[key] = this.dataModel.mapping;
                    }
                }, this);

                ConfigDelegate.updateEntity("sync", this.dataModel.sync).then(function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "correlationQuerySaveSuccess");
                });
            },

            showExpressionMenu: function (e) {
                e.preventDefault();

                var clickedEle = e.target;

                if ($(clickedEle).not("button")) {
                    clickedEle = $(clickedEle).closest("button");
                }

                this.$el.find(".expressionMenu").hide();

                $(clickedEle).next(".expressionMenu").show().hover(_.noop(), function () {
                    $(this).hide();
                });
            },

            getExpressionContext: function (e) {
                e.preventDefault();

                var objectPath = _.map(
                        $(e.target).parentsUntil(".expressionTree fieldset>div", ".node[index]"),
                        function(li) {
                            return $(li).attr("index");
                        }
                    ).reverse(),
                    previousNode = null,
                    node = this.data.expressionTree;

                _.each(objectPath, function (index) {
                    previousNode = node;
                    node = node[index];
                });

                return {current: node, parent: previousNode, path: objectPath};
            },

            addExpressionField: function (e) {
                var node = this.getExpressionContext(e).current,
                    field = $(e.target).text();

                node.push(field);
                this.renderExpressionTree();
            },

            addExpressionGroup: function (e) {
                var node = this.getExpressionContext(e).current;

                node.push({"any": []});
                this.renderExpressionTree();
            },

            deleteField: function (e) {
                var objectPath = this.getExpressionContext(e).path,
                    toRemove = objectPath.splice(-1,1),
                    previousNode = null,
                    node = this.data.expressionTree;

                if (toRemove.length) {
                    _.each(objectPath, function (index) {
                        previousNode = node;
                        node = node[index];
                    });

                    if (_.isArray(node)) {
                        node.splice(toRemove[0],1);
                    } else if (_.isArray(previousNode) && objectPath.length) {
                        previousNode.splice(objectPath.splice(-1,1)[0],1);
                    }
                }

                this.renderExpressionTree();
            },

            updateType: function (e) {
                var node = this.getExpressionContext(e).parent,
                    newVal = $(e.target).val(),
                    oldVal = newVal === "any" ? "all" : "any";

                node[newVal] = node[oldVal];
                delete node[oldVal];

                this.renderExpressionTree();
            }
        }),
        expressionClosure;

    Handlebars.registerHelper("expressionDisplay", function (rules, fieldNames, options) {
        var returnVal = '';

        if (options.fn !== undefined) {
            expressionClosure = options.fn;
        }

        _.each(rules, function (fields, type) {
            var currentField = 0;
            returnVal += expressionClosure({
                "types": [  {
                    "value": "any",
                    "label": $.t("templates.correlation.query.correlationAny"),
                    "selected": type === "any"
                }, {
                    "value": "all",
                    "label": $.t("templates.correlation.query.correlationAll"),
                    "selected": type === "all"
                }],
                "filteredFieldNames": _.filter(fieldNames, function (f) { return _.indexOf(fields, f) === -1; }),
                "term": (type === "any") ? $.t("templates.correlation.query.or") : $.t("templates.correlation.query.and"),
                "expressionTree": _.map(fields, function (v) {
                    currentField++;
                    return {
                        "req": v,
                        "fieldNames": fieldNames,
                        "isObject": (typeof v === "object"),
                        "isAny": (typeof v === "object" && _.has(v, "any")),
                        "index": (currentField-1),
                        "notLast": (currentField !== fields.length)
                    };
                })
            });
        });

        return returnVal;
    });

    return new CorrelationQueryView();
});
