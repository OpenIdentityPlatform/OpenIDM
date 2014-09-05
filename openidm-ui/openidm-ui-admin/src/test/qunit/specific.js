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
    "org/forgerock/openidm/ui/admin/MandatoryPasswordChangeDialog",
    "org/forgerock/openidm/ui/admin/Dashboard",
    "org/forgerock/openidm/ui/admin/managed/AddEditManagedView",
    "org/forgerock/openidm/ui/admin/ResourcesView",
    "./mocks/adminInit",
    "./mocks/encryptedPW",
    "./mocks/cleartextPW",
    "./mocks/addEditManaged"
], function (sinon, constants, router, eventManager, mandatoryPasswordChangeDialog, dashboard, addEditManagedView, resourcesView, adminInit, encryptedPW, cleartextPW, addEditManaged) {

    return {
        executeAll: function (server) {

            adminInit(server);
            
            var testPromises = [];

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
                var dashboardRenderStub = sinon.stub(dashboard, "render", function (args, callback) {
                    dashboard.render.restore();
                    dashboard.render(args, function () {
                        var viewManager = require("org/forgerock/commons/ui/common/main/ViewManager");

                        QUnit.ok(viewManager.currentView === "org/forgerock/openidm/ui/admin/Dashboard" && viewManager.currentDialog === "null", "Dashboard shown after successful login with encrypted password");

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
                var resourceRenderStub = sinon.stub(resourcesView, "render", function (args, callback) {
                    resourcesView.render.restore();
                    resourcesView.render(args, function () {
                        var viewManager = require("org/forgerock/commons/ui/common/main/ViewManager");

                        QUnit.ok(viewManager.currentView === "org/forgerock/openidm/ui/admin/ResourcesView" && viewManager.currentDialog === "null", "Resource view successfully loaded");

                        QUnit.equal(resourcesView.$el.find("#resourceConnectorContainer .resource-body").length, 2, "Connectors and add Connector successfully added");

                        QUnit.equal(resourcesView.$el.find("#resourceManagedContainer .resource-body").length, 5, "Managed Objects and add Managed Object successfully added");

                        eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.addManagedView, args: []});

                        QUnit.ok(viewManager.currentView === "org/forgerock/openidm/ui/admin/managed/AddEditManagedView" && viewManager.currentDialog === "null", "Able to successfully load add/edit Managed page.");

                        if (callback) {
                            callback();
                        }

                        QUnit.start();
                    });
                });

                addEditManaged(server);

                eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.resourceView});
            });

            QUnit.asyncTest("Managed Objects Add/Edit", function () {
                var managedObjectRenderStub = sinon.stub(addEditManagedView, "render", function (args, callback) {
                    addEditManagedView.render.restore();

                    addEditManagedView.render(args, function () {
                        var viewManager = require("org/forgerock/commons/ui/common/main/ViewManager");

                        QUnit.ok(viewManager.currentView === "org/forgerock/openidm/ui/admin/managed/AddEditManagedView" && viewManager.currentDialog === "null", "Managed Object page successfully shown");

                        addEditManagedView.$el.find("#addManagedProperties").trigger("click");

                        QUnit.equal(addEditManagedView.$el.find(".add-remove-block:visible").length, 1, "Add Object Type property successfull");

                        addEditManagedView.$el.find(".add-remove-block:visible .remove-btn").trigger("click");

                        QUnit.equal(addEditManagedView.$el.find(".add-remove-block:visible").length, 0, "Delete Object Type property successfull");

                        addEditManagedView.$el.find("#managedObjectName").val("testname");
                        addEditManagedView.$el.find("#managedObjectName").trigger("blur");

                        QUnit.ok(addEditManagedView.$el.find("#addEditManaged").is(":disabled") === false, "Submit button enabled");

                        addEditManagedView.$el.find("#addEditManaged").trigger("click");

                        QUnit.stop();

                        setTimeout(function() {
                            QUnit.ok(viewManager.currentView === "org/forgerock/openidm/ui/admin/ResourcesView" && viewManager.currentDialog === "null", "Managed Object successfully saved");

                            QUnit.start();
                        }, 2000);


                        if (callback) {
                            callback();
                        }

                        QUnit.start();
                    });
                });

                addEditManaged(server);

                eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.addManagedView, args: []});
            });
        }
    };

}); 