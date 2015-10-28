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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/settings/authentication/SessionModuleView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationAbstractView",
    "org/forgerock/commons/ui/common/components/ChangesPending"

], function($, _,
            AuthenticationAbstractView,
            ChangesPending) {

    var SessionModuleView = AuthenticationAbstractView.extend({
        template: "templates/admin/settings/authentication/SessionModuleTemplate.html",
        element: "#sessionModuleView",
        noBaseTemplate: true,
        events: {
            "change .changes-watched": "checkChanges",
            "keyup .changes-watched": "checkChanges"
        },
        data: {
            "properties": {
                "maxTokenLife": "120",
                "tokenIdleTime": "30",
                "sessionOnly": "false"
            }
        },
        model: {},

        /**
         * @param configs {object}
         * @param configs.hasAM {boolean}
         * @param [callback]
         */
        render: function (configs, callback) {
            this.model = _.extend(
                {
                    "amTokenTime": "5",
                    "amTokenMinutes": false,
                    "defaults": {
                        "maxTokenLife": "120",
                        "tokenIdleTime": "30",
                        "maxTokenLifeMinutes": true,
                        "tokenIdleTimeMinutes": true
                    }
                },
                configs,
                this.getAuthenticationData()
            );

            if (this.model.changes) {
                this.data.sessionModule = this.model.changes;

                // So we don't overwrite our model we only use the changes object once unless reset by undo
                delete this.model.changes;

            } else {
                this.data.sessionModule = _.clone(this.model.sessionModule);
            }

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

            // If there is an OPENAM module and the configured token settings are the defaults, them to the OPENAM session defaults
            if (this.model.hasAM &&
                this.data.maxTokenLife === this.model.defaults.maxTokenLife && this.data.maxTokenLifeMinutes === this.model.defaults.maxTokenLifeMinutes &&
                this.data.tokenIdleTime === this.model.defaults.tokenIdleTime && this.data.tokenIdleTimeMinutes === this.model.defaults.tokenIdleTimeMinutes) {

                this.data.sessionModule.properties.maxTokenLifeSeconds = this.model.amTokenTime;
                this.data.sessionModule.properties.tokenIdleTimeSeconds = this.model.amTokenTime;

                this.data.maxTokenLife = this.model.amTokenTime;
                this.data.maxTokenLifeMinutes = this.model.amTokenMinutes;
                this.data.tokenIdleTime = this.model.amTokenTime;
                this.data.tokenIdleTimeMinutes = this.model.amTokenMinutes;

                // If the use wants to, changes these settings they can so we remove this flag so its only set when an OPENAM_SESSION auth module is created
                delete this.model.hasAM;

            } else if (this.model.hasAM) {
                delete this.model.hasAM;
            }

            this.parentRender(_.bind(function() {
                // Watch for changes
                if (!_.has(this.model, "changesModule")) {
                    this.model.changesModule = ChangesPending.watchChanges({
                        element: this.$el.find(".authentication-session-changes"),
                        undo: true,
                        watchedObj: this.model.sessionModule,
                        undoCallback: _.bind(function (original) {
                            this.reRender({changes: original});
                            this.checkChanges();
                        }, this)
                    });
                } else {
                    this.model.changesModule.reRender(this.$el.find(".authentication-session-changes"));
                }

                this.checkChanges();

                if (callback) {
                    callback();
                }
            }, this));
        },

        checkChanges: function(e) {

            _.each(["maxTokenLifeSeconds", "maxTokenLifeMinutes", "tokenIdleTimeSeconds", "tokenIdleTimeMinutes"], _.bind(function(prop) {
                if (_.has(this.data.sessionModule.properties, prop)) {
                    delete this.data.sessionModule.properties[prop];
                }
            }, this));

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

            this.setProperties(["sessionModule"], this.data);
            this.model.changesModule.makeChanges(this.data.sessionModule, true);
        },

        reRender: function(options) {
            this.render(_.extend(this.model, options));
        },

        addedOpenAM: function() {
            this.reRender({"hasAM": true, "changes": this.data.sessionModule});
        }
    });

    return new SessionModuleView();
});
