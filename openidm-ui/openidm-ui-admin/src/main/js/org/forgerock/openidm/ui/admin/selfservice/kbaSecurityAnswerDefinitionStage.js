/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "bootstrap",
    "handlebars",
    "form2js",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/AdminUtils",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"

], function($, _,
            boostrap,
            Handlebars,
            form2js,
            AbstractView,
            UiUtils,
            AdminUtils,
            ValidatorsManager,
            ConfigDelegate,
            EventManager,
            Constants) {

    var kbaSecurityAnswerDefinitionStage = AbstractView.extend({
        element: "#SelfServiceStageDialog",
        template: "templates/admin/selfservice/kbaSecurityAnswerDefinitionStage.html",
        noBaseTemplate: true,
        events: {
            "click .fa-pencil": "showEditPanel",
            "click .cancel-add-edit": "cancelAdd",
            "click .add-translation": "addTranslation",
            "click .update-add-edit": "updateQuestion",
            "click .addEditPanel .fa-times": "deleteQuestionTranslation",
            "click .preview-row .fa-times": "deleteQuestion",
            "click .add-question": "addQuestion",
            "change .translation-locale": "checkLocale"
        },
        partials: [
            "partials/selfservice/_kbaTranslation.html",
            "partials/_alert.html"
        ],
        model: {
            selectize: {}
        },
        data: {
            "locales": [
                "de",
                "en",
                "en_GB",
                "es",
                "fr",
                "it",
                "ja",
                "ko",
                "pt_BR",
                "sv",
                "zh_CN",
                "zh_TW"
            ]
        },
        render: function(args) {
            _.extend(this.data, args.data, true);
            this.args = _.clone(args, true);

            this.data.locales = _.sortBy(this.data.locales);

            ConfigDelegate.readEntity("selfservice.kba").then((kba) => {
                this.model.kba = _.clone(kba, true);
                this.data.questions = this.getFormattedQuestions(kba.questions);
                this.data.minimumAnswersToDefine = kba.minimumAnswersToDefine;
                this.model.questions = _.clone(this.data.questions, true);
                this.renderParent();
            });
        },

        renderParent: function() {
            this.model.usedQuestionKeys = _.map(this.model.questions, (translations, key) => {
                let num = parseInt(key, 10);

                if (!_.isNaN(num)) {
                    return num;
                } else {
                    return key;
                }
            });

            this.parentRender(() => {
                _.each(this.data.questions, (val, key) => {
                    this.model.selectize[key] = this.$el.find(".editPanel[data-question-key='" + key + "'] .translation-locale").selectize({
                        "create": true,
                        "createOnBlur": true
                    });
                });

                this.model.addSelectize = this.$el.find(".addPanel .translation-locale").selectize({
                    "create": true,
                    "createOnBlur": true
                });

                ValidatorsManager.bindValidators(this.$el.find("#kbaSecurityAnswerDefinitionStage"));
                ValidatorsManager.validateAllFields(this.$el.find("#kbaSecurityAnswerDefinitionStage"));
            });
        },

        checkLocale: function(e) {
            var addTranslationContainer = $(e.currentTarget).closest(".input-row"),
                selectedLocale = e.currentTarget.value,
                questionPanel = $(e.currentTarget).closest(".addEditPanel"),
                key = questionPanel.attr("data-question-key"),
                usedLocales = _.map(this.model.questions[key], "locale");

            if (_.indexOf(usedLocales, selectedLocale) > -1) {
                questionPanel.find(".localeAlert").show();
                addTranslationContainer.find(".add-translation").toggleClass("disabled", true);
            } else {
                questionPanel.find(".localeAlert").hide();
                addTranslationContainer.find(".add-translation").toggleClass("disabled", false);
            }
        },

        getFreshKey: function(usedKeys) {
            var max = _.max(usedKeys);

            if (_.isUndefined(max)) {
                return 1;
            } else {
                return max + 1;
            }
        },

        addQuestion: function(e) {
            e.preventDefault();
            this.$el.find(".addPanel").show();
            this.$el.find(".add-question").hide();

            this.$el.find(".preview-row").show();
            this.$el.find(".editPanel").hide();
            this.clearInputs();

            this.$el.find(".addPanel").attr("data-question-key", this.getFreshKey(this.model.usedQuestionKeys));
        },

        addTranslation: function(e) {
            e.preventDefault();

            if ($(e.currentTarget).hasClass("disabled")) {
                return false;
            }

            var panel = $(e.currentTarget).closest(".addEditPanel"),
                key = panel.attr("data-question-key"),
                locale = panel.find(".translation-locale").val(),
                translation = {
                    "locale": locale,
                    "translation": panel.find(".translation-value").val()
                },
                insertedIndex,
                newRow;

            if (!_.isArray(this.model.questions[key])) {
                this.model.questions[key] = [];
            }

            this.model.questions[key].push(translation);
            this.model.questions[key] = _.sortBy(this.model.questions[key], "locale");

            insertedIndex = _.findIndex(this.model.questions[key], translation);
            newRow = Handlebars.compile("{{> selfservice/_kbaTranslation}}")(translation);

            $(newRow).insertBefore(panel.find("li")[insertedIndex]);

            this.clearInputs();
        },

        clearInputs: function() {
            this.$el.find(".translation-value").val("");

            this.model.addSelectize[0].selectize.clear();
            _.each(this.model.selectize, (selectize) => {
                selectize[0].selectize.clear();
            });

            this.$el.find(".add-translation").toggleClass("disabled", true);
        },

        deleteQuestion: function(e) {
            e.preventDefault();

            var container = $(e.currentTarget).closest(".preview-row"),
                key = container.attr("data-question-key"),
                translationContainer = container.next();

            delete this.model.questions[key];

            delete this.data.questions[key];

            container.remove();
            translationContainer.remove();
        },

        deleteQuestionTranslation: function(e) {
            e.preventDefault();

            var questionPanel = $(e.currentTarget).closest(".addEditPanel"),
                key = questionPanel.attr("data-question-key"),
                translationContainer = $(e.currentTarget).closest("li"),
                deleteIndex = _.findIndex(questionPanel.find(".translation"), function(el){
                    return el === translationContainer[0];
                });

            this.model.questions[key].splice(deleteIndex, 1);
            translationContainer.remove();
        },

        updateQuestion: function(e) {
            e.preventDefault();
            this.data.questions = _.clone(this.model.questions, true);
            this.renderParent();
        },

        /**
         * Takes the raw questions object from selfserbice.kba.json and formats the data for handlebars rendering
         * @param unformattedQuestions {object}
         * @returns {*}
         */
        getFormattedQuestions: function(unformattedQuestions) {
            var questions = _.clone(unformattedQuestions, true);

            _.each(unformattedQuestions, (question, key) => {
                questions[key] = [];
                _.each(question, (translation, locale) => {
                    questions[key].push({
                        "locale": locale,
                        "translation": translation
                    });
                });
                questions[key] = _.sortBy(questions[key], "locale");
            });

            return questions;
        },

        saveKBA: function() {
            this.model.kba.minimumAnswersToDefine = parseInt(this.$el.find("#input-minimumAnswersToDefine").val(), 10);
            this.model.kba.questions = {};

            _.each(this.data.questions, (questionArray, key) => {
                this.model.kba.questions[key] = {};
                _.each(questionArray, (val) => {
                    this.model.kba.questions[key][val.locale] = val.translation;
                });
            });

            ConfigDelegate.updateEntity("selfservice.kba", this.model.kba);
        },

        getData: function() {
            this.saveKBA();
            return this.args;
        },

        showEditPanel: function(e) {
            e.preventDefault();
            var key = $(e.currentTarget).closest("li").attr("data-question-key");

            // If a panel is opened while another was being worked on the previous unsaved changes
            // need to be overwritten so the ui doesn't get in an odd state
            this.model.questions = _.clone(this.data.questions, true);
            this.renderParent();

            this.$el.find(".preview-row").show();
            this.$el.find(".editPanel").hide();
            this.$el.find(".addPanel").hide();
            this.$el.find(".add-question").show();

            this.$el.find(".editPanel[data-question-key='" + key + "']").slideToggle(300);
            this.$el.find(".editPanel[data-question-key='" + key + "']").prev().hide();
        },

        cancelAdd: function(e) {
            e.preventDefault();
            this.renderParent();
        },

        validationSuccessful: function (event) {
            AbstractView.prototype.validationSuccessful(event);
            this.$el.closest(".modal-content").find("#saveUserConfig").toggleClass("disabled", false);
        },

        validationFailed: function (event, details) {
            AbstractView.prototype.validationFailed(event, details);
            this.$el.closest(".modal-content").find("#saveUserConfig").toggleClass("disabled", true);
        }
    });

    return new kbaSecurityAnswerDefinitionStage();
});
