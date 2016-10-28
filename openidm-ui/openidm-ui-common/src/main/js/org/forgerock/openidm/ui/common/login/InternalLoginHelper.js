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

define([
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/common/UserModel",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/commons/ui/common/main/ServiceInvoker",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/util/AMLoginUtils",
    "org/forgerock/openidm/ui/common/util/Constants"
], function ($, _,
             UserModel,
             eventManager,
             AbstractConfigurationAware,
             serviceInvoker,
             conf,
             amLoginUtils,
             Constants) {
    var obj = new AbstractConfigurationAware();

    obj.login = function(params, successCallback, errorCallback) {
        if (_.has(params, "userName") && _.has(params, "password")) {
            return (new UserModel()).login(params.userName, params.password).then(successCallback, function (xhr) {
                var reason = xhr.responseJSON.reason;
                if (reason === "Unauthorized") {
                    reason = "authenticationFailed";
                }
                if (errorCallback) {
                    errorCallback(reason);
                }
            });
        } else if (_.has(params, "authToken") && _.has(params, "provider")) {
            return (new UserModel()).tokenLogin(params.authToken, params.provider).then(successCallback, function (xhr) {
                var reason = xhr.responseJSON.reason;

                if (reason === "Unauthorized") {
                    eventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, {"key" : "socialAuthenticationFailed", "provider" : params.provider});
                    errorCallback();
                } else {
                    errorCallback(reason);
                }
            });
        }
    };

    obj.logout = function (successCallback, errorCallback) {
        var promise;

        if (conf.loggedUser) {
            promise = conf.loggedUser.logout().then((logoutUrl) => {
                delete conf.loggedUser;
                return logoutUrl;
            });
        } else {
            promise = $.Deferred().resolve();
        }

        promise.then((logoutUrl) => {
            if (conf.globalData.openamAuthEnabled){
                amLoginUtils.openamLogout(successCallback);
                return false;
            }

            if (logoutUrl || conf.globalData.logoutUrl) {
                window.location.href = logoutUrl || conf.globalData.logoutUrl;
                return false;
            }

            successCallback();
        });
    };

    obj.getLoggedUser = function(successCallback, errorCallback) {
        return (new UserModel()).getProfile().then(successCallback, function(e) {
            if(e.responseJSON && e.responseJSON.detail && e.responseJSON.detail.failureReasons && e.responseJSON.detail.failureReasons.length){
                if(_.where(e.responseJSON.detail.failureReasons,{ isAlive: false }).length){
                    conf.globalData.authenticationUnavailable = true;
                }
            }
            errorCallback();
        });

    };
    return obj;
});
