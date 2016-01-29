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
        },
        'Toggle Enabled Switch and Test': function (client) {
            client
                .assert.attributeEquals("#connectorEnabled", "value", "true")
                .click("#connectorEnabled")
                .pause(100)
                .assert.attributeEquals("#connectorEnabled", "value", "false");
        },
        'Change Connector Type to Database Table and Test Toggles': function (client) {
            client
                .isVisible("#forCsvFileConnector")
                .click("#connectorType option[value='org.identityconnectors.databasetable.DatabaseTableConnector_1.1.0.1']")
                .waitForElementVisible("#forDatabaseTableConnector", 1000)

                .assert.attributeEquals("#enableEmptyString", "value", "false")
                .click("#enableEmptyString")
                .pause(100)
                .assert.attributeEquals("#enableEmptyString", "value", "true")

                .assert.attributeEquals("#rethrowAllSQLExceptions", "value", "true")
                .click("#rethrowAllSQLExceptions")
                .pause(100)
                .assert.attributeEquals("#rethrowAllSQLExceptions", "value", "false")

                .assert.attributeEquals("#nativeTimestamps", "value", "false")
                .click("#nativeTimestamps")
                .pause(100)
                .assert.attributeEquals("#nativeTimestamps", "value", "true")

                .assert.attributeEquals("#allNative", "value", "false")
                .click("#allNative")
                .pause(100)
                .assert.attributeEquals("#allNative", "value", "true");
        },
        'Change Connector Type to LDAP Connector': function (client) {
            client
                .click("#connectorType option[value='org.identityconnectors.ldap.LdapConnector_1.4.1.0']")
                .waitForElementVisible("#forLdapConnector", 1000);
        },
        'Change Connector Type to XML Connector and Test Toggle': function (client) {
            client
                .click("#connectorType option[value='org.forgerock.openicf.connectors.xml.XMLConnector_1.1.0.2']")
                .waitForElementVisible("#forXMLConnector", 1000)

                .assert.attributeEquals("#xmlCreateIfNotExists", "value", "false")
                .click("#xmlCreateIfNotExists")
                .pause(100)
                .assert.attributeEquals("#xmlCreateIfNotExists", "value", "true");

            client.end();
        }
};
