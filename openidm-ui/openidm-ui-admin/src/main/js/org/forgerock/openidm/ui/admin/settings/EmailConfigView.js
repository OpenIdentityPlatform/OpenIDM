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

define("org/forgerock/openidm/ui/admin/settings/EmailConfigView", [
    "jquery",
    "underscore",
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
        template: "templates/admin/settings/EmailConfigTemplate.html",
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

                    if (_.has(this.data.config, "auth") && _.has(this.data.config.auth, "password")) {
                        this.data.password = this.data.config.auth.password;
                    }

                    this.model.externalEmailExists = true;

                    this.parentRender(_.bind(function() {
                        this.setup(callback);
                    }, this));
                }, this),
                _.bind(function() {
                    this.setup(callback);
                }, this)

            );
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

        save: function() {
            var formData = form2js("emailConfigForm",".", true);

            _.extend(this.data.config, formData);

            if (_.has(formData, "starttls") && _.has(formData.starttls, "enable")) {
                this.data.config.starttls.enable = true;
            } else {
                delete this.data.config.starttls;
            }

            if (_.has(formData, "auth")){
                if (this.data.password) {
                    this.data.config.auth.password = this.data.password;
                }

                if (_.has(formData.auth, "enable")) {
                    this.data.config.auth.enable = true;
                } else {
                    delete this.data.config.auth;
                }
            }

            if (!this.$el.find("#emailToggle").is(":checked")) {
                this.data.config = {};
            }

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
