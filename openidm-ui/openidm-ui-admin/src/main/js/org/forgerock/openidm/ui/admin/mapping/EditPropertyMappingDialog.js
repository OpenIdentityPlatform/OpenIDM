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
            "onValidate": "onValidate"
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

        /*
         // reads the translation script and parses out the values used for presenting the translation gui
         loadTransformConfig: function () {
         var data = this.data;

         data.transformConfig = {
         "map": {},
         "precedence": []
         };

         if (typeof(data.property.transform) === "object" &&
         data.property.transform.type === "text/javascript" &&
         typeof (data.property.transform.source) === "string") {

         try {

         // the closure will attempt to contain variables defined within the transform script
         data.transformConfig = (function () {
         var source = []; // dummy used in the evaluation of the transform script
         eval(data.property.transform.source);
         // the transform source should define two variables: map and precedence.  If so, they will be exported, here:
         //return {"map": map, "precedence": precedence};
         return {"map": {}, "precedence": []};
         }());

         } catch (e) {
         // unable to eval javascript apparently....
         }

         }

         },

         // Used to fetch the script template and render it with the current values found from the data struct
         updateTransformConfig : function () {
         var data = this.data;

         return $.ajax({url:"templates/admin/mapping/ArrayTransformations.jstemplate", dataType:"text", cache:"true" }).then(_.bind(function (templateSrc) {
         templateSrc = templateSrc.replace("map = {}", "map = " + JSON.stringify(data.transformConfig.map, null, 4));
         templateSrc = templateSrc.replace(/precedence = \[\]/, "precedence = " + JSON.stringify(data.transformConfig.precedence, null, 4));
         data.property.transform = {
         "type": "text/javascript",
         "source": templateSrc
         };
         $(":input[name='transformation_script']",this.$el).val(templateSrc).trigger("change");
         this.loadTransformConfig();
         }, this));
         },*/

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
                propertyObj = _.chain(mappingProperties)
                    .flatten()
                    .find(function (p) { return p.target === target; })
                    .value();

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

            if(this.conditional_script_editor !== undefined) {
                propertyObj.condition = this.conditional_script_editor.generateScript();

                if(propertyObj.condition === null) {
                    delete propertyObj.condition;
                }
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

            mappingProperties = _.map(mappingProperties,function(p){
                if(p.target === propertyObj.target){
                    p = propertyObj;
                }
                return p;
            });

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
                dialogPromise = $.Deferred(),
                currentProperties,
                settings;

            this.data.mappingName = params[0];
            this.property = params[1];
            this.transform_script_editor = undefined;
            this.conditional_script_editor = undefined;

            currentProperties = browserStorageDelegate.get(this.data.mappingName + "_Properties") || browserStorageDelegate.get("currentMapping").properties;
            this.data.currentProperties = currentProperties;

            browserStorageDelegate.set(this.data.mappingName + "_Properties",currentProperties);

            this.data.property = _.find(currentProperties, _.bind(function (p) { return p.target === this.property; }, this));

            settings = {
                "title": $.t("templates.mapping.propertyEdit.title", {"property": this.property}),
                "template": this.template,
                "postRender": _.bind(this.loadData, this)
            };

            this.currentDialog = $('<div id="propertyDialog"></div>');
            this.setElement(this.currentDialog);

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
                            $(_this.$el).dialog( "option", "position", { my: "center center", at: "center center", of: $(window) } );

                            _this.$el.parents(".ui-dialog,#dialogs").show();

                            if(callback){
                                callback();
                            }
                        }, "append");
                }
            });
        },
        loadData: function() {
            var selectedTab = 0,
                _this = this,
                prop = this.data.property,
                data = this.data,
                availableObjects = [];

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

                    } else if(ui.newTab[0].textContent === "Conditional Update Script"){
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
                    $(_this.$el).dialog( "option", "position", { my: "center center", at: "center center", of: $(window) } );
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