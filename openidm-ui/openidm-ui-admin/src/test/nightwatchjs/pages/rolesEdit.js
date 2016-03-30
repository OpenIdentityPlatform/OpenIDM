module.exports = {
    elements: {
        spinner: {
          selector: ".spinner"
        },
        nameInput: {
            selector: 'input[name="root[name]"]'
        },
        descriptionInput: {
            selector: 'input[name="root[description]"]'
        },
        saveButton: {
            selector: '#saveBtn'
        },
        resetButton: {
            selector: "#resetBtn"
        },
        deleteButton: {
            selector: '#deleteBtn'
        },
        confirmationOkButton: {
            selector: '.bootstrap-dialog-footer-buttons button.btn-danger'
        },
        alertMessage: {
            selector: 'div[role=alert]'
        },
        conditionTabHeader: {
            selector: 'a[href="#condition"]'
        },
        enableConditionSlider: {
            selector: '#enableDynamicRoleGrantCheckbox'
        },
        expressionTree: {
            selector: '.expressionTree'
        },
        conditionStatus: {
            selector: '#tabHeader_condition span i.fa-toggle-on'
        },
        conditionOpSelect: {
            selector: '.expressionTree select:first-of-type'
        },
        conditionOpValueForExpr: {
            selector: '.expressionTree select:first-of-type option[value=expr]'
        },
        conditionOpValueForAnd: {
            selector: '.expressionTree select:first-of-type option[value=and]'
        },
        conditionOpNameInputFirst: {
            selector: '.expressionTree .name-body:first-of-type .selectize-input'
        },
        conditionOpNameInputSecond: {
            selector: '.expressionTree ul.subgroup li:nth-of-type(2) .selectize-input'
        },
        conditionOpValueInput: {
            selector: '.expressionTree input.value'
        },
        conditionQueryText: {
            selector: '#conditionFilterHolder div.filter.well'
        },
        changesPending: {
            selector: '#resourceChangesPending #changedFields'
        },
        conditionOpNameInputSelectOption: {
            selector: '.selectize-dropdown-content div[data-value="/mail"].option'
        },
        enableTemporalConstraintSlider: {
            selector: '#enableTemporalConstraintsCheckbox'
        },
        temporalConstraintStartDate: {
            selector: '.temporalConstraintStartDate'
        },
        temporalConstraintEndDate: {
            selector: '.temporalConstraintEndDate'
        }
    }
};
