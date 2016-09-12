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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "handlebars",
    "backbone",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingUtils",
    "org/forgerock/openidm/ui/admin/delegates/ScriptDelegate",
    "org/forgerock/openidm/ui/admin/util/FilterEvaluator",
    "org/forgerock/openidm/ui/admin/mapping/util/QueryFilterEditor",
    "org/forgerock/openidm/ui/admin/mapping/properties/AddPropertyMappingDialog",
    "org/forgerock/openidm/ui/admin/mapping/properties/EditPropertyMappingDialog",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function($, _, Handlebars, Backbone,
            AdminAbstractView,
            eventManager,
            conf,
            constants,
            mappingUtils,
            ScriptDelegate,
            FilterEvaluator,
            QueryFilterEditor,
            AddPropertyMappingDialog,
            EditPropertyMappingDialog,
            Backgrid,
            BackgridUtils,
            UIUtils) {

    var AttributesGridView = AdminAbstractView.extend({
        template: "templates/admin/mapping/properties/AttributesGridTemplate.html",
        element: "#attributesGrid",
        noBaseTemplate: true,
        events: {
            "click .add-property": "addProperty",
            "click #updateMappingButton": "saveMapping",
            "click #clearChanges": "clearChanges",
            "click #missingRequiredPropertiesButton": "addRequiredProperties"
        },
        partials: [
            "partials/mapping/properties/_IconContainerPartial.html",
            "partials/mapping/properties/_PropertyContainerPartial.html"
        ],
        model: {
            availableObjects: {},
            mappingProperties: null
        },
        data: {
            requiredProperties: [],
            missingRequiredProperties: []
        },
        sampleDisplay: [],

        render: function (args, callback) {
            this.model.renderArgs = _.clone(args, true);
            this.model.defaultMapping = _.clone(args.mapping, true);

            this.model.usesDragIcon = args.useDragIcon;
            this.model.staticSourceSample = args.staticSourceSample;
            this.model.mapping = _.clone(args.mapping, true);
            this.model.save = args.save;
            this.model.numRepresentativeProps = args.numRepresentativeProps;
            this.model.availableObjects = args.availableObjects;
            this.model.mappingProperties = _.clone(args.mappingProperties, true);

            this.data.usesDynamicSampleSource = args.usesDynamicSampleSource;
            this.data.usesLinkQualifier = args.usesLinkQualifier;
            this.data.requiredProperties = args.requiredProperties || [];
            this.data.missingRequiredProperties = [];

            if (this.data.usesLinkQualifier) {
                this.data.linkQualifiers = _.clone(args.linkQualifiers, true);
                this.model.currentLinkQualifier = this.data.linkQualifiers[0];
                this.data.hasLinkQualifiers = this.model.mapping.linkQualifiers;
            }

            if (conf.globalData.sampleSource && conf.globalData.sampleSource.IDMSampleMappingName === this.model.mapping.name && this.model.mapping.properties.length) {
                this.data.sampleSource_txt = conf.globalData.sampleSource[this.model.mapping.properties[0].source];
            }

            this.checkMissingRequiredProperties();

            this.parentRender(_.bind(function () {
                var mapProps = this.model.mappingProperties || _.clone(this.model.defaultMapping, true).properties,
                    sampleSource = {};

                if (conf.globalData.sampleSource && conf.globalData.sampleSource.IDMSampleMappingName === this.model.mapping.name) {
                    sampleSource = conf.globalData.sampleSource;
                }

                this.data.mapProps = mapProps;

                if (this.data.usesDynamicSampleSource) {
                    let autocompleteProps = _.pluck(this.model.mapping.properties,"source").slice(0, this.model.numRepresentativeProps);

                    mappingUtils.setupSampleSearch($("#findSampleSource", this.$el), this.model.mapping, autocompleteProps, (item)  => {
                        item.IDMSampleMappingName = this.model.mapping.name;
                        conf.globalData.sampleSource = item;
                        sampleSource = item;

                        this.gridFromMapProps(mapProps);
                    });
                } else {
                    conf.globalData.sampleSource = sampleSource = this.model.staticSourceSample;
                }

                this.gridFromMapProps(mapProps);

                this.checkChanges();

                if (callback){
                    callback();
                }
            }, this));
        },

        initSort: function() {
            BackgridUtils.sortable({
                "containers": [this.$el.find("#attributesGridHolder tbody")[0]],
                "rows": _.clone(this.model.mappingProperties, true)
            }, _.bind(this.setMappingProperties, this));
        },

        checkChanges: function () {
            var currentProperties = _.clone(this.model.defaultMapping, true).properties,
                changedProperties = this.model.mappingProperties || currentProperties,
                changesPending = !_.isEqual(currentProperties, changedProperties);

            if (changesPending) {
                this.$el.find("#updateMappingButton").prop('disabled', false);
                this.$el.find("#clearChanges").prop('disabled', false);
                this.$el.find(".changesPending").show();
            }
            else {
                this.$el.find("#updateMappingButton").prop('disabled', true);
                this.$el.find("#clearChanges").prop('disabled', true);
                this.$el.find(".changesPending").hide();
            }
        },

        addProperty: function (e) {
            e.preventDefault();

            AddPropertyMappingDialog.render({
                usesLinkQualifier: this.data.usesLinkQualifier,
                mappingProperties: this.model.mappingProperties,
                availProperties: this.model.availableObjects,
                targetSchema: this.model.availableObjects.target.schema,
                saveCallback: _.bind(function(props) {
                    this.setMappingProperties(props);
                }, this)
            });
        },

        clearChanges: function(e) {
            e.preventDefault();
            delete this.model.renderArgs.mappingProperties;
            this.render(this.model.renderArgs);
        },

        checkMissingRequiredProperties: function() {
            var props = this.model.mappingProperties || _.clone(this.model.defaultMapping, true).properties;

            _.each(this.data.requiredProperties, function(key) {
                if (!_.filter(props, function(p) {return p.target === key;}).length) {
                    this.data.missingRequiredProperties.push(key);
                }
            }, this);
        },

        addRequiredProperties: function(e) {
            var props = this.model.mappingProperties || this.data.mapProps;

            if (e) {
                e.preventDefault();
            }
            _.each(this.data.requiredProperties, function(key) {
                if (!_.filter(props, function(p) {return p.target === key;}).length){
                    props.push({target: key});
                }
            });

            this.setMappingProperties(props);
        },

        setMappingProperties: function(mappingProperties) {
            this.render(_.extend(this.model.renderArgs, {
                "mappingProperties" : mappingProperties
            }));
        },

        loadGrid: function(evalResults, attributes) {
            var attributesGrid,
                AttributesModel = Backbone.Model.extend({}),
                Attributes = Backbone.Collection.extend({ model: AttributesModel }),
                evalCounter = 0,
                tempResults = null,
                tempSample = null,
                _this = this,
                ClickableRow = Backgrid.Row.extend({
                    events: {
                        "click": "rowClick"
                    },
                    rowClick: function (event) {
                        if (!$(event.target).hasClass("fa-times")) {
                            EditPropertyMappingDialog.render({
                                usesLinkQualifier: _this.data.usesLinkQualifier,
                                id: this.model.attributes.id,
                                mappingProperties: _this.model.mappingProperties,
                                availProperties: _this.model.availableObjects.source.properties,
                                targetSchema: _this.model.availableObjects.target.schema,
                                saveCallback: function(props) {
                                    _this.setMappingProperties(props);
                                }
                            });
                        }
                    }
                });

            this.model.mappingProperties = attributes;
            this.model.attributes = new Attributes();

            _.each(attributes, function(attribute) {
                if(evalResults !== null) {
                    tempResults = evalResults[evalCounter];
                } else {
                    tempResults = null;
                }

                if(conf.globalData.sampleSource !== undefined && conf.globalData.sampleSource.IDMSampleMappingName === this.model.mapping.name && conf.globalData.sampleSource[attribute.source]) {
                    tempSample = conf.globalData.sampleSource[attribute.source];
                } else {
                    tempSample = null;
                }

                this.model.attributes.add({
                    "attribute": attribute,
                    "evalResult" : tempResults,
                    "sample" : tempSample,
                    "id" : evalCounter + 1
                });

                evalCounter++;
            }, this);

            attributesGrid = new Backgrid.Grid({
                className: "table backgrid",
                emptyText: $.t("templates.mapping.noMappingAttributes"),
                row: ClickableRow,
                columns: BackgridUtils.addSmallScreenCell([
                    {
                        name: "source",
                        sortable: false,
                        editable: false,
                        cell: Backgrid.Cell.extend({
                            render: function () {
                                var attributes = this.model.attributes,
                                    locals = {
                                        title: attributes.attribute.source,
                                        usesDragIcon: _this.model.usesDragIcon,
                                        isSource: true
                                    };

                                if (attributes.sample) {
                                    locals.textMuted = "(" + attributes.sample + ")";
                                }

                                this.$el.html(
                                    Handlebars.compile("{{> mapping/properties/_PropertyContainerPartial}}")({"locals": locals})
                                );

                                this.delegateEvents();

                                return this;
                            }
                        })
                    },
                    {
                        name: "",
                        sortable: false,
                        editable: false,
                        cell: Backgrid.Cell.extend({
                            className: "properties-icon-container-parent",
                            render: function () {
                                var locals = {},
                                    attribute = this.model.attributes.attribute;

                                if(attribute.condition) {
                                    if(_.isObject(attribute.condition)) {
                                        if (attribute.condition.source){
                                            locals.conditionIcon = attribute.condition.source;
                                        } else {
                                            locals.conditionIcon = "File: " + attribute.condition.file;
                                        }
                                    } else {
                                        locals.conditionIcon = attribute.condition;
                                    }
                                }

                                if(attribute.transform) {
                                    if(_.isObject(attribute.transform)) {
                                        if (attribute.transform.source) {
                                            locals.transformIcon = attribute.transform.source;
                                        } else {
                                            locals.transformIcon = "File: " + attribute.transform.file;
                                        }
                                    } else {
                                        locals.transformIcon = attribute.transform;
                                    }
                                }

                                this.$el.html(
                                    Handlebars.compile("{{> mapping/properties/_IconContainerPartial}}")({"locals": locals})
                                );

                                this.delegateEvents();

                                return this;
                            }
                        })
                    },
                    {
                        name: "target",
                        sortable: false,
                        editable: false,
                        cell: Backgrid.Cell.extend({
                            render: function () {
                                var locals = {},
                                    attributes = this.model.attributes;

                                if(attributes.attribute.target) {
                                    locals.title = attributes.attribute.target;
                                }

                                if(!attributes.evalResult || !attributes.evalResult.conditionResults || attributes.evalResult.conditionResults.result) {
                                    if (attributes.evalResult && attributes.evalResult.transformResults){
                                        locals.textMuted = attributes.evalResult.transformResults;
                                    } else if (attributes.sample !== null) {
                                        locals.textMuted = attributes.sample;
                                    } else if (attributes.attribute["default"]) {
                                        locals.textMuted = attributes.attribute["default"];
                                    }

                                    if (attributes.evalResult && attributes.evalResult.conditionResults && attributes.evalResult.conditionResults.results === false) {
                                        locals.textMuted = "";
                                    }
                                }

                                if (locals.textMuted) {
                                    locals.textMuted = "(" + locals.textMuted + ")";
                                }

                                this.$el.html(
                                    Handlebars.compile("{{> mapping/properties/_PropertyContainerPartial}}")({"locals": locals})
                                );

                                this.delegateEvents();

                                return this;
                            }
                        })
                    },
                    {
                        name: "",
                        cell: BackgridUtils.ButtonCell([
                            {
                                className: "fa fa-times grid-icon removeProperty",
                                callback: function(event){
                                    event.preventDefault();

                                    UIUtils.confirmDialog($.t("templates.mapping.confirmRemoveProperty",{property: this.model.attributes.attribute.target}), "danger", _.bind(function(){
                                        _this.model.mappingProperties.splice(($(event.target).parents("tr")[0].rowIndex - 1), 1);
                                        _this.checkChanges();
                                        _this.render(_.extend(_this.model.renderArgs, {
                                            "mappingProperties" : _this.model.mappingProperties
                                        }));
                                    }, this));
                                }
                            }
                        ]),
                        sortable: false,
                        editable: false
                    }]),
                collection: this.model.attributes
            });

            this.$el.find("#attributesGridHolder").empty();
            this.$el.find("#attributesGridHolder").append(attributesGrid.render().el);

            this.$el.find(".properties-badge").popover({
                content: function () { return $(this).find(".tooltip-details").clone().show();},
                placement:'top',
                container: 'body',
                html: 'true',
                title: ''
            });

            if (this.data.usesLinkQualifier) {
                this.$el.find("#linkQualifierSelect").change(_.bind(function (event) {
                    var element = event.target;

                    event.preventDefault();

                    if ($(element).val().length > 0) {
                        this.model.currentLinkQualifier = $(element).val();
                    }

                    this.gridFromMapProps(this.model.mappingProperties);
                    this.initSort();
                }, this));

                this.$el.find("#linkQualifierSelect").selectize({
                    placeholder: $.t("templates.mapping.linkQualifier"),
                    create: false,
                    sortField: 'text'
                });
            }

            this.initSort();
        },

        /**
            Produces grid data from mapping transform properties. After every property is processed
            (conditional and transform logic applied) the full set of details is provided to this.loadGrid.

            @param {array} props - array of maps, each of which represent the mapping logic necessary for a single target property
            @return {array} promises that will be resolved for each property in need of transformation
        */
        gridFromMapProps : function (props) {
            var propertyDetails = _.clone(props),
                evalPromises = [],
                globals = {
                    source : {}
                },
                evalCheck,
                tempDetails = {},
                sampleSource = conf.globalData.sampleSource || {};

            if (sampleSource.IDMSampleMappingName !== this.model.mapping.name) {
                sampleSource = {};
            }

            this.sampleDisplay = [];

            if (!_.isEmpty(sampleSource)) {
                _.each(propertyDetails, function (item) {

                    globals = {
                        source: {}
                    };

                    tempDetails = {};

                    if (this.data.usesLinkQualifier) {
                        globals.linkQualifier = this.model.currentLinkQualifier;
                    }

                    if (item.source !== "") {
                        globals.source = sampleSource[item.source];
                    } else {
                        globals.source = sampleSource;
                    }

                    if (item.condition) {
                        tempDetails.hasCondition = true;
                        tempDetails.condition = item.condition;
                    } else {
                        tempDetails.hasCondition = false;
                    }

                    if (item.transform) {
                        tempDetails.hasTransform = true;
                        tempDetails.transform = item.transform;
                    } else {
                        tempDetails.hasTransform = false;
                    }

                    evalCheck = this.sampleEvalCheck(tempDetails, globals);

                    tempDetails.results = evalCheck;

                    this.sampleDisplay.push(tempDetails);

                    evalPromises.push(evalCheck);
                }, this);
            }

            if (evalPromises.length > 0) {
                $.when.apply($, evalPromises).then(_.bind(function() {
                    this.loadGrid(arguments, props);
                }, this), function(e) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingEvalError");
                });
            } else {
                this.loadGrid(null, props);
            }

            return evalPromises;
        },

        //Returns a promise and determines if a transform and/or conditional needs to be eval
        sampleEvalCheck: function(sampleDetails, globals) {
            var samplePromise = $.Deferred(),
                sampleSource = conf.globalData.sampleSource || {};

            if (sampleSource.IDMSampleMappingName !== this.model.mapping.name) {
                return samplePromise.resolve(null);
            }

            if (sampleDetails.hasCondition) {
                if (_.isString(sampleDetails.condition)) {
                    ScriptDelegate.parseQueryFilter(sampleDetails.condition)
                    .then(function (queryFilterTree) {
                        var qfe = new QueryFilterEditor();

                        return FilterEvaluator.evaluate(
                            qfe.transform(queryFilterTree),
                            {
                                "linkQualifier": globals.linkQualifier,
                                "object": sampleSource
                            }
                        );
                    })
                    .then(function (filterCheck) {
                        if (filterCheck) {
                            if (sampleDetails.hasTransform) {
                                ScriptDelegate.evalScript(sampleDetails.transform, globals).then(function(transformResults) {
                                    samplePromise.resolve({
                                        conditionResults: {
                                            result: true
                                        },
                                        transformResults: transformResults
                                    });
                                }, function(e) {
                                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingEvalError");
                                });
                            } else {
                                samplePromise.resolve({
                                    conditionResults: {
                                        result: true
                                    }
                                });
                            }
                        } else {
                            samplePromise.resolve({
                                conditionResults: {
                                    result: false
                                },
                                transformResults: ""
                            });
                        }
                    });
                } else {
                    ScriptDelegate.evalScript(sampleDetails.condition, { "linkQualifier": globals.linkQualifier, "object": sampleSource}).then(function(conditionResults) {
                        if (sampleDetails.hasTransform && conditionResults === true) {
                            ScriptDelegate.evalScript(sampleDetails.transform, globals).then(function(transformResults) {
                                samplePromise.resolve({
                                    conditionResults: {
                                        result: conditionResults
                                    },
                                    transformResults: transformResults
                                });
                            }, function(e) {
                                let errorResponse = JSON.parse(e.responseText);

                                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingEvalError");

                                samplePromise.resolve({
                                    conditionResults: {
                                        result: conditionResults
                                    },
                                    transformResults:  $.t("templates.mapping.errorEvalTransform") +" " +errorResponse.message
                                });
                            });
                        } else {
                            samplePromise.resolve({
                                conditionResults: {
                                    result: conditionResults
                                }
                            });
                        }
                    }, function(e) {
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingEvalError");
                    });
                }
            } else if (sampleDetails.hasTransform) {
                ScriptDelegate.evalScript(sampleDetails.transform, globals).then(function(transformResults) {
                    samplePromise.resolve({
                        transformResults: transformResults
                    });
                }, function(e) {
                    let errorResponse = JSON.parse(e.responseText);

                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingEvalError");

                    samplePromise.resolve({
                        transformResults: $.t("templates.mapping.errorEvalTransform") +" " +errorResponse.message
                    });
                });
            } else {
                samplePromise.resolve(null);
            }

            return samplePromise;
        },

        saveMapping: function(e) {
            e.preventDefault();
            this.model.save(this.model.mappingProperties);
            this.model.mappingProperties = null;
        }
    });

    return new AttributesGridView();
});
