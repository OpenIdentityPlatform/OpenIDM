/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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

/*global require, define, QUnit, $, module */

define([
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/admin/mapping/association/dataAssociationManagement/ChangeAssociationDialog",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
    "org/forgerock/openidm/ui/admin/util/LinkQualifierUtils",
    "org/forgerock/openidm/ui/admin/mapping/association/DataAssociationManagementView",
    "../mocks/correlation"
], function (constants,
             router,
             eventManager,
             ChangeAssociationDialog,
             ConnectorDelegate,
             MappingBaseView,
             LinkQualifierUtils,
             DataAssociationManagementView,
             correlation) {

    return {
        executeAll: function (server) {
            module('Correlation UI Functions');

            QUnit.asyncTest("Correlation View", function () {
                var mappingDetails = {"sync":{"mappings":[{"name":"systemXmlfileAccounts_managedUser","source":"system/xmlfile/account","target":"managed/user","correlationQuery":{"type":"text/javascript","source":"var query = {'_queryId' : 'for-userName', 'uid' : source.name};query;"},"properties":[{"source":"email","target":"mail"},{"source":"firstname","target":"givenName"},{"source":"lastname","target":"sn"},{"source":"description","target":"description"},{"source":"_id","target":"_id"},{"source":"name","target":"userName"},{"source":"password","target":"password"},{"source":"mobileTelephoneNumber","target":"telephoneNumber"},{"source":"roles","transform":{"type":"text/javascript","source":"source.split(',')"},"target":"roles"}],"policies":[{"situation":"CONFIRMED","action":"UPDATE"},{"situation":"FOUND","action":"IGNORE"},{"situation":"ABSENT","action":"CREATE"},{"situation":"AMBIGUOUS","action":"IGNORE"},{"situation":"MISSING","action":"IGNORE"},{"situation":"SOURCE_MISSING","action":"IGNORE"},{"situation":"UNQUALIFIED","action":"IGNORE"},{"situation":"UNASSIGNED","action":"IGNORE"}]}]},"mapping":{"name":"systemXmlfileAccounts_managedUser","source":"system/xmlfile/account","target":"managed/user","correlationQuery":{"type":"text/javascript","source":"var query = {'_queryId' : 'for-userName', 'uid' : source.name};query;"},"properties":[{"source":"email","target":"mail"},{"source":"firstname","target":"givenName"},{"source":"lastname","target":"sn"},{"source":"description","target":"description"},{"source":"_id","target":"_id"},{"source":"name","target":"userName"},{"source":"password","target":"password"},{"source":"mobileTelephoneNumber","target":"telephoneNumber"},{"source":"roles","transform":{"type":"text/javascript","source":"source.split(',')"},"target":"roles"}],"policies":[{"situation":"CONFIRMED","action":"UPDATE"},{"situation":"FOUND","action":"IGNORE"},{"situation":"ABSENT","action":"CREATE"},{"situation":"AMBIGUOUS","action":"IGNORE"},{"situation":"MISSING","action":"IGNORE"},{"situation":"SOURCE_MISSING","action":"IGNORE"},{"situation":"UNQUALIFIED","action":"IGNORE"},{"situation":"UNASSIGNED","action":"IGNORE"}]},"mappingName":"systemXmlfileAccounts_managedUser"},
                    reconDetails = {"_id":"cba4a732-b648-47fc-a93f-1f8855a50727","mapping":"systemXmlfileAccounts_managedUser","state":"SUCCESS","stage":"COMPLETED_SUCCESS","stageDescription":"reconciliation completed.","progress":{"source":{"existing":{"processed":2,"total":"2"}},"target":{"existing":{"processed":0,"total":"0"},"created":2},"links":{"existing":{"processed":0,"total":"0"},"created":2}},"situationSummary":{"SOURCE_IGNORED":0,"MISSING":0,"FOUND":0,"AMBIGUOUS":0,"UNQUALIFIED":0,"CONFIRMED":0,"SOURCE_MISSING":0,"ABSENT":2,"TARGET_IGNORED":0,"UNASSIGNED":0,"FOUND_ALREADY_LINKED":0},"statusSummary":{"FAILURE":0,"SUCCESS":2},"parameters":{"object":{"sourceQuery":{"resourceName":"system/xmlfile/account","_queryId":"query-all-ids"},"targetQuery":{"resourceName":"managed/user","_queryId":"query-all-ids"}},"pointer":{"empty":true},"transformers":[],"set":false,"map":true,"string":false,"wrappedObject":{"sourceQuery":{"resourceName":"system/xmlfile/account","_queryId":"query-all-ids"},"targetQuery":{"resourceName":"managed/user","_queryId":"query-all-ids"}},"list":false,"number":false,"boolean":false,"null":false,"collection":false},"started":"2015-01-22T17:38:17.346Z","ended":"2015-01-22T17:38:17.588Z","duration":242};

                correlation(server);

                $("#qunit-fixture").append("<div id='analysisView'></div>")

                MappingBaseView.setRecon(reconDetails);
                MappingBaseView.setSyncCancelled(false);
                DataAssociationManagementView.setCurrentMapping(mappingDetails.sync.mappings[0]);
                DataAssociationManagementView.setSyncConfig(mappingDetails);


                ConnectorDelegate.connectorDelegateCache = {};

                DataAssociationManagementView.render({}, function () {
                    QUnit.ok(DataAssociationManagementView.$el.children().length > 0, "Analysis View properly rendered.");

                    QUnit.ok(DataAssociationManagementView.$el.find("#changeAssociation").is(":disabled") === true, "Change association properly disabled");

                    QUnit.ok(DataAssociationManagementView.$el.find("#singleRecordSync").is(":disabled") === true, "Single record sync button properly disabled");

                    QUnit.ok(DataAssociationManagementView.$el.find("#situationSelection option").length === 12, "Twelve situations selectable");

                    $("#analysisGridContainer #1").trigger("click");

                    QUnit.ok(DataAssociationManagementView.$el.find("#changeAssociation").is(":disabled") === false, "Change association properly enabled");

                    QUnit.ok(DataAssociationManagementView.$el.find("#singleRecordSync").is(":disabled") === false, "Single record sync button properly enabled");

                    QUnit.start();
                });
            });

            QUnit.asyncTest("Change Association Window", function () {
                var linkCheck = false,
                    stubbedLink,
                    dialogData = {"sourceObj":{"firstname":"Barbara","description":"Created By XML1","_id":"bjensen","mobileTelephoneNumber":"1234567","password":"TestPassw0rd#","lastname":"Jensen","roles":"openidm-authorized","email":"bjensen@example.com","name":"bjensen@example.com"},"sourceObjRep":"<span title=\"mail\" class=\"objectRepresentationHeader\">bjensen@example.com</span><br/><span title=\"givenName\" class=\"objectRepresentation\">Barbara</span><br/><span title=\"sn\" class=\"objectRepresentation\">Jensen</span><br/><span title=\"description\" class=\"objectRepresentation\">Created By XML1</span>","targetObj":{"_id":"bjensen","_rev":"1","mail":"bjensen@example.com","sn":"Jensen","description":"Created By XML1","roles":["openidm-authorized"],"telephoneNumber":"1234567","userName":"bjensen@example.com","givenName":"Barbara","password":{"$crypto":{"value":{"iv":"/4RSRnPywXegeBVcBtuvJQ==","data":"b7WowHq6jnePEXj+6D1ptg==","cipher":"AES/CBC/PKCS5Padding","key":"openidm-sym-default"},"type":"x-simple-encryption"}},"accountStatus":"active","lastPasswordSet":"","postalCode":"","stateProvince":"","passwordAttempts":"0","lastPasswordAttempt":"Tue Jan 20 2015 14:51:43 GMT-0800 (PST)","postalAddress":"","address2":"","country":"","city":"","effectiveRoles":["openidm-authorized"],"effectiveAssignments":null},"targetObjRep":"<span title=\"mail\" class=\"objectRepresentationHeader\">bjensen@example.com</span><br/><span title=\"givenName\" class=\"objectRepresentation\">Barbara</span><br/><span title=\"sn\" class=\"objectRepresentation\">Jensen</span><br/><span title=\"description\" class=\"objectRepresentation\">Created By XML1</span>","targetProps":{"0":"mail","1":"givenName","2":"sn","3":"description"},"ambiguousTargetObjectIds":"","recon":{"_id":"8afcffb2-d17d-490c-9782-487b36be3f7d","mapping":"systemXmlfileAccounts_managedUser","state":"SUCCESS","stage":"COMPLETED_SUCCESS","stageDescription":"reconciliation completed.","progress":{"source":{"existing":{"processed":2,"total":"2"}},"target":{"existing":{"processed":0,"total":"0"},"created":2},"links":{"existing":{"processed":0,"total":"0"},"created":2}},"situationSummary":{"SOURCE_IGNORED":0,"MISSING":0,"FOUND":0,"AMBIGUOUS":0,"UNQUALIFIED":0,"CONFIRMED":0,"SOURCE_MISSING":0,"ABSENT":2,"TARGET_IGNORED":0,"UNASSIGNED":0,"FOUND_ALREADY_LINKED":0},"statusSummary":{"FAILURE":0,"SUCCESS":2},"parameters":{"object":{"sourceQuery":{"resourceName":"system/xmlfile/account","_queryId":"query-all-ids"},"targetQuery":{"resourceName":"managed/user","_queryId":"query-all-ids"}},"pointer":{"empty":true},"transformers":[],"set":false,"map":true,"string":false,"collection":false,"wrappedObject":{"sourceQuery":{"resourceName":"system/xmlfile/account","_queryId":"query-all-ids"},"targetQuery":{"resourceName":"managed/user","_queryId":"query-all-ids"}},"list":false,"number":false,"boolean":false,"null":false},"started":"2015-01-20T22:51:43.001Z","ended":"2015-01-20T22:51:43.295Z","duration":294},"linkTypes":[]};

                correlation(server);
                MappingBaseView.setCurrentMapping({"name": "test"});
                LinkQualifierUtils.setLinkQualifier(["default"], "test");

                stubbedLink = sinon.stub(ChangeAssociationDialog, "linkObject", function(){
                    linkCheck = true;
                });

                ChangeAssociationDialog.render(dialogData, function () {
                    QUnit.ok($("#changeAssociationDialog").length > 0, "Change association window displayed");

                    QUnit.ok($("#linkTypeSelect").is(":disabled") === false, "Link Qualifier select disabled");

                    QUnit.ok($("#linkTypeSelect option[value=default]").length === 1, "Link Qualifier displayed with default option");

                    ChangeAssociationDialog.$el.find("#search_results li").trigger("click");
                    ChangeAssociationDialog.$el.find("#linkObjectBtn").trigger("click");

                    QUnit.ok(linkCheck === true, "LinkObject function called from DOM events");

                    stubbedLink.restore();

                    QUnit.start();
                });
            });

        }
    };
});
