define([
    "org/forgerock/openidm/ui/admin/connector/AbstractConnectorView"
], function (AbstractConnectorView) {
    QUnit.module('AbstractConnectorView Tests');
    QUnit.test("#cleanseObject traverses object removing all empty strings and falsy array elements", function (assert) {
        let uncleanObj = {
                a: "",
                b: {
                    a: "",
                    b: {
                        a: ["", "foo", null, "bar", undefined]
                    }
                },
                c: "foo"
            },
            cleanObj = {
                a: null,
                b: {
                    a: null,
                    b: {
                        a: ["foo", "bar"]
                    }
                },
                c: "foo"
            },
            testView = new AbstractConnectorView();
        assert.deepEqual(testView.cleanseObject(uncleanObj), cleanObj, "AbstractConnectorView #cleanseObject");
    });
});
