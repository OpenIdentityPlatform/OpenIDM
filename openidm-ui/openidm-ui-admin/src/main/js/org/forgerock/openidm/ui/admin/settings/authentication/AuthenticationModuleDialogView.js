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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "jsonEditor",
    "handlebars",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationAbstractView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/AdminUtils",
    "selectize"
], function($, _,
            JSONEditor,
            Handlebars,
            BootstrapDialog,
            Conf,
            AuthenticationAbstractView,
            ConfigDelegate,
            InlineScriptEditor,
            ConnectorDelegate,
            UIUtils,
            AdminUtils) {

    var AuthenticationModuleDialogView = AuthenticationAbstractView.extend({
        template: "templates/admin/settings/authentication/AuthenticationModuleDialogTemplate.html",
        element: "#dialogs",
        noBaseTemplate: true,
        events: {
            "click .advancedForm > div > h3": "toggleAdvanced",
            "change .changes-watched": "changed"
        },
        model: {},
        data: {},

        /**
         * @param configs {object}
         * @param configs.config {object} - the existing config for the module
         * @param callback
         */
        render: function (configs) {
            this.model = _.extend(
                {
                    defaultUserRoles: [
                        "openidm-admin",
                        "openidm-authorized",
                        "openidm-cert",
                        "openidm-reg",
                        "openidm-task-manager"
                    ]
                },
                configs
            );

            this.model.readableName = $.t("templates.auth.modules." + this.model.config.name + ".name");

            //TODO: OPENIDM-6189 - When ModuleLoader properly rejects promises use the following commented out code and remove the getModuleView function.
            //var viewPath = "org/forgerock/openidm/ui/admin/settings/authentication/modules/test" + this.model.config.name;

            // Get resources and get the JSON schema
            //$.when(AdminUtils.getAvailableResourceEndpoints(), ModuleLoader.load(viewPath))
            $.when(AdminUtils.getAvailableResourceEndpoints(), this.getModuleView(this.model.config.name))
                .done(_.bind(function(resources, view) {
                    $("#missingTemplateError").toggleClass("hidden", true);
                    this.parentRender(() => {
                        var self = this,
                            prefix =  $.t("templates.auth.edit");

                        if (this.model.newModule) {
                            prefix =  $.t("templates.auth.new");
                        }

                        this.model.currentDialog = $('<div id="AuthenticationModuleDialog"></div>');
                        this.setElement(this.model.currentDialog);
                        $('#dialogs').append(this.model.currentDialog);

                        BootstrapDialog.show({
                            title: prefix + " " + this.model.readableName + " " + $.t("templates.auth.authFieldsetName"),
                            size: BootstrapDialog.SIZE_WIDE,
                            type: BootstrapDialog.TYPE_DEFAULT,
                            message: '<div id="AuthenticationModuleDialogContainer"></div>',
                            onshown: () => {
                                view.render(_.extend({"resources": resources}, this.model));
                            },
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
                                    action: function(dialogRef) {
                                        if (this.hasClass("disabled")) {
                                            return false;
                                        }

                                        self.model.saveCallback(view.getConfig());
                                        dialogRef.close();
                                    }
                                }
                            ]
                        });
                    });
                }, this))
                .fail((brokenModuleName) => {
                    $("#missingTemplateError").toggleClass("hidden", false);
                    $("#missingTemplateName").text(brokenModuleName);
                });
        },

        /**
         * Gets the view corresponding to the moduleName provided
         *
         * @param moduleName
         * @returns {promise}
         */
        getModuleView: function(moduleName) {
            var viewPromise = $.Deferred();

            require(["org/forgerock/openidm/ui/admin/settings/authentication/modules/" + moduleName],
                (result) => {
                    viewPromise.resolve(result);
                },
                () => {
                    viewPromise.reject(moduleName);
                }
            );

            return viewPromise;
        }
    });

    return new AuthenticationModuleDialogView();
});
