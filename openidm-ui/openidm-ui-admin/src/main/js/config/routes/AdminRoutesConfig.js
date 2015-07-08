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

/*global define*/
define("config/routes/AdminRoutesConfig", [
    "config/routes/CommonRoutesConfig"
], function(commonRoutes) {

    var obj = {
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
            pattern: "connectors/edit/?/?",
            forceUpdate: true
        },
        "addConnectorView" : {
            view: "org/forgerock/openidm/ui/admin/connector/AddConnectorView",
            role: "ui-admin",
            url: "connectors/add/",
            forceUpdate: true
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
            pattern: "managed/edit/?/",
            forceUpdate: true
        },
        "addManagedView" : {
            view: "org/forgerock/openidm/ui/admin/managed/AddEditManagedView",
            role: "ui-admin",
            url: "managed/add/",
            forceUpdate: true
        },
        "authenticationView" : {
            view: "org/forgerock/openidm/ui/admin/authentication/AuthenticationView",
            role: "ui-admin",
            url: "authentication/",
            forceUpdate: true
        },
        "settingsView" : {
            view: "org/forgerock/openidm/ui/admin/settings/SettingsView",
            role: "ui-admin",
            url: "settings/",
            forceUpdate: true
        },
        "addMappingView" : {
            view: "org/forgerock/openidm/ui/admin/mapping/AddMappingView",
            role: "ui-admin",
            url: "mapping/add/",
            forceUpdate: true
        },
        "autoAddMappingView" : {
            view: "org/forgerock/openidm/ui/admin/mapping/AddMappingView",
            role: "ui-admin",
            url: /mapping\/add\/(.+?)\/(.+?)$/,
            pattern: "mapping/add/?/?",
            forceUpdate: true
        },
        "mappingListView" : {
            view: "org/forgerock/openidm/ui/admin/mapping/MappingListView",
            role: "ui-admin",
            url: "mapping/",
            forceUpdate: true
        },
        "propertiesView" : {
            childView: "org/forgerock/openidm/ui/admin/mapping/PropertiesView",
            view: "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
            role: "ui-admin",
            url: /^properties\/([^\/]+)\/$/,
            pattern: "properties/?/"
        },
        "editMappingProperty" : {
            base: "propertiesView",
            dialog: "org/forgerock/openidm/ui/admin/mapping/properties/EditPropertyMappingDialog",
            role: "ui-admin",
            url: /properties\/(.+?)\/(.+?)$/,
            pattern: "properties/?/?",
            forceUpdate: true
        },
        "addMappingProperty" : {
            base: "propertiesView",
            dialog: "org/forgerock/openidm/ui/admin/mapping/properties/AddPropertyMappingDialog",
            role: "ui-admin",
            url: /properties\/(.+?)\/_new$/,
            pattern: "properties/?/_new",
            forceUpdate: true
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
        }
    };

    obj.landingPage = obj.connectorListView;
    commonRoutes["default"].role = "ui-admin";
    return obj;
});