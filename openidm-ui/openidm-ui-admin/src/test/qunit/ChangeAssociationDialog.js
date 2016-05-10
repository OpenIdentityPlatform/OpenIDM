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
        "org/forgerock/openidm/ui/admin/mapping/association/dataAssociationManagement/ChangeAssociationDialog"
    ],
    function (ChangeAssociationDialog, _) {
        QUnit.module('ChangeAssociationDialog');

        QUnit.test("formatResult", function () {
            var testObject = {"_id": "1234", "uid": "bboat", "givenName": "Boaty", "sn": "McBoatface"},
                testProps = ["uid","sn","givenName"],
                result = ChangeAssociationDialog.formatResult(testObject, testProps),
                div = $("<div>").append(result.objRep);

            QUnit.equal(ChangeAssociationDialog.formatResult(undefined, []), undefined, "Undefined value passed to formatResult returns undefined");
            QUnit.equal(result._id, "1234");
            QUnit.equal(div.find(".objectRepresentationHeader").attr("title"), "uid");
            QUnit.equal(div.find(".objectRepresentationHeader").text(), "bboat");
            QUnit.equal(div.find(".objectRepresentation[title=sn]").text(), "McBoatface");
        });
    });
