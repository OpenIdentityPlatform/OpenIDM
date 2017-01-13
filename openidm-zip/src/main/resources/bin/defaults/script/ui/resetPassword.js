/*
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
 * Copyright 2016-2017 ForgeRock AS.
 */

/*global require, openidm, exports */

var Crypto = require("crypto"),
    Handlebars = require("lib/handlebars");

exports.sendMail = function (object) {

    // if there is a configuration found, assume that it has been properly configured
    var emailConfig = openidm.read("config/external.email"),
        emailTemplate = openidm.read("config/emailTemplate/resetPassword");

    if (emailConfig && emailConfig.host && emailTemplate && emailTemplate.enabled) {
        var email,
            password,
            template,
            locale = emailTemplate.defaultLocale;

        password = Crypto.generateRandomString(emailTemplate.passwordRules, emailTemplate.passwordLength);

        openidm.patch(resourcePath, object._rev, [{
            operation: "add",
            field: "/password",
            value: password
        }]);

        email =  {
            "from": emailConfig.from,
            "to": object.mail,
            "subject": emailTemplate.subject[locale],
            "type": "text/html"
        };

        template = Handlebars.compile(emailTemplate.message[locale]);

        email.body = template({
            "password": password,
            "object": object
        });

        try {
            openidm.action("external/email", "sendEmail", email);
        } catch (e) {
            logger.info("There was an error with the outbound email service configuration.  The password was reset but the user hasn't been notified.");
            throw {"code": 400}
        }
    } else {
        logger.info("Email service not configured; password not reset. ");
    }
};
