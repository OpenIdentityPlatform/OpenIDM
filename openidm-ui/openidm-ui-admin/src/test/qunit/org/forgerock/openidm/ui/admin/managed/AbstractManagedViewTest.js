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

    QUnit.test("Combine properties into schema", function () {
        var managedObject = {
                "schema" : {
                    "properties" : {
                        "test" : {}
                    }
                },
                "properties" : [
                    {
                        "name" : "test",
                        "fakeProp" : "test"
                    }
                ]
            },
            testView = new AbstractManagedView();

        managedObject = testView.combineSchemaAndProperties(managedObject);

        QUnit.equal(managedObject.properties, undefined, "Successfully removed properties array");
        QUnit.equal(managedObject.schema.properties.test.fakeProp, "test", "Successfully copied over property to schema property");
    });

    QUnit.test("Properly remove preferences from schema", function () {
        var managedObject = {
                "schema" : {
                    "properties" : {
                        "preferences" : []
                    },
                    "order" : []
                }
            },
            testView = new AbstractManagedView();

        testView.getManagedPreferences = function() {
            return {};
        };

        managedObject = testView.handlePreferences(managedObject);

        QUnit.equal(managedObject.schema.properties.preferences, undefined, "Successfully removed preferences array");

        testView.getManagedPreferences = function() {
            return {
                "test" : "test"
            };
        };

        managedObject = testView.handlePreferences(managedObject);

        QUnit.equal(managedObject.schema.properties.preferences.returnByDefault, false, "Successfully added full preference schema");
    });
});