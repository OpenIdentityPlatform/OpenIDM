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
                .url(client.globals.baseUrl + "#association/managedAssignment_managedRole/");
            done();
        });
    },

    after: function(client, done) {
        client.config.update("sync", resetMapping, function() {
            client.end();
            done();
        });
    },

    "Query filter should add two fields for 'any of / all of'": function(client) {
        mappingPage
            .waitForElementVisible("@reconQueryFilterPanelToggle", 2000)
            .click("@reconQueryFilterPanelToggle");

        targetQuery
            .waitForElementVisible("@select", 2000)
            .setValue("@select", ["any of", client.Keys.ENTER] )
            .waitForElementVisible("ul.subgroup", 2000);

        client.elements("css selector", "ul.subgroup li[index]", function(results) {
            client.assert.equal(results.value.length, 2, "'Any of / All of' adds 2 nodes");
        });
    },

    "Filter should clear down to one line for 'None of'": function(client) {
        targetQuery
            .waitForElementVisible("@select", 2000)
            .setValue("@select", ["none of", client.Keys.ENTER] )
            .waitForElementVisible("ul.subgroup", 2000);

        client.elements("css selector", "ul.subgroup li[index]", function(results) {
            client.assert.equal(results.value.length, 1, "'none of' clears to 1 nodes");
        });
    },

    "Filter should clear to no lines and an empty filter string for 'No filters'": function(client) {
        var filter = "#targetQuery " + targetQuery.elements.filter.selector;

        targetQuery
            .waitForElementVisible("@select", 2000)
            .setValue("@select", ["No Filter", client.Keys.ENTER] )
            .expect.element("ul.subgroup").to.not.be.present.after(2000);

        client.elements("css selector", "ul.subgroup li[index]", function(results) {
            client.assert.equal(results.value.length, 0, "'no filters' clears all nodes");
        });

        client
            .getText(filter, function(result) {
                client.assert.equal(result.value, "", "'no filters' clears filter string");
            });
        client.refresh();
    },

    "It should generate correct recon query filter for 'Any of'": function(client) {
        var filter = "#targetQuery " + targetQuery.elements.filter.selector;

        mappingPage
            .waitForElementVisible("@reconQueryFilterPanelToggle", 2000)
            .click("@reconQueryFilterPanelToggle");

        targetQuery
            .waitForElementVisible("@select", 2000)
            .setValue("@select", ["any of", client.Keys.ENTER] )
            .setValue("@firstInputName", ["firstName", client.Keys.TAB])
            .setValue("@firstInputValue", ["null", client.Keys.TAB])
            .setValue("@secondInputName", ["sn", client.Keys.TAB])
            .setValue("@secondInputValue", ["jenson", client.Keys.TAB])
            .expect.element("@filter").to.contain.text('(firstName eq "null" or sn eq "jenson")').after(2000);
    }
};

resetMapping = {"_id": "sync", "mappings": []};

singleMapping = {
    "_id": "sync",
    "mappings": [
        {
            "target": "managed/role",
            "source": "managed/assignment",
            "name": "managedAssignment_managedRole",
            "properties": [ { "source": "email", "target": "mail" } ],
            "policies": [ { "action": "ASYNC", "situation": "ABSENT" } ]
        }
    ]
};
