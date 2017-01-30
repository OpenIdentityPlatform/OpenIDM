define([
    "org/forgerock/openidm/ui/admin/managed/AbstractManagedView"
], function (AbstractManagedView) {
    QUnit.module('AbstractManagedView Tests');

    QUnit.test("Prevent managed objects having duplicate names", function () {
        var managedObjectList = [
            {
                "name" : "test"
            }
            ],
            testView = new AbstractManagedView();

        QUnit.equal(testView.checkManagedName("test", managedObjectList), true, "Found managed object with existing name");
        QUnit.equal(testView.checkManagedName("fake", managedObjectList), false, "No managed object with same name");
    });
});
