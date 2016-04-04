module.exports = {
    before: function(client) {
        client.globals.login.helpers.login(client);
    },
    after: function(client) {
        client.end();
    },
    'Changes script type': function (client) {
        var scriptEditor = client.page.scriptEditor();

        scriptEditor.navigate()
            .waitForElementPresent("@auditEventTopics", 2000)
            .click("@authenticationEvent")
            .waitForElementPresent("@scriptTab", 1000)
            .click("@scriptTabLink")
            .waitForElementPresent("@scriptType", 1000);

        client
            .execute(function () {
                $("#auditEventsForm .CodeMirror")[0].CodeMirror.setValue("true;");
                $("#auditEventsForm .event-select").val("groovy");
                $("#auditEventsForm .event-select").trigger("change");
            })
            .pause(1000);

        scriptEditor
            .click("@submitAuditEvent");

        client
            .pause(1000);

        scriptEditor
            .click("@authenticationEvent")
            .waitForElementPresent("@scriptTab", 1000)
            .click("#scriptTab a")
            .assert.value("@scriptType", "groovy");
    }
};