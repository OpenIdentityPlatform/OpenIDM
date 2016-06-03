module.exports = {
        'Changes to passed variables being saved': function (client) {
            // Originally noticed with OPENIDM-4954, so tested in the context of audit settings

            var passedVariables = "#filterScript .passed-variables-holder .list-table ";

            function openDialog() {
                return client
                    .click("#AuditTopicsView .editEvent[data-name=authentication]")
                    .waitForElementPresent("#scriptTab", 1000)
                    .click("#scriptTab a");
            }

            client.globals.login.helpers.login(client);

            // load the audit script editor dialog and save a trivial script value
            client
                .url(client.globals.baseUrl + "#settings/audit/")
                .waitForElementPresent("#AuditTopicsView", 2000);

            openDialog()
                .execute(function () {
                    $("#auditEventsForm .CodeMirror")[0].CodeMirror.setValue("true;");
                })
                .click("#submitAuditEvent")
                .pause(1000)
                .click("#submitAudit")
                .pause(1000);

            // open the dialog again to set a new passed variable entry
            openDialog()
                .click("#filterScript .add-passed-variables")
                .waitForElementVisible(passedVariables + ".passed-variable-name", 100)
                .setValue(passedVariables + ".list-group-item .passed-variable-name input", "foo")

                .click(passedVariables + ".list-group-item .editor-body select")
                .pause(100)
                .click(passedVariables + ".list-group-item .editor-body select option[value=string]")
                .pause(100)
                .click(passedVariables + ".list-group-item .editor-body select")

                .waitForElementVisible(passedVariables + ".list-group-item .editor-body input[name=root]", 100)
                .click(passedVariables + ".list-group-item .editor-body input[name=root]")
                .keys("bar")
                .execute(function () {
                    $("#filterScript .passed-variables-holder .list-table .list-group-item .editor-body input[name=root]").trigger("change");
                })
                .pause(1000)
                .click("#submitAuditEvent");

            //verify that there are changes pending as a result
            client.expect.element('#AuditEventsBody .alert-warning [role=heading]').text.to.equal("Changes Pending");

            client.pause(1000);

            //open the dialog again, ensure that passed variables are still present
            openDialog();

            client.expect.element(passedVariables + ".passed-variable-name input").to.have.value.that.equals('foo');

            client.pause(1000);

            client.expect.element(passedVariables + ".editor-body input[name=root]").to.have.value.that.equals('bar');

            //remove them and make sure 'Changes Pending' prompt is gone
            client
                .click(passedVariables + ".btn-delete-attribute")
                .pause(1000)
                .click("#submitAuditEvent")
                .pause(1000);

            client.expect.element('#AuditEventsBody .alert-warning').to.not.be.visible;


            // remove the trivial script
            openDialog()
                .execute(function () {
                    $("#auditEventsForm .CodeMirror")[0].CodeMirror.setValue("");
                })
                .click("#submitAuditEvent")
                .pause(1000)
                .click("#submitAudit");
        }
};
