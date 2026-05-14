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
 * Copyright 2011-2016 ForgeRock AS.
 */

define(["underscore", "placeholder", "org/forgerock/commons/ui/common/main/AbstractView", "org/forgerock/commons/ui/common/util/ModuleLoader", "org/forgerock/commons/ui/common/main/ValidatorsManager", "org/forgerock/commons/ui/common/main/EventManager", "org/forgerock/commons/ui/common/util/Constants", "org/forgerock/commons/ui/common/util/CookieHelper"], function (_, placeholder, AbstractView, ModuleLoader, validatorsManager, eventManager, constants, cookieHelper) {
    var LoginView = AbstractView.extend({
        template: "templates/common/LoginTemplate.html",
        baseTemplate: "templates/common/LoginBaseTemplate.html",

        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate"
        },

        partials: ["partials/providers/_providerButton.html"],

        formSubmit: function formSubmit(event) {
            event.preventDefault();
            if (this.$el.find("[name=loginRemember]:checked").length !== 0) {
                var expire = new Date();
                expire.setDate(expire.getDate() + 365 * 20);
                cookieHelper.setCookie("login", this.$el.find("input[name=login]").val(), expire);
            } else {
                cookieHelper.deleteCookie("login");
            }

            eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, {
                userName: this.$el.find("input[name=login]").val(),
                password: this.$el.find("input[name=password]").val()
            });
        },

        render: function render(args, callback) {
            this.parentRender(function () {
                validatorsManager.bindValidators(this.$el);

                this.$el.find("input").placeholder();

                var login = cookieHelper.getCookie("login");
                if (login) {
                    this.$el.find("input[name=login]").val(login).prop('autofocus', false);
                    this.$el.find("[name=loginRemember]").prop("checked", true);
                    validatorsManager.validateAllFields(this.$el);
                    this.$el.find("[name=password]").focus();
                } else {
                    this.$el.find("input[name=login]").focus();
                }

                if (callback) {
                    callback();
                }
            });
        }
    });

    return new LoginView();
});
