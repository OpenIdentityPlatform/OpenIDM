/* @license 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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

/*global define, window*/

/**
 * @author yaromin
 */
define("config/process/UserConfig", [
    "org/forgerock/openidm/ui/common/util/Constants", 
    "org/forgerock/openidm/ui/common/main/EventManager"
], function(constants, eventManager) {
    var obj = [
        {
            startEvent: constants.EVENT_APP_INTIALIZED,
            description: "Starting basic components",
            dependencies: [
                "org/forgerock/openidm/ui/user/login/LoginCtrl",
                "org/forgerock/openidm/ui/common/components/Navigation",
                "org/forgerock/openidm/ui/common/components/popup/PopupCtrl",
                "org/forgerock/openidm/ui/common/components/Breadcrumbs",
                "org/forgerock/openidm/ui/common/main/Router",
                "org/forgerock/openidm/ui/user/delegates/UserDelegate",
                "org/forgerock/openidm/ui/common/main/Configuration"
            ],
            processDescription: function(event, 
                    loginCtrl, 
                    navigation, 
                    popupCtrl, 
                    breadcrumbs, 
                    router,
                    userDelegate,
                    conf) {
                              
                breadcrumbs.init();
                
                userDelegate.getProfile(function(user) {
                    conf.setProperty('loggedUser', user);
                    eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false});
                    router.init();
                }, function() {
                    eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true});
                    router.init();
                }, {"serverError": {status: "503"}, "unauthorized": {status: "401"}});
            }    
        },
        {
            startEvent: constants.EVENT_CHANGE_BASE_VIEW,
            description: "",
            dependencies: [
                "org/forgerock/openidm/ui/common/components/Navigation",
                "org/forgerock/openidm/ui/common/components/popup/PopupCtrl",
                "org/forgerock/openidm/ui/common/components/Breadcrumbs",
                "org/forgerock/openidm/ui/user/login/LoginCtrl",
                "org/forgerock/openidm/ui/common/main/Configuration"
            ],
            processDescription: function(event, navigation, popupCtrl, breadcrumbs, loginCtrl, conf) {
                navigation.init();
                popupCtrl.init();                
                
                breadcrumbs.buildByUrl();
                
                if(conf.loggedUser) {
                    loginCtrl.afterSuccessfulLogin();
                } else {
                    loginCtrl.init();
                }
            }
        },
        {
            startEvent: constants.EVENT_AUTHENTICATION_DATA_CHANGED,
            description: "",
            dependencies: [
                "org/forgerock/openidm/ui/common/main/Configuration",
                "org/forgerock/openidm/ui/user/login/LoginCtrl",
                "org/forgerock/openidm/ui/common/components/Navigation"
            ],
            processDescription: function(event, configuration, loginCtrl, navigation) {
                var serviceInvokerModuleName, serviceInvokerConfig; 
                serviceInvokerModuleName = "org/forgerock/openidm/ui/common/main/ServiceInvoker";
                serviceInvokerConfig = configuration.getModuleConfiguration(serviceInvokerModuleName);
                if(!event.anonymousMode) {
                    delete serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_PASSWORD];
                    delete serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_USERNAME];
                    delete serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_NO_SESION];
                    
                    loginCtrl.afterSuccessfulLogin();
                    navigation.reload();
                } else {
                    serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_PASSWORD] = constants.OPENIDM_ANONYMOUS_PASSWORD;
                    serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_USERNAME] = constants.OPENIDM_ANONYMOUS_USERNAME;
                    serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_NO_SESION]= true; 
                    
                    configuration.setProperty('loggedUser', null);
                    loginCtrl.init();
                    navigation.reload();
                }
                configuration.sendSingleModuleConfigurationChangeInfo(serviceInvokerModuleName);
            }
        },
        {
            startEvent: constants.FORGOTTEN_PASSWORD_CHANGED_SUCCESSFULLY,
            description: "",
            dependencies: [
            ],
            processDescription: function(event) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changedPassword");
                eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, { userName: event.userName, password: event.password});
            }
        },
        {
            startEvent: constants.EVENT_LOGIN_REQUEST,
            description: "",
            dependencies: [
                "org/forgerock/openidm/ui/user/login/LoginCtrl"
            ],
            processDescription: function(event, loginCtrl) {
                loginCtrl.loginUser(event.userName, event.password);
            }
        },
        {
            startEvent: constants.EVENT_USER_SUCCESSFULY_REGISTERED,
            description: "User registered",
            dependencies: [
                "org/forgerock/openidm/ui/common/main/Router"
            ],
            processDescription: function(event, router) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "afterRegistration");

                if(event.selfRegistration) {
                    eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, { userName: event.user.userName, password: event.user.password});
                } else {
                    router.navigate(router.configuration.routes.adminUsers.url, {trigger: true});
                }
            }
        },
        {
            startEvent: constants.EVENT_SUCCESFULY_LOGGGED_IN, 
            description: "",
            dependencies: [
                "org/forgerock/openidm/ui/user/delegates/UserDelegate",
                "org/forgerock/openidm/ui/common/main/Configuration",
                "org/forgerock/openidm/ui/user/login/LoginCtrl",
                "org/forgerock/openidm/ui/common/main/Configuration",
                "org/forgerock/openidm/ui/common/main/Router"
        ],
            processDescription: function(userData, userDelegate, conf, loginCtrl, appConfiguration, router) {
                var loggedUser = userData.user;                
                
                //if(loggedUser.roles && loggedUser.roles.indexOf("openidm-admin") !== -1) {
                if(userData.userName === "openidm-admin") {
                    conf.setProperty('loggedUser', {roles: "openidm-admin,openidm-authorized", userName: userData.userName});
                    eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false});
                    
                    if(conf.gotoURL) {
                        console.log("Auto redirect to " + conf.gotoURL);
                        router.navigate(conf.gotoURL, {trigger: true});
                        delete conf.gotoURL;
                    } else {
                        router.navigate("#/", {trigger: true});
                    }
                    
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loggedIn");
                    return;
                }
                
                userDelegate.getForUserName(loggedUser.userName, function(user) {
                    conf.setProperty('loggedUser', user);
                    
                    eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false});
                    
                    if(conf.gotoURL) {
                        console.log("Auto redirect to " + conf.gotoURL);
                        router.navigate(conf.gotoURL, {trigger: true});
                        delete conf.gotoURL;
                    } else {
                        router.navigate("#/", {trigger: true});
                    }
                    
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loggedIn");
                }, 
                function() { 
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "errorFetchingData");
					loginCtrl.afterLoginFailed();                                            
					loginCtrl.logoutUser();                                            
                });
            }
        },
        {
            startEvent: constants.EVENT_LOGOUT,
            description: "",
            dependencies: [
                "org/forgerock/openidm/ui/user/login/LoginCtrl",
                "org/forgerock/openidm/ui/common/main/Router",
                "org/forgerock/openidm/ui/common/main/Configuration"
            ],
            processDescription: function(event, loginCtrl, router, conf) {
                loginCtrl.logoutUser();
                eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, {anonymousMode: true}); 
                conf.setProperty('loggedUser', null);
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loggedOut");
                router.execRouteHandler("");
            }
         },
         {
             startEvent: constants.EVENT_UNAUTHORIZED,
             description: "",
             dependencies: [
                 "org/forgerock/openidm/ui/common/main/Router",
                 "org/forgerock/openidm/ui/common/main/Configuration",
                 "org/forgerock/openidm/ui/common/main/ViewManager",
                 "org/forgerock/openidm/ui/user/delegates/UserDelegate",
                 "org/forgerock/openidm/ui/common/main/EventManager",
                 "org/forgerock/openidm/ui/common/util/Constants"
             ],
             processDescription: function(error, router, conf, viewManager, userDelegate, eventManager, constants) {
                 if(!conf.loggedUser) {
                     if(!conf.gotoURL) {
                         conf.setProperty("gotoURL", window.location.hash);
                     }
                     
                     router.routeTo("login", {trigger: true});
                     return;
                 }
                 
                 userDelegate.getProfile(function(user) {   
                     eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unauthorized");
                     router.routeTo("", {trigger: true});
                 }, function() {
                     eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unauthorized");
                     console.log("Saving redirection link" + window.location.hash);
                     
                     if(!conf.gotoURL) {
                         conf.setProperty("gotoURL", window.location.hash);
                     }
                     
                     eventManager.sendEvent(constants.EVENT_LOGOUT);                                         
                 }, {"serverError": {status: "503"}, "unauthorized": {status: "401"}});    
             }
         },
         {
             startEvent: constants.EVENT_NOTIFICATION_DELETE_FAILED,
             description: "Error in deleting notification",
             dependencies: [
             ],
             processDescription: function(event) {
                 //TODO
                 //messagesCtrl.displayMessage('error', 'Failed to delete notification');
             }
         },
         {
             startEvent: constants.EVENT_GET_NOTIFICATION_FOR_USER_ERROR,
             description: "Error in getting notifications",
             dependencies: [
             ],
             processDescription: function(event) {
                 //TODO
                 //messagesCtrl.displayMessage('error', 'Unable to get notifications for user');
             }
         }
         ];
    return obj;
});
