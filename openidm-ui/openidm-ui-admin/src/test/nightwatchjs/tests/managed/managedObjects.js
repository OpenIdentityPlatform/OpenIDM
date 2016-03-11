module.exports = {
    before: function(client) {
        client.globals.login.helpers.login(client);

        ManagedListPage = client.page.configureManagedObjectsList();
        ManagedAddPage = client.page.configureManagedObjectsAdd();
        ManagedEditPage = client.page.configureManagedObjectsEdit();
    },

    after: function(client) {
        client.end();
    },

    "Add managed object": function (client) {
        var navigation = ManagedListPage.section.navigation,
            toolbar = ManagedListPage.section.toolbar,
            addDetails = ManagedAddPage.section.details;

        navigation
            .waitForElementPresent("@configureDropDownLink", 2000)
            .click("@configureDropDownLink")
            .waitForElementVisible("@managedObjectsLink", 2000)
            .click("@managedObjectsLink");

        toolbar
            .waitForElementPresent("@addNewManagedObjectButton", 2000)
            .click("@addNewManagedObjectButton");

        addDetails
            .waitForElementPresent("@nameInput", 2000)
            .setValue("@nameInput", 'device')
            .setValue("@iconInput", 'fa-android')
            .click("@addButton");

        ManagedAddPage
            .waitForElementVisible("@globalAlert", 2000)
            .expect.element("@globalAlert").text.to.equal("Managed object has been saved.");

        ManagedAddPage.waitForElementNotPresent("@globalAlert", 5500);
    },

    "Properties tab is dynamic based off of schema presence": function (client) {
        var tabs = ManagedEditPage.section.tabs,
            schemaTab = ManagedEditPage.section.schemaTab;

        tabs
            .waitForElementPresent("@schemaTab", 2000)
            .click("@schemaTab")
            .assert.cssClassPresent("@propertiesTab", "disabled")
            .click("@propertiesTab")
            .assert.cssClassPresent("@schemaTab", "active");

        schemaTab
            .waitForElementPresent("@propertiesButton", 2000)
            .click("@propertiesButton")
            .waitForElementPresent("@propertyNameInput", 2000)
            .setValue("@propertyNameInput", "test");

        ManagedEditPage.click("@save");

        client.pause(1000);

        tabs
            .assert.cssClassNotPresent("@propertiesTab", "disabled")
            .click("@propertiesTab")
            .assert.cssClassPresent("@propertiesTab", "active")
            .click("@schemaTab");

        schemaTab
            .waitForElementPresent("@deleteProperty", 2000)
            .click("@deleteProperty");

        ManagedEditPage.click("@save");

        client.pause(1000);

        tabs
            .waitForElementPresent("@propertiesTab", 2000)
            .assert.cssClassPresent("@propertiesTab", "disabled")
            .click("@propertiesTab")
            .assert.cssClassPresent("@schemaTab", "active");
        ManagedAddPage.waitForElementNotPresent("@globalAlert", 5500);
    },

    "Delete managed object": function (client) {
        var editToolbar = ManagedEditPage.section.toolbar,
            confirmDialog = ManagedEditPage.section.confirmDialog;

        editToolbar
            .waitForElementPresent("@moreOptionsToggle", 2000)
            .click("@moreOptionsToggle")
            .waitForElementVisible("@deleteButton", 2000)
            .click("@deleteButton");

        confirmDialog
            .waitForElementPresent("@okayButton", 2000)
            .expect.element("@okayButton").text.to.equal("Ok");
        confirmDialog.click("@okayButton");

        ManagedEditPage
            .waitForElementVisible("@globalAlert", 2000)
            .expect.element("@globalAlert").text.to.equal("Managed object successfully deleted.");
    }
};