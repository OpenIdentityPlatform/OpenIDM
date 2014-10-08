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
], function() {

    var obj = {
        "mandatoryPasswordChangeDialog" : {
            base: "resourcesView",
            dialog: "org/forgerock/openidm/ui/admin/MandatoryPasswordChangeDialog",
            url: "resources/change_password/",
            role: "ui-admin"
        },
        "resourcesView" : {
            view: "org/forgerock/openidm/ui/admin/ResourcesView",
            role: "ui-admin",
            defaults : [""],
            url: /^resources\/(.*)$/,
            pattern: "resources/?"
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
        "mappingListView" : {
            view: "org/forgerock/openidm/ui/admin/mapping/MappingListView",
            role: "ui-admin",
            url: "mapping/",
            forceUpdate: true
        },
        "mappingView" : {
            view: "org/forgerock/openidm/ui/admin/mapping/PropertiesView",
            role: "ui-admin",
            url: /^mapping\/([^\/]+)\/$/,
            pattern: "mapping/?/",
            forceUpdate: true
        },
        "editMappingProperty" : {
            base: "mappingView",
            dialog: "org/forgerock/openidm/ui/admin/mapping/EditPropertyMappingDialog",
            role: "ui-admin",
            url: /mapping\/(.+?)\/property\/(.+?)$/,
            pattern: "mapping/?/property/?",
            forceUpdate: true
        },
        "addMappingProperty" : {
            base: "mappingView",
            dialog: "org/forgerock/openidm/ui/admin/mapping/AddPropertyMappingDialog",
            role: "ui-admin",
            url: /mapping\/(.+?)\/property\/_new$/,
            pattern: "mapping/?/property/_new",
            forceUpdate: true
        },
        "syncView" : {
            view: "org/forgerock/openidm/ui/admin/sync/SyncView",
            role: "ui-admin",
            url: /^sync\/(.+)\/$/,
            pattern: "sync/?/",
            forceUpdate: true
        }
    };

    return obj;
});
