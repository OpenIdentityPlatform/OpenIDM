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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "bootstrap-dialog",
    "org/forgerock/openidm/ui/admin/authentication/AuthenticationAbstractView",
    "org/forgerock/openidm/ui/admin/delegates/ExternalAccessDelegate"

], function($, _,
            UIUtils,
            ValidatorsManager,
            Configuration,
            EventManager,
            Constants,
            ConfigDelegate,
            BootstrapDialog,
            AuthenticationAbstractView,
            ExternalAccessDelegate) {

    var ProvidersModuleDialogView = AuthenticationAbstractView.extend({
        template: "templates/admin/authentication/ProvidersModuleDialogViewTemplate.html",
        element: "#dialogs",
        noBaseTemplate: true,
        events: {
            "change #amURL": "generateAuthFromWellKnownURL"
        },
        partials: [
            "partials/_alert.html"
        ],
        model: {
            defaultConfig: {
                "enabled": true,
                "properties": {
                    "resolvers": [{
                        "name":"OPENAM",
                        "icon":"<button class=\"btn btn-lg btn-default btn-block btn-social-provider\"><img src=\"images/forgerock_logo.png\">Sign In</button>",
                        "scope":["openid"],
                        "well-known": "https://openam.example.com/openam/oauth2/.well-known/openid-configuration",
                        "client_id": "",
                        "client_secret": "",
                        "authorization_endpoint": "",
                        "token_endpoint": ""
                    }],
                    "queryOnResource": "managed/user",
                    "defaultUserRoles": ["openidm-authorized"],
                    "openIdConnectHeader": "authToken",
                    "propertyMapping": {
                        "authenticationId": "userName",
                        "userRoles": "authzRoles"
                    },
                    "augmentSecurityContext": {
                        "type": "text/javascript",
                        "globals": {
                            "sessionValidationBaseEndpoint": ""
                        },
                        "file": "auth/amSessionCheck.js"
                    }
                },
                "name": "OPENID_CONNECT"
            }
        },
        data: {
            pw_filler: $.t("common.form.passwordPlaceholder"),
            AMDocHelpUrl: Constants.AM_DOC_URL
        },
        /**
         * @param configs {object}
         * @param configs.config {object} - the existing config for the module
         * @param callback
         */
        render: function (configs) {
            _.extend(this.model, configs);
            var self = this;

            this.data.interruptClose = false;

            if (_.isEmpty(this.model.config)) {
                this.setConfig(this.model.defaultConfig);
            } else {
                this.setConfig(this.model.config);
            }


            this.data.adminCallback = window.location.protocol+"//"+window.location.host + "/admin/oauthReturn.html";
            this.data.enduserCallback =  window.location.protocol+"//"+window.location.host + "/oauthReturn.html";

            this.parentRender(_.bind(function() {

                this.model.currentDialog = $('<div id="ProviderModuleDialog"></div>');
                this.setElement(this.model.currentDialog);
                $('#dialogs').append(this.model.currentDialog);

                BootstrapDialog.show({
                    title: $.t("templates.auth.providers.providerDialogTitle"),
                    size: BootstrapDialog.SIZE_WIDE,
                    type: BootstrapDialog.TYPE_DEFAULT,
                    message: this.model.currentDialog,
                    onshown: function() {
                        UIUtils.renderTemplate(
                            self.template,
                            self.$el,
                            _.extend({}, Configuration.globalData, self.data),
                            () => {
                                // Validates starting values
                                self.generateAuthFromWellKnownURL(true);

                                ValidatorsManager.bindValidators(self.$el.find("form"));
                                ValidatorsManager.validateAllFields(self.$el.find("form"));
                            },
                            "replace"
                        );
                    },
                    onhide: function() {
                        if (!self.data.interruptClose) {
                            self.model.cancelCallback();
                        }
                    },
                    buttons: [
                        {
                            label: $.t("common.form.cancel"),
                            action: function (dialogRef) {
                                dialogRef.close();
                            }
                        }, {
                            label: $.t("common.form.submit"),
                            id: "submitAuth",
                            cssClass: "btn-primary",
                            action: function (dialogRef) {
                                if (this.hasClass("disabled")) {
                                    return false;
                                }

                                var saveConfig = self.getSaveConfig();

                                if (saveConfig) {
                                    self.saveConfig(saveConfig);
                                    self.data.interruptClose = true;
                                    dialogRef.close();
                                }
                            }
                        }
                    ]
                });
            }, this));
        },

        getConfig: function() {
            return this.data.currentConfig;
        },

        setConfig: function(config) {
            this.data.currentConfig = config;
        },

        getSaveConfig: function() {
            var currentConfig = this.getConfig(),
                id = this.$el.find("#amClientID").val(),
                secret = this.$el.find("#amClientSecret").val();

            if (secret.length > 0) {
                _.set(currentConfig.properties.resolvers[0], "client_secret", secret);
            }

            _.set(currentConfig.properties.resolvers[0], "client_id", id);
            _.set(currentConfig, "enabled", true);

            return currentConfig;
        },

        getAuthModulesConfig: function(authData, AMAuthConfig) {
            var allAuthModules = _.get(authData, "authModules");

            // Set cache period
            _.set(authData, "sessionModule.properties.maxTokenLifeSeconds", "5");
            _.set(authData, "sessionModule.properties.tokenIdleTimeSeconds", "5");
            delete authData.sessionModule.properties.maxTokenLifeMinutes;
            delete authData.sessionModule.properties.tokenIdleTimeMinutes;

            // Disable all modules besides static user and internal user which are always enabled.
            _.map(allAuthModules, function(module) {
                module.enabled = (module.name === "STATIC_USER" || module.name === "INTERNAL_USER");
            });

            let openAMModuleIndex = this.getAMModuleIndex(allAuthModules);

            // If the OPENAM modules doesn't exist push it
            if (openAMModuleIndex === -1) {
                allAuthModules.push(AMAuthConfig);

            // If it does exist, replace it
            } else {
                allAuthModules[openAMModuleIndex] = AMAuthConfig;
            }

            return authData;
        },

        saveConfig: function(AMAuthConfig) {
            var newAuth = this.getAuthModulesConfig(this.getAuthenticationData(), AMAuthConfig);
            this.setProperties(["authModules", "sessionModule"], newAuth);
            this.saveAuthentication();
        },

        generateAuthFromWellKnownURL: function(suppressMsg) {
            var wellKnownURL = this.$el.find("#amURL").val(),
                currentConfig = this.getConfig();

            ExternalAccessDelegate.externalRestRequest(wellKnownURL).then(
                (config) => {
                    let sessionValidationBaseEndpoint = wellKnownURL.replace('oauth2', 'json').replace('.well-known/openid-configuration', 'sessions/');

                    _.set(currentConfig.properties.resolvers[0], "well-known", wellKnownURL);
                    _.set(currentConfig.properties.resolvers[0], "authorization_endpoint", config.authorization_endpoint);
                    _.set(currentConfig.properties.resolvers[0], "token_endpoint", config.token_endpoint);
                    _.set(currentConfig.properties.resolvers[0], "end_session_endpoint", config.end_session_endpoint);
                    _.set(currentConfig.properties.augmentSecurityContext, "globals.sessionValidationBaseEndpoint", sessionValidationBaseEndpoint);

                    this.setConfig(currentConfig);

                    this.$el.find("#wellKnownURLError").hide();
                    this.customValidate();
                },
                () => {
                    if (!suppressMsg) {
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "amAuthWellKnownEndpointFailure");
                    }
                    this.$el.find("#wellKnownURLError").show();
                    $("#submitAuth").toggleClass("disabled", true);
                }
            );
        },

        validationSuccessful: function (event) {
            AuthenticationAbstractView.prototype.validationSuccessful(event);
            this.customValidate();
        },

        validationFailed: function (event, details) {
            AuthenticationAbstractView.prototype.validationFailed(event, details);
            this.customValidate();
        },

        customValidate: function() {
            var formValid = ValidatorsManager.formValidated(this.$el.find("form"));

            // If there is a url provided, but it isn't valid
            if (this.$el.find("#wellKnownURLError").is(":visible")) {
                $("#submitAuth").toggleClass("disabled", true);

            } else {
                $("#submitAuth").toggleClass("disabled", !formValid);
            }

        }

    });

    return new ProvidersModuleDialogView();
});
