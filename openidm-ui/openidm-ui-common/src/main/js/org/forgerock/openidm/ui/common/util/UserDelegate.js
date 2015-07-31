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

define("org/forgerock/openidm/ui/common/util/UserDelegate", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager"
], function($, _, constants, AbstractDelegate, configuration, eventManager) {

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

        return obj.serviceCall({url: "?_queryId=query-all", success: function(data) {
            if(successCallback) {
                obj.users = data.result;
                successCallback(data.result);
            }
        }, error: errorCallback} );
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

    /**
     * Aggregate View Calls
     */

    obj.userLinkedView = function(id) {
        return obj.serviceCall({
            serviceUrl: constants.host + "/openidm/endpoint/linkedView/managed/user/",
            url: id,
            type: "GET"
        });
    };

    return obj;
});
