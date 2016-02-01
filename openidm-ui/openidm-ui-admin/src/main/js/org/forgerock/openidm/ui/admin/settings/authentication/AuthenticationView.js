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

/*global define */

define("org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationAbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/settings/authentication/SessionModuleView",
    "org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationModuleView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/SiteConfigurationDelegate",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/common/delegates/OpenAMProxyDelegate"

], function($, _,
            AuthenticationAbstractView,
            Constants,
            SessionModuleView,
            AuthenticationModuleView,
            UIUtils,
            SiteConfigurationDelegate,
            ConfigDelegate,
            OpenAMProxyDelegate) {

    var AuthenticationView = AuthenticationAbstractView.extend({
        template: "templates/admin/settings/AuthenticationTemplate.html",
        element: "#authenticationContainer",
        noBaseTemplate: true,
        events: {
            "click #submitAuth": "save"
        },
        data: {},
        model: {},

        render: function (args, callback) {
            this.data.docHelpUrl = Constants.DOC_URL;

            this.retrieveAuthenticationData(_.bind(function() {
                this.parentRender(function () {

                    SessionModuleView.render();
                    AuthenticationModuleView.render({
                        "addedOpenAM": function() {
                            SessionModuleView.addedOpenAM();
                        }
                    });

                    if (callback) {
                        callback();
                    }

                });
            }, this));
        },

        save: function (e) {
            var doSave = _.bind(function (reload) {
                    this.saveAuthentication().then(function() {
                        if (!reload) {
                            SessionModuleView.render();
                            AuthenticationModuleView.render({
                                "addedOpenAM": function () {
                                    SessionModuleView.addedOpenAM();
                                }
                            });
                        } else {
                            /*when changes are made to the OPENAM_SESSION module the page needs to be reloaded so
                              the changes to ui-configuration.json are seen immediately*/
                            location.reload(true);
                        }
                    });
                }, this);
            e.preventDefault();
            
            if(AuthenticationModuleView.model.amSettings) {
                this.handleOpenAMUISettings(AuthenticationModuleView.model.amSettings).then(function () {
                    doSave(true);
                });
            } else {
                doSave();
            }
        },

        handleOpenAMUISettings: function(amSettings){
            var prom = $.Deferred(),
                amAuthIndex = _.findIndex(AuthenticationModuleView.model.changes, { name: "OPENAM_SESSION" }),
                confirmed = function(){
                    SiteConfigurationDelegate.getConfiguration().then(function(uiConfig){
                        ConfigDelegate.updateEntity("ui/configuration", { configuration: _.extend(uiConfig, amSettings) }).then(function() {
                            prom.resolve();
                        });
                    });
                };
            
            if (amSettings.openamAuthEnabled) {
                // Validate openamDeploymentUrl
                OpenAMProxyDelegate.serverinfo(amSettings.openamDeploymentUrl).then(_.bind(function(info){
                        if (info.cookieName) {
                            // Set openamSSOTokenCookieName for this module
                            AuthenticationModuleView.model.changes[amAuthIndex].properties.openamSSOTokenCookieName = info.cookieName;
                            confirmed();
                        } else {
                            UIUtils.jqConfirm($.t("templates.auth.openamDeploymentUrlConfirmation"), confirmed);
                        }
                    },this),
                    _.bind(function(){
                        UIUtils.jqConfirm($.t("templates.auth.openamDeploymentUrlConfirmation"), confirmed);
                    },this)
                );
            } else {
                confirmed();
            }
            
            //remove this property so it doesn't get saved in ui-configuration.json
            delete amSettings.openamDeploymentUrl;
            
            return prom;
        }

    });

    return new AuthenticationView();
});
