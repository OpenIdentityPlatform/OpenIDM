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
        generalDetails.expect.element('@enabledToggle').to.be.selected;
        dbTableDetails.expect.element('@emptyStringToggle').to.not.be.selected;
        dbTableDetails.expect.element('@rethrowSQLToggle').to.be.selected;
        dbTableDetails.expect.element('@nativeTimeToggle').to.not.be.selected;
        dbTableDetails.expect.element('@allNativeToggle').to.not.be.selected;

        client.elements('css selector', 'div.checkbox label', function (elements) {
            elements.value.forEach(function(element) {
                client.elementIdClick(element.ELEMENT, function(result) {
                    client.verify.equal(result.state, 'success');
                }).pause(200);
            });
        });
        generalDetails.expect.element('@enabledToggle').to.not.be.selected;
        dbTableDetails.expect.element('@emptyStringToggle').to.be.selected;
        dbTableDetails.expect.element('@rethrowSQLToggle').to.not.be.selected;
        dbTableDetails.expect.element('@nativeTimeToggle').to.be.selected;
        dbTableDetails.expect.element('@allNativeToggle').to.be.selected;
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

        xmlDetails.expect.element('@createIfToggle').to.not.be.selected;

        client.execute(function() {
            $('#xmlCreateIfNotExists').parent().click();
        });
        xmlDetails.expect.element('@createIfToggle').to.be.selected;

        xmlDetails
            .click('#xmlCreateIfNotExists');
    }
};
