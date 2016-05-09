var connector;

module.exports = {
    before: function(client, done) {
        client.globals.login.helpers.setSession(client, function() {
            client.config.read('sync', done);
        });
    },

    after: function(client, done) {
        client.config.reset('sync', function() {
            client.end();
            done();
        });
    },

    "It should name connectors 'system' when creating a new mapping": function(client) {
        var mappingAdd = client.page.mappingAdd(),
            newMappingName = mappingAdd.elements.newMappingName.selector;


        client
            .url(client.globals.baseUrl + '#mapping/')
            .refresh()
            .waitForElementVisible("#addMapping", 2000);


        client
            .timeoutsAsyncScript(2000)
            .executeAsync(function(conn, done) {
                require(['org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate'], function(connDel) {
                    window.connDel = connDel;
                    connDel.connectorDelegateCache.currentConnectors = [conn];

                    console.log('ok');
                    done();
                });
            }, [connector]);


        mappingAdd.navigate();
        mappingAdd
            // this now selects the first generic connector card
            .waitForElementPresent("@connector", 5000)
            .click("@connector").click("@connector").click("@connector");
        client.pause(50);
        mappingAdd
            .click("@managedUserObj");
        client.pause(50);
        mappingAdd
            .click("@createMappingButton");
        client.pause(1000);
        mappingAdd
            .click("@mappingSaveOkay");
        client
            .waitForElementVisible(newMappingName, 2000)
            .getText(newMappingName, function(result) {
                client.pause(250);
                // this checks that the resulting mapping matches 'system' for the connector
                client.assert.ok(result.value.match(/(system).+(_managedUser)/));
            });
        }
};

connector = {
    "name": "xmlfile",
    "enabled": true,
    "config": "config/provisioner.openicf/xml",
    "objectTypes": ["account"],
    "connectorRef": {
        "bundleName": "org.forgerock.openicf.connectors.xml-connector",
        "connectorName": "org.forgerock.openicf.connectors.xml.XMLConnector",
        "bundleVersion": "1.1.0.2"
    },
    "displayName": "XML Connector",
    "ok": true,
    "iconClass": "icon-xml",
    "iconSrc": "img/icon-xml.png",
    "displayObjectType": "account",
    "cleanUrlName": "provisioner.openicf_xml",
    "cleanEditName": "xml"
};
