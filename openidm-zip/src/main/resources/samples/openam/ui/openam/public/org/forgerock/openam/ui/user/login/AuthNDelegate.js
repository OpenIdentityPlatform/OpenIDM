/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global document, $, define, _, window */

define("org/forgerock/openam/ui/user/login/AuthNDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/i18nManager"
], function(constants, AbstractDelegate, configuration, eventManager, cookieHelper, router, i18nManager) {

    var obj = new AbstractDelegate(constants.host + "/"+ constants.amContext + "/json/authenticate"),
        requirementList = [],
        knownAuth = {}; // to be used to keep track of the attributes associated with whatever requirementList contains

    obj.begin = function () {

        var url,
            args = {},
            promise = $.Deferred();

        if (configuration.globalData.auth.realm !== "/") {
            args.realm = configuration.globalData.auth.realm;
        }

        knownAuth = _.clone(configuration.globalData.auth);

        url = "?" + $.param(args);
        if(configuration.globalData.auth.additional){
            if(args.realm){
                url += configuration.globalData.auth.additional;
            }
            else{
                //if no realm get rid of the 1st char ('&')
                url += configuration.globalData.auth.additional.substring(1);
            }
        }

        obj.serviceCall({
                type: "POST",
                data: "",
                url: url + "&_action=start",
                errorsHandlers: {
                    "unauthorized": { status: "401"}
                }
            })
        .done(function (requirements) {
            // only resolve the auth promise when we know the cookie name

            var tokenCookie = cookieHelper.getCookie(configuration.globalData.auth.cookieName);
            if(configuration.loggedUser && tokenCookie && window.location.hash.replace(/^#/, '').match(router.configuration.routes.login.url)){
                requirements.tokenId = tokenCookie;
            }
            promise.resolve(requirements);

        })
        .fail(function (jqXHR) {
                   // some auth processes might throw an error fail immediately
                   var errorBody = $.parseJSON(jqXHR.responseText);

                   // if the error body contains an authId, then we might be able to
                   // continue on after this error to the next module in the chain
                   if (errorBody.hasOwnProperty("authId")) {
                       obj.submitRequirements(errorBody)
                          .done(function (requirements) {
                              obj.resetProcess();
                              promise.resolve(requirements);
                          })
                          .fail(function () {
                              promise.reject();
                          });
                   } else {
                       // in this case, the user has no way to login
                       promise.reject();
                   }
               });

        return promise;

    };

    obj.handleRequirements = function (requirements) {
        if (requirements.hasOwnProperty("authId")) {
            requirementList.push(requirements);
        } else if (requirements.hasOwnProperty("tokenId")) {
            _.each(configuration.globalData.auth.cookieDomains,function(cookieDomain){
                cookieHelper.setCookie(configuration.globalData.auth.cookieName, requirements.tokenId, "", "/", cookieDomain);
            });
        }
    };

    obj.submitRequirements = function (requirements) {
        var promise = $.Deferred(),
            processSucceeded = function (requirements) {
                obj.handleRequirements(requirements);
                promise.resolve(requirements);
            },
            processFailed = function () {
                var failedStage = requirementList.length;
                obj.resetProcess();
                promise.reject(failedStage);
            };

            obj.serviceCall({
                type: "POST",
                data: JSON.stringify(requirements),
                url: "?_action=submitRequirements",
                errorsHandlers: {
                    "unauthorized": { status: "401"},
                    "timeout": { status: "408" }
                }
            })
            .then(processSucceeded,
                  function (jqXHR) {
                    var oldReqs,errorBody,currentStage = requirementList.length;
                    if (jqXHR.status === 408) {
                        // we timed out, so let's try again with a fresh session
                        oldReqs = requirementList[0];
                        obj.resetProcess();
                        obj.begin()
                            .done(function (requirements) {

                                obj.handleRequirements(requirements);

                                if (requirements.hasOwnProperty("authId")) {
                                    if (currentStage === 1) {
                                        // if we were at the first stage when the timeout occurred, try to do it again immediately.
                                        oldReqs.authId = requirements.authId;
                                        obj.submitRequirements(oldReqs)
                                            .done(processSucceeded)
                                            .fail(processFailed);
                                    } else {
                                        // restart the process at the beginning
                                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loginTimeout");
                                        promise.resolve(requirements);
                                    }
                                } else {
                                    promise.resolve(requirements);
                                }
                            })
                            .fail(processFailed); // this is very unlikely, since it would require a call to .begin() to fail after having succeeded once before
                    } else { // we have a 401 unauthorized response
                        errorBody = $.parseJSON(jqXHR.responseText);

                        // if the error body has an authId property, then we may be
                        // able to advance beyond this error
                        if (errorBody.hasOwnProperty("authId")) {

                            obj.submitRequirements(errorBody)
                               .done(processSucceeded)
                               .fail(processFailed);

                        } else {
                            processFailed();
                        }
                    }
                });

        return promise;
    };

    obj.resetProcess = function () {
        requirementList = [];
    };

    obj.getRequirements = function (args) {
        var ret = $.Deferred();

        // if we don't have any requires yet, or if the realm changes.
        if (requirementList.length === 0 || !_.isEqual(configuration.globalData.auth, knownAuth)) {

            obj.begin(args)
               .done(function (requirements) {
                   obj.handleRequirements(requirements);
                   ret.resolve(requirements);
               })
               .fail(function (jqXHR) {
                   ret.reject();
               });

        } else {
            ret.resolve(requirementList[requirementList.length-1]);
        }
        return ret;
    };

    obj.logout = function () {
        obj.resetProcess();
        var headers = {};
        headers[configuration.globalData.auth.cookieName] = cookieHelper.getCookie(configuration.globalData.auth.cookieName);

        return obj.serviceCall({
            type: "POST",
            data: "{}",
            url: "",
            serviceUrl: constants.host + "/"+ constants.amContext + "/json/sessions?_action=logout",
            headers : headers,
            errorsHandlers: {"Bad Request": {status: 400}}
        })
        .done(function () {
            console.log("Successfully logged out");
            _.each(configuration.globalData.auth.cookieDomains,function(cookieDomain){
                cookieHelper.deleteCookie(configuration.globalData.auth.cookieName, "/", cookieDomain);
            });
            cookieHelper.deleteCookie("session-jwt", "/");
        });

    };

    return obj;
});
