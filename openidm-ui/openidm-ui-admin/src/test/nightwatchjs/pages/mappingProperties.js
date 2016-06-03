module.exports = {
    commands: [{
        loadPropertyMappingUrl: function(mappingName) {
            this.url = this.api.globals.baseUrl + "#properties/" + mappingName + "/";

            return this
                .navigate()
                .waitForElementPresent("@attributesGrid", 2000);
        },
        deleteFirstProperty: function () {
            this.click("@removeFirstPropertyButton")
                .waitForElementPresent("@confirmDialogButton", 100)
                .click("@confirmDialogButton")
                .api.pause(1000);
            return this;
        }
    }],
    elements: {
        attributesGrid: '#attributesGridHolder',
        removeFirstPropertyButton: '#attributesGridHolder table tbody tr:nth-child(1) td i.removeProperty',
        confirmDialogButton: '#frConfirmationDialog #frConfirmationDialogBtnOk',
        savePropertiesButton: '#updateMappingButton',
        pageTitle: "h1",
        pageType: ".page-type",
        syncMessage: "#syncMessage",
        syncNowButton: '#syncNowButton',
        syncStatus: ".managedAssignment_managedRole_syncStatus"
    }
};
