module.exports = {
    url: function() {
        return this.api.globals.baseUrl + '#resource/managed/role/list/';
    },
    elements: {
        newRoleButton: {
            selector: 'a[title="New Role"] button'
        },
        alertMessage: {
            selector: 'div[role=alert]'
        },
        emptyListMessage: {
            selector: '#managedViewTable tr.empty td'
        },
        grid: {
            selector: '#managedViewTable'
        },
        firstGridRowNameCell: {
            selector: '#managedViewTable tbody tr:nth-child(1) td:nth-child(2)'
        }
    }
};
