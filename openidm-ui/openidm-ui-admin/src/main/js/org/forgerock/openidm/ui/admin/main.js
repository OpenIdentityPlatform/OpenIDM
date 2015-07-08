/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

    "./components/Footer",

    "./delegates/ConnectorDelegate",
    "./delegates/SyncDelegate",
    "./delegates/ExternalAccessDelegate",
    "./delegates/ReconDelegate",
    "./delegates/SecurityDelegate",
    "./delegates/WorkflowDelegate",
    "./delegates/ScriptDelegate",
    "./delegates/SiteConfigurationDelegate",

    "./MapResourceView",

    "./connector/AbstractConnectorView",
    "./connector/EditConnectorView",
    "./connector/AddConnectorView",
    "./connector/ConnectorTypeAbstractView",
    "./connector/ConnectorTypeView",
    "./connector/ConnectorRegistry",
    "./connector/ConnectorListView",

    "./managed/AddEditManagedView",
    "./managed/ManagedListView",

    "./util/ConnectorUtils",
    "./util/LinkQualifierUtils",
    "./util/ScriptList",
    "./util/ScriptDialog",
    "./util/AutoCompleteUtils",
    "./util/Scheduler",
    "./util/ReconDetailsView",
    "./util/QueryFilterUtils",
    "./util/AutoCompleteUtils",
    "./util/SaveChangesView",
    "./util/FilterEvaluator",

    "./objectTypes/ObjectTypesDialog",

    "./mapping/AssociationView",
    "./mapping/association/AssociationRuleView",
    "./mapping/association/DataAssociationManagementView",
    "./mapping/association/IndividualRecordValidationView",
    "./mapping/association/ReconciliationQueryFiltersView",
    "./mapping/association/correlationQuery/CorrelationQueryBuilderView",
    "./mapping/association/correlationQuery/CorrelationQueryDialog",
    "./mapping/association/dataAssociationManagement/ChangeAssociationDialog",
    "./mapping/association/dataAssociationManagement/TestSyncDialog",

    "./mapping/BehaviorsView",
    "./mapping/behaviors/SingleRecordReconciliationGridView",
    "./mapping/behaviors/SingleRecordReconciliationView",
    "./mapping/behaviors/ReconciliationScriptView",
    "./mapping/behaviors/SituationalEventScriptsView",
    "./mapping/behaviors/PoliciesDialogView",
    "./mapping/behaviors/PoliciesView",

    "./mapping/PropertiesView",
    "./mapping/properties/LinkQualifiersView",
    "./mapping/properties/AttributesGridView",
    "./mapping/properties/RoleEntitlementsView",
    "./mapping/properties/AddPropertyMappingDialog",
    "./mapping/properties/EditPropertyMappingDialog",

    "./mapping/ScheduleView",
    "./mapping/scheduling/SchedulerView",
    "./mapping/scheduling/LiveSyncView",

    "./mapping/MappingBaseView",
    "./mapping/AddMappingView",
    "./mapping/MappingListView",

    "./mapping/util/MappingUtils",
    "./mapping/util/MappingScriptsView",
    "./mapping/util/QueryFilterEditor",
    "./mapping/util/LinkQualifierFilterEditor",

    "./login/LoginView",
    
    "./settings/SettingsView",
    "./settings/AuthenticationView",
    "./settings/AuditView",

    "./role/EditRoleView",
    "./role/RoleUsersView",
    "./role/RoleEntitlementsListView",
    "./role/RoleEntitlementsEditView",

    "./workflow/ProcessListView",
    "./workflow/TaskListView"
]);