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
        }
    }
};
