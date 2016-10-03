/*global $ */
define([
    "org/forgerock/openidm/ui/admin/social/SocialConfigView",
    "org/forgerock/openidm/ui/admin/util/AdminUtils"
], function (SocialConfigView,
             AdminUtils) {
    QUnit.module('SocialConfigView Tests');

    QUnit.test("Generate card details", function (assert) {
        var cardDetails = SocialConfigView.getCardDetails($("<div data-type='test' data-name='testName'></div>"));

        assert.equal(cardDetails.type, "test", "Type extracted from card correctly");
        assert.equal(cardDetails.name, "testName", "Name extracted from card correctly");
    });

    QUnit.test("Generate saved social provider details", function (assert) {
        var oldData = {
                "client_id" : "configure_me",
                "client_secret" : null,
                "scope" : [
                    "profile",
                    "email",
                    "openid"
                ]
            },
            newData = {
                "client_id" : "test",
                "client_secret" : "test",
                "scope" : [
                    "test"
                ]
            },
            saveDetails = SocialConfigView.generateSaveData(newData, oldData);

        assert.equal(saveDetails.client_id, "test", "Client Id changed correctly");
        assert.equal(saveDetails.client_secret, "test", "Client secret changed correctly");

        oldData = saveDetails;

        newData.client_secret = null;

        saveDetails = SocialConfigView.generateSaveData(newData, oldData);

        assert.equal(saveDetails.client_secret, "test", "Client secret remains the same with null return");
    });

    QUnit.test("Convert name to proper capitalization", function (assert) {
        assert.equal(AdminUtils.capitalizeName("google"), "Google", "Name correctly capitalized");
    });

    QUnit.test("Verify social provider help messages", function (assert) {
        var messageDisplay,
            fakeRegistration = {
                "stageConfigs" : [
                    {
                        "name" : "socialUserDetails"
                    }
                ]
            };

        messageDisplay = SocialConfigView.getMessageState(1, null, false);

        assert.equal(messageDisplay.registration, true, "No registration message displayed");
        assert.equal(messageDisplay.login, true, "No login message displayed");

        messageDisplay = SocialConfigView.getMessageState(1, fakeRegistration, true);

        assert.equal(messageDisplay.registration, false, "No registration message hidden");
        assert.equal(messageDisplay.login, false, "No login message hidden");
    });

    QUnit.test("addBindUnbindBehavior", function (assert) {
        var managedConfig = {
                "objects": [{
                    "name" : "user"
                }]
            },
            amendedConfig = SocialConfigView.addBindUnbindBehavior(managedConfig);
        assert.ok(amendedConfig.objects[0].actions.bind && amendedConfig.objects[0].actions.unbind,
            "bind and unbind actions added to user object");
    });


    QUnit.test("removeBindUnbindBehavior", function (assert) {
        var managedConfig = {
                "objects": [{
                    "name" : "user",
                    "actions" : {
                        "action1" : {
                            "type" : "text/javascript",
                            "source" : "console.log('hello');"
                        },
                        "bind" : {
                            "type" : "text/javascript",
                            "source": "console.log('bind')"
                        },
                        "unbind" : {
                            "type" : "text/javascript",
                            "source": "console.log('bind')"
                        }
                    }
                }]
            },
            amendedConfig = SocialConfigView.removeBindUnbindBehavior(managedConfig);

        assert.ok(
            typeof amendedConfig.objects[0].actions.bind === "undefined" &&
            typeof amendedConfig.objects[0].actions.unbind === "undefined" &&
            typeof amendedConfig.objects[0].actions.action1 === "object",
            "bind and unbind actions removed from user object");
    });

    QUnit.test("addIDPsProperty", function (assert) {
        var managedConfig = {
                "objects": [{
                    "name" : "user",
                    "schema" : {
                        "properties" : { },
                        "order": [ ]
                    }
                }]
            },
            amendedConfig =  SocialConfigView.addIDPsProperty(managedConfig);
        assert.ok(amendedConfig.objects[0].schema.properties.idps.items.resourceCollection, "idps object defined");
        assert.equal(amendedConfig.objects[0].schema.order.filter(function (o) {return o === "idps";}).length, 1,
            "idps present in order only once");

        amendedConfig =  SocialConfigView.addIDPsProperty(amendedConfig);
        assert.equal(amendedConfig.objects[0].schema.order.filter(function (o) {return o === "idps";}).length, 1,
            "idps present in order only once, even after calling function again");

    });

    QUnit.test("removeIDPsProperty", function (assert) {
        var managedConfig = {
                "objects": [{
                    "name" : "user",
                    "schema" : {
                        "properties" : {
                            "idps" : {
                                "items" : {
                                    "resourceCollection" : []
                                }
                            }
                        },
                        "order": [ "idps" ]
                    }
                }]
            },
            amendedConfig = SocialConfigView.removeIDPsProperty(managedConfig);
        assert.equal(typeof amendedConfig.objects[0].schema.properties.idps, "undefined", "idps removed from schema properties");
        assert.equal(amendedConfig.objects[0].schema.order.filter(function (o) {return o === "idps";}).length, 0,
            "idps not present in order");
    });

    QUnit.test("addManagedObjectForIDP", function (assert) {
        var managedConfig = {
                "objects": [{
                    "name" : "user",
                    "schema" : {
                        "properties" : {
                            "idps" : {
                                "items" : {
                                    "resourceCollection" : []
                                }
                            }
                        },
                        "order": [ "idps" ]
                    }
                }]
            },
            basicProviderConfig = {
                "name" : "testProvider"
            },
            advancedProviderConfig = {
                "name" : "testProvider",
                "schema": {
                    "viewable" : true,
                    "type": "object",
                    "properties" : {
                        "name" : {
                            "type" : "string"
                        }
                    },
                    "order" : [ "name" ]
                }
            },
            amendedConfig = SocialConfigView.addManagedObjectForIDP(basicProviderConfig, managedConfig);

        assert.equal(amendedConfig.objects[1].name, "testProvider", "new managed object defined with name of provider");
        assert.ok(amendedConfig.objects[1].schema.properties.user &&
                amendedConfig.objects[1].schema.type === "object" &&
                amendedConfig.objects[1].schema.viewable === true &&
                amendedConfig.objects[1].schema.properties._id,
            "provider definition with no schema results in default managed object schema with default properties");

        assert.deepEqual(amendedConfig.objects[1].schema.order, ["_id", "user"], "default order generated for managed object without schema");

        assert.equal(amendedConfig.objects[0].schema.properties.idps.items.resourceCollection.filter(function (rc) {
            return rc.path === "managed/testProvider";
        }).length, 1, "Found proper resourceCollection added to managed/user object schema");

        amendedConfig = SocialConfigView.addManagedObjectForIDP(advancedProviderConfig, managedConfig);

        assert.equal(amendedConfig.objects[1].schema.properties.name.type, "string",
            "provider definition with schema results in managed object schema with provider schema properties");
        assert.equal(typeof amendedConfig.objects[1].schema.properties._id, "undefined", "default _id property not added when schema present");
        assert.deepEqual(amendedConfig.objects[1].schema.order, ["name", "user"], "order generated for managed object with schema");

        amendedConfig = SocialConfigView.addManagedObjectForIDP(advancedProviderConfig, amendedConfig);

        assert.equal(amendedConfig.objects.filter(function (o) {return o.name === "testProvider";}).length, 1,
            "Only one object type for provider defined after repeated calls");

    });


    QUnit.test("removeManagedObjectForIDP", function (assert) {
        var managedConfig = {
                "objects" : [{
                    "name" : "user",
                    "schema" : {
                        "properties" : {
                            "idps" : {
                                "items": {
                                    "resourceCollection" : [{
                                        "path" : "managed/testProvider"
                                    }]
                                }
                            }
                        },
                        "order" : [ "idps" ]
                    }
                },{
                    "name" : "testProvider"
                }]
            },
            providerConfig = {
                "name" : "testProvider"
            },
            amendedConfig = SocialConfigView.removeManagedObjectForIDP(providerConfig, managedConfig);

        assert.ok(amendedConfig.objects.length === 1 && amendedConfig.objects[0].name === "user", "provider-related object removed from managed config");
        assert.equal(amendedConfig.objects[0].schema.properties.idps.items.resourceCollection.length, 0, "provider-related object removed from resourceCollection");
    });
});
