module.exports = {
    before: function(client) {
        client.globals.login.helpers.login(client);

        client.execute(function(data) {
                require(["org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
                    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate"],
                    function (ConfigDelegate,
                              ConnectorDelegate) {

                        var connectorDetails = {
                            "connectorRef":{
                                "connectorName":"org.forgerock.openicf.connectors.xml.XMLConnector",
                                "displayName":"XML Connector",
                                "bundleName":"org.forgerock.openicf.connectors.xml-connector",
                                "systemType":"provisioner.openicf",
                                "bundleVersion":"1.1.0.2"
                            },
                            "name":"testtest"
                        };

                        ConfigDelegate.createEntity("provisioner.openicf/test_test", connectorDetails).then(_.bind(function () {
                            ConnectorDelegate.deleteCurrentConnectorsCache();

                            return true;
                        }, this));
                    });

            }, ["test"], function() {}
        );
    },
    after: function(client) {
       client.end();
    },
    'Connector List Page Loaded': function (client) {
        var connectors = client.page.connectors(),
            connectorList = connectors.section.connectorList;

        client.pause(2000);

        connectors
            .navigate();

        connectorList
            .waitForElementVisible('@addCard', 2000);
    },
    'Connector List Display Toggle' : function(client) {
        var connectors = client.page.connectors(),
            connectorList = connectors.section.connectorList;

        connectorList
            .click('@toggleGridViewButton')
            .waitForElementVisible('@gridView', 1000)
            .assert.visible('@gridView', "Successfully displaying grid")
            .click('@toggleCardViewButton')
            .assert.visible('@addCard', "Successfully displaying cards")
            .waitForElementVisible('@addCard', 1000);
    },
    'Connector List Filter' : function(client) {
        var connectors = client.page.connectors(),
            connectorList = connectors.section.connectorList;

        connectorList
            .click('@connectorListFilter')
            .setValue('@connectorListFilter', 'noo');

        client
            .pause(2000);

        connectorList
            .clearValue('@connectorListFilter')
            .assert.hidden('@cards', "Card filtered out of display")
            .click('@connectorListFilter')
            .setValue('@connectorListFilter', 'tes');

        client
            .pause(2000);

        connectorList
            .assert.visible('@cards', "Card no longer filtered out of display");
    },
    'Connector grid view drop down' : function(client) {
        var connectors = client.page.connectors(),
            connectorList = connectors.section.connectorList;

        connectorList
            .click('@toggleGridViewButton')
            .waitForElementVisible('@gridView', 1000)
            .click('@connectorGridToggle')
            .waitForElementVisible('@connectorGridToggle', 1000)
            .click('@toggleCardViewButton')
            .waitForElementVisible('@addCard', 1000);
    },
    'New Connector Page Change' : function(client) {
        var connectors = client.page.connectors(),
            connectorList = connectors.section.connectorList;

        connectorList
            .click('@addCard')
            .waitForElementVisible('@backToConnector', 1000)
            .click('@backToConnector')
            .waitForElementVisible('@addCard', 1000);

     },
    'Connector Successfully Deleted' : function(client) {
        var connectors = client.page.connectors(),
            connectorList = connectors.section.connectorList,
            connectorModal = connectors.section.connectorModal,
            message = connectors.section.message;

        connectorList
            .waitForElementVisible('@connectorCard', 2000)
            .click('@connectorListToggle')
            .waitForElementVisible('@connectorDropdown', 1000)
            .click('@connectorDelete');

        connectorModal
            .waitForElementVisible('@connectorDeleteDialog', 3000)
            .click("@connectorDeleteDialogOkay");

        client.pause(2000);

        message
            .expect.element('@displayMessage').text.to.equal("Connector successfully deleted.");
    }
};
