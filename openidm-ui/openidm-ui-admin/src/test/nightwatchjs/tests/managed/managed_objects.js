var managedListPage,
    managedAddPage,
    managedEditPage;

module.exports = {
    before: function(client, done) {
        managedListPage = client.page.configureManagedObjectsList();
        managedAddPage = client.page.configureManagedObjectsAdd();
        managedEditPage = client.page.configureManagedObjectsEdit();

        client.globals.login.helpers.setSession(client, function() {
            client.globals.config.read('managed', done);
        });


    },

    after: function(client, done) {
        client.globals.config.reset('managed', function() {
            client.end();
            done();
        });
    },

    "It should toggle back and forth from views": function(client) {
        managedListPage
            .navigate()
            .waitForElementVisible('@managedCardContainer', 2000)
            .click("@managedGridToggle")
            .waitForElementVisible('@managedGridContainer', 2000)
            .click("@managedCardToggle")
            .waitForElementVisible('@managedCardContainer', 2000);
    },

    "It should load cards on card view": function(client) {
        var cards = managedListPage.elements.cards.selector;

        managedListPage
            .click("@managedCardToggle")
            .waitForElementVisible("@managedCardContainer", 2000);
        client
            .elements('css selector', cards, function(result) {
                client.assert.equal(result.value.length, 4, "4 cards rendered (including 'new')");
            });
    },

    "It should load grid on grid view": function(client) {
        var gridRows = managedListPage.elements.gridRows.selector;

        managedListPage
            .click("@managedGridToggle")
            .waitForElementVisible('@managedGridContainer', 2000);
        client
            .elements('css selector', gridRows, function(result) {
                client.assert.equal(result.value.length, 3, "3 grid rows rendered");
            });
    },

    "Toggle menu working in both card and grid view": function(client) {

        managedListPage
            .waitForElementVisible("@gridDropdownButton", 3000)
            .click("@gridDropdownButton")
            .waitForElementVisible("@gridDropdownList", 2000)
            .click("@managedCardToggle")
            .waitForElementVisible('@managedCardContainer', 2000)
            .click("@cardDropdownButton")
            .waitForElementVisible("@cardDropdownList", 2000);
    },

    "Delete working in both views": function(client) {
        managedListPage
            .click("@cardDeleteButton")
            .waitForElementVisible("button.btn-danger", 2000)
            .click("button.btn-danger")
            .waitForElementPresent("@managedGridToggle", 2000)
            .waitForElementVisible("@managedGridToggle", 2000);
        client.pause(250);
        managedListPage
            .click("@managedGridToggle")
            .waitForElementVisible('@managedGridContainer', 2000);
        client.pause(250);
        managedListPage
            .click("@gridDropdownButton")
            .waitForElementVisible("@gridDropdownList", 2000)
            .click("@gridDeleteButton")
            .waitForElementVisible("button.btn-danger", 2000)
            .click("button.btn-danger");
        client
            .elements('css selector', managedListPage.elements.gridRows.selector, function(result) {
                client.assert.equal(result.value.length, 1, "1 grid row left after 2 deletions");
            });
    },

    "Filtering works in both views ": function(client) {

        // reset managed config to have the three objects back
        client.globals.config.reset('managed', function () {
            client.globals.config.read('managed', function() {
                var gridRows = managedListPage.elements.gridRows.selector,
                    cards = managedListPage.elements.cards.selector;

                client
                    .refresh();
                managedListPage
                    .waitForElementVisible('@managedCardContainer', 6000)
                    .click("@managedGridToggle")
                    .waitForElementVisible('@managedGridContainer', 2000);
                client
                    .elements('css selector', gridRows, function(result) {
                        client.assert.equal(result.value.length, 3, "back to 3 rows after config reset");
                    });
                managedListPage
                    .click('input.filter-input');
                client
                    .keys('assignment')
                    .waitForElementNotVisible("#managedGridContainer table > tbody > tr:nth-child(2)", 2000);
                managedListPage
                    .click("@managedCardToggle")
                    .waitForElementVisible('@managedCardContainer', 2000);
                client
                    .expect.element('.card-body > a[href="#managed/edit/role/"]').to.not.be.visible;
            });
        });
    },

    "Data, edit, and create new mapping lead to correct pages.": function(client) {
        client
            .refresh();

        managedListPage
            .waitForElementVisible('@managedCardContainer', 2000)
            .click("@cardDropdownButton")
            .waitForElementVisible("@cardDropdownList", 2000)
            .click('div[data-managed-title="assignment"] a[href="#resource/managed/assignment/list/"]')
            .assert.urlContains("/#resource/managed/assignment/list/")
            .waitForElementVisible("h1", 2000)
            .assert.containsText("h1", 'Assignment List');
        client.back();
        managedListPage
            .waitForElementVisible('@managedCardContainer', 2000)
            .click("@cardDropdownButton")
            .waitForElementVisible("@cardDropdownList", 2000)
            .click('div[data-managed-title="assignment"] a[href="#managed/edit/assignment/"]')
            .assert.urlContains("#managed/edit/assignment/")
            .waitForElementVisible("#generalDetails", 2000);
        client.back();
        managedListPage
            .waitForElementVisible('@managedCardContainer', 2000)
            .click("@cardDropdownButton")
            .waitForElementVisible("@cardDropdownList", 2000)
            .click('div[data-managed-title="assignment"] a[href="#mapping/add/managed/assignment"]')
            .assert.urlContains("#mapping/add/managed/assignment")
            .waitForElementVisible("h1", 2000)
            .assert.containsText("h1", 'New Mapping');
        client.back();
    },

    "Add managed object": function (client) {
        var navigation = managedListPage.section.navigation,
            toolbar = managedListPage.section.toolbar,
            addDetails = managedAddPage.section.details;

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

        managedAddPage
            .waitForElementVisible("@globalAlert", 2000)
            .expect.element("@globalAlert").text.to.equal("Managed object has been saved.");

        managedAddPage.waitForElementNotPresent("@globalAlert", 5500);
    },

    "Properties tab is dynamic based off of schema presence": function (client) {
        var tabs = managedEditPage.section.tabs,
            schemaTab = managedEditPage.section.schemaTab;

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

        managedEditPage.click("@save");

        client.pause(1000);

        tabs
            .assert.cssClassNotPresent("@propertiesTab", "disabled")
            .click("@propertiesTab")
            .assert.cssClassPresent("@propertiesTab", "active")
            .click("@schemaTab");

        schemaTab
            .waitForElementPresent("@deleteProperty", 2000)
            .click("@deleteProperty");

        managedEditPage.click("@save");

        client.pause(1000);

        tabs
            .waitForElementPresent("@propertiesTab", 2000)
            .assert.cssClassPresent("@propertiesTab", "disabled")
            .click("@propertiesTab")
            .assert.cssClassPresent("@schemaTab", "active");
        managedAddPage.waitForElementNotPresent("@globalAlert", 5500);
    },

    "Delete managed object": function (client) {
        var editToolbar = managedEditPage.section.toolbar,
            confirmDialog = managedEditPage.section.confirmDialog;

        editToolbar
            .waitForElementPresent("@moreOptionsToggle", 2000)
            .click("@moreOptionsToggle")
            .waitForElementVisible("@deleteButton", 2000)
            .click("@deleteButton");

        confirmDialog
            .waitForElementPresent("@okayButton", 2000)
            .expect.element("@okayButton").text.to.equal("Ok");
        confirmDialog.click("@okayButton");

        managedEditPage
            .waitForElementVisible("@globalAlert", 2000)
            .expect.element("@globalAlert").text.to.equal("Managed object successfully deleted.");
    }
};
