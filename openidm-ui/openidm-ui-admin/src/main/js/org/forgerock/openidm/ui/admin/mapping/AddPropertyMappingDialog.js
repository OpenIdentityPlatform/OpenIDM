/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
 */

/*global define, $, _, Handlebars, form2js, window */
/*jslint evil: true */

define("org/forgerock/openidm/ui/admin/mapping/AddPropertyMappingDialog", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate",
    "org/forgerock/openidm/ui/admin/util/AutoCompletUtils"
], function(AbstractView, conf, uiUtils, eventManager, constants, browserStorageDelegate, autoCompleteUtils) {
    var AddPropertyMappingDialog = AbstractView.extend({
        template: "templates/admin/mapping/PropertyMappingDialogAddTemplate.html",
        data: {
            width: 600,
            height: 400
        },
        el: "#dialogs",
        events: {
            "click input[type=submit]": "formSubmit",
            "change :input": "validateMapping"
        },
        
        formSubmit: function (event) {
            var property = $(":input[name=propertyList]",this.$el).val(),
                mappingProperties = browserStorageDelegate.get(this.data.mappingName + "_Properties");
            
            event.preventDefault();
            
            if (property.length) {
                this.$el.empty();
                
                mappingProperties.push({target: property});

                browserStorageDelegate.set(this.data.mappingName + "_Properties",mappingProperties);
                
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "propertiesView", args: [this.data.mappingName]});
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "editMappingProperty", args: [this.data.mappingName, property]});
            }
        },
        validateMapping: function () {
            var propList = $("input[name='propertyList']", this.$el).val(),
                validationMessage = $("#Property_List .validation-message", this.$el),
                hasAvailableProps = this.data.availableTargetProps  && this.data.availableTargetProps.length,
                hasPropListValue = propList && propList.length,
                invalidProp = hasAvailableProps && hasPropListValue && !_.contains(this.data.availableTargetProps,propList),
                mappingExists = _.contains(_.pluck(this.data.currentProperties,"target"),propList),
                disableSave = function(message){
                    $("input[type=submit]", this.$el).prop("disabled", true);
                    validationMessage.text(message);
                };
            
            if (invalidProp) {
                disableSave($.t("templates.mapping.validPropertyRequired"));
                return false; 
            } else if(mappingExists){
                disableSave($.t("templates.mapping.mappingExists"));
                return false; 
            } else {
                $("input[type=submit]", this.$el).prop("disabled", false);
                validationMessage.text("");
                return true;
            }
     
        },
        close: function () {
            if(this.currentDialog) {
                this.currentDialog.dialog('destroy').remove();
            }
        },
        getAvailableTargetProps: function(){
            var availableProps;
            
            this.data.currentProperties = browserStorageDelegate.get(this.data.mappingName + "_Properties") || browserStorageDelegate.get("currentMapping").properties;
            
            browserStorageDelegate.set(this.data.mappingName + "_Properties", this.data.currentProperties);
            
            availableProps = browserStorageDelegate.get(this.data.mappingName + "_AvailableObjects").target.properties || [];
            
            return _.reject(availableProps, _.bind(function(p){
                return _.contains(_.pluck(this.data.currentProperties,"target"), p);
            },this));
        },
        render: function(params) {
            var _this = this,
                settings;

            this.data.mappingName = params[0];
            this.property = "_new";
            
            this.data.availableTargetProps = this.getAvailableTargetProps();
            
            settings = {
                "title": $.t("templates.mapping.propertyAdd.title"),
                "template": this.template,
                "postRender": _.bind(function() {
                    if(this.data.availableTargetProps){
                        autoCompleteUtils.selectionSetup($("input[name='propertyList']", this.$el),_.sortBy(this.data.availableTargetProps,function(s){ return s; }));
                    }
                },this)
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
                    eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "propertiesView", args: [this.data.mappingName]});
                },this),
                open: function(){

                    uiUtils.renderTemplate(settings.template, $(this), 
                                            _.extend(conf.globalData, _this.data), 
                                            function () {
                                                settings.postRender();
                                                $(_this.$el).dialog( "option", "position", { my: "center center", at: "center center", of: $(window) } );
                                            }, "append");
                }
            });
        }
    }); 
    
    return new AddPropertyMappingDialog();
});