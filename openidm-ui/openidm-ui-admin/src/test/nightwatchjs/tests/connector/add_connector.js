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
    'Load CSV Form': function (client) {
        var addConnector = client.page.connectorsAdd();

        addConnector
            .waitForElementVisible('@connectorForm', 2000)
            .assert.urlContains('@href')
            .waitForElementPresent('@addConnectorButton', 2000);
    },
    'Add Non Existent CSV Connector': function (client) {
        var addConnector = client.page.connectorsAdd(),
            csvDetails = addConnector.section.csvDetails;

        addConnector
            .assert.attributeEquals('@connectorName', 'data-validation-status', 'error')
            .setValue('@connectorName','CSV')
            .assert.attributeEquals('@connectorName', 'data-validation-status', 'ok');
        csvDetails
            .assert.attributeEquals('@csvFile', 'data-validation-status', 'error')
            .setValue('@csvFile','CSVfile.csv')
            .assert.attributeEquals('@csvFile', 'data-validation-status', 'ok');
        addConnector
            .click('@addConnectorButton')
            .waitForElementVisible('@errorMessage', 2000)
            .assert.containsText('@errorMessage', 'Error File CSVfile.csv does not exist')
        // cleanup
            .clearValue('@connectorName');
        csvDetails
            .clearValue('@csvFile');
    },
    'Change Connector Type to Database Table': function (client) {
        var addConnector = client.page.connectorsAdd(),
            csvDetails = addConnector.section.csvDetails,
            dbTableDetails = addConnector.section.dbTableDetails,
            generalDetails = addConnector.section.generalDetails;

        csvDetails
            .waitForElementVisible('@csvFile', 2000);
        generalDetails
            .click('@dbTableDropdown');
        dbTableDetails
            .waitForElementVisible(dbTableDetails.selector, 2000);
    },
    'Complete Required Form Fields Before "Add Connector" is selectable': function(client) {
        var addConnector = client.page.connectorsAdd(),
            dbTableDetails = addConnector.section.dbTableDetails;

        addConnector
            .assert.attributeEquals('@addConnectorButton', 'disabled', 'true')
            .setValue('@connectorName','dbTable')
            .assert.attributeEquals('@addConnectorButton', 'disabled', 'true')
        dbTableDetails
            .setValue('@table','Table');
        addConnector
            .assert.attributeEquals('@addConnectorButton', 'disabled', 'true');
        dbTableDetails
            .setValue('@keyColumn','KeyColumn');
        addConnector
            .getAttribute('@addConnectorButton', 'disabled', function(result) {
                client.assert.equal(result.state, 'success');
                client.assert.equal(result.value, null);
            });
    }
};
