

module.exports = {

    url: function() {
        return this.api.globals.baseUrl + "#mapping/add/";
    },
    elements: {
        connector: {
            selector: 'div[data-resource-type="connector"]'
        },
        managedUserObj: {
            selector: 'div[data-managed-title="user"]'
        },
        createMappingButton: {
            selector: "#createMapping"
        },
        mappingSaveOkay: {
            selector: "#mappingSaveOkay"
        },
        mappingDialog: {
            selector: "#saveMappingDialogClone"
        },
        newMappingName: {
            selector: ".page-header > h1"
        }
    }
};
