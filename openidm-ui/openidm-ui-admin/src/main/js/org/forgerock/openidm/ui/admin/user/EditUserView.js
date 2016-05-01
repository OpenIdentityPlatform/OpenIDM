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
    "org/forgerock/openidm/ui/admin/role/MembersView"
],
function ($, _, Handlebars, AbstractView, GenericEditResourceView, ValidatorsManager, MembersView) {
    var EditUserView = function () {
        return AbstractView.apply(this, arguments);
    };

    EditUserView.prototype = Object.create(GenericEditResourceView);
    EditUserView.prototype.tabViewOverrides.roles = MembersView;
    EditUserView.prototype.events = _.extend({
        "change #password :input": "showPendingChanges",
        "keyup #password :input": "showPendingChanges"
    }, GenericEditResourceView.events);

    EditUserView.prototype.partials = GenericEditResourceView.partials.concat(["partials/resource/_passwordTab.html"]);

    EditUserView.prototype.render = function (args, callback) {
        GenericEditResourceView.render.call(this, args, _.bind(function () {
            if (_.has(this.data.schema.properties, "password") && !this.$el.find("#password").length) {
                this.addPasswordTab();
            }
            if (callback) {
                callback();
            }
        }, this));
    };

    EditUserView.prototype.addPasswordTab = function () {
        var tabHeader = this.$el.find("#tabHeaderTemplate").clone(),
            tabContent = Handlebars.compile("{{> resource/_passwordTab}}");

        tabHeader.attr("id", "tabHeader_password");
        tabHeader.find("a").attr("href","#password").text($.t('common.user.password'));
        tabHeader.show();

        this.$el.find("#resourceDetailsTabHeader").after(tabHeader);
        this.$el.find("#resource-details").after(tabContent);

        ValidatorsManager.bindValidators(
            this.$el.find("#password"),
            [this.data.objectType,this.objectName,this.objectId || "*"].join("/")
        );
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
