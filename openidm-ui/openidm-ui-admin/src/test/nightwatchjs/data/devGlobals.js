module.exports = {
        baseUrl: "http://localhost:8080/admin/",
        login: {
            username: "openidm-admin",
            password: "openidm-admin",
            url: "#login/",
            helpers: {
                login: function (client) {
                    var data = client.globals;

                    client
                        .url(data.baseUrl + data.login.url)
                        .waitForElementPresent('input[name=loginButton]', 2000)
                        .setValue('input[name=login]', data.login.username)
                        .setValue('input[name=password]', data.login.password)
                        .click('input[name=loginButton]')
                        .waitForElementPresent('#logout_link', 2000)
                        .assert.urlContains("#dashboard/");
                },
                setSession: function (client, callback) {
                    var data = client.globals;
                    client.url(data.baseUrl)
                        .pause(200)
                        .timeoutsAsyncScript(2000)
                        .executeAsync(
                            function (args, done) {
                                var eventManager = require("org/forgerock/commons/ui/common/main/EventManager"),
                                    constants = require("org/forgerock/commons/ui/common/util/Constants");
                                
                                eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, {userName: "openidm-admin", password: "openidm-admin"}).then(function () {
                                    done();
                                });
                            },
                            ['mustBeAValueHere'],//bug with nightwatch--must be a value in at least one arg passed to execute func (https://github.com/nightwatchjs/nightwatch/issues/616)
                            function (result) {
                                client.pause(100);
                                
                                callback();
                            }
                        );
                }
            }
        }
};
