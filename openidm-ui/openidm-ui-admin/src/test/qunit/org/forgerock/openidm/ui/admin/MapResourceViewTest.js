define([
    "org/forgerock/openidm/ui/admin/MapResourceView"
], function (MapResourceView) {
    QUnit.module('MapResourceView Tests');

    QUnit.test("Generate a mapping name based off of source and target", function (assert) {
        var targetDetails = {
                "name" : "targetTest",
                "resourceType" : "managed"
            },
            sourceDetails = {
                "name" : "sourceTest",
                "resourceType" : "connector"
            },
            generateDetails = MapResourceView.createMappingName(targetDetails, sourceDetails, null, "account");

        assert.equal(generateDetails.generatedName, "systemSourcetestAccount_managedTargettest", "Correctly generated mapping name based off of target and source information");
        assert.equal(generateDetails.source, "system/sourceTest/account", "Correctly identified connector location");
        assert.equal(generateDetails.target, "managed/targetTest", "Correctly identified managed object location");
    });
});