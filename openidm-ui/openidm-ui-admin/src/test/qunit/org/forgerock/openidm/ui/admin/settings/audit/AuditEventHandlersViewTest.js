define([
    "org/forgerock/openidm/ui/admin/settings/audit/AuditEventHandlersView"
], function (AuditEventHandlersView) {
    QUnit.module('AuditEventHandlersView Tests');

    QUnit.test("Retrieves event handler index", function () {
        var eventHandlers = [
                {"config": {"name": "test1"}},
                {"config": {"name": "test2"}},
                {"config": {"name": "test3"}}
            ],
            index = AuditEventHandlersView.getIndex(eventHandlers, "test1");
        QUnit.equal(index, 0, "Index is 0");
        index = AuditEventHandlersView.getIndex(eventHandlers, "test3");
        QUnit.equal(index, 2, "Index is 2");
        index = AuditEventHandlersView.getIndex(eventHandlers, "test4");
        QUnit.equal(index, -1, "Name doesn't exist, index is -1");
    });
});