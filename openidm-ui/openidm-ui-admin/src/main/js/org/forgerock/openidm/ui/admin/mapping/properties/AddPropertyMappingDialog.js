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
    "org/forgerock/openidm/ui/admin/util/AdminUtils",
    "bootstrap-dialog",
    "selectize"
], function($, _,
            MappingAdminAbstractView,
            conf,
            uiUtils,
            eventManager,
            constants,
            autoCompleteUtils,
            EditPropertyMappingDialog,
            AdminUtils,
            BootstrapDialog,
            selectize) {

    var AddPropertyMappingDialog = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/properties/AddPropertyMappingDialogTemplate.html",
        data: {
            width: 600,
            height: 400
        },
        el: "#dialogs",
        events: {
            "click input[type=submit]": "formSubmit"
        },
        model: {},

        formSubmit: function (event) {
            var property = $("#addPropertySelect" ,this.$el).val(),
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
                    saveCallback: this.model.saveCallback
                });
            }
        },

        close: function () {
            $("#dialogs").hide();
        },

        render: function(params, callback) {
            var targetType;

            this.data.mappingName = this.getMappingName();
            this.property = "_new";
            this.data.currentProperties = params.mappingProperties || this.getCurrentMapping().properties;
            this.model.saveCallback = params.saveCallback;
            this.model.mapping = this.getCurrentMapping();

            targetType = this.model.mapping.target.split("/");

            AdminUtils.findPropertiesList(targetType).then(_.bind(function(properties){
                this.data.resourceSchema = properties;

                this.renderAddProperty(callback);
            }, this));
        },

        renderAddProperty: function(callback) {
            var _this = this,
                settings;

            settings = {
                "title": $.t("templates.mapping.propertyAdd.title"),
                "template": this.template,
                "postRender": _.bind(function() {
                    _this.$el.find("#addPropertySelect").selectize({
                        persist: false,
                        create: false,
                        onChange: _.bind(function(value) {
                            if(value.length > 0) {
                                this.model.dialog.$modalFooter.find("#scriptDialogUpdate").prop("disabled", false);
                            } else {
                                this.model.dialog.$modalFooter.find("#scriptDialogUpdate").prop("disabled", true);
                            }
                        }, this)
                    });
                },this)
            };

            this.currentDialog = $('<form id="propertyMappingDialogForm"></form>');

            $('#dialogs').append(this.currentDialog);
            this.setElement(this.currentDialog);

            this.model.dialog = BootstrapDialog.show({
                title: settings.title,
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.currentDialog,
                size: BootstrapDialog.SIZE_WIDE,
                onshown : function (dialogRef) {
                    uiUtils.renderTemplate(settings.template, _this.$el,
                        _.extend(conf.globalData, _this.data),
                        function () {
                            settings.postRender();

                            _this.$el.find("#addPropertySelect")

                            _this.$el.find("#addPropertySelect").focus();

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
