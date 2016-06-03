module.exports = {
        before : function(client, done) {
            //must create a session before tests can begin
            client.globals.login.helpers.setSession(client, function () {
                //read all configs that need to have the originals cached
                client.config.read("audit", function () {
                    done();
                });
            });
        },

        after : function(client) {
            client.config.resetAll(function () {
                client.end();
            });
        },

        "An audit event handler should start enabled": function(client) {
            var auditPage = client.page.configureSysPrefAudit(),
                eventHandlers = auditPage.section.eventHandlers,
                eventHandlersDialog = auditPage.section.eventHandlersDialog;

            auditPage.navigate();

            eventHandlers
                .waitForElementPresent("@csvEditButton", 2000)
                .click("@csvEditButton");

            auditPage.waitForElementPresent("@modal", 2000);

            eventHandlersDialog
                .waitForElementPresent("@title", 2000)
                .expect.element("@enabledCheck").to.be.selected;

            eventHandlersDialog.click("@closeButton");
        },

        'Check endOfLineSymbols saved properly': function (client) {
            client
                .waitForElementPresent('a[href="#auditContainer"]', 2000)
                .click('a[href="#auditContainer"]')
                .waitForElementVisible('button.editEventHandler[data-name=csv]', 2000)
                .click('button.editEventHandler[data-name=csv]')
                .waitForElementPresent('select[name="root[formatting][endOfLineSymbols]"]', 2000)
                .click('select[name="root[formatting][endOfLineSymbols]"]')
                .waitForElementVisible('select[name="root[formatting][endOfLineSymbols]"] option:last-of-type', 2000)
                .click('select[name="root[formatting][endOfLineSymbols]"] option:last-of-type')
                .pause(1000)
                .click("#submitAuditEventHandlers")
                .waitForElementVisible('#submitAudit', 2000)
                .click("#submitAudit")
                .waitForElementVisible('div[role=alert]', 2000)
                .expect.element('div[role=alert]').text.to.equal("Audit configurations have been saved.");

            client
                .pause(2000)
                .click('button.editEventHandler[data-name=csv]')
                .waitForElementPresent('select[name="root[formatting][endOfLineSymbols]"]', 2000)
                .execute(
                    function () {
                        return $('select[name="root[formatting][endOfLineSymbols]"] option:selected').index();
                    },
                    [],
                    function (result) {
                        var index = result.value + 1;
                        client.expect.element('select[name="root[formatting][endOfLineSymbols]"] option:nth-of-type(' + index + ')').text.to.be.equal('Carriage Return + Linefeed (\\r\\n)');
                    }
                )
                .config.read("audit", function (config) {
                    var _ = require("lodash"),
                        eventHandler = _.filter(config.eventHandlers,{"class":"org.forgerock.audit.handlers.csv.CsvAuditEventHandler"});

                    client.assert.equal(eventHandler[0].config.formatting.endOfLineSymbols, '\r\n','endOfLineSymbol saved in audit.json config correctly');
                });
        }
};