"use strict";

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
 * Copyright 2011-2016 ForgeRock AS.
 */

define([], function () {
    //definitions for views here are generic
    //the actual path to each view is defined in config/AppConfiguration.js
    //view files are mapped aliases registered within requirejs
    var obj = {
        "profile": {
            view: "UserProfileView",
            role: "ui-self-service-user",
            url: /^profile\/(.*)/,
            pattern: "profile/?",
            defaults: ["details"],
            navGroup: "user"
        },
        "forgotUsername": {
            view: "ForgotUsernameView",
            url: /^forgotUsername(\/[^\&]*)(\&.+)?/,
            pattern: "forgotUsername??",
            argumentNames: ["realm", "additionalParameters"],
            defaults: ["/", ""]
        },
        "passwordReset": {
            view: "PasswordResetView",
            url: /^passwordReset(\/[^\&]*)(\&.+)?/,
            pattern: "passwordReset??",
            argumentNames: ["realm", "additionalParameters"],
            defaults: ["/", ""]
        },
        "selfRegistration": {
            view: "RegisterView",
            url: /^register(\/[^\&]*)(\&.+)?/,
            pattern: "register??",
            argumentNames: ["realm", "additionalParameters"],
            defaults: ["/", ""]
        }
    };

    return obj;
});
