/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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

/*global require, define, QUnit, $ */

define([
    "sinon",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/admin/sync/SituationPolicyView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "../mocks/reconPolicyMappings"

], function (sinon, eventManager, SituationPolicyView, ConfigDelegate, reconPolicyMappings) {

    $("body").append("<div id='policyPattern'></div>");

    return {
        executeAll: function (server) {
            // These three constants come from MappingBaseView and are the arguments passed into the SituationPolicyView render function.
            // Should those arguments change than these need to be updated.
            var sync = [{"target":"managed/user","properties":[{"target":"displayName","source":"cn"},{"target":"description","source":"description"},{"target":"givenName","source":"givenName"},{"target":"mail","source":"mail"},{"target":"telephoneNumber","source":"telephoneNumber"},{"target":"sn","source":"sn"},{"target":"userName","source":"uid"}],"source":"system/ldap/account","policies":[{"action":"UPDATE","situation":"CONFIRMED"},{"action":"UPDATE","situation":"FOUND"},{"action":"CREATE","situation":"ABSENT"},{"action":"EXCEPTION","situation":"AMBIGUOUS"},{"action":"CREATE","situation":"MISSING"},{"action":"DELETE","situation":"SOURCE_MISSING"},{"action":"IGNORE","situation":"UNQUALIFIED"},{"action":"IGNORE","situation":"UNASSIGNED"}],"name":"systemLdapAccounts_managedUser"},{"target":"system/ldap/account","links":"systemLdapAccounts_managedUser","properties":[{"target":"givenName","source":"givenName"},{"target":"sn","source":"sn"},{"target":"cn","source":"","transform":{"type":"text/javascript","source":"source.displayName || (source.givenName + ' ' + source.sn);"}},{"target":"uid","source":"userName"},{"target":"description","source":"description","condition":{"type":"text/javascript","source":"!!object.description"}},{"target":"mail","source":"mail"},{"target":"userPassword","transform":{"type":"text/javascript","source":"openidm.decrypt(source);"},"source":"password","condition":{"type":"text/javascript","source":"object.password != null"}},{"target":"telephoneNumber","source":"telephoneNumber","condition":{"type":"text/javascript","source":"!!object.telephoneNumber"}}],"source":"managed/user","onCreate":{"type":"text/javascript","source":"target.dn = 'uid=' + source.userName + ',ou=People,dc=example,dc=com';"},"policies":[{"action":"UPDATE","situation":"CONFIRMED"},{"action":"LINK","situation":"FOUND"},{"action":"CREATE","situation":"ABSENT"},{"action":"IGNORE","situation":"AMBIGUOUS"},{"action":"IGNORE","situation":"MISSING"},{"action":"DELETE","situation":"SOURCE_MISSING"},{"action":"IGNORE","situation":"UNQUALIFIED"},{"action":"IGNORE","situation":"UNASSIGNED"}],"name":"managedUser_systemLdapAccounts"}],
                mapping = {"target":"managed/user","properties":[{"target":"displayName","source":"cn"},{"target":"description","source":"description"},{"target":"givenName","source":"givenName"},{"target":"mail","source":"mail"},{"target":"telephoneNumber","source":"telephoneNumber"},{"target":"sn","source":"sn"},{"target":"userName","source":"uid"}],"source":"system/ldap/account","policies":[{"action":"UPDATE","situation":"CONFIRMED"},{"action":"UPDATE","situation":"FOUND"},{"action":"CREATE","situation":"ABSENT"},{"action":"EXCEPTION","situation":"AMBIGUOUS"},{"action":"CREATE","situation":"MISSING"},{"action":"DELETE","situation":"SOURCE_MISSING"},{"action":"IGNORE","situation":"UNQUALIFIED"},{"action":"IGNORE","situation":"UNASSIGNED"}],"name":"systemLdapAccounts_managedUser"},
                mappingName = "systemLdapAccounts_managedUser";

            reconPolicyMappings(server);

            module("Admin Mapping tab, Recon sub-tab tests");

            QUnit.asyncTest("Situational Policy Renders", function () {
                QUnit.equal(SituationPolicyView.$el.children().length, 0, "There are no children in the container before render.");

                SituationPolicyView.render({sync: sync, mapping: mapping, mappingName: mappingName}, function() {
                    QUnit.ok(SituationPolicyView.$el.children().length > 0, "After rendering there are now child elements");

                    // -1 because of the source row used to create the others is still on the page but hidden
                    QUnit.equal(SituationPolicyView.$el.find(".situationRow").length-1,
                        _.size(SituationPolicyView.model.allPatterns["Default Actions"].policies),
                        "All situations are present");

                    QUnit.ok(SituationPolicyView.$el.find(".situationRow .action")[0].length >= 1,
                        "All actions are present");

                    QUnit.equal(_.size(SituationPolicyView.model.allPatterns),
                        SituationPolicyView.$el.find("#policyPatterns option").length,
                        "Patterns rendered are the same as the stored patterns");

                    var messages = true, order = true;
                    _.each(SituationPolicyView.model.allPatterns["Default Actions"].policies, function(situation, index) {
                        if (situation.note !== SituationPolicyView.$el.find(".situationRow .info:eq("+index+")").attr("data-title")) {
                            messages = false;
                        }

                        if (SituationPolicyView.model.lookup.situations[situation.situation] !== SituationPolicyView.$el.find(".situationRow label:eq("+index+")").text()) {
                            order = false;
                        }
                    });

                    QUnit.ok(messages, "The help tooltips have been set");
                    QUnit.ok(order, "The order of the situations match the order of the Default Action object");

                    QUnit.equal(SituationPolicyView.$el.find(".situationRow .failure-display").length, 7, "There are 7 situations with error classes");
                    QUnit.equal(SituationPolicyView.$el.find(".situationRow .warning-display").length, 3, "There are 2 situations with warning classes");
                    QUnit.equal(SituationPolicyView.$el.find(".situationRow .success-display").length, 3, "There are 3 situations with success classes");

                    var readOnly = true, defaultActions = true;
                    SituationPolicyView.$el.find("#policyPatterns").val("Read-only").change();
                    _.each(SituationPolicyView.$el.find(".situationRow"), function(row, index) {
                        if ($(row).find(".action").val() !== "ASYNC"  && index < SituationPolicyView.model.allPatterns["Read-only"].policies.length) {
                            readOnly = false
                        }
                    });
                    QUnit.ok(readOnly, "When the pattern changes to Read-Only all actions are set to Async");

                    SituationPolicyView.$el.find("#policyPatterns").val("Default Actions").change();
                    _.each(SituationPolicyView.$el.find(".situationRow"), function(row, index) {
                        if ($(row).find(".action :selected").text().indexOf("★") < 0  && index < SituationPolicyView.model.allPatterns["Read-only"].policies.length) {
                            defaultActions = false
                        }
                    });
                    QUnit.ok(defaultActions, "When the pattern changes to Default-Actions all actions are set to the starred value");

                    SituationPolicyView.$el.find(".ABSENT .action").val("LINK");

                    var callback = sinon.spy(eventManager, "sendEvent"),
                        mappingUpdated = false,
                        deferredRead = $.Deferred(),
                        deferredWrite = $.Deferred(),
                        stubbedRead = sinon.stub(ConfigDelegate, "readEntity", function () {
                            return deferredRead.promise();
                        }),
                        stubbedWrite = sinon.stub(ConfigDelegate, "updateEntity", function (name, data) {
                            _.each(data.mappings, function(mapping) {
                                if(mapping.name === mappingName) {
                                    _.each(mapping.policies, function(policy) {
                                       if (policy.action === "LINK" && policy.situation === "ABSENT") {
                                           mappingUpdated = true;
                                       }
                                    });
                                }
                            });

                            QUnit.ok(mappingUpdated, "The policy mapping was updated properly.");

                            return deferredWrite.promise();
                        });

                    deferredRead.done(function() {
                        deferredWrite.resolve();
                    });

                    SituationPolicyView.$el.find(".savePolicy").click();
                    deferredRead.resolve({mappings: sync});

                    QUnit.ok(callback.called, "The policy successfully saves");

                    stubbedRead.restore();
                    stubbedWrite.restore();
                    $("#policyPattern").remove();
                    QUnit.start();

                });
            });
        }
    };
});