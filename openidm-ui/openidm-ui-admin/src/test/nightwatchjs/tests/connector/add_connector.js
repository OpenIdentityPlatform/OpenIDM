module.exports = {
    before: function(client) {
        client.globals.login.helpers.login(client);
    },
    after: function(client) {
        client.end();
    },
    'Add Connector Click': function (client) {
        client
            .url(client.globals.baseUrl + "#dashboard/")
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
            .assert.containsText("#connectorErrorMessage", "Error File CSVfile.csv does not exist")
            .clearValue("#connectorName")
            .clearValue("#csvFile");
    },
    'Change Connector Type to Database Table': function (client) {
        client
            .isVisible("#forCsvFileConnector")
            .click("#connectorType option[value='org.identityconnectors.databasetable.DatabaseTableConnector_1.1.0.1']")
            .waitForElementVisible("#forDatabaseTableConnector", 2000);
    },
    'Complete Required Form Fields Before "Add Connector" is selectable': function(client) {
        client
            .assert.attributeEquals("#submitConnector", "disabled", "true")
            .setValue("#connectorName","dbTable")
            .assert.attributeEquals("#submitConnector", "disabled", "true")
            .setValue("#table","Table")
            .assert.attributeEquals("#submitConnector", "disabled", "true")
            .setValue("#keyColumn","KeyColumn")
            .getAttribute("#submitConnector", "diabled", function(result) {
                this.assert.equal(result.state, "success");
                this.assert.equal(result.value, null);
            });
    }
};
