define([
    "org/forgerock/openidm/ui/admin/dashboard/DashboardWidgetLoader"
], function (DashboardWidgetLoader) {
    QUnit.module('DashboardWidgetLoader Tests');

    QUnit.test("Get supported widget list", function () {
        var widgetList = DashboardWidgetLoader.getWidgetList();

        QUnit.equal(widgetList["lifeCycleMemoryHeap"].defaultSize, "small", "Properly retrieved the list of supported UI widgets");
    });
});