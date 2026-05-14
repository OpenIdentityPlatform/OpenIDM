"use strict";

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
 * Copyright 2016 ForgeRock AS.
 */

define(["jquery", "lodash", "org/forgerock/commons/ui/common/main/AbstractView", "org/forgerock/commons/ui/user/anonymousProcess/KBAQuestionView"], function ($, _, AbstractView, KBAQuestionView) {
    var KBAView = AbstractView.extend({
        element: "#kbaQuestions",
        template: "templates/user/process/KBATemplate.html",
        noBaseTemplate: true,
        MIN_NUMBER_OF_QUESTIONS: 1,
        events: {
            "click [data-add-question]": "addQuestion"
        },

        /**
         * TODO Implement mechanism of passing existing KBA questions to this view, as this is to be used on the profile
         * page as well.
         *
         * The format of existing KBA questions is the following:
         *  "kbaInfo":[
         *      {
         *          "customQuestion": "question2",
         *          "answer":{"$crypto":{"value":{"algorithm":"SHA-256","data":"....."},"type":"salted-hash"}}
         *      },
         *      {
         *          "questionId": "2",
         *          "answer":{"$crypto":{"value":{"algorithm":"SHA-256","data":"....."},"type":"salted-hash"}}
         *      }
         * ]
         */
        render: function render(kbaConfig) {
            this.allQuestions = kbaConfig.questions;
            this.minNumberOfQuestions = kbaConfig.minItems || this.MIN_NUMBER_OF_QUESTIONS;
            this.selectedQuestions = [];
            this.questionsCounter = 0;

            this.parentRender(function () {
                this.setDescription();

                this.itemsContainer = this.$el.find("#kbaItems");

                _.times(this.minNumberOfQuestions, _.bind(function () {
                    this.addQuestion();
                }, this));
            });
        },

        setDescription: function setDescription() {
            this.$el.find("#kbaDescription").text($.t("common.user.kba.description", {
                numberOfQuestions: this.minNumberOfQuestions
            }));
        },

        getUnSelectedQuestions: function getUnSelectedQuestions() {
            var selectedQuestions = _(this.selectedQuestions).map(function (questionView) {
                return questionView.getSelectedQuestionId();
            }).compact().value();

            return _.filter(this.allQuestions, function (question) {
                return selectedQuestions.indexOf(question.id) === -1;
            });
        },

        addQuestion: function addQuestion(e) {
            if (e) {
                e.preventDefault();
            }

            var atMinimumThresholdBeforeAdding = this.isAtMinimumThreshold(),
                question = new KBAQuestionView({ id: this.questionsCounter++ });

            this.selectedQuestions.push(question);

            question.render({
                possibleQuestions: this.getUnSelectedQuestions(),
                numberOfQuestionsSufficient: this.isNumberOfQuestionsSufficient()
            }, this.itemsContainer);

            if (atMinimumThresholdBeforeAdding) {
                this.reRenderAllQuestions();
            }
        },

        reRenderAllQuestions: function reRenderAllQuestions() {
            var unSelectedQuestions = this.getUnSelectedQuestions();

            _.each(this.selectedQuestions, _.bind(function (questionView) {
                var currentViewQuestion = _.findWhere(this.allQuestions, { id: questionView.getSelectedQuestionId() }),
                    possibleQuestions = _.clone(unSelectedQuestions);

                if (currentViewQuestion) {
                    possibleQuestions.push(currentViewQuestion);
                    possibleQuestions.sort(function (q1, q2) {
                        return q1.id - q2.id;
                    });
                }

                questionView.updateQuestionWithNewData({
                    possibleQuestions: possibleQuestions,
                    numberOfQuestionsSufficient: this.isNumberOfQuestionsSufficient()
                });
            }, this));
        },

        isNumberOfQuestionsSufficient: function isNumberOfQuestionsSufficient() {
            return this.minNumberOfQuestions < this.selectedQuestions.length;
        },

        deleteQuestion: function deleteQuestion(viewId) {
            var questionView = _.findWhere(this.selectedQuestions, { id: viewId });

            questionView.remove();
            this.selectedQuestions = _.without(this.selectedQuestions, questionView);

            if (questionView.getSelectedQuestionId() || this.isAtMinimumThreshold()) {
                this.reRenderAllQuestions();
            }
        },

        changeQuestion: function changeQuestion() {
            this.reRenderAllQuestions();
        },

        isAtMinimumThreshold: function isAtMinimumThreshold() {
            return this.minNumberOfQuestions === this.selectedQuestions.length;
        },

        getQuestions: function getQuestions() {
            return _.map(this.selectedQuestions, function (questionView) {
                return questionView.getPair();
            });
        }
    });

    return new KBAView();
});
