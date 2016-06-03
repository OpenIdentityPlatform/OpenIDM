module.exports = {
    url: function() {
        return this.api.globals.baseUrl + "#profile/password";
    },
    elements: {
        newPassword: "#input-password",
        confirmPassword: "#input-confirmPassword",
        currentPassword: "#confirmPasswordForm #currentPassword",
        confirmPasswordButton: ".modal-content #btnUpdate",
        saveButton: "#password input[type=submit]"
    }
};
