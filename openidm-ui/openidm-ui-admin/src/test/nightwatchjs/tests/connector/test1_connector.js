module.exports = {
        'Add Connector Click': function (client) {
            //must login first at the beginning of a session
            client.globals.login.helpers.login(client);
            
            client
                .waitForElementPresent("#AddConnector", 2000)
                .click("#AddConnector")
                .assert.urlContains("#connectors/add/")
                .waitForElementPresent("#connectorName", 2000);
        },
        'Add Non Existent CSV Connector': function (client) {
            client
                .assert.attributeEquals("#connectorName", "data-validation-status", "error")
                .setValue("#connectorName","CSV")
                .assert.attributeEquals("#connectorName", "data-validation-status", "ok")
                .assert.attributeEquals("#csvFile", "data-validation-status", "error")
                .setValue("#csvFile","CSVfile.csv")
                .assert.attributeEquals("#csvFile", "data-validation-status", "ok")
                .click("#submitConnector")
                .waitForElementPresent("#connectorErrorMessage", 2000)
                .waitForElementVisible("#connectorErrorMessage", 2000)
                .expect.element('#connectorErrorMessage').text.to.equal("Error File CSVfile.csv does not exist");
            
            client.end();
        }
};