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
 * @author jdabrowski
 */
define("config/routes/AdminRoutesConfig", [
], function() {
    
    var obj = {
            "": {
              view: "org/forgerock/openidm/ui/apps/dashboard/DashboardView",
              role: "openidm-authorized",
              url: ""                                   
          },     
            
          //for admin
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
            
            //for tasks 
            "tasksWithMenu": {
                view: "org/forgerock/openidm/ui/admin/tasks/TasksWithMenuView",
                role: "openidm-admin",
                //url: "tasksmenu/:category/:id"
                url: /^tasks\/([A-Za-z]+)\/?([0-9]*)$/,
                pattern: "tasks/?/?",
                forceUpdate: true
            },
            "tasks": {
                view: "org/forgerock/openidm/ui/admin/tasks/TasksDashboard",
                role: "openidm-admin",
                url: "tasks/dashboard"
            },
            
            //for apps
            "addMoreAppsView": {
                view: "org/forgerock/openidm/ui/apps/AddMoreAppsView",
                role: "openidm-authorized", 
                excludedRole: "openidm-admin",
                url: "applications/addmore/"
            },
            "allApps": {
                view: "org/forgerock/openidm/ui/apps/AllAppsView",
                role: "openidm-authorized",  
                excludedRole: "openidm-admin",
                url: "applications/all/"
            },
            "apps": {
                view: "org/forgerock/openidm/ui/apps/AppsView",
                role: "openidm-authorized",    
                excludedRole: "openidm-admin",
                url: "applications/"
            }
    };
    
    return obj;
});