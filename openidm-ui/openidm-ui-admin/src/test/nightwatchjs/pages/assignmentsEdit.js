module.exports = {
    elements: {
        nameInput: {
            selector: 'input[name="name"]'
        },
        descriptionInput: {
            selector: '#assignmentDescription'
        },
        addButton: {
            selector: '#addAssignment'
        },
        saveButton: {
            selector: '#saveBtn'
        },
        deleteButton: {
            selector: '#deleteAssignment'
        },
        confirmationOkButton: {
            selector: '.bootstrap-dialog-footer-buttons button.btn-danger'
        },
        alertMessage: {
            selector: 'div[role=alert]'
        },
        attributesTabHeader: {
            selector: 'a[href="#assignmentAttributes"]'
        },
        addAttributeButton: {
            selector: '#addAttribute'
        },
        assignmentOperationPopover: {
            selector: '.btn-toggle-attribute-operations'
        },
        onAssignmentSelect: {
            selector: '.onAssignment-select'
        },
        onAssignmentSelectOption: {
            selector: '.onAssignment-select option[value="noOp"]'
        },
        attributeSelect: {
            selector: ".select-attribute"
        },
        attributeSelectManagerOption: {
            selector: '.select-attribute option[value="manager"]'
        },
        attributeJSONEditorRoot: {
            selector: ".attribute-value > div"
        }
    }
};
