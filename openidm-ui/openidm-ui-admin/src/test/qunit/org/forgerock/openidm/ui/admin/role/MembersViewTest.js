define([
    "org/forgerock/openidm/ui/admin/role/MembersView",
    "jquery"
], function (MembersView, $) {
    //this is a table with 3 data rows 2 of which are conditional
    var membersListHTML = '<table>' +
    '<tr><td class="select-row-cell"><input type="checkbox"></td><td>conditional</td></tr>' +
    '<tr><td class="select-row-cell"><input type="checkbox"></td><td>conditional</td></tr>' +
    '<tr><td class="select-row-cell"><input type="checkbox"></td><td></td></tr>' +
    '</table>';

    QUnit.module('MembersView Tests');

    QUnit.test("Checkboxes on grid rows that have _grantType = 'condtion' have been removed", function () {
        var membersList = $(membersListHTML),
            checkboxesBefore = membersList.find("td.select-row-cell input:checkbox"),
            checkboxesAfter;


        QUnit.equal(checkboxesBefore.length, 3, "Correct number of checkboxes are displayed before removing the ones for conditional grants");

        MembersView.removeConditionalGrantCheckboxes(membersList);

        checkboxesAfter = membersList.find("td.select-row-cell input:checkbox");

        QUnit.equal(checkboxesAfter.length, 1, "Correct number of checkboxes are displayed after removing the ones for conditional grants");
    });

});
