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
    "form2js",
    "handlebars",
    "org/forgerock/commons/ui/user/anonymousProcess/AnonymousProcessView",
    "org/forgerock/commons/ui/common/util/OAuth",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/user/anonymousProcess/SelfRegistrationView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/util/Constants"
], function($, _, form2js, Handlebars,
            AnonymousProcessView,
            OAuth,
            Router,
            CommonSelfRegistrationView,
            ValidatorsManager,
            UIUtils,
            Configuration,
            EventManager,
            Constants) {

    var SelfRegistrationView = AnonymousProcessView.extend({
        baseEntity: "selfservice/registration",
        partials: [
            "partials/process/_coreProfileDetails.html",
            "partials/profile/_multiValueFormFields.html",
            "partials/profile/_emailEntry.html",
            "partials/profile/_preferences.html",
            "partials/providers/_providerButton.html"
        ],
        events: _.extend({
            "click [data-oauth=button]": "oauthHandler",
            "focus .float-label input": "addFloatLabelStyles",
            "blur .float-label": "removeFloatLabelStyles"
        }, CommonSelfRegistrationView.events),
        oauthHandler: function (e) {
            e.preventDefault();
            window.location.href = OAuth.getRequestURL(
                $(e.target).parents("[data-oauth=button]").attr("authorization_endpoint"),
                $(e.target).parents("[data-oauth=button]").attr("client_id"),
                $(e.target).parents("[data-oauth=button]").attr("scope"),
                Router.getLink(Router.configuration.routes.login,
                    [
                        "&provider=" +  $(e.target).parents("[data-oauth=button]").attr("value") +
                        "&redirect_uri=" + OAuth.getRedirectURI() +
                        "&gotoURL=" + (Configuration.gotoURL || "#")
                    ]
                )
            );
        },

        /**
         Intercept the request to the backend to inject the nonce taken from session storage,
         when appropriate
         */
        submitDelegate: function (params, onSubmit) {
            if (params.provider && params.code && params.redirect_uri) {
                params = _.extend({
                    nonce: OAuth.getCurrentNonce()
                }, params);
            }
            CommonSelfRegistrationView.submitDelegate.call(this, params, onSubmit);
        },

        addFloatLabelStyles: function(e) {
            if (!$(e.target).attr("readonly")) {
                $(e.target).removeClass("input-lg");
                $(e.target).prev().removeClass("sr-only");
                $(e.target).parent().addClass("float-label-with-focus");
            }
        },

        removeFloatLabelStyles: function(e) {
            if (!$(e.target).val()) {
                $(e.target).addClass("input-lg");
                $(e.target).prev().addClass("sr-only");
                $(e.target).parent().removeClass("float-label-with-focus");
            }
        },

        attemptCustomTemplate: function(stateData, baseTemplateUrl, response, processStatePromise) {
            var templateUrl = baseTemplateUrl + this.processType
                + "/" + response.type + "-" + response.tag + ".html",
                type = {
                    "action": $.t("templates.socialIdentities.register")
                };

            if (_.has(stateData, "requirements.definitions.providers.items.oneOf")) {
                _.each(stateData.requirements.definitions.providers.items.oneOf, (provider) => {
                    provider.icon =  Handlebars.compile(provider.icon)(type);
                });
            }

            if(stateData.additions && stateData.additions.credentialJwt) {
                EventManager.sendEvent(Constants.EVENT_LOGIN_REQUEST, {
                    jwt: stateData.additions.credentialJwt
                });
            } else if (stateData.additions && stateData.additions.id_token){
                EventManager.sendEvent(Constants.EVENT_LOGIN_REQUEST, {
                    idToken: stateData.additions.id_token,
                    provider: stateData.additions.provider,
                    suppressMessage: false
                });
            } else {
                UIUtils.compileTemplate(templateUrl, stateData)
                .then(function (renderedTemplate) {
                    processStatePromise.resolve(renderedTemplate);
                }, _.bind(function () {
                    this.loadGenericTemplate(stateData, baseTemplateUrl, response, processStatePromise);
                }, this));
            }
        }
    });

    SelfRegistrationView.prototype = _.extend(Object.create(CommonSelfRegistrationView), SelfRegistrationView.prototype);

    return new SelfRegistrationView();
});
