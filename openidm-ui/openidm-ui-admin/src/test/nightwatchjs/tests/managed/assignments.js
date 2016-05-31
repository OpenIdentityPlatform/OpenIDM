module.exports = {
        before : function (client, done) {
            client.globals.login.helpers.setSession(client, function () {
                client.globals.config.read("managed", function() {
                    done();
                });
            });
        },
        after : function (client, done) {
            client.globals.config.resetAll(function(data) {
                client.end();
                done();
            });
        },
        'Add Assignment': function (client) {
            var assignmentsList = client.page.assignmentsList(),
                assignmentsEdit = client.page.assignmentsEdit();

            assignmentsList
                .navigate()
                .waitForElementPresent('@newAssignmentButton', 2000)
                .click('@newAssignmentButton');

            assignmentsEdit
                .waitForElementPresent('@nameInput', 2000)
                .waitForElementPresent('@descriptionInput', 2000)
                .setValue('@nameInput', "Assignment1")
                .setValue('@descriptionInput', "Assignment1 description")
                .click('@addButton')
                .waitForElementPresent('@alertMessage', 2000)
                .waitForElementVisible('@alertMessage', 2000)
                .expect.element('@alertMessage').text.to.equal("Assignment successfully saved.");
        },

        'OPENIDM-5444 onAssignment popover stays visible and selection can be made': function (client) {
            var assignmentsEdit = client.page.assignmentsEdit();

            assignmentsEdit
                .waitForElementPresent('@attributesTabHeader', 2000)
                .click('@attributesTabHeader')
                .waitForElementVisible('@addAttributeButton', 2000)
                .click('@addAttributeButton')
                .waitForElementVisible('@assignmentOperationPopover', 2000)
                .click('@assignmentOperationPopover')
                .waitForElementVisible('@onAssignmentSelect', 2000)
                .click('@onAssignmentSelect')
                .waitForElementVisible('@onAssignmentSelectOption', 2000)
                .click('@onAssignmentSelectOption')
                .click('@assignmentOperationPopover');
        },

        "OPENIDM-4859 manager on assignment attribute tab should load an object json editor": function(client) {
            var assignmentsEdit = client.page.assignmentsEdit();

            assignmentsEdit
                .waitForElementVisible('@attributeSelect', 2000)
                .click('@attributeSelect')
                .waitForElementVisible('@attributeSelectManagerOption', 2000)
                .click('@attributeSelectManagerOption')
                .click('@attributeSelect')
                .assert.attributeEquals("@attributeJSONEditorRoot", "data-schematype", "object");
        },

        'Remove Assignment1': function (client) {
            var assignmentsList = client.page.assignmentsList(),
                assignmentsEdit = client.page.assignmentsEdit();

            assignmentsEdit
                .waitForElementPresent('@deleteButton', 2000)
                .waitForElementVisible('@deleteButton', 2000)
                .click('@deleteButton')
                .waitForElementPresent('@confirmationOkButton', 2000)
                .waitForElementVisible('@confirmationOkButton', 2000)
                .click('@confirmationOkButton');

            assignmentsList
                .waitForElementPresent('@newAssignmentButton', 2000)
                .expect.element('@emptyListMessage').text.to.equal("No Data");
        }
};
