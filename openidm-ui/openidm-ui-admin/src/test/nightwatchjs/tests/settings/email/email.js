module.exports = {
    before: function(client) {
        client.globals.login.helpers.login(client);
    },
    after: function(client) {
        client.end();
    },

    "Email should be disabled by default": function(client) {
        var emailSettingsPage = client.page.emailSettings();

        emailSettingsPage
            .navigate()
            .waitForElementPresent("@enableEmailToggle", 2000);

        emailSettingsPage.expect.element('@enableEmailToggle').to.not.be.selected;
    },

    "Enable email, enter details and save should display success message": function(client) {
        var emailSettingsPage = client.page.emailSettings();

        emailSettingsPage.navigate();

        emailSettingsPage
            .waitForElementPresent("@enableEmailToggle", 2000)
            .click("@enableEmailToggle")
            .waitForElementPresent("@hostField", 2000)
            .setValue("@hostField", 'localhost')
            .waitForElementPresent("@portField", 2000)
            .setValue("@portField", '1234')
            .click("@hostField")
            .click("@portField")
            .waitForElementPresent("@saveButton", 2000)
            .click("@saveButton")
            .waitForElementVisible("@infoAlert", 2000);
    },

    "Disable email can be saved": function(client) {
        var emailSettingsPage = client.page.emailSettings();

        emailSettingsPage
            .navigate()
            .waitForElementPresent("@enableEmailToggle", 2000)
            .click("@enableEmailToggle")
            .waitForElementPresent("@saveButton", 2000)
            .click("@saveButton")
            .waitForElementVisible("@infoAlert", 2000);

        emailSettingsPage.expect.element('@hostField').to.not.be.visible;
    }
};
