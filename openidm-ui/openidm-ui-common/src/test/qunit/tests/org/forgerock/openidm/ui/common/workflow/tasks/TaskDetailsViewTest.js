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
