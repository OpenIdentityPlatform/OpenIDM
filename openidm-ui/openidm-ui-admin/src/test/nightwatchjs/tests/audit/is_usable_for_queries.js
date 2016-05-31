module.exports = {
    before: function(client, done) {
        audit = client.page.configureSysPrefAudit();
        eventHandlers = audit.section.eventHandlers;
        eventHandlersDialog = audit.section.eventHandlersDialog;

        client.globals.login.helpers.setSession(client, function () {
            client.globals.config.read("audit", function() {
                audit.navigate();
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

    "UseForQueries based off of isUsableForQueries property": function(client) {
        // Default handlers CSV and Repo should have a isUsableForQueries property set to true so
        // the corresponding radio button should be present.  When adding a jms audit event
        // handler the titatoggle for use for queries should not be present and when added the
        // useForQueries radio should no be present.
        eventHandlers
            .waitForElementPresent("@addEventHandlerButton", 2000)
            .assert.elementPresent("@csvUseForQueries")
            .assert.elementPresent("@repoUseForQueries")
            .click("@eventHandlerSelect")
            .click("@eventHandlerJMSOption")
            .click("@addEventHandlerButton");

        eventHandlersDialog
            .waitForElementPresent("@name", 2000)
            .setValue("@name", "testJMS")
            .click("@submitAuditEventHandlers");

        eventHandlers
            .waitForElementPresent("@addEventHandlerButton", 2000)
            .assert.elementNotPresent("@jmsUseForQueries")

    }
};
