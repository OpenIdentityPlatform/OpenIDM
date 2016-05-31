var serverSettings,
    mockMapping;

module.exports = {
    before: function(client, done) {

        client.globals.login.helpers.setSession(client, function () {
            client.globals.config.update('sync', mockMapping, done, true);
        });


    },
    after: function(client, done) {
        client.globals.config.resetAll(function(){
            client.end();
            done();
        });
    },
    "It should navigate to mapping": function(client) {
        var mapping = client.page.mapping(),
            mappingProperties = client.page.mappingProperties();

        mapping
            .navigate();

        mappingProperties
            .waitForElementVisible("@syncStatus", 5000)
            .loadPropertyMappingUrl("managedAssignment_managedRole")
            .waitForElementPresent("@pageType", 2000)
            .assert.containsText("@pageType", "MAPPING DETAIL")
            .assert.containsText("@pageTitle", "managedAssignment_managedRole");
    },

    "It should navigate to mapping after cancelled recon": function(client) {
        var mapping = client.page.mapping(),
            mappingProperties = client.page.mappingProperties();

        mappingProperties
            .loadPropertyMappingUrl("managedAssignment_managedRole");

        client
            .timeoutsAsyncScript(2000)
            .executeAsync(function(settings, done) {
                    require(
                    ["sinon",
                    "org/forgerock/openidm/ui/admin/delegates/ReconDelegate"],
                    function(sinon, reconDel) {

                        // stub reconDel call
                        sinon.stub(reconDel, 'triggerRecon', function (mapping, suppressSpinner, progressCallback) {
                            return reconDel.serviceCall({
                                "suppressSpinner": suppressSpinner,
                                "url": "?_action=recon&mapping=" + mapping,
                                "type": "POST"
                            })
                            .then(function (reconId) {
                                return reconDel.serviceCall({
                                    "suppressSpinner": suppressSpinner,
                                    "serviceUrl": "/openidm/recon/" + reconId._id,
                                    "url": "?_action=cancel",
                                    "type": "POST"
                                });
                            })
                            .then(function(reconId) {
                                return reconDel.waitForAll([reconId._id], suppressSpinner, progressCallback)
                                .then(function (reconArray) {
                                    return reconArray[0];
                                });
                            });
                        });

                        // create fake server to handle all requests from here out
                        var server = window.server = sinon.fakeServer.create();

                        window._.map(settings, function(setting) {
                            server.respondWith(setting.method, setting.url, JSON.stringify(setting.resp));
                        });

                        server.autoRespond = true;

                        done();
                });
            }, [serverSettings]);

        mappingProperties
            .click("@syncNowButton");

        client
            .pause(250);

        mappingProperties
            .assert.containsText("@syncMessage", 'reconciliation aborted');

        client
            .refresh();


        mappingProperties
            .waitForElementPresent("@pageType", 2000)
            .assert.containsText("@pageType", "MAPPING DETAIL")
            .assert.containsText("@pageTitle", "managedAssignment_managedRole");
    }
};

mockMapping = {
    "_id": "sync",
    "mappings": [
        {
          "target": "managed/role",
          "source": "managed/assignment",
          "name": "managedAssignment_managedRole",
          "properties": [
              {
                  "source": "email",
                  "target": "mail"
              }
          ],
          "policies": [
                {
                  "action": "ASYNC",
                  "situation": "ABSENT"
                }
            ]
        }
    ]
};

serverSettings = {
    reconCall: {
        method: "POST",
        url: "/openidm/recon?_action=recon&mapping=managedAssignment_managedRole",
        resp: {_id: "recon_1", state: "ACTIVE"}
    },
    cancelReconCall: {
        method: "POST",
        url: "/openidm/recon/recon_1?_action=cancel",
        resp: {"_id": "recon_1", "action": "cancel","status": "SUCCESS"}
    },
    getRecon: {
        method: "GET",
        url: "/openidm/recon/recon_1",
        resp: {
            "_id": "recon_1",
            "mapping": "managedAssignment_managedRole",
            "state": "CANCELED",
            "stage": "COMPLETED_CANCELED",
            "stageDescription": "reconciliation aborted.",
            "progress": {
                "source": {
                    "existing": {
                        "processed": 0,
                        "total": "?"
                    }
                },
                "target": {
                    "existing": {
                        "processed": 0,
                        "total": "?"
                    },
                    "created": 0
                },
                "links": {
                    "existing": {
                        "processed": 0,
                        "total": "?"
                    },
                    "created": 0
                }
            },
            "situationSummary": {
                "SOURCE_IGNORED": 0,
                "UNASSIGNED": 0,
                "AMBIGUOUS": 0,
                "CONFIRMED": 0,
                "FOUND_ALREADY_LINKED": 0,
                "UNQUALIFIED": 0,
                "ABSENT": 0,
                "TARGET_IGNORED": 0,
                "SOURCE_MISSING": 0,
                "MISSING": 0,"FOUND": 0
            },
            "statusSummary": {
                "SUCCESS": 0,
                "FAILURE": 0
            },
            "parameters": {
                "sourceQuery": {
                    "resourceName": "managed/assignment",
                    "queryId": "query-all-ids"
                },
                "targetQuery": {
                    "resourceName": "managed/role",
                    "queryId": "query-all-ids"
                }
            },
            "started": "2016-03-07T23:43:15.552Z",
            "ended": "2016-03-07T23:43:15.584Z",
            "duration": 32
        }
    },
    assignment: {
        method: "GET",
        url: "/openidm/managed/assignment?_queryFilter=/mapping%20eq%20%27managedAssignment_managedRole%27",
        resp: {"result": [],"resultCount": 0,"pagedResultsCookie": null,"totalPagedResultsPolicy": "NONE","totalPagedResults": -1,"remainingPagedResults": -1}
    }
};
