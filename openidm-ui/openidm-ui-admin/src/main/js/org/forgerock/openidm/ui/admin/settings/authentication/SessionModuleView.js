/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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
                "maxTokenLifeMinutes": "",
                "tokenIdleTimeMinutes": "",
                "sessionOnly": ""
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
                    "defaultTokenTime": "120",
                    "defaultTokenIdleTime": "30"
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

            // If there is an OPENAM module and the configured token settings are the defaults, them to the OPENAM session defaults
            if (this.model.hasAM &&
                this.data.sessionModule.properties.maxTokenLifeMinutes === this.model.defaultTokenTime &&
                this.data.sessionModule.properties.tokenIdleTimeMinutes === this.model.defaultTokenIdleTime) {

                this.data.sessionModule.properties.maxTokenLifeMinutes = this.model.amTokenTime;
                this.data.sessionModule.properties.tokenIdleTimeMinutes = this.model.amTokenTime;

                // If the use wants to, changes these settings they can so we remove this flag so its only set when an OPENAM_SESSION auth module is created
                delete this.model.hasAM;

                this.checkChanges();

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

                if (callback) {
                    callback();
                }
            }, this));
        },

        checkChanges: function(e) {
            if (!_.isUndefined(e) && e.currentTarget.type === "checkbox") {
                this.data.sessionModule.properties[e.currentTarget.name] = e.currentTarget.checked;
            } else if (!_.isUndefined(e)) {
                this.data.sessionModule.properties[e.currentTarget.name] = $(e.currentTarget).val();
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
