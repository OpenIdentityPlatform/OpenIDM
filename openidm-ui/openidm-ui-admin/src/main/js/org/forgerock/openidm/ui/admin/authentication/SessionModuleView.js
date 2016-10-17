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
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/authentication/AuthenticationAbstractView"

], function($, _, Constants, ConfigDelegate, AuthenticationAbstractView) {

    var SessionModuleView = AuthenticationAbstractView.extend({
        template: "templates/admin/authentication/SessionModuleTemplate.html",
        element: "#sessionContainer",
        noBaseTemplate: true,
        events: {
            "change .changes-watched": "checkChanges",
            "keyup .changes-watched": "checkChanges",
            "click #reset": "reset",
            "click #save": "save"
        },
        data: {
            "properties": {
                "maxTokenLife": "120",
                "tokenIdleTime": "30",
                "sessionOnly": "false"
            },
            "docHelpUrl": Constants.DOC_URL
        },
        model: {},

        /**
         * @param [callback]
         */
        render: function (callback) {
            this.model = this.getAuthenticationData();
            this.data.sessionModule = _.clone(this.model.sessionModule, true);
            this.data.sessionModuleClean = _.clone(this.model.sessionModule, true);

            ConfigDelegate.readEntity("ui/configuration").then((uiConfig) => {
                this.data.logoutURLClean = this.data.logoutURL = _.get(uiConfig, "configuration.logoutUrl");

                if (_.has(this.data.sessionModule.properties, "maxTokenLifeSeconds")) {
                    this.data.maxTokenLife = this.data.sessionModule.properties.maxTokenLifeSeconds;
                    this.data.maxTokenLifeMinutes = false;

                } else if (_.has(this.data.sessionModule.properties, "maxTokenLifeMinutes")) {
                    this.data.maxTokenLife = this.data.sessionModule.properties.maxTokenLifeMinutes;
                    this.data.maxTokenLifeMinutes = true;
                }

                if (_.has(this.data.sessionModule.properties, "tokenIdleTimeSeconds")) {
                    this.data.tokenIdleTime = this.data.sessionModule.properties.tokenIdleTimeSeconds;
                    this.data.tokenIdleTimeMinutes = false;

                } else if (_.has(this.data.sessionModule.properties, "tokenIdleTimeMinutes")) {
                    this.data.tokenIdleTime = this.data.sessionModule.properties.tokenIdleTimeMinutes;
                    this.data.tokenIdleTimeMinutes = true;
                }

                this.parentRender(_.bind(function() {

                    this.$el.find("#maxTokenLifeUnits").selectize();
                    this.$el.find("#tokenIdleTimeUnits").selectize();

                    if (callback) {
                        callback();
                    }
                }, this));
            });
        },

        checkChanges: function(e) {
            _.each(["maxTokenLifeSeconds", "maxTokenLifeMinutes", "tokenIdleTimeSeconds", "tokenIdleTimeMinutes"], _.bind(function(prop) {
                if (_.has(this.data.sessionModule.properties, prop)) {
                    delete this.data.sessionModule.properties[prop];
                }
            }, this));

            this.data.logoutURL = this.$el.find("#logoutURL").val();
            this.data.sessionModule.properties.sessionOnly = this.$el.find("#sessionOnly").is(":checked");

            if (this.$el.find("#maxTokenLifeUnits").val() === "seconds") {
                this.data.sessionModule.properties.maxTokenLifeSeconds = this.$el.find("#maxTokenLife").val();
            } else {
                this.data.sessionModule.properties.maxTokenLifeMinutes = this.$el.find("#maxTokenLife").val();
            }

            if (this.$el.find("#tokenIdleTimeUnits").val() === "seconds") {
                this.data.sessionModule.properties.tokenIdleTimeSeconds = this.$el.find("#tokenIdleTime").val();
            } else {
                this.data.sessionModule.properties.tokenIdleTimeMinutes = this.$el.find("#tokenIdleTime").val();
            }

            let sessionEqual = _.isEqual(this.data.sessionModuleClean, this.data.sessionModule);
            let logoutURLEqual = _.isEqual(this.data.logoutURLClean, this.data.logoutURL);

            this.toggleButtons(sessionEqual && logoutURLEqual);
        },

        toggleButtons: function(state) {
            this.$el.find("#save, #reset").toggleClass("disabled", state);
        },

        reset: function(e) {
            e.preventDefault();
            if ($(e.currentTarget).hasClass("disabled")) {
                return false;
            }
            this.render();
        },

        save: function(e) {
            e.preventDefault();

            if ($(e.currentTarget).hasClass("disabled")) {
                return false;
            }

            this.saveLogoutURL(this.data.logoutURL).then(() => {
                this.setProperties(["sessionModule"], {"sessionModule": this.data.sessionModule});
                this.saveAuthentication().then(() => {
                    this.render();
                });

            });
        }
    });

    return new SessionModuleView();
});
