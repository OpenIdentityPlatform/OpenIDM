module.exports = {
    url: function() {
        return this.api.globals.baseUrl + "#managed/edit/";
    },
    elements: {
        globalAlert: {
            selector: ".alert-system span"
        },
        save: {
            selector: "#saveManagedSchema"
        }
    },
    sections: {
        toolbar: {
            selector: ".page-header",
            elements: {
                moreOptionsToggle: {
                    selector: "button.btn-actions[data-toggle=dropdown]"
                },
                deleteButton: {
                    selector: "#deleteManaged"
                }
            }
        },
        confirmDialog: {
            selector: ".modal-content",
            elements: {
                okayButton: {
                    selector: "button.btn.btn-danger"
                }
            }
        },
        tabs: {
            selector: ".tab-menu",
            elements: {
                schemaTab: {
                    selector: ".tab-menu ul li:nth-child(4)"
                },
                propertiesTab: {
                    selector: ".tab-menu ul li:nth-child(6)"
                },
                addButton: {
                    selector: "#addManagedObject"
                }
            }
        },
        schemaTab: {
            selector: "#managedSchema",
            elements: {
                propertiesButton: {
                    selector: "button[title='Add Property']"
                },
                propertyNameInput: {
                    selector: "input[name='root[properties][0][propertyName]']"
                },
                deleteProperty: {
                    selector: "button[title='Delete Property']"
                }
            }
        }
    }
};
