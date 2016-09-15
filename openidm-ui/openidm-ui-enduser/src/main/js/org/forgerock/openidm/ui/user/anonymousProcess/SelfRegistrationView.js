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
    "org/forgerock/commons/ui/common/util/UIUtils"
], function($, _, form2js, Handlebars,
    AnonymousProcessView,
    OAuth,
    Router,
    CommonSelfRegistrationView,
    ValidatorsManager,
    UIUtils) {

    var SelfRegistrationView = AnonymousProcessView.extend({
        baseEntity: "selfservice/registration",
        partials: [
            "partials/process/_coreProfileDetails.html",
            "partials/profile/_multiValueFormFields.html",
            "partials/profile/_emailEntry.html",
            "partials/providers/_providerButton.html"
        ],
        events: _.extend({
            "click [data-oauth=button]": "oauthHandler"
        }, CommonSelfRegistrationView.events),
        oauthHandler: function (e) {
            e.preventDefault();
            window.location.href = OAuth.getRequestURL(
                $(e.target).parents("[data-oauth=button]").attr("authorization_endpoint"),
                $(e.target).parents("[data-oauth=button]").attr("client_id"),
                $(e.target).parents("[data-oauth=button]").attr("scope"),
                Router.getLink(Router.currentRoute,
                    [
                        "/continue" +
                        (this.delegate.token ? ("&token=" + this.delegate.token) : "") +
                        "&provider=" + $(e.target).parents("[data-oauth=button]").attr("value") +
                        "&redirect_uri=" + OAuth.getRedirectURI()
                    ]
                )
            );
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

            UIUtils.compileTemplate(templateUrl, stateData)
            .then(function (renderedTemplate) {
                processStatePromise.resolve(renderedTemplate);
            }, _.bind(function () {
                this.loadGenericTemplate(stateData, baseTemplateUrl, response, processStatePromise);
            }, this));
        }
    });

    SelfRegistrationView.prototype = _.extend(Object.create(CommonSelfRegistrationView), SelfRegistrationView.prototype);

    return new SelfRegistrationView();
});
