define([
    "org/forgerock/openidm/ui/admin/mapping/association/ReconciliationQueryFiltersView"
], function (ReconciliationQueryFiltersView) {
    QUnit.module('ReconciliationQueryFiltersView Tests');

    QUnit.test("Preserve correct mapping details across mapping sections", function (assert) {
        var sampleSync = {
            "stuff" : "test",
            "recon" : {}
        };

        ReconciliationQueryFiltersView.setCurrentMapping(sampleSync);

        assert.equal(ReconciliationQueryFiltersView.getCurrentMapping().recon, undefined, "Clean mapping results without persisted recon details");
    });
});