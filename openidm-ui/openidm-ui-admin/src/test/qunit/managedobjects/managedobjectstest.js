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
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/admin/managed/AddEditManagedView",
    "org/forgerock/openidm/ui/admin/ResourcesView",
    "../mocks/resourceDetails"
], function (constants, router, eventManager, addEditManagedView, resourcesView, resourceDetails) {

    return {
        executeAll: function (server) {
            module('Admin Managed Object UI Functions');

            QUnit.asyncTest("Managed Objects Add/Edit", function () {

                resourceDetails(server);

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

                        $("#managedObjectName").val("role");

                        QUnit.ok(addEditManagedView.$el.find("#addEditManaged").is(":disabled") === false, "Submit button enabled");

                        addEditManagedView.$el.find("#addEditManaged").trigger("click");

                        QUnit.equal($("#managedErrorMessage").length, 1, "Duplicate name succesfully detected");

                        QUnit.ok(addEditManagedView.$el.find("#addEditManaged").is(":disabled") === true, "Submit button disabled");

                        $("#managedObjectName").val("test1234");
                        addEditManagedView.$el.find("#managedObjectName").trigger("blur");

                        QUnit.ok(addEditManagedView.$el.find("#addEditManaged").is(":disabled") === false, "Submit button enabled after new name");

                        addEditManagedView.$el.find("#addEditManaged").trigger("click");

                        QUnit.start();
                    }, 100);

                });

            });
        }

    };

});