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
        enableConditionSlider: {
            selector: '#enableDynamicRoleGrantCheckbox'
        },
        expressionTree: {
            selector: '.expressionTree'
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
        enableRoleTemporalConstraintSlider: {
            selector: '#temporalContstraintsFormContainer .enableTemporalConstraintsCheckbox'
        },
        roleTemporalConstraintStartDate: {
            selector: '#temporalContstraintsFormContainer .temporalConstraintStartDate'
        },
        roleTemporalConstraintEndDate: {
            selector: '#temporalContstraintsFormContainer .temporalConstraintEndDate'
        },
        roleMembersHeader: {
            selector: 'a[href="#resource-members"]'
        },
        addRoleMembersButton: {
            selector: '#relationshipArray-members .add-relationship-btn'
        },
        removeRoleMembersButton: {
            selector: '#relationshipArray-members .remove-relationships-btn'
        },
        membersListSelectAll: {
            selector: '#relationshipArray-members th input[type=checkbox]'
        },
        resourceCollectionSearchDialog: {
            selector: '#resourceCollectionSearchDialog'
        },
        resourceCollectionSearchDialogDropdown : {
            selector: '#resourceCollectionSearchDialog .selectize-control input[placeholder]'
        },
        resourceCollectionSearchDialogSaveBtn : {
            selector: '#resourceCollectionSearchDialogSaveBtn'
        },
        resourceCollectionSearchDialogCloseBtn : {
            selector: '#resourceCollectionSearchDialogCloseBtn'
        },
        roleMemberSelection : {
            selector: '#resourceCollectionSearchDialog .selectize-dropdown-content div[data-value=dummyUser]'
        },
        enableGrantTemporalConstraintSlider: {
            selector: '#membersTemporalContstraintsFormContainer .enableTemporalConstraintsCheckbox'
        },
        roleMembersTemporalConstraintStartDate: {
            selector: '#membersTemporalContstraintsFormContainer .temporalConstraintStartDate'
        },
        roleMembersTemporalConstraintEndDate: {
            selector: '#membersTemporalContstraintsFormContainer .temporalConstraintEndDate'
        },
        membersGrid: {
            selector: '#relationshipArray-members'
        },
        firstMembersGridRowNameCell: {
            selector: '#relationshipArray-members tbody tr:nth-child(1) td:nth-child(2)'
        }
    }
};
