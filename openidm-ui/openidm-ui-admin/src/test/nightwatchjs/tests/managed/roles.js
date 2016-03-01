module.exports = {
        before : function (client, done) {
            client.globals.login.helpers.setSession(client, function () {
                done();
            });
        },
        after : function (client) {
            client.config.resetAll(function(data) {
                client.end();
            });
        },
        'Add Role1': function (client) {
            var rolesList = client.page.rolesList(),
                rolesEdit = client.page.rolesEdit();
            
            rolesList
                .navigate()
                .waitForElementPresent('@newRoleButton', 2000)
                .click('@newRoleButton');
           
            rolesEdit
                .waitForElementPresent('@nameInput', 2000)
                .setValue('@nameInput', "Role1")
                .setValue('@descriptionInput', "Role1 description")
                .click('@saveButton')
                .waitForElementPresent('@alertMessage', 2000)
                .waitForElementVisible('@alertMessage', 2000)
                .expect.element('@alertMessage').text.to.equal("Successfully added Role");
        },
        'Condition tab status Displayed': function (client) {
            var rolesEdit = client.page.rolesEdit();
            
            client.pause(3000);//don't know why but the click on @conditionTabHeader does not work unless we wait 3 seconds...can't be any less than that
            
            rolesEdit
                .waitForElementPresent('@conditionTabHeader', 2000, 'Condition tab is displayed')
                .click('@conditionTabHeader')
                .waitForElementPresent('@enableConditionSlider', 2000)
                .click('@enableConditionSlider')
                .waitForElementPresent('@conditionStatus', 2000)
                .waitForElementVisible('@conditionStatus', 2000, 'Condition status is displayed in condition tab');
            
        },
        'Condition is set and changes pending shows Condition as pending': function (client) {
            var rolesEdit = client.page.rolesEdit();
            
            rolesEdit
                .click('@conditionOpSelect')
                .click('@conditionOpValueForOption')
                .setValue('@conditionOpNameInput','name')
                .setValue('@conditionOpValueInput','BigBoss')
                .click('@conditionQueryText')
                .expect.element('@conditionQueryText').text.to.equal('name eq "BigBoss"');
            
            rolesEdit
                .waitForElementVisible('@changesPending', 2000)
                .expect.element('@changesPending').text.to.equal('- Condition');
        },
        'Condition is properly saved': function (client) {
            var rolesList = client.page.rolesList(),
                rolesEdit = client.page.rolesEdit();
            
            rolesEdit
                .click('@saveButton')
                .waitForElementPresent('@alertMessage', 2000)
                .waitForElementVisible('@alertMessage', 2000)
                .expect.element('@alertMessage').text.to.equal("Successfully updated Role");
            
            rolesList
                .navigate()
                .waitForElementPresent('@grid', 2000)
                .expect.element('@firstGridRowNameCell').text.to.equal("Role1");
            
            rolesList
                .click('@firstGridRowNameCell');
            
            rolesEdit
                .waitForElementPresent('@conditionTabHeader', 2000)
                .click('@conditionTabHeader')
                .waitForElementVisible('@conditionQueryText', 2000)
                .expect.element('@conditionQueryText').text.to.equal('name eq "BigBoss"', 'After saving Role1 going to the grid and coming back the condition query text is the correct value');
        },
        'Remove Role1': function (client) {
            var rolesList = client.page.rolesList(),
                rolesEdit = client.page.rolesEdit();
            
            rolesEdit
                .waitForElementPresent('@deleteButton', 2000)
                .click('@deleteButton')
                .waitForElementPresent('@confirmationOkButton', 2000)
                .waitForElementVisible('@confirmationOkButton', 2000)
                .click('@confirmationOkButton');
            
            rolesList
                .waitForElementPresent('@newRoleButton', 2000)
                .expect.element('@emptyListMessage').text.to.equal("No Data");
        }
};