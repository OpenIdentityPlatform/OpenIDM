module.exports = {
    url: function() {
        return this.api.globals.baseUrl + "#dashboard/0";
    },
    elements: {
        addConnector: {
            selector: 'a[href="#connectors/add/"]'
        }
    },
    sections: {
        navigation: {
            selector: "#menu",
            elements: {
                dashboardDropdown: {
                    selector: ".active .dropdown-toggle"
                },
                //        firstDashboardLink: {
                //            selector: ""
                //        },
                firstDashboardText: {
                    selector: ".active .dropdown-menu li:first-child a"
                },
                secondDashboardLink: {
                    selector: ".active .dropdown-menu li:nth-child(2)"
                },
                secondDashboardText: {
                    selector: ".active .dropdown-menu li:nth-child(2) a"
                },
                //        thirdDashboardLink: {
                //            selector: ""
                //        },
                //        thirdDashboardText: {
                //            selector: ""
                //        },
                newDashboardLink: {
                    selector: ".active .dropdown-menu li:last-child"
                },
                configureDropdown: {
                    selector: ".navbar-admin.navbar-nav>li:nth-child(2)"
                },
                mappingLink: {
                    selector: "a[title=Mappings]"
                },
                logoLink: {
                    selector: "#navbarBrand"
                }
            }
        },
        dashboardHeader: {
            selector: ".page-header",
            elements: {
                title: {
                    selector: "h1"
                },
                addWidgetsButton: {
                    selector: ".open-add-widget-dialog"
                },
                optionsToggle: {
                    selector: ".page-header-button-group .dropdown-toggle"
                },
                renameButton: {
                    selector: "#RenameDashboard"
                },
                duplicateButton: {
                    selector: "#DuplicateDashboard"
                },
                defaultButton: {
                    selector: "#DefaultDashboard"
                },
                deleteButton: {
                    selector: "#DeleteDashboard"
                }
            }
        },
        dashboardBody: {
            selector: "#content",
            elements: {
                widgetContainer: {
                    selector: "#dashboardWidgets"
                },
                noWidgetWellButton: {
                    selector: ".well .open-add-widget-dialog"
                },
                firstWidgetTitle: {
                    selector: ".widget-holder:nth-child(1) .pull-left.widget-title"
                },
                firstWidgetOptionsToggle: {
                    selector: ".widget-holder:nth-child(1) .widget-section-title .dropdown-toggle"
                },
                firstWidgetMenu: {
                    selector: ".widget-holder:nth-child(1) .dropdown-menu"
                },
                firstWidgetSettings: {
                    selector: ".widget-holder:nth-child(1) .widget-settings"
                },
                secondWidgetTitle: {
                    selector: ".widget-holder:nth-child(2) .pull-left.widget-title"
                },
                secondWidgetOptionsToggle: {
                    selector: ".widget-holder:nth-child(2) .widget-section-title .dropdown-toggle"
                },
                secondWidgetMenu: {
                    selector: ".widget-holder:nth-child(2) .dropdown-menu"
                },
                secondWidgetSettings: {
                    selector: ".widget-holder:nth-child(2) .widget-settings"
                },
                secondWidgetDelete: {
                    selector: ".widget-holder:nth-child(2) .widget-delete"
                },
                thirdWidgetOptionsToggle: {
                    selector: ".widget-holder:nth-child(3) .widget-section-title .dropdown-toggle"
                },
                thirdWidgetMenu: {
                    selector: ".widget-holder:nth-child(3) .dropdown-menu"
                },
                thirdWidgetSettings: {
                    selector: ".widget-holder:nth-child(3) .widget-settings"
                },
                quickLinksWidgetCardContainer: {
                    selector: ".card-container"
                },
                quickLinksCard: {
                    selector: ".card .circle i"
                },
                frame: {
                    selector: "iframe"
                }
            }
        },
        widgetDeleteWindow: {
            selector: ".bootstrap-dialog.type-danger",
            elements: {
                confirm: {
                    selector: ".btn.btn-danger"
                }
            }
        },
        renameDashboardWindow: {
            selector: ".modal-dialog",
            elements: {
                body: {
                    selector: "#RenameDashboardDialog"
                },
                name: {
                    selector: "#DashboardName"
                },
                submit: {
                    selector: "#SaveNewName"
                }
            }
        },
        duplicateDashboardWindow: {
            selector: ".modal-dialog",
            elements: {
                body: {
                    selector: "#DuplicateDashboardDialog"
                },
                name: {
                    selector: "#DashboardName"
                },
                submit: {
                    selector: "#SaveNewName"
                }
            }
        },
        widgetSettingsWindow: {
            selector: ".modal-dialog",
            elements: {
                widgetSettingsBody: {
                    selector: "#widgetConfigForm"
                },
                widgetSizeSelect: {
                    selector: "#widgetSize"
                },
                frameWidgetTitleInput: {
                    selector: "input[name='title']"
                },
                frameWidgetURLInput: {
                    selector: "#frameUrl"
                },
                frameWidgetHeightInput: {
                    selector: "#frameHeight"
                },
                widgetSaveButton: {
                    selector: "#saveUserConfig"
                },
                reconWidgetType: {
                    selector: "#barchart"
                },
                addQuickLinkButton: {
                    selector: "#addQuickLink"
                },
                quickLinkContainer: {
                    selector: "#quickLinkAddHolder"
                },
                quickLinkNameInput: {
                    selector: "#quickLinkName"
                },
                quickLinkHrefInput: {
                    selector: "#quickLinkHref"
                },
                quickLinkIconInput: {
                    selector: "#quickLinkIcon"
                },
                quickLinkCreateButton: {
                    selector: "#createQuickLink"
                },
                quickLinkRow: {
                    selector: ".backgrid tbody tr"
                }
            }
        },
        addWidgetsWindow:{
            selector: ".modal-dialog",
            elements: {
                dialogBody: {
                    selector: "#AddWidgetDialog"
                },
                lifeCycleMemoryHeapAddButton: {
                    selector: "button[data-widget-id='lifeCycleMemoryHeap']"
                },
                lifeCycleMemoryNonHeapAddButton: {
                    selector: "button[data-widget-id='lifeCycleMemoryNonHeap']"
                },
                systemHealthFullAddButton: {
                    selector: "button[data-widget-id='systemHealthFull']"
                },
                cpuWidgetAddButton: {
                    selector: "button[data-widget-id='cpuUsage']"
                },
                reconWidgetAddButton: {
                    selector: "button[data-widget-id='lastRecon']"
                },
                resourcesWidgetAddButton: {
                    selector: "button[data-widget-id='resourceList']"
                },
                quickStartWidgetAddButton: {
                    selector: "button[data-widget-id='quickStart']"
                },
                frameWidgetAddButton: {
                    selector: "button[data-widget-id='frame']"
                },
                userRelationshipWidgetAddButton: {
                    selector: "button[data-widget-id='userRelationship']"
                },
                closeButton: {
                    selector: ".bootstrap-dialog-footer-buttons button"
                }
            }
        },
        newDashboard: {
            selector: "#content",
            elements: {
                title: {
                    selector: "h1"
                },
                form: {
                    selector: "#NewDashboardForm"
                },
                nameInput: {
                    selector: "#DashboardName"
                },
                setAsDefaultToggle: {
                    selector: ".checkbox-slider label"
                },
                createDashboardButton: {
                    selector: "input[type=submit]"
                }
            }
        }
    }
};
