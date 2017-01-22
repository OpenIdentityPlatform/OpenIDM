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
 * Copyright 2017 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/common/UserModel",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/commons/ui/common/main/ServiceInvoker",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/commons/ui/user/delegates/AnonymousProcessDelegate",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/login/InternalLoginHelper"
], function ($, _,
             UserModel,
             eventManager,
             AbstractConfigurationAware,
             serviceInvoker,
             conf,
             Constants,
             AnonymousProcessDelegate,
             Router,
             InternalLoginHelper) {

    var obj = _.clone(InternalLoginHelper);

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
        } else if (_.has(params, "jwt")) {
            return (new UserModel()).autoLogin(params.jwt).then(successCallback, function (xhr) {
                var reason = xhr.responseJSON.reason;

                if (reason === "Unauthorized") {
                    reason = "authenticationFailed";
                }

                if (errorCallback) {
                    errorCallback(reason);
                }
            });
        } else if (_.has(params, "idToken") && _.has(params, "provider")) {
            return (new UserModel()).tokenLogin(params.idToken, params.provider).then(successCallback, function (xhr) {
                var reason = xhr.responseJSON.reason,
                    delegate = new AnonymousProcessDelegate("selfservice/socialUserClaim"),
                    input =  {
                        "id_token": params.idToken,
                        "access_token": params.accessToken,
                        "provider": params.provider
                    };

                if (reason === "Unauthorized") {
                    /*
                     First social provider login fail we attempt to locate an account to claim
                     */
                    delegate.submit(input).then((claimResult) => {
                        /*
                         If successful account located
                         */
                        if (claimResult.additions.claimedProfile) {
                            eventManager.sendEvent(Constants.EVENT_LOGIN_REQUEST, {
                                idToken: claimResult.additions.id_token,
                                provider: claimResult.additions.provider,
                                suppressMessage: false,
                                failureCallback: (reason) => {
                                    eventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "socialAuthenticationFailed");

                                    errorCallback();
                                }
                            });
                        } else {
                            /*
                             If account claim fails to find an account pass through to registration
                             */

                            window.location.href = Constants.host + "/#register/" + "&id_token=" + claimResult.additions.id_token + "&access_token=" + claimResult.additions.access_token + "&provider=" + claimResult.additions.provider;
                        }
                    },
                    (claimResult) => {
                        /*
                         Hard fail when multiple accounts or some unknown critical failure
                         */
                        eventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "socialAuthenticationFailed");

                        eventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                            route: Router.configuration.routes.login,
                            args: ["&preventAutoLogin=true"]
                        });
                    });
                } else {
                    errorCallback(reason);
                }
            });
        }
    };

    return obj;
});
