define([
    "org/forgerock/openidm/ui/admin/mapping/association/dataAssociationManagement/ChangeAssociationDialog"
],
function (ChangeAssociationDialog, _) {
    QUnit.module('ChangeAssociationDialog');

    QUnit.test("formatResult", function (assert) {
        var testObject = {"_id": "1234", "uid": "bboat", "givenName": "Boaty", "sn": "McBoatface"},
            testProps = ["uid","sn","givenName"],
            result = ChangeAssociationDialog.formatResult(testObject, testProps),
            div = $("<div>").append(result.objRep);

        assert.equal(ChangeAssociationDialog.formatResult(undefined, []), undefined, "Undefined value passed to formatResult returns undefined");
        assert.equal(result._id, "1234");
        assert.equal(div.find(".objectRepresentationHeader").attr("title"), "uid");
        assert.equal(div.find(".objectRepresentationHeader").text(), "bboat");
        assert.equal(div.find(".objectRepresentation[title=sn]").text(), "McBoatface");
    });
});
