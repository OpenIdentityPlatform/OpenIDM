/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
 */

/*global define, $, _, Handlebars, form2js, window */
/*jslint evil: true */

define("org/forgerock/openidm/ui/admin/mapping/EditPropertyMappingDialog", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/admin/delegates/SyncDelegate",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/SpinnerManager",
    "org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate",
    "org/forgerock/openidm/ui/admin/util/AutoCompleteUtils",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor"
], function(AbstractView, syncDelegate, validatorsManager, conf, uiUtils, eventManager, constants, spinner, browserStorageDelegate, autoCompleteUtils, inlineScriptEditor) {
    var EditPropertyMappingDialog = AbstractView.extend({
        template: "templates/admin/mapping/PropertyMappingDialogEditTemplate.html",
        data: {
            width: 600,
            height: 400
        },
        el: "#dialogs",
        events: {
            "click input[type=submit]": "formSubmit",
            "change :input[name=source]": "updateProperty",
            "change :input": "validateMapping",
            "onValidate": "onValidate",
            "change .conditionalUpdateType": "conditionalUpdateType",
            "change .linkQualifier": "changeLinkQualifier"
        },
        updateProperty: function (e) {
            if ($(e.target).val().length || _.has(this.data.property, "source")) {
                this.data.property.source = $(e.target).val();
            }
            this.showTransformSample();
            this.showCondition();
        },
        showTransformSample: function () {

            var translatedProperty,
                scriptValue = null,
                generatedScript = null;

            if (conf.globalData.sampleSource && this.data.property) {
                if(this.transform_script_editor !== undefined) {
                    generatedScript = this.transform_script_editor.generateScript();

                    if(generatedScript && generatedScript.source && generatedScript.type === "text/javascript") {
                        scriptValue = generatedScript.source;
                    }
                }

                if(scriptValue) {
                    // only passing in the script source to the translation function; default and simple association results purposely ignored.
                    translatedProperty = syncDelegate.translatePropertyToTarget(conf.globalData.sampleSource, {
                        source: this.data.property.source,
                        target: this.data.property.target,
                        transform: {
                            type: "text/javascript",
                            source: scriptValue
                        }
                    });

                    $("#exampleResult", this.$el).val(translatedProperty[1]);
                } else {
                    $("#exampleResult", this.$el).val("");
                }
                this.validateMapping();
            }

        },

        showCondition: function () {

            var conditionAction,
                scriptValue = null,
                generatedScript = null;

            if (conf.globalData.sampleSource && this.data.property ) {

                if(this.conditional_script_editor !== undefined) {
                    generatedScript = this.conditional_script_editor.generateScript();

                    if(generatedScript && generatedScript.source && generatedScript.type === "text/javascript") {
                        scriptValue = generatedScript.source;
                    }
                }
                if(scriptValue) {
                    conditionAction = syncDelegate.conditionAction(conf.globalData.sampleSource, {
                        source: this.data.property.source,
                        target: this.data.property.target,
                        condition: {
                            type: "text/javascript",
                            source: scriptValue
                        }
                    });

                    $("#conditionResult", this.$el).text(conditionAction);
                } else {
                    $("#conditionResult", this.$el).text("");
                }
                this.validateMapping();
            }
        },

        conditionalUpdateType: function () {
            var type = this.$el.find(".conditionalUpdateType:checked").val();

            if (type === "linkQualifier") {
                this.$el.find(".conditionalLinkQualifiers").show();
                this.$el.find(".conditionalScript").hide();
            } else if (type === "script") {
                this.$el.find(".conditionalLinkQualifiers").hide();
                this.$el.find(".conditionalScript").show();
            } else if (type === "none") {
                this.$el.find(".conditionalLinkQualifiers").hide();
                this.$el.find(".conditionalScript").hide();
            }

            $("#propertyDialog").dialog("option","position","center");
            this.currentDialog.dialog("option","position","center");
            $( "#dialog" ).dialog( "option", "position", { my: "center", at: "center", of: window } );
        },

        changeLinkQualifier: function () {
            this.$el.find(".notAvailable").hide();
        },

        validateMapping: function () {
            var source = $("input[name='source']", this.$el).val(),
                hasAvailableSourceProps = this.data.availableSourceProps && this.data.availableSourceProps.length,
                hasSourceValue = source && source.length,
                invalidSourceProp = hasAvailableSourceProps && hasSourceValue && !_.contains(this.data.availableSourceProps,source),
                proplistValidationMessage = $("#Property_List .validation-message", this.$el),
                transformValidationMessage = $("#Transformation_Script .validation-message", this.$el),
                conditionValidationMessage = $("#Condition_Script .validation-message", this.$el),
                disableSave = function(el, message){
                    $("input[type=submit]", this.$el).prop("disabled", true);
                    el.text(message);
                };

            if (invalidSourceProp) {
                disableSave(proplistValidationMessage, $.t("templates.mapping.validPropertyRequired"));
                return false;
            } else if ($("#exampleResult", this.$el).val() === "ERROR WITH SCRIPT") {
                disableSave(transformValidationMessage, $.t("templates.mapping.invalidScript"));
                return false;
            } else if ($("#conditionResult", this.$el).text() === "ERROR WITH SCRIPT") {
                disableSave(conditionValidationMessage, $.t("templates.mapping.invalidConditionScript"));
                return false;
            } else {
                $("input[type=submit]", this.$el).prop("disabled", false);
                transformValidationMessage.text("");
                conditionValidationMessage.text("");
                proplistValidationMessage.text("");
                return true;
            }
        },

        formSubmit: function(event) {
            if(event){
                event.preventDefault();
            }

            var formContent = form2js(this.el),
                mappingProperties = browserStorageDelegate.get(this.data.mappingName + "_Properties"),
                target = this.property,
                propertyObj = mappingProperties[target - 1];

            // in the case when our property isn't currently found in the sync config...
            if (!propertyObj) {
                propertyObj = {"target": this.property};
                _.find(browserStorageDelegate.get("currentMapping"), function (o) { return o.name === this.data.mappingName; })
                    .properties
                    .push(propertyObj);
            }

            propertyObj.source = formContent.source || "";

            if(this.transform_script_editor !== undefined) {
                propertyObj.transform = this.transform_script_editor.generateScript();

                if(propertyObj.transform === null) {
                    delete propertyObj.transform;
                }
            } else {
                delete propertyObj.transform;
            }

            if (this.$el.find("input[name=conditionalUpdate]:checked").val() === "script" && this.conditional_script_editor !== undefined) {
                propertyObj.condition = this.conditional_script_editor.generateScript();

                if (propertyObj.condition === null) {
                    delete propertyObj.condition;
                }
            } else if (this.$el.find("#Condition_Script input[name=conditionalUpdate]:checked").val() === "linkQualifier" && this.$el.find(".linkQualifier").val().length > 0) {
                propertyObj.condition = {};
                propertyObj.condition.linkQualifier = this.$el.find(".linkQualifier").val();

            } else {
                delete propertyObj.condition;
            }

            if (_.has(formContent, "default")) {
                if($('#default_desc', this.$el).text().length > 0){
                    propertyObj["default"] = $('#default_desc', this.$el).text();
                }
                else{
                    propertyObj["default"] = formContent["default"];
                }

            } else {
                delete propertyObj["default"];
            }

            browserStorageDelegate.set(this.data.mappingName + "_Properties",mappingProperties);

            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "propertiesView", args: [this.data.mappingName]});
        },

        close: function () {
            if(this.currentDialog) {
                this.currentDialog.dialog('destroy').remove();
            }
            $("#dialogs").hide();
        },

        render: function(params, callback) {
            var _this = this,
                currentProperties,
                settings,
                syncConfig;

            this.data.mappingName = params[0];
            this.property = params[1];
            this.transform_script_editor = undefined;
            this.conditional_script_editor = undefined;

            currentProperties = browserStorageDelegate.get(this.data.mappingName + "_Properties") || browserStorageDelegate.get("currentMapping").properties;

            this.data.currentProperties = currentProperties;

            browserStorageDelegate.set(this.data.mappingName + "_Properties",currentProperties);

            this.data.property = currentProperties[this.property - 1];

            settings = {
                "title": $.t("templates.mapping.propertyEdit.title", {"property": this.data.property.target}),
                "template": this.template,
                "postRender": _.bind(this.loadData, this)
            };

            this.currentDialog = $('<div id="propertyDialog"></div>');
            this.setElement(this.currentDialog);

            syncConfig = syncDelegate.mappingDetails(this.data.mappingName);

            syncConfig.then(_.bind(function(details){

                //This will give us access to the linkQualifier list
                this.data.currentMappingDetails = _.find(details.mappings, function(map) {
                    return map.name === this.data.mappingName;
                }, this);

                if (_.has(this.data, "currentMappingDetails") && _.has(this.data.currentMappingDetails, "linkQualifiers") && this.data.currentMappingDetails.linkQualifiers.length > 0) {
                    this.data.availableLinkQualifiers = this.data.currentMappingDetails.linkQualifiers;
                    this.data.hasLinkQualifiers = true;
                } else {
                    this.data.availableLinkQualifiers = [];
                    this.data.hasLinkQualifiers = false;
                }

                this.currentDialog.dialog({
                    appendTo: $("#dialogs"),
                    title: settings.title,
                    position: ['center',25],
                    modal: true,
                    resizable: true,
                    bgiframe: true,
                    width:'850px',
                    dialogClass: "overflow-visible",
                    close: _.bind(function(){
                        $("#dialogs").hide();
                        eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "propertiesView", args: [this.data.mappingName]});
                    }, this),
                    open: function(){

                        uiUtils.renderTemplate(settings.template, $(this),
                            _.extend(conf.globalData, _this.data),
                            function () {
                                settings.postRender();

                                _this.$el.parents(".ui-dialog,#dialogs").show();
                                $(_this.$el).dialog( "option", "position", { my: "top center-110", at: "top center-110", of: $(window) } );

                                if(callback){
                                    callback();
                                }
                            }, "append");
                    }
                });
            }, this));
        },

        loadData: function() {
            var selectedTab = 0,
                _this = this,
                prop = this.data.property;

            if (prop) {
                if (typeof(prop.source) !== "undefined" && prop.source.length) {
                    selectedTab = 0;
                } else if (typeof(prop.transform) === "object" && prop.transform.type === "text/javascript" &&
                    typeof (prop.transform.source) === "string") {
                    this.transform_script_editor = inlineScriptEditor.generateScriptEditor({
                            "element": this.$el.find("#transformationScriptHolder"),
                            "eventName": "",
                            "noValidation": true,
                            "scriptData": prop.transform,
                            "disablePassedVariable": true,
                            "onBlur" : _.bind(this.showTransformSample, this),
                            "onChange" :  _.bind(this.showTransformSample, this),
                            "placeHolder" : "source.givenName.toLowerCase() + \" .\" + source.sn.toLowerCase()"
                        },
                        _.bind(this.showTransformSample, this));

                    selectedTab = 1;
                } else if (typeof(prop["default"]) !== "undefined" && prop["default"].length) {
                    selectedTab = 3;
                }

                if (_.has(prop, "condition")) {
                    if (_.has(prop.condition, "type")) {
                        this.$el.find("#conditionalScript").prop("checked", true).change();
                    } else if (_.has(prop.condition, "linkQualifier")) {
                        if (this.data.availableLinkQualifiers.indexOf(prop.condition.linkQualifier) >= 0) {
                            this.$el.find(".linkQualifier").val(prop.condition.linkQualifier);
                        } else {
                            this.$el.find(".notAvailable").show();
                            this.$el.find(".notAvailable span").text(prop.condition.linkQualifier);
                        }

                        this.$el.find("#conditionalLinkQualifier").prop("checked", true).change();
                    }
                }
            }

            $('#mappingDialogTabs', this.$el).css('width','830px').tabs({
                active: selectedTab,
                load: _.bind(function(e, ui){
                    if(ui.tab[0].textContent === "Transformation Script") {
                        this.transform_script_editor = inlineScriptEditor.generateScriptEditor({
                                "element": this.$el.find("#transformationScriptHolder"),
                                "eventName": "",
                                "noValidation": true,
                                "scriptData": this.data.property.transform,
                                "disablePassedVariable": true,
                                "onBlur" : _.bind(this.showTransformSample, this),
                                "onChange" :  _.bind(this.showTransformSample, this),
                                "placeHolder" : "source.givenName.toLowerCase() + \" .\" + source.sn.toLowerCase()"
                            },
                            _.bind(this.showTransformSample, this));
                    }
                }, this),
                activate: _.bind(function (e, ui) {
                    if(ui.newTab[0].textContent === "Transformation Script") {
                        this.transform_script_editor = inlineScriptEditor.generateScriptEditor({
                                "element": this.$el.find("#transformationScriptHolder"),
                                "eventName": "",
                                "noValidation": true,
                                "scriptData": prop.transform,
                                "disablePassedVariable": true,
                                "onBlur" : _.bind(this.showTransformSample, this),
                                "onChange" :  _.bind(this.showTransformSample, this),
                                "placeHolder" : "source.givenName.toLowerCase() + \" .\" + source.sn.toLowerCase()"
                            },
                            _.bind(this.showTransformSample, this));

                    } else if(ui.newTab[0].textContent === $.t("templates.mapping.propertyEdit.conditionalUpdateHeader")) {
                        this.conditional_script_editor = inlineScriptEditor.generateScriptEditor({
                                "element": this.$el.find("#conditionScriptHolder"),
                                "eventName": "",
                                "noValidation": true,
                                "scriptData": prop.condition,
                                "disablePassedVariable": true,
                                "onBlur" : _.bind(this.showCondition, this),
                                "onChange" :  _.bind(this.showCondition, this)
                            },
                            _.bind(this.showCondition, this));
                    }

                    $(':input:first', ui.newPanel).focus();
                }, this)
            });

            $('#mappingDialogTabs [aria-expanded="true"] :input:first', this.$el).focus();

            this.showTransformSample();

            this.data.availableSourceProps = browserStorageDelegate.get(this.data.mappingName + "_AvailableObjects").source.properties || [];

            if(this.data.availableSourceProps){
                autoCompleteUtils.selectionSetup($("input[name='source']:last", this.$el), _.sortBy(this.data.availableSourceProps,function(s){ return s; }));
            }

            $("input[name='source']", this.$el).on('change autocompleteclose', function (e, initialRender) {
                var val = $(this).val(),
                    isValid;

                if (val) {
                    $("#currentSourceDisplay", _this.$el).val(val);
                } else {
                    $("#currentSourceDisplay", _this.$el).val($.t("templates.mapping.completeSourceObject"));
                }

                isValid = _this.validateMapping();

                if(isValid && initialRender !== "true" && $('#propertyDialog').size() === 0){
                    _this.formSubmit();
                }
            });

            $("input[name='source']", this.$el).trigger('change', 'true');

            spinner.hideSpinner();
        }
    });

    return new EditPropertyMappingDialog();
});