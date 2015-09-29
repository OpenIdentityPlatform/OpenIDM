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
        "objectTypeSaved": {
            msg: "config.messages.ConnectorMessages.objectTypeSaved",
            type: "info"
        },
        "liveSyncSaved": {
            msg: "config.messages.ConnectorMessages.liveSyncSaved",
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
        "objectTypeLoaded" : {
            msg: "config.messages.ObjectTypeMessages.objectSuccessfullyLoaded",
            type: "info"
        },
        "objectTypeFailedToLoad" : {
            msg: "config.messages.ObjectTypeMessages.objectFailedToLoad",
            type: "error"
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
        },
        "mappingEvalError" : {
            msg: "config.messages.MappingMessages.mappingEvalError",
            type: "error"
        },
        "syncPolicySaveSuccess" : {
            msg: "config.messages.SyncMessages.policySaveSuccessful",
            type: "info"
        },
        "auditSaveSuccess" : {
            msg: "config.messages.AuditMessages.auditSaveSuccessful",
            type: "info"
        },
        "scheduleCreated" : {
            msg: "config.messages.SyncMessages.scheduleCreated",
            type: "info"
        },
        "scheduleSaved" : {
            msg: "config.messages.SyncMessages.scheduleSaved",
            type: "info"
        },
        "scheduleDeleted" : {
            msg: "config.messages.SyncMessages.scheduleDeleted",
            type: "info"
        },
        "syncLiveSyncSaveSuccess": {
            msg: "config.messages.SyncMessages.liveSyncSaved",
            type: "info"
        },
        "correlationQuerySaveSuccess": {
            msg: "config.messages.SyncMessages.correlationQuerySaved",
            type: "info"
        },
        "reconQueryFilterSaveSuccess": {
            msg: "config.messages.SyncMessages.reconQueryFilterSaveSuccess",
            type: "info"
        },
        "objectFiltersSaveSuccess": {
            msg: "config.messages.SyncMessages.objectFiltersSaved",
            type: "info"
        },
        "triggeredBySituationSaveSuccess": {
            msg: "config.messages.SyncMessages.triggeredBySituationSaved",
            type: "info"
        },
        "triggeredByReconSaveSuccess": {
            msg: "config.messages.SyncMessages.triggeredByReconSaved",
            type: "info"
        },
        "linkQualifierSaveSuccess": {
            msg: "config.messages.SyncMessages.linkQualifierSaveSuccess",
            type: "info"
        },
        "cancelActiveProcess": {
            msg: "config.messages.WorkflowMessages.cancelActiveProcess",
            type: "info"
        },
        "selfServiceSaveSuccess": {
            msg: "config.messages.settingMessages.saveSelfServiceSuccess",
            type: "info"
        },
        "emailConfigSaveSuccess": {
            msg: "config.messages.settingMessages.saveEmailSuccess",
            type: "info"
        },
        "selfServiceUserRegistrationSave" : {
            msg: "config.messages.selfService.userRegistrationSave",
            type: "info"
        },
        "selfServiceUserRegistrationDelete": {
            msg: "config.messages.selfService.userRegistrationDelete",
            type: "error"
        },
        "selfServicePasswordSave" : {
            msg: "config.messages.selfService.passwordSave",
            type: "info"
        },
        "selfServicePasswordDelete": {
            msg: "config.messages.selfService.passwordDelete",
            type: "error"
        }
    };

    return obj;
});