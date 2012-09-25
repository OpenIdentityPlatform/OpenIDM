/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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
define("config/AppConfiguration", [
    "org/forgerock/commons/ui/common/util/Constants", 
    "org/forgerock/commons/ui/common/main/EventManager"
], function(constants, eventManager) {
    var obj = {
            moduleDefinition: [
                {
                    moduleClass: "org/forgerock/commons/ui/user/login/LoginCtrl",
                    configuration: {
                        loginHelperClass: "org/forgerock/commons/ui/user/login/InternalLoginHelper",
                        showCredentialFields: true,
                        hideLoginButton: false,
                        loginButtonDisabledByDefault: true
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/user/login/OpenAMLoginHelper",
                   configuration: {
                       loginURL: "http://openaminstallationdomain.com:8090/openam/UI/Login",
                       logoutURL: "http://openaminstallationdomain.com:8090/openam/UI/Logout",
                       passwordParameterName: "IDToken2",
                       userNameParameterName: "IDToken1",
                       logoutTestOnly: false,
                       loginTestOnly: false,
                       ajaxLogout: false
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/common/main/Router",
                   configuration: {
                       routes: {
                           "": {
                               view: "org/forgerock/openidm/ui/apps/dashboard/DashboardView",
                               role: "openidm-authorized",
                               url: ""                                   
                           },
                           "profile": {
                               view: "org/forgerock/commons/ui/user/profile/UserProfileView",
                               role: "openidm-authorized",
                               excludedRole: "openidm-admin",
                               url: "profile/" 
                           },
                           "siteIdentification": {
                               base: "profile",
                               dialog: "org/forgerock/commons/ui/user/profile/ChangeSiteIdentificationDialog",
                               url: "profile/site_identification/",
                               role: "openidm-authorized",
                               excludedRole: "openidm-admin"
                           },
                           "adminUsers": {
                               view: "org/forgerock/openidm/ui/admin/users/UsersView",
                               url: "users/",
                               role: "admin"
                           },
                           "adminUsersAdd" : {
                               view: "org/forgerock/openidm/ui/admin/users/AdminUserRegistrationView",
                               role: "admin",
                               url: "users/add/"
                           },
                           "adminUserProfile" : {
                               view: "org/forgerock/openidm/ui/admin/users/AdminUserProfileView",
                               role: "admin",
                               url: /^users\/(([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4}))\/$/,
                               pattern: "users/?/"
                           },
                           "adminUserChangePassword" : {
                               base: "adminUserProfile",
                               dialog: "org/forgerock/openidm/ui/admin/users/ChangeUserPasswordDialog",
                               url: /^users\/(([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4}))\/change_password\/$/,
                               pattern: "users/?/change_password/",
                               role: "admin"
                           },
                           "register": {
                               view: "org/forgerock/commons/ui/user/UserRegistrationView",
                               url: "register/"
                           },
                           "termsOfUse": {
                               base: "register",
                               dialog: "org/forgerock/commons/ui/user/TermsOfUseDialog",
                               url: "register/terms_of_use/"
                           },
                           "login" : {
                               view: "org/forgerock/commons/ui/user/LoginView",
                               url: "login/"
                           },                           
                           "forgottenPassword" : {
                               base: "login",
                               dialog: "org/forgerock/commons/ui/user/ForgottenPasswordDialog",
                               url: "profile/forgotten_password/"
                           },
                           "enterOldPassword": {
                               base: "profile",
                               dialog: "org/forgerock/commons/ui/user/profile/EnterOldPasswordDialog",
                               role: "openidm-authorized",
                               url: "profile/old_password/",
                               excludedRoles: "openidm-admin"
                           },
                           "changeSecurityData": {
                               base: "profile",
                               dialog: "org/forgerock/commons/ui/user/profile/ChangeSecurityDataDialog",
                               role: "openidm-authorized",
                               url: "profile/change_security_data/",
                               excludedRole: "openidm-admin"
                           },
                           "addMoreAppsView": {
                               view: "org/forgerock/openidm/ui/apps/AddMoreAppsView",
                               role: "openidm-authorized",                               
                               url: "applications/addmore/"
                           },
                           "allApps": {
                               view: "org/forgerock/openidm/ui/apps/AllAppsView",
                               role: "openidm-authorized",                               
                               url: "applications/all/"
                           },
                           "apps": {
                               view: "org/forgerock/openidm/ui/apps/AppsView",
                               role: "openidm-authorized",                               
                               url: "applications/"
                           },
                           "tasks": {
                               view: "org/forgerock/openidm/ui/admin/tasks/TasksView",
                               role: "openidm-admin",
                               url: "tasks/"
                           },
                           "taskDetails": {
                               view: "org/forgerock/openidm/ui/admin/tasks/TaskDetailsView",
                               role: "openidm-admin",
                               url: "tasks/:id"
                           },
                           "tasksWithMenu": {
                               view: "org/forgerock/openidm/ui/admin/tasks/TasksWithMenuView",
                               role: "openidm-admin",
                               url: "tasksmenu/"
                           }
                       }
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/common/main/ProcessConfiguration",
                   configuration: {
                       processConfigurationFiles: [
                           "config/process/AdminConfig",
                           "config/process/UserConfig",
                           "config/process/CommonConfig"
                       ]
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/common/main/ServiceInvoker",
                   configuration: {
                       defaultHeaders: {
                       }                                         
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/common/main/ErrorsHandler",
                   configuration: {
                       defaultHandlers: {
                           "unauthorized": {
                               status: "401",
                               event: constants.EVENT_UNAUTHORIZED
                           },
                           "serverError": {
                               status: "503",
                               event: constants.EVENT_SERVICE_UNAVAILABLE
                           }
                       }
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/common/components/Navigation",
                   configuration: {
                       links: {
                           "admin" : {
                               "role": "admin",
                               "urls": {
                                   "users": {
                                       "url": "#users/",
                                       "name": "Users"
                                   },
                                   "groups": {
                                       "url": "#groups/",
                                       "name": "Groups"
                                   },
                                   "tasks": {
                                       "url": "#tasks/",
                                       "name": "Tasks"
                                   }
                                   /*"tasksMenu": {
                                       "baseUrl": "#tasksmenu/",
                                       "url": "#tasksmenu/",
                                       "name": "Tasks with menu",
                                       "urls": {
                                           "myTasks": {
                                               "url": "#tasksmenu/",
                                               "name": "My tasks (4)"
                                           },
                                           "groupsTasks": {
                                               "url": "#tasksmenu/groups",
                                               "name": "Groups queue"
                                           },
                                           "usersTasks": {
                                               "url": "#tasksmenu/users",
                                               "name": "Users queue"
                                           }
                                       }
                                   }*/
                               }
                           },
                           "user" : {
                               "urls": {
                                   "apps": {
                                       "baseUrl": "#applications/",
                                       "url": "#applications/all/",
                                       "name": "Applications",
                                       "urls": {
                                           "allApps": {
                                               "url": "#applications/all/",
                                               "name": "All applications"
                                           },
                                           "addMore": {
                                               "url": "#applications/addmore/",
                                               "name": "Add more apps"
                                           }
                                       }    
                                   },
                                   "groups": {
                                       "url": "#groups/",
                                       "name": "Groups"
                                   }
                               }    
                           }
                       }                                       
                   } 
               },
               {
                   moduleClass: "org/forgerock/openidm/ui/apps/dashboard/NotificationViewHelper",
                   configuration: {
                       typeToIconMapping: {
                           "1": "images/notifications/pending.png",
                           "2": "images/notifications/group_added.png",
                           "3": "images/notifications/approved.png",
                           "4": "images/notifications/removed.png"
                       }
                   } 
               },
               {
                   moduleClass: "org/forgerock/openidm/ui/admin/tasks/TasksFormManager",
                   configuration: {
                       forms: {
                           "applicationAcceptance": "org/forgerock/openidm/ui/admin/tasks/ApplicationAcceptanceTask"
                       }
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/common/util/UIUtils",
                   configuration: {
                       templateUrls: [
                           "templates/apps/application.html"
                       ]
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/common/components/Messages",
                   configuration: {
                       messages: {
                           "invalidCredentials": {
                               msg: "Login/password combination is invalid.",
                               type: "error"
                           },
                           "serviceUnavailable": {
                               msg: "Service unavailable",
                               type: "error"
                           },
                           "changedPassword": {
                               msg: "Password has been changed",
                               type: "info"
                           },
                           "userAlreadyExists": {
                               msg: "User already exists",
                               type: "error"
                           },
                           "unknown": {
                               msg: "Unknown error. Please contact with administrator",
                               type: "error"
                           },
                           "profileUpdateFailed": {
                               msg: "Problem during profile update",
                               type: "error"
                           },
                           "profileUpdateSuccessful": {
                               msg: "Profile has been updated",
                               type: "info"
                           },
                           "userNameUpdated": {
                               msg: "Username has been modified succesfully.",
                               type: "info"
                           },
                           "afterRegistration": {
                               msg: "User has been registered successfully",
                               type: "info"
                           },
                           "loggedIn": {
                               msg: "You have been successfully logged in.",
                               type: "info"
                           },
                           "errorFetchingData": {
                               msg: "Error fetching user data",
                               type: "error"
                           },
                           "loggedOut": {
                               msg: "You have been logged out.",
                               type: "info"
                           },
                           "cannotDeleteYourself": {
                               msg: "You can't delete yourself",
                               type: "error"
                           },
                           "userDeleted": {
                               msg: "User has been deleted",
                               type: "info"
                           },
                           "userDeleteError": {
                               msg: "Error when deleting user",
                               type: "error"
                           },
                           "siteIdentificationChanged": {
                               msg: "Site identification image has been changed",
                               type: "info"
                           },
                           "securityDataChanged": {
                               msg: "Security data has been changed",
                               type: "info"
                           },
                           "unauthorized": {
                               msg: "Unauthorized access",
                               type: "error"
                           },
                           "userApplicationsUpdate": {
                               msg: "Application settings have been changed.",
                               type: "info"
                           },
                           "completedTask": {
                               msg: "Task has been completed.",
                               type: "info"
                           }
                       }
                   } 
               }
               ],
               loggerLevel: 'debug'
    };
    return obj;
});
