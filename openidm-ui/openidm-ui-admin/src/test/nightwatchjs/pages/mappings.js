module.exports = {
    url: function() {
        return this.api.globals.baseUrl + "#dashboard/";
    },
    sections: {
        navigation: {
            selector: '#mainNavBar',
            elements: {
                mainNavigationButton: {
                    selector: 'a[title=Configure]'
                },
                mainNavigationMenuItem: {
                    selector: 'a[title^="Mappings"]'
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
        },
        mappingListDeleteDialog: {
            selector: '.bootstrap-dialog',
            elements: {
                confirmButton: {
                    selector: 'button.btn.btn-danger'
                }
            }
        },
        mappingList: {
            selector: '#content',
            elements: {
                addNewMapping: {
                    selector: '#addMapping'
                },
                noMappings: {
                    selector: '#noMappingsDefined'
                },
                mappingGridHolder: {
                    selector: "a[href='#mappingConfigGridHolder']"
                },
                mappingGrid: {
                    selector: '#mappingGrid'
                },
                mappingGridItem: {
                    selector: 'tr[data-mapping-title=managedAssignment_managedRole]'
                },
                mappingListItem: {
                    selector: '.wide-card'
                },
                filterInput: {
                    selector: '.filter-input'
                },
                mappingConfigHolder: {
                    selector: "a[href='#mappingConfigHolder']"
                },
                deleteMapping: {
                    selector: '.wide-card .delete-button'
                },
                listTableLink : {
                    selector: '.table-clink'
                }
            }
        },
        addMapping: {
            selector: '#content',
            elements: {
                mainAddBody: {
                    selector: '.addMapping'
                },
                addMappingResource: {
                    selector: '#mappingSource'
                },
                assignmentAddResource: {
                    selector: '[data-managed-title=assignment] .add-resource-button'
                },
                roleAddResource: {
                    selector: '[data-managed-title=role] .add-resource-button'
                },
                createMapping: {
                    selector: '#createMapping'
                }
            }
        },
        createMappingDialog: {
            selector: '.bootstrap-dialog',
            elements: {
                saveMapping: {
                    selector: '#mappingSaveOkay'
                }
            }
        },
        mappingBase: {
            selector: '#content',
            elements: {
                syncNowButton: {
                    selector: '#syncNowButton'
                },
                syncStatus: {
                    selector: '#syncStatus'
                },
                syncDetails: {
                    selector: '.sync-results'
                },
                syncSuccess: {
                    selector: '.success-display'
                },
                headerLink: {
                    selector: '.header-link-text'
                }
            }
        },
        attributesGridPanel: {
            selector: '#attributesGridPanel',
            elements: {
                addProperty: {
                    selector: '.add-property'
                },
                updateMappingButton: {
                    selector: '#updateMappingButton'
                },
                dragHandle: {
                    selector: '.dragToSort'
                },
                sampleUserSearch: {
                    selector: 'input[placeholder="Search to see preview"]'
                }
            }
        },
        addAttributesDialog: {
            selector: '.bootstrap-dialog',
            elements: {
                addPropertySelect: {
                    selector: '#addPropertySelect'
                },
                updatePropertyButton: {
                    selector: "#scriptDialogUpdate"
                },
                mappingDialogTabs: {
                    selector: "#mappingDialogTabs"
                }
            }
        }
    }
};