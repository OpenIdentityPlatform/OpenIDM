module.exports = {
    before: function (client, done) {
        client.globals.login.helpers.setSession(client, done);
    },
    after: function (client) {
        client.end();
    },

    "Translation add button only enables when form is filled out": function (client) {
        var userRegistrationPage = client.page.userRegistration();

        userRegistrationPage
            .loadPage()
            .toggleSlider()
            .click("@emailValidationCard")
            .waitForElementPresent("@subjectAddBtn", 2000)
            .expect.element("@subjectAddBtn").to.not.be.enabled;

        client.execute(function() {
            return $(".translationMapGroup[field='subjectTranslations'] li").length;
        }, ["test"], function(startCount) {
            userRegistrationPage
                .setValue("@subjectLocale", "testLocale")
                .expect.element("@subjectAddBtn").to.not.be.enabled;

            userRegistrationPage
                .setValue("@subjectTranslation", "testTranslation")
                .expect.element("@subjectAddBtn").to.be.enabled;

            userRegistrationPage
                .clearValue("@subjectLocale")
                .setValue("@subjectLocale", "\u0008")
                .expect.element("@subjectAddBtn").to.not.be.enabled;

            userRegistrationPage
                .setValue("@subjectLocale", "testLocale")
                .expect.element("@subjectAddBtn").to.be.enabled;

            userRegistrationPage.click("@subjectAddBtn");

            client.execute(function() {
                return $(".translationMapGroup[field='subjectTranslations'] li").length;
            }, ["test"], function(endCount) {
                client.assert.equal(startCount.value + 1, endCount.value, "A translation was added.");
            });
        });

    }
};


