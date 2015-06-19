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
 * @author jdabrowski
 */
define("config/routes/SelfServiceRoutesConfig", [
], function() {
    
    var obj = {
        "dashboard": {
            view: "org/forgerock/openidm/ui/dashboard/Dashboard",
            role: "ui-user,ui-admin",
            url: "dashboard/",
            forceUpdate: true
        },
        "processesDashboard": {
            view: "org/forgerock/openidm/ui/dashboard/workflow/processes/StartProcessDashboardView",
            role: "ui-admin",
            url: "processes/",
            forceUpdate: true
        },
        "startProcesses": {
            view: "org/forgerock/openidm/ui/dashboard/workflow/processes/StartProcessDashboardView",
            role: "ui-admin",
            url: /^processes\/([A-Za-z0-9:]+)/,
            pattern: "processes/?/"
        },
        "completeTask": {
            view: "org/forgerock/openidm/ui/dashboard/Dashboard",
            role: "ui-admin",
            url: /^tasks\/([0-9]+)/,
            pattern: "tasks/?/"
        },
        "adminListManagedObjectView" : {
            view: "org/forgerock/openidm/ui/common/resource/ListResourceView",
            role: "ui-admin",
            url: /^resource\/(managed)\/(.+)\/list\/$/,
            pattern: "resource/?/?/list/"
        },
        "adminEditManagedObjectView" : {
            view: "org/forgerock/openidm/ui/common/resource/EditResourceView",
            role: "ui-admin",
            url: /^resource\/(managed)\/(.+)\/edit\/(.+)$/,
            pattern: "resource/?/?/edit/?",
            forceUpdate: true
        },
        "adminNewManagedObjectView" : {
            view: "org/forgerock/openidm/ui/common/resource/EditResourceView",
            role: "ui-admin",
            url: /^resource\/(managed)\/(.+)\/add\/$/,
            pattern: "resource/?/?/add/"
        }
    };

    obj.landingPage = obj.dashboard;

    return obj;
});