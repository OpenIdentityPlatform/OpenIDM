var resetMapping,
    singleMapping,
    mappingPage,
    modalBody,
    behaviorsTab;

module.exports = {
    before: function(client, done) {
        client.globals.login.helpers.login(client);
        client.globals.config.update("sync", singleMapping, function() {
            mappingPage = client.page.mapping();
            behaviorsTab = mappingPage.section.behaviorsTab;
            modalBody = mappingPage.section.modalBody;
            client
                .url(client.globals.baseUrl + '#behaviors/managedAssignment_managedRole/');
            done();
        }, true);
    },

    after: function(client, done) {
        client.globals.config.resetAll(function() {
            client.end();
            done();
        });
    },

    "Policy shouldn't be removable if there is only one of each": function(client) {
        behaviorsTab
            .waitForElementPresent('@policiesTable', 2000)
            // check that the delete policy button not present on the first row
            .assert.elementNotPresent("@deletePolicyButton");
    },

    "Adding a second type of the same policy should allow for delete": function(client) {
        var policiesTable = behaviorsTab.elements.policiesTable.selector,
            addPolicyButton = behaviorsTab.elements.addPolicyButton.selector,
            policyDialogSubmit = mappingPage.elements.policyDialogSubmit.selector,
            policiesTableRows = behaviorsTab.elements.policiesTableRows.selector,
            rowsCount;

        client
            .elements('css selector', policiesTableRows, function(result) {
                client.assert.equal(result.value.length, 13, "13 default policies (one of each)");
                rowsCount = result.value.length;
            })
            // for whatever reason, this click through cycle is not triggering the new policy
            // have to do it twice
            .click(addPolicyButton)
            .waitForElementVisible(policyDialogSubmit, 2000)
            .click(policyDialogSubmit)
            .waitForElementNotPresent(policyDialogSubmit, 2000)

            .click(addPolicyButton)
            .waitForElementVisible(policyDialogSubmit, 2000)
            .click(policyDialogSubmit)
            .waitForElementNotPresent(policyDialogSubmit, 2000);

        mappingPage
            .waitForElementNotPresent("@policyDialogSubmit", 2000);

        client
            .elements('css selector', policiesTableRows, function(result) {
                client.assert.equal(result.value.length, rowsCount + 1);
            });

        behaviorsTab
            // check that the delete policy button is now present on the first row
            .assert.elementPresent("@deletePolicyButton");
    },

    "Current policy drop down should change all the policy types to the specific pre defined": function(client) {
        behaviorsTab
            .assert.containsText("@policiesTableFirstRow", 'Exception')
            .setValue("@policyPatterns", 'Read-only')
            .click("@policyPatterns");

        client.keys(client.Keys.RETURN);

        behaviorsTab
            .click("@policyPatterns");

        mappingPage
            .waitForElementVisible("@confirmPolicyPatternButton", 2000)
            .click("@confirmPolicyPatternButton");

        behaviorsTab
            .assert.containsText("@policiesTableFirstRow", 'Async');
    },

    "Editing a policy should provide three options": function(client) {
        var editOptions = [
            {tab: "#restrictToTab", text: "When these Conditions apply"},
            {tab: "#actionTab", text: "Perform this Action"},
            {tab: "#actionCompleteTab", text: "And On Complete"}
        ];

        behaviorsTab
            .waitForElementPresent('@policiesTable', 2000)
            .click("@policiesTableFirstRow")
            .click("@editPolicyButton");

        mappingPage
            .waitForElementVisible("@modal", 2000);

        editOptions.forEach(function(option) {
            client
                .expect.element(option.tab).to.be.visible;
            client
                .assert.containsText(option.tab, option.text);
        });

    },

    "Editing an option should update corresponding grid field": function(client) {
        client.refresh();

        behaviorsTab
            .waitForElementPresent('@policiesTable', 2000)
            .click("@policiesTableFirstRow")
            .click("@editPolicyButton");

        modalBody
            .waitForElementPresent("@actionTab", 2000)
            .click("@actionTab")
            .waitForElementVisible('@defaultActionPane', 2000)
            .setValue("@defaultActionPaneSelect", "ASYNC");

        client
            .keys(client.Keys.RETURN);

        mappingPage
            .click("@policyDialogSubmit");

        behaviorsTab
            .assert.containsText("@policiesTableFirstRowActionColumn", 'Async')
            .click("@policiesTableFirstRow")
            .click("@editPolicyButton");

        modalBody
            .waitForElementPresent("@actionCompleteTab", 2000)
            .click("@actionCompleteTab")
            .waitForElementVisible('@actionCompletePre', 2000)
            .click("@actionCompletePre");

        client
            .keys("hello");

        mappingPage
            .click("@policyDialogSubmit");

        behaviorsTab
            .assert.containsText("@policiesTableFirstRowOnCompleteColumn", '(text/javascript)')
            .click("@policiesTableFirstRow")
            .click("@editPolicyButton");

        modalBody
            .waitForElementPresent("@restrictToTab", 2000)
            .click("@restrictToTab")
            .waitForElementVisible('@conditionFilterPaneSelect', 2000)
            .setValue('@conditionFilterPaneSelect', 'and');

        client
            .keys(client.Keys.RETURN);

        mappingPage
            .click("@policyDialogSubmit");

        behaviorsTab
            .assert.containsText("@policiesTableFirstRowConditionColumn", '(( eq "" or eq ""))');
    },

    "Reset button should return the policy grid to the last saved state": function(client) {
        client.refresh();

        behaviorsTab
            .waitForElementVisible('@policiesTable', 2000)
            .assert.containsText("@policiesTableFirstRowActionColumn", 'Exception')
            .click("@policiesTableFirstRow")
            .click("@editPolicyButton");

        modalBody
            .waitForElementPresent("@actionTab", 2000)
            .click("@actionTab")
            .waitForElementVisible('@defaultActionPane', 2000)
            .setValue("@defaultActionPaneSelect", "ASYNC");

        client
            .keys(client.Keys.RETURN);

        mappingPage
            .click("@policyDialogSubmit");

        behaviorsTab
            .assert.containsText("@policiesTableFirstRowActionColumn", 'Async')
            .waitForElementVisible("@reset", 2000)
            .click("input.reset")
            .waitForElementVisible("@policiesTableFirstRowActionColumn", 2000)
            .assert.containsText("@policiesTableFirstRowActionColumn", 'Exception');
    },

    "Saving the policy grid should retain policy edits": function(client) {
        client.refresh();

        behaviorsTab
            .waitForElementVisible('@policiesTable', 2000)
            .assert.containsText("@policiesTableFirstRowActionColumn", 'Exception')
            .click("@policiesTableFirstRow")
            .click("@editPolicyButton");

        modalBody
            .waitForElementPresent("@actionTab", 2000)
            .click("@actionTab")
            .waitForElementVisible('@defaultActionPane', 2000)
            .setValue("@defaultActionPaneSelect", "ASYNC");

        client
            .keys(client.Keys.RETURN);

        mappingPage
            .click("@policyDialogSubmit");

        behaviorsTab
            .assert.containsText("@policiesTableFirstRowActionColumn", 'Async')
            .waitForElementVisible("@save", 2000)
            .click("@save");

        client
            .refresh();

        behaviorsTab
            .waitForElementVisible("@policiesTableFirstRowActionColumn", 2000)
            .assert.containsText("@policiesTableFirstRowActionColumn", 'Async');
    }



};

resetMapping = {"_id": "sync","mappings": []};

singleMapping = {
    "_id": "sync",
    "mappings": [
        {
          "target": "managed/role",
          "source": "managed/assignment",
          "name": "managedAssignment_managedRole",
          "properties": [
              {
                  "source": "email",
                  "target": "mail"
              }
          ],
          "policies": [
                {
                  "action": "ASYNC",
                  "situation": "ABSENT"
                }
            ]
        }
    ]
};
