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
    "lodash",
    "form2js",
    "jsonEditor",
    "org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationAbstractView",
    "org/forgerock/openidm/ui/common/delegates/SiteConfigurationDelegate",
    "org/forgerock/openidm/ui/common/delegates/OpenAMProxyDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/settings/authentication/SessionModuleView"

], function($, _,
            Form2js,
            JSONEditor,
            AuthenticationAbstractView,
            SiteConfigurationDelegate,
            OpenamProxyDelegate,
            UIUtils,
            ConfigDelegate,
            SessionModuleView) {

    var OpenAMSessionView = AuthenticationAbstractView.extend({
        template: "templates/admin/settings/authentication/modules/OPENAM_SESSION.html",

        knownProperties: AuthenticationAbstractView.prototype.knownProperties.concat([
            "openamDeploymentUrl",
            "openamUseExclusively",
            "openamLoginUrl",
            "openamLoginLinkText",
            "openamSSOTokenCookieName",
            "openamUserAttribute",
            "groupComparisonMethod",
            "truststoreType",
            "truststorePath",
            "truststorePassword"
        ]),
        model: {
            "amTokenTime": "5",
            "amTokenMinutes": false,
            "defaults": {
                "maxTokenLife": "120",
                "tokenIdleTime": "30",
                "maxTokenLifeMinutes": true,
                "tokenIdleTimeMinutes": true
            }
        },
        events: _.extend({
            "focus #input-properties\\.openamDeploymentUrl" : "openamDeploymentUrlFocus",
            "blur #input-properties\\.openamDeploymentUrl" : "openamDeploymentUrlBlur"
        },AuthenticationAbstractView.prototype.events),

        render: function (args) {
            this.data = _.clone(args, true);
            this.data.userOrGroupValue = "userRoles";
            this.data.userOrGroupOptions = _.clone(AuthenticationAbstractView.prototype.userOrGroupOptions, true);
            this.data.customProperties = this.getCustomPropertiesList(this.knownProperties, this.data.config.properties || {});
            this.data.userOrGroupDefault = this.getUserOrGroupDefault(this.data.config || {});

            this.beforeSaveCallbacks.push(this.handleOpenAMUISettings);

            SiteConfigurationDelegate.getConfiguration().then((uiConfig) => {
                var props = this.data.config.properties;

                //amUIProperties to config
                props.openamUseExclusively = props.openamUseExclusively || uiConfig.openamUseExclusively;
                props.openamLoginLinkText = props.openamLoginLinkText || uiConfig.openamLoginLinkText;
                props.openamLoginUrl = props.openamLoginUrl || uiConfig.openamLoginUrl;

                this.parentRender(() => {
                    this.postRenderComponents({
                        "customProperties":this.data.customProperties,
                        "name": this.data.config.name,
                        "augmentSecurityContext": this.data.config.properties.augmentSecurityContext || {},
                        "userOrGroup": this.data.userOrGroupDefault
                    });
                });
            });
        },
        openamDeploymentUrlFocus: function (e) {
            $(e.target).attr("beforeValue", $(e.target).val());
        },
        openamDeploymentUrlBlur: function (e) {
            var openamLoginUrl = this.$el.find("#input-properties\\.openamLoginUrl");

            openamLoginUrl.val(
                openamLoginUrl.val().replace($(e.target).attr("beforeValue"), $(e.target).val())
            );
        },

        /**
         * Overriding getConfig here so the tokenIdleTime and maxTokenLife values of the SessionModule can be set appropriately
         */
        getConfig: function () {
            var config = AuthenticationAbstractView.prototype.getConfig.call(this);

            //if this module is enabled re-render the sessionModule changing maxTokenLifeSeconds to 5
            if (config.enabled &&
                SessionModuleView.data.maxTokenLife === this.model.defaults.maxTokenLife && SessionModuleView.data.maxTokenLifeMinutes === this.model.defaults.maxTokenLifeMinutes &&
                SessionModuleView.data.tokenIdleTime === this.model.defaults.tokenIdleTime && SessionModuleView.data.tokenIdleTimeMinutes === this.model.defaults.tokenIdleTimeMinutes) {

                SessionModuleView.data.sessionModule.properties.maxTokenLifeSeconds = this.model.amTokenTime;
                SessionModuleView.data.sessionModule.properties.tokenIdleTimeSeconds = this.model.amTokenTime;

                SessionModuleView.data.maxTokenLife = this.model.amTokenTime;
                SessionModuleView.data.maxTokenLifeMinutes = this.model.amTokenMinutes;
                SessionModuleView.data.tokenIdleTime = this.model.amTokenTime;
                SessionModuleView.data.tokenIdleTimeMinutes = this.model.amTokenMinutes;

                SessionModuleView.reRender({"changes": SessionModuleView.data.sessionModule});
            }

            return config;
        },

        /**
         * Checks to make sure there is a valid openam running at the openamDeploymentUrl,
         * alerts the user if not and gives the option to continue. If there is a valid
         * openam or the user chooses to continue then it picks out openam UI
         * settings which are not supposed to be in authentication.json and
         * saves them to ui-configuration.json
         *
         * @param authenticationDataChanges - current state of AuthenticationAbstractView authenticationDataChanges
         * @returns {promise}
         */
        handleOpenAMUISettings: function(authenticationDataChanges){
            var prom = $.Deferred(),
                amSessionModuleIndex = _.findIndex(authenticationDataChanges.serverAuthContext.authModules, { name: "OPENAM_SESSION"}),
                moduleSettings = authenticationDataChanges.serverAuthContext.authModules[amSessionModuleIndex],
                amUIProperties = [
                    "openamLoginUrl",
                    "openamLoginLinkText",
                    "openamUseExclusively"
                ],
                amSettings = _.pick(moduleSettings.properties, amUIProperties),
                confirmed = function(){
                    SiteConfigurationDelegate.getConfiguration().then(function(uiConfig){
                        ConfigDelegate.updateEntity("ui/configuration", { configuration: _.extend(uiConfig, amSettings) }).then(function() {
                            //remove the amUIProperties before saving authentication.json
                            moduleSettings.properties = _.omit(moduleSettings.properties,amUIProperties);
                            prom.resolve();
                        });
                    });
                };

            amSettings.openamAuthEnabled = moduleSettings.enabled;

            if (amSettings.openamAuthEnabled) {
                // Validate openamDeploymentUrl
                OpenamProxyDelegate.serverinfo(moduleSettings.properties.openamDeploymentUrl).then(_.bind(function(info){
                    if (info.cookieName) {
                        // Set openamSSOTokenCookieName for this module
                        moduleSettings.properties.openamSSOTokenCookieName = info.cookieName;
                        confirmed();
                    } else {
                        UIUtils.confirmDialog($.t("templates.auth.openamDeploymentUrlConfirmation"), confirmed);
                    }
                },this),
                _.bind(function(){
                    UIUtils.confirmDialog($.t("templates.auth.openamDeploymentUrlConfirmation"), confirmed);
                },this));
            } else {
                confirmed();
            }

            return prom;
        }
    });

    return new OpenAMSessionView();
});
