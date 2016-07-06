/*
    This test is to test widgets with configurations beyond size

    Currently:
    -frame
    -lastRecon
    -quickStart
*/

module.exports = {
    before: function(client, done) {
        //must create a session before tests can begin
        client.globals.login.helpers.setSession(client, function () {
            //read all configs that need to have the originals cached
            client.globals.config.read("ui/dashboard", function (data) {
                done();
            });
        });
    },

    after: function(client, done) {
        client.globals.config.resetAll(function(data) {
            client.end();
            done();
        });
    },

    "Add a dashboard and widgets": function(client) {
        var dashboard = client.page.dashboard(),
            navigation = dashboard.section.navigation,
            dashboardBody = dashboard.section.dashboardBody,
            newDashboard = dashboard.section.newDashboard,
            addWidgetsWindow = dashboard.section.addWidgetsWindow,
            nameInput = newDashboard.elements.nameInput.selector;

        navigation
            .waitForElementPresent("@dashboardDropdown", 2000)
            .click("@dashboardDropdown")
            .click("@newDashboardLink");

        newDashboard
            .waitForElementPresent("@nameInput", 2000)
            .click("@nameInput")
            .setValue("@nameInput", "Widget Dashboard");
        client.pause(500);
        client.keys(client.Keys.TAB);
        console.log(client.Keys.TAB);
        newDashboard
            .click("@createDashboardButton");

        client.pause(1000);

        dashboardBody
            .waitForElementPresent("@widgetContainer", 2000)
            .click("@noWidgetWellButton");

        addWidgetsWindow
            .waitForElementPresent("@dialogBody", 2000)
            .assert.elementPresent("@dialogBody")
            .click("@frameWidgetAddButton");

        client.pause(1000);
        addWidgetsWindow.click("@reconWidgetAddButton");
        client.pause(1000);
        addWidgetsWindow.click("@quickStartWidgetAddButton");
        client.pause(1000);

        addWidgetsWindow
            .click("@closeButton")
            .waitForElementNotPresent("@dialogBody", 3000);
    },

    "Frame widget is configurable": function (client) {
        var dashboard = client.page.dashboard(),
            dashboardBody = dashboard.section.dashboardBody,
            widgetSettingsWindow = dashboard.section.widgetSettingsWindow;

        client.pause(1000);

        dashboardBody
            .assert.cssProperty("@frame", "height", "600px")
            .expect.element("@firstWidgetTitle").text.to.not.contain("WIDGET TITLE");

        dashboardBody.expect.element("@frame").to.have.attribute("src").which.does.not.contain("TESTURL");

        dashboardBody.click("@firstWidgetOptionsToggle")
            .waitForElementPresent("@firstWidgetMenu", 2000)
            .assert.elementPresent("@firstWidgetSettings")
            .click("@firstWidgetSettings");

        widgetSettingsWindow
            .waitForElementPresent("@widgetSettingsBody", 3000)
            .assert.elementPresent("@frameWidgetTitleInput")
            .setValue("@frameWidgetTitleInput", "WIDGET TITLE")
            .assert.elementPresent("@frameWidgetURLInput")
            .setValue("@frameWidgetURLInput", "TESTURL")
            .assert.elementPresent("@frameWidgetHeightInput")
            .setValue("@frameWidgetHeightInput", "100px")
            .click("@widgetSaveButton");

        client.pause(2000);

        dashboardBody
            .assert.cssProperty("@frame", "height", "100px")
            .assert.containsText("@firstWidgetTitle", "WIDGET TITLE")
            .assert.attributeContains("@frame", "src", "TESTURL");
    },

    // This test should work with OPENIDM-5306
    "Recon widget is configurable": function (client) {
        var dashboard = client.page.dashboard(),
            dashboardBody = dashboard.section.dashboardBody,
            widgetSettingsWindow = dashboard.section.widgetSettingsWindow;

        dashboardBody
            .click("@secondWidgetOptionsToggle")
            .waitForElementPresent("@secondWidgetMenu", 2000)
            .assert.elementPresent("@secondWidgetSettings")
            .click("@secondWidgetSettings");

        widgetSettingsWindow
            .waitForElementPresent("@widgetSettingsBody", 3000)
            .assert.elementPresent("@reconWidgetType")
            .assert.value("@reconWidgetType", "false")
            .setValue("@reconWidgetType", "true")
            .assert.value("@widgetSizeSelect", "large")
            .setValue("@widgetSizeSelect", "small")
            .assert.value("@widgetSizeSelect", "small")
            .click("@widgetSaveButton")
            .waitForElementNotPresent("@widgetSettingsBody", 2000)
            .assert.elementNotPresent("@widgetSettingsBody");

        client.refresh(function() {
            dashboardBody
                .waitForElementPresent("@secondWidgetOptionsToggle", 2000)
                .click("@secondWidgetOptionsToggle")
                .waitForElementPresent("@secondWidgetMenu", 2000)
                .assert.elementPresent("@secondWidgetSettings")
                .click("@secondWidgetSettings");
            client.pause(500);
            widgetSettingsWindow
                .assert.value("@reconWidgetType", "true")
                .assert.value("@widgetSizeSelect", "small");
        });
    },

    "Quick Start widget is configurable": function (client) {
        client.refresh();

        var dashboard = client.page.dashboard(),
            dashboardBody = dashboard.section.dashboardBody,
            widgetSettingsWindow = dashboard.section.widgetSettingsWindow;

        dashboardBody
            .waitForElementPresent("@thirdWidgetOptionsToggle", 2000)
            .click("@thirdWidgetOptionsToggle")
            .waitForElementPresent("@thirdWidgetMenu", 2000)
            .assert.elementPresent("@thirdWidgetSettings")
            .click("@thirdWidgetSettings");

        widgetSettingsWindow
            .waitForElementPresent("@widgetSettingsBody", 3000)
            .click("@addQuickLinkButton")
            .waitForElementPresent("@quickLinkContainer", 2000)
            .setValue("@quickLinkNameInput", "test")
            .setValue("@quickLinkHrefInput", "test")
            .setValue("@quickLinkIconInput", "fa-link")
            .click("@quickLinkCreateButton")
            .waitForElementPresent("@quickLinkRow", 1000)
            .click("@widgetSaveButton");

        dashboardBody
            .waitForElementPresent("@quickLinksWidgetCardContainer", 2000)
            .assert.elementPresent("@quickLinksWidgetCardContainer")
            .assert.cssClassPresent("@quickLinksCard", "fa-link");
    }
};
