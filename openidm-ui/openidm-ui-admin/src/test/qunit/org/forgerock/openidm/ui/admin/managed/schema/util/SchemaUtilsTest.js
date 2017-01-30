define([
    "lodash",
    "org/forgerock/openidm/ui/admin/managed/schema/util/SchemaUtils"
], function (_, SchemaUtils) {
    var schemaObject,
        schemaPropertiesArray,
        arrayItems;

    QUnit.module('SchemaUtils Tests');

    QUnit.test("convertSchemaToPropertiesArray test", function () {
        QUnit.deepEqual(SchemaUtils.convertSchemaToPropertiesArray(_.cloneDeep(schemaObject)), schemaPropertiesArray, "Schema object properly converted to array");
    });

    QUnit.test("convertPropertiesArrayToSchema test", function () {
        QUnit.deepEqual(SchemaUtils.convertPropertiesArrayToSchema(_.cloneDeep(schemaPropertiesArray)), schemaObject, "Schema property array properly converted to object");
    });

    QUnit.test("handleArrayNest test", function () {
        QUnit.deepEqual(SchemaUtils.handleArrayNest(_.cloneDeep(arrayItems)), arrayItems.items.items.items.items, "Nested array items handled properly");
    });

    schemaObject = {
        "properties": {
            "serialNumber": {
                "description": "",
                "title": "SerialNumber",
                "viewable": true,
                "searchable": true,
                "userEditable": true,
                "type": "string"
            },
            "name": {
                "description": "",
                "title": "Name",
                "viewable": true,
                "searchable": true,
                "userEditable": true,
                "type": "string"
            }
        },
        "required": ["serialNumber"],
        "order": ["serialNumber","name"]
    };

    schemaPropertiesArray = [
        {
            "description": "",
            "title": "SerialNumber",
            "viewable": true,
            "searchable": true,
            "userEditable": true,
            "type": "string",
            "required": true,
            "propName": "serialNumber"
        },
        {
            "description": "",
            "title": "Name",
            "viewable": true,
            "searchable": true,
            "userEditable": true,
            "type": "string",
            "required": false,
            "propName": "name"
        }
    ];

    arrayItems = {
        "type" : "array",
        "items" : {
            "type" : "array",
            "items" : {
                "type" : "array",
                "items" : {
                    "type" : "array",
                    "items" : {
                        "type" : "object",
                        "properties" : {
                            "nestedProp" : {
                                "title" : "Nested Property",
                                "type" : "string",
                                "viewable" : true,
                                "searchable" : true,
                                "userEditable" : true
                            }
                        },
                        "order" : [
                            "nestedProp"
                        ],
                        "required" : [ ]
                    }
                }
            }
        }
    };
});
