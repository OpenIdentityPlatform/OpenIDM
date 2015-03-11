/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global require, define, QUnit, $ */

define([
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/admin/managed/AddEditManagedView",
    "org/forgerock/openidm/ui/admin/ResourcesView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "../mocks/resourceDetails"
], function (constants, router, eventManager, addEditManagedView, resourcesView, ConfigDelegate, resourceDetails) {

    return {
        executeAll: function (server) {
            module('Admin Managed Object UI Functions');

            QUnit.asyncTest("Managed Objects Add/Edit", function () {
                var stubbedSave,
                    saveCheck = false;

                resourceDetails(server);

                addEditManagedView.render([], function () {

                    addEditManagedView.$el.find("#addManagedProperties").trigger("click");

                    QUnit.equal(addEditManagedView.$el.find(".managed-property:visible").length, 1, "Add object type property successful");

                    addEditManagedView.$el.find(".managed-property:visible .remove-btn").trigger("click");

                    _.delay(function(){
                        $(".bootstrap-dialog .btn-primary").trigger("click");

                        QUnit.equal(addEditManagedView.$el.find(".add-remove-block:visible").length, 0, "Delete object type property successful");

                        setTimeout(function() {
                            addEditManagedView.$el.find("#managedObjectName").val("testname");
                            addEditManagedView.$el.find("#managedObjectName").trigger("blur");

                            $("#managedObjectName").val("role");

                            QUnit.ok(addEditManagedView.$el.find("#addEditManaged").is(":disabled") === false, "Submit button enabled");

                            addEditManagedView.$el.find("#addEditManaged").trigger("click");

                            QUnit.equal($("#managedErrorMessage").length, 1, "Duplicate name successfully detected");

                            QUnit.ok(addEditManagedView.$el.find("#addEditManaged").is(":disabled") === true, "Submit button disabled");

                            $("#managedObjectName").val("test1234");
                            addEditManagedView.$el.find("#managedObjectName").trigger("blur");

                            QUnit.ok(addEditManagedView.$el.find("#addEditManaged").is(":disabled") === false, "Submit button enabled after new name");

                            // Create a complex Schema with nested values and set that to the JSONEditor instance
                            var jsonEditorFormat = {"title":"testTitle","description":"Test Description","properties":[{"propertyName":"stringString","title":"Test String","description":"Test string desciption","viewable":false,"searchable":false,"required":true,"type":""},{"propertyName":"testArrayOfStrings","title":"Test Array of String","description":"Description of the test array of strings","viewable":true,"searchable":true,"required":true,"type":{"itemType":""}},{"propertyName":"testBoolean","title":"Test Boolean","description":"Test boolean description","viewable":false,"searchable":true,"required":false,"type":""},{"propertyName":"testInteger","title":"Test Integer","description":"Description of the test integer","viewable":false,"searchable":false,"required":false,"type":""},{"propertyName":"testNumber","title":"Test Number","description":"Test Number description","viewable":true,"searchable":false,"required":false,"type":""},{"propertyName":"testObjectWithComplexProperties","title":"Test Object with Complex Properties","description":"The description for the test object containing complex properties","viewable":false,"searchable":false,"required":false,"type":[{"propertyName":"simpleProp","title":"","description":"","viewable":false,"searchable":false,"required":false,"type":""},{"propertyName":"complexArrayProp","title":"","description":"An array with an array of strings","viewable":false,"searchable":false,"required":false,"type":{"itemType":{"itemType":""}}}]},{"propertyName":"testResourceCollection","title":"Test Resource Collection","description":"Description of the test resource collection","viewable":false,"searchable":false,"required":false,"type":{"path":"test/path","query":{"queryFilter":"testQueryFilter","fields":["testField"],"sortKeys":["testKey","testKey2"]}}}]};
                            var jsonSchema = {"$schema":"http://forgerock.org/json-schema#","type":"object","title":"testTitle","description":"Test Description","properties":{"stringString":{"description":"Test string desciption","title":"Test String","viewable":false,"searchable":false,"type":"string"},"testArrayOfStrings":{"description":"Description of the test array of strings","title":"Test Array of String","viewable":true,"searchable":true,"type":"string"},"testBoolean":{"description":"Test boolean description","title":"Test Boolean","viewable":false,"searchable":true,"type":"string"},"testInteger":{"description":"Description of the test integer","title":"Test Integer","viewable":false,"searchable":false,"type":"string"},"testNumber":{"description":"Test Number description","title":"Test Number","viewable":true,"searchable":false,"type":"string"},"testObjectWithComplexProperties":{"description":"The description for the test object containing complex properties","title":"Test Object with Complex Properties","viewable":false,"searchable":false,"type":"string"},"testResourceCollection":{"description":"Description of the test resource collection","title":"Test Resource Collection","viewable":false,"searchable":false,"resourceCollection":{"path":"test/path","query":{"queryFilter":"testQueryFilter","fields":["testField"],"sortKeys":["testKey","testKey2"]}},"type":"string"}},"required":["stringString","testArrayOfStrings"],"order":["stringString","testArrayOfStrings","testBoolean","testInteger","testNumber","testObjectWithComplexProperties","testResourceCollection"]};

                            addEditManagedView.data.managedObjectSchema.setValue(jsonEditorFormat);

                            // On save JSONEditor format is converted into valid schema: http://forgerock.org/json-schema
                            stubbedSave = sinon.stub(ConfigDelegate, "updateEntity", function(name, managedObject){
                                if (name === "managed") {
                                    var savedObj = _.find(managedObject.objects, function(obj) {
                                        return obj.name === "test1234";
                                    });

                                    QUnit.deepEqual(savedObj.schema, jsonSchema, "JSONEditor JSON has converted properly into ForgeRock Schema");
                                }

                                saveCheck = true;
                            });

                            addEditManagedView.$el.find("#addEditManaged").trigger("click");

                            QUnit.ok(saveCheck, "Save successful");

                            stubbedSave.restore();

                            QUnit.start();
                        }, 100);
                    }, 200);
                });

            });
        }

    };

});