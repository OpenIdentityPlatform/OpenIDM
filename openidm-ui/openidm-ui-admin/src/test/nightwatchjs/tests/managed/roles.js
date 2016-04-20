module.exports = {
        before : function (client, done) {
            client.globals.login.helpers.setSession(client, function () {
                //add a dummy user for use in a test where a user is needed in a dropdown
                client.globals.user.createDummyUser(client, function () {
                    done();
                });
            });
        },
        after : function (client) {
            client.config.resetAll(function(data) {
                //remove the dummy user
                client.globals.user.removeDummyUser(client, function () {
                    client.end();
                });
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
        "OPENIDM-4857 Click a disabled reset button doesn't reload the page": function(client) {
            var rolesEdit = client.page.rolesEdit();

            rolesEdit
                .waitForElementPresent('@resetButton', 2000)
                .click("@resetButton")
                .expect.element("@spinner").to.not.be.present;
        },
        'Condition is set and changes pending shows Condition as pending': function (client) {
            var rolesEdit = client.page.rolesEdit();

            rolesEdit
                .waitForElementPresent('@enableConditionSlider', 2000)
                .click('@enableConditionSlider')
                .click('@conditionOpSelect')
                .click('@conditionOpValueForExpr')
                .waitForElementPresent('@conditionOpNameInputFirst', 2000)
                .click('@conditionOpNameInputFirst')
                .waitForElementPresent('@conditionOpNameInputSelectOption', 2000)
                .waitForElementVisible('@conditionOpNameInputSelectOption', 2000)
                .click('@conditionOpNameInputSelectOption')
                .setValue('@conditionOpValueInput','BigBoss')
                .click('@conditionQueryText')
                .expect.element('@conditionQueryText').text.to.equal('/mail eq "BigBoss"');

            rolesEdit
                .waitForElementVisible('@changesPending', 2000)
                .expect.element('@changesPending').text.to.equal('- Condition');
        },
        'Condition is properly saved': function (client) {
            var rolesList = client.page.rolesList(),
                rolesEdit = client.page.rolesEdit();

            rolesEdit
                .waitForElementNotPresent('@alertMessage', 2000)
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
                .waitForElementVisible('@conditionQueryText', 2000)
                .expect.element('@conditionQueryText').text.to.equal('/mail eq "BigBoss"', 'After saving Role1 going to the grid and coming back the condition query text is the correct value');
        },
        "When condition is changed to use 'All of' two comparisons show up by default": function(client) {
            var rolesEdit = client.page.rolesEdit();

            rolesEdit
                .waitForElementPresent('@conditionOpSelect', 2000)
                .click('@conditionOpSelect')
                .click('@conditionOpValueForAnd')
                .waitForElementPresent('@conditionOpNameInputSecond', 2000);
        },
        'Condition is disabled and removed from role': function (client) {
            var rolesEdit = client.page.rolesEdit(),
                checkCondition = function (hasCondition, message) {
                    client
                        .timeoutsAsyncScript(2000)
                        .executeAsync(
                            function(arg, done) {
                                require(["lodash","org/forgerock/openidm/ui/admin/role/EditRoleView"], function(_, editRoleView) {
                                    done(_.has(editRoleView.oldObject,"condition"));
                                });
                                return true;
                            },
                            ["arg"],
                            function (result) {
                                client.assert.equal(result.value, hasCondition, message);
                            }
                        );
                };

            rolesEdit
                .waitForElementPresent('@enableConditionSlider', 2000)
                .click('@enableConditionSlider')
                .waitForElementVisible('@changesPending', 2000)
                .expect.element('@changesPending').text.to.equal('- Condition');

            checkCondition(true, "Role has the condition property before disabling");

            rolesEdit
                .click('@saveButton')
                .waitForElementPresent('@alertMessage', 2000)
                .waitForElementVisible('@alertMessage', 2000)
                .expect.element('@alertMessage').text.to.equal("Successfully updated Role");

            rolesEdit
                .waitForElementNotPresent('@alertMessage', 2000);

            checkCondition(false, "Role does not have the condition property after disabling...successfully removed condition from role!");
        },
        'Temporal Constraint is set and changes pending shows Temporal Constraint as pending': function (client) {
            var rolesEdit = client.page.rolesEdit();

            rolesEdit
                .waitForElementPresent('@enableRoleTemporalConstraintSlider', 2000)
                .click('@enableRoleTemporalConstraintSlider')
                .setValue('@roleTemporalConstraintStartDate', "04/25/2016 12:00 AM")
                .setValue('@roleTemporalConstraintEndDate', "04/30/2016 3:00 PM");

            rolesEdit
                .waitForElementVisible('@changesPending', 2000)
                .expect.element('@changesPending').text.to.equal('- Temporal Constraints');
        },
        'Temporal Constraint is properly saved': function (client) {
            var rolesList = client.page.rolesList(),
                rolesEdit = client.page.rolesEdit();

            rolesEdit
                .waitForElementNotPresent('@alertMessage', 2000)
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
                .waitForElementVisible('@roleTemporalConstraintStartDate', 2000)
                .assert.value('@roleTemporalConstraintStartDate','04/25/2016 12:00 AM', 'After saving Role1 going to the grid and coming back the temporal constraints start date is the correct value')
                .assert.value('@roleTemporalConstraintEndDate','04/30/2016 3:00 PM', 'temporal constraints end date is the correct value');
        },
        'Add role add role member with temporal constraint' : function (client) {
            var rolesEdit = client.page.rolesEdit();

            rolesEdit
                .click('@roleMembersHeader')
                .waitForElementPresent('@addRoleMembersButton', 2000)
                .click('@addRoleMembersButton');

            client.pause(1000);

            rolesEdit
                .waitForElementPresent('@resourceCollectionSearchDialog', 2000)
                .waitForElementPresent('@resourceCollectionSearchDialogDropdown', 2000)
                .waitForElementVisible('@resourceCollectionSearchDialogDropdown', 3000)
                /*the 'du' below is the first two letters of 'dummyUser'
                * typing this into the search field makes the dummyUser value visible
                * to selenium hence it can be selected
                */
                .setValue('@resourceCollectionSearchDialogDropdown','du')
                .waitForElementVisible('@roleMemberSelection', 2000)
                .click('@roleMemberSelection')
                .waitForElementPresent('@enableGrantTemporalConstraintSlider', 2000)
                .click('@enableGrantTemporalConstraintSlider')
                .setValue('@roleMembersTemporalConstraintStartDate', "04/25/2016 12:00 AM")
                .setValue('@roleMembersTemporalConstraintEndDate', "04/30/2016 3:00 PM")
                .click('@resourceCollectionSearchDialogSaveBtn');
        },
        'Role member is added and saved properly' : function (client) {
                var rolesEdit = client.page.rolesEdit();

                rolesEdit
                    .waitForElementPresent('@firstMembersGridRowNameCell', 2000)
                    .click('@firstMembersGridRowNameCell')
                    .waitForElementPresent('@resourceCollectionSearchDialog', 2000)
                    .waitForElementVisible('@roleMembersTemporalConstraintStartDate', 2000)
                    .assert.value('@roleMembersTemporalConstraintStartDate','04/25/2016 12:00 AM', 'grant temporal constraints start date is the correct value')
                    .assert.value('@roleMembersTemporalConstraintEndDate','04/30/2016 3:00 PM', 'grant temporal constraints end date is the correct value')
                    .click('@resourceCollectionSearchDialogCloseBtn');

        },
        'Remove role member' : function (client) {
            var rolesEdit = client.page.rolesEdit();

            rolesEdit
                .waitForElementPresent('@membersListSelectAll', 2000);
                //.click('@membersListSelectAll') does not fire the change event on the checkbox
            client
                .execute(function(data) {
                    var selectAll = $('#relationshipArray-members th input[type=checkbox]'),
                        removeButton = $('#relationshipArray-members .remove-relationships-btn');

                    selectAll.click();
                    removeButton.click();
                    return true;
                }, [{}]);

            rolesEdit
                //.click('@removeRoleMembersButton') does not work moved to above .execute func
                .waitForElementPresent('@confirmationOkButton', 2000)
                .waitForElementVisible('@confirmationOkButton', 2000)
                .click('@confirmationOkButton')
                .waitForElementNotPresent('@firstMembersGridRowNameCell', 2000);

        },
        'Remove Role1': function (client) {
            var rolesList = client.page.rolesList(),
                rolesEdit = client.page.rolesEdit();

            //without this pause the deleteButton cannot be clicked
            client.pause(500);

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
