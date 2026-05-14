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

    var obj = {
        "usernameNotFound": {
            msg: "config.messages.UserMessages.usernameNotFound",
            type: "error"
        },
        "noEmailProvided": {
            msg: "config.messages.UserMessages.noEmailProvided",
            type: "error"
        },
        "emailNotSent": {
            msg: "config.messages.UserMessages.emailNotSent",
            type: "error"
        },
        "unableToRegister": {
            msg: "config.messages.UserMessages.unableToRegister",
            type: "error"
        },
        "invalidOldPassword": {
            msg: "config.messages.UserMessages.invalidOldPassword",
            type: "error"
        },
        "changedPassword": {
            msg: "config.messages.UserMessages.changedPassword",
            type: "info"
        },
        "userProfileIncorrectPassword": {
            msg: "config.messages.UserMessages.userProfileIncorrectPassword",
            type: "error"
        },
        "profileUpdateSuccessful": {
            msg: "config.messages.UserMessages.profileUpdateSuccessful",
            type: "info"
        },
        "userNameUpdated": {
            msg: "config.messages.UserMessages.userNameUpdated",
            type: "info"
        },
        "afterRegistration": {
            msg: "config.messages.UserMessages.afterRegistration",
            type: "info"
        },
        "userAlreadyExists": {
            msg: "config.messages.UserMessages.userAlreadyExists",
            type: "error"
        },
        "errorDeletingNotification": {
            msg: "config.messages.UserMessages.errorDeletingNotification",
            type: "error"
        },
        "errorFetchingNotifications": {
            msg: "config.messages.UserMessages.errorFetchingNotifications",
            type: "error"
        },
        "identityNoSpace": {
            msg: "config.messages.UserMessages.identityNoSpace",
            type: "error"
        },
        "selfRegistrationDisabled": {
            msg: "config.messages.UserMessages.selfRegistrationDisabled",
            type: "error"
        }
    };

    return obj;
});
