define([
    "org/forgerock/openidm/ui/admin/dashboard/DashboardWidgetLoader"
], function (DashboardWidgetLoader) {
    QUnit.module('DashboardWidgetLoader Tests');

    QUnit.test("Get supported widget list", function (assert) {
        var widgetList = DashboardWidgetLoader.getWidgetList();

        assert.equal(widgetList["lifeCycleMemoryHeap"].defaultSize, "small", "Properly retrieved the list of supported UI widgets");
    });
});