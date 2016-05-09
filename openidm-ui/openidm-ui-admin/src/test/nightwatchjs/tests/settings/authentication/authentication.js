module.exports = {
        'Add OPENAM_SESSION module': function (client) {
            //must login first at the beginning of a session
            client.globals.login.helpers.login(client);
            
            client
                .waitForElementPresent('a[title=Configure]', 2000)
                .click('a[title=Configure]')
                .waitForElementVisible('a[title^="System Preferences"]', 2000)
                .click('a[title^="System Preferences"]')
                .waitForElementPresent('a[href^="#settings/"]', 2000)
                .click('a[href^="#settings/"]')
                .waitForElementPresent('#moduleType', 2000)
                .click('.selectize-input input[placeholder="Select a Module"]')
                .waitForElementPresent('div[data-value=OPENAM_SESSION]', 2000)
                .waitForElementVisible('div[data-value=OPENAM_SESSION]', 2000)
                .click('div[data-value=OPENAM_SESSION]')
                .assert.value('#moduleType','OPENAM_SESSION')
                .click('button.add-auth-module')
                .waitForElementPresent('select[name="root[enabled]"]', 2000)
                .setValue('select[name="root[enabled]"]', 'true')
                .assert.value('select[name="root[enabled]"]', '1')
                .click('.advancedShowHide')
                .setValue('input[name="root[openamDeploymentUrl]"]', 'http://oam.com/openam')
                .setValue('input[name="root[openamLoginUrl]"]', 'http://oam.com/openam/XUI/#login/')
                .setValue('input[name="root[openamLoginLinkText]"]', 'Login to OpenAM')
                .click('#submitAuditEventHandlers')
                .pause(1000)
                .assert.containsText('#authModuleGrid tr:last-of-type td:first-of-type', 'OpenAM Session')
                .waitForElementVisible('.authentication-module-changes', 2000)
                .click('#submitAuth');

        },
        'Delete OPENAM_SESSION module': function (client) {
            client
                .waitForElementPresent('#authModuleGrid', 2000)
                .waitForElementVisible('#authModuleGrid tr:last-of-type td:first-of-type', 2000)
                .pause(1000)
                .assert.containsText('#authModuleGrid tr:last-of-type td:first-of-type', 'OpenAM Session');
            
            client
                .click('#authModuleGrid tr:last-of-type td:nth-child(2) .fa-times')
                .waitForElementPresent('#submitAuth', 2000)
                .click('#submitAuth')
                .pause(2000)
                .waitForElementVisible('#authModuleGrid tr:last-of-type td:first-of-type', 2000)
                .assert.containsText('#authModuleGrid tr:last-of-type td:first-of-type', 'Client Cert');
            
            client.end();
        }
};