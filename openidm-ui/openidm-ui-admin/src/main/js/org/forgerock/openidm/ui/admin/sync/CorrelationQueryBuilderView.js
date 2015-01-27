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

define("org/forgerock/openidm/ui/admin/sync/CorrelationQueryBuilderView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor"
], function(AdminAbstractView,
            eventManager,
            constants,
            ConfigDelegate,
            InlineScriptEditor) {

    var CorrelationQueryBuilderView = AdminAbstractView.extend({
        template: "templates/admin/sync/CorrelationQueryBuilderTemplate.html",
        element: "#correlationQueryBuilderView",
        noBaseTemplate: true,
        events: {
            //"click .expressionTree button.remove": "deleteField",
            //"click .expressionTree button.add": "showExpressionMenu",
            //"click .expressionMenu .addField": "addExpressionField",
            //"click .expressionMenu .addGroup": "addExpressionGroup",
            //"click .expressionMenu .parentMenuChoice": _.noop(),
            //"change .expressionTree select": "updateType",
            "change .queryType": "changeQueryType",
            "change .linkQualifier": "validate"
        },
        data: {},
        model: {
            scriptData: null
        },

        /**
         * The properties following "reRender" are only passed is when the expression tree is requires re-rendering.
         *
         * @param args
         *      mapping
         *      mappingName
         *      linkQualifiers
         *      linkQualifier
         *      validation
         *      edit
         *
         *      reRender
         *      expressionTree
         *      script
         *      isAny
         *      selected
         */
        render: function(args) {
            //var hasEmptyGroups,
            var expressionTree,
                correlationQuery = null;

            this.model.mapping = args.mapping;
            this.model.mappingName = args.mappingName;
            this.model.linkQualifiers = args.linkQualifiers;
            this.model.linkQualifier = args.linkQualifier;
            this.model.validation = args.validation;
            this.model.edit = args.edit;

            //this.model.reRender = args.reRender;
            //this.data.expressionTree = expressionTree = {"any" : []}; // Expression tree default value
            //if (_.isUndefined(args.isAny)){
            //    this.data.isAny = true;
            //} else {
            //    this.data.isAny = args.isAny;
            //}

            //this.data.fieldNames = _.chain(this.model.mapping.properties)
            //    .pluck("target")
            //    .sortBy(function (name) { return name; })
            //    .value();

            // This is the first load and we are editing an existing query
            if (!this.model.reRender && this.model.edit) {
                correlationQuery = _.find(this.model.mapping.correlationQuery, {"linkQualifier": this.model.linkQualifier});

                if (_.has(correlationQuery, "expressionTree")) {
                    this.data.expressionTree = expressionTree = correlationQuery.expressionTree;

                } else if (_.has(correlationQuery, "type")) {
                    this.model.scriptData = _.omit(correlationQuery, "linkQualifier");
                }

                //} else if (this.model.reRender && args.selected === "expression") {
                //    this.data.expressionTree = expressionTree = args.expressionTree;

            } else if (this.model.reRender && args.selected === "script") {
                this.model.scriptData = args.script;
            }

            this.parentRender(function () {
                //function emptyGroups(node) {
                //    var returnVal = true,
                //        arrayCheck = function (v) {
                //            if (_.isObject(v)) {
                //                returnVal = emptyGroups(v);
                //            } else {
                //                returnVal = false;
                //            }
                //        };
                //    if (_.has(node, "any")) {
                //        _.each(node.any, arrayCheck);
                //    } else {
                //        _.each(node.all, arrayCheck);
                //    }
                //
                //    return returnVal;
                //}

                if (this.model.edit) {
                    this.$el.find(".linkQualifier").prop('disabled', true);
                }

                this.$el.find(".linkQualifier").val(args.linkQualifier);

                this.model.scriptEditor = InlineScriptEditor.generateScriptEditor({
                    "element": $(".queryScript"),
                    "eventName": "",
                    "noValidation": true,
                    "scriptData": this.model.scriptData,
                    "onChange" :  _.bind(this.validate, this),
                    "onBlur" :  _.bind(this.validate, this)
                }, _.bind(function() {
                    // There is a correlationQuery
                    //if (this.model.reRender || correlationQuery) {
                    //    if (_.has(correlationQuery, "expressionTree") || args.selected === "expression") {
                    //        hasEmptyGroups = emptyGroups(this.data.expressionTree);
                    //        this.$el.find("#expressionTreeQueryRadio").prop("checked", true);
                    //        this.$el.find(".queryScript").hide();
                    //
                    //    } else if (_.has(correlationQuery, "type") || args.selected === "script") {
                    //        this.$el.find("#scriptQueryRadio").prop("checked", true);
                    //        this.$el.find(".expressionTree").hide();
                    //    }
                    //} else {
                    //    this.$el.find(".queryScript").hide();
                    //}

                    //this.$el.find(".expressionTree .expressionMenu").menu({
                    //    position: { my: "right top", at: "left-4 top-6" }
                    //}).hide();
                    //
                    //this.$el.find(".expressionTree .remove:first").prop('disabled', true);
                    this.validate();
                }, this));
            });
        },

        validLinkQualifier: function (linkQualifier) {
            if (linkQualifier.length > 0 && (
                (this.model.edit) ||
                (!this.model.edit && _.indexOf(this.model.linkQualifiers, linkQualifier) === -1))) {
                return true;
            } else {
                return false;
            }
        },

        validate: function() {
            // If the no correlation radio is selected or the script is selected and has a script hook or the expression is selected and has a field
            //if (((this.$el.find("#scriptQueryRadio").prop("checked") && this.model.scriptEditor.generateScript() !== null) ||
            //    (this.$el.find("#expressionTreeQueryRadio").prop("checked") && this.$el.find(".expressionTree li[field]").length > 0)) &&
            if (this.model.scriptEditor.generateScript() !== null && this.validLinkQualifier(this.$el.find(".linkQualifier").val())) {
                this.model.validation(true);
            } else {
                this.model.validation(false);
            }
        },
        getQuery: function() {
            var data = {
                linkQualifier: this.$el.find(".linkQualifier").val()
            };
            //if (this.$el.find(".queryType:checked").val() === "expression") {
            //    _.extend(data, {
            //        expressionTree: this.data.expressionTree,
            //        mapping: this.model.mappingName,
            //        type: "text/javascript",
            //        file: "ui/correlateTreeToQueryFilter.js"
            //    });
            //} else {
            _.extend(data, this.model.scriptEditor.generateScript());
            //}

            return data;
        },

        clear: function() {
            this.data = {};
            this.model = {};
        }

        //renderExpressionTree: function() {
        //    this.render({
        //        mapping: this.model.mapping,
        //        mappingName: this.model.mappingName,
        //        linkQualifiers: this.model.linkQualifiers,
        //        linkQualifier: this.$el.find(".linkQualifier").val(),
        //        validation: this.model.validation,
        //        edit: this.model.edit,
        //
        //        reRender: true,
        //        expressionTree: this.data.expressionTree,
        //        script: this.model.scriptEditor.generateScript(),
        //        isAny: _.has(this.data.expressionTree, "any") || false,
        //        selected: this.$el.find(".queryType:checked").val()
        //    });
        //
        //    this.validate();
        //},

        //changeQueryType: function() {
        //    _.each(this.$el.find(".queryType"), function(which) {
        //        if ($(which).prop("checked")) {
        //            $(which).parent().find(".queryContainer").slideDown();
        //        } else {
        //            $(which).parent().find(".queryContainer").slideUp();
        //        }
        //    });
        //
        //    this.validate();
        //},


        //,
        //
        //showExpressionMenu: function (e) {
        //    e.preventDefault();
        //
        //    var clickedEle = e.target;
        //
        //    if ($(clickedEle).not("button")) {
        //        clickedEle = $(clickedEle).closest("button");
        //    }
        //
        //    this.$el.find(".expressionMenu").hide();
        //
        //    $(clickedEle).next(".expressionMenu").show().hover(_.noop(), function () {
        //        $(this).hide();
        //    });
        //},
        //
        //getExpressionContext: function (e) {
        //    e.preventDefault();
        //
        //    var objectPath = _.map(
        //            $(e.target).parentsUntil(".expressionTree fieldset>div", ".node[index]"),
        //            function(li) {
        //                return $(li).attr("index");
        //            }
        //        ).reverse(),
        //        previousNode = null,
        //        node = this.data.expressionTree;
        //
        //    _.each(objectPath, function (index) {
        //        previousNode = node;
        //        node = node[index];
        //    });
        //
        //    return {current: node, parent: previousNode, path: objectPath};
        //},
        //
        //addExpressionField: function (e) {
        //    var node = this.getExpressionContext(e).current,
        //        field = $(e.target).text();
        //
        //    node.push(field);
        //    this.renderExpressionTree();
        //},
        //
        //addExpressionGroup: function (e) {
        //    var node = this.getExpressionContext(e).current;
        //
        //    node.push({"any": []});
        //    this.renderExpressionTree();
        //},
        //
        //deleteField: function (e) {
        //    var objectPath = this.getExpressionContext(e).path,
        //        toRemove = objectPath.splice(-1,1),
        //        previousNode = null,
        //        node = this.data.expressionTree;
        //
        //    if (toRemove.length) {
        //        _.each(objectPath, function (index) {
        //            previousNode = node;
        //            node = node[index];
        //        });
        //
        //        if (_.isArray(node)) {
        //            node.splice(toRemove[0],1);
        //        } else if (_.isArray(previousNode) && objectPath.length) {
        //            previousNode.splice(objectPath.splice(-1,1)[0],1);
        //        }
        //    }
        //
        //    this.renderExpressionTree();
        //},
        //
        //updateType: function (e) {
        //    var node = this.getExpressionContext(e).parent,
        //        newVal = $(e.target).val(),
        //        oldVal = newVal === "any" ? "all" : "any";
        //
        //    node[newVal] = node[oldVal];
        //    delete node[oldVal];
        //
        //    this.renderExpressionTree();
        //}
    });
    //,
    //expressionClosure;

    //Handlebars.registerHelper("expressionDisplay", function (rules, fieldNames, options) {
    //    var returnVal = '';
    //
    //    if (options.fn !== undefined) {
    //        expressionClosure = options.fn;
    //    }
    //
    //    _.each(rules, function (fields, type) {
    //        var currentField = 0;
    //        returnVal += expressionClosure({
    //            "types": [  {
    //                "value": "any",
    //                "label": $.t("templates.correlation.query.correlationAny"),
    //                "selected": type === "any"
    //            }, {
    //                "value": "all",
    //                "label": $.t("templates.correlation.query.correlationAll"),
    //                "selected": type === "all"
    //            }],
    //            "filteredFieldNames": _.filter(fieldNames, function (f) { return _.indexOf(fields, f) === -1; }),
    //            "term": (type === "any") ? $.t("templates.correlation.query.or") : $.t("templates.correlation.query.and"),
    //            "expressionTree": _.map(fields, function (v) {
    //                currentField++;
    //                return {
    //                    "req": v,
    //                    "fieldNames": fieldNames,
    //                    "isObject": (typeof v === "object"),
    //                    "isAny": (typeof v === "object" && _.has(v, "any")),
    //                    "index": (currentField-1),
    //                    "notLast": (currentField !== fields.length)
    //                };
    //            })
    //        });
    //    });
    //
    //    return returnVal;
    //});
    return new CorrelationQueryBuilderView();
});