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
 * Copyright 2015-2016 ForgeRock AS.
 */

define(["jquery", "lodash", "form2js", "js2form", "handlebars", "org/forgerock/commons/ui/common/main/AbstractView", "org/forgerock/commons/ui/common/components/ChangesPending", "org/forgerock/commons/ui/common/main/Configuration", "org/forgerock/commons/ui/user/profile/ConfirmPasswordDialog", "org/forgerock/commons/ui/common/util/Constants", "org/forgerock/commons/ui/common/main/EventManager", "org/forgerock/commons/ui/common/main/ValidatorsManager"], function ($, _, form2js, js2form, Handlebars, AbstractView, ChangesPending, Configuration, ConfirmPasswordDialog, Constants, EventManager, ValidatorsManager) {

    /**
     * Provides base functionality for all tabs within UserProfileView
     * @exports org/forgerock/commons/ui/user/profile/AbstractUserProfileTab
     */
    var AbstractUserProfileTab = AbstractView.extend({
        noBaseTempate: true,
        events: {
            "click input[type=submit]": "formSubmit",
            "click input[type=reset]": "resetForm",
            "reload form": "reloadFormData",
            "change :input": "checkChanges"
        },

        /**
         * Attaches a ChangesPending instance within the view
         * Requires the presence of an element with the "changes-pending" class
         * Initializes with the current value from this.getFormContent()
         */
        initializeChangesPending: function initializeChangesPending() {
            this.changesPendingWidget = ChangesPending.watchChanges({
                element: this.$el.find(".changes-pending"),
                watchedObj: { subform: this.getFormContent() },
                watchedProperties: ["subform"],
                alertClass: "alert-warning alert-sm"
            });
        },

        /**
         * Works with form validators and changes pending widget to reflect the state of the
         * form as the user makes their edits.
         */
        checkChanges: function checkChanges(event) {
            var target = $(event.target),
                form = $(target.closest("form"));

            ValidatorsManager.bindValidators(form, Configuration.loggedUser.baseEntity, function () {
                ValidatorsManager.validateAllFields(form);
            });

            target.trigger("validate");
            if (!target.attr("data-validation-dependents")) {
                this.changesPendingWidget.makeChanges({ subform: this.getFormContent() });
            }

            form.find("input[type='reset']").prop("disabled", false);
        },

        /**
         * Generic method for reading content from the view's form. Extend if necessary for more
         * complex form parsing needs.
         */
        getFormContent: function getFormContent() {
            return form2js(this.$el.find("form")[0], ".", false);
        },

        /**
         * Used for populating the form with a "clean" set of data, either when first rendered
         * or when the form is reset.
         */
        reloadFormData: function reloadFormData(userData) {
            var form = this.$el.find("form");
            this.data.user = userData || this.data.user;
            js2form(form[0], this.data.user);
            $("input[type=password]", form).val("").attr("placeholder", $.t("common.form.passwordPlaceholder"));

            ValidatorsManager.clearValidators(form);
            ValidatorsManager.bindValidators(form, Configuration.loggedUser.baseEntity, function () {
                form.find("input[type='reset']").prop("disabled", true);
                form.find("input[type='submit']").prop("disabled", true);
            });
            this.initializeChangesPending();
        },

        resetForm: function resetForm(event) {
            event.preventDefault();
            this.reloadFormData();
        },

        /**
         * Generic save method  - patch the user model with the local data and persist it
         */
        submit: function submit(formData) {
            Configuration.loggedUser.save(formData, { patch: true }).then(_.bind(function () {
                this.submitSuccess();
            }, this));
        },

        /**
         * After a form is saved, reset the content with the most recent data for the user
         */
        submitSuccess: function submitSuccess() {
            this.data.user = Configuration.loggedUser.toJSON();
            this.reloadFormData();
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateSuccessful");
        },

        /**
         * Attempt to submit the form. If the form is invalid, it will fail. If the user
         * is changing a protected attribute, prompt them to first enter their old password.
         * Finally, attempt to actually submit the form data.
         */
        formSubmit: function formSubmit(event) {

            event.preventDefault();
            event.stopPropagation();

            var changedProtected = [],
                form = $(event.target).closest("form"),
                formData = this.getFormContent(form[0]);

            if (ValidatorsManager.formValidated(form)) {

                changedProtected = _.chain(Configuration.loggedUser.getProtectedAttributes()).filter(function (attr) {
                    if (_.has(formData, attr)) {
                        if (_.isEmpty(Configuration.loggedUser.get(attr)) && _.isEmpty(formData[attr])) {
                            return false;
                        } else {
                            return !_.isEqual(Configuration.loggedUser.get(attr), formData[attr]);
                        }
                    } else {
                        return false;
                    }
                }, this).map(function (attr) {
                    return this.$el.find("label[for=input-" + attr + "]").text();
                }, this).value();

                if (changedProtected.length === 0) {
                    this.submit(formData);
                } else {
                    ConfirmPasswordDialog.render(changedProtected, _.bind(function (currentPassword) {
                        Configuration.loggedUser.setCurrentPassword(currentPassword);
                        this.submit(formData);
                    }, this));
                }
            }
        }

    });

    return AbstractUserProfileTab;
});
