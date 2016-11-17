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
    "org/forgerock/openidm/ui/admin/selfservice/AbstractSelfServiceView"
], function($, _, AbstractSelfServiceView) {
    var PasswordResetConfigView = AbstractSelfServiceView.extend({
        template: "templates/admin/selfservice/PasswordResetConfigTemplate.html",
        partials: AbstractSelfServiceView.prototype.partials.concat([
            "partials/selfservice/_userQuery.html",
            "partials/selfservice/_resetStage.html",
            "partials/selfservice/_captcha.html",
            "partials/selfservice/_emailValidation.html"
        ]),
        model: {
            surpressSave: false,
            uiConfigurationParameter: "passwordReset",
            serviceType: "password",
            configUrl: "selfservice/reset",
            msgType: "selfServicePassword",
            "configDefault": {
                "stageConfigs": [
                    {
                        "name" : "captcha",
                        "recaptchaSiteKey": "",
                        "recaptchaSecretKey": "",
                        "recaptchaUri" : "https://www.google.com/recaptcha/api/siteverify"
                    },
                    {
                        "name" : "userQuery",
                        "validQueryFields" : [
                            "userName",
                            "mail",
                            "givenName",
                            "sn"
                        ],
                        "identityIdField" : "_id",
                        "identityEmailField" : "mail",
                        "identityUsernameField" : "userName",
                        "identityServiceUrl" : "managed/user"
                    },
                    {
                        "name" : "emailValidation",
                        "identityEmailField" : "mail",
                        "emailServiceUrl": "external/email",
                        "from" : "info@admin.org",
                        "subject" : "Reset password email",
                        "mimeType" : "text/html",
                        "subjectTranslations": {
                            "en": "Reset your password",
                            "fr": "Réinitialisez votre mot de passe"
                        },
                        "messageTranslations": {
                            "en": "<h3>Click to reset your password</h3><h4><a href=\"%link%\">Password reset link</a></h4>",
                            "fr": "<h3>Cliquez pour réinitialiser votre mot de passe</h3><h4><a href=\"%link%\">Mot de passe lien de réinitialisation</a></h4>"
                        },
                        "verificationLinkToken" : "%link%",
                        "verificationLink" : "https://localhost:8443/#passwordReset/"
                    },
                    {
                        "name" : "kbaSecurityAnswerVerificationStage",
                        "kbaPropertyName" : "kbaInfo",
                        "identityServiceUrl" : "managed/user",
                        "numberOfQuestionsUserMustAnswer" : "1",
                        "kbaConfig" : null
                    },
                    {
                        "name" : "resetStage",
                        "identityServiceUrl" : "managed/user",
                        "identityPasswordField" : "password"
                    }
                ],
                "snapshotToken" : {
                    "type": "jwt",
                    "jweAlgorithm" : "RSAES_PKCS1_V1_5",
                    "encryptionMethod" : "A128CBC_HS256",
                    "jwsAlgorithm" : "HS256",
                    "tokenExpiry": 1800
                },
                "storage": "stateless"
            },
            "saveConfig": {},
            identityServiceURLSaveLocations: [
                {
                    "stepName": "userQuery",
                    "stepProperty": "identityServiceUrl"
                }, {
                    "stepName": "resetStage",
                    "stepProperty": "identityServiceUrl"
                }, {
                    "stepName": "kbaSecurityAnswerVerificationStage",
                    "stepProperty": "identityServiceUrl"
                }
            ]
        },
        render: function(args, callback) {
            this.data.configList = [{
                type: "captcha",
                title: $.t("templates.selfservice.password.captchaTitle"),
                help: $.t("templates.selfservice.captcha.description"),
                editable: true,
                enabledByDefault: false
            }, {
                type: "userQuery",
                title: $.t("templates.selfservice.userQuery.name"),
                help: $.t("templates.selfservice.userQuery.description"),
                editable: true,
                icon: "user",
                enabledByDefault: true
            }, {
                type: "emailValidation",
                title: $.t("templates.selfservice.emailValidation"),
                help: $.t("templates.selfservice.emailValidationDescription"),
                editable: true,
                enabledByDefault: true
            }, {
                type: "kbaSecurityAnswerVerificationStage",
                title: $.t("templates.selfservice.kbaSecurityAnswerVerificationStageForm"),
                help: $.t("templates.selfservice.kbaSecurityAnswerVerificationStageFormDescription"),
                editable: false,
                enabledByDefault: true
            }, {
                type: "resetStage",
                title: $.t("templates.selfservice.passwordResetForm"),
                help: $.t("templates.selfservice.passwordResetFormDescription"),
                editable: true,
                enabledByDefault: true,
                icon: "user"
            }];

            this.selfServiceRender(args, callback);
        },
        setKBAVerificationEnabled: function () {
            this.model.uiConfig.configuration.kbaVerificationEnabled =
                !!_.find(this.model.saveConfig.stageConfigs, function (stage) {
                    return stage.name === "kbaSecurityAnswerVerificationStage";
                });
        },
        createConfig: function () {
            this.setKBAVerificationEnabled();
            return AbstractSelfServiceView.prototype.createConfig.call(this);
        },
        deleteConfig: function () {
            this.setKBAVerificationEnabled();
            return AbstractSelfServiceView.prototype.deleteConfig.call(this);
        },
        saveConfig: function () {
            this.setKBAVerificationEnabled();
            return AbstractSelfServiceView.prototype.saveConfig.call(this);
        },
        filterPropertiesList: function (properties, type, details) {
            var cleanedList = [];

            if(type === "resetStage") {
                _.each(properties, (prop, index) => {
                    if(_.has(details[prop], "encryption") || _.has(details[prop], "hashed")) {
                        cleanedList.push(prop);
                    }
                });
            }

            return cleanedList;
        }
    });

    return new PasswordResetConfigView();
});
