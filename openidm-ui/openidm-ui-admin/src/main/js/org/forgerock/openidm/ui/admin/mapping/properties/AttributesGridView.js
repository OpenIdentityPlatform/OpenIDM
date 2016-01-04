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

/*global define, $, _, Handlebars, form2js, window */

define("org/forgerock/openidm/ui/admin/mapping/properties/AttributesGridView", [
    "jquery",
    "underscore",
    "handlebars",
    "backbone",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/SearchDelegate",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingUtils",
    "org/forgerock/openidm/ui/admin/util/LinkQualifierUtils",
    "org/forgerock/openidm/ui/admin/delegates/ScriptDelegate",
    "org/forgerock/openidm/ui/admin/util/FilterEvaluator",
    "org/forgerock/openidm/ui/admin/mapping/util/QueryFilterEditor",
    "org/forgerock/openidm/ui/admin/mapping/properties/AddPropertyMappingDialog",
    "org/forgerock/openidm/ui/admin/mapping/properties/EditPropertyMappingDialog",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/AdminUtils",
    "jquerySortable"
], function($, _, Handlebars, Backbone,
            MappingAdminAbstractView,
            eventManager,
            conf,
            constants,
            searchDelegate,
            connectorDelegate,
            configDelegate,
            mappingUtils,
            LinkQualifierUtil,
            ScriptDelegate,
            FilterEvaluator,
            QueryFilterEditor,
            AddPropertyMappingDialog,
            EditPropertyMappingDialog,
            Backgrid,
            BackgridUtils,
            UIUtils,
            AdminUtils) {

    var AttributesGridView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/properties/AttributesGridTemplate.html",
        element: "#attributesGrid",
        noBaseTemplate: true,
        events: {
            "click .add-property": "addProperty",
            "click #updateMappingButton": "saveMapping",
            "click #clearChanges": "clearChanges",
            "click #missingRequiredPropertiesButton": "addRequiredProperties"
        },
        model: {
            availableObjects: {},
            mappingProperties: null
        },
        data: {},
        sampleDisplay: [],

        render: function (args, callback) {
            this.mapping = this.getCurrentMapping();

            this.data.requiredProperties = [];
            this.data.missingRequiredProperties = [];

            if(args && args.mappingProperties) {
                this.model.mappingProperties = args.mappingProperties;
            } else {
                this.model.mappingProperties = null;
            }

            this.data.linkQualifiers = LinkQualifierUtil.getLinkQualifier(this.mapping.name);

            this.currentLinkQualifier = this.data.linkQualifiers[0];
            this.data.hasLinkQualifiers = this.mapping.linkQualifiers;

            if (conf.globalData.sampleSource && this.mapping.properties.length) {
                this.data.sampleSource_txt = conf.globalData.sampleSource[this.mapping.properties[0].source];
            }

            this.buildAvailableObjectsMap().then(_.bind(function(availableObjects) {
                this.model.availableObjects = availableObjects;
                this.checkMissingRequiredProperties();

                this.parentRender(_.bind(function () {
                    var mapProps = this.model.mappingProperties || this.getCurrentMapping().properties,
                        sampleSource = {},
                        autocompleteProps = _.pluck(this.mapping.properties,"source").slice(0,this.getNumRepresentativeProps());


                    if(conf.globalData.sampleSource && conf.globalData.sampleSource.IDMSampleMappingName === this.mapping.name) {
                        sampleSource = conf.globalData.sampleSource;
                    }

                    this.data.mapProps = mapProps;
                    this.gridFromMapProps(mapProps);

                    mappingUtils.setupSampleSearch($("#findSampleSource",this.$el), this.mapping, autocompleteProps, _.bind(function(item) {
                        item.IDMSampleMappingName = this.mapping.name;
                        conf.globalData.sampleSource = item;
                        sampleSource = item;

                        this.gridFromMapProps(mapProps);
                    }, this));

                    this.checkAvailableProperties();
                    this.checkChanges();

                    if (callback){
                        callback();
                    }
                }, this));
            }, this));
        },

        initSort: function() {
            BackgridUtils.sortable({
                "grid": this.$el.find("#attributesGridHolder table"),
                "rows": _.clone(this.model.mappingProperties, true)
            }, _.bind(this.setMappingProperties, this));
        },

        checkChanges: function () {
            var currentProperties = this.getCurrentMapping().properties,
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
                mappingProperties: this.model.mappingProperties,
                availProperties: this.model.availableObjects.target.properties,
                saveCallback: _.bind(function(props) {
                    this.setMappingProperties(props);
                }, this)
            });
        },

        clearChanges: function(e) {
            e.preventDefault();

            this.render();
        },

        checkMissingRequiredProperties: function() {
            var props = this.model.mappingProperties || this.getCurrentMapping().properties;

            _.each(this.data.requiredProperties, function(reqProp, key) {
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
            _.each(this.data.requiredProperties, function(reqProp, key) {
                if (!_.filter(props, function(p) {return p.target === key;}).length){
                    props.push({target: key});
                }
            });

            this.setMappingProperties(props);
        },

        setMappingProperties: function(mappingProperties) {
            this.render({
                "mappingProperties" : mappingProperties
            });
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
                                id: this.model.attributes.id,
                                mappingProperties: _this.model.mappingProperties,
                                availProperties: _this.model.availableObjects.source.properties,
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

                if(conf.globalData.sampleSource !== undefined && conf.globalData.sampleSource.IDMSampleMappingName === this.mapping.name && conf.globalData.sampleSource[attribute.source]) {
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
                                var previewElement = $('<i class="dragToSort fa fa-arrows pull-left"></i> <div class="property-container-parent"><div class="property-container"></div></div>');

                                if(this.model.attributes.attribute.source) {
                                    previewElement.find(".property-container").append('<div class="title">' + this.model.attributes.attribute.source + '</div>');
                                } else {
                                    previewElement.find(".property-container").append('<div class="title"></div>');
                                }

                                if (this.model.attributes.sample !== null) {
                                    previewElement.find(".property-container").append('<div class="text-muted">(' + this.model.attributes.sample + ')</div>');
                                }

                                this.$el.html(previewElement.text());
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
                                var iconElement = $('<div class="properties-icon-container"></div>'),
                                    conditionIcon = "",
                                    transformIcon = "";

                                if(this.model.attributes.attribute.condition) {
                                    if(_.isObject(this.model.attributes.attribute.condition)) {
                                        if(this.model.attributes.attribute.condition.source) {
                                            conditionIcon = this.model.attributes.attribute.condition.source;
                                        } else {
                                            conditionIcon = "File: " + this.model.attributes.attribute.condition.file;
                                        }
                                    } else {
                                        conditionIcon = this.model.attributes.attribute.condition;
                                    }

                                    iconElement.append('<span class="badge properties-badge" rel="tooltip" data-toggle="popover" data-placement="top" title=""><i class="fa fa-filter"></i>'
                                        +'<div style="display:none;" class="tooltip-details">' + $.t("templates.mapping.conditionalUpon") +'<pre class="text-muted code-tooltip">' +conditionIcon +'</pre></div></span>');
                                }

                                if(this.model.attributes.attribute.transform) {
                                    if(_.isObject(this.model.attributes.attribute.transform)) {
                                        if(this.model.attributes.attribute.transform.source) {
                                            transformIcon = this.model.attributes.attribute.transform.source;
                                        } else {
                                            transformIcon = "File: " + this.model.attributes.attribute.transform.file;
                                        }
                                    } else {
                                        transformIcon = this.model.attributes.attribute.transform;
                                    }

                                    iconElement.append('<span class="badge properties-badge" rel="tooltip" data-toggle="popover" data-placement="top" title=""><i class="fa fa-wrench"></i>'
                                        +'<div style="display:none;" class="tooltip-details">' +$.t("templates.mapping.transformationScriptApplied") +'<pre class="text-muted code-tooltip">' +transformIcon +'</pre></div></span>');
                                }

                                this.$el.html(iconElement);
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
                                var previewElement = $('<div class="property-container-parent"><div class="property-container"></div></div>');

                                if(this.model.attributes.attribute.target) {
                                    previewElement.find(".property-container").append('<div class="title">' + this.model.attributes.attribute.target + '</div>');
                                } else {
                                    previewElement.find(".property-container").append('<div class="title"></div>');
                                }


                                if(this.model.attributes.evalResult && this.model.attributes.evalResult.conditionResults && !this.model.attributes.evalResult.conditionResults.result) {
                                    previewElement.find(".property-container").append('<div class="text-muted"></div>');
                                } else {
                                    if (this.model.attributes.sample !== null) {
                                        if(this.model.attributes.evalResult && this.model.attributes.evalResult.transformResults) {
                                            previewElement.find(".property-container").append('<div class="text-muted">(' + this.model.attributes.evalResult.transformResults + ')</div>');
                                        } else {
                                            previewElement.find(".property-container").append('<div class="text-muted">(' + this.model.attributes.sample + ')</div>');
                                        }
                                    } else if (this.model.attributes.attribute["default"]) {
                                        previewElement.find(".property-container").append('<div class="text-muted">(' + this.model.attributes.attribute["default"] + ')</div>');
                                    }
                                }

                                this.$el.html(previewElement.text());
                                this.delegateEvents();

                                return this;
                            }
                        })
                    },
                    {
                        name: "",
                        cell: BackgridUtils.ButtonCell([
                            {
                                className: "fa fa-times grid-icon",
                                callback: function(event){
                                    event.preventDefault();

                                    UIUtils.confirmDialog($.t("templates.mapping.confirmRemoveProperty",{property: this.model.attributes.attribute.target}), "danger", _.bind(function(){
                                        _this.model.mappingProperties.splice(($(event.target).parents("tr")[0].rowIndex - 1), 1);
                                        _this.checkChanges();
                                        _this.render({
                                            "mappingProperties" : _this.model.mappingProperties
                                        });
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
                trigger:'hover',
                placement:'top',
                container: 'body',
                html: 'true',
                title: ''
            });

            this.$el.find("#linkQualifierSelect").change(_.bind(function(event) {
                var element = event.target;

                event.preventDefault();

                if ($(element).val().length > 0) {
                    this.currentLinkQualifier = $(element).val();
                }

                this.gridFromMapProps(this.model.mappingProperties);
                this.initSort();
            }, this));

            this.$el.find("#linkQualifierSelect").selectize({
                placeholder: $.t("templates.mapping.linkQualifier"),
                create: false,
                sortField: 'text'
            });

            this.initSort();
        },

        gridFromMapProps : function (props) {
            var propertyDetails = _.clone(props),
                evalPromises = [],
                globals = {
                    source : {}
                },
                evalCheck,
                tempDetails = {},
                sampleSource = conf.globalData.sampleSource || {};

            this.sampleDisplay = [];

            if (!_.isEmpty(sampleSource)) {
                _.each(propertyDetails, function (item) {

                    globals = {
                        source: {}
                    };

                    tempDetails = {};

                    if (item.condition || item.transform) {
                        globals.linkQualifier = this.currentLinkQualifier;

                        if (sampleSource[item.source]) {
                            globals.source = sampleSource[item.source];
                        } else {
                            globals.source = sampleSource;
                        }
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
                filterCheck,
                sampleSource = conf.globalData.sampleSource || {};

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
                                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingEvalError");
                                });
                            } else {
                                samplePromise.resolve({
                                    conditionResults: {
                                        result: conditionResults
                                    }
                                });
                            }
                        },
                        function(e) {
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingEvalError");
                        });
                }
            } else if (sampleDetails.hasTransform) {
                ScriptDelegate.evalScript(sampleDetails.transform, globals).then(function(transformResults) {
                    samplePromise.resolve({
                        transformResults: transformResults
                    });
                }, function(e) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingEvalError");
                });
            } else {
                samplePromise.resolve(null);
            }

            return samplePromise;
        },

        checkAvailableProperties: function(){
            var availableProps;

            availableProps = this.model.availableObjects.target.properties || [];

            if (!availableProps.length || _.difference(availableProps, _.pluck(this.data.mapProps,"target")).length) {
                this.$el.find('.addProperty').removeProp('disabled');
                this.$el.find('#allPropertiesMapped').hide();
            } else {
                this.$el.find('.addProperty').prop('disabled',true);
                this.$el.find('#allPropertiesMapped').show();
            }
        },

        buildAvailableObjectsMap: function() {
            var sourceProm = $.Deferred(),
                targetProm = $.Deferred(),
                currentConnectors = connectorDelegate.currentConnectors(),
                managedConfig = configDelegate.readEntity("managed");

            return $.when(currentConnectors, managedConfig).then(_.bind(function(currConnectors, managed) {

                _.map(managed.objects,_.bind(function(o){
                    if(this.getCurrentMapping().source === "managed/" + o.name){
                        sourceProm.resolve({ name: o.name, fullName: "managed/" + o.name });
                    }
                    if(this.getCurrentMapping().target === "managed/" + o.name){
                        targetProm.resolve({ name: o.name, fullName: "managed/" + o.name });
                    }
                }, this));

                if (!(sourceProm.state() === "resolved" && targetProm.state() === "resolved")) {
                    _.each(currConnectors, function(connector) {
                        _.each(connector.objectTypes, function(objType) {
                            var objTypeMap = {
                                    name: connector.name,
                                    fullName: "system/" + connector.name + "/" + objType
                                },
                                getProps = function(){
                                    return configDelegate.readEntity(connector.config.replace("config/", "")).then(function(connector) {
                                        return connector.objectTypes[objType].properties;
                                    });
                                };

                            if (this.getCurrentMapping().source === objTypeMap.fullName) {
                                getProps().then(function(props){
                                    objTypeMap.properties = _.keys(props).sort();
                                    sourceProm.resolve(objTypeMap);
                                });
                            }

                            AdminUtils.findPropertiesList(this.getCurrentMapping().target.split("/"), true).then(_.bind(function(properties){
                                this.data.requiredProperties = properties;
                                targetProm.resolve(objTypeMap);
                            }, this));
                        }, this);
                    }, this);
                }

                return $.when(sourceProm,targetProm).then(function(source,target) {
                    return { source: source, target: target};
                });

            }, this));
        },

        saveMapping: function(e) {
            e.preventDefault();

            var mapping = this.getCurrentMapping();

            if (this.model.mappingProperties) {
                mapping.properties = this.model.mappingProperties;
                this.model.mappingProperties = null;

                this.AbstractMappingSave(mapping, _.bind(function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingSaveSuccess");
                    this.render();
                }, this));
            }
        }
    });

    return new AttributesGridView();
});
