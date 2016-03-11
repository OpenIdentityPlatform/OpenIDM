module.exports = {
    url: function() {
        return this.api.globals.baseUrl + "#managed/";
    },
    sections: {
        toolbar: {
            selector: ".toolbar",
            elements: {
                addNewManagedObjectButton: {
                    selector: "a[href^='#managed/add/']"
                }
            }
        },
        navigation: {
            selector: "#menu",
            elements: {
                configureDropDownLink: {
                    selector: "a[title=Configure]"
                },
                managedObjectsLink: {
                    selector: "a[title^='Managed Objects']"
                }
            }
        }
    }
};
