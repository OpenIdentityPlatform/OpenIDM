module.exports = {
    elements: {
        nameInput: {
            selector: 'input[name="root[name]"]'
        },
        descriptionInput: {
            selector: 'input[name="root[description]"]'
        },
        saveButton: {
            selector: '#saveBtn'
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
            selector: '#tabHeader_condition span i.fa-check'
        },
        conditionOpSelect: {
            selector: '.expressionTree select:first-of-type'
        },
        conditionOpValueForOption: {
            selector: '.expressionTree select:first-of-type option[value=expr]'
        },
        conditionOpNameInput: {
            selector: '.expressionTree input.name'
        },
        conditionOpValueInput: {
            selector: '.expressionTree input.value'
        },
        conditionQueryText: {
            selector: '#conditionFilterHolder div.filter.well'
        },
        changesPending: {
            selector: '#resourceChangesPending #changedFields'
        }
    }
};
