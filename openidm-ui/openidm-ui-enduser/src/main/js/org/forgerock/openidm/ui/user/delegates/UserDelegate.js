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

/*global $, define, _ */

/**
 * @author yaromin
 */
define("UserDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager"
], function(constants, AbstractDelegate, configuration, eventManager) {

    var obj = new AbstractDelegate(constants.host + "/openidm/managed/user");

    obj.getUserResourceName = function (user) {
        return user.component + "/" + user._id;
    };

    obj.usersCallback = null;
    obj.users = null;
    obj.numberOfUsers = 0;

    obj.getAllUsers = function(successCallback, errorCallback) {
        console.info("getting all users");

        obj.usersCallback = successCallback;
        obj.numberOfUsers = 0;

        return obj.serviceCall({url: "?_queryId=query-all&fields=*", success: function(data) {
            if(successCallback) {
                obj.users = data.result;
                successCallback(data.result);
            }
        }, error: errorCallback} );
    };

    /**
     * Starting session. Sending username and password to authenticate and returns user's id.
     */
    obj.login = function(uid, password, successCallback, errorCallback, errorsHandlers) {
        var headers = {},
            promise;
        headers[constants.HEADER_PARAM_USERNAME] = uid;
        headers[constants.HEADER_PARAM_PASSWORD] = password;
        headers[constants.HEADER_PARAM_NO_SESSION] = false;

        promise = obj.getProfile(successCallback, errorCallback, errorsHandlers, headers);

        delete headers[constants.HEADER_PARAM_PASSWORD];

        return promise;
    };
    
    obj.getUserById = function(id, component, successCallback, errorCallback, errorsHandlers) {

        return this.serviceCall({
            serviceUrl: constants.host + "/openidm/" + component, url: "/" + id, type: "GET", 
            error: errorCallback,
            errorsHandlers: errorsHandlers}).then(function (user) {
                if (!_.has(user, 'uid')) {
                    user.uid = user.userName || user._id;
                }

                if (successCallback) {
                    successCallback(user);
                }

                return user;
            });
    };

       
    /**
     * Check credentials method
     */
    obj.checkCredentials = function(password, successCallback, errorCallback) {

        var headers = {};
        headers[constants.HEADER_PARAM_REAUTH] = password;
        return obj.serviceCall({
            serviceUrl: constants.host + "/openidm/authentication?_action=reauthenticate",
            url: "",
            type: "POST",
            headers: headers,
            success: successCallback,
            error: errorCallback,
            errorsHandlers: {
                "forbidden": { 
                    status: "403"
                }
            }

        });
    };
    
    /**
     * Checks if logged in and returns users id
     */
    obj.getProfile = function(successCallback, errorCallback, errorsHandlers, headers) {
        var uiRoles = {
            "openidm-authorized": "ui-user",
            "openidm-admin": "ui-admin"
        };

        return obj.serviceCall({
            serviceUrl: constants.host + "/openidm/info/login",
            url: "",
            headers: headers,
            success: function (rawData) {
                var i,data;

                if(!rawData.authorizationId) {
                    if(errorCallback) {
                        errorCallback();
                    }
                } else if(successCallback) {
                
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

                    for (i=(data.roles.length-1);i>=0;i--) {
                        if (_.has(uiRoles,data.roles[i])) {
                            data.roles.push(uiRoles[data.roles[i]]);
                        }
                    }

                    obj.getUserById(data.id, data.component, function (userData) {
                        userData.roles = data.roles;
                        userData.component = data.component;
                        if(!userData.userName){
                            userData.userName = userData._id;
                        }
                        successCallback(userData);
                    }, errorCallback, errorsHandlers);
                }
            },
            error: errorCallback,
            errorsHandlers: errorsHandlers
        });
    };

    obj.getSecurityQuestionForUserName = function(uid, successCallback, errorCallback) {
        return obj.serviceCall({
            serviceUrl: constants.host + "/openidm/endpoint/securityQA?_action=securityQuestionForUserName&" + $.param({uid: uid}), 
            type: "POST",
            url: "",
            success: function (data) {
                if(data.hasOwnProperty('securityQuestion')) {
                    successCallback(data.securityQuestion);
                } else if(errorCallback) {
                    errorCallback();
                }
            },
            error: errorCallback
        });
    };
    /**
     * Check security answer method
     */
    obj.getBySecurityAnswer = function(uid, securityAnswer, successCallback, errorCallback) {
        return obj.serviceCall({
            serviceUrl: constants.host + "/openidm/endpoint/securityQA?_action=checkSecurityAnswerForUserName&" + $.param({uid: uid, securityAnswer: securityAnswer}), 
            type: "POST",
            url: "",
            success: function (data) {
                if(data.result === "correct" && successCallback) {
                    successCallback(data);
                } else if (data.result === "error" && errorCallback) {
                    errorCallback(data);
                }
                
            },
            error: errorCallback
        });
    };

    
    /**
     * Setting new password for username if security answer is correct
     */
    obj.setNewPassword = function(userName, securityAnswer, newPassword, successCallback, errorCallback) {
        console.info("setting new password for user and security question");
        return obj.serviceCall({
            serviceUrl: constants.host + "/openidm/endpoint/securityQA?_action=setNewPasswordForUserName&" + $.param({newPassword: newPassword, uid: userName, securityAnswer: securityAnswer}), 
            type: "POST",
            url: "",
            success: function (data) {
                if(data.result === "correct" && successCallback) {
                    successCallback(data);
                } else if (data.result === "error") {
                    errorCallback(data);
                }
                
            },
            error: errorCallback
        });
    };

    obj.getForUserName = function(uid, successCallback, errorCallback) {
        return obj.serviceCall({
            url: "?_queryId=for-userName&" + $.param({uid: uid}), 
            error: errorCallback
        }).then(function (data) {
            if(data.result.length !== 1) {
                if(errorCallback) {
                    errorCallback();
                }
                return false;
            } else {
                if (!_.has(data.result[0], 'uid')) {
                    data.result[0].uid = data.result[0].userName || data.result[0]._id;
                }
            }

            if(successCallback) {
                successCallback(data.result[0]);
            }

            return data.result[0];
        });
    };
    
    /**
     * See AbstractDelegate.patchEntityDifferences
     */
    obj.patchUserDifferences = function(oldUserData, newUserData, successCallback, errorCallback, noChangesCallback, errorsHandlers) {
        delete oldUserData.uid;
        delete newUserData.uid;
        console.info("updating user");
        return obj.patchEntityDifferences({id: oldUserData._id, rev: oldUserData._rev}, oldUserData, newUserData, successCallback, errorCallback, noChangesCallback, errorsHandlers);
    };
    
    obj.updateUser = function(oldUserData, newUserData, successCallback, errorCallback, noChangesCallback) {
        delete oldUserData.uid;
        delete newUserData.uid;
        return obj.patchUserDifferences(oldUserData, newUserData, successCallback, errorCallback, noChangesCallback, {
                "forbidden": {
                    status: "403",
                    event: constants.EVENT_USER_UPDATE_POLICY_FAILURE
                }
            });
    };

    /**
     * See AbstractDelegate.patchEntity
     */
    obj.patchSelectedUserAttributes = function(id, rev, patchDefinitionObject, successCallback, errorCallback, noChangesCallback) {
        console.info("updating user");
        return obj.patchEntity({id: id, rev: rev}, patchDefinitionObject, successCallback, errorCallback, noChangesCallback);
    };

    return obj;
});



