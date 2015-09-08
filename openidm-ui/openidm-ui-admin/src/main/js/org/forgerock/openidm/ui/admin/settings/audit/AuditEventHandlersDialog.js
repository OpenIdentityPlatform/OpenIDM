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
    "bootstrap-tabdrop"

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
            _(JSONEditor.defaults.options).extend({
                theme: "bootstrap3",
                iconlib: "fontawesome4",
                disable_edit_json: true,
                disable_array_reorder: true,
                disable_collapse: true,
                disable_properties: true,
                no_additional_properties: true,
                show_errors: "never"
            });

            var _this = this,
                title = "";

            this.data = _.clone(args);

            this.data.events = {};

            if (this.data.newEventHandler) {
                title = $.t("templates.audit.eventHandlers.dialog.add") + ": " +  _.last(this.data.eventHandlerType.split("."));
            } else {
                title = $.t("templates.audit.eventHandlers.dialog.edit") + ": " + this.data.eventHandler.name;
            }

            _.each(this.data.availableEvents, function(event) {
                this.data.events[event] = _.contains(this.data.eventHandler.events, event);
            }, this);

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

                                if (!_.isEmpty(this.data.schemaEditor)) {
                                    data.eventHandler.config =  this.data.schemaEditor.getValue();
                                }

                                data.eventHandler.name = this.$el.find("#eventHandlerName").val();
                                data.eventHandler.class = this.data.eventHandlerType;
                                data.eventHandler.events = [];

                                _.each(this.$el.find(".auditEventsCheckBox"), function(check) {
                                    if (check.checked) {
                                        data.eventHandler.events.push(check.value);
                                    }
                                }, this);

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
                        this.data.schemaEditor = new JSONEditor(this.$el.find("#auditEventHandlerConfig")[0], {schema: schema});
                        this.data.schemaEditor.setValue(this.data.eventHandler.config);
                    }
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