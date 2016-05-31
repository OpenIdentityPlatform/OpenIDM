module.exports = {
    before: function(client, done) {
        dashboard = client.page.dashboard();
        navigation = dashboard.section.navigation;
        dashboardHeader = dashboard.section.dashboardHeader;
        dashboardBody = dashboard.section.dashboardBody;
        newDashboard = dashboard.section.newDashboard;
        addWidgetsWindow = dashboard.section.addWidgetsWindow;

        //must create a session before tests can begin
        client.globals.login.helpers.setSession(client, function () {
            //read all configs that need to have the originals cached
            client.globals.config.read("ui/configuration", function (data) {
                done();
            });
        });
    },

    after: function(client, done) {
        client.globals.config.resetAll(function() {
            client.end();
            done();
        });
    },

    "Add a dashboard": function(client) {
        dashboardBody.waitForElementPresent("@widgetContainer", 2000);

        // You see a dashboard called "Reconciliation Dashboard" marked as default
        dashboardHeader.assert.containsText("@title", "Reconciliation Dashboard (Default)");
        navigation
            .click("@dashboardDropdown")
            .expect.element("@firstDashboardText").text.to.contain("DEFAULT");

        // Add a new dashboard named "Test Dashboard 1"
        navigation.click("@newDashboardLink");

        newDashboard
            .waitForElementPresent("@form", 2000)
            .setValue("@nameInput", "Test Dashboard 1");

        client
            .pause(1000)
            .execute(function () {
                $("#DashboardName").trigger("blur");
            })
            .pause(1000);

        newDashboard.click("@createDashboardButton");

        dashboardBody.waitForElementPresent("@widgetContainer", 2000);
        dashboardHeader.assert.containsText("@title", "Test Dashboard 1");
    },

    "Add a widget to dashboard 1": function(client) {
        // Add a Frame widget, use top button to add widgets
        dashboardHeader.click("@addWidgetsButton");

        addWidgetsWindow
            .waitForElementPresent("@dialogBody", 2000)
            .assert.elementPresent("@dialogBody")
            .waitForElementPresent("@cpuWidgetAddButton", 2000)
            .click("@cpuWidgetAddButton");

        dashboardBody
            .waitForElementPresent("@firstWidgetTitle", 2000)
            .assert.containsText("@firstWidgetTitle", "CPU USAGE");

        client.pause(2000);

        addWidgetsWindow
            .assert.elementPresent("@dialogBody")
            .waitForElementPresent("@closeButton", 2000)
            .click("@closeButton")
            .waitForElementNotPresent("@dialogBody", 3000);
    },


    "Attempt to add a dashboard with a used name": function(client) {
        // Attempt to add a dashboard with the same name as a previously added dashboard
        navigation
            .click("@dashboardDropdown")
            .click("@newDashboardLink");


        newDashboard
            .waitForElementPresent("@form", 2000)
            .setValue("@nameInput", "Test Dashboard 1");

        client
            .pause(1000)
            .execute(function () {
                $("#DashboardName").trigger("blur");
            })
            .pause(1000);

        newDashboard
            .assert.attributeEquals("@createDashboardButton", "disabled", "true");
    },

    "Add a new dashboard and make it default": function(client) {
        // when that doesn't work ddd a new DEFAULT dashboard named "Test Dashboard 2"
        newDashboard
            .clearValue("@nameInput")
            .setValue("@nameInput", "Test Dashboard 2")

        client
            .pause(1000)
            .execute(function () {
                $("#DashboardName").trigger("blur");
            })
            .pause(1000);

        newDashboard
            .click("@setAsDefaultToggle")
            .click("@createDashboardButton");

        dashboardBody.waitForElementPresent("@widgetContainer", 2000);

        dashboardHeader.assert.containsText("@title", "Test Dashboard 2 (Default)");

        navigation
            .click("@dashboardDropdown")
            .expect.element("@firstDashboardText").text.to.not.contain("Default");
        navigation.click(".active .dropdown-toggle");
    },

    "Add some widgets to dashboard 2": function(client) {
        dashboardBody.click("@noWidgetWellButton");

        addWidgetsWindow
            .waitForElementPresent("@dialogBody", 2000)
            .assert.elementPresent("@dialogBody")
            .click("@lifeCycleMemoryHeapAddButton");

        client.pause(2000);

        dashboardBody
            .waitForElementPresent("@firstWidgetTitle", 2000)
            .assert.containsText("@firstWidgetTitle", "MEMORY USAGE (JVM HEAP)");

        addWidgetsWindow
            .waitForElementPresent("@dialogBody", 2000)
            .assert.elementPresent("@dialogBody")
            .waitForElementPresent("@systemHealthFullAddButton", 2000)
            .click("@systemHealthFullAddButton");

        client.pause(2000);

        dashboardBody.waitForElementPresent("@secondWidgetTitle", 3000);

        addWidgetsWindow
            .assert.elementPresent("@dialogBody")
            .click("@closeButton")
            .waitForElementNotPresent("@dialogBody", 3000);
    },

    "Remove a widget": function(client) {
        var widgetDeleteWindow = dashboard.section.widgetDeleteWindow;

        dashboardBody
            .click("@secondWidgetOptionsToggle")
            .waitForElementPresent("@secondWidgetDelete", 2000)
            .click("@secondWidgetDelete");

        widgetDeleteWindow
            .waitForElementPresent("@confirm", 2000)
            .click("@confirm");

        dashboardBody
            .waitForElementNotPresent("@secondWidgetTitle", 2000)
            .assert.elementNotPresent("@secondWidgetTitle")
    },

    "Logo takes you to default dashboard": function(client) {
        // Navigating to a different view and clicking the log returns you to the default "Test Dashboard 2"
        navigation
            .click("@configureDropdown")
            .click("@mappingLink")
            .waitForElementPresent("@logoLink", 2000)
            .click("@logoLink");

        dashboardHeader
            .waitForElementPresent("@title", 2000)
            .assert.containsText("@title", "Test Dashboard 2 (Default)");
    },

    "Deleting a dashboard takes you to the first dash": function(client) {
        // Deleting the default dashboard "Test Dashboard 2" takes you to the top dashboard "Reconciliation Dashboard"
        client.pause(1000);

        dashboardHeader
            .click("@optionsToggle")
            .click("@deleteButton");

        client.pause(2000);
        dashboardHeader.assert.containsText("@title", "Reconciliation Dashboard");
    },

    "Defaulting a dashboard updates the nav and header": function(client) {
        // Making a dashboard default marks it in the dropdown and header immediately
        navigation
            .click("@dashboardDropdown")
            .click("@secondDashboardLink");

        client.pause(1000);

        dashboardHeader
            .click("@optionsToggle")
            .click("@defaultButton");

        client.pause(2000);

        dashboardHeader.assert.containsText("@title", "Default");

        navigation
            .click("@dashboardDropdown")
            .expect.element("@secondDashboardText").text.to.contain("DEFAULT");

        navigation.click("@dashboardDropdown");
    },

    "Rename a dashboard": function(client) {
        var renameDashboardWindow = dashboard.section.renameDashboardWindow;

        // Attempt to rename dashboard to "Reconciliation Dashboard", assert there is a validation error
        dashboardHeader
            .click("@optionsToggle")
            .click("@renameButton");

        renameDashboardWindow
            .waitForElementPresent("@body", 2000)
            .setValue("@name", "Reconciliation Dashboard");

        client
            .pause(1000)
            .execute(function () {
                $("#DashboardName").trigger("keyup");
            })
            .pause(1000);

        renameDashboardWindow.assert.attributeEquals("@submit", "disabled", "true");

        // Rename dashboard to "Renamed Test Dashboard 1", assert the view now has this as a title
        renameDashboardWindow
            .clearValue("@name")
            .setValue("@name", "Renamed Test Dashboard 1");

        client
            .pause(1000)
            .execute(function () {
                $("#DashboardName").trigger("keyup");
            })
            .pause(1000);

        renameDashboardWindow
            .click("@submit");

        client.pause(1000);

        dashboardHeader.assert.containsText("@title", "Renamed Test Dashboard 1");
    },

    "Duplicating a dashboard": function(client) {
        var duplicateDashboardWindow = dashboard.section.duplicateDashboardWindow;

        // Duplicate "Renamed Test Dashboard 1" assert that the rename dialog opens
        dashboardHeader
            .click("@optionsToggle")
            .click("@duplicateButton");

        duplicateDashboardWindow
            .waitForElementPresent("@body", 2000)
            .assert.value("@name", "Duplicate of Renamed Test Dashboard 1");

        // assert you can't name it "Renamed Test Dashboard 1"
       duplicateDashboardWindow
            .clearValue("@name")
            .setValue("@name", "Renamed Test Dashboard 1");

        client
            .pause(1000)
            .execute(function () {
                $("#DashboardName").trigger("keyup");
            })
            .pause(1000);

        duplicateDashboardWindow.assert.attributeEquals("@submit", "disabled", "true");

        // rename it "Duplicated Test Dashboard",
        duplicateDashboardWindow
            .clearValue("@name")
            .setValue("@name", "Duplicated Test Dashboard");

        client
            .pause(1000)
            .execute(function () {
                $("#DashboardName").trigger("keyup");
            })
            .pause(1000);

        duplicateDashboardWindow
            .click("@submit");

        client.pause(1000);

        dashboardHeader.assert.containsText("@title", "Duplicated Test Dashboard");

        duplicateDashboardWindow.assert.elementNotPresent("@body");
    },

    "Delete all dashboards": function(client) {
        client.pause(1000)

        // Delete "Duplicated Test Dashboard"
        dashboardHeader
            .click("@optionsToggle")
            .click("@deleteButton");
        client.pause(1000);
        dashboardHeader.assert.containsText("@title", "Renamed Test Dashboard 1");

        //Delete "Renamed Test Dashboard 1"
        dashboardHeader
            .click("@optionsToggle")
            .click("@deleteButton");
        client.pause(1000);

        dashboardHeader.assert.containsText("@title", "Reconciliation Dashboard");
    }
};
