/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openidm/ui/common/delegates/AuthnDelegate", [
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate"
], function(_, constants, AbstractDelegate) {

    var obj = new AbstractDelegate(constants.host + "/openidm/info/login");

    /**
     * Starting session. Sending username and password to authenticate and returns user's id.
     */
    obj.login = function(uid, password, errorsHandlers, successCallback, errorCallback) {
        var headers = {};

        headers[constants.HEADER_PARAM_USERNAME] = uid;
        headers[constants.HEADER_PARAM_PASSWORD] = password;
        headers[constants.HEADER_PARAM_NO_SESSION] = false;

        return obj.getProfile(errorsHandlers, headers)
            .then(successCallback, errorCallback);
    };

    obj.getUserById = function(id, component, errorsHandlers) {
        return this.serviceCall({
            serviceUrl: constants.host + "/openidm/" + component, url: "/" + id, type: "GET",
            errorsHandlers: errorsHandlers})
        .then(function (user) {
            if (!_.has(user, 'uid')) {
                user.uid = user.userName || user._id;
            }

            return user;
        });
    };

    /**
     * Checks if logged in and returns users id
     */
    obj.getProfile = function(errorsHandlers, headers) {
        var uiRoles = {
            "openidm-authorized": "ui-user",
            "openidm-admin": "ui-admin"
        };

        return obj.serviceCall({
            serviceUrl: constants.host + "/openidm/info/login",
            url: "",
            headers: headers,
            errorsHandlers: errorsHandlers
        })
        .then(function(rawData) {
            var i, data;

            data = {
                id : rawData.authorizationId.id,
                username : rawData.authenticationId,
                uid : rawData.authenticationId,
                roles: rawData.authorizationId.roles,
                component: rawData.authorizationId.component
            };

            // previously roles were sometimes stored as a CSV - convert those into a proper array
            if (typeof data.roles === "string") {
                data.roles = data.roles.split(",");
            }

            for (i=(data.roles.length-1); i>=0; i--) {
                if (_.has(uiRoles,data.roles[i])) {
                    data.roles.push(uiRoles[data.roles[i]]);
                }
            }

            return obj.getUserById(data.id, data.component, errorsHandlers)
                .then(function(userData) {
                    userData.roles = data.roles;
                    userData.component = data.component;
                    if (!userData.userName) {
                        userData.userName = userData._id;
                    }
                    return userData;
                });
        });
    };


    return obj;
});
