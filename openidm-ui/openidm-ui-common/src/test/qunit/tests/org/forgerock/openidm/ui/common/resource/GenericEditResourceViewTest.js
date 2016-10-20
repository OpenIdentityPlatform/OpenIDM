define([
    "org/forgerock/openidm/ui/common/resource/GenericEditResourceView"
], function (GenericEditResourceView) {
    QUnit.module('GenericEditResourceView Tests');

    QUnit.test("handleArrayOfTypes", function(assert) {
        var schema = {
                properties : {
                    "prop1": {
                        "type": "array"
                    },
                    "prop2": {
                        "type": [
                            "string",
                            "null"
                        ]
                    },
                    "prop3": {
                        "type": "integer"
                    },
                    "prop4": {
                        "type": [
                            "object",
                            "null"
                        ]
                    }
                }
            },
            expectedResultSchema = {
                properties : {
                    "prop1": {
                        "type": "array"
                    },
                    "prop2": {
                        "type": "string"
                    },
                    "prop3": {
                        "type": "integer"
                    },
                    "prop4": {
                        "type": "object"
                    }
                }
            };

        assert.deepEqual(GenericEditResourceView.handleArrayOfTypes(schema),expectedResultSchema);
    });
});
