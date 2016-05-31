var numRoleProperties,
    numUserProperties;

module.exports = {
    before: function (client, done) {
        client.globals.login.helpers.setSession(client, done);

        client.globals.config.read("managed" , function(config) {
            client.execute(function(name, config) {
                return _.size(_.filter(config.objects, {"name": name})[0].schema.properties);
            }, ["user", config], function(num) {
                numUserProperties = num.value;
            });

            client.execute(function(name, config) {
                return _.size(_.filter(config.objects, {"name": name})[0].schema.properties);
            }, ["role", config], function(num) {
                numRoleProperties = num.value;
            })
        });

    },
    after: function (client, done) {
        client.globals.config.resetAll(function() {
            client.end();
            done();
        });
    },
    "Password reset smart fields are present": function (client) {
        var passwordResetPage = client.page.passwordReset();

        passwordResetPage.loadPage();

        passwordResetPage
            .waitForElementPresent("@resourceDropdown", 2000)
            .toggleSlider()
            .expect.element("@resourceDropdown").to.not.have.attribute("disabled");

        //reCaptcha
        passwordResetPage
            .waitForElementPresent("@reCaptchaStep", 2000)
            .assert.cssClassPresent("@reCaptchaStep", "disabled")
            .assert.visible("@reCaptchaStepToggle")
            .assert.hidden("@reCaptchaStepToggleEdit");

        // User Query Form
        passwordResetPage
            .waitForElementPresent("@userQueryFormStep", 2000)
            .assert.cssClassNotPresent("@userQueryFormStep", "disabled")
            .assert.elementNotPresent("@userQueryFormStepToggle")
            .assert.visible("@userQueryFormStepEdit");

        // Email Validation
        passwordResetPage
            .waitForElementPresent("@emailValidationStep", 2000)
            .assert.cssClassNotPresent("@emailValidationStep", "disabled")
            .assert.visible("@emailValidationStepToggle")
            .assert.visible("@emailValidationStepEdit");

        // KBA Stage
        passwordResetPage
            .waitForElementPresent("@KBAStep", 2000)
            .assert.cssClassNotPresent("@KBAStep", "disabled")
            .assert.visible("@KBAStepToggle")
            .assert.elementNotPresent("@KBAStepEdit");

        // Password Reset Form
        passwordResetPage
            .waitForElementPresent("@passwordResetFormStep", 2000)
            .assert.cssClassNotPresent("@passwordResetFormStep", "disabled")
            .assert.elementNotPresent("@passwordResetFormStepToggle")
            .assert.visible("@passwordResetFormStepEdit");

        passwordResetPage
            .click("@userQueryFormStepEdit")
            .waitForElementPresent("@stepEditDialog", 2000)
            .click("@idInput");

        client.elements("css selector", "#select-identityIdField + div .selectize-dropdown-content div", function(result) {
            client.assert.equal(result.value.length, numUserProperties);
        });

        passwordResetPage
            .assert.value("@idSelect", "_id")
            .click("@dialogClose")
            .waitForElementPresent("@resourceDropdown", 2000)
            .setValue("@resourceDropdown", "managed/role")
            .waitForElementPresent("@userQueryFormStepEdit", 2000)
            .click("@userQueryFormStepEdit")
            .click("@userQueryFormStepEdit")
            .waitForElementPresent("@stepEditDialog", 2000)
            .click("@idInput");

        client.elements("css selector", "#select-identityIdField + div .selectize-dropdown-content div", function(result) {
            client.assert.equal(result.value.length, numRoleProperties);
        });

        passwordResetPage
            .assert.value("@idSelect", "_id")
            .loadPage()
            .assert.value("@resourceDropdown", "managed/role");

    }
};
