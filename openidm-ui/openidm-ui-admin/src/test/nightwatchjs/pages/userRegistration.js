module.exports = {
    url: function() {
        return this.api.globals.baseUrl + "#selfservice/userregistration/";
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
                .waitForElementPresent("@mainEnableSlider", 2000);
            return this;
        }
    }],
    elements: {
        mainEnableSlider: ".toggle-header .checkbox-slider",
        emailValidationCard: ".self-service-card[data-type='emailValidation']",
        subjectLocale: ".translationMapGroup[field='subjectTranslations'] .newTranslationLocale",
        subjectTranslation: ".translationMapGroup[field='subjectTranslations'] .newTranslationText",
        subjectAddBtn: ".translationMapGroup[field='subjectTranslations'] .add"
    }
};
