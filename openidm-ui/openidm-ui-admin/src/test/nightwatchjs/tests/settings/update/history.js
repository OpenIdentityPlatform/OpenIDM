var resp,
    updatePage;

module.exports = {
    before: function(client, done) {
        client.globals.login.helpers.setSession(client, function() {
            updatePage = client.page.update();

            client
                .timeoutsAsyncScript(2000)
                .executeAsync(function(response, fini) {
                    require(
                        ['org/forgerock/openidm/ui/admin/settings/UpdateView', 'sinon'],
                        function(updateView, sinon) {

                            var result = response.result[0],
                            xhr = sinon.useFakeXMLHttpRequest(),
                            server = sinon.fakeServer.create();

                            server.autoRespond = true;
                            xhr.useFilters = true;

                            xhr.addFilter(function(method, url) {
                                if (url.match('/openidm/maintenance/update/log/')) {
                                    return false;
                                } else {
                                    return true;
                                }
                            });

                            server.respondWith(/queryFilter=true/, JSON.stringify(response));
                            server.respondWith(/TestID/, JSON.stringify(result));

                            window.xhr = xhr;
                        }
                    );
                }, [resp], function() {
                    updatePage
                        .navigate()
                        .waitForElementVisible('@versionsView', 2000);
                        done();
                });
        });

    },

    after: function(client) {
        client.end();
    },

    "It should display history when history available": function(client) {
        updatePage
            .waitForElementVisible('@historyView', 2000);
        updatePage
            .click('@historyViewToggle')
            .waitForElementVisible('@reportButton', 250)
            .assert.containsText('@historyGridUserName', 'Updated by: openidm-admin');

    },

    "It should link out to report": function(client) {
        updatePage
            .click('@reportButton');
        updatePage
            .waitForElementVisible('@reportView', 2000);
        updatePage
            .expect.element('@successAlert').to.not.be.present;
        updatePage
            .expect.element('@panelDescription').to.be.visible;
        updatePage
            .assert.containsText('@panelDescription', 'Updated by: openidm-admin');
    },

    "Report should have the proper amount of files": function(client) {
        var treeGridNode = updatePage.elements.treeGridNode.selector;
        client.elements('css selector', treeGridNode, function(results) {
            client.assert.equal(results.value.length, 2, 'correct number of tree nodes rendering');
        });
    },

    "Report should link back to version/history page": function(client) {
        client
            .execute(function() {
                window.xhr.restore();
            });
        updatePage
            .click('@reportBackButton')
            .waitForElementVisible('@versionsView', 2000)
            .expect.element('@historyView').to.not.be.visible;
    }
};

resp = {
    "resultCount": 1,
    "pagedResultsCookie": null,
    "totalPagedResultsPolicy": "NONE",
    "totalPagedResults": -1,
    "remainingPagedResults": -1,
    "result": [
        {
            "_id": "TestID",
            "_rev": "5699",
            "archive": "openidm.zip",
            "status": "COMPLETE",
            "completedTasks": 2,
            "totalTasks": 2,
            "startDate": "2016-04-14T03:31:21.16-07:00",
            "endDate": "2016-04-14T03:31:21.16-07:00",
            "userName": "openidm-admin",
            "statusMessage": "Update complete.",
            "nodeId": "Javiers-MacBook-Pro-2.local",
            "files": [
                {
                  "filePath": "ui/selfservice/default/templates/user/process/registration/captcha-initial.html",
                  "fileState": "UNCHANGED",
                  "actionTaken": "REPLACED"
                },
                {
                  "filePath": "samples/trustedservletfilter/conf/logging.properties",
                  "fileState": "UNCHANGED",
                  "actionTaken": "REPLACED"
                }
            ]
        }
    ]
};
