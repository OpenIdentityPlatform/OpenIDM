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
    'Connector Successfully Deleted' : function(client) {
        var connectors = client.page.connectors(),
            connectorList = connectors.section.connectorList,
            connectorModal = connectors.section.connectorModal,
            message = connectors.section.message;

        connectorList
            .waitForElementVisible('@connectorCard', 2000)
            .click('@connectorToggle')
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
