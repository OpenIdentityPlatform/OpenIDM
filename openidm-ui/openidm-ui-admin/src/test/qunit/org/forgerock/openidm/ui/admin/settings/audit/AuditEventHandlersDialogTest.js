define([
    "org/forgerock/openidm/ui/admin/settings/audit/AuditEventHandlersDialog"
], function (AuditEventHandlersDialog) {
    QUnit.module('AuditEventHandlersDialog Tests');

    QUnit.test("Translate Descriptions", function () {
        var schema = {
            "description": "common.form.info",
            "deeper": {
                "description": "common.form.add",
                "deeper1": [
                    {
                        "description": "common.form.list"
                    },

                    {
                        "description": "common.form.true",
                        "deeper2": {
                            "description": "common.form.false"
                        }
                    }
                ]
            }
        };

        AuditEventHandlersDialog.translateDescriptions(schema);

        QUnit.equal(schema.description, "Info", "Level 1 translation");
        QUnit.equal(schema.deeper.description, "Add", "Level 2 translation");
        QUnit.equal(schema.deeper.deeper1[0].description, "List", "Level 3 array item translation");
        QUnit.equal(schema.deeper.deeper1[1].deeper2.description, "False", "Level 3 array item object translation");
    });
});