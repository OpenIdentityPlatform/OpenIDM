module.exports = {
    url: function() {
        return this.api.globals.baseUrl + "#managed/";
    },
    elements: {
        cards: "#managedCardContainer > div.row > div",
        managedCardContainer: "#managedCardContainer",
        managedCardToggle: 'a[href="#managedCardContainer"]',
        cardDropdownButton: ".card button.dropdown-toggle",
        cardDropdownList: ".card ul.dropdown-menu",
        cardDeleteButton: ".card ul.dropdown-menu li.managed-delete",
        gridRows: '#managedGrid > table > tbody > tr',
        managedGridContainer: "#managedGridContainer",
        managedGridToggle: 'a[href="#managedGridContainer"]',
        gridDropdownButton: "#managedGridContainer table > tbody > tr:first-child button.dropdown-toggle",
        gridDropdownList: "#managedGridContainer ul.dropdown-menu",
        gridDeleteButton: "#managedGridContainer ul.dropdown-menu li.managed-delete"
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
