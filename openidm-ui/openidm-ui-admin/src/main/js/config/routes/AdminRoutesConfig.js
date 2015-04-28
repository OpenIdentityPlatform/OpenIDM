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
        "resourcesView" : {
            view: "org/forgerock/openidm/ui/admin/ResourcesView",
            role: "ui-admin",
            url: "resources/"
        },
        "editConnectorView" : {
            view: "org/forgerock/openidm/ui/admin/connector/AddEditConnectorView",
            role: "ui-admin",
            defaults : ["", ""],
            url: /^connectors\/edit\/(.+?)\/(.*)$/,
            pattern: "connectors/edit/?/?",
            forceUpdate: true
        },
        "addConnectorView" : {
            view: "org/forgerock/openidm/ui/admin/connector/AddEditConnectorView",
            role: "ui-admin",
            url: "connectors/add/",
            forceUpdate: true
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
        "mappingListView" : {
            view: "org/forgerock/openidm/ui/admin/mapping/MappingListView",
            role: "ui-admin",
            url: "mapping/",
            forceUpdate: true
        },
        "propertiesView" : {
            view: "org/forgerock/openidm/ui/admin/mapping/PropertiesView",
            role: "ui-admin",
            url: /^properties\/([^\/]+)\/$/,
            pattern: "properties/?/",
            forceUpdate: true
        },
        "editMappingProperty" : {
            base: "propertiesView",
            dialog: "org/forgerock/openidm/ui/admin/mapping/EditPropertyMappingDialog",
            role: "ui-admin",
            url: /property\/(.+?)\/(.+?)$/,
            pattern: "property/?/?",
            forceUpdate: true
        },
        "addMappingProperty" : {
            base: "propertiesView",
            dialog: "org/forgerock/openidm/ui/admin/mapping/AddPropertyMappingDialog",
            role: "ui-admin",
            url: /property\/(.+?)\/_new$/,
            pattern: "property/?/_new",
            forceUpdate: true
        },
        "syncView" : {
            view: "org/forgerock/openidm/ui/admin/sync/SyncView",
            role: "ui-admin",
            url: /^sync\/(.+)\/$/,
            pattern: "sync/?/",
            forceUpdate: true
        },
        "correlationView" : {
            view: "org/forgerock/openidm/ui/admin/sync/CorrelationView",
            role: "ui-admin",
            url: /^correlation\/(.+)\/$/,
            pattern: "correlation/?/",
            forceUpdate: true
        },
        "scheduleView" : {
            view: "org/forgerock/openidm/ui/admin/sync/ScheduleView",
            role: "ui-admin",
            url: /^schedule\/(.+)\/$/,
            pattern: "schedule/?/",
            forceUpdate: true
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
            pattern: "resource/?/?/?/edit/?"
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
            pattern: "resource/?/?/edit/?"
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
        }
    };

    obj.landingPage = obj.resourcesView;
    commonRoutes["default"].role = "ui-admin";
    return obj;
});
