module.exports = {
    url: function() {
        return this.api.globals.baseUrl + "#selfservice/passwordreset/";
    },
    commands: [{
        toggleSlider: function () {
            this
                .waitForElementPresent("@mainEnableSlider", 2000)
                .click("@mainEnableSlider")
                .api.pause(1000);
            return this;
        },
        loadPage: function () {
            this
                .navigate()
                .waitForElementPresent("@mainEnableSlider", 2000)
                .api.pause(1000);
            return this;
        }
    }],
    elements: {
        mainEnableSlider: ".toggle-header .checkbox-slider",
        resourceDropdown: "#identityServiceUrl",

        reCaptchaStep: ".self-service-card[data-type='captcha']",
        reCaptchaStepToggle: ".self-service-card[data-type='captcha'] .checkbox-slider",
        reCaptchaStepToggleEdit: ".self-service-card[data-type='captcha'] .fa-pencil",

        userQueryFormStep: ".self-service-card[data-type='userQuery']",
        userQueryFormStepToggle: ".self-service-card[data-type='userQuery'] .checkbox-slider",
        userQueryFormStepEdit: ".self-service-card[data-type='userQuery'] .fa-pencil",

        emailValidationStep: ".self-service-card[data-type='emailValidation']",
        emailValidationStepToggle: ".self-service-card[data-type='emailValidation'] .checkbox-slider",
        emailValidationStepEdit: ".self-service-card[data-type='emailValidation'] .fa-pencil",

        KBAStep: ".self-service-card[data-type='kbaSecurityAnswerVerificationStage']",
        KBAStepToggle: ".self-service-card[data-type='kbaSecurityAnswerVerificationStage'] .checkbox-slider",
        KBAStepEdit: ".self-service-card[data-type='kbaSecurityAnswerVerificationStage'] .fa-pencil",

        passwordResetFormStep: ".self-service-card[data-type='resetStage']",
        passwordResetFormStepToggle: ".self-service-card[data-type='resetStage'] .checkbox-slider",
        passwordResetFormStepEdit: ".self-service-card[data-type='resetStage'] .fa-pencil",

        stepEditDialog: "#configDialogForm",
        idSelect: "#select-identityIdField",
        idInput: "#select-identityIdField +div .selectize-input",
        idInputChildren: "#select-identityIdField +div .selectize-dropdown-content div",
        dialogClose: ".btn-default"
    }
};
