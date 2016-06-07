define([
    "org/forgerock/openidm/ui/admin/settings/SocialConfigView",
    "org/forgerock/openidm/ui/admin/util/AdminUtils"
], function (SocialConfigView,
             AdminUtils) {
    QUnit.module('SocialConfigView Tests');

    QUnit.test("Generate card details", function (assert) {
        var cardDetails = SocialConfigView.getCardDetails($("<div data-type='test' data-name='testName'></div>"));

        assert.equal(cardDetails.type, "test", "Type extracted from card correctly");
        assert.equal(cardDetails.name, "testName", "Name extracted from card correctly");
    });

    QUnit.test("Generate saved social provider details", function (assert) {
        var oldData = {
            "client_id" : "configure_me",
            "client_secret" : null,
            "scope" : [
                "profile",
                "email",
                "openid"
            ]
        },
        newData = {
            "client_id" : "test",
            "client_secret" : "test",
            "scope" : [
                "test"
            ]
        },
        saveDetails = SocialConfigView.generateSaveData(newData, oldData);

        assert.equal(saveDetails.client_id, "test", "Client Id changed correctly");
        assert.equal(saveDetails.client_secret, "test", "Client secret changed correctly");

        oldData = saveDetails;

        newData.client_secret = null;

        saveDetails = SocialConfigView.generateSaveData(newData, oldData);

        assert.equal(saveDetails.client_secret, "test", "Client secret remains the same with null return");
    });

    QUnit.test("Convert name to proper capitalization", function (assert) {
        assert.equal(AdminUtils.capitalizeName("google"), "Google", "Name correctly capitalized");
    });
});