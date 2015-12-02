/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
 */
package org.forgerock.openidm.provisioner.salesforce.internal.schema;

import org.forgerock.json.fluent.JsonValue;

import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * Brute-force schema details.
 */
public class SchemaHelper {
    enum ObjectTypes {
        User,
        Group,
        GroupMember,
        Organization,
        PermissionSet,
        PermissionSetAssignment,
        PermissionSetLicense,
        PermissionSetLicenseAssignment,
        Profile
    }

    private static final JsonValue objectSchema = json(
            object(
                    field("Group", object(
                            field("id", "Group"),
                            field("type", "object"),
                            field("properties", object(
                                    field("Id", object(
                                            field("type", "id"),
                                            field("nativeName", "Id"),
                                            field("nativeType", "id"),
                                            field("required", false)
                                    )),
                                    field("Name", object(
                                            field("type", "string"),
                                            field("nativeName", "Name"),
                                            field("nativeType", "string"),
                                            field("required", true)
                                    )),
                                    field("DeveloperName", object(
                                            field("type", "string"),
                                            field("nativeName", "DeveloperName"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("RelatedId", object(
                                            field("type", "reference"),
                                            field("nativeName", "RelatedId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("Type", object(
                                            field("type", "picklist"),
                                            field("nativeName", "Type"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("Email", object(
                                            field("type", "email"),
                                            field("nativeName", "Email"),
                                            field("nativeType", "email"),
                                            field("required", false)
                                    )),
                                    field("OwnerId", object(
                                            field("type", "reference"),
                                            field("nativeName", "OwnerId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("DoesSendEmailToMembers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "DoesSendEmailToMembers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("DoesIncludeBosses", object(
                                            field("type", "boolean"),
                                            field("nativeName", "DoesIncludeBosses"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("CreatedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "CreatedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("CreatedById", object(
                                            field("type", "reference"),
                                            field("nativeName", "CreatedById"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("LastModifiedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "LastModifiedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("LastModifiedById", object(
                                            field("type", "reference"),
                                            field("nativeName", "LastModifiedById"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("SystemModstamp", object(
                                            field("type", "datetime"),
                                            field("nativeName", "SystemModstamp"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    ))
                            ))
                    )),
                    field("GroupMember", object(
                            field("id", "GroupMember"),
                            field("type", "object"),
                            field("properties", object(
                                    field("Id", object(
                                            field("type", "id"),
                                            field("nativeName", "Id"),
                                            field("nativeType", "id"),
                                            field("required", false)
                                    )),
                                    field("GroupId", object(
                                            field("type", "reference"),
                                            field("nativeName", "GroupId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("UserOrGroupId", object(
                                            field("type", "reference"),
                                            field("nativeName", "UserOrGroupId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("SystemModstamp", object(
                                            field("type", "datetime"),
                                            field("nativeName", "SystemModstamp"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    ))
                            ))
                    )),
                    field("Organization", object(
                            field("id", "Organization"),
                            field("type", "object"),
                            field("properties", object(
                                    field("Id", object(
                                            field("type", "id"),
                                            field("nativeName", "Id"),
                                            field("nativeType", "id"),
                                            field("required", false)
                                    )),
                                    field("Name", object(
                                            field("type", "string"),
                                            field("nativeName", "Name"),
                                            field("nativeType", "string"),
                                            field("required", true)
                                    )),
                                    field("Division", object(
                                            field("type", "string"),
                                            field("nativeName", "Division"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("Street", object(
                                            field("type", "textarea"),
                                            field("nativeName", "Street"),
                                            field("nativeType", "textarea"),
                                            field("required", false)
                                    )),
                                    field("City", object(
                                            field("type", "string"),
                                            field("nativeName", "City"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("State", object(
                                            field("type", "string"),
                                            field("nativeName", "State"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("PostalCode", object(
                                            field("type", "string"),
                                            field("nativeName", "PostalCode"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("Country", object(
                                            field("type", "string"),
                                            field("nativeName", "Country"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("Latitude", object(
                                            field("type", "double"),
                                            field("nativeName", "Latitude"),
                                            field("nativeType", "double"),
                                            field("required", false)
                                    )),
                                    field("Longitude", object(
                                            field("type", "double"),
                                            field("nativeName", "Longitude"),
                                            field("nativeType", "double"),
                                            field("required", false)
                                    )),
                                    field("Phone", object(
                                            field("type", "phone"),
                                            field("nativeName", "Phone"),
                                            field("nativeType", "phone"),
                                            field("required", false)
                                    )),
                                    field("Fax", object(
                                            field("type", "phone"),
                                            field("nativeName", "Fax"),
                                            field("nativeType", "phone"),
                                            field("required", false)
                                    )),
                                    field("PrimaryContact", object(
                                            field("type", "string"),
                                            field("nativeName", "PrimaryContact"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("DefaultLocaleSidKey", object(
                                            field("type", "picklist"),
                                            field("nativeName", "DefaultLocaleSidKey"),
                                            field("nativeType", "picklist"),
                                            field("required", true)
                                    )),
                                    field("LanguageLocaleKey", object(
                                            field("type", "picklist"),
                                            field("nativeName", "LanguageLocaleKey"),
                                            field("nativeType", "picklist"),
                                            field("required", true)
                                    )),
                                    field("ReceivesInfoEmails", object(
                                            field("type", "boolean"),
                                            field("nativeName", "ReceivesInfoEmails"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("ReceivesAdminInfoEmails", object(
                                            field("type", "boolean"),
                                            field("nativeName", "ReceivesAdminInfoEmails"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PreferencesRequireOpportunityProducts", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PreferencesRequireOpportunityProducts"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("FiscalYearStartMonth", object(
                                            field("type", "int"),
                                            field("nativeName", "FiscalYearStartMonth"),
                                            field("nativeType", "int"),
                                            field("required", false)
                                    )),
                                    field("UsesStartDateAsFiscalYearName", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UsesStartDateAsFiscalYearName"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("DefaultAccountAccess", object(
                                            field("type", "picklist"),
                                            field("nativeName", "DefaultAccountAccess"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("DefaultContactAccess", object(
                                            field("type", "picklist"),
                                            field("nativeName", "DefaultContactAccess"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("DefaultOpportunityAccess", object(
                                            field("type", "picklist"),
                                            field("nativeName", "DefaultOpportunityAccess"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("DefaultLeadAccess", object(
                                            field("type", "picklist"),
                                            field("nativeName", "DefaultLeadAccess"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("DefaultCaseAccess", object(
                                            field("type", "picklist"),
                                            field("nativeName", "DefaultCaseAccess"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("DefaultCalendarAccess", object(
                                            field("type", "picklist"),
                                            field("nativeName", "DefaultCalendarAccess"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("DefaultPricebookAccess", object(
                                            field("type", "picklist"),
                                            field("nativeName", "DefaultPricebookAccess"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("DefaultCampaignAccess", object(
                                            field("type", "picklist"),
                                            field("nativeName", "DefaultCampaignAccess"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("SystemModstamp", object(
                                            field("type", "datetime"),
                                            field("nativeName", "SystemModstamp"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("ComplianceBccEmail", object(
                                            field("type", "email"),
                                            field("nativeName", "ComplianceBccEmail"),
                                            field("nativeType", "email"),
                                            field("required", false)
                                    )),
                                    field("UiSkin", object(
                                            field("type", "picklist"),
                                            field("nativeName", "UiSkin"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("TrialExpirationDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "TrialExpirationDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("OrganizationType", object(
                                            field("type", "picklist"),
                                            field("nativeName", "OrganizationType"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("WebToCaseDefaultOrigin", object(
                                            field("type", "string"),
                                            field("nativeName", "WebToCaseDefaultOrigin"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("CreatedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "CreatedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("CreatedById", object(
                                            field("type", "reference"),
                                            field("nativeName", "CreatedById"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("LastModifiedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "LastModifiedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("LastModifiedById", object(
                                            field("type", "reference"),
                                            field("nativeName", "LastModifiedById"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    ))
                            ))
                    )),
                    field("PermissionSet", object(
                            field("id", "PermissionSet"),
                            field("type", "object"),
                            field("properties", object(
                                    field("Id", object(
                                            field("type", "id"),
                                            field("nativeName", "Id"),
                                            field("nativeType", "id"),
                                            field("required", false)
                                    )),
                                    field("Name", object(
                                            field("type", "string"),
                                            field("nativeName", "Name"),
                                            field("nativeType", "string"),
                                            field("required", true)
                                    )),
                                    field("Label", object(
                                            field("type", "string"),
                                            field("nativeName", "Label"),
                                            field("nativeType", "string"),
                                            field("required", true)
                                    )),
                                    field("UserLicenseId", object(
                                            field("type", "reference"),
                                            field("nativeName", "UserLicenseId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("ProfileId", object(
                                            field("type", "reference"),
                                            field("nativeName", "ProfileId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("IsOwnedByProfile", object(
                                            field("type", "boolean"),
                                            field("nativeName", "IsOwnedByProfile"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEmailSingle", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEmailSingle"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEmailMass", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEmailMass"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditTask", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditTask"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditEvent", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditEvent"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsExportReport", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsExportReport"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsImportPersonal", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsImportPersonal"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsDataExport", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsDataExport"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditPublicTemplates", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditPublicTemplates"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsModifyAllData", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsModifyAllData"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageCases", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageCases"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsMassInlineEdit", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsMassInlineEdit"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageSolutions", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageSolutions"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsCustomizeApplication", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsCustomizeApplication"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditReadonlyFields", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditReadonlyFields"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsRunReports", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsRunReports"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsViewSetup", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsViewSetup"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsTransferAnyEntity", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsTransferAnyEntity"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsNewReportBuilder", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsNewReportBuilder"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsActivateContract", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsActivateContract"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsImportLeads", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsImportLeads"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageLeads", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageLeads"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsTransferAnyLead", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsTransferAnyLead"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsViewAllData", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsViewAllData"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditPublicDocuments", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditPublicDocuments"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsViewEncryptedData", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsViewEncryptedData"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditBrandTemplates", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditBrandTemplates"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditHtmlTemplates", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditHtmlTemplates"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsChatterInternalUser", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsChatterInternalUser"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsDeleteActivatedContract", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsDeleteActivatedContract"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsChatterInviteExternalUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsChatterInviteExternalUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsSendSitRequests", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsSendSitRequests"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsApiUserOnly", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsApiUserOnly"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageRemoteAccess", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageRemoteAccess"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsCanUseNewDashboardBuilder", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsCanUseNewDashboardBuilder"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageCategories", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageCategories"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsConvertLeads", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsConvertLeads"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsPasswordNeverExpires", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsPasswordNeverExpires"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsUseTeamReassignWizards", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsUseTeamReassignWizards"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsInstallPackaging", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsInstallPackaging"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsPublishPackaging", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsPublishPackaging"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsChatterOwnGroups", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsChatterOwnGroups"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditOppLineItemUnitPrice", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditOppLineItemUnitPrice"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsCreatePackaging", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsCreatePackaging"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsBulkApiHardDelete", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsBulkApiHardDelete"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsInboundMigrationToolsUser", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsInboundMigrationToolsUser"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsSolutionImport", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsSolutionImport"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageCallCenters", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageCallCenters"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsOutboundMigrationToolsUser", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsOutboundMigrationToolsUser"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsViewContent", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsViewContent"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageEmailClientConfig", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageEmailClientConfig"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEnableNotifications", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEnableNotifications"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageDataIntegrations", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageDataIntegrations"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsDistributeFromPersWksp", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsDistributeFromPersWksp"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsViewDataCategories", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsViewDataCategories"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageDataCategories", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageDataCategories"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsAuthorApex", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsAuthorApex"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageMobile", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageMobile"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsApiEnabled", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsApiEnabled"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageCustomReportTypes", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageCustomReportTypes"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditCaseComments", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditCaseComments"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsTransferAnyCase", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsTransferAnyCase"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsContentAdministrator", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsContentAdministrator"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsCreateWorkspaces", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsCreateWorkspaces"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageContentPermissions", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageContentPermissions"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageContentProperties", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageContentProperties"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageContentTypes", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageContentTypes"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsScheduleJob", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsScheduleJob"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageAnalyticSnapshots", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageAnalyticSnapshots"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsScheduleReports", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsScheduleReports"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageBusinessHourHolidays", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageBusinessHourHolidays"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsCustomSidebarOnAllPages", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsCustomSidebarOnAllPages"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsViewMyTeamsDashboards", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsViewMyTeamsDashboards"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsModerateChatter", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsModerateChatter"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsResetPasswords", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsResetPasswords"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsFlowUFLRequired", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsFlowUFLRequired"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsCanInsertFeedSystemFields", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsCanInsertFeedSystemFields"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEmailTemplateManagement", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEmailTemplateManagement"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEmailAdministration", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEmailAdministration"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageChatterMessages", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageChatterMessages"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsAllowEmailIC", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsAllowEmailIC"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsChatterFileLink", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsChatterFileLink"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsForceTwoFactor", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsForceTwoFactor"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsViewCaseInteraction", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsViewCaseInteraction"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageAuthProviders", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageAuthProviders"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsViewAllUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsViewAllUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsConnectOrgToEnvironmentHub", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsConnectOrgToEnvironmentHub"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsTwoFactorApi", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsTwoFactorApi"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsDeleteTopics", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsDeleteTopics"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditTopics", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditTopics"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsCreateTopics", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsCreateTopics"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsAssignTopics", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsAssignTopics"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsIdentityEnabled", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsIdentityEnabled"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsIdentityConnect", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsIdentityConnect"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("Description", object(
                                            field("type", "string"),
                                            field("nativeName", "Description"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("CreatedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "CreatedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("CreatedById", object(
                                            field("type", "reference"),
                                            field("nativeName", "CreatedById"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("LastModifiedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "LastModifiedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("LastModifiedById", object(
                                            field("type", "reference"),
                                            field("nativeName", "LastModifiedById"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("SystemModstamp", object(
                                            field("type", "datetime"),
                                            field("nativeName", "SystemModstamp"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("NamespacePrefix", object(
                                            field("type", "string"),
                                            field("nativeName", "NamespacePrefix"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    ))
                            ))
                    )),
                    field("PermissionSetAssignment", object(
                            field("id", "PermissionSetAssignment"),
                            field("type", "object"),
                            field("properties", object(
                                    field("Id", object(
                                            field("type", "id"),
                                            field("nativeName", "Id"),
                                            field("nativeType", "id"),
                                            field("required", false)
                                    )),
                                    field("PermissionSetId", object(
                                            field("type", "reference"),
                                            field("nativeName", "PermissionSetId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("AssigneeId", object(
                                            field("type", "reference"),
                                            field("nativeName", "AssigneeId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("SystemModstamp", object(
                                            field("type", "datetime"),
                                            field("nativeName", "SystemModstamp"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    ))
                            ))
                    )),
                    field("PermissionSetLicense", object(
                            field("id", "PermissionSetLicense"),
                            field("type", "object"),
                            field("properties", object(
                                    field("Id", object(
                                            field("type", "id"),
                                            field("nativeName", "Id"),
                                            field("nativeType", "id"),
                                            field("required", false)
                                    )),
                                    field("IsDeleted", object(
                                            field("type", "boolean"),
                                            field("nativeName", "IsDeleted"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("DeveloperName", object(
                                            field("type", "string"),
                                            field("nativeName", "DeveloperName"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("Language", object(
                                            field("type", "picklist"),
                                            field("nativeName", "Language"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("MasterLabel", object(
                                            field("type", "string"),
                                            field("nativeName", "MasterLabel"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("CreatedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "CreatedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("CreatedById", object(
                                            field("type", "reference"),
                                            field("nativeName", "CreatedById"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("LastModifiedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "LastModifiedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("LastModifiedById", object(
                                            field("type", "reference"),
                                            field("nativeName", "LastModifiedById"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("SystemModstamp", object(
                                            field("type", "datetime"),
                                            field("nativeName", "SystemModstamp"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("PermissionSetLicenseKey", object(
                                            field("type", "string"),
                                            field("nativeName", "PermissionSetLicenseKey"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("TotalLicenses", object(
                                            field("type", "int"),
                                            field("nativeName", "TotalLicenses"),
                                            field("nativeType", "int"),
                                            field("required", false)
                                    )),
                                    field("Status", object(
                                            field("type", "picklist"),
                                            field("nativeName", "Status"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("ExpirationDate", object(
                                            field("type", "date"),
                                            field("nativeName", "ExpirationDate"),
                                            field("nativeType", "date"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsEmailSingle", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsEmailSingle"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsEmailMass", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsEmailMass"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsEditTask", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsEditTask"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsEditEvent", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsEditEvent"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsExportReport", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsExportReport"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsImportPersonal", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsImportPersonal"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsDataExport", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsDataExport"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsEditPublicTemplates", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsEditPublicTemplates"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsModifyAllData", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsModifyAllData"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageCases", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageCases"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsMassInlineEdit", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsMassInlineEdit"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageSolutions", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageSolutions"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsCustomizeApplication", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsCustomizeApplication"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsEditReadonlyFields", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsEditReadonlyFields"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsRunReports", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsRunReports"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsViewSetup", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsViewSetup"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsTransferAnyEntity", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsTransferAnyEntity"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsNewReportBuilder", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsNewReportBuilder"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsActivateContract", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsActivateContract"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsImportLeads", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsImportLeads"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageLeads", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageLeads"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsTransferAnyLead", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsTransferAnyLead"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsViewAllData", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsViewAllData"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsEditPublicDocuments", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsEditPublicDocuments"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsViewEncryptedData", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsViewEncryptedData"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsEditBrandTemplates", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsEditBrandTemplates"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsEditHtmlTemplates", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsEditHtmlTemplates"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsChatterInternalUser", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsChatterInternalUser"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsDeleteActivatedContract", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsDeleteActivatedContract"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsChatterInviteExternalUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsChatterInviteExternalUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsSendSitRequests", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsSendSitRequests"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsApiUserOnly", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsApiUserOnly"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageRemoteAccess", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageRemoteAccess"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsCanUseNewDashboardBuilder", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsCanUseNewDashboardBuilder"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageCategories", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageCategories"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsConvertLeads", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsConvertLeads"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsPasswordNeverExpires", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsPasswordNeverExpires"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsUseTeamReassignWizards", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsUseTeamReassignWizards"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsInstallPackaging", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsInstallPackaging"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsPublishPackaging", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsPublishPackaging"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsChatterOwnGroups", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsChatterOwnGroups"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsEditOppLineItemUnitPrice", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsEditOppLineItemUnitPrice"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsCreatePackaging", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsCreatePackaging"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsBulkApiHardDelete", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsBulkApiHardDelete"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsInboundMigrationToolsUser", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsInboundMigrationToolsUser"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsSolutionImport", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsSolutionImport"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageCallCenters", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageCallCenters"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsOutboundMigrationToolsUser", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsOutboundMigrationToolsUser"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsViewContent", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsViewContent"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageEmailClientConfig", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageEmailClientConfig"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsEnableNotifications", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsEnableNotifications"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageDataIntegrations", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageDataIntegrations"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsDistributeFromPersWksp", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsDistributeFromPersWksp"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsViewDataCategories", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsViewDataCategories"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageDataCategories", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageDataCategories"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsAuthorApex", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsAuthorApex"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageMobile", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageMobile"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsApiEnabled", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsApiEnabled"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageCustomReportTypes", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageCustomReportTypes"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsEditCaseComments", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsEditCaseComments"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsTransferAnyCase", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsTransferAnyCase"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsContentAdministrator", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsContentAdministrator"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsCreateWorkspaces", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsCreateWorkspaces"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageContentPermissions", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageContentPermissions"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageContentProperties", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageContentProperties"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageContentTypes", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageContentTypes"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsScheduleJob", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsScheduleJob"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageAnalyticSnapshots", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageAnalyticSnapshots"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsScheduleReports", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsScheduleReports"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageBusinessHourHolidays", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageBusinessHourHolidays"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsCustomSidebarOnAllPages", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsCustomSidebarOnAllPages"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsViewMyTeamsDashboards", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsViewMyTeamsDashboards"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsModerateChatter", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsModerateChatter"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsResetPasswords", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsResetPasswords"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsFlowUFLRequired", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsFlowUFLRequired"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsCanInsertFeedSystemFields", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsCanInsertFeedSystemFields"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsEmailTemplateManagement", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsEmailTemplateManagement"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsEmailAdministration", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsEmailAdministration"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageChatterMessages", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageChatterMessages"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsAllowEmailIC", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsAllowEmailIC"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsChatterFileLink", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsChatterFileLink"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsForceTwoFactor", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsForceTwoFactor"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsViewCaseInteraction", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsViewCaseInteraction"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsManageAuthProviders", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsManageAuthProviders"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsViewAllUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsViewAllUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsConnectOrgToEnvironmentHub", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsConnectOrgToEnvironmentHub"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsTwoFactorApi", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsTwoFactorApi"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsDeleteTopics", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsDeleteTopics"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsEditTopics", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsEditTopics"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsCreateTopics", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsCreateTopics"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsAssignTopics", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsAssignTopics"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsIdentityEnabled", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsIdentityEnabled"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("MaximumPermissionsIdentityConnect", object(
                                            field("type", "boolean"),
                                            field("nativeName", "MaximumPermissionsIdentityConnect"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UsedLicenses", object(
                                            field("type", "int"),
                                            field("nativeName", "UsedLicenses"),
                                            field("nativeType", "int"),
                                            field("required", false)
                                    ))
                            ))
                    )),
                    field("PermissionSetLicenseAssign", object(
                            field("id", "PermissionSetLicenseAssign"),
                            field("type", "object"),
                            field("properties", object(
                                    field("Id", object(
                                            field("type", "id"),
                                            field("nativeName", "Id"),
                                            field("nativeType", "id"),
                                            field("required", false)
                                    )),
                                    field("IsDeleted", object(
                                            field("type", "boolean"),
                                            field("nativeName", "IsDeleted"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("CreatedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "CreatedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("CreatedById", object(
                                            field("type", "reference"),
                                            field("nativeName", "CreatedById"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("LastModifiedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "LastModifiedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("LastModifiedById", object(
                                            field("type", "reference"),
                                            field("nativeName", "LastModifiedById"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("SystemModstamp", object(
                                            field("type", "datetime"),
                                            field("nativeName", "SystemModstamp"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("PermissionSetLicenseId", object(
                                            field("type", "reference"),
                                            field("nativeName", "PermissionSetLicenseId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("AssigneeId", object(
                                            field("type", "reference"),
                                            field("nativeName", "AssigneeId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    ))
                            ))
                    )),
                    field("Profile", object(
                            field("id", "Profile"),
                            field("type", "object"),
                            field("properties", object(
                                    field("Id", object(
                                            field("type", "id"),
                                            field("nativeName", "Id"),
                                            field("nativeType", "id"),
                                            field("required", false)
                                    )),
                                    field("Name", object(
                                            field("type", "string"),
                                            field("nativeName", "Name"),
                                            field("nativeType", "string"),
                                            field("required", true)
                                    )),
                                    field("PermissionsEmailSingle", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEmailSingle"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEmailMass", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEmailMass"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditTask", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditTask"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditEvent", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditEvent"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsExportReport", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsExportReport"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsImportPersonal", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsImportPersonal"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsDataExport", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsDataExport"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditPublicTemplates", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditPublicTemplates"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsModifyAllData", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsModifyAllData"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageCases", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageCases"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsMassInlineEdit", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsMassInlineEdit"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageSolutions", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageSolutions"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsCustomizeApplication", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsCustomizeApplication"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditReadonlyFields", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditReadonlyFields"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsRunReports", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsRunReports"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsViewSetup", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsViewSetup"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsTransferAnyEntity", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsTransferAnyEntity"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsNewReportBuilder", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsNewReportBuilder"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsActivateContract", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsActivateContract"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsImportLeads", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsImportLeads"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageLeads", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageLeads"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsTransferAnyLead", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsTransferAnyLead"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsViewAllData", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsViewAllData"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditPublicDocuments", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditPublicDocuments"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsViewEncryptedData", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsViewEncryptedData"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditBrandTemplates", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditBrandTemplates"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditHtmlTemplates", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditHtmlTemplates"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsChatterInternalUser", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsChatterInternalUser"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsDeleteActivatedContract", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsDeleteActivatedContract"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsChatterInviteExternalUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsChatterInviteExternalUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsSendSitRequests", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsSendSitRequests"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsApiUserOnly", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsApiUserOnly"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageRemoteAccess", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageRemoteAccess"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsCanUseNewDashboardBuilder", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsCanUseNewDashboardBuilder"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageCategories", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageCategories"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsConvertLeads", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsConvertLeads"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsPasswordNeverExpires", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsPasswordNeverExpires"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsUseTeamReassignWizards", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsUseTeamReassignWizards"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsInstallMultiforce", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsInstallMultiforce"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsPublishMultiforce", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsPublishMultiforce"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsChatterOwnGroups", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsChatterOwnGroups"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditOppLineItemUnitPrice", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditOppLineItemUnitPrice"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsCreateMultiforce", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsCreateMultiforce"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsBulkApiHardDelete", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsBulkApiHardDelete"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsInboundMigrationToolsUser", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsInboundMigrationToolsUser"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsSolutionImport", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsSolutionImport"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageCallCenters", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageCallCenters"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsOutboundMigrationToolsUser", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsOutboundMigrationToolsUser"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsViewContent", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsViewContent"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageEmailClientConfig", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageEmailClientConfig"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEnableNotifications", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEnableNotifications"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageDataIntegrations", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageDataIntegrations"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsDistributeFromPersWksp", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsDistributeFromPersWksp"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsViewDataCategories", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsViewDataCategories"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageDataCategories", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageDataCategories"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsAuthorApex", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsAuthorApex"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageMobile", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageMobile"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsApiEnabled", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsApiEnabled"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageCustomReportTypes", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageCustomReportTypes"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditCaseComments", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditCaseComments"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsTransferAnyCase", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsTransferAnyCase"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsContentAdministrator", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsContentAdministrator"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsCreateWorkspaces", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsCreateWorkspaces"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageContentPermissions", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageContentPermissions"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageContentProperties", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageContentProperties"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageContentTypes", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageContentTypes"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsScheduleJob", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsScheduleJob"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageAnalyticSnapshots", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageAnalyticSnapshots"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsScheduleReports", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsScheduleReports"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageBusinessHourHolidays", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageBusinessHourHolidays"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsCustomSidebarOnAllPages", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsCustomSidebarOnAllPages"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsViewMyTeamsDashboards", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsViewMyTeamsDashboards"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsModerateChatter", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsModerateChatter"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsResetPasswords", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsResetPasswords"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsFlowUFLRequired", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsFlowUFLRequired"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsCanInsertFeedSystemFields", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsCanInsertFeedSystemFields"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEmailTemplateManagement", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEmailTemplateManagement"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEmailAdministration", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEmailAdministration"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageChatterMessages", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageChatterMessages"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsAllowEmailIC", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsAllowEmailIC"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsChatterFileLink", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsChatterFileLink"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsForceTwoFactor", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsForceTwoFactor"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsViewCaseInteraction", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsViewCaseInteraction"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsManageAuthProviders", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsManageAuthProviders"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsViewAllUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsViewAllUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsConnectOrgToEnvironmentHub", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsConnectOrgToEnvironmentHub"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsTwoFactorApi", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsTwoFactorApi"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsDeleteTopics", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsDeleteTopics"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsEditTopics", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsEditTopics"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsCreateTopics", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsCreateTopics"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsAssignTopics", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsAssignTopics"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsIdentityEnabled", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsIdentityEnabled"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("PermissionsIdentityConnect", object(
                                            field("type", "boolean"),
                                            field("nativeName", "PermissionsIdentityConnect"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserLicenseId", object(
                                            field("type", "reference"),
                                            field("nativeName", "UserLicenseId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("UserType", object(
                                            field("type", "picklist"),
                                            field("nativeName", "UserType"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("CreatedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "CreatedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("CreatedById", object(
                                            field("type", "reference"),
                                            field("nativeName", "CreatedById"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("LastModifiedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "LastModifiedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("LastModifiedById", object(
                                            field("type", "reference"),
                                            field("nativeName", "LastModifiedById"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("SystemModstamp", object(
                                            field("type", "datetime"),
                                            field("nativeName", "SystemModstamp"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("Description", object(
                                            field("type", "string"),
                                            field("nativeName", "Description"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("LastViewedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "LastViewedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("LastReferencedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "LastReferencedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    ))
                            ))
                    )),
                    field("User", object(
                            field("id", "User"),
                            field("type", "object"),
                            field("properties", object(
                                    field("Id", object(
                                            field("type", "id"),
                                            field("nativeName", "Id"),
                                            field("nativeType", "id"),
                                            field("required", false)
                                    )),
                                    field("Username", object(
                                            field("type", "string"),
                                            field("nativeName", "Username"),
                                            field("nativeType", "string"),
                                            field("required", true)
                                    )),
                                    field("LastName", object(
                                            field("type", "string"),
                                            field("nativeName", "LastName"),
                                            field("nativeType", "string"),
                                            field("required", true)
                                    )),
                                    field("FirstName", object(
                                            field("type", "string"),
                                            field("nativeName", "FirstName"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("Name", object(
                                            field("type", "string"),
                                            field("nativeName", "Name"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("CompanyName", object(
                                            field("type", "string"),
                                            field("nativeName", "CompanyName"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("Division", object(
                                            field("type", "string"),
                                            field("nativeName", "Division"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("Department", object(
                                            field("type", "string"),
                                            field("nativeName", "Department"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("Title", object(
                                            field("type", "string"),
                                            field("nativeName", "Title"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("Street", object(
                                            field("type", "textarea"),
                                            field("nativeName", "Street"),
                                            field("nativeType", "textarea"),
                                            field("required", false)
                                    )),
                                    field("City", object(
                                            field("type", "string"),
                                            field("nativeName", "City"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("State", object(
                                            field("type", "string"),
                                            field("nativeName", "State"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("PostalCode", object(
                                            field("type", "string"),
                                            field("nativeName", "PostalCode"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("Country", object(
                                            field("type", "string"),
                                            field("nativeName", "Country"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("Latitude", object(
                                            field("type", "double"),
                                            field("nativeName", "Latitude"),
                                            field("nativeType", "double"),
                                            field("required", false)
                                    )),
                                    field("Longitude", object(
                                            field("type", "double"),
                                            field("nativeName", "Longitude"),
                                            field("nativeType", "double"),
                                            field("required", false)
                                    )),
                                    field("Email", object(
                                            field("type", "email"),
                                            field("nativeName", "Email"),
                                            field("nativeType", "email"),
                                            field("required", true)
                                    )),
                                    field("EmailPreferencesAutoBcc", object(
                                            field("type", "boolean"),
                                            field("nativeName", "EmailPreferencesAutoBcc"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("EmailPreferencesAutoBccStayInTouch", object(
                                            field("type", "boolean"),
                                            field("nativeName", "EmailPreferencesAutoBccStayInTouch"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("EmailPreferencesStayInTouchReminder", object(
                                            field("type", "boolean"),
                                            field("nativeName", "EmailPreferencesStayInTouchReminder"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("SenderEmail", object(
                                            field("type", "email"),
                                            field("nativeName", "SenderEmail"),
                                            field("nativeType", "email"),
                                            field("required", false)
                                    )),
                                    field("SenderName", object(
                                            field("type", "string"),
                                            field("nativeName", "SenderName"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("Signature", object(
                                            field("type", "string"),
                                            field("nativeName", "Signature"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("StayInTouchSubject", object(
                                            field("type", "string"),
                                            field("nativeName", "StayInTouchSubject"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("StayInTouchSignature", object(
                                            field("type", "string"),
                                            field("nativeName", "StayInTouchSignature"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("StayInTouchNote", object(
                                            field("type", "string"),
                                            field("nativeName", "StayInTouchNote"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("Phone", object(
                                            field("type", "phone"),
                                            field("nativeName", "Phone"),
                                            field("nativeType", "phone"),
                                            field("required", false)
                                    )),
                                    field("Fax", object(
                                            field("type", "phone"),
                                            field("nativeName", "Fax"),
                                            field("nativeType", "phone"),
                                            field("required", false)
                                    )),
                                    field("MobilePhone", object(
                                            field("type", "phone"),
                                            field("nativeName", "MobilePhone"),
                                            field("nativeType", "phone"),
                                            field("required", false)
                                    )),
                                    field("Alias", object(
                                            field("type", "string"),
                                            field("nativeName", "Alias"),
                                            field("nativeType", "string"),
                                            field("required", true)
                                    )),
                                    field("CommunityNickname", object(
                                            field("type", "string"),
                                            field("nativeName", "CommunityNickname"),
                                            field("nativeType", "string"),
                                            field("required", true)
                                    )),
                                    field("IsActive", object(
                                            field("type", "boolean"),
                                            field("nativeName", "IsActive"),
                                            field("nativeType", "boolean"),
                                            field("required", true)
                                    )),
                                    field("TimeZoneSidKey", object(
                                            field("type", "picklist"),
                                            field("nativeName", "TimeZoneSidKey"),
                                            field("nativeType", "picklist"),
                                            field("required", true)
                                    )),
                                    field("UserRoleId", object(
                                            field("type", "reference"),
                                            field("nativeName", "UserRoleId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("LocaleSidKey", object(
                                            field("type", "picklist"),
                                            field("nativeName", "LocaleSidKey"),
                                            field("nativeType", "picklist"),
                                            field("required", true)
                                    )),
                                    field("ReceivesInfoEmails", object(
                                            field("type", "boolean"),
                                            field("nativeName", "ReceivesInfoEmails"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("ReceivesAdminInfoEmails", object(
                                            field("type", "boolean"),
                                            field("nativeName", "ReceivesAdminInfoEmails"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("EmailEncodingKey", object(
                                            field("type", "picklist"),
                                            field("nativeName", "EmailEncodingKey"),
                                            field("nativeType", "picklist"),
                                            field("required", true)
                                    )),
                                    field("ProfileId", object(
                                            field("type", "reference"),
                                            field("nativeName", "ProfileId"),
                                            field("nativeType", "reference"),
                                            field("required", true)
                                    )),
                                    field("UserType", object(
                                            field("type", "picklist"),
                                            field("nativeName", "UserType"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("LanguageLocaleKey", object(
                                            field("type", "picklist"),
                                            field("nativeName", "LanguageLocaleKey"),
                                            field("nativeType", "picklist"),
                                            field("required", true)
                                    )),
                                    field("EmployeeNumber", object(
                                            field("type", "string"),
                                            field("nativeName", "EmployeeNumber"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("DelegatedApproverId", object(
                                            field("type", "reference"),
                                            field("nativeName", "DelegatedApproverId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("ManagerId", object(
                                            field("type", "reference"),
                                            field("nativeName", "ManagerId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("LastLoginDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "LastLoginDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("LastPasswordChangeDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "LastPasswordChangeDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("CreatedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "CreatedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("CreatedById", object(
                                            field("type", "reference"),
                                            field("nativeName", "CreatedById"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("LastModifiedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "LastModifiedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("LastModifiedById", object(
                                            field("type", "reference"),
                                            field("nativeName", "LastModifiedById"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("SystemModstamp", object(
                                            field("type", "datetime"),
                                            field("nativeName", "SystemModstamp"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("OfflineTrialExpirationDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "OfflineTrialExpirationDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("OfflinePdaTrialExpirationDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "OfflinePdaTrialExpirationDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("UserPermissionsMarketingUser", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPermissionsMarketingUser"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPermissionsOfflineUser", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPermissionsOfflineUser"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPermissionsAvantgoUser", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPermissionsAvantgoUser"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPermissionsCallCenterAutoLogin", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPermissionsCallCenterAutoLogin"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPermissionsMobileUser", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPermissionsMobileUser"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPermissionsSFContentUser", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPermissionsSFContentUser"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPermissionsChatterAnswersUser", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPermissionsChatterAnswersUser"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("ForecastEnabled", object(
                                            field("type", "boolean"),
                                            field("nativeName", "ForecastEnabled"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesActivityRemindersPopup", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesActivityRemindersPopup"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesEventRemindersCheckboxDefault", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesEventRemindersCheckboxDefault"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesTaskRemindersCheckboxDefault", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesTaskRemindersCheckboxDefault"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesReminderSoundOff", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesReminderSoundOff"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesDisableAllFeedsEmail", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesDisableAllFeedsEmail"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesDisableFollowersEmail", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesDisableFollowersEmail"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesDisableProfilePostEmail", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesDisableProfilePostEmail"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesDisableChangeCommentEmail", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesDisableChangeCommentEmail"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesDisableLaterCommentEmail", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesDisableLaterCommentEmail"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesDisProfPostCommentEmail", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesDisProfPostCommentEmail"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesApexPagesDeveloperMode", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesApexPagesDeveloperMode"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesHideCSNGetChatterMobileTask", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesHideCSNGetChatterMobileTask"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesDisableMentionsPostEmail", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesDisableMentionsPostEmail"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesDisMentionsCommentEmail", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesDisMentionsCommentEmail"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesHideCSNDesktopTask", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesHideCSNDesktopTask"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesHideChatterOnboardingSplash", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesHideChatterOnboardingSplash"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesHideSecondChatterOnboardingSplash", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesHideSecondChatterOnboardingSplash"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesDisCommentAfterLikeEmail", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesDisCommentAfterLikeEmail"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesDisableLikeEmail", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesDisableLikeEmail"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesDisableMessageEmail", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesDisableMessageEmail"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesOptOutOfTouch", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesOptOutOfTouch"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesDisableBookmarkEmail", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesDisableBookmarkEmail"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesDisableSharePostEmail", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesDisableSharePostEmail"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesEnableAutoSubForFeeds", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesEnableAutoSubForFeeds"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesDisableFileShareNotificationsForApi", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesDisableFileShareNotificationsForApi"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowTitleToExternalUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowTitleToExternalUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowManagerToExternalUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowManagerToExternalUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowEmailToExternalUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowEmailToExternalUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowWorkPhoneToExternalUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowWorkPhoneToExternalUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowMobilePhoneToExternalUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowMobilePhoneToExternalUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowFaxToExternalUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowFaxToExternalUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowStreetAddressToExternalUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowStreetAddressToExternalUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowCityToExternalUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowCityToExternalUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowStateToExternalUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowStateToExternalUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowPostalCodeToExternalUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowPostalCodeToExternalUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowCountryToExternalUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowCountryToExternalUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowProfilePicToGuestUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowProfilePicToGuestUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowTitleToGuestUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowTitleToGuestUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowCityToGuestUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowCityToGuestUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowStateToGuestUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowStateToGuestUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowPostalCodeToGuestUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowPostalCodeToGuestUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesShowCountryToGuestUsers", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesShowCountryToGuestUsers"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("UserPreferencesHideS1BrowserUI", object(
                                            field("type", "boolean"),
                                            field("nativeName", "UserPreferencesHideS1BrowserUI"),
                                            field("nativeType", "boolean"),
                                            field("required", false)
                                    )),
                                    field("ContactId", object(
                                            field("type", "reference"),
                                            field("nativeName", "ContactId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("AccountId", object(
                                            field("type", "reference"),
                                            field("nativeName", "AccountId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("CallCenterId", object(
                                            field("type", "reference"),
                                            field("nativeName", "CallCenterId"),
                                            field("nativeType", "reference"),
                                            field("required", false)
                                    )),
                                    field("Extension", object(
                                            field("type", "phone"),
                                            field("nativeName", "Extension"),
                                            field("nativeType", "phone"),
                                            field("required", false)
                                    )),
                                    field("FederationIdentifier", object(
                                            field("type", "string"),
                                            field("nativeName", "FederationIdentifier"),
                                            field("nativeType", "string"),
                                            field("required", false)
                                    )),
                                    field("AboutMe", object(
                                            field("type", "textarea"),
                                            field("nativeName", "AboutMe"),
                                            field("nativeType", "textarea"),
                                            field("required", false)
                                    )),
                                    field("FullPhotoUrl", object(
                                            field("type", "url"),
                                            field("nativeName", "FullPhotoUrl"),
                                            field("nativeType", "url"),
                                            field("required", false)
                                    )),
                                    field("SmallPhotoUrl", object(
                                            field("type", "url"),
                                            field("nativeName", "SmallPhotoUrl"),
                                            field("nativeType", "url"),
                                            field("required", false)
                                    )),
                                    field("DigestFrequency", object(
                                            field("type", "picklist"),
                                            field("nativeName", "DigestFrequency"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("DefaultGroupNotificationFrequency", object(
                                            field("type", "picklist"),
                                            field("nativeName", "DefaultGroupNotificationFrequency"),
                                            field("nativeType", "picklist"),
                                            field("required", false)
                                    )),
                                    field("LastViewedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "LastViewedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    )),
                                    field("LastReferencedDate", object(
                                            field("type", "datetime"),
                                            field("nativeName", "LastReferencedDate"),
                                            field("nativeType", "datetime"),
                                            field("required", false)
                                    ))
                            ))
                    ))
            ));

    public static JsonValue getObjectSchema() {
        return objectSchema;
    }

    public static Set<String> getObjectProperties(String type) {
        return getObjectSchema().get(type).get("properties").keys();
    }
}
