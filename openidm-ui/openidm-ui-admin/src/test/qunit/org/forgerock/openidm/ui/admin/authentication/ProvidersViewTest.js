define([
    "lodash",
    "org/forgerock/openidm/ui/admin/authentication/ProvidersView"
], function (_, ProvidersView) {
    QUnit.module('ProvidersView Tests');

    QUnit.test("Update Authentication Modules data for local use", function (assert) {

        var authData = ProvidersView.getLocalAuthConfig({
            "authModules": [
                {"enabled": false},
                {
                    "enabled": true,
                    "name": "OPENID_CONNECT",
                    "properties": {
                        "resolvers": [{
                            "name": "OPENAM"
                        }]
                    }
                }
            ],
            "sessionModule": {
                "properties": {
                    "maxTokenLifeSeconds": "123",
                    "tokenIdleTimeSeconds": "456"
                }
            }
        });

        assert.equal(_.get(authData, "sessionModule.properties.maxTokenLifeMinutes"), "120" , "maxTokenLifeMinutes set");
        assert.equal(_.get(authData, "sessionModule.properties.tokenIdleTimeMinutes"), "30" , "maxTokenLifeMinutes set");
        assert.equal(_.get(authData, "sessionModule.properties.maxTokenLifeSeconds"), undefined , "maxTokenLifeSeconds unset");
        assert.equal(_.get(authData, "sessionModule.properties.tokenIdleTimeSeconds"), undefined , "tokenIdleTimeSeconds unset");
        assert.equal(_.get(authData, "authModules[0].enabled"), true , "Basic auth module enabled");
        assert.equal(_.get(authData, "authModules[1].enabled"), false , "OPENAM auth module disabled");
        
    });
});