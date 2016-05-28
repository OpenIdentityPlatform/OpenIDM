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
