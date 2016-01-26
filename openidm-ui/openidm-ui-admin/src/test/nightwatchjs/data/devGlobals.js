module.exports = {
        login: {
            username: "openidm-admin",
            password: "openidm-admin",
            url: "http:localhost:8080/admin/index.html#login/",
            helpers: {
                login: function (client) {
                    var data = client.globals;
                    
                    client
                        .url(data.login.url)
                        .waitForElementPresent('input[name=loginButton]', 2000)
                        .setValue('input[name=login]', data.login.username)
                        .setValue('input[name=password]', data.login.password)
                        .click('input[name=loginButton]')
                        .waitForElementPresent('#logout_link', 2000)
                        .assert.urlContains("#dashboard/");
                }
            }
        }
};
