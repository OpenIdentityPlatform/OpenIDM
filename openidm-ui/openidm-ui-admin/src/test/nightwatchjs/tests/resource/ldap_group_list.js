var response;

module.exports = {
    before: function(client) {
        client.globals.login.helpers.login(client);
    },

    after: function(client) {
        client.end();
    },

    'Table should display mock data': function(client) {
        var ldapGroupList = client.page.ldapGroupList(),
            expectedText = "uid=jdoe,ou=People,dc=example,dc=com test1 openidm";


        client
            .execute(function(data) {
                require(["http://sinonjs.org/releases/sinon-1.12.2.js"], function(sinon){
                    var server = sinon.fakeServer.create(),
                    url = "/openidm/system/ldap/group?page=1&_pageSize=50&sort_by=dn&_sortKeys=dn&_queryFilter=true&_pagedResultsOffset=0&_totalPagedResultsPolicy=ESTIMATE";

                    server.autoRespond = true;
                    server.xhr.useFilters = true;

                    server.xhr.addFilter(function(method, url) {
                      //whenever the return is true the request will not faked
                      return !url.match(/\/openidm\/system\/ldap\/group.*/);
                    });
                    server.respondWith("GET", url, [ 200, { "Content-Type": "application/json" }, data ]);
                });
                return data;
            }, [response]).pause(2000);

        ldapGroupList
            .navigate()
            .waitForElementVisible('@tableGrid', 2000)
            .getText('@tableRow', function(result){
                client.assert.equal(result.value, expectedText);
            });
    }
};

response = JSON.stringify({
    "result": [{
        "_id": "cn=openidm,ou=Groups,dc=example,dc=com",
        "ou": [],
        "uniqueMember": ["uid=jdoe,ou=People,dc=example,dc=com"],
        "cn": ["openidm"],
        "owner": [],
        "o": [],
        "description": [],
        "dn": "test1",
        "seeAlso": [],
        "businessCategory": []
    }],
    "resultCount": 1,
    "pagedResultsCookie": null,
    "totalPagedResultsPolicy": "NONE",
    "totalPagedResults": -1,
    "remainingPagedResults": -1
});
