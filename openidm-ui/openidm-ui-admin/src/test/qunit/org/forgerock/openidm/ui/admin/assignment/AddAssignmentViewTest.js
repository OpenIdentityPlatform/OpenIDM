define([
    "org/forgerock/openidm/ui/admin/assignment/AddAssignmentView"
], function (AddAssignmentView) {
    QUnit.module('AddAssignmentView Tests');


    QUnit.test("Check if mappings available for assignment use", function () {
        var mappings = [],
            noMappingsAvailable;

        noMappingsAvailable  = AddAssignmentView.findMappings(mappings);

        QUnit.equal(noMappingsAvailable, true, "No mappings found for assignment use");

        mappings.push({});
        noMappingsAvailable  = AddAssignmentView.findMappings(mappings);

        QUnit.equal(noMappingsAvailable, false, "Mappings found for assignment use");
    });
});