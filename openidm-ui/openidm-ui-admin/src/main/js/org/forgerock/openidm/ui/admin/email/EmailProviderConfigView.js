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
 * Copyright 2015-2017 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "form2js",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"
], function($, _, form2js,
            AdminAbstractView,
            eventManager,
            constants,
            ConfigDelegate,
            validatorsManager) {

    var EmailConfigView = AdminAbstractView.extend({
        template: "templates/admin/email/EmailProviderConfigTemplate.html",
        element: "#emailContainer",
        noBaseTemplate: true,
        events: {
            "click #emailAuth": "toggleUserPass",
            "change #emailToggle": "toggleEmail",
            "change #emailAuthPassword": "updatePassword",
            "click #saveEmailConfig": "save"
        },
        model: {
            externalEmailExists: false
        },
        data: {
            "password": null,
            "config": {
                "host": "",
                "port": "",
                "auth": {
                    "enable": false,
                    "username": "",
                    "password": ""
                },
                "starttls": {
                    "enable": false
                },
                "from": ""
            }
        },

        render: function (args, callback) {
            this.data.docHelpUrl = constants.DOC_URL;

            ConfigDelegate.readEntity("external.email").then(
                _.bind(function(data) {
                    _.extend(this.data.config, data);

                    this.data.password = this.findPassword(data);

                    this.model.externalEmailExists = true;

                    this.setup(callback);
                }, this),
                _.bind(function() {
                    this.setup(callback);
                }, this)

            );
        },

        /**
         * @param emailConfig - Object of the current email config
         * @returns {*} - Found password crypto if it exists
         */
        findPassword : function(emailConfig) {
            var foundPassword = null;

            if (_.has(emailConfig, "auth") && _.has(emailConfig.auth, "password")) {
                foundPassword = emailConfig.auth.password;
            }

            return foundPassword;
        },

        setup: function(callback) {
            this.parentRender(_.bind(function() {
                if (_.isEmpty(this.data.config) || !this.data.config.host) {
                    this.$el.find("#emailToggle").prop("checked", false);
                    this.$el.find("#emailSettingsForm").hide();
                } else {
                    this.$el.find("#emailToggle").prop("checked", true);
                    this.toggleEmail();
                }

                if (callback) {
                    callback();
                }
            }, this));
        },

        toggleUserPass: function(e) {
            this.$el.find("#smtpauth").slideToggle($(e.currentTarget).prop("checked"));
        },

        toggleEmail: function() {
            if (!this.$el.find("#emailToggle").is(":checked")) {
                if (this.$el.find("#smtpauth").is(":visible")) {
                    this.$el.find("#smtpauth").slideToggle();
                }
                this.$el.find("fieldset").find("input:checkbox").prop("checked", false);
                this.$el.find("fieldset").prop("disabled", true);
                this.$el.find("#emailSettingsForm").hide();

                validatorsManager.clearValidators(this.$el.find("#emailConfigForm"));
                this.$el.find("#saveEmailConfig").prop('disabled', false);

            } else {
                this.$el.find("fieldset").prop("disabled", false);
                this.$el.find("#emailSettingsForm").show();

                validatorsManager.bindValidators(this.$el.find("#emailConfigForm"));
                validatorsManager.validateAllFields(this.$el.find("#emailConfigForm"));
            }
        },

        updatePassword: function(e) {
            this.data.password = $(e.currentTarget).val();
        },

        /**
         *
         * @param saveConfig - Object of the current email configuration
         * @param formData - Object of the current HTML form details inputted by the user
         * @param password - The current password if available
         * @returns {*} - Returns an updated email configuration object used for saving
         */
        cleanSaveData: function(saveConfig, formData, password) {
            if (!_.has(formData, "starttls") || !_.has(formData.starttls, "enable")) {
                delete saveConfig.starttls;
            }

            if (_.has(formData, "auth")){
                if (password) {
                    saveConfig.auth.password = password;
                }

                if (!_.has(formData.auth, "enable")) {
                    delete saveConfig.auth;
                }
            }

            return saveConfig;
        },

        save: function(e) {
            e.preventDefault();
            var formData = form2js("emailConfigForm",".", false);

            _.extend(this.data.config, formData);

            if (!this.$el.find("#emailToggle").is(":checked")) {
                this.data.config = {};
            }

            this.data.config = this.cleanSaveData(this.data.config, formData, this.data.password);

            if (this.model.externalEmailExists) {
                ConfigDelegate.updateEntity("external.email", this.data.config).then(_.bind(function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "emailConfigSaveSuccess");
                    this.render();
                }, this));
            } else {
                ConfigDelegate.createEntity("external.email", this.data.config).then(_.bind(function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "emailConfigSaveSuccess");
                    this.model.externalEmailExists = true;
                }, this));

            }
        }
    });

    return new EmailConfigView();
});
