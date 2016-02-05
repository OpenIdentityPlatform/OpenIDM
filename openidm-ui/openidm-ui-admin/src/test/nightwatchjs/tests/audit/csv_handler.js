module.exports = {
        'Default value shown for signatureInterval in csv handler': function (client) {
            // OPENIDM-4946
            client.globals.login.helpers.login(client);
            client
                .url(client.globals.baseUrl + "#settings/audit/")
                .waitForElementPresent("#AuditEventHandlersBody", 2000)
                .click(".editEventHandler[data-name=csv]")
                .waitForElementPresent('[data-schemapath="root.security.signatureInterval"] input', 2000);

            client.expect.element('[data-schemapath="root.security.signatureInterval"] input').to.have.value.that.equals('1 hour');
        }
};
