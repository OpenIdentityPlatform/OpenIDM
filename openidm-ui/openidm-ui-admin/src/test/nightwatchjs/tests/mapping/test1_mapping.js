module.exports = {
        'Add mapping': function (client) {
            //must login first at the beginning of a session
            client.globals.login.helpers.login(client);
            
            client
                .waitForElementPresent('a[title=Configure]', 2000)
                .click('a[title=Configure]')
                .waitForElementVisible('a[title^="Mappings"]', 2000)
                .click('a[title^="Mappings"]')
                .waitForElementPresent('#noMappingsDefined', 2000)
                .waitForElementVisible('#noMappingsDefined', 2000)
                .waitForElementPresent('#addMapping', 2000)
                .click('#addMapping')
                .waitForElementPresent('#mappingSource', 2000)
                .click('[data-managed-title=assignment] .add-resource-button')
                .click('[data-managed-title=role] .add-resource-button')
                .click('#createMapping')
                .waitForElementPresent('#mappingSaveOkay', 2000)
                .click('#mappingSaveOkay')
                .waitForElementVisible('div[role=alert]', 2000)
                .expect.element('div[role=alert]').text.to.equal("Mapping successfully updated");
            
            client.waitForElementNotVisible('div[role=alert]', 5500);
        },
        'Check grid and filter': function (client) {
            client
                .click(".header-link-text")
                .waitForElementPresent("a[href='#mappingConfigGridHolder']", 2000)
                .click("a[href='#mappingConfigGridHolder']")
                .waitForElementVisible('#mappingGrid', 2000)
                .waitForElementVisible('tr[data-mapping-title=managedAssignment_managedRole]', 2000)
                .setValue('.filter-input', 'xx')
                .waitForElementNotVisible('tr[data-mapping-title=managedAssignment_managedRole]', 2000)
                .clearValue('.filter-input')
                .setValue('.filter-input','ma')
                .waitForElementVisible('tr[data-mapping-title=managedAssignment_managedRole]', 2000)
                .click("a[href='#mappingConfigHolder']")
                .waitForElementVisible('div[mapping=managedAssignment_managedRole]', 2000);
        },
        'Delete Mapping': function (client) {
            client
                .click("div[mapping=managedAssignment_managedRole] .delete-button")
                .waitForElementPresent('button.btn.btn-danger', 2000)
                .expect.element('button.btn.btn-danger').text.to.equal("Ok");
            
            client
                .click('button.btn.btn-danger')
                .waitForElementVisible('div[role=alert]', 2000)
                .expect.element('div[role=alert]').text.to.equal("Mapping successfully deleted");
            
            
            client.end();
        }
};