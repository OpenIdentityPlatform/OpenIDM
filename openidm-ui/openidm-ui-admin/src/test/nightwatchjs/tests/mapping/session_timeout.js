module.exports = {

    before : function(client, done) {
        //must create a session before tests can begin
        client.globals.login.helpers.setSession(client, function () {
            //read all configs that need to have the originals cached
            client.globals.config.update("sync", {
                "mappings" : [ {
                    "name" : "testMapping",
                    "source" : "managed/user",
                    "target" : "managed/user",
                    "properties" : [ { "source" : "_id", "target" : "_id" } ]
                }]
            }, done, true);
        });
    },
    after : function(client, done) {
        client.globals.config.resetAll(function () {
            client.end();
            done()
        });
    },
    'Save correctly after reauth': function (client) {
        var mappingPage = client.page.mappingProperties(),
            loginDialog = client.page.loginDialog();

        mappingPage
            .loadPropertyMappingUrl("testMapping");

        // logout
        client.execute(function () {
            require("org/forgerock/openidm/ui/common/login/InternalLoginHelper").logout($.noop);
        });

        // delete a property from the grid and attempt to save
        mappingPage
            .deleteFirstProperty()
            .click("@savePropertiesButton");

        // attempt to login using inline login dialog, using invalid credentials
        loginDialog
            .loginWithBadCredentials();

        // login again with the inline login dialog, with good credentials
        loginDialog
            .loginWithCredentials(client.globals.login.username, client.globals.login.password);

        // verify sync has been updated successfully without further interaction
        client
            .config.read("sync", function (config) {
                client.assert.equal(config.mappings[0].properties.length, 0,'Property was deleted');
            });
    }
};
