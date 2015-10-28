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

define("org/forgerock/openidm/ui/admin/mapping/association/correlationQuery/CorrelationQueryBuilderView", [
    "jquery",
    "underscore",
    "handlebars",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor"
], function($, _, Handlebars, AdminAbstractView, InlineScriptEditor) {

    var CorrelationQueryBuilderView = AdminAbstractView.extend({
        template: "templates/admin/mapping/association/correlationQuery/CorrelationQueryBuilderTemplate.html",
        element: "#correlationQueryBuilderView",
        noBaseTemplate: true,
        events: {
            "click .expressionTree button.remove": "deleteField",
            "click .expressionTree button.add": "showExpressionMenu",
            "click .expressionMenu .addField": "addExpressionField",
            "click .expressionMenu .addGroup": "addExpressionGroup",
            "click .expressionMenu .parentMenuChoice": _.noop(),
            "change .expressionTree select": "updateType",
            "change .queryType": "changeQueryType",
            "change .linkQualifier": "updateFields"
        },
        data: {},
        model: {
            scriptData: null
        },

        /**
         * The properties following "reRender" are only passed is when the expression tree is requires re-rendering.
         *
         * @param args
         *      query
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
            var hasEmptyGroups;

            this.model.query = args.query;
            this.model.mapping = args.mapping;
            this.model.mappingName = args.mappingName;
            this.model.linkQualifiers = args.linkQualifiers;
            this.model.addedLinkQualifiers = args.addedLinkQualifiers;
            this.model.linkQualifier = args.linkQualifier;
            this.model.validation = args.validation;
            this.model.edit = args.edit;
            this.model.reRender = args.reRender;

            if (!this.model.edit) {
                this.data.availableLinkQualifiers = (_.difference(this.model.linkQualifiers, this.model.addedLinkQualifiers));
            } else {
                this.data.availableLinkQualifiers = [this.model.linkQualifier];
            }

            if (this.model.edit && this.model.linkQualifiers.indexOf(this.model.linkQualifier) === -1) {
                this.data.missingLinkQualifier = true;
            }

            this.data.fieldNames = [];

            _.each(this.model.mapping.properties, function(property) {
                // Link qualifier must match selected link qualifier
                if ( (_.has(property, "condition") && _.has(property.condition, "linkQualifier") && property.condition.linkQualifier === this.model.linkQualifier) ||
                        // Or if there is a condition there cannot be a linkQualifier
                    (_.has(property, "condition") && !_.has(property.condition, "linkQualifier") ) ||
                        // Or there cannot be any condition
                    (!_.has(property, "condition") ) ) {
                    this.data.fieldNames.push(property.target);
                }
            }, this);

            this.data.fieldNames = _.chain(this.data.fieldNames)
                .unique()
                .sortBy("name")
                .value();

            this.data.expressionTree = {"any" : []}; // Expression tree default value

            // This is the first load and we are editing an existing query
            if (!this.model.reRender && this.model.edit) {
                if (_.has(this.model.query, "expressionTree")) {
                    this.data.expressionTree = this.model.query.expressionTree;

                } else if (_.has(this.model.query, "type")) {
                    this.model.scriptData = _.omit(this.model.query, "linkQualifier", "deleted", "added", "edited", "changes");
                }

            } else if (this.model.reRender && args.selected === "expression") {
                this.data.expressionTree = args.expressionTree;

            } else if (this.model.reRender && args.selected === "script") {
                this.model.scriptData = args.script;
            }

            if (_.has(this.data.expressionTree, "any")) {
                this.data.isAny = true;
            } else if (_.has(this.data.expressionTree, "all")) {
                this.data.isAny = false;
            }

            this.parentRender(_.bind(function () {
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
                    if (this.model.reRender || this.model.query) {
                        if (_.has(this.model.query, "expressionTree") || args.selected === "expression") {
                            hasEmptyGroups = emptyGroups(this.data.expressionTree);
                            this.$el.find("#expressionTreeQueryRadio").prop("checked", true);
                            this.$el.find(".queryScript").hide();

                        } else if (_.has(this.model.query, "type") || args.selected === "script") {
                            this.$el.find("#scriptQueryRadio").prop("checked", true);
                            this.$el.find(".expressionTree").hide();
                        }
                    } else {
                        this.$el.find(".queryScript").hide();
                    }


                    this.$el.find(".expressionTree .remove:first").prop('disabled', true);
                    this.validate();
                }, this));
            }, this));
        },

        validLinkQualifier: function (linkQualifier) {
            if (linkQualifier.length > 0 &&
                ( (this.model.edit) ||
                (!this.model.edit && _.indexOf(this.model.linkQualifiers, linkQualifier) > -1) ) ) {
                return true;
            } else {
                return false;
            }
        },

        updateFields: function() {
            // Only reset if changing link qualifiers
            if (this.model.linkQualifier) {
                this.data.expressionTree = {"any" : []};
            }
            this.renderExpressionTree();
            this.validate();
        },

        validate: function() {
            // If the no correlation radio is selected or the script is selected and has a script hook or the expression is selected and has a field
            if ( ( (this.$el.find("#scriptQueryRadio").prop("checked") && _.has(this.model, "scriptEditor") && this.model.scriptEditor.generateScript() !== null) ||
                (this.$el.find("#expressionTreeQueryRadio").prop("checked") && this.$el.find(".expressionTree li[field]").length > 0) ) &&
                this.validLinkQualifier(this.$el.find(".linkQualifier").val() || "") ) {
                this.model.validation(true);
            } else {
                this.model.validation(false);
            }
        },

        getQuery: function() {
            var query = {
                linkQualifier: this.$el.find(".linkQualifier").val()
            };

            if (this.$el.find(".queryType:checked").val() === "expression") {
                _.extend(query, {
                    expressionTree: this.data.expressionTree,
                    mapping: this.model.mappingName,
                    type: "text/javascript",
                    file: "ui/correlateTreeToQueryFilter.js"
                });
            } else {
                _.extend(query, this.model.scriptEditor.generateScript());
            }

            return query;
        },

        clear: function() {
            this.data = {};
            this.model = {};
        },

        renderExpressionTree: function() {
            this.render({
                query: this.model.query,
                mapping: this.model.mapping,
                mappingName: this.model.mappingName,
                linkQualifiers: this.model.linkQualifiers,
                addedLinkQualifiers: this.model.addedLinkQualifiers,
                linkQualifier: this.$el.find(".linkQualifier").val(),
                validation: this.model.validation,
                edit: this.model.edit,

                reRender: true,
                expressionTree: this.data.expressionTree,
                script: this.model.scriptEditor.generateScript(),
                isAny: _.has(this.data.expressionTree, "any") || false,
                selected: this.$el.find(".queryType:checked").val()
            });

            this.validate();
        },

        changeQueryType: function() {
            _.each(this.$el.find(".queryType"), function(which) {
                if ($(which).prop("checked")) {
                    $(which).parent().find(".queryContainer").slideDown();
                } else {
                    $(which).parent().find(".queryContainer").slideUp();
                }
            });

            this.validate();
        },

        showExpressionMenu: function (e) {
            e.preventDefault();

            var clickedEle = e.target;

            if ($(clickedEle).not("button")) {
                clickedEle = $(clickedEle).closest("button");
            }
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
    }), expressionClosure;

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
    return new CorrelationQueryBuilderView();
});
