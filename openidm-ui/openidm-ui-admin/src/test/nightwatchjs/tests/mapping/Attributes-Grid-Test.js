var resetMapping,
    singleMapping,
    sampleData;

module.exports = {
    before: function(client, done) {
        client.globals.login.helpers.login(client);
        client.config.update("sync", singleMapping, done);
    },

    after: function(client, done) {
        client.config.update("sync", resetMapping, function() {
            client.end();
            done();
        });
    },

    "Grid should start with 1 row (given single mapping)": function(client) {
        var mappingPage = client.page.mapping(),
            attributesGridRows = mappingPage.elements.attributesGridRows.selector,
            mappingName = singleMapping.mappings[0].name;

            mappingPage.elements.mappingCard.selector = 'div[mapping="' + mappingName + '"]';

        mappingPage
            .navigate()
            .assert.urlContains('@href')
            .waitForElementPresent('@mappingCard', 1000)
            .click('@mappingCard')
            .waitForElementPresent('@attributesGrid', 1000);
        client
            .elements('css selector', attributesGridRows, function(result) {
                client.assert.ok(result.value.length === 1, 'Attributes grid starts with 1 item.');
            });
    },

    "Grid should change default target values": function(client) {
        var mappingPage = client.page.mapping();

        mappingPage
            .waitForElementPresent('@attributesGridFirstRow', 1000)
            .click('@attributesGridFirstRow')
            .waitForElementVisible('@scriptDialogUpdate', 2000)
            .click('@defaultValuesTab');
        client
            .keys('hello');
        mappingPage
            .click('@scriptDialogUpdate')
            .waitForElementVisible('@firstRowTargetText', 1000)
            .assert.containsText('@attributesGridFirstRow', '(hello)');
    },

    'Grid should modify properties on click': function(client) {
        var mappingPage = client.page.mapping();

        mappingPage
            .assert.elementNotPresent('@firstRowIconBadge');
        client
            .pause(250);
        mappingPage
            .click('@attributesGridFirstRow')
            .waitForElementVisible('@scriptDialogUpdate', 1000)
            .click('@transformationScriptTab')
            .waitForElementVisible('@transformationScriptTextArea', 1000)
            .click('@transformationScriptTextArea');
        client
            .keys('"test"');
        mappingPage
            .click('@scriptDialogUpdate')
            .assert.elementPresent('@firstRowIconBadge');
    },

    'Condition Filter shows correct values after property is selected (OPENIDM-5404)': function(client) {
        var mappingPage = client.page.mapping();

        client
            .pause(1000);
        mappingPage
            .waitForElementPresent('@attributesGridFirstRow', 1000)
            .click('@attributesGridFirstRow')
            .waitForElementVisible('@scriptDialogUpdate', 1000)
            .click('@conditionScriptTab')
            .waitForElementVisible('@conditionFilter', 1000)
            .click('@conditionFilter')
            .waitForElementVisible('@conditionFilterSelect', 1000)
            .click('@conditionFilterSelect')
            .waitForElementVisible('@conditionFilterSelectOption', 1000)
            .click('@conditionFilterSelectOption')
            .waitForElementVisible('@conditionFilterValueForProperty', 1000)
            .click('@conditionFilterValueForProperty')
            .waitForElementVisible('@conditionFilterValueForPropertyOption', 1000)
            .click('@conditionFilterValueForPropertyOption')
            .waitForElementNotPresent('@conditionFilterValueForPropertyErroneous', 1000, "There are no erroneously created select elements");
    },

    'Condition Filter add button should add a comparison': function(client) {
        var mappingPage = client.page.mapping(),
            conditionFilterTab = mappingPage.section.conditionFilterTab;

        client
            .pause(1000);
        mappingPage
            .waitForElementPresent('@attributesGridFirstRow', 1000)
            .click('@attributesGridFirstRow')
            .waitForElementVisible('@scriptDialogUpdate', 1000)
            .click('@conditionScriptTab')
            .waitForElementVisible('@conditionFilter', 1000)
            .click('@conditionFilter')
            .waitForElementVisible('@conditionFilterSelect', 1000)
            .click('@conditionFilterSelect')
            .waitForElementVisible('@conditionFilterSelectOption', 1000)
            .click('@conditionFilterSelectOption');

        conditionFilterTab
            .click('@addButton')
            .waitForElementPresent('@comparison', 1000);
    },

    'Condition Filter should display the correct description': function(client) {
        var mappingPage = client.page.mapping(),
            conditionFilterTab = mappingPage.section.conditionFilterTab;

            mappingPage
                .assert.containsText('@conditionScriptEditor','Define a declarative Condition Filter or a Script to update a property.');
    },

    'Condition Filter remove button should remove a comparison': function(client) {
        var mappingPage = client.page.mapping(),
            conditionFilterTab = mappingPage.section.conditionFilterTab;

            conditionFilterTab
                .click('@removeButton')
                .waitForElementNotPresent('@comparison', 1000);

            mappingPage
                .click('@scriptDialogCancel');
    },

    "Grid should render sample data into row": function(client) {
        var mappingPage = client.page.mapping(),
            previewPopUpValue = mappingPage.elements.previewPopUpValue.selector;

        client
            .refresh();
        mappingPage
            .waitForElementVisible('@attributesGridFirstRow', 2000);
        client
            .execute(function(data) {
                require(["http://sinonjs.org/releases/sinon-1.12.2.js"], function (sinon) {
                    var server = window.server = sinon.fakeServer.create();
                    server.autoRespond = true;
                    server.respondWith(JSON.stringify(data));
                });
            return true;
            }, [sampleData]);
        mappingPage
            .waitForElementPresent('@firstRowSourceText', 5000)
            .getText('@firstRowSourceText', function(result) {
                client.assert.equal(result.value, '', 'source text starts empty');
            })
            .getText('@firstRowTargetText', function(result) {
                client.assert.equal(result.value, '', 'target text starts empty');
            })
            .waitForElementPresent('@gridPreviewInput', 1000);
        mappingPage
            .click('@gridPreviewInput');
        client
            .keys('bj')
            .waitForElementVisible(previewPopUpValue, 1000)
            .keys(client.Keys.ENTER);
        mappingPage
            .waitForElementPresent('@firstRowSourceText', 1000)
            .getText('@firstRowSourceText', function(result) {
                client.assert.equal(result.value, '(bjensen@example.com)', 'source text should be email');
            })
            .waitForElementPresent('@firstRowTargetText', 1000)
            .getText('@firstRowTargetText', function(result) {
                client.assert.equal(result.value, '(bjensen@example.com)', 'target text should be email');
            });
    },

    "Grid should reload to an empty table on page refresh": function(client) {
        var mapping = client.page.mapping(),
            attributesGridRows = mapping.elements.attributesGridRows.selector;

        client
            .refresh();
        mapping
            .waitForElementVisible('@attributesGrid', 4000);
        client
            .elements('css selector', attributesGridRows, function(result) {
                client.assert.ok(result.value.length === 1, 'Attributes grid starts with 1 item.');
            });
        mapping
            .waitForElementPresent('@firstRowSourceText', 1000)
            .getText('@firstRowSourceText', function(result) {
                client.assert.equal(result.value, '', 'source text starts empty');
            })
            .getText('@firstRowTargetText', function(result) {
                client.assert.equal(result.value, '', 'target text starts empty');
            });
    }
};

resetMapping = {"_id": "sync","mappings": []};

singleMapping = {
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

sampleData = {
    "result": [{
        "_id": "bjensen",
        "email": "bjensen@example.com",
        "description": "Created By XML1",
        "lastname": "Jensen",
        "roles": "openidm-authorized",
        "name": "bjensen@example.com",
        "firstname": "Barbara",
        "mobileTelephoneNumber": "1234567"
    }],
    "resultCount": 1,
    "pagedResultsCookie": null,
    "totalPagedResultsPolicy": "NONE",
    "totalPagedResults": -1,
    "remainingPagedResults": -1
};
