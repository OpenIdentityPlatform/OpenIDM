var response;

module.exports = {
    before: function(client) {
        client.globals.login.helpers.login(client);
    },

    after: function(client) {
        client.end();
    },

    'Update should display binaries': function(client) {
        var update = client.page.update(),
            expected = [true, true, false, false];

        update
            .navigate()
            .waitForElementVisible("@infoAlert", 2000);

        client
            .timeoutsAsyncScript(2000)
            .executeAsync(function(data) {
                require(["http://sinonjs.org/releases/sinon-1.12.2.js"], function(sinon) {
                    var server = sinon.fakeServer.create(),
                        url = "/openidm/maintenance/update?_action=available";

                    server.autoRespond = true;
                    server.xhr.useFilters = true;

                    server.xhr.addFilter(function(method, url) {
                      //whenever the return is true the request will not faked
                      return !url.match(/\/openidm\/maintenance\/update\?_action=available/);
                    });
                    server.respondWith("POST", url, [ 200, { "Content-Type": "application/json" }, data ]);
                });
                return true;
            }, [response], function() {
                update
                    .click("@refresh")
                    .waitForElementVisible("@errorAlert", 2000);
                    client.elements('css selector', 'button.test', function (elements) {
                        elements.value.forEach(function(element) {
                            client.elementIdEnabled(element.ELEMENT, function(response) {
                                client.assert.equal(response.value, expected.shift());
                            });
                        });
                    });

            });
    }
};

response = JSON.stringify({
	"updates": [{
		"archive": "openidm.zip",
		"fileSize": 79964153,
		"fileDate": "2016-03-07T09:21:08.0-08:00",
		"checksum": "41AC880D1529489C47D6BC427834A6B5",
		"fromProduct": "OpenIDM",
		"fromVersion": ["4.1.0-SNAPSHOT"],
		"toProduct": "OpenIDM",
		"toVersion": "4.2.0",
		"description": "Full product installation",
		"resource": "https://forgerock.org/openidm/doc/bootstrap/release-notes/",
		"restartRequired": true
	}],
	"rejects": [{
		"archive": "openidm3.zip",
		"reason": "The archive openidm3.zip can be used only to update version '[4.0]' and you are running version 4.1.0-SNAPSHOT"
	}]
});
