define([
    "org/forgerock/openidm/ui/admin/mapping/MappingListView"
], function (MappingListView) {
    QUnit.module('Mapping List Tests');

    QUnit.test("Mapping missing check", function () {
        var testResource = {};

        testResource = MappingListView.setCardState(testResource, "managed", "managed/user", {objects: []});

        QUnit.equal(testResource.isMissing, true, "Detected missing managed object from mapping list");

        testResource = {
            config: "config/provisioner.openicf/ldap",
            displayName: "LDAP Connector",
            connectorRef: {
                connectorName : "org.identityconnectors.ldap.LdapConnector"
            }
        };

        testResource = MappingListView.setCardState(testResource, "ldap", "system/ldap/account", {objects: []});

        QUnit.equal(testResource.isMissing, undefined, "Connector properly detected and not set to missing state");

        testResource = {};

        testResource = MappingListView.setCardState(testResource, "ldap", "system/ldap/account", {objects: []});

        QUnit.equal(testResource.isMissing, true, "Connector missing and set to missing state");
    });
});
