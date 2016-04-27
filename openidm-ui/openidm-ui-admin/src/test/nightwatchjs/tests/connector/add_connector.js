module.exports = {
    before: function(client) {
        client.globals.login.helpers.login(client);
    },
    after: function(client) {
        client.end();
    },
    'Add Connector Click': function (client) {
        var dashboard = client.page.dashboard();
        dashboard
            .navigate() //navigates to url associated with page object
            .waitForElementVisible('@addConnector', 2000)
            .click('@addConnector')
            .waitForElementNotPresent('@addConnector', 2000);
    },
    'Load Add Connector Form': function (client) {
        var addConnector = client.page.connectorsAdd();

        addConnector
            .waitForElementVisible('@connectorForm', 2000)
            .assert.urlContains('@href')
            .waitForElementPresent('@addConnectorButton', 2000);
    },
    'Test required CSV fields': function (client) {
        var addConnector = client.page.connectorsAdd(),
            csvDetails = addConnector.section.csvDetails;

        addConnector
            .assert.attributeEquals('@connectorName', 'data-validation-status', 'error')
            .setValue('@connectorName','CSV')
            .assert.attributeEquals('@connectorName', 'data-validation-status', 'ok');
        csvDetails
            .assert.attributeEquals('@csvFile', 'data-validation-status', 'error')
            .setValue('@csvFile','CSVfile.csv')
            .assert.attributeEquals('@csvFile', 'data-validation-status', 'ok')
            .clearValue('@uid')
            .assert.attributeEquals('@uid', 'data-validation-status', 'error')
            .setValue('@uid','test_uid')
            .click('@uid').api.pause(500);
        client
            .keys(['\uE004']).pause(500); //press TAB to change focus
        csvDetails
            .assert.attributeEquals('@uid', 'data-validation-status', 'ok')
            .assert.attributeEquals('@username', 'data-validation-status', 'error')
            .setValue('@username','test_user')
            .assert.attributeEquals('@username', 'data-validation-status', 'ok');
        addConnector
            .click('@addConnectorButton')
            .waitForElementVisible('@errorMessage', 2000)
            .assert.containsText('@errorMessage', 'Error File CSVfile.csv does not exist');
    },
    'Test required Database Table fields': function (client) {
        var addConnector = client.page.connectorsAdd(),
            generalDetails = addConnector.section.generalDetails,
            dbTableDetails = addConnector.section.dbTableDetails;


        generalDetails
            .click('@dbTableDropdown');
        dbTableDetails
            .waitForElementVisible(dbTableDetails.selector, 2000)
            .assert.attributeEquals('@host', 'data-validation-status', 'error')
            .setValue('@host','localhost')
            .assert.attributeEquals('@host', 'data-validation-status', 'ok')
            .assert.attributeEquals('@port', 'data-validation-status', 'error')
            .setValue('@port','1389')
            .assert.attributeEquals('@port', 'data-validation-status', 'ok')
            .assert.attributeEquals('@username', 'data-validation-status', 'error')
            .setValue('@username','test_user')
            .assert.attributeEquals('@username', 'data-validation-status', 'ok')
            .assert.attributeEquals('@password', 'data-validation-status', 'error')
            .setValue('@password','password')
            .assert.attributeEquals('@password', 'data-validation-status', 'ok')
            .assert.attributeEquals('@dbName', 'data-validation-status', 'error')
            .setValue('@dbName','testDB')
            .assert.attributeEquals('@dbName', 'data-validation-status', 'ok')
            .assert.attributeEquals('@table', 'data-validation-status', 'error')
            .setValue('@table','test_table')
            .assert.attributeEquals('@table', 'data-validation-status', 'ok')
            .assert.attributeEquals('@keyColumn', 'data-validation-status', 'error')
            .setValue('@keyColumn','TestKeyColumn')
            .assert.attributeEquals('@dbName', 'data-validation-status', 'ok')
            .clearValue('@jdbcDriver')
            .assert.attributeEquals('@jdbcDriver', 'data-validation-status', 'error')
            .setValue('@jdbcDriver','test.driver')
            .click('@jdbcDriver').api.pause(500);
        client
            .keys(['\uE004']).pause(500); //press TAB to change focus
        dbTableDetails
            .assert.attributeEquals('@jdbcDriver', 'data-validation-status', 'ok')
            .clearValue('@jdbcUrl')
            .assert.attributeEquals('@jdbcUrl', 'data-validation-status', 'error')
            .setValue('@jdbcUrl','test.url')
            .click('@jdbcUrl').api.pause(500);
        client
            .keys(['\uE004']).pause(500);
        dbTableDetails
            .assert.attributeEquals('@jdbcUrl', 'data-validation-status', 'ok');
        addConnector
            .click('@addConnectorButton')
            .waitForElementVisible('@errorMessage', 2000)
            .assert.containsText('@errorMessage', 'Error JDBC Driver is not found on classpath');
        client.execute(function() {
            $('#connectorErrorMessage').hide()
        });
    },
    'Test required LDAP fields': function (client) {
        var addConnector = client.page.connectorsAdd(),
            generalDetails = addConnector.section.generalDetails,
            ldapDetails = addConnector.section.ldapDetails;


        generalDetails
            .click('@ldapDropdown');
        ldapDetails
            .waitForElementVisible(ldapDetails.selector, 2000)
            .assert.attributeEquals('@hostName', 'data-validation-status', 'error')
            .setValue('@hostName','testlocalhost')
            .assert.attributeEquals('@hostName', 'data-validation-status', 'ok')
            .clearValue('@port')
            .assert.attributeEquals('@port', 'data-validation-status', 'error')
            .setValue('@port','1389')
            .click('@port').api.pause(500);
        client
            .keys(['\uE004']).pause(500); //press TAB to change focus
        ldapDetails
            .assert.attributeEquals('@port', 'data-validation-status', 'ok')
            .clearValue('@accountDN')
            .assert.attributeEquals('@accountDN', 'data-validation-status', 'error')
            .setValue('@accountDN','cn=Administrator')
            .click('@accountDN').api.pause(500);
        client
            .keys(['\uE004']).pause(500); //press TAB to change focus
        ldapDetails
            .assert.attributeEquals('@accountDN', 'data-validation-status', 'ok')
            .assert.attributeEquals('@password', 'data-validation-status', 'error')
            .setValue('@password','password')
            .assert.attributeEquals('@password', 'data-validation-status', 'ok')
            .clearValue('@baseDN')
            .assert.attributeEquals('@baseDN', 'data-validation-status', 'error')
            .setValue('@baseDN','cn=Users')
            .click('@baseDN').api.pause(500);
        client
            .keys(['\uE004']).pause(500); //press TAB to change focus
        ldapDetails
            .assert.attributeEquals('@baseDN', 'data-validation-status', 'ok');
        addConnector
            .click('@addConnectorButton')
            .waitForElementVisible('@errorMessage', 2000)
            .assert.containsText('@errorMessage', 'Error Unknown Host');
    },
    'Test required XML fields': function (client) {
        var addConnector = client.page.connectorsAdd(),
            generalDetails = addConnector.section.generalDetails,
            xmlDetails = addConnector.section.xmlDetails;

        generalDetails
            .click('@xmlDropdown');
        xmlDetails
            .waitForElementVisible(xmlDetails.selector, 2000)
            .assert.attributeEquals('@xsdPath', 'data-validation-status', 'error')
            .click('@xsdPath')
            .setValue('@xsdPath','testXSD')
            .assert.attributeEquals('@xsdPath', 'data-validation-status', 'ok')
            .assert.attributeEquals('@xmlPath', 'data-validation-status', 'error')
            .setValue('@xmlPath','testXML')
            .assert.attributeEquals('@xmlPath', 'data-validation-status', 'ok');
        addConnector
            .click('@addConnectorButton')
            .waitForElementVisible('@errorMessage', 2000)
            .assert.containsText('@errorMessage', 'Error Failed to parse XSD-schema from file');
    }
};
