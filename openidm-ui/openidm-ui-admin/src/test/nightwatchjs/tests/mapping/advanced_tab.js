var resetMapping,
    singleMapping,
    mappingPage,
    advancedTab;


module.exports = {
    before: function(client, done) {
        client.globals.login.helpers.login(client);
        client.globals.config.update("sync", singleMapping, function() {
            mappingPage = client.page.mapping();
            advancedTab = mappingPage.section.advancedTab;
            client
                .url(client.globals.baseUrl + '#advanced/managedAssignment_managedRole/');
            done();
        }, true);
    },

    after: function(client, done) {
        client.globals.config.resetAll(function() {
            client.end();
            done();
        });
    },

    "Should show the correct adanced mapping config on first render": function(client) {
        // do something
    },
    "Should detect changes to the config": function(client) {
        // do something
    },

    "Should activate buttons/ changes pending warning on a per panel basis": function(client) {
        // do something
    },

    "Should correctly configure the mapping on 'save'": function(client) {
        // do something
    },

    "Should reset the buttons to the current mapping on 'cancel'": function(client) {
        // do something
    }

};


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
