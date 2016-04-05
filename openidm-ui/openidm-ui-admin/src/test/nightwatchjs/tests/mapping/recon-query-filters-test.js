var resetMapping,
    singleMapping,
    mappingPage,
    targetQuery;

module.exports = {
    before: function(client, done) {
        client.globals.login.helpers.login(client);
        client.config.update("sync", singleMapping, function() {
            mappingPage = client.page.mapping();
            targetQuery = mappingPage.section.targetQuery;
            client
                .url(client.globals.baseUrl + '#association/managedAssignment_managedRole/');
            done();
        });
    },

    after: function(client, done) {
        client.config.update("sync", resetMapping, function() {
            client.end();
            done();
        });
    },

    "It should generate correct recon query filter for 'Any of'": function(client) {
        var filter = "#targetQuery " + targetQuery.elements.filter.selector;

        mappingPage
            .waitForElementVisible("@reconQueryFilterPanelToggle", 2000)
            .click("@reconQueryFilterPanelToggle");

        targetQuery
            .waitForElementVisible('@select', 2000)
            .setValue('@select', ['any of', client.Keys.ENTER] )
            .setValue('@firstInputName', ['firstName', client.Keys.TAB])
            .setValue('@firstInputValue', ['null', client.Keys.TAB])
            .setValue('@secondInputName', ['sn', client.Keys.TAB])
            .setValue('@secondInputValue', ['jenson', client.Keys.TAB]);

        client
            .getText(filter, function(result) {
                client.assert.equal(result.value, '(firstName eq "null" or sn eq "jenson")');
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
