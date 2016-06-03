module.exports = {
    before: function(client) {
        client.globals.login.helpers.login(client);
    },
    after: function(client) {
        client.end();
    },
    'Change Connector Type to Database Table': function (client) {
        var addConnector = client.page.connectorsAdd(),
            csvDetails = addConnector.section.csvDetails,
            dbTableDetails = addConnector.section.dbTableDetails,
            generalDetails = addConnector.section.generalDetails;

        addConnector
            .navigate()
            .waitForElementVisible('@generalDetails', 2000);
        csvDetails
            .waitForElementVisible('@csvFile', 2000);
        generalDetails
            .click('@dbTableDropdown');
        dbTableDetails
            .waitForElementVisible(dbTableDetails.selector, 2000);
    },
    'Test "Enabled" and "DB Table" Toggles': function (client) {
        var addConnector = client.page.connectorsAdd(),
            dbTableDetails = addConnector.section.dbTableDetails,
            generalDetails = addConnector.section.generalDetails;
        // FireFox
        generalDetails
            .assert.attributeEquals('@enabledToggle', 'value', 'true');
        dbTableDetails
            .assert.attributeEquals('@emptyStringToggle', 'value', 'false')
            .assert.attributeEquals('@rethrowSQLToggle', 'value', 'true')
            .assert.attributeEquals('@nativeTimeToggle', 'value', 'false')
            .assert.attributeEquals('@allNativeToggle', 'value', 'false');

        client.elements('css selector', 'div.checkbox label', function (elements) {
            elements.value.forEach(function(element) {
                client.elementIdClick(element.ELEMENT, function(result) {
                    client.verify.equal(result.state, 'success');
                }).pause(200);
            });
        });
        generalDetails
            .assert.attributeEquals('@enabledToggle', 'value', 'false');
        dbTableDetails
            .verify.attributeEquals('@emptyStringToggle', 'value', 'true')
            .verify.attributeEquals('@rethrowSQLToggle', 'value', 'false')
            .verify.attributeEquals('@nativeTimeToggle', 'value', 'true')
            .verify.attributeEquals('@allNativeToggle', 'value', 'true');
    },
    'Change Connector Type to XML Connector': function (client) {
        var addConnector = client.page.connectorsAdd(),
            xmlDetails = addConnector.section.xmlDetails,
            generalDetails = addConnector.section.generalDetails;

        generalDetails
            .click('@xmlDropdown');
        xmlDetails
            .waitForElementVisible(xmlDetails.selector, 2000);
        },
    'Test Toggle XML': function(client) {
        var addConnector = client.page.connectorsAdd(),
            xmlDetails = addConnector.section.xmlDetails;

        xmlDetails
            .assert.attributeEquals('@createIfToggle', 'value', 'false');
        client.execute(function() {
            $('#xmlCreateIfNotExists').parent().click();
        });
        xmlDetails
            .assert.attributeEquals('@createIfToggle', 'value', 'true')
            .click('#xmlCreateIfNotExists');
    }
};
