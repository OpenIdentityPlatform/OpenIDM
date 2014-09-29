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

define("config/messages/AdminMessages", [
], function() {

    var obj = {
        "connectorsNotAvailable": {
            msg: "config.messages.ConnectorMessages.connectorsNotAvailable",
            type: "error"
        },
        "connectorSaved": {
            msg: "config.messages.ConnectorMessages.connectorSaved",
            type: "info"
        },
        "connectorSaveFail": {
            msg: "config.messages.ConnectorMessages.connectorSaveFail",
            type: "error"
        },
        "connectorTestPass": {
            msg: "config.messages.ConnectorMessages.connectorTestPass",
            type: "info"
        },
        "connectorTestFailed": {
            msg: "config.messages.ConnectorMessages.connectorTestFailed",
            type: "error"
        },
        "deleteConnectorSuccess": {
            msg: "config.messages.ConnectorMessages.deleteConnectorSuccess",
            type: "info"
        },
        "deleteConnectorFail": {
            msg: "config.messages.ConnectorMessages.deleteConnectorFail",
            type: "error"
        },
        "connectorBadMainVersion": {
            msg: "config.messages.ConnectorMessages.connectorBadMainVersion",
            type: "error"
        },
        "connectorBadMinorVersion": {
            msg: "config.messages.ConnectorMessages.connectorBadMinorVersion",
            type: "error"
        },
        "connectorVersionChange": {
            msg: "config.messages.ConnectorMessages.connectorVersionChange",
            type: "info"
        },
        "deleteManagedSuccess": {
            msg: "config.messages.ManagedObjectMessages.deleteManagedSuccess",
            type: "info"
        },
        "deleteManagedFail": {
            msg: "config.messages.ManagedObjectMessages.deleteManagedFail",
            type: "error"
        },
        "managedObjectSaveSuccess" : {
            msg: "config.messages.ManagedObjectMessages.saveSuccessful",
            type: "info"
        },
        "authSaveSuccess" : {
            msg: "config.messages.AuthenticationMessages.saveSuccessful",
            type: "info"
        },
        "mappingSaveSuccess" : {
            msg: "config.messages.MappingMessages.mappingSaveSuccess",
            type: "info"
        },
        "newMappingAdded" : {
            msg: "config.messages.MappingMessages.newMappingAdded",
            type: "info"
        },
        "mappingDeleted" : {
            msg: "config.messages.MappingMessages.mappingDeleted",
            type: "info"
        }
    };

    return obj;
});

