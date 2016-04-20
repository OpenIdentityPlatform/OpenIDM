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
        },
        versionsView: '#versionsView',
        historyView: '#historyView',
        historyViewToggle: 'a[href="#updateHistoryBody"]',
        historyGridUserName: '#historyGrid table td:first-child div.version-info small:nth-child(2)',
        reportButton: '#historyView table td:nth-child(2) button',
        reportView: '#installationReportView',
        successAlert: 'div.alert-success',
        treeGridNode: '.treegrid > .node',
        reportBackButton: '#installationReportView button.back',
        panelDescription: 'p.panel-description small:first-child'
    }
};
