module.exports = {
        'Add managed object': function (client) {
            //must login first at the beginning of a session
            client.globals.login.helpers.login(client);
            
            client
                .waitForElementPresent('a[title=Configure]', 2000)
                .click('a[title=Configure]')
                .waitForElementVisible('a[title^="Managed Objects"]', 2000)
                .click('a[title^="Managed Objects"]')
                .waitForElementPresent('a[href^="#managed/add/"]', 2000)
                .click('a[href^="#managed/add/"]')
                .waitForElementPresent('#managedObjectName', 2000)
                .setValue('#managedObjectName', 'device')
                .setValue('#managedObjectIcon', 'fa-android')
                .click('#addManagedObject')
                .waitForElementVisible('div[role=alert]', 2000)
                .expect.element('div[role=alert]').text.to.equal("Managed object has been saved.");
            
            client.waitForElementNotVisible('div[role=alert]', 5500);
        },
        
        'Delete managed object': function (client) {
            client
                .waitForElementPresent('button.btn-actions[data-toggle=dropdown]', 2000)
                .click('button.btn-actions[data-toggle=dropdown]')
                .waitForElementPresent('#deleteManaged', 2000)
                .waitForElementVisible('#deleteManaged', 2000)
                .click('#deleteManaged')
                .waitForElementPresent('button.btn.btn-danger', 2000)
                .expect.element('button.btn.btn-danger').text.to.equal("Ok");
            
            client
                .click('button.btn.btn-danger')
                .waitForElementVisible('div[role=alert]', 2000)
                .expect.element('div[role=alert]').text.to.equal("Managed object successfully deleted.");
            
            client.end();
        }
};