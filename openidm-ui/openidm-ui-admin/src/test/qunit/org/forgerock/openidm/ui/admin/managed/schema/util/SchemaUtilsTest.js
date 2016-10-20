define([
    "lodash",
    "org/forgerock/openidm/ui/admin/managed/schema/util/SchemaUtils"
], function (_, SchemaUtils) {
    QUnit.module('SchemaUtils Tests');
    var managedJSONProperties = {
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
        },
        schemaEditorProperties = {
            "prop1": {
                "type": "array"
            },
            "prop2": {
                "type": "string",
                "nullable": true
            },
            "prop3": {
                "type": "integer"
            },
            "prop4": {
                "type": "object",
                "nullable": true
            }
        };

    QUnit.test("setNullableProperties", function(assert) {
        assert.deepEqual(SchemaUtils.setNullableProperties(_.cloneDeep(schemaEditorProperties)), managedJSONProperties);
    });

    QUnit.test("getNullableProperties", function(assert) {
        assert.deepEqual(SchemaUtils.getNullableProperties(_.cloneDeep(managedJSONProperties)), schemaEditorProperties);
    });
});
