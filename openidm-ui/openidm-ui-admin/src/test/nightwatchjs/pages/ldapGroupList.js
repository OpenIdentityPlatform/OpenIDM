module.exports = {
    url: function() {
        return this.api.globals.baseUrl + '#resource/system/ldap/group/list/';
    },
    elements: {
        tableGrid: {
            selector: '#systemViewTable'
        },
        reloadButton: {
            selector: '#reloadGridBtn'
        },
        tableRow: {
            selector: 'tbody tr'
        }
    }
};
