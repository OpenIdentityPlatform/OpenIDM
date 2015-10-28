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

/*global define*/
/*jslint es5: true */


define("org/forgerock/openidm/ui/admin/settings/audit/AuditEventHandlersDialog", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditAdminAbstractView",
    "org/forgerock/openidm/ui/admin/delegates/AuditDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "bootstrap-dialog",
    "jsonEditor",
    "bootstrap-tabdrop",
    "selectize"

], function($, _, AuditAdminAbstractView,
            AuditDelegate,
            uiUtils,
            conf,
            InlineScriptEditor,
            constants,
            validatorsManager,
            BootstrapDialog,
            JSONEditor) {

    var AuditEventHandlersDialog = AuditAdminAbstractView.extend({
        template: "templates/admin/settings/audit/AuditEventHandlersDialogTemplate.html",
        el: "#dialogs",
        events: {
            "onValidate": "onValidate",
            "customValidate": "customValidate"
        },
        model: {},

        render: function(args, callback) {
            var _this = this,
                title = "";

            this.data = _.clone(args);

            // We don't want JSON Editor handeling these fields, we will add them back to the config before saving.
            if (_.has(this.data.eventHandler, "config")) {
                if (_.has(this.data.eventHandler.config, "name")) {
                    this.data.name = this.data.eventHandler.config.name;
                    delete this.data.eventHandler.config.name;
                }
                if (_.has(this.data.eventHandler.config, "topics")) {
                    this.data.selectedTopics = this.data.eventHandler.config.topics;
                    delete this.data.eventHandler.config.topics;
                }

                if (_.has(this.data.eventHandler.config, "enabled")) {
                    this.data.enabled = this.data.eventHandler.config.enabled;
                    delete this.data.eventHandler.config.enabled;
                }
            }

            if (this.data.newEventHandler) {
                title = $.t("templates.audit.eventHandlers.dialog.add") + ": " +  _.last(this.data.eventHandlerType.split("."));
            } else {
                title = $.t("templates.audit.eventHandlers.dialog.edit") + ": " + this.data.name;
            }

            this.data.topics = _.union(this.data.selectedTopics, this.getTopics());

            AuditDelegate.availableHandlers().then(_.bind(function (data) {
                this.data.handler = _.findWhere(data, {"class": this.data.eventHandlerType});

                this.model.currentDialog = $('<div id="AuditEventHandlersDialog"></div>');
                this.setElement(this.model.currentDialog);
                $('#dialogs').append(this.model.currentDialog);

                BootstrapDialog.show({
                    title: title,
                    size: BootstrapDialog.SIZE_WIDE,
                    type: BootstrapDialog.TYPE_DEFAULT,
                    message: this.model.currentDialog,
                    onshown: _this.renderTemplate(_this.data),
                    buttons: [
                        {
                            label: $.t("common.form.cancel"),
                            action: function(dialogRef) {
                                dialogRef.close();
                            }
                        }, {
                            label: $.t("common.form.submit"),
                            id: "submitAuditEventHandlers",
                            cssClass: "btn-primary",
                            action: _.bind(function(dialogRef) {
                                var data = {
                                    eventHandler: {}
                                };

                                data.eventHandler.class = this.data.eventHandlerType;

                                if (!_.isEmpty(this.data.schemaEditor)) {
                                    data.eventHandler.config =  this.data.schemaEditor.getValue();
                                }

                                data.eventHandler.config.name = this.$el.find("#eventHandlerName").val();
                                data.eventHandler.config.topics = this.$el.find(".topics").val();
                                data.eventHandler.config.enabled = this.$el.find("#enabled").is(":checked");
                                data.useForQueries = this.$el.find(".useForQueries").is(":checked");

                                if (callback) {
                                    callback(data);
                                }

                                dialogRef.close();
                            }, _this)
                        }
                    ]
                });
            }, this));
        },

        /**
         * Performs a deep search of a provided object,
         * if any nested properties has the key "description" it is translated.
         * @param schema
         */
        translateDescriptions: function(schema) {
            if (_.has(schema, "description")) {
                schema.description = $.t(schema.description);
            }

            _.forEach(schema, function(subSchema) {
                if (_.isObject(subSchema)) {
                    this.translateDescriptions(subSchema);
                }
            }, this);
        },

        renderTemplate: function(data) {
            uiUtils.renderTemplate(
                this.template,
                this.$el,
                _.extend({}, conf.globalData, data),
                _.bind(function(data) {
                    var schema = {};
                    if (_.has(this.data.handler, "config")) {
                        schema = this.data.handler.config;
                    }
                    validatorsManager.bindValidators(this.$el.find("#auditEventHandlersForm"));
                    validatorsManager.validateAllFields(this.$el.find("#auditEventHandlersForm"));

                    if (!_.isEmpty(schema)) {

                        if (_.has(schema.properties, "name")) {
                            delete schema.properties.name;
                        }
                        if (_.has(schema.properties, "topics")) {
                            delete schema.properties.topics;
                        }
                        if (_.has(schema.properties, "enabled")) {
                            delete schema.properties.enabled;
                        }

                        this.translateDescriptions(schema);
                        this.data.schemaEditor = new JSONEditor(this.$el.find("#auditEventHandlerConfig")[0], {
                            "schema": schema,
                            "disable_edit_json": true,
                            "disable_array_reorder": false,
                            "disable_collapse": true,
                            "disable_properties": true,
                            "show_errors": "never",
                            "template": "handlebars",
                            "iconlib": "fontawesome4",
                            "theme": "bootstrap3",
                            "no_additional_properties": true,
                            "additionalItems": false,
                            "required_by_default": true
                        });

                        this.data.schemaEditor.setValue(this.data.eventHandler.config);
                    }

                    this.$el.find(".topics").selectize({
                        delimiter: ',',
                        persist: false,
                        create: false,
                        items: this.data.selectedTopics
                    });

                }, this),
                "replace"
            );
        },

        customValidate: function() {
            this.validationResult = validatorsManager.formValidated(this.$el.find("#auditEventHandlersForm"));
            this.$el.parentsUntil(".model-content").find("#submitAuditEventHandlers").prop('disabled', !this.validationResult);
        }
    });

    return new AuditEventHandlersDialog();
});