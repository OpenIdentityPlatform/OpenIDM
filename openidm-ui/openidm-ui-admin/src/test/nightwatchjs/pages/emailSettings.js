module.exports = {
    url: function() {
        return this.api.globals.baseUrl + "#settings/email/";
    },
    elements: {
        infoAlert: {
            selector: "div.alert.alert-info"
        },
        enableEmailToggle: {
            selector : '#emailToggle'
        },
        hostField: {
            selector : '#emailHost'
        },
        portField: {
            selector : '#emailPort'
        },
        saveButton: {
            selector : '#saveEmailConfig'
        },
        fromField: {
            selector: '#emailFrom'
        }
    }
};
