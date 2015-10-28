/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

/*global require, define, QUnit, $, module */

define([
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
    "org/forgerock/openidm/ui/admin/mapping/properties/LinkQualifiersView",
    "../mocks/mapping/linkQualifierProperties"
], function (constants,
             router,
             eventManager,
             MappingBaseView,
             PropertiesLinkQualifier,
             linkQualifierProperties) {

    return {
        executeAll: function (server) {
            var syncDetails = {"sync":{"mappings":[{"name":"systemXmlfileAccounts_managedUser","source":"system/xmlfile/account","target":"managed/user","correlationQuery":{"type":"text/javascript","source":"var query = {'_queryId' : 'for-userName', 'uid' : source.name};query;"},"properties":[{"source":"email","target":"mail"},{"source":"firstname","target":"givenName"},{"source":"lastname","target":"sn"},{"source":"description","target":"description"},{"source":"_id","target":"_id"},{"source":"name","target":"userName"},{"source":"password","target":"password"},{"source":"mobileTelephoneNumber","target":"telephoneNumber"},{"source":"roles","transform":{"type":"text/javascript","source":"source.split(',')"},"target":"roles"}],"policies":[{"situation":"CONFIRMED","action":"UPDATE"},{"situation":"FOUND","action":"IGNORE"},{"situation":"ABSENT","action":"CREATE"},{"situation":"AMBIGUOUS","action":"IGNORE"},{"situation":"MISSING","action":"IGNORE"},{"situation":"SOURCE_MISSING","action":"IGNORE"},{"situation":"UNQUALIFIED","action":"IGNORE"},{"situation":"UNASSIGNED","action":"IGNORE"}]}]},"mapping":{"name":"systemXmlfileAccounts_managedUser","source":"system/xmlfile/account","target":"managed/user","correlationQuery":{"type":"text/javascript","source":"var query = {'_queryId' : 'for-userName', 'uid' : source.name};query;"},"properties":[{"source":"email","target":"mail"},{"source":"firstname","target":"givenName"},{"source":"lastname","target":"sn"},{"source":"description","target":"description"},{"source":"_id","target":"_id"},{"source":"name","target":"userName"},{"source":"password","target":"password"},{"source":"mobileTelephoneNumber","target":"telephoneNumber"},{"source":"roles","transform":{"type":"text/javascript","source":"source.split(',')"},"target":"roles"}],"policies":[{"situation":"CONFIRMED","action":"UPDATE"},{"situation":"FOUND","action":"IGNORE"},{"situation":"ABSENT","action":"CREATE"},{"situation":"AMBIGUOUS","action":"IGNORE"},{"situation":"MISSING","action":"IGNORE"},{"situation":"SOURCE_MISSING","action":"IGNORE"},{"situation":"UNQUALIFIED","action":"IGNORE"},{"situation":"UNASSIGNED","action":"IGNORE"}]},"mappingName":"systemXmlfileAccounts_managedUser"};


                module('Link Qualifier Properties UI Functions');

            QUnit.asyncTest("Link Qualifier Properties View", function () {
                var editor,
                    errorMessageStub;

                PropertiesLinkQualifier.setCurrentMapping(syncDetails.sync.mappings[0]);
                PropertiesLinkQualifier.setSyncConfig(syncDetails);

                linkQualifierProperties(server);

                $("#qunit-fixture").append("<div id='mappingLinkQualifiers'></div>");

                PropertiesLinkQualifier.render("systemXmlfileAccounts_managedUser", _.bind(function(){
                    QUnit.ok(PropertiesLinkQualifier.$el.find("#linkQualifierTabs a").length === 2, "Both dynamic and declarative link qualifier methods are available");

                    QUnit.ok(PropertiesLinkQualifier.$el.find("#staticLinkQualifierList .linkQualifier").html() === "default", "Correctly loaded default declarative link qualifiers");

                    QUnit.ok(PropertiesLinkQualifier.$el.find("#staticLinkQualifierList button:enabled").length === 0, "Declarative link qualifier not deletable");
                    //newLinkQualifier
                    PropertiesLinkQualifier.$el.find(".newLinkQualifier").val("test");
                    PropertiesLinkQualifier.$el.find(".addLinkQualifier").trigger("click");

                    QUnit.ok(PropertiesLinkQualifier.$el.find("#staticLinkQualifierList button:nth-child(2) .linkQualifier").html() === "test", "Added a declarative link qualifier");

                    QUnit.ok(PropertiesLinkQualifier.$el.find("#staticLinkQualifierList button:enabled").length === 2, "Both declarative link qualifiers are deletable");

                    PropertiesLinkQualifier.$el.find(".removeLinkQualifier:nth-child(2)").trigger("click");

                    QUnit.ok(PropertiesLinkQualifier.$el.find("#staticLinkQualifierList button").length === 1, "Removed declarative link qualifier");

                    PropertiesLinkQualifier.$el.find("#scriptQualifierTab").trigger("click");

                    QUnit.ok(PropertiesLinkQualifier.$el.find("#scriptLinkQualifier").hasClass("active"), "Successfully changed to Dynamic tab");

                    QUnit.ok(PropertiesLinkQualifier.$el.find(".CodeMirror").length === 1, "Code mirror loaded for dynamic tab");

                    PropertiesLinkQualifier.$el.find(".inline-code input").trigger("click");

                    editor = PropertiesLinkQualifier.linkQualifierScript.getInlineEditor();

                    editor.setValue("test");

                    PropertiesLinkQualifier.$el.find(".linkQualifierSave").trigger("click");

                    errorMessageStub = sinon.stub(PropertiesLinkQualifier, "showErrorMessage", function(message){
                        errorMessageStub.restore();
                        PropertiesLinkQualifier.showErrorMessage(message);


                        QUnit.ok(PropertiesLinkQualifier.$el.find("#badLinkQualifierScript:visible").length === 1, "Error message successfully displayed");

                        QUnit.start();
                    });

                }, this));
            });
        }
    };
});
