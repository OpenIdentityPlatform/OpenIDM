module.exports = {
    url: function() {
        return this.api.globals.baseUrl + "#settings/update/";
    },
    elements: {
        errorAlert: {
            selector: "#updateInstallError"
        },
        infoAlert: {
            selector: "div.alert.alert-info"
        },
        versionGrid: {
            selector: "#versionGrid"
        },
        refresh: {
            selector: ".checkUpdatesAvailable"
        },
        buttons: {
            selector: "button.test"
        }
    }
}
