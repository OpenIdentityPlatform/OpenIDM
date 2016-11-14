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
    "handlebars",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/common/resource/GenericEditResourceView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/admin/role/MembersView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"],
function ($, _,
          Handlebars,
          AbstractView,
          GenericEditResourceView,
          ValidatorsManager,
          MembersView,
          ConfigDelegate,
          ResourceDelegate,
          EventManager,
          Constants) {

    var EditUserView = function () {
        return AbstractView.apply(this, arguments);
    };

    EditUserView.prototype = Object.create(GenericEditResourceView);
    EditUserView.prototype.tabViewOverrides.roles = MembersView;
    EditUserView.prototype.events = _.extend({
        "change #password :input": "showPendingChanges",
        "keyup #password :input": "showPendingChanges",
        "click #resetPasswordBtn": "resetPasswordBtn",
        "shown.bs.tab #tabHeader_password a[data-toggle='tab']": "toggleResetPasswordEventBtn",
        "hidden.bs.tab #tabHeader_password > a[data-toggle='tab']": "toggleResetPasswordEventBtn"
    }, GenericEditResourceView.events);

    EditUserView.prototype.partials = GenericEditResourceView.partials.concat(["partials/resource/_passwordTab.html", "partials/_alert.html"]);

    EditUserView.prototype.render = function (args, callback) {
        GenericEditResourceView.render.call(this, args, _.bind(function () {
            if (_.has(this.data.schema.properties, "password") && !this.$el.find("#password").length) {
                this.addPasswordTab().then(callback);
            } else if (callback) {
                callback();
            }
        }, this));
    };

    EditUserView.prototype.resetPasswordBtn = function (e) {
        e.preventDefault();

        if ($(e.currentTarget).attr("disabled") !== "disabled") {

            ResourceDelegate.serviceCall({
                serviceUrl: "/openidm/managed/user",
                url: "/" + this.objectId + "?_action=resetPassword",
                type: "POST",
                success: (e) => {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "userPasswordResetSuccess");
                },
                errorsHandlers: {
                    "badRequest": {
                        status: "400",
                        message: "userPasswordResetEmailFailure"
                    }
                }
            });
        }
    };

    EditUserView.prototype.setResetPasswordScriptAvailable = function (resetPasswordScriptAvailable) {
        this.resetPasswordScriptAvailable = resetPasswordScriptAvailable;
    };

    EditUserView.prototype.getResetPasswordScriptAvailable = function () {
        return this.resetPasswordScriptAvailable;
    };

    EditUserView.prototype.setEmailServiceAvailable = function (emailServiceAvailable) {
        this.emailServiceAvailable = emailServiceAvailable;
    };
    EditUserView.prototype.getEmailServiceAvailable = function (emailServiceAvailable) {
        return this.emailServiceAvailable;
    };

    EditUserView.prototype.getEmailConfigAlertHidden = function () {
        if (!this.data.newObject && this.getResetPasswordScriptAvailable()) {
            return this.getEmailServiceAvailable();
        } else {
            return true;
        }
    };

    EditUserView.prototype.addPasswordTab = function () {
        var emailRead = ConfigDelegate.readEntityAlways("external.email"),
            managedRead =  ConfigDelegate.readEntity("managed");

        return $.when(emailRead, managedRead).then(_.bind(function(emailConfig, managedObjects) {

            this.setEmailServiceAvailable(!_.isUndefined(emailConfig) && _.has(emailConfig, "host"));
            this.setResetPasswordScriptAvailable(_.get(_.find(managedObjects.objects, {"name": "user"}), "actions.resetPassword") !== undefined);

            var tabHeader = this.$el.find("#tabHeaderTemplate").clone(),
                tabContent = Handlebars.compile("{{> resource/_passwordTab" +
                " emailServiceAvailable=" + this.getEmailConfigAlertHidden() +
                " resetPasswordScriptAvailable=" + this.getResetPasswordScriptAvailable() +
                " emailMessage='" + $.t("templates.admin.ChangeUserPasswordDialogTemplate.outboundEmailConfigRequired") + "'" +
                " linkMessage='" + $.t("templates.admin.ChangeUserPasswordDialogTemplate.outboundEmailConfigRequiredLinkMessage") + "'" +
                " error='" + $.t("common.form.warning") + "'" +
                "}}");

            tabHeader.attr("id", "tabHeader_password");
            tabHeader.find("a").attr("href", "#password").text($.t('common.user.password'));
            tabHeader.show();

            this.$el.find("#resourceDetailsTabHeader").after(tabHeader);
            this.$el.find("#resource-details").after(tabContent);

            this.addResetPasswordButton();

            ValidatorsManager.bindValidators(
                this.$el.find("#password"),
                [this.data.objectType, this.objectName, this.objectId || "*"].join("/")
            );
        }, this));
    };

    EditUserView.prototype.addResetPasswordButton = function () {
        var el = $('<a href="#" class="btn btn-primary" id="resetPasswordBtn" style="display: none;">' + $.t("templates.admin.ChangeUserPasswordDialogTemplate.resetPassword") + '</a>');

        this.$el.find("#saveBtn").before(this.configureResetPasswordButton(el));
    };

    EditUserView.prototype.configureResetPasswordButton = function (el) {
        if (this.getResetPasswordScriptAvailable() && !this.data.newObject) {
            el.attr("disabled", !this.getEmailConfigAlertHidden());
        } else {
            el.hide();
        }

        return el;
    };

    EditUserView.prototype.toggleResetPasswordBtn = function (state, newObject, resetPasswordScriptAvailable) {
        if (!newObject && resetPasswordScriptAvailable) {
            this.$el.find("#resetPasswordBtn").toggle(state);
        }
    };

    EditUserView.prototype.toggleResetPasswordEventBtn = function (e) {
        this.toggleResetPasswordBtn(e.type === "shown", this.data.newObject, this.getResetPasswordScriptAvailable());
    };

    EditUserView.prototype.getFormValue = function () {
        var passwordText = this.$el.find("#input-password").val();

        if (ValidatorsManager.formValidated(this.$el.find("#password")) && passwordText && passwordText.length) {
            return _.extend(
                { "password": passwordText },
                GenericEditResourceView.getFormValue.call(this)
            );
        } else {
            return GenericEditResourceView.getFormValue.call(this);
        }

    };

    return new EditUserView();
});
