define([
    "org/forgerock/openidm/ui/admin/dashboard/Dashboard"
], function (Dashboard) {
    QUnit.module('Dashboard Tests');

    QUnit.test("Add widget to dashboard", function (assert) {
        var widgetList = Dashboard.addWidget("test", "small", [{}, {}]);

        assert.equal(widgetList.length, 3, "Properly added widget object to current dashboard widgets");
        assert.equal(widgetList[2].type, "test", "Properly set new widget type");
        assert.equal(widgetList[2].size, "small", "Properly set new widget size");
    });

    QUnit.test("Set new default dashboard", function (assert) {
        var dashboardList = [
                {
                    "isDefault" : true
                },
                {
                    "isDefault" : false
                },
                {
                    "isDefault" : false
                }
            ],
            currentDashboard = dashboardList[2];

        currentDashboard = Dashboard.defaultDashboard(dashboardList, currentDashboard);

        assert.equal(dashboardList[0].isDefault, false, "Properly changed removed old default dashboard");
        assert.equal(currentDashboard.isDefault, true, "Properly changed set new default dashboard");
    });

    QUnit.test("Remove a dashboard", function (assert) {
        var dashboardList = [
                {
                    "isDefault" : true
                },
                {
                    "isDefault" : false
                },
                {
                    "isDefault" : false
                }
            ],
            dashboardDetails = Dashboard.deleteDashboard(dashboardList, 0);

        assert.equal(dashboardDetails.list.length, 2, "Properly removed dashboard");
        assert.equal(dashboardDetails.index, -1, "Properly detected no default dashboard exists");
    });

    QUnit.test("Remove a dashboard", function (assert) {
        var dashboardList = [
                {
                    "name" : "test"
                },
                {
                    "name" : "test2"
                },
                {
                    "name" : "test3"
                }
            ],
            afterDeleteList = Dashboard.deleteWidget(dashboardList, 1);

        assert.equal(dashboardList.length, 2, "Properly removed widget from dashboard");

    });
});