/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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
    "org/forgerock/openidm/ui/admin/util/AutoCompletUtils",
    "libs/codemirror/lib/codemirror",
    "libs/codemirror/addon/display/placeholder"
], function(AbstractView, syncDelegate, validatorsManager, conf, uiUtils, eventManager, constants, spinner, browserStorageDelegate, autoCompleteUtils, codeMirror, placeHolder) {
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
            "change :input[name=transformation_script]": "showTransformSample",
            "keyup :input[name=transformation_script]":  function(e){
                this.validateMapping(e);
                this.showTransformSample(e);
            },
            "change :input[name=condition_script]": "showCondition",
            "keyup :input[name=condition_script]": function(e){
                this.validateMapping(e);
                this.showCondition(e);
            },
            "onValidate": "onValidate"
        },
        updateProperty: function (e) {
            if ($(e.target).val().length || _.has(this.data.property, "source")) {
                this.data.property.source = $(e.target).val();
            }
            this.showTransformSample();
            this.showCondition();
        },
        showTransformSample: function (e) {
            var translatedProperty,
                scriptEl = $(":input[name=transformation_script]", this.$el),
                sampleValue = "";
            
            if (conf.globalData.sampleSource && this.data.property && scriptEl.length) {

                // only passing in the script source to the translation function; default and simple association results purposely ignored.
                translatedProperty = syncDelegate.translatePropertyToTarget(conf.globalData.sampleSource, {
                    source: this.data.property.source,
                    target: this.data.property.target,
                    transform: {
                        type: "text/javascript",
                        source: scriptEl.val()
                    }
                });

                $("#exampleResult", this.$el).val(translatedProperty[1]);
                this.validateMapping();
            }
        },
        showCondition: function (e) {
            var conditionAction,
                scriptEl = $(":input[name=condition_script]", this.$el),
                sampleValue = "";

            if (conf.globalData.sampleSource && this.data.property && scriptEl.length) {

                conditionAction = syncDelegate.conditionAction(conf.globalData.sampleSource, {
                    source: this.data.property.source,
                    target: this.data.property.target,
                    condition: {
                        type: "text/javascript",
                        source: scriptEl.val()
                    }
                });

                $("#conditionResult", this.$el).text(conditionAction);
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
            
            if (_.has(formContent, "source")) {
                propertyObj.source = formContent.source;

            } else {
                propertyObj.source = "";
                if (_.has(formContent, "transformation_script")) {
                    propertyObj.source = "";
                } else {
                    delete propertyObj.source;
                }
            }

            if (_.has(formContent, "transformation_script")) {
                propertyObj.transform = {
                    "type": "text/javascript",
                    "source": formContent.transformation_script
                };
            } else {
                delete propertyObj.transform;
            }

            if (_.has(formContent, "condition_script")) {
                propertyObj.condition = {
                    "type": "text/javascript",
                    "source": formContent.condition_script
                };
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
            
            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "editMappingView", args: [this.data.mappingName]});
        },
        close: function () {
            if(this.currentDialog) {
                this.currentDialog.dialog('destroy').remove();
            }
        },
        render: function(params) {
            var _this = this,
                dialogPromise = $.Deferred(),
                currentProperties,
                settings;

            this.data.mappingName = params[0];
            this.property = params[1];
            
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
            $('#dialogs').append(this.currentDialog);
            
            this.currentDialog.dialog({
                title: settings.title,
                modal: true,
                resizable: true,
                bgiframe: true,
                width:'850px',
                dialogClass: "overflow-visible",
                close: _.bind(function(){
                    eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "editMappingView", args: [this.data.mappingName]});
                }, this),
                open: function(){

                    uiUtils.renderTemplate(settings.template, $(this), 
                                            _.extend(conf.globalData, _this.data), 
                                            function () {
                                                settings.postRender();
                                                $(_this.$el).dialog( "option", "position", { my: "center center", at: "center center", of: $(window) } );
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
                    selectedTab = 1;
                } else if (typeof(prop["default"]) !== "undefined" && prop["default"].length) {
                    selectedTab = 3;
                }
            }
            
            $('#mappingDialogTabs', this.$el).css('width','830px').tabs({ 
                active: selectedTab,
                activate: function (e, ui) {
                    $(':input:first', ui.newPanel).focus();
                }
            });
            $('#mappingDialogTabs [aria-expanded="true"] :input:first', this.$el).focus();
            

            this.transform_script_editor = codeMirror.fromTextArea(this.$el.find("[name=transformation_script]")[0], {
                lineNumbers: true,
                mode: "javascript"
            });
            
            this.transform_script_editor.on("changes", _.bind(function() {
                this.transform_script_editor.save();
                this.$el.find("[name=transformation_script]").trigger("blur");
            }, this));
            
            this.conditional_script_editor = codeMirror.fromTextArea(this.$el.find("[name=condition_script]")[0], {
                lineNumbers: true,
                mode: "javascript"
            });
            
            this.conditional_script_editor.on("changes", _.bind(function() {
                this.conditional_script_editor.save();
                this.$el.find("[name=condition_script]").trigger("blur");
            }, this));
            
            this.showTransformSample();

            this.data.availableSourceProps = browserStorageDelegate.get(this.data.mappingName + "_AvailableObjects").source.properties || [];
            
            if(this.data.availableSourceProps){
                autoCompleteUtils.selectionSetup($("input[name='source']:last", this.$el), _.sortBy(this.data.availableSourceProps,function(s){ return s; }));
            }
            
            $("input[name='source']", this.$el).on('change autocompleteclose', function (e, initialRender) {
                var val = $(this).val(),
                    sourceVal = (_this.data.availableSourceProps) ? _this.data.availableSourceProps[val] : "",
                    isValid;
                
                if (sourceVal) {

                    $("input[type=submit]", _this.$el).prop("disabled", false);
                    $("#Property_List .validation-message", _this.$el).text("");

                    data.sourceVal = sourceVal;
                    
                    if (val !== _this.data.property.source) {
                        _this.data.property.source = val;
                        if (sourceVal.type === "array"){
                            $('[name=transformation_script]', _this.$el).val('null');
                            if(!_this.data.property.transform){
                                _this.data.property.transform = {};
                            }
                            _this.data.property.transform.source = null;
                        }
                    }
                    $("#arrayAssociationTabs", _this.$el).hide();
                    if($('[name=transformation_script]', _this.$el).val() === "null"){
                        $('[name=transformation_script]', _this.$el).val('');
                    }
                } else {
                    val = "";
                    data.sourceVal = null;
                }

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