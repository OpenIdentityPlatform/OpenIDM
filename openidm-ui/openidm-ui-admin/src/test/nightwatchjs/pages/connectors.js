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
    },
    sections: {
        connectorList: {
            selector: '#connectorCardContainer',
            elements: {
                addCard: {
                    selector: '.add-card'
                },
                connectorCard: {
                    selector: '.card-container'
                },
                connectorToggle: {
                    selector: '.dropdown-toggle'
                },
                connectorDelete: {
                    selector: '.connector-delete'
                },
                connectorDropdown: {
                    selector: '.dropdown-menu'
                }
            }
        },
        connectorModal: {
            selector: 'body',
            elements: {
                connectorDeleteDialog: {
                    selector: '.modal-body'
                },
                connectorDeleteDialogOkay: {
                    selector: '.btn-danger'
                }
            }
        },
        message: {
            selector: '#messages',
            elements: {
                displayMessage: {
                    selector: 'div[role=alert]'
                }
            }
        }
    }
};
