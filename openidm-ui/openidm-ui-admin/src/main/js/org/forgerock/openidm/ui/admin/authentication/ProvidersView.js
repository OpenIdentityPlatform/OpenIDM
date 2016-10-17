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
    "org/forgerock/openidm/ui/admin/authentication/AuthenticationAbstractView",
    "org/forgerock/openidm/ui/admin/authentication/ProvidersModuleDialogView",
    "org/forgerock/openidm/ui/common/delegates/SocialDelegate",
    "org/forgerock/openidm/ui/common/util/oAuthUtils"

], function($, _,
            AuthenticationAbstractView,
            ProvidersModuleDialogView,
            SocialDelegate,
            OAuthUtils) {

    var ProvidersView = AuthenticationAbstractView.extend({
        template: "templates/admin/authentication/ProvidersTemplate.html",
        element: "#providerContainer",
        noBaseTemplate: true,
        events: {
            "change .providerSelection": "providerChanged",
            "click .edit-am-module": "openAMWindow"
        },
        data: {
            localChecked: true
        },
        model: {},
        partials: [
            "partials/_alert.html"
        ],

        render: function (configs, callback) {
            var allModules = _.get(this.getAuthenticationData(), "authModules"),
                amModuleIndex = this.getAMModuleIndex(allModules);

            // If there is an OPENAM module and it is enabled then local is not checked
            this.data.localChecked = !(amModuleIndex !== -1 && allModules[amModuleIndex].enabled);

            SocialDelegate.providerList().then((availableProviders) => {
                this.data.providerList = OAuthUtils.setDisplayIcons(availableProviders.providers);
                this.parentRender(_.bind(function() {
                    if (callback) {
                        callback();
                    }
                }, this));
            });
        },

        /**
         * Called when the radio button changes value.
         *
         * @param e
         */
        providerChanged: function(e) {
            this.selectProvider(this.$el.find("input[name=providerSelection]:checked").val());
        },

        /**
         * Given a name of a provider this will set the disable state and make any configuration changes necessary to align with the selection.
         *
         * @param selectedProvider {string} - "am" or "local"
         */
        selectProvider: function(selectedProvider, skipDataChanges) {
            if (selectedProvider !== "am" && selectedProvider !== "local") {
                selectedProvider = "local";
            }

            var selectedRadio = this.$el.find(".providerSelection[value=" + selectedProvider + "]"),
                selectedPanel = selectedRadio.closest(".provider-panel"),
                allPanel = this.$el.find(".provider-panel");

            selectedPanel.removeClass("disabled");
            allPanel.not(selectedPanel).addClass("disabled");
            selectedRadio.prop("checked", true);

            if (!skipDataChanges) {
                switch (selectedProvider) {
                    case "am":
                        this.openAMWindow();
                        break;

                    case "local":
                    default:
                        this.removeLogoutURL();
                        this.setProperties(["authModules", "sessionModule"], this.getLocalAuthConfig(this.getAuthenticationData()));
                        this.saveAuthentication();
                        break;
                }
            }
        },

        getLocalAuthConfig: function(authData) {
            var  allAuthModules = _.get(authData, "authModules");

            // Set cache period
            _.set(authData, "sessionModule.properties.maxTokenLifeMinutes", "120");
            _.set(authData, "sessionModule.properties.tokenIdleTimeMinutes", "30");
            delete authData.sessionModule.properties.maxTokenLifeSeconds;
            delete authData.sessionModule.properties.tokenIdleTimeSeconds;

            // Enable all modules
            _.each(allAuthModules, function(module) {
                module.enabled = true;
            });

            // If the OPENAM module exists, disable it.
            let openAMModuleIndex = this.getAMModuleIndex(allAuthModules);
            if (openAMModuleIndex !== -1) {
                allAuthModules[openAMModuleIndex].enabled = false;
            }

            return authData;
        },

        openAMWindow: function (e) {
            var allModules = _.get(this.getAuthenticationData(), "authModules"),
                existingAMConfig = allModules[this.getAMModuleIndex(allModules)];

            ProvidersModuleDialogView.render({
                "config": existingAMConfig || {},
                "cancelCallback": () => {
                    var allModules = _.get(this.getAuthenticationData(), "authModules"),
                        amModuleIndex = this.getAMModuleIndex(allModules);

                    // If there isn't an enabled OPENAM module then revert to local
                    if (! (amModuleIndex !== -1 && allModules[amModuleIndex].enabled)) {
                        this.selectProvider("local", true);
                    }
                }
            }, _.noop);
        }

    });

    return new ProvidersView();
});
