/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/mapping/properties/AddPropertyMappingDialog", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/util/AutoCompleteUtils",
    "org/forgerock/openidm/ui/admin/mapping/properties/EditPropertyMappingDialog",
    "bootstrap-dialog"
], function($, _,
            MappingAdminAbstractView,
            conf,
            uiUtils,
            eventManager,
            constants,
            autoCompleteUtils,
            EditPropertyMappingDialog,
            BootstrapDialog) {

    var AddPropertyMappingDialog = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/properties/AddPropertyMappingDialogTemplate.html",
        data: {
            width: 600,
            height: 400
        },
        el: "#dialogs",
        events: {
            "click input[type=submit]": "formSubmit",
            "change :input": "validateMapping"
        },
        model: {},

        formSubmit: function (event) {
            var property = $(":input[name=propertyList]",this.$el).val(),
                mappingProperties = this.data.currentProperties;

            if (event) {
                event.preventDefault();
            }

            if (property.length) {
                this.$el.empty();

                mappingProperties.push({target: property});

                this.model.saveCallback(mappingProperties);

                this.close();
                EditPropertyMappingDialog.render({
                    id: mappingProperties.length.toString(),
                    mappingProperties: mappingProperties,
                    availProperties: this.data.availableTargetProps,
                    saveCallback: this.model.saveCallback
                });
            }
        },

        validateMapping: function () {
            var propList = $("input[name='propertyList']", this.$el).val(),
                validationMessage = $("#Property_List .validation-message", this.$el),
                hasAvailableProps = this.data.availableTargetProps  && this.data.availableTargetProps.length,
                hasPropListValue = propList && propList.length,
                invalidProp = hasAvailableProps && hasPropListValue && !_.contains(this.data.availableTargetProps, propList),
                disableSave = function(message) {
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

        render: function(params, callback) {
            var _this = this,
                settings;

            this.data.mappingName = this.getMappingName();
            this.property = "_new";
            this.data.currentProperties = params.mappingProperties || this.getCurrentMapping().properties;
            this.data.availableTargetProps = params.availProperties;
            this.model.saveCallback = params.saveCallback;

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
                onshown : function (dialogRef) {
                    uiUtils.renderTemplate(settings.template, _this.$el,
                        _.extend(conf.globalData, _this.data),
                        function () {
                            settings.postRender();
                            $(':input:first', _this.currentDialog).focus();
                            if (callback) {
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
