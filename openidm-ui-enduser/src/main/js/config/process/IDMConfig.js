/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
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

/*global define*/

/**
 * @author huck.elliott
 */
define("config/process/IDMConfig", [
    "org/forgerock/commons/ui/common/util/Constants", 
    "org/forgerock/commons/ui/common/main/EventManager"
], function(constants, eventManager) {
    var obj = [
        {
            startEvent: constants.EVENT_PROFILE_DELETE_USER_REQUEST,
            description: "",
            dependencies: [
                "UserDelegate",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/commons/ui/common/main/Router"
            ],
            processDescription: function(event, userDelegate, globalConfiguration, router) {
                if(event.userId === globalConfiguration.loggedUser._id) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "cannotDeleteYourself");
                    if(event.errorCallback) { event.errorCallback(); }
                    return;
                }
                
                userDelegate.deleteEntity(event.userId, function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "userDeleted");                    
                    router.routeTo(router.configuration.routes.adminUsers, {trigger: true});
                }, function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "userDeleteError");
                    router.routeTo(router.configuration.routes.adminUsers, {trigger: true});
                });
            }
        },
        {
            startEvent: constants.EVENT_USER_LIST_DELETE_USER_REQUEST,
            description: "",
            dependencies: [
                "UserDelegate",
                "org/forgerock/commons/ui/common/main/Configuration"
            ],
            processDescription: function(event, userDelegate, globalConfiguration) {
                if(event.userId === globalConfiguration.loggedUser._id) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "cannotDeleteYourself");
                    if(event.errorCallback) { event.errorCallback();}
                    return;
                }
                
                userDelegate.deleteEntity(event.userId, function() {
                    if(event.successCallback) { event.successCallback(); }
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "userDeleted");                    
                }, function() {
                    if(event.errorCallback) { event.errorCallback(); }
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "userDeleteError");
                });
            }
        },
        {
            startEvent: constants.EVENT_NOTIFICATION_DELETE_FAILED,
            description: "Error in deleting notification",
            dependencies: [ ],
            processDescription: function(event) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "errorDeletingNotification");
            }
        },
        {
            startEvent: constants.EVENT_GET_NOTIFICATION_FOR_USER_ERROR,
            description: "Error in getting notifications",
            dependencies: [ ],
            processDescription: function(event) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "errorFetchingNotifications");
            }
        }];
    
    return obj;
});