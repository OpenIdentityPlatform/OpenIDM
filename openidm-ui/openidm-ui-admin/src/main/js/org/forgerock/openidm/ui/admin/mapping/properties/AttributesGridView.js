/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

/*global define, $, _, Handlebars, form2js, window */

define("org/forgerock/openidm/ui/admin/mapping/properties/AttributesGridView", [
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/SearchDelegate",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingUtils",
    "org/forgerock/openidm/ui/admin/util/LinkQualifierUtils",
    "org/forgerock/openidm/ui/admin/delegates/ScriptDelegate",
    "org/forgerock/openidm/ui/admin/util/FilterEvaluator",
    "org/forgerock/openidm/ui/admin/util/QueryFilterUtils",
    "org/forgerock/openidm/ui/admin/mapping/util/QueryFilterEditor"
], function(MappingAdminAbstractView,
            eventManager,
            conf,
            UIUtils,
            constants,
            searchDelegate,
            connectorDelegate,
            configDelegate,
            mappingUtils,
            LinkQualifierUtil,
            ScriptDelegate,
            FilterEvaluator,
            QueryFilterUtils,
            QueryFilterEditor) {

    var AttributesGridView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/properties/AttributesGridTemplate.html",
        element: "#attributesGrid",
        noBaseTemplate: true,
        events: {
            "click .addProperty": "addProperty",
            "click .removePropertyBtn": "removeProperty",
            "click .attrTabBtn": "openPropertyEditTab",
            "keyup #numRepresentativeProps": "resetNumRepresentativeProps",
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

            $(window).resize(function () {
                $("#mappingTable").setGridWidth( $('.jqgrid-container').width());
            });

            this.mapping = this.getCurrentMapping();
            this.data.numRepresentativeProps = this.getNumRepresentativeProps;
            this.data.requiredProperties = [];
            this.data.missingRequiredProperties = [];

            this.data.linkQualifiers = LinkQualifierUtil.getLinkQualifier(this.mapping.name);
            this.currentLinkQualifier = this.data.linkQualifiers[0];

            if (this.mapping.linkQualifiers) {
                this.data.hasLinkQualifiers = true;
            } else {
                this.data.hasLinkQualifiers = false;
            }

            if (conf.globalData.sampleSource && this.mapping.properties.length) {
                this.data.sampleSource_txt = conf.globalData.sampleSource[this.mapping.properties[0].source];
            }

            this.buildAvailableObjectsMap().then(_.bind(function(availableObjects) {
                this.model.availableObjects = availableObjects;
                this.checkMissingRequiredProperties();
                this.parentRender(_.bind(function () {
                    this.loadMappingPropertiesGrid();
                    this.checkAvailableProperties();
                    this.checkChanges();

                    if (callback){
                        callback();
                    }
                }, this));
            }, this));
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
            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "addMappingProperty", args: [this.mapping.name]});
        },

        clearChanges: function(e) {
            e.preventDefault();
            this.model.mappingProperties = null;
            this.render();
        },

        removeProperty: function (e) {
            e.preventDefault();

            UIUtils.jqConfirm($.t("templates.mapping.confirmRemoveProperty",{property: $(e.target).attr('target')}),_.bind(function(){
                var mapProps = this.model.mappingProperties || this.data.mapProps;

                //This is removing the correct row from the stored array
                mapProps.splice(($(e.target).parents("tr")[0].rowIndex - 1), 1);

                this.model.mappingProperties = mapProps;
                this.checkChanges();

                this.render();
            }, this));
        },

        openPropertyEditTab: function(e) {
            var id = $(e.target).attr('rowId'),
                tab = $(e.target).attr('tab');

            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "editMappingProperty", args: [this.mapping.name, id]});

            if (tab === 'condition'){
                $("[href=#Condition_Script]").click();
            } else if (tab === 'transform'){
                $("[href=#Transformation_Script]").click();
            } else if (tab === 'default'){
                $("[href=#Default_Values]").click();
            }
        },

        resetNumRepresentativeProps: function(e) {
            var num = parseInt($(e.target).val(), 10);

            e.preventDefault();

            if (num && num <= this.mapping.properties.length) {
                this.setNumRepresentativeProps(num);
                this.render();
            }
        },

        checkMissingRequiredProperties: function() {
            var props = this.model.mappingProperties || this.getCurrentMapping().properties;
            _.each(this.data.requiredProperties, function(reqProp) {
                if (!_.filter(props, function(p) {return p.target === reqProp;}).length) {
                    this.data.missingRequiredProperties.push(reqProp);
                }
            }, this);
        },

        addRequiredProperties: function(e) {
            var props = this.model.mappingProperties || this.data.mapProps;

            if (e) {
                e.preventDefault();
            }
            _.each(this.data.requiredProperties, function(reqProp) {
                if (!_.filter(props, function(p) {return p.target === reqProp;}).length){
                    props.push({target: reqProp});
                }
            });

            this.model.mappingProperties = props;
            this.render();
        },

        //Returns a promise and determines if a transform and/or conditional needs to be eval
        sampleEvalCheck: function(sampleDetails, globals) {
            var samplePromise = $.Deferred(),
                filterCheck,
                sampleSource = conf.globalData.sampleSource || {},
                qfe = new QueryFilterEditor();

            if (sampleDetails.hasCondition) {
                if (_.isString(sampleDetails.condition)) {
                    filterCheck = FilterEvaluator.evaluate(qfe.transform(QueryFilterUtils.convertFrom(sampleDetails.condition)), { "linkQualifier": globals.linkQualifier, "object": sampleSource});

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

        loadMappingPropertiesGrid: function() {
            var _this = this,
                mapProps = this.model.mappingProperties || this.getCurrentMapping().properties,
                sampleSource = conf.globalData.sampleSource || {},
                autocompleteProps = _.pluck(this.mapping.properties,"source").slice(0,this.getNumRepresentativeProps()),
                processDisplayDetails = function(evalResults, gridDetailsPromise, props) {
                    var propCounter = 0;

                    gridDetailsPromise.resolve(
                        _.chain(props)
                            .map(function (prop) {
                                var sampleData = null,
                                    sourceProp = " ",
                                    hasConditionScript = false,
                                    conditionScript = null,
                                    hasTransformScript = false,
                                    transformScript = null,
                                    cleanData = "";

                                //Sets has condition icon and tooltip
                                if (_.has(prop, "condition")) {
                                    hasConditionScript = true;
                                    conditionScript = prop.condition;
                                }

                                //Logic to find and display source sample
                                if (!_.isEmpty(sampleSource)) {
                                    if(sampleSource[prop.source]){
                                        cleanData = sampleSource[prop.source];
                                    } else if (_.isUndefined(prop.source)){
                                        cleanData = "Object";
                                    } else {
                                        if(prop.source.length === 0) {
                                            cleanData = "Object";
                                        } else {
                                            cleanData = "Null";
                                        }
                                    }
                                } else {
                                    cleanData = "";
                                }

                                //Sets transform icon and tooltip
                                if (_.has(prop, "transform")) {
                                    hasTransformScript = true;

                                    if (prop.transform.source) {
                                        transformScript = prop.transform.source;
                                    } else {
                                        transformScript = "File: " + prop.transform.file;
                                    }
                                }

                                if (typeof(prop.source) !== "undefined" && prop.source.length) {
                                    sourceProp = prop.source;
                                }

                                //Display eval results for transform and conditional
                                if (evalResults !== null && evalResults[propCounter] !== null) {
                                    if (_.isObject(evalResults[propCounter].conditionResults)) {
                                        if (evalResults[propCounter].conditionResults.result === true) {
                                            if (evalResults[propCounter].transformResults) {
                                                sampleData = evalResults[propCounter].transformResults;
                                            } else {
                                                sampleData = sampleSource[prop.source];
                                            }
                                        } else {
                                            sampleData = "";
                                        }
                                    } else {
                                        if (evalResults[propCounter].transformResults) {
                                            sampleData = evalResults[propCounter].transformResults;
                                        } else {
                                            if(prop["default"]) {
                                                sampleData = prop["default"];
                                            } else {
                                                sampleData = sampleSource[prop.source];
                                            }
                                        }
                                    }
                                } else if (typeof(prop.source) !== "undefined" && prop.source.length) {
                                    if (_.isEmpty(sampleSource)) {
                                        sampleData = "";
                                    } else {
                                        if (sampleSource[prop.source] === null || sampleSource[prop.source] === undefined) {
                                            if (!_.isUndefined(prop["default"])) {
                                                sampleData = prop["default"];
                                            } else {
                                                sampleData = "Null";
                                            }
                                        } else {
                                            sampleData = sampleSource[prop.source];
                                        }
                                    }
                                } else if (!_.isEmpty(sampleSource)) {
                                    if (!_.isUndefined(prop["default"])) {
                                        sampleData = prop["default"];
                                    } else {
                                        sampleData = "Null";
                                    }
                                } else if (evalResults === null || evalResults === undefined) {
                                    if (!_.isUndefined(prop["default"])) {
                                        sampleData = prop["default"];
                                    } else {
                                        sampleData = "Null";
                                    }
                                }

                                propCounter++;

                                return {
                                    "target": {
                                        "property" : Handlebars.Utils.escapeExpression(prop.target),
                                        "sample" : Handlebars.Utils.escapeExpression(decodeURIComponent((sampleData || "")))
                                    },
                                    "source": {
                                        "property" : Handlebars.Utils.escapeExpression(sourceProp),
                                        "sample" : Handlebars.Utils.escapeExpression(decodeURIComponent((cleanData)))
                                    },
                                    "iconDisplay": {
                                        "hasCondition": hasConditionScript,
                                        "conditionScript" : conditionScript,
                                        "hasTransform" : hasTransformScript,
                                        "transformScript" : transformScript
                                    }
                                };

                            }).value()
                    );
                },

                gridFromMapProps = function (props) {
                    var propertyDetails = _.clone(props),
                        gridDetailsPromise = $.Deferred(),
                        evalPromises = [],
                        globals = {
                            source : {}
                        },
                        evalCheck,
                        tempDetails = {};

                    _this.sampleDisplay = [];

                    if (!_.isEmpty(sampleSource)) {
                        _.each(propertyDetails, function (item) {

                            globals = {
                                source: {}
                            };

                            tempDetails = {};

                            if (item.condition || item.transform) {
                                globals.linkQualifier = _this.currentLinkQualifier;

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

                            evalCheck = _this.sampleEvalCheck(tempDetails, globals);

                            tempDetails.results = evalCheck;

                            _this.sampleDisplay.push(tempDetails);

                            evalPromises.push(evalCheck);
                        });
                    }

                    if (evalPromises.length > 0) {
                        $.when.apply($, evalPromises).then(function() {
                            processDisplayDetails(arguments, gridDetailsPromise, props);
                        }, function(e) {
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingEvalError");
                        });
                    } else {
                        processDisplayDetails(null, gridDetailsPromise, props);
                    }

                    return gridDetailsPromise;
                },
                cols = [
                    {
                        "name": "source",
                        "label": $.t("templates.mapping.source"),
                        "width": "125px",
                        "formatter": function(data, opt, row) {
                            var previewElement = $('<div class="property-container-parent"><div class="property-container"></div></div>');

                            if(data !== undefined) {
                                previewElement.find(".property-container").append('<div class="title">' + data.property + '</div>');

                                if (data.sample.length > 0) {
                                    previewElement.find(".property-container").append('<div class="text-muted">(' + data.sample + ')</div>');
                                }
                            }

                            return previewElement.html();
                        }
                    },
                    {
                        "name": "iconDisplay",
                        "label": "&nbsp;",
                        "width": "30px",
                        "align": "center",
                        "title": false,
                        "formatter": function(iconDisplay,opt,row) {
                            var iconElement = $('<div class="properties-icon-container-parent"><div class="properties-icon-container"></div></div>');

                            if (iconDisplay !== undefined && iconDisplay.hasCondition) {

                                if(_.isObject(iconDisplay.conditionScript)) {
                                    if(iconDisplay.conditionScript.source) {
                                        iconDisplay.conditionScript = iconDisplay.conditionScript.source;
                                    } else {
                                        iconDisplay.conditionScript = "File: " + iconDisplay.conditionScript.file;
                                    }
                                }

                                iconElement.find(".properties-icon-container").append('<span class="badge properties-badge" rel="tooltip" data-toggle="popover" data-placement="top" title=""><i class="fa fa-filter"></i>'
                                +'<div style="display:none;" class="tooltip-details">' + $.t("templates.mapping.conditionalUpon") +'<pre class="text-muted code-tooltip">' +iconDisplay.conditionScript +'</pre></div></span>');
                            }

                            if (iconDisplay !== undefined && iconDisplay.hasTransform) {
                                iconElement.find(".properties-icon-container").append('<span class="badge properties-badge" rel="tooltip" data-toggle="popover" data-placement="top" title=""><i class="fa fa-wrench"></i>'
                                +'<div style="display:none;" class="tooltip-details">' +$.t("templates.mapping.transformationScriptApplied") +'<pre class="text-muted code-tooltip">' +iconDisplay.transformScript +'</pre></div></span>');
                            }

                            return iconElement.html();
                        }
                    },
                    {
                        "name": "target",
                        "label": $.t("templates.mapping.target"),
                        "width": "125px",
                        "formatter" : function(data, opt, row) {
                            var previewElement = $('<div class="property-container-parent"><div class="property-container"></div></div>');

                            previewElement.find(".property-container").append('<div class="title">' +data.property +'</div>');

                            if (_.isString(data) !== true && data.sample.length > 0) {
                                previewElement.find(".property-container").append('<div class="text-muted">(' +data.sample +')</div>');
                            } else if (_.isString(data) === true ) {
                                previewElement.find(".property-container").html(data);
                            }

                            return previewElement.html();
                        }
                    },
                    {
                        "name": "required",
                        "label": "&nbsp;",
                        "width": "25px",
                        "align": "center",
                        "title": false,
                        "formatter": function(required,opt,row) {
                            return (!required) ? '<i target="' + row.target.property + '" title="' + $.t("common.form.removeAttribute") + ': ' + row.target.property + '" class="fa fa-times removePropertyBtn" style="margin-top:4px;"></i>' : '';
                        }
                    }
                ];

            this.data.mapProps = mapProps;

            mappingUtils.setupSampleSearch($("#findSampleSource",this.$el), this.mapping, autocompleteProps, _.bind(function(item) {
                conf.globalData.sampleSource = item;
                sampleSource = item;

                gridFromMapProps(mapProps).then(function(gridResults) {
                    $('#mappingTable',this.$el).jqGrid('setGridParam', {
                        datatype: 'local',
                        data: gridResults
                    }).trigger('reloadGrid');
                });
            }, this));

            gridFromMapProps(mapProps).then(function(gridResults) {
                $('#mappingTable').jqGrid({
                    datatype: "local",
                    data: gridResults,
                    height: 'auto',
                    autowidth: true,
                    shrinkToFit: true,
                    rowNum: mapProps.length,
                    pager: 'mappingTable_pager',
                    hidegrid: false,
                    colModel: cols,
                    cmTemplate: {sortable: false},
                    onSelectRow: function (id) {
                        if (id !== "blankRow") {
                            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "editMappingProperty", args: [_this.mapping.name, id]});
                        }
                    },

                    beforeSelectRow: function (rowid, e) {
                        if ($(e.target).hasClass("removePropertyBtn")) {
                            return false;
                        }
                        return true;
                    },

                    loadComplete: function (data) {
                        if (!data.rows.length) {
                            $('#mappingTable').addRowData("blankRow", {"required": true, "target": $.t("templates.mapping.noPropertiesMapped"), "default": "", "script": "", "hasConditionScript": false});
                            _this.$el.find("#findSampleSource").parent().hide();
                        }

                        _this.setNumRepresentativePropsLine();

                        _this.$el.find(".properties-badge").popover({
                            content: function () { return $(this).find(".tooltip-details").clone().show();},
                            trigger:'hover',
                            placement:'top',
                            container: 'body',
                            html: 'true',
                            template: '<div class="popover popover-info" role="tooltip"><div class="popover-content"></div></div>'
                        });

                        _this.$el.find("#linkQualifierSelect").change(function(event) {
                            var element = event.target;
                            event.preventDefault();

                            if ($(element).val().length > 0) {
                                _this.currentLinkQualifier = $(element).val();
                            }

                            gridFromMapProps(mapProps).then(function(gridResults) {
                                _this.$el.find('#mappingTable').jqGrid('setGridParam', {
                                    datatype: 'local',
                                    data: gridResults
                                }).trigger('reloadGrid');
                            });
                        });

                        _this.$el.find("#linkQualifierSelect").selectize({
                            placeholder: $.t("templates.mapping.linkQualifier"),
                            create: false,
                            sortField: 'text'
                        });
                    }
                }).jqGrid('sortableRows', {
                    update: function (ev, ui) {
                        var item = ui.item[0];

                        mapProps.splice((item.rowIndex - 1), 0, _this.draggedRow);

                        _this.model.mappingProperties = mapProps;

                        _this.setNumRepresentativePropsLine();
                        _this.checkChanges();
                    },

                    start: function (ev, ui) {
                        _this.draggedRow = mapProps[(ui.item[0].rowIndex - 1)];

                        mapProps.splice((ui.item[0].rowIndex - 1), 1);

                        $("#mappingTable", _this.$el).find("tr td").css("border-bottom-width", "");
                    }
                });
            });
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

        setNumRepresentativePropsLine: function() {
            $("#mappingTable", this.$el).find("tr:eq(" + this.getNumRepresentativeProps() + ") td").css("border-bottom-width","5px");
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
                    _.chain(currConnectors)
                        .each(function(connector) {
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
                                if (this.getCurrentMapping().target === objTypeMap.fullName) {
                                    getProps().then(_.bind(function(props) {
                                        this.data.requiredProperties = _.keys(_.omit(props, function(val) { return !val.required; }));
                                        objTypeMap.properties = _.keys(props).sort();
                                        targetProm.resolve(objTypeMap);
                                    }, this));
                                }
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