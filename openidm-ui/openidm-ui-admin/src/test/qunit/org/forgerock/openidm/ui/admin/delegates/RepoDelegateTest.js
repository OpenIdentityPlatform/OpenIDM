define([
    "lodash",
    "sinon",
    "org/forgerock/openidm/ui/admin/delegates/RepoDelegate"
], function (_, sinon, RepoDelegate) {
    var jdbcRepoConfig = {
            "_id": "repo.jdbc",
            "resourceMapping" : {
                "genericMapping" : {
                    "managed/user" : {
                        "mainTable" : "managedobjects",
                        "propertiesTable" : "managedobjectproperties",
                        "searchableDefault" : false,
                        "properties" : {
                            "/userName" : {
                                "searchable" : true
                            },
                            "/givenName" : {
                                "searchable" : true
                            }
                        }
                    }
                }
            }
        },
        orientRepoConfig = {
            "_id": "repo.orientdb",
            "dbStructure" : {
                "orientdbClass" : {
                    "managed_user" : { },
                    "managed_role" : { }
                }
            }
        };

    QUnit.module('RepoDelegate Tests');
    QUnit.test("getRepoTypeFromConfig", function (assert) {
        assert.equal(RepoDelegate.getRepoTypeFromConfig({"_id": "repo.jdbc"}), "jdbc", "jdbc");
        assert.equal(RepoDelegate.getRepoTypeFromConfig({"_id": "repo.orientdb"}), "orientdb", "orientdb");
    });

    QUnit.test("deleteManagedObject", function (assert) {
        sinon.stub(RepoDelegate, "updateEntity", function (id, updatedConfig) {
            assert.ok(updatedConfig.dbStructure.orientdbClass.managed_user === undefined, "managed/user properly deleted from orient");
            RepoDelegate.updateEntity.restore();
        });

        RepoDelegate.deleteManagedObject(_.cloneDeep(orientRepoConfig), "user");
    });

    QUnit.test("addManagedObjectToOrientClasses", function (assert) {
        var updatedConfig = RepoDelegate.addManagedObjectToOrientClasses(_.cloneDeep(orientRepoConfig), "foo");
        assert.ok(updatedConfig.dbStructure.orientdbClass.managed_foo !== undefined, "managed_foo added to orientdb");
    });

    QUnit.test("findGenericResourceMappingForRoute", function (assert){
        assert.ok(RepoDelegate.findGenericResourceMappingForRoute(jdbcRepoConfig, "generic/user") === undefined, "no resource found for generic/user");
        assert.ok(RepoDelegate.findGenericResourceMappingForRoute(jdbcRepoConfig, "managed/foo") === undefined, "no resource found for managed/foo");
        assert.ok(RepoDelegate.findGenericResourceMappingForRoute(jdbcRepoConfig, "managed/user") !== undefined, "resource found for managed/user");
    });

    QUnit.test("syncSearchablePropertiesForGenericResource", function (assert) {
        var updatedConfig = RepoDelegate.syncSearchablePropertiesForGenericResource(
                RepoDelegate.findGenericResourceMappingForRoute(_.cloneDeep(jdbcRepoConfig), "managed/user"),
                ["userName","mail"]
            );

        assert.equal(updatedConfig.properties["/userName"].searchable,true, "userName still searchable");
        assert.equal(updatedConfig.properties["/mail"].searchable,true, "mail now searchable");
        assert.equal(updatedConfig.properties["/givenName"],undefined, "givenName no longer specified");

        updatedConfig = RepoDelegate.syncSearchablePropertiesForGenericResource(
            RepoDelegate.findGenericResourceMappingForRoute({
                "_id": "repo.jdbc",
                "resourceMapping" : {
                    "genericMapping" : {
                        "managed/user" : {
                            "mainTable" : "managedobjects",
                            "propertiesTable" : "managedobjectproperties",
                            "searchableDefault" : true
                        }
                    }
                }
            }, "managed/user"),
            ["userName","mail"]
        );
        assert.equal(updatedConfig.properties,undefined, "no properties called out when searchableDefault is true");

    });
});
