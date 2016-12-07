define([
    "org/forgerock/openidm/ui/admin/settings/audit/AuditTopicsDialog"
], function (AuditTopicsDialog) {
    QUnit.module('AuditTopicsDialog Tests');

    QUnit.test("Audit topic filters are updated...", function(assert) {
        var testResults,
            event = {
                "filter": {
                    "actions": ["action1", "action2"],
                    "triggers": ["trigger1", "trigger2"]
                }
            };


        testResults = AuditTopicsDialog.updateFilter({}, "actions", ["testAction"]);
        assert.deepEqual(testResults.filter.actions, ["testAction"], "When given an empty event, a property is created and set appropriately.");

        testResults = AuditTopicsDialog.updateFilter(event, "actions", ["action1", "action3", "action4"]);
        assert.deepEqual(testResults.filter.actions, ["action1", "action3", "action4"], "Updates values for an existing filters.");

        testResults = AuditTopicsDialog.updateFilter(event, "triggers", []);
        assert.equal(testResults.filter.triggers.length, 0, "When given an empty array all values are removed.");

    });
});