module.exports = {
    before: function(client) {
        client.globals.login.helpers.login(client);
        client.url(client.globals.baseUrl + "#connectors/add/");
    },
    after: function(client) {
        client.end();
    },
    'Test "Enabled" Toggle': function (client) {
        client
            .waitForElementVisible("#connectorGeneralDetails", 2000)
            .assert.attributeEquals("#connectorEnabled", "value", "true")
            .click("#connectorEnabled").pause(200)
            .assert.attributeEquals("#connectorEnabled", "value", "false")
            .click("#connectorEnabled").pause(200)
    },
    'Change Connector Type to Database Table': function (client) {
        client
            .isVisible("#forCsvFileConnector")
            .click("#connectorType option[value='org.identityconnectors.databasetable.DatabaseTableConnector_1.1.0.1']")
            .waitForElementVisible("#forDatabaseTableConnector", 2000);
    },
    'Test Toggles - DB Table': function (client) {
        client
            .assert.attributeEquals("#enableEmptyString", "value", "false")
            .click("#enableEmptyString").pause(200)
            .assert.attributeEquals("#enableEmptyString", "value", "true")
            .click("#enableEmptyString").pause(200)

            .assert.attributeEquals("#rethrowAllSQLExceptions", "value", "true")
            .click("#rethrowAllSQLExceptions").pause(200)
            .assert.attributeEquals("#rethrowAllSQLExceptions", "value", "false")
            .click("#rethrowAllSQLExceptions").pause(200)

            .assert.attributeEquals("#nativeTimestamps", "value", "false")
            .click("#nativeTimestamps").pause(200)
            .assert.attributeEquals("#nativeTimestamps", "value", "true")
            .click("#nativeTimestamps").pause(200)

            .assert.attributeEquals("#allNative", "value", "false")
            .click("#allNative").pause(200)
            .assert.attributeEquals("#allNative", "value", "true")
            .click("#allNative").pause(200);
    },
    'Change Connector Type to XML Connector': function (client) {
        client
            .click("#connectorType option[value='org.forgerock.openicf.connectors.xml.XMLConnector_1.1.0.2']")
            .waitForElementVisible("#connectorSpecificBase", 2000);
        },
    'Test Toggle XML': function(client) {
        client
            .assert.attributeEquals("#xmlCreateIfNotExists", "value", "false")
            .click("#xmlCreateIfNotExists").pause(200)
            .assert.attributeEquals("#xmlCreateIfNotExists", "value", "true")
            .click("#xmlCreateIfNotExists").pause(200);
    }
};
