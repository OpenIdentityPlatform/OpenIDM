define([
    "org/forgerock/openidm/ui/admin/mapping/AddMappingView"
], function (AddMappingView) {
    QUnit.module('AddMappingView Tests');

    QUnit.test("Preselect mapping card based on URL", function () {
        var connectors = [
                {
                    "name" : "ldap"
                }
            ],
            managed = [
                {
                    "name" : "User"
                }
            ],
            selectConnector = ["connector", "ldap"],
            selectManaged = ["managed", "User"],
            selectResult = AddMappingView.preselectMappingCard(selectConnector, connectors, managed);

            QUnit.equal(selectResult.name, "ldap", "Successfully selected LDAP connector");
            QUnit.equal(selectResult.resourceType, "connector", "Successfully detected and set resource type of connector");

            selectResult = AddMappingView.preselectMappingCard(selectManaged, connectors, managed);

            QUnit.equal(selectResult.name, "User", "Successfully selected User managed object");
            QUnit.equal(selectResult.resourceType, "managed", "Successfully detected and set resource type of managed");
    });

    QUnit.test("Add mapping card based on card location in dom", function () {
        var connectors = [
                {
                    "name" : "ldap"
                }
            ],
            managed = [
                {
                    "name" : "User"
                }
            ],
            selectResult;

        selectResult = AddMappingView.addResourceMapping("connector", 0, connectors, managed);

        QUnit.equal(selectResult.name, "ldap", "Successfully selected LDAP connector");
        QUnit.equal(selectResult.resourceType, "connector", "Successfully detected and set resource type of connector");

        selectResult = AddMappingView.addResourceMapping("managed", 0, connectors, managed);

        QUnit.equal(selectResult.name, "User", "Successfully selected User managed object");
        QUnit.equal(selectResult.resourceType, "managed", "Successfully detected and set resource type of managed");
    });

    QUnit.test("Add correct display meta data for connectors based on connector config", function () {
        var connector = {
                "name" : "ldap",
                "objectTypes" : ["user", "roles"],
                "config" : "config/provisioner.openicf/ldap"
            },
            displayConnector;

        displayConnector = AddMappingView.setupDisplayConnector(connector);

        QUnit.equal(displayConnector.displayObjectType, "roles, user", "Successfully setup object type display");
        QUnit.equal(displayConnector.cleanUrlName, "provisioner.openicf_ldap", "Successfully create the clean URL name");
    });
});
