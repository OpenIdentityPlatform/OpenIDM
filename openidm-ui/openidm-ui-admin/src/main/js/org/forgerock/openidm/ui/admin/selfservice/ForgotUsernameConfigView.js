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
    "org/forgerock/openidm/ui/admin/selfservice/AbstractSelfServiceView"
], function($, AbstractSelfServiceView) {
    var ForgotUsernameConfigView = AbstractSelfServiceView.extend({
        template: "templates/admin/selfservice/ForgotUsernameConfigTemplate.html",
        partials: AbstractSelfServiceView.prototype.partials.concat([
            "partials/selfservice/_userQuery.html",
            "partials/selfservice/_captcha.html",
            "partials/selfservice/_emailUsername.html",
            "partials/selfservice/_retrieveUsername.html"
        ]),
        model: {
            surpressSave: false,
            uiConfigurationParameter: "forgotUsername",
            serviceType: "username",
            configUrl: "selfservice/username",
            msgType: "selfServiceUsername",
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
                        "name" : "emailUsername",
                        "emailServiceUrl": "external/email",
                        "from": "info@admin.org",
                        "mimeType": "text/html",
                        "subjectTranslations": {
                            "en": "Account Information - username"
                        },
                        "messageTranslations": {
                            "en": "<h3>Username is:</h3><br />%username%"
                        },
                        "usernameToken": "%username%"
                    },
                    {
                        "name" : "retrieveUsername"
                    }
                ],
                "snapshotToken" : {
                    "type": "jwt",
                    "keyPairAlgorithm" : "RSA",
                    "keyPairSize" : 1024,
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
                }
            ]
        },
        render: function(args, callback) {
            this.data.configList = [{
                type: "captcha",
                title: $.t("templates.selfservice.username.captchaTitle"),
                help: $.t("templates.selfservice.captcha.description"),
                editable: true,
                enabledByDefault: false
            }, {
                type: "userQuery",
                title: $.t("templates.selfservice.userQuery.name"),
                help: $.t("templates.selfservice.userQuery.description"),
                editable: true,
                enabledByDefault: true,
                icon: "user"
            }, {
                type: "emailUsername",
                title: $.t("templates.selfservice.emailUsername.name"),
                help: $.t("templates.selfservice.emailUsername.description"),
                editable: true,
                enabledByDefault: true
            }, {
                type: "retrieveUsername",
                title: $.t("templates.selfservice.retrieveUsername.name"),
                help: $.t("templates.selfservice.retrieveUsername.description"),
                editable: false,
                enabledByDefault: true
            }];

            this.selfServiceRender(args, callback);
        }
    });

    return new ForgotUsernameConfigView();
});
