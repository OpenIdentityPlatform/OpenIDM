/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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

/*global $, define */

/**
 * @author yaromin
 */
define("app/comp/user/delegates/UserDelegate",
        ["app/util/Constants",
         "app/comp/common/delegate/AbstractDelegate",
         "app/comp/main/Configuration",
         "app/comp/common/eventmanager/EventManager"],
         function(constants, AbstractDelegate, configuration, eventManager) {

    var obj = new AbstractDelegate(constants.host + "/openidm/managed/user");

    obj.usersCallback = null;
    obj.users = null;
    obj.numberOfUsers = 0;

    obj.getAllUsers = function(successCallback, errorCallback) {
        console.info("getting all users");

        obj.usersCallback = successCallback;
        obj.numberOfUsers = 0;

        obj.serviceCall({url: "/?_query-id=query-all&fields=*", success: function(data) {
            if(successCallback) {
                obj.users = data.result;
                successCallback(data.result);
            }
        }, error: errorCallback} );
    };

    //TODO this is only test utility method should be moved to the special test package
    obj.removeAllUsers = function() {
        var serviceInvokerConfig, finished, serviceInvokerModuleName = "app/comp/common/delegate/ServiceInvoker";
        serviceInvokerConfig = configuration.getModuleConfiguration(serviceInvokerModuleName);

        serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_PASSWORD] = "openidm-admin";
        serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_USERNAME] = "openidm-admin";
        serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_NO_SESION] = true;

        configuration.sendSingleModuleConfigurationChangeInfo(serviceInvokerModuleName);

        finished = 0;
        obj.getAllUsers(function() {
            var i, successCallback;
            
            successCallback = function(){
                finished++;
                if(finished === obj.users.length) {
                    console.debug("deleting finished");
                    serviceInvokerConfig.defaultHeaders = [];
                    eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true});
                }
            };
            
            for(i = 0; i < obj.users.length; i++ ) {
                obj.deleteEntity(obj.users[i]._id, successCallback);
            }			
        });
    };

    /**
     * Check credentials method
     */
    obj.getByCredentials = function(uid, password, successCallback, errorCallback) {
        var headers = {};
        headers[constants.OPENIDM_HEADER_PARAM_USERNAME] = "openidm-admin";
        headers[constants.OPENIDM_HEADER_PARAM_PASSWORD] = "openidm-admin";
        headers[constants.OPENIDM_HEADER_PARAM_NO_SESION] = false;
        obj.serviceCall({
            url: "/?_query-id=for-credentials&" + $.param({password: password, uid: uid.toLowerCase()}),
            headers: headers,
            success: function (data) {
                if(!data.result) {
                    if(errorCallback) {
                        errorCallback();
                    }
                } else if(successCallback) {
                    successCallback(data.result[0]);
                }
            },
            error: errorCallback
        });
    };

    /**
     * Check security answer method
     */
    obj.getBySecurityAnswer = function(uid, securityAnswer, successCallback, errorCallback) {
        obj.serviceCall({
            url: "/?_query-id=for-security-answer&" + $.param({uid: uid, securityanswer: securityAnswer.toLowerCase()}), 
            success: function (data) {
                if(!data.result) {
                    if(errorCallback) {
                        errorCallback();
                    }
                } else if(successCallback) {
                    successCallback(data.result[0]);
                }
            },
            error: errorCallback
        });
    };

    obj.getSecurityQuestionForUserName = function(uid, successCallback, errorCallback) {
        obj.serviceCall({
            url: "/?_query-id=get-security-question&" + $.param({uid: uid.toLowerCase()}), 
            success: function (data) {
                if(data.result.length !== 1) {
                    successCallback();
                } else if(successCallback) {
                    successCallback(data.result[0].securityquestion);
                }
            },
            error: errorCallback
        });
    };

    obj.getForUserName = function(uid, successCallback, errorCallback) {
        obj.serviceCall({
            url: "/?_query-id=for-userName&" + $.param({uid: uid.toLowerCase()}), 
            success: function (data) {
                if(data.result.length !== 1) {
                    if(errorCallback) {
                        errorCallback();
                    }
                } else if(successCallback) {
                    successCallback(data.result[0]);
                }
            },
            error: errorCallback
        });
    };

    /**
     * UserName availability check. 
     * If userName is available successCallback(true) is invoked, otherwise successCallback(false) is invoked
     * If error occurred, errorCallback is invoked. 
     */
    obj.checkUserNameAvailability = function(uid, successCallback, errorCallback) {
        obj.serviceCall({
            url: "/?_query-id=check-userName-availability&" + $.param({uid: uid.toLowerCase()}), 
            success: function (data) {
                if(successCallback) {
                    if(data.result.length === 0) { 
                        successCallback(true);
                    } else {
                        successCallback(false);
                    }
                }
            },
            error: errorCallback
        });
    };

    obj.logout = function() {
        var callParams = {
                url: "/",
                headers: {  },
                success: function () {
                    console.debug("Successfully logged out");
                },
                error: function () {
                    console.debug("Error during logging out");
                }
        };
        callParams.headers[constants.OPENIDM_HEADER_PARAM_LOGOUT] = true;

        obj.serviceCall(callParams);
    };

    /**
     * See AbstractDelegate.patchEntityDifferences
     */
    obj.patchUserDifferences = function(oldUserData, newUserData, successCallback, errorCallback, noChangesCallback) {
        console.info("updating user");
        obj.patchEntityDifferences({"_query-id": "for-userName", uid: oldUserData.userName.toLowerCase()}, oldUserData, newUserData, successCallback, errorCallback, noChangesCallback);
    };

    /**
     * See AbstractDelegate.patchEntity
     */
    obj.patchSelectedUserAttributes = function(userName, patchDefinitionObject, successCallback, errorCallback, noChangesCallback) {
        console.info("updating user");
        obj.patchEntity({"_query-id": "for-userName", uid: userName.toLowerCase()}, patchDefinitionObject, successCallback, errorCallback, noChangesCallback);
    };

    return obj;
});



