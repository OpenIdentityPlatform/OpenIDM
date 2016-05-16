/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

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
