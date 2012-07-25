/*! @license 
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

/*global define*/

/**
 * @author yaromin
 */
define("config/process/AdminConfig",["app/util/Constants", "app/comp/common/eventmanager/EventManager"], 
        function(constants, eventManager) {
    var obj = 
        [
         {
             startEvent: constants.EVENT_ADMIN_SHOW_PROFILE_REQUEST,
             description: "",
             dependencies: [
                            "app/comp/common/breadcrumbs/BreadcrumbsCtrl",
                            "app/comp/admin/usermanagement/UsersCtrl",
                            "app/comp/user/profile/ProfileCtrl"
                            ],
                            processDescription: function(event, breadCrumbsCtrl, usersCtrl, profileCtrl) {
                                breadCrumbsCtrl.addPath('Users',function() {  
                                    usersCtrl.init();
                                    return false;
                                });
                                var profileTitle = event.user.firstname + " " + event.user.lastname + '\'s profile';
                                breadCrumbsCtrl.set(profileTitle);
                                profileCtrl.init(constants.MODE_ADMIN, profileTitle , event.user, event.callback);
                            }
         },
         {
             startEvent: constants.EVENT_PROFILE_DELETE_USER_REQUEST,
             description: "",
             dependencies: [
                            "app/comp/user/delegates/UserDelegate",
                            "app/comp/common/messages/MessagesCtrl",
                            "app/comp/common/breadcrumbs/BreadcrumbsCtrl",
                            "app/comp/main/Configuration"
                            ],
                            processDescription: function(event, userDelegate, messagesCtrl, breadcrumbsCtrl, globalConfiguration) {
                                if(event.userId === globalConfiguration.loggedUser._id) {
                                    messagesCtrl.displayMessage('error', "You can't delete yourself");
                                    if(event.errorCallback) { event.errorCallback(); }
                                    return;
                                }
                                userDelegate.deleteEntity(event.userId, 
                                        function() {
                                    if(event.successCallback) { event.successCallback(); }
                                    messagesCtrl.displayMessage('info', 'User has been deleted');
                                    breadcrumbsCtrl.goBack();
                                }, function() {
                                    if(event.errorCallback) { event.errorCallback(); }
                                    messagesCtrl.displayMessage('error', 'Error when deleting user');
                                    breadcrumbsCtrl.goBack();
                                });
                            }
         },
         {
             startEvent: constants.EVENT_USER_LIST_DELETE_USER_REQUEST,
             description: "",
             dependencies: [
                            "app/comp/user/delegates/UserDelegate",
                            "app/comp/common/messages/MessagesCtrl",
                            "app/comp/main/Configuration"
                            ],
                            processDescription: function(event, userDelegate, messagesCtrl, globalConfiguration) {
                                if(event.userId === globalConfiguration.loggedUser._id) {
                                    messagesCtrl.displayMessage('error', "You can't delete yourself");
                                    if(event.errorCallback) { event.errorCallback();}
                                    return;
                                }
                                userDelegate.deleteEntity(event.userId, 
                                        function() {
                                    if(event.successCallback) { event.successCallback(); }
                                    messagesCtrl.displayMessage('info', 'User has been deleted');
                                }, function() {
                                    if(event.errorCallback) { event.errorCallback(); }
                                    messagesCtrl.displayMessage('error', 'Error when deleting user');
                                }
                                );
                            }
         },
         {
             startEvent: constants.EVENT_ADMIN_ADD_USER_REQUEST,
             description: "Admin registration request",
             dependencies: [
                            "app/comp/user/registration/RegistrationCtrl",
                            "app/comp/common/breadcrumbs/BreadcrumbsCtrl",
                            "app/comp/admin/usermanagement/UsersCtrl"
                            ],
                            processDescription: function(event, registrationCtrl, breadcrumbsCtrl, usersCtrl) {
                                registrationCtrl.init(constants.MODE_ADMIN);
                                breadcrumbsCtrl.addPath('Users', function() {  
                                    usersCtrl.init();
                                    return false;
                                });
                                breadcrumbsCtrl.set("Add user");
                            }
         },
         {
             startEvent: constants.EVENT_ADMIN_AUTHENTICATED,
             description: "",
             dependencies: [
                            "app/comp/main/Configuration"
                            ],
                            processDescription: function(event, appConfiguration) {
                                appConfiguration.forceUserRole = constants.MODE_ADMIN;
                                eventManager.sendEvent(constants.EVENT_AUTHENTICATED, event);
                            }
         }
         ];
    return obj;
});