module.exports = {
    before: function(client) {
        client.globals.login.helpers.login(client);
        client.connectors.addLdap();
    },

    after: function(client) {
        var connectors = client.page.connectors();    

        connectors
            .navigate();
        client
            .refresh();
        connectors
            .waitForElementVisible('@ldapCard', 2000)
            .waitForElementVisible('@ellipsis', 2000)
            .click('@ellipsis')
            .waitForElementVisible('@deleteLDAP', 2000)
            .click('@deleteLDAP')
            .waitForElementVisible('@confirmDelete', 2000)
            .click('@confirmDelete')
            .waitForElementVisible('#messages', 2000)
            .waitForElementNotVisible('#messages', 4000);
        client
            .end();
    },

    'Table should display data': function(client) {
        var ldapGroupList = client.page.ldapGroupList(),
            expectedText = "uid=jdoe,ou=People,dc=example,dc=com cn=openidm,ou=Groups,dc=example,dc=com openidm";

        ldapGroupList
            .navigate()
            .waitForElementVisible('@tableGrid', 2000)
            .getText('@tableRow', function(result) {
                client.assert.deepEqual(result.value, expectedText);
            });
    }
};
