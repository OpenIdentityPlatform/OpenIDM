var xssTestUser,
    nonXssTestUser;

module.exports = {
    before: function(client) {
        client.globals.login.helpers.login(client);

        [xssTestUser, nonXssTestUser].map(function(userData, ind) {
            client.execute(function(user, id) {
                require(["org/forgerock/openidm/ui/common/delegates/ResourceDelegate"], function(delegate) {
                        delegate.createResource('/openidm/managed/user', id, user);
                });
            }, [userData, 'user_' + ind]);
        });
    },

    after: function(client) {
        ['user_0', 'user_1'].map(function(userId) {
            client.execute(function(id) {
                require(["org/forgerock/openidm/ui/common/delegates/ResourceDelegate"], function(delegate) {
                        delegate.deleteResource('/openidm/managed/user', id);
                });
            }, [userId]);
        });
        client.end();
    },

    'Edit object does not trigger alert on wierd html user': function (client) {

        client
            .url('http://localhost:8080/admin/#resource/managed/user/edit/user_0').pause(1000)
            .getAlertText(function(result) {
                // error message on the result object indicates no alert was present
                client.assert.ok(!!result.error, 'No alert fired.');
            });
    },

    'Edit object does not trigger alert on normal user name': function (client) {

        client
            .url('http://localhost:8080/admin/#resource/managed/user/edit/user_1').pause(1000)
            .getAlertText(function(result) {
                // error message on the result object indicates no alert was present
                client.assert.ok(!!result.error, 'No alert fired.');
            });
    }
};

xssTestUser = {
    "mail": "t@t.t",
    "sn": "t",
    "givenName": "t",
    "userName": "<img src='x' onerror=alert('fail')>",
    "accountStatus": "active",
    "effectiveRoles": [],
    "effectiveAssignments": []
};

nonXssTestUser = {
    "mail": "t@t.t",
    "sn": "t",
    "givenName": "t",
    "userName": "O'Malley",
    "accountStatus": "active",
    "effectiveRoles": [],
    "effectiveAssignments": []
};
