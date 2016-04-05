module.exports = {
    url: function() {
        return this.api.globals.baseUrl + "#mapping/";
    },
    elements: {
        href: {
            selector: "#mapping"
        },
        mappingCard: {
            selector: '#mappingCard'
        },
        attributesGrid: {
            selector: "#attributesGridHolder"
        },
        attributesGridRows: {
            selector: "#attributesGridHolder table tbody > tr"
        },
        attributesGridFirstRow: {
            selector: "#attributesGridHolder table tbody > tr:first-child"
        },
        firstRowSourceText: {
            selector: "#attributesGridHolder table tbody > tr:first-child > td:first-child .text-muted"
        },
        firstRowTargetText: {
            selector: "#attributesGridHolder table tbody > tr:first-child > td:nth-child(3) .text-muted"
        },
        firstRowIconBadge: {
            selector: "#attributesGridHolder table tbody > tr:first-child span.properties-badge"
        },
        scriptDialogUpdate: {
            selector: "#scriptDialogUpdate"
        },
        defaultValuesTab: {
            selector: 'a[href="#Default_Values"]'
        },
        transformationScriptTab: {
            selector: 'a[href="#Transformation_Script"]'
        },
        transformationScriptTextArea: {
            selector: "#transformationScriptHolder .CodeMirror-code pre"
        },
        conditionScriptTab: {
            selector: 'a[href="#Condition_Script"]'
        },
        conditionScriptEditor: {
            selector: "#Condition_Script"
        },
        conditionFilter: {
            selector: "#conditionalFilter"
        },
        conditionFilterSelect: {
            selector: "#conditionFilterHolder select"
        },
        conditionFilterSelectOption: {
            selector: "#conditionFilterHolder option[value='and']"
        },
        conditionFilterValueForProperty: {
            selector: "#conditionFilterHolder .expressionTree .row:first-of-type .selectize-input"
        },
        conditionFilterValueForPropertyErroneous: {
            selector: "#conditionFilterHolder .expressionTree .row:first-of-type select:nth-of-type(2)"
        },
        conditionFilterValueForPropertyOption: {
            selector: "#conditionFilterHolder .expressionTree .row:first-of-type div[data-value='/linkQualifier']"
        },
        gridPreviewInput: {
            selector: 'input[placeholder="Search to see preview"]'
        },
        previewPopUpValue: {
            selector: 'div[data-value="bjensen@example.com"]'
        },
        policyDialogSubmit: "#submitPolicyDialog",
        confirmPolicyPatternButton: ".bootstrap-dialog-footer-buttons > .btn-primary",
        modal: ".modal-body",
        reconQueryFilterPanelToggle: 'a[href="#reconQueriesBase"]'
    },
    sections: {
        modalBody: {
            selector: ".modal-body",
            elements: {
                restrictToTab: "#restrictToTab",
                actionTab: "#actionTab",
                actionCompleteTab: "#actionCompleteTab",
                defaultActionPane: "#defaultActionPane",
                defaultActionPaneSelect: "#defaultActionPane select",
                actionCompletePre: "#actionComplete pre",
                conditionFilterPaneSelect: '#conditionFilterPane select'
            }
        },
        behaviorsTab: {
            selector: '#policyPatternBody',
            elements: {
                tabLink: {
                    selector: 'a[href="#mappingContent"]'
                },
                policiesTable: "#situationalPolicies table",
                policiesTableRows: '#situationalPolicies table > tbody > tr',
                policiesTableFirstRow: "#situationalPolicies table tbody > tr:first-child",
                policiesTableFirstRowActionColumn: "#situationalPolicies table tbody > tr:first-child > td:nth-child(4)",
                policiesTableFirstRowOnCompleteColumn: "#situationalPolicies table tbody > tr:first-child > td:nth-child(5)",
                policiesTableFirstRowConditionColumn: "#situationalPolicies table tbody > tr:first-child > td:nth-child(3)",
                addPolicyButton: "button.add-policy",
                deletePolicyButton: "#situationalPolicies table tbody > tr:first-child button.delete-policy",
                policyPatterns: "#policyPatterns",
                readOnlyPolicyPattern: 'option[value="Read-only"]',
                editPolicyButton: "#situationalPolicies table tbody > tr:first-child button.edit-policy",
                reset: "input.reset",
                save: "input.savePolicy"
            }
        },
        targetQuery: {
            selector: "#targetQuery",
            elements: {
                select: "select",
                firstInputName: 'li[index="0"] input.name',
                firstInputValue: 'li[index="0"] input.value',
                secondInputName: 'li[index="1"] input.name',
                secondInputValue: 'li[index="1"] input.value',
                filter: '.filter'
            }
        }
    }
};
