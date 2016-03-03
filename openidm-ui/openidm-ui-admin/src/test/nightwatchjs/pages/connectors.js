module.exports = {
    url: function() {
        return this.api.globals.baseUrl + '#connectors/';
    },
    elements: {
        ldapCard: {
            selector: 'div[data-connector-title="provisioner.openicf_ldap"]'
        },
        deleteLDAP: {
            selector: 'button#ldapConnectorToggle ~ ul li.connector-delete span'
        },
        confirmDelete: {
            selector: 'button.btn-danger'
        },
        ellipsis: {
            selector: '#ldapConnectorToggle'
        },
        alertMessage: {
            selector: '#messages .alert-message'
        }
    }
};
