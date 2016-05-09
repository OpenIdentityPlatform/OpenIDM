var whsObj;

module.exports = {
    before: function(client, done) {

        client.globals.login.helpers.setSession(client, function() {
            client.config.read('managed', function() {
                client.config.update('managed', whsObj, done);
            });
        });

    },
    after: function(client, done) {

        client
            .config.reset('managed', function() {
                client.end();
                done();
            });
    },
    "It should not allow xss attacks on page": function(client) {

        client
            .url(client.globals.baseUrl + '#resource/managed/Whs/add/').pause(1000)
            .getAlertText(function(result) {
                // error message on the result object indicates no alert was present
                client.assert.ok(!!result.error, 'No alert fired.');
            });

    }
};

whsObj = {
  "_id": "managed",
  "objects": [
    {
      "name": "Whs",
      "schema": {
        "$schema": "http://forgerock.org/json-schema#",
        "type": "object",
        "title": "",
        "description": "",
        "properties": {
          "": {
            "description": "\"><script>alert(1)</script>",
            "title": "",
            "viewable": true,
            "searchable": false,
            "type": "string"
          }
        },
        "required": [],
        "order": [
          ""
        ],
        "icon": ""
      },
      "iconClass": "icon-database",
      "iconSrc": "img/icon-managedobject.png"
    }
  ]
};
