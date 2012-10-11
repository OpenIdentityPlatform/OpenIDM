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
                   moduleClass: "org/forgerock/commons/ui/common/main/SessionManager",
                   configuration: {
                       loginHelperClass: "org/forgerock/commons/ui/user/login/InternalLoginHelper"
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/user/SiteConfigurator",
                   configuration: {
                       selfRegistration: true,
                       enterprise: true
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
                   moduleClass: "org/forgerock/commons/ui/common/main/Router",
                   configuration: {
                       routes: {
                       },
                       loader: [
                           {"routes":"config/routes/CommonRoutesConfig"}, 
                           {"routes":"config/routes/AdminRoutesConfig"},
                           {"routes":"config/routes/UserRoutesConfig"}
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
                       },
                       loader: [
                                {"defaultHandlers":"config/errorhandlers/CommonErrorHandlers"}
                       ]
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/common/components/Navigation",
                   configuration: {
                       links: {
                           "admin" : {
                               "role": "admin",
                               "urls": {
                                   "dashboard": {
                                       "url": "#/",
                                       "name": "Dashboard"
                                   },
                                   "users": {
                                       "url": "#users/",
                                       "name": "Users"
                                   },
                                   "groups": {
                                       "url": "#groups/",
                                       "name": "Groups"
                                   },
                                   "tasksMenu": {
                                       "baseUrl": "#tasks/",
                                       "url": "#tasks/dashboard",
                                       "name": "Tasks",
                                       "urls": {
                                           "tasksDashboard": {
                                               "url": "#tasks/dashboard",
                                               "name": "Dashboard"
                                           },
                                           "allTasks": {
                                               "url": "#tasks/all",
                                               "name": "Candidate tasks"
                                           },
                                           "myTasks": {
                                               "url": "#tasks/assigned",
                                               "name": "My tasks"
                                           }
                                       }
                                   }
                               }
                           },
                           "user" : {
                               "urls": {
                                   "dashboard": {
                                       "url": "#/",
                                       "name": "Dashboard"
                                   },
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
                       forms: { // Workflow User Task to View mapping
                           "applicationAcceptance": "org/forgerock/openidm/ui/admin/tasks/ApplicationAcceptanceTask"
                       }
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/common/util/UIUtils",
                   configuration: {
                       templateUrls: [ //preloaded templates
                           "templates/apps/application.html",
                           "templates/admin/tasks/ProcessUserTaskTableTemplate.html"
                       ]
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/common/components/Messages",
                   configuration: {
                       messages: {
                       },
                       loader: [
                                {"messages":"config/messages/AdminMessages"},
                                {"messages":"config/messages/UserMessages"}
                       ]
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/common/main/ValidatorsManager",
                   configuration: {
                       validators: {
                       },
                       loader: [
                                {"validators":"config/validators/AdminValidators"},
                                {"validators":"config/validators/UserValidators"},
                                {"validators":"config/validators/CommonValidators"}
                       ]
                   } 
               }
               ],
               loggerLevel: 'debug'
    };
    return obj;
});
