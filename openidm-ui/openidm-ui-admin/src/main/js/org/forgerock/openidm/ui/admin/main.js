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


define("org/forgerock/openidm/ui/admin/main", [

    "./delegates/ConnectorDelegate",
    "./delegates/SyncDelegate",
    "./delegates/BrowserStorageDelegate",
    "./delegates/ExternalAccessDelegate",
    "./delegates/SearchDelegate",
    "./delegates/ReconDelegate",
    "./delegates/SecurityDelegate",
    "./delegates/WorkflowDelegate",
    "./delegates/ScriptDelegate",
    "./delegates/SiteConfigurationDelegate",

    "./ResourcesView",
    "./MapResourceView",
    
    "./connector/AddEditConnectorView",
    "./connector/ConnectorTypeAbstractView",
    "./connector/ConnectorTypeView",
    "./connector/ConnectorRegistry",

    "./managed/AddEditManagedView",

    "./util/ConnectorUtils",
    "./util/LinkQualifierUtils",
    "./util/ScriptEditor",
    "./util/ScriptList",
    "./util/ScriptDialog",
    "./util/AutoCompleteUtils",
    "./util/Scheduler",
    "./util/MappingUtils",
    "./util/ReconDetailsView",
    "./util/QueryFilterUtils",
    "./util/AutoCompleteUtils",
    "./util/SaveChangesView",
    "./util/FilterEvaluator",

    "./objectTypes/ObjectTypesDialog",

    "./authentication/AuthenticationView",

    "./mapping/AddMappingView",
    "./mapping/MappingListView",
    "./mapping/MappingBaseView",
    "./mapping/PropertiesView",
    "./mapping/AddPropertyMappingDialog",
    "./mapping/EditPropertyMappingDialog",
    "./mapping/MappingRoleEntitlementsView",

    "./sync/SyncView",
    "./sync/SituationPolicyView",
    "./sync/SituationPolicyDialogView",
    "./sync/CorrelationView",
    "./sync/AnalysisView",
    "./sync/ObjectFiltersView",
    "./sync/SituationalScriptsView",
    "./sync/ReconScriptsView",
    "./sync/ReconQueriesView",
    "./sync/QueryFilterEditor",
    "./sync/LinkQualifierFilterEditor",
    "./sync/CorrelationQueryBuilderView",
    "./sync/CorrelationQueryDialog",
    "./sync/CorrelationQueryView",
    "./sync/ChangeAssociationDialog",
    "./sync/TestSyncView",
    "./sync/TestSyncGridView",
    "./sync/ScheduleView",

    "./login/LoginView",
    
    "./settings/SettingsView",
    
    "./role/EditRoleView",
    "./role/RoleUsersView",
    "./role/RoleEntitlementsListView",
    "./role/RoleEntitlementsEditView"

]);