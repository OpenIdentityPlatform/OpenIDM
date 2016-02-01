module.exports = {
    "Add a dashboard": function(client) {
        client.globals.login.helpers.login(client);

        client.waitForElementPresent("#dashboardWidgets", 2000)
        // You see a dashboard called "Reconciliation Dashboard" marked as default
        .assert.containsText("h1", "Reconciliation Dashboard (Default)")
        .click(".active .dropdown-toggle")
        .expect.element(".active .dropdown-menu li:first-child a").text.to.contain("DEFAULT");

        // Add a new dashboard named "Test Dashboard 1"
        client.click(".active .dropdown-menu li:last-child")
        .waitForElementPresent("#NewDashboardForm", 2000)
        .setValue("#DashboardName", "Test Dashboard 1")
        .pause(1000)
        .click("input[type=submit]")
        .waitForElementPresent("#dashboardWidgets", 2000)
        .assert.containsText("h1", "Test Dashboard 1");
    },

    "Add a widget dashboard 1": function(client) {
        // Add a Frame widget, use top button to add widgets
        client.click("#dashboardWidgets .open-add-widget-dialog")
        .waitForElementPresent("button[data-widget-id='frame']", 2000)
        .click("button[data-widget-id='frame']")
        .waitForElementPresent(".widget .widget-title", 2000)
        .assert.containsText(".widget .widget-title", "EMBED WEB PAGE")
        .assert.elementPresent("#AddWidgetDialog")
        .waitForElementPresent(".bootstrap-dialog-footer-buttons button", 2000)
        .click(".bootstrap-dialog-footer-buttons button")
        .waitForElementNotPresent("#AddWidgetDialog", 3000);
    },

    "Attempt to add a dashboard with a used name": function(client) {
        // Attempt to add a dashboard with the same name as a previously added dashboard
        client.click(".active.dropdown")
        .click(".active .dropdown-menu li:last-child")
        .waitForElementPresent("#NewDashboardForm", 2000)
        .setValue("#DashboardName", "Test Dashboard 1")
        .assert.attributeEquals("input[type=submit]", "disabled", "true");
    },

    "Add a new dashboard and make it default": function(client) {
        // when that doesn't work ddd a new DEFAULT dashboard named "Test Dashboard 2"
        client.clearValue("#DashboardName")
        .setValue("#DashboardName", "Test Dashboard 2")
        .click(".checkbox-slider label")
        .click("input[type=submit]")
        .waitForElementPresent("#dashboardWidgets", 2000)
        .assert.containsText("h1", "Test Dashboard 2 (Default)")

        .click(".active .dropdown-toggle")
        .expect.element(".active .dropdown-menu li:first-child a").text.to.not.contain("Default");
        client.click(".active .dropdown-toggle");
    },

    "Add some widgets to dashboard 2": function(client) {
        // Add a System Health widget, use well button to add widget
        client.click("#dashboardWidgets button")
        .waitForElementPresent("button[data-widget-id='frame']", 2000)
        .click("button[data-widget-id='frame']")
        .waitForElementPresent(".widget .widget-title", 2000)
        .assert.containsText(".widget .widget-title", "EMBED WEB PAGE")

        // Add a Resources Widget
        .waitForElementPresent("button[data-widget-id='resourceList']", 2000)
        .click("button[data-widget-id='resourceList']")
        .waitForElementPresent(".resource-widget", 3000)
        .assert.elementPresent("#AddWidgetDialog")
        .click(".bootstrap-dialog-footer-buttons button")
        .waitForElementNotPresent("#AddWidgetDialog", 3000);
    },

    'Frame widget is configurable': function (client) {
        client.click(".btn-link")
        .waitForElementPresent(".dropdown-menu", 2000)
        .assert.elementPresent(".widget-settings")
        .click(".widget-settings")
        .waitForElementPresent("#widgetConfigForm", 3000)
        .assert.elementPresent("input[name='title']")
        .setValue("input[name='title']", "WIDGET TITLE")
        .click("#saveUserConfig")
        .pause(2000)
        .waitForElementPresent(".widget .widget-title", 2000)
        .assert.containsText(".widget .widget-title", "WIDGET TITLE")
    },

    "Logo takes you to default dashboard": function(client) {
        // Navigating to a different view and clicking the log returns you to the default "Test Dashboard 2"
        client.click(".navbar-admin.navbar-nav>li:nth-child(2)")
        .click("a[title=Mappings]")
        .waitForElementPresent("#navbarBrand", 2000)
        .click("#navbarBrand")
        .assert.containsText("h1", "Test Dashboard 2 (Default)");
    },

    "Deleting a dashboard takes you to the first dash": function(client) {
        // Deleting the default dashboard "Test Dashboard 2" takes you to the top dashboard "Reconciliation Dashboard"
        client.pause(1000)
        .click(".page-header .page-header-button-group .btn-group:nth-child(2) button")
        .click("#DeleteDashboard")
        .pause(2000)
        .assert.containsText("h1", "Reconciliation Dashboard");
    },

    "Defaulting a dashboard updates the nav and header": function(client) {
        // Making a dashboard default marks it in the dropdown and header immediately
        client.click(".active.dropdown")
        .click(".active .dropdown-menu li:nth-child(2)")
        .pause(1000)
        .click(".page-header .page-header-button-group .btn-group:nth-child(2) button")
        .click("#DefaultDashboard")
        .pause(2000)
        .assert.containsText("h1", "Default")
        .click(".active .dropdown-toggle")
        .expect.element(".active .dropdown-menu li:nth-child(2) a").text.to.contain("DEFAULT");
        client.click(".active .dropdown-toggle");
    },

    "Rename a dashboard": function(client) {
        // Attempt to rename dashboard to "Reconciliation Dashboard", assert there is a validation error
        client.click(".active.dropdown")
        .click(".active .dropdown-menu li:nth-child(2)")
        .pause(1000)
        .click(".page-header .page-header-button-group .btn-group:nth-child(2) button")
        .click("#RenameDashboard")
        .waitForElementPresent("#RenameDashboardDialog", 2000)
        .setValue("#DashboardName", "Reconciliation Dashboard")
        .assert.attributeEquals("#SaveNewName", "disabled", "true")

        // Rename dashboard to "Renamed Test Dashboard 1", assert the view now has this as a title
        .clearValue("#DashboardName")
        .setValue("#DashboardName", "Renamed Test Dashboard 1")
        .click("#SaveNewName")
        .pause(1000)
        .assert.containsText("h1", "Renamed Test Dashboard 1");
    },

    "Duplicating a dashboard": function(client) {
        // Duplicate "Renamed Test Dashboard 1" assert that the rename dialog opens
        client.click(".active.dropdown")
        .click(".active .dropdown-menu li:nth-child(2)")
        .pause(1000)
        .click(".page-header .page-header-button-group .btn-group:nth-child(2) button")
        .click("#DuplicateDashboard")
        .waitForElementPresent("#DuplicateDashboardDialog", 2000)
        .assert.value("#DashboardName", "Duplicate of Renamed Test Dashboard 1")

        // assert you can't name it "Renamed Test Dashboard 1",
        .clearValue("#DashboardName")
        .setValue("#DashboardName", "Renamed Test Dashboard 1")
        .assert.attributeEquals("#SaveNewName", "disabled", "true")

        // rename it "Duplicated Test Dashboard",
        .clearValue("#DashboardName")
        .setValue("#DashboardName", "Duplicated Test Dashboard")
        .click("#SaveNewName")
        .pause(1000)
        .assert.containsText("h1", "Duplicated Test Dashboard")
        .assert.elementNotPresent("DuplicateDashboardDialog");
    },

    "Delete all dashboards": function(client) {
        // Delete "Duplicated Test Dashboard"
        client.pause(1000)
        .click(".page-header .page-header-button-group .btn-group:nth-child(2) button")
        .click("#DeleteDashboard")
        .pause(1000)
        .assert.containsText("h1", "Renamed Test Dashboard 1")

        //Delete "Renamed Test Dashboard 1"
        .click(".page-header .page-header-button-group .btn-group:nth-child(2) button")
        .click("#DeleteDashboard")
        .pause(1000)
        .assert.containsText("h1", "Reconciliation Dashboard")

        //Delete "Reconciliation Dashboard"
        .click(".page-header .page-header-button-group .btn-group:nth-child(2) button")
        .click("#DeleteDashboard")
        .pause(1000)

        // With no dashboards you should be redirected to the create a dashboard view
        .assert.containsText("h1", "Create New Dashboard");
    },

    "cleanup": function(client) {
        //Re-add the recon dashboard and set it as default
        client.setValue("#DashboardName", "Reconciliation Dashboard")
        .click(".checkbox-slider label")
        .pause(1000)
        .click("input[type=submit]")
        .end();
    }

};