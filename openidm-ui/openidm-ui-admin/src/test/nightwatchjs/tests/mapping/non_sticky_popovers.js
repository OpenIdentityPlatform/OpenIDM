var iSelector = 'i[data-title="Source object correlates to multiple target objects, without a link."]',
    mapping = {
        "_id": "sync",
        "mappings": [
            {
                "target": "managed/role",
                "source": "managed/assignment",
                "name": "managedAssignment_managedRole",
                "properties": [{ "source": "email", "target": "mail" }],
                "policies": [{  "action": "ASYNC",  "situation": "ABSENT" }]
            }
        ]
    };

module.exports = {
    before: function(client, done) {
        client.globals.login.helpers.login(client);
        client.globals.config.update("sync", mapping, done, true);
    },
    after: function(client, done) {
        client.globals.config.resetAll(function() {
            client.end();
            done();
        });
    },

    "It should trigger on hover": function(client) {
        client
            .url(client.globals.baseUrl + "#behaviors/managedAssignment_managedRole/")
            .waitForElementPresent(iSelector, 1000)
            .element('css selector', iSelector, function(result) {
                client.moveTo(result.value.ELEMENT);
            })
            .waitForElementPresent("div.popover", 1000)
            .getAttribute(iSelector, 'aria-describedby', function(result) {
                var popoverId = "#" + result.value;
                client
                    .assert.elementPresent(popoverId)
                    .moveToElement(iSelector, 100, 100).pause(250)
                    .assert.elementNotPresent(popoverId);
            });
    },

    "It should trigger on focus": function(client) {

        client
            .execute(function(args) {
                var event = new FocusEvent('focus');
                document.querySelector(args).dispatchEvent(event);
                return true;
            },[iSelector])
            .waitForElementPresent("div.popover", 1000)
            .getAttribute(iSelector, 'aria-describedby', function(result) {
                var popoverId = "#" + result.value;
                client
                    .assert.elementPresent(popoverId).pause(250)
                    .execute(function(args) {
                        var event = new FocusEvent('blur');
                        document.querySelector('i[data-title="Source object correlates to multiple target objects, without a link."]').dispatchEvent(event);
                        return true;
                    },[iSelector]).pause(250)
                    .assert.elementNotPresent(popoverId);
            });
    },

    "It should not stick on click": function(client) {

        client
            .element('css selector', iSelector, function(result) {
                client.moveTo(result.value.ELEMENT);
            })
            .getAttribute(iSelector, 'aria-describedby', function(result) {
                var popoverId = "#" + result.value;
                client
                    .assert.elementPresent(popoverId)
                    .click(iSelector)
                    .moveToElement(iSelector, 100, 100).pause(250)
                    .assert.elementNotPresent(popoverId);
            });
    }
};
