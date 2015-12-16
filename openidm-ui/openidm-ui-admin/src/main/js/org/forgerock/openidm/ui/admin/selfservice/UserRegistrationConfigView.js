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

define("org/forgerock/openidm/ui/admin/selfservice/UserRegistrationConfigView", [
    "jquery",
    "lodash",
    "org/forgerock/openidm/ui/admin/selfservice/AbstractSelfServiceView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function($, _, AbstractSelfServiceView, ConfigDelegate) {
    var UserRegistrationConfigView = AbstractSelfServiceView.extend({
        template: "templates/admin/selfservice/UserRegistrationConfigTemplate.html",
        partials: AbstractSelfServiceView.prototype.partials.concat([
            "partials/selfservice/_emailValidation.html",
            "partials/selfservice/_kbaStage.html",
            "partials/selfservice/_selfRegistration.html",
            "partials/selfservice/_userDetails.html",
            "partials/selfservice/_captcha.html"
        ]),
        model: {
            surpressSave: false,
            uiConfigurationParameter: "selfRegistration",
            serviceType: "user",
            configUrl: "selfservice/registration",
            msgType: "selfServiceUserRegistration",
            "configDefault": {
                "stageConfigs" : [
                    {
                        "name" : "captcha",
                        "recaptchaSiteKey": "",
                        "recaptchaSecretKey": "",
                        "recaptchaUri" : "https://www.google.com/recaptcha/api/siteverify"
                    },
                    {
                        "name" : "emailValidation",
                        "identityEmailField" : "mail",
                        "emailServiceUrl": "external/email",
                        "from" : "info@admin.org",
                        "subject" : "Register new account",
                        "mimeType" : "text/html",
                        "subjectTranslations" : {
                            "en" : "Register new account",
                            "fr" : "Créer un nouveau compte"
                        },
                        "messageTranslations" : {
                            "en" : "<h3>This is your registration email.</h3><h4><a href=\"%link%\">Email verification link</a></h4>",
                            "fr" : "<h3>Ceci est votre mail d'inscription.</h3><h4><a href=\"%link%\">Lien de vérification email</a></h4>"
                        },
                        "verificationLinkToken" : "%link%",
                        "verificationLink" : "https://localhost:8443/#register/"
                    },
                    {
                        "name" : "userDetails",
                        "identityEmailField" : "mail"
                    },
                    {
                        "name" : "kbaSecurityAnswerDefinitionStage",
                        "numberOfAnswersUserMustSet": 1,
                        "kbaConfig" : null
                    },
                    {
                        "name" : "selfRegistration",
                        "identityServiceUrl" : "managed/user"
                    }
                ],
                "snapshotToken" : {
                    "type" : "jwt",
                    "keyPairAlgorithm" : "RSA",
                    "keyPairSize" : 1024,
                    "jweAlgorithm" : "RSAES_PKCS1_V1_5",
                    "encryptionMethod" : "A128CBC_HS256",
                    "jwsAlgorithm" : "HS256",
                    "tokenExpiry" : 1800
                },
                "storage" : "stateless"
            },
            "saveConfig": {}
        },
        render: function(args, callback) {
            this.data.configList = [{
                type: "captcha",
                title: $.t("templates.selfservice.user.captchaTitle"),
                help: $.t("templates.selfservice.captcha.description"),
                editable: true,
                enabledByDefault: false
            },
            {
                type: "emailValidation",
                title: $.t("templates.selfservice.emailValidation"),
                help: $.t("templates.selfservice.emailValidationDescription"),
                editable: true,
                enabledByDefault: true
            },
            {
                type: "userDetails",
                title: $.t("templates.selfservice.userDetailsTitle"),
                help: $.t("templates.selfservice.userDetailsHelp"),
                editable: true,
                enabledByDefault: true
            },
            {
                type: "kbaSecurityAnswerDefinitionStage",
                title: $.t("templates.selfservice.kbaSecurityAnswerDefinitionStageTitle"),
                help: $.t("templates.selfservice.kbaSecurityAnswerDefinitionStageHelp"),
                editable: false,
                enabledByDefault: true
            },
            {
                type: "selfRegistration",
                title: $.t("templates.selfservice.registrationForm"),
                help: $.t("templates.selfservice.registrationFormDescription"),
                editable: true,
                enabledByDefault: true
            }];

            this.selfServiceRender(args, callback);
        },
        setKBAEnabled: function () {
            var kbaEnabled = !!_.find(this.model.saveConfig.stageConfigs, function (stage) {
                return stage.name === "kbaSecurityAnswerDefinitionStage";
            });
            if (kbaEnabled !== this.model.uiConfig.configuration.kbaEnabled) {
                this.model.uiConfig.configuration.kbaEnabled = kbaEnabled;
            }
        },
        createConfig: function () {
            this.setKBAEnabled();
            return AbstractSelfServiceView.prototype.createConfig.call(this);
        },
        deleteConfig: function () {
            this.setKBAEnabled();
            return AbstractSelfServiceView.prototype.deleteConfig.call(this);
        },
        saveConfig: function () {
            return AbstractSelfServiceView.prototype.saveConfig.call(this).then(_.bind(function () {
                var kbaCurrentlyEnabled = this.model.uiConfig.configuration.kbaEnabled;
                this.setKBAEnabled();
                if (kbaCurrentlyEnabled !== this.model.uiConfig.configuration.kbaEnabled) {
                    return ConfigDelegate.updateEntity("ui/configuration", this.model.uiConfig);
                }
            }, this));
        }
    });

    return new UserRegistrationConfigView();
});
