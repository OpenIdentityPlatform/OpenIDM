/* @license 
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
define("config/process/UserConfig",["app/util/Constants", "app/comp/common/eventmanager/EventManager"], 
        function(constants, eventManager) {
    var obj = 
        [
         {
             startEvent: constants.FORGOTTEN_PASSWORD_CHANGED_SUCCESSFULLY,
             description: "",
             dependencies: [
                            "app/comp/common/messages/MessagesCtrl"
                            ],
                            processDescription: function(event, messagesCtrl) {
                                messagesCtrl.displayMessage('info', 'Password has been changed');
                                eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, { userName: event.userName, password: event.password});
                            }
         },
         {
             startEvent: constants.EVENT_AUTHENTICATED,
             description: "",
             dependencies: [],
             processDescription: function(event) {
                 eventManager.sendEvent(constants.EVENT_SUCCESFULY_LOGGGED_IN, event.uid);
             }
         },
         {
             startEvent: constants.EVENT_LOGIN_REQUEST,
             description: "",
             dependencies: [
                            "app/comp/user/login/LoginCtrl"
                            ],
                            processDescription: function(event, loginCtrl) {
                                loginCtrl.loginUser(event.userName, event.password);
                            }
         },
         {
             startEvent: constants.EVENT_USER_REGISTRATION_ERROR,
             description: "User registration error",
             dependencies: [
                            "app/comp/common/messages/MessagesCtrl"
                            ],
                            processDescription: function(event, messagesCtrl) {
                                messagesCtrl.displayMessage('error', event.causeDescription);

                            }
         },
         {
             startEvent: constants.EVENT_USER_PROFILE_UPDATE_FAILED,
             description: "",
             dependencies: [
                            "app/comp/common/messages/MessagesCtrl"
                            ],
                            processDescription: function(event, messagesCtrl) {
                                messagesCtrl.displayMessage('error', 'Problem during profile update');
                            }
         },
         {
             startEvent: constants.EVENT_USER_PROFILE_UPDATED_SUCCESSFULY,
             description: "",
             dependencies: [
                            "app/comp/common/messages/MessagesCtrl"
                            ],
                            processDescription: function(event, messagesCtrl) {
                                messagesCtrl.displayMessage('info', 'Profile has been updated');
                            }
         },
         {
             startEvent: constants.EVENT_USER_SUCCESSFULY_REGISTERED,
             description: "User registered",
             dependencies: [
                            "app/comp/common/messages/MessagesCtrl",
                            "app/comp/common/breadcrumbs/BreadcrumbsCtrl"
                            ],
                            processDescription: function(event, messagesCtrl, breadcrumbsCtrl) {
                                messagesCtrl.displayMessage('info', 'User has been registered successfully');
                                if(event.selfRegistration) {
                                    eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, { userName: event.user.userName, password: event.user.password});
                                } else {
                                    breadcrumbsCtrl.goBack();	
                                }
                            }
         },
         {
             startEvent: constants.EVENT_SHOW_PROFILE_REQUEST,
             description: "",
             dependencies: [
                            "app/comp/common/breadcrumbs/BreadcrumbsCtrl",
                            "app/comp/user/profile/ProfileCtrl"
                            ],
                            processDescription: function(event,breadCrumbsCtrl, profileCtrl) {
                                breadCrumbsCtrl.clearPath();
                                profileCtrl.init(constants.MODE_USER);
                            }
         },
         {
             startEvent: constants.EVENT_SELF_REGISTRATION_REQUEST,
             description: "Self registration request",
             dependencies: [
                            "app/comp/common/breadcrumbs/BreadcrumbsCtrl",
                            "app/comp/user/registration/RegistrationCtrl"
                            ],
                            processDescription: function(event, breadcrumbsCtrl, registrationCtrl) {
                                breadcrumbsCtrl.set('Registration');
                                registrationCtrl.init();
                            }
         },
         {
             startEvent: constants.EVENT_FORGOTTEN_SHOW_REQUEST,
             description: "",
             dependencies: [
                            "app/comp/user/forgottenpassword/ForgottenPasswordDialogCtrl"
                            ],
                            processDescription: function(event, forgottenPasswordDialogCtrl) {
                                forgottenPasswordDialogCtrl.init(event);
                            }
         },
         {
             startEvent: constants.EVENT_PROFILE_INITIALIZATION,
             description: "",
             dependencies: [
                            "app/comp/common/breadcrumbs/BreadcrumbsCtrl"
                            ],
                            processDescription: function(event, breadcrumbsCtrl) {
                                breadcrumbsCtrl.set(event.profileName);
                            }
         },
         {
             startEvent: constants.EVENT_SUCCESFULY_LOGGGED_IN,
             description: "",
             dependencies: [
                            "app/comp/user/delegates/UserDelegate",
                            "app/comp/main/Configuration",
                            "app/comp/user/profile/ProfileCtrl",
                            "app/comp/user/login/LoginCtrl",
                            "app/comp/common/messages/MessagesCtrl",
                            "app/comp/main/Configuration",
                            "app/comp/common/navigation/NavigationCtrl"
                            ],
                            processDescription: function(userName, userDelegate, conf, profileCtrl, loginCtrl, messagesCtrl, appConfiguration, navigationCtrl) {
                                eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false});
                                userDelegate.getForUserName( 
                                        userName, 
                                        function(user) {
                                            conf.setProperty('loggedUser', user);


                                            if(appConfiguration.forceUserRole === constants.MODE_ADMIN || user.role === constants.MODE_ADMIN) {
                                                eventManager.sendEvent(constants.EVENT_SWITCH_VIEW_REQUEST, {viewId: "app/comp/admin/usermanagement/UsersCtrl"});
                                            } else {
                                                profileCtrl.init(constants.MODE_USER);
                                            }
                                            navigationCtrl.init(user.role);
                                            loginCtrl.afterSuccessfulLogin(user);
                                            messagesCtrl.displayMessage('info', 'You have been successfully logged in.');
                                         }, 
                                         function() { messagesCtrl.displayMessage('error', 'Error fetching user data');
                                        }
                                );
                            }
         },
         {
             startEvent: constants.EVENT_LOGIN_FAILED,
             description: "",
             dependencies: [
                            "app/comp/common/messages/MessagesCtrl",
                            "app/comp/user/login/LoginCtrl"
                            ],
                            processDescription: function(event, messagesCtrl, loginCtrl) {
                                messagesCtrl.displayMessage('error', 'Login/password combination is invalid.');
                                loginCtrl.afterLoginFailed();
                            }
         },
         {
             startEvent: constants.EVENT_LOGOUT,
             description: "",
             dependencies: [
                            "app/comp/main/MainCtrl",
                            "app/comp/user/login/LoginCtrl",
                            "app/comp/common/breadcrumbs/BreadcrumbsCtrl",
                            "app/comp/main/Configuration",
                            "app/comp/common/navigation/NavigationCtrl"
                            ],
                            processDescription: function(event, mainCtrl, loginCtrl, breadcrumbsCtrl, conf, navigationCtrl) {
                                loginCtrl.logoutUser();

                                eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true});
                                mainCtrl.clearContent();
                                breadcrumbsCtrl.clearPath();
                                breadcrumbsCtrl.set('Home');
                                conf.setProperty('loggedUser', null);
                                navigationCtrl.init();
                            }
         }
         ];
    return obj;
});