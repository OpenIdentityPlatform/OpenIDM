/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global define */
define("config/routes/AdminRoutesConfig", [
    "config/routes/CommonRoutesConfig"
], function(commonRoutes) {

    var obj = {
        "dashboardView" : {
            view: "org/forgerock/openidm/ui/admin/dashboard/Dashboard",
            role: "ui-admin",
            url: "dashboard/"
        },
        "userRegistrationView" : {
            view: "org/forgerock/openidm/ui/admin/selfservice/UserRegistrationConfigView",
            role: "ui-admin",
            url: "selfservice/userregistration/"
        },
        "passwordResetView" : {
            view: "org/forgerock/openidm/ui/admin/selfservice/PasswordResetConfigView",
            role: "ui-admin",
            url: "selfservice/passwordreset/"
        },
        "connectorListView" : {
            view: "org/forgerock/openidm/ui/admin/connector/ConnectorListView",
            role: "ui-admin",
            url: "connectors/"
        },
        "editConnectorView" : {
            view: "org/forgerock/openidm/ui/admin/connector/EditConnectorView",
            role: "ui-admin",
            defaults : ["", ""],
            url: /^connectors\/edit\/(.+?)\/(.*)$/,
            pattern: "connectors/edit/?/?"
        },
        "addConnectorView" : {
            view: "org/forgerock/openidm/ui/admin/connector/AddConnectorView",
            role: "ui-admin",
            url: "connectors/add/"
        },
        "managedListView" : {
            view: "org/forgerock/openidm/ui/admin/managed/ManagedListView",
            role: "ui-admin",
            url: "managed/"
        },
        "editManagedView" : {
            view: "org/forgerock/openidm/ui/admin/managed/AddEditManagedView",
            role: "ui-admin",
            url: /^managed\/edit\/(.+)\/$/,
            pattern: "managed/edit/?/"
        },
        "addManagedView" : {
            view: "org/forgerock/openidm/ui/admin/managed/AddEditManagedView",
            role: "ui-admin",
            url: "managed/add/"
        },
        "authenticationView" : {
            view: "org/forgerock/openidm/ui/admin/authentication/AuthenticationView",
            role: "ui-admin",
            url: "authentication/"
        },
        "settingsView" : {
            view: "org/forgerock/openidm/ui/admin/settings/SettingsView",
            role: "ui-admin",
            url: "settings/"
        },
        "addMappingView" : {
            view: "org/forgerock/openidm/ui/admin/mapping/AddMappingView",
            role: "ui-admin",
            url: "mapping/add/"
        },
        "autoAddMappingView" : {
            view: "org/forgerock/openidm/ui/admin/mapping/AddMappingView",
            role: "ui-admin",
            url: /mapping\/add\/(.+?)\/(.+?)$/,
            pattern: "mapping/add/?/?"
        },
        "mappingListView" : {
            view: "org/forgerock/openidm/ui/admin/mapping/MappingListView",
            role: "ui-admin",
            url: "mapping/"
        },
        "propertiesView" : {
            childView: "org/forgerock/openidm/ui/admin/mapping/PropertiesView",
            view: "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
            role: "ui-admin",
            url: /^properties\/([^\/]+)\/$/,
            pattern: "properties/?/"
        },
        "behaviorsView" : {
            childView: "org/forgerock/openidm/ui/admin/mapping/BehaviorsView",
            view: "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
            role: "ui-admin",
            url: /^behaviors\/(.+)\/$/,
            pattern: "behaviors/?/"
        },
        "associationView" : {
            childView: "org/forgerock/openidm/ui/admin/mapping/AssociationView",
            view: "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
            role: "ui-admin",
            url: /^association\/(.+)\/$/,
            pattern: "association/?/"
        },
        "scheduleView" : {
            childView: "org/forgerock/openidm/ui/admin/mapping/ScheduleView",
            view: "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
            role: "ui-admin",
            url: /^schedule\/(.+)\/$/,
            pattern: "schedule/?/"
        },
        "adminListSystemObjectView" : {
            view: "org/forgerock/openidm/ui/common/resource/ListResourceView",
            role: "ui-admin",
            url: /^resource\/(system)\/(.+)\/(.+)\/list\/$/,
            pattern: "resource/?/?/?/list/"
        },
        "adminEditSystemObjectView" : {
            view: "org/forgerock/openidm/ui/common/resource/EditResourceView",
            role: "ui-admin",
            url: /^resource\/(system)\/(.+)\/(.+)\/edit\/(.+)$/,
            pattern: "resource/?/?/?/edit/?",
            forceUpdate: true
        },
        "adminNewSystemObjectView" : {
            view: "org/forgerock/openidm/ui/common/resource/EditResourceView",
            role: "ui-admin",
            url: /^resource\/(system)\/(.+)\/(.+)\/add\/$/,
            pattern: "resource/?/?/?/add/"
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
        },
        "adminEditRoleEntitlementView" : {
            view: "org/forgerock/openidm/ui/common/resource/EditResourceView",
            role: "ui-admin",
            url: /^resource\/(managed)\/(role)\/edit\/(.+)\/(.+)$/,
            pattern: "resource/?/?/edit/?/?"
        },
        "processListView" : {
            view: "org/forgerock/openidm/ui/admin/workflow/ProcessListView",
            role: "ui-admin",
            url: "workflow/processes/"
        },
        "taskListView" : {
            view: "org/forgerock/openidm/ui/admin/workflow/TaskListView",
            role: "ui-admin",
            url: "workflow/tasks/"
        },
        "taskInstanceView" : {
            view: "org/forgerock/openidm/ui/admin/workflow/TaskInstanceView",
            role: "ui-admin",
            url: /^workflow\/taskinstance\/(.+)$/,
            pattern: "workflow/taskinstance/?"
        },
        "processInstanceView" : {
            view: "org/forgerock/openidm/ui/admin/workflow/ProcessInstanceView",
            role: "ui-admin",
            url: /^workflow\/processinstance\/(.+)$/,
            pattern: "workflow/processinstance/?"
        },
        "processDefinitionView" : {
            view: "org/forgerock/openidm/ui/admin/workflow/ProcessDefinitionView",
            role: "ui-admin",
            url: /^workflow\/processdefinition\/(.+)$/,
            pattern: "workflow/processdefinition/?"
        }
    };

    obj.landingPage = obj.dashboardView;
    commonRoutes["default"].role = "ui-admin";
    return obj;
});