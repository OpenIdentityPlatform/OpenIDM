define([
    "lodash",
    "org/forgerock/openidm/ui/admin/authentication/ProvidersModuleDialogView"
], function (_, ProvidersModuleDialogView) {
    QUnit.module('ProvidersModuleDialogView Tests');

    QUnit.test("Create a logout URL from wellknown URL", function (assert) {
        var newurl = ProvidersModuleDialogView.formatLogoutURL("https://openam.example.com/openam/oauth2/.well-known/openid-configuration");
        assert.equal(newurl, "https://openam.example.com/openam/UI/Logout", "Logout URL Created from valid well-known URL");

        newurl = ProvidersModuleDialogView.formatLogoutURL("openam/oauth2/testtest");
        assert.equal(newurl, "openam/UI/Logout", "Logout URL created from semi valid-well known URL");

        newurl = ProvidersModuleDialogView.formatLogoutURL("testtest");
        assert.equal(newurl, "testtest", "No string replacement is done in invalid well-known URLs");
    });

    QUnit.test("Update Authentication Modules data for AM use", function (assert) {
        var authData = ProvidersModuleDialogView.getAuthModulesConfig({
            "authModules": [
                {
                    "enabled": false,
                    "name": "STATIC_USER"
                },{
                    "enabled": false,
                    "name": "INTERNAL_USER"
                }, {
                    "enabled": true,
                    "name": "something_else"
                },
                {
                    "enabled": false,
                    "name": "OPENID_CONNECT",
                    "properties": {
                        "resolvers": [{
                            "name": "OPENAM",
                            "testSetting": 1
                        }]
                    }
                }
            ],
            "sessionModule": {
                "properties": {
                    "maxTokenLifeMinutes": "123456",
                    "tokenIdleTimeMinutes": "123456"
                }
            }
        }, {
            "enabled": true,
            "name": "OPENID_CONNECT",
            "properties": {
                "resolvers": [{
                    "name": "OPENAM",
                    "testSetting": 2
                }]
            }
        });

        assert.equal(_.get(authData, "sessionModule.properties.maxTokenLifeSeconds"), "5" , "maxTokenLifeSeconds set");
        assert.equal(_.get(authData, "sessionModule.properties.tokenIdleTimeSeconds"), "5" , "tokenIdleTimeSeconds set");
        assert.equal(_.get(authData, "sessionModule.properties.maxTokenLifeMinutes"), undefined , "maxTokenLifeMinutes unset");
        assert.equal(_.get(authData, "sessionModule.properties.tokenIdleTimeMinutes"), undefined , "tokenIdleTimeMinutes unset");
        assert.equal(_.get(authData, "authModules[0].enabled"), true , "STATIC_USER auth module enabled");
        assert.equal(_.get(authData, "authModules[1].enabled"), true , "INTERNAL_USER auth module enabled");
        assert.equal(_.get(authData, "authModules[2].enabled"), false , "Other auth module disabled");
        assert.equal(_.get(authData, "authModules[3].enabled"), true , "OPENAM auth module enabled");
        assert.equal(_.get(authData, "authModules[3].properties.resolvers[0].testSetting"), 2 , "OPENAM auth module settings overwritten");
    });
});