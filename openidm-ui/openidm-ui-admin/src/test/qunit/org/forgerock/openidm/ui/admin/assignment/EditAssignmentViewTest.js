define([
    "org/forgerock/openidm/ui/admin/assignment/EditAssignmentView"
], function (EditAssignmentView) {
    QUnit.module('EditAssignmentView Tests');


    QUnit.test("Delete attribute from assignment", function () {
        var fakeDom = $("<div id='group'><div class='list-item'></div><div class='list-item'></div></div>"),
            schema = [{
                "name" : "schema1"
            }, {
                "name" : "schema2"
            }],
            assignmentAttributes = [{
                "name" : "attribute1"
            }, {
                "name" : "attribute2"
            }],
            deleteDetails;

        deleteDetails = EditAssignmentView.deleteAttribute($(fakeDom.find(".list-item")[0]), fakeDom.find(".list-item"), schema, assignmentAttributes);

        QUnit.equal(deleteDetails.schemaEditors.length, 1, "Removed the schema editor");
        QUnit.equal(deleteDetails.assignmentAttributes.length, 1, "Removed the assignment attributes");
        QUnit.equal(fakeDom.find(".list-item").length, 1, "Removed HTML representation of the attribute");
    });

    QUnit.test("Save assignment attribute", function () {
        var fakeDom = $("<div><input type='text' class='form-control name' value='test' /><input type='text' class='form-control name' value='test2' /></div>"),
            editors = $("<div><input type='text' class='editor form-control' value='fake' /><input type='text' class='editor form-control' value='fake2' /></div>"),
            assignmentAttributes = [{
                "name" : "",
                "value" : ""
            }, {
                "name" : "",
                "value" : ""
            }];

        editors = [$(editors.find(".editor")[0]), $(editors.find(".editor")[1])];

        assignmentAttributes = EditAssignmentView.saveAssignmentAttribute(fakeDom.find(".name"), assignmentAttributes, editors);

        QUnit.equal(assignmentAttributes[0].name, "test", "Updated assignment attribute name");
        QUnit.equal(assignmentAttributes[0].value, "fake", "Updated assignment attribute value");
        QUnit.equal(assignmentAttributes[1].name, "test2", "Updated second assignment attribute name");
        QUnit.equal(assignmentAttributes[1].value, "fake2", "Updated second assignment attribute value");
    });

    QUnit.test("Save assignment attribute operation events", function () {
        var onAssignment = $("<select selected='selected'><option>test</option></select>"),
            onUnassignment = $("<select selected='selected'><option>test2</option></select>"),
            assignmentAttributes = {
                "assignmentOperation" : "",
                "unassignmentOperation" : ""
            };

        assignmentAttributes = EditAssignmentView.saveOperations(assignmentAttributes, onAssignment, onUnassignment);

        QUnit.equal(assignmentAttributes.assignmentOperation, "test", "Updated assignment attribute name");
        QUnit.equal(assignmentAttributes.unassignmentOperation, "test2", "Updated assignment attribute value");
    });
});