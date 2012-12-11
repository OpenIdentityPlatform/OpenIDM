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
            view: "org/forgerock/openidm/ui/admin/Dashboard",
            role: "openidm-authorized,openidm-admin",
            url: "",
            forceUpdate: true
        },     
        
        //for admin
        "adminUsers": {
            view: "org/forgerock/openidm/ui/admin/users/UsersView",
            url: "users/",
            role: "openidm-admin"
        },
        "adminUserProfile" : {
            view: "org/forgerock/openidm/ui/admin/users/AdminUserProfileView",
            role: "openidm-admin",
            url: /^users\/show\/(.+)\/$/, 
            pattern: "users/show/?/"
        },
        "adminUserChangePassword" : {
            base: "adminUserProfile",
            dialog: "org/forgerock/openidm/ui/admin/users/ChangeUserPasswordDialog",
            url: /^users\/(.+)\/change_password\/$/, 
            pattern: "users/?/change_password/",
            role: "openidm-admin"
        },
        "adminUsersAdd" : {
            view: "org/forgerock/openidm/ui/admin/users/AdminUserRegistrationView",
            role: "openidm-admin",
            url: "users/add/"
        },
        
        "processesDashboard": {
            view: "org/forgerock/openidm/ui/admin/workflow/processes/StartProcessDashboardView",
            role: "openidm-admin",
            url: "processes/",
            forceUpdate: true
        },
        "startProcesses": {
            view: "org/forgerock/openidm/ui/admin/workflow/processes/StartProcessDashboardView",
            role: "openidm-admin",
            url: /^processes\/([A-Za-z0-9:]+)/,
            pattern: "processes/?/"
        },
        "completeTask": {
            view: "org/forgerock/openidm/ui/admin/Dashboard",
            role: "openidm-admin",
            url: /^tasks\/([0-9]+)/,
            pattern: "tasks/?/"
        }
    };
    
    return obj;
});