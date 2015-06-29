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
    "sinon",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/admin/mapping/behaviors/PoliciesView"
], function (sinon, eventManager, PoliciesView) {

    $("body").append("<div id='policyPattern'></div>");

    return {
        executeAll: function (server) {
            module("Admin Mapping tab, Recon sub-tab tests");

            QUnit.asyncTest("Situational Policy Renders", function () {
                QUnit.equal(PoliciesView.$el.children().length, 0, "There are no children in the container before render.");

                PoliciesView.render({}, function() {
                    QUnit.ok(PoliciesView.$el.children().length > 0, "After rendering there are now child elements");

                    // -1 because of the source row used to create the others is still on the page but hidden
                    QUnit.equal(PoliciesView.$el.find("#situationalPolicies table tbody tr.event-hook").length,
                        _.size(PoliciesView.model.allPatterns["Default Actions"].policies),
                        "All default situations are present");

                    QUnit.equal(_.size(PoliciesView.model.allPatterns),
                        PoliciesView.$el.find("#policyPatterns option").length,
                        "Patterns rendered are the same as the stored patterns");

                    var messages = true;
                    _.each(PoliciesView.model.allPatterns["Default Actions"].policies, function(situation, index) {
                        if ($.t(situation.note) !== PoliciesView.$el.find("#situationalPolicies table tbody tr.event-hook:eq("+index+") td:eq(1) [data-title]").attr("data-title")) {
                            messages = false;
                        }
                    });

                    QUnit.ok(messages, "The help tooltips have been set");

                    QUnit.equal(PoliciesView.$el.find("#situationalPolicies table tbody .failure-display").length, 7, "There are 7 situations with error classes");
                    QUnit.equal(PoliciesView.$el.find("#situationalPolicies table tbody .warning-display").length, 3, "There are 3 situations with warning classes");
                    QUnit.equal(PoliciesView.$el.find("#situationalPolicies table tbody .success-display").length, 3, "There are 3 situations with success classes");

                    var readOnly = true, defaultActions = true;
                    PoliciesView.$el.find("#policyPatterns").val("Read-only").change();
                    _.each(PoliciesView.$el.find(".situationRow"), function(row, index) {
                        if ($(row).find(".action").val() !== "ASYNC"  && index < PoliciesView.model.allPatterns["Read-only"].policies.length) {
                            readOnly = false
                        }
                    });
                    QUnit.ok(readOnly, "When the pattern changes to Read-Only all actions are set to Async");

                    PoliciesView.$el.find("#policyPatterns").val("Default Actions").change();
                    _.each(PoliciesView.$el.find(".situationRow"), function(row, index) {
                        if ($(row).find(".action :selected").text().indexOf("★") < 0  && index < PoliciesView.model.allPatterns["Read-only"].policies.length) {
                            defaultActions = false
                        }
                    });
                    QUnit.ok(defaultActions, "When the pattern changes to Default-Actions all actions are set to the starred value");
/*
                    PoliciesView.$el.find(".ABSENT .action").val("LINK");

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

                    PoliciesView.$el.find(".savePolicy").click();
                    deferredRead.resolve({mappings: sync});

                    QUnit.ok(callback.called, "The policy successfully saves");
                    stubbedRead.restore();
                    stubbedWrite.restore();
                    */
                    $("#policyPattern").remove();
                    QUnit.start();

                });
            });
        }
    };
});
