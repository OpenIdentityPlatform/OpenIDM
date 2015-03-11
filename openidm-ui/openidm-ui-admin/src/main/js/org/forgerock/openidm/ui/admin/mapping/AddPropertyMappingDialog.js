/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
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
    "org/forgerock/openidm/ui/admin/util/AutoCompleteUtils",
    "bootstrap-dialog"
], function(AbstractView, conf, uiUtils, eventManager, constants, browserStorageDelegate, autoCompleteUtils, BootstrapDialog) {
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
            
            if(event){
                event.preventDefault();
            }

            if (property.length) {
                this.$el.empty();

                mappingProperties.push({target: property});

                browserStorageDelegate.set(this.data.mappingName + "_Properties",mappingProperties);

                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "propertiesView", args: [this.data.mappingName]});
                this.close();
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "editMappingProperty", args: [this.data.mappingName, mappingProperties.length.toString()]});
            }
        },

        validateMapping: function () {
            var propList = $("input[name='propertyList']", this.$el).val(),
                validationMessage = $("#Property_List .validation-message", this.$el),
                hasAvailableProps = this.data.availableTargetProps  && this.data.availableTargetProps.length,
                hasPropListValue = propList && propList.length,
                invalidProp = hasAvailableProps && hasPropListValue && !_.contains(this.data.availableTargetProps,propList),
                disableSave = function(message){
                    $("#scriptDialogUpdate").prop("disabled", true);
                    validationMessage.text(message);
                };

            if (invalidProp) {
                disableSave($.t("templates.mapping.validPropertyRequired"));
                return false;
            } else {
                $("#scriptDialogUpdate").prop("disabled", false);
                validationMessage.text("");
                return true;
            }

        },

        close: function () {
            $("#dialogs").hide();
        },

        getAvailableTargetProps: function(){
            var availableProps;

            this.data.currentProperties = browserStorageDelegate.get(this.data.mappingName + "_Properties") || browserStorageDelegate.get("currentMapping").properties;

            browserStorageDelegate.set(this.data.mappingName + "_Properties", this.data.currentProperties);

            availableProps = browserStorageDelegate.get(this.data.mappingName + "_AvailableObjects").target.properties || [];

            return availableProps;
        },

        render: function(params, callback) {
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


            this.currentDialog = $('<form id="propertyMappingDialogForm"></form>');

            $('#dialogs').append(this.currentDialog);
            this.setElement(this.currentDialog);
            
            BootstrapDialog.show({
                title: settings.title,
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.currentDialog,
                size: BootstrapDialog.SIZE_WIDE,
                onhide: function(dialogRef){
                    eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "propertiesView", args: [_this.data.mappingName]});
                },
                onshown : function (dialogRef) {
                    uiUtils.renderTemplate(settings.template, _this.$el,
                            _.extend(conf.globalData, _this.data),
                            function () {
                                settings.postRender();
                                $(':input:first', _this.currentDialog).focus();
                                if(callback){
                                    callback();
                                }
                            }, "replace");
                },
                buttons: [{
                    label: $.t("common.form.cancel"),
                    id:"scriptDialogCancel",
                    action: function(dialogRef) {
                        dialogRef.close();
                    }
                },
                {
                    label: $.t("common.form.update"),
                    id:"scriptDialogUpdate",
                    cssClass: 'btn-primary',
                    action: _.bind(function(dialogRef) {
                        this.formSubmit();
                        dialogRef.close();
                    },_this)
                }]
            });
        }
    });

    return new AddPropertyMappingDialog();
});