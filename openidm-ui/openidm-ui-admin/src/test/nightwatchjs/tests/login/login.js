module.exports = {
        'Login': function (client) {
            client.globals.login.helpers.login(client);
        },

        'Logout': function (client) {
            client
                .waitForElementVisible('#loginContent', 2000)
                .click("#loginContent")
                .waitForElementVisible('#logout_link', 2000)
                .click('#logout_link')
                .waitForElementPresent('input[name=loginButton]', 2000)
                .assert.urlContains("#login/")
                .waitForElementVisible('div[role=alert]', 1000)
                .expect.element('div[role=alert]').text.to.equal("You have been logged out.");
            
            //wait for the message to disappear
            client.waitForElementNotPresent('div[role=alert]', 5500);
        },
        
        'Invalid Login': function (client) {
            var data = client.globals;
            
            client
                .waitForElementPresent('input[name=loginButton]', 2000)
                .setValue('input[name=login]', "bogusUsername")
                .setValue('input[name=password]', "bogusPassword")
                .click('input[name=loginButton]')
                .waitForElementVisible('div[role=alert]', 1000)
                .expect.element('div[role=alert]').text.to.equal("Login/password combination is invalid.");
            
            client.end();
        }
};