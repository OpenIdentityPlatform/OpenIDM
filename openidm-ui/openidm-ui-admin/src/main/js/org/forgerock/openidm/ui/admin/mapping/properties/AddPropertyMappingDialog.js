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
 * Copyright 2014-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
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

            if (property && property.length) {
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
                this.data.resourcePropertiesList = _.chain(properties).keys().sortBy().value();

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
                                this.model.dialog.$modalFooter.find("#scriptDialogUpdate").prop("disabled", false).focus();
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

                            _this.model.dialog.$modalFooter.find("#scriptDialogUpdate").prop("disabled", false).focus();

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
                        hotkey: 13,
                        action: _.bind(function(dialogRef) {
                            this.formSubmit();
                            dialogRef.close();
                        },_this)
                    }
                ]
            });
        }
    });

    return new AddPropertyMappingDialog();
});
