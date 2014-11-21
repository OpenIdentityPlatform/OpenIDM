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
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/MandatoryPasswordChangeDialog",
    "org/forgerock/openidm/ui/admin/managed/AddEditManagedView",
    "org/forgerock/openidm/ui/admin/ResourcesView",
    "./mocks/adminInit",
    "./mocks/encryptedPW",
    "./mocks/cleartextPW",
    "./mocks/addEditManaged"
], function (sinon, constants, router, eventManager, mandatoryPasswordChangeDialog, addEditManagedView, resourcesView, adminInit, encryptedPW, cleartextPW, addEditManaged) {

    return {
        executeAll: function (server) {

            adminInit(server);

            module('Admin UI Functions');

            QUnit.asyncTest("Initial Login Process", function () {

                var dialogRenderStub = sinon.stub(mandatoryPasswordChangeDialog, "render", function (args, callback) {

                    mandatoryPasswordChangeDialog.render.restore();
                    mandatoryPasswordChangeDialog.render(args, function () {

                        QUnit.ok(true, "Mandatory password change dialog displayed when cleartext password used");

                        QUnit.equal(mandatoryPasswordChangeDialog.$el.find(".validationRules.group-field-errors>.field-rule").length, 5, "Five validation rules for password displayed");

                        if (callback) {
                            callback();
                        }

                        QUnit.start();
                    });
                });

                cleartextPW(server);

                eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, { userName: "openidm-admin", password: "openidm-admin" });
            });

            QUnit.asyncTest("Subsequent Login Process", function () {
                var resourceRenderStub = sinon.stub(resourcesView, "render", function (args, callback) {

                    resourcesView.render.restore();
                    resourcesView.render(args, function () {
                        var viewManager = require("org/forgerock/commons/ui/common/main/ViewManager");

                        QUnit.ok(viewManager.currentView === "org/forgerock/openidm/ui/admin/ResourcesView" && viewManager.currentDialog === "null", "Resource page shown after successful login with encrypted password");

                        if (callback) {
                            callback();
                        }

                        QUnit.start();
                    });
                });

                encryptedPW(server);

                eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, { userName: "openidm-admin", password: "Passw0rd" });
            });

            QUnit.asyncTest("Resource View", function () {

                addEditManaged(server);

                resourcesView.render([], function () {
                    var viewManager = require("org/forgerock/commons/ui/common/main/ViewManager");

                    QUnit.equal(resourcesView.$el.find("#resourceConnectorContainer .resource-body").length, 2, "Connectors and add Connector successfully added");

                    QUnit.equal(resourcesView.$el.find("#resourceManagedContainer .resource-body").length, 5, "Managed Objects and add Managed Object successfully added");

                    QUnit.start();
                });
            });

            QUnit.asyncTest("Managed Objects Add/Edit", function () {

                addEditManaged(server);

                addEditManagedView.render([], function () {

                    QUnit.start();

                    addEditManagedView.$el.find("#addManagedProperties").trigger("click");

                    QUnit.equal(addEditManagedView.$el.find(".add-remove-block:visible").length, 1, "Add Object Type property successfull");

                    addEditManagedView.$el.find(".add-remove-block:visible .remove-btn").trigger("click");
                    $(".ui-dialog .ui-dialog-buttonset .ui-button:last").trigger("click");

                    QUnit.equal(addEditManagedView.$el.find(".add-remove-block:visible").length, 0, "Delete Object Type property successfull");

                    addEditManagedView.$el.find("#addManagedScript").trigger("click");

                    QUnit.equal(addEditManagedView.$el.find(".managed-event-hook").length, 1, "Script hook added");

                    QUnit.stop();

                    setTimeout(function() {
                        addEditManagedView.$el.find(".event-hook-empty").trigger("click");

                        QUnit.equal($("#scriptManagerDialogForm").length, 1, "Script editor successfully opened");

                        $("#scriptFilePath").val("test");
                        $("#scriptFilePath").trigger("blur");

                        QUnit.ok($("#scriptDialogOkay").is(":disabled") === false, "Script Dialog submit button enabled");

                        $("#scriptDialogOkay").trigger("click");

                        QUnit.equal(addEditManagedView.$el.find(".event-hook-status").length, 2, "Script Successfully Added");

                        addEditManagedView.$el.find(".event-hook-status").trigger("click");

                        addEditManagedView.$el.find(".add-remove-block:visible .remove-btn").trigger("click");
                        $(".ui-dialog .ui-dialog-buttonset .ui-button:last").trigger("click");

                        QUnit.equal(addEditManagedView.$el.find(".add-remove-block:visible").length, 0, "Delete script successfull");

                        addEditManagedView.$el.find("#managedObjectName").val("testname");
                        addEditManagedView.$el.find("#managedObjectName").trigger("blur");

                        QUnit.ok(addEditManagedView.$el.find("#addEditManaged").is(":disabled") === false, "Submit button enabled");

                        addEditManagedView.$el.find("#addEditManaged").trigger("click");

                        QUnit.start();
                    }, 100);

                });

            });
        }

    };

});