module.exports = {
    before: function(client, done) {
        audit = client.page.configureSysPrefAudit();
        eventHandlers = audit.section.eventHandlers;
        eventHandlersDialog = audit.section.eventHandlersDialog;

        client.globals.login.helpers.setSession(client, function () {
            audit.navigate();
            done();
        });

    },

    after: function(client) {
        client.config.resetAll(function(data) {
            client.end();
        });
    },

    "Adding a new CSV Handler should provide a properties form": function(client) {
        // OPENIDM-5462
        eventHandlers
            .waitForElementPresent("@eventHandlerSelect", 2000)
            .click("@eventHandlerSelect")
            .click("@eventHandlerCSVOption");

        eventHandlers.click("@addEventHandlerButton");

        eventHandlersDialog
            .waitForElementPresent("@title", 2000)
            .assert.elementPresent("@title")
            .assert.elementPresent("@propertiesContainerFirstChild")
            .click("@closeButton");
    },

    "Default value shown for signatureInterval in csv handler": function (client) {
        // OPENIDM-4946
        client.pause(1000);
        eventHandlers
            .waitForElementPresent("@csvEditButton", 2000)
            .click("@csvEditButton");

        eventHandlersDialog
            .waitForElementPresent("@title", 2000)
            .expect.element("@signatureIntervalInput").to.have.value.that.equals("1 hour");
    }
};
