define([
    "org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationAbstractView"
], function (AuthenticationAbstractView) {
    QUnit.module("AuthenticationAbstractView Tests");

    QUnit.test("Derives custom properties from all properties", function(assert) {
        var view = new AuthenticationAbstractView,
            known = ["prop1", "prop2", "prop3"],
            noCustomPropertiesCompleteSet = {
                "prop1": "prop1",
                "prop2": "prop2",
                "prop3": "prop3"
            },
            noCustomPropertiesPartialSet = {
                "prop2": "prop2",
                "prop3": "prop3"
            },
            someCustomPropertiesPartialSet = {
                "prop2": "prop2",
                "prop3": "prop3",
                "custom1": "custom1",
                "custom2": "custom2"
            },
            someCustomPropertiesCompleteSet = {
                "prop1": "prop1",
                "prop2": "prop2",
                "prop3": "prop3",
                "custom1": "custom1",
                "custom2": "custom2"
            },
            onlyCustomProperties = {
                "custom1": "custom1",
                "custom2": "custom2",
                "custom3": "custom3"
            };

        assert.equal(view.getCustomPropertiesList(known, {}).length, "0", "No custom properties when there are no config properties" );
        assert.equal(view.getCustomPropertiesList(known, noCustomPropertiesCompleteSet).length, "0", "No custom properties when the config properties are all of the knownProperties");
        assert.equal(view.getCustomPropertiesList(known, noCustomPropertiesPartialSet).length, "0", "No custom properties when the config properties are a subset of the knownProperties");
        assert.equal(view.getCustomPropertiesList(known, someCustomPropertiesPartialSet).length, "2", "Custom properties detected when the config properties contains a subset of the knownProperties and some unknown properties");
        assert.equal(view.getCustomPropertiesList(known, someCustomPropertiesCompleteSet).length, "2", "Custom properties detected when the config properties contains all of the knownProperties and some unknown properties");
        assert.equal(view.getCustomPropertiesList(known, onlyCustomProperties).length, "3", "Custom properties detected when the config properties contains none of the knownProperties and some unknown properties");
    });

    QUnit.test("Takes a JSONEditor formatted value of custom properties and converts them to an IDM format", function(assert) {
        var view = new AuthenticationAbstractView,
            emptyProperties =[
                {
                    "propertyName": "",
                    "propertyValue": "abc"
                }
            ],
            config = [
                {
                    "propertyName": "test1",
                    "propertyValue": "abc"
                }, {
                    "propertyName": "test2",
                    "propertyValue": ["a", "b", "c"]
                }
            ],
            formatted = view.getCustomProperties(config);

        assert.deepEqual(view.getCustomProperties(emptyProperties), {}, "Given a custom properties configuration with an empty property name, an empty object is returned");
        assert.deepEqual(view.getCustomProperties([]), {}, "Given an empty custom properties configuration, an empty object is returned");
        assert.equal(
            (
                _.has(formatted, "test1") &&
                _.isEqual(formatted["test1"], "abc") &&
                _.has(formatted, "test2") &&
                _.isEqual(formatted["test2"], ["a", "b", "c"])
            ),
            true,
            "Given a populated custom properties configuration, an properly configured object is returned");
    });

    QUnit.test("Gets a data object for determining to use user roles or group membership", function(assert) {
        var view = new AuthenticationAbstractView,
            userRolesConfig = {
                "properties": {
                    "propertyMapping": {
                        "userRoles": "Test User Roles"
                    }
                }
            },
            groupMembershipConfig = {
                "properties": {
                    "propertyMapping": {
                        "groupMembership": "Test Group Membership"
                    },
                    "groupRoleMapping": {
                        "testMapping1": [1, 2, 3],
                        "testMapping2": ["a", "b", "c"]
                    }
                }
            };

        assert.equal(view.getUserOrGroupDefault(userRolesConfig).type, "userRoles", "Given a User Roles configuration, an object with type userRoles is returned");
        assert.equal(view.getUserOrGroupDefault(userRolesConfig).formattedGroupRoleMapping.length, "0", "Given a User Roles configuration, an array with formattedGroupRoleMapping containing no items is returned");
        assert.equal(view.getUserOrGroupDefault(groupMembershipConfig).type, "groupMembership", "Given a Group Membership configuration, an object with type groupMembership is returned");
        assert.equal(view.getUserOrGroupDefault(groupMembershipConfig).formattedGroupRoleMapping.length, "2", "Given a Group Membership configuration, an array containing the two formattedGroupRoleMappings is returned");
    });

    QUnit.test("Takes a JSONEditor formatted value of group membership properties and converts them to an IDM format", function(assert) {
        var view = new AuthenticationAbstractView,
            populatedValue = [
                {
                    "roleName": "testMapping1",
                    "groupMapping": [1, 2, 3]
                },
                {
                    "roleName": "testMapping2",
                    "groupMapping": ["a", "b", "c"]
                }
            ],
            formatted = view.formatGroupMembershipProperties(populatedValue);

        assert.deepEqual(view.formatGroupMembershipProperties([]), {}, "Given an empty group membership configuration, an empty object is returned");
        assert.equal(
            (
                _.has(formatted, "testMapping1") &&
                _.isEqual(formatted["testMapping1"], [1, 2, 3]) &&
                _.has(formatted, "testMapping2") &&
                _.isEqual(formatted["testMapping2"], ["a", "b", "c"])
            ),
            true,
            "Given a populated group membership configuration, an properly configured object is returned");

    });

});