module.exports = {
    url: function() {
        return this.api.globals.baseUrl + "#managed/add/";
    },
    elements: {
        globalAlert: {
            selector: ".alert-system"
        }
    },
    sections: {
        details: {
            selector: "#addManagedObjectForm",
            elements: {
                nameInput: {
                    selector: "#managedObjectName"
                },
                iconInput: {
                    selector: "#managedObjectIcon"
                },
                addButton: {
                    selector: "#addManagedObject"
                }
            }
        }
    }
};
