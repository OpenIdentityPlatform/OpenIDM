module.exports = {
    before: function (client, done) {
        client.globals.login.helpers.setSession(client, done);
    },
    after: function (client) {
        client.end();
    },
    'Navigation items reflect service state': function (client) {
        // OPENIDM-4512
        var userRegistrationPage = client.page.userRegistration(),
            navPage = client.page.navigation(),
            registraionNavSection = navPage.section.userregistration;

        userRegistrationPage.loadPage();

        // initially the nav item should be disabled
        registraionNavSection.expect.element('@toggleOff').to.be.present;

        // enable self-registration
        userRegistrationPage.toggleSlider();

        // now the nav item should be enabled
        registraionNavSection.expect.element('@toggleOn').to.be.present;

        // disable self-registration
        userRegistrationPage.toggleSlider();

        // now the nav item should be disabled again
        registraionNavSection.expect.element('@toggleOff').to.be.present;
    }
};
