/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

/*global define, $, _, form2js */

define("org/forgerock/openidm/ui/admin/settings/EmailConfigView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"

], function(AdminAbstractView,
            eventManager,
            constants,
            ConfigDelegate,
            validatorsManager) {

    var EmailConfigView = AdminAbstractView.extend({
        template: "templates/admin/settings/EmailConfigTemplate.html",
        element: "#emailConfigContainer",
        noBaseTemplate: true,
        events: {
            "onValidate": "onValidate",
            "customValidate": "customValidate",
            "click #emailAuth": "toggleUserPass",
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
                        validatorsManager.bindValidators(this.$el.find("#emailConfigForm"));
                        validatorsManager.validateAllFields(this.$el.find("#emailConfigForm"));
                    }, this));
                }, this),
                _.bind(function() {
                    this.parentRender(_.bind(function() {
                        validatorsManager.bindValidators(this.$el.find("#emailConfigForm"));
                        validatorsManager.validateAllFields(this.$el.find("#emailConfigForm"));
                    }, this));
                }, this)

            );
        },

        toggleUserPass: function(e) {
            this.$el.find("#smtpauth").slideToggle($(e.currentTarget).prop("checked"));
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

            if (this.model.externalEmailExists) {
                ConfigDelegate.updateEntity("external.email", this.data.config).then(_.bind(function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "emailConfigSaveSuccess");
                }, this));
            } else {
                ConfigDelegate.createEntity("external.email", this.data.config).then(_.bind(function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "emailConfigSaveSuccess");
                    this.model.externalEmailExists = true;
                }, this));

            }
        },

        customValidate: function() {
            this.validationResult = validatorsManager.formValidated(this.$el.find("#emailConfigForm"));
            this.$el.find("#saveEmailConfig").prop('disabled', !this.validationResult);
        }
    });

    return new EmailConfigView();
});
