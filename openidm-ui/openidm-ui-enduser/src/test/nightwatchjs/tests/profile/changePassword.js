module.exports = {
    before: function (client, done) {
        client.globals.createUser(client, function () {
            client.globals.setSession(client, client.globals.nonAdminUser, done);
        });
    },
    after: function (client) {
        client.globals.endSession(client, function () {
            client.globals.deleteUser(client, function () {
                client.end();
            });
        });
    },
    'User can change their password': function (client) {
        // OPENIDM-5309
        var profilePasswordPage = client.page.profilePassword();

        profilePasswordPage
            .navigate()
            .waitForElementPresent("@newPassword", 2000)
            .setValue("@newPassword", "Passw0rd2")
            .setValue("@confirmPassword", "Passw0rd2")
            .click("@newPassword"); // lame way to trigger a change event on confirmPassword

        profilePasswordPage.expect.element("@saveButton").to.be.enabled;

        profilePasswordPage
            .click("@saveButton")
            .waitForElementPresent("@currentPassword", 2000)
            .setValue("@currentPassword", client.globals.nonAdminUser.password)
            .click("@confirmPasswordButton")
            .api.pause(500);

        client.globals.endSession(client, function () {
            client.globals.setSession(client, {
                userName: client.globals.nonAdminUser.userName,
                password: "Passw0rd2"
            }, function (response) {
                client.assert.equal(response.value.authenticationId, client.globals.nonAdminUser.userName, "Session established with new password");
            });
        });

    }
};
