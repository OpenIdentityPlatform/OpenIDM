module.exports = {
    commands: [{
        loginWithCredentials: function(username, password) {
            this.waitForElementVisible("@credentialForm", 1000)
                .clearValue("@usernameField")
                .setValue("@usernameField", username)
                .clearValue("@passwordField")
                .setValue("@passwordField", password)
                .click("@loginButton")
                .api.pause(200);
            return this;
        },
        loginWithBadCredentials: function () {
            return this.loginWithCredentials("badUser", "badPass");
        }
    }],
    elements: {
        credentialForm: "#loginDialog",
        usernameField: "#loginDialog #login",
        passwordField: "#loginDialog #password",
        loginButton: "#loginDialogSubmitButton"
    }
};
