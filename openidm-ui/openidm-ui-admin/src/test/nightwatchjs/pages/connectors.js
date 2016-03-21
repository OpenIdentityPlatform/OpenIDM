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
            selector: '#content',
            elements: {
                addCard: {
                    selector: '.add-card'
                },
                connectorCard: {
                    selector: '.card-container'
                },
                cards: {
                    selector: '.card-body'
                },
                connectorListToggle: {
                    selector: '#connectorCardContainer .dropdown-toggle'
                },
                connectorGridToggle: {
                    selector: '#connectorGridContainer .dropdown-toggle'
                },
                connectorDelete: {
                    selector: '.connector-delete'
                },
                connectorDropdown: {
                    selector: '.dropdown-menu'
                },
                toggleCardViewButton : {
                    selector: 'a[href="#connectorCardContainer"]'
                },
                toggleGridViewButton : {
                    selector: 'a[href="#connectorGridContainer"]'
                },
                gridView: {
                    selector: '#connectorGrid'
                },
                connectorListFilter: {
                    selector: '.filter-input'
                },
                backToConnector: {
                    selector: 'a[href="#connectors/"]'
                },
                addToMapping: {
                    selector: 'a[href="#mapping/add/connector/testtest"]'
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
