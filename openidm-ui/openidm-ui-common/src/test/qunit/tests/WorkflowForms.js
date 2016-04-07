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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "jquery",
    "sinon",
    "org/forgerock/openidm/ui/common/workflow/tasks/TaskDetailsView",
    "org/forgerock/openidm/ui/common/workflow/WorkflowDelegate"
], function ($, sinon, TaskDetailsView, WorkflowDelegate) {
    QUnit.module('Workflow Form Functions');

    QUnit.test("Task Details Form Submit validation (OPENIDM-5558)", function () {
        var fakeForm = $("<form id='test'><input data-validation-status='ok'></form>")

        $("#qunit-fixture").append(fakeForm);

        sinon.stub(WorkflowDelegate, "completeTask", $.noop);

        TaskDetailsView.formSubmit.call({
            "$el": fakeForm,
            "task": { "_id" : "test"}
        }, {
            preventDefault: $.noop
        });

        QUnit.equal(WorkflowDelegate.completeTask.callCount, 1, "A valid task form resulted in one complete task call");

        WorkflowDelegate.completeTask.reset();

        fakeForm.find(":input").attr("data-validation-status", "error");
        TaskDetailsView.formSubmit.call({
            "$el": fakeForm,
            "task": { "_id" : "test"}
        }, {
            preventDefault: $.noop
        });

        QUnit.equal(WorkflowDelegate.completeTask.callCount, 0, "An invalid task form resulted in no complete task call");

        WorkflowDelegate.completeTask.restore();

    });
});
