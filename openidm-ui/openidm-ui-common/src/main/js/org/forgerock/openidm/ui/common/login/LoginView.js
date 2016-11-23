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

define([
    "jquery",
    "lodash",
    "handlebars",
    "org/forgerock/openidm/ui/common/delegates/SocialDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/LoginView",
    "org/forgerock/commons/ui/common/util/OAuth",
    "org/forgerock/commons/ui/common/main/Router"
], function($, _, Handlebars,
            SocialDelegate,
            Configuration,
            Constants,
            EventManager,
            commonLoginView,
            OAuth,
            Router) {

    var LoginView = function () {},
        obj;

    LoginView.prototype = commonLoginView;

    obj = new LoginView();

    _.extend(obj.events, {
        "click [data-oauth=button]": "oauthHandler"
    });

    obj.redirectToIDP = function (authorization_endpoint, client_id, scope, provider) {
        // TODO: share oauthHandler logic between registration and login
        window.location.href = OAuth.getRequestURL(authorization_endpoint, client_id, scope,
            Router.getLink(Router.currentRoute,
                [
                    "&provider=" + provider +
                    "&redirect_uri=" + OAuth.getRedirectURI() +
                    "&gotoURL=" + (Configuration.gotoURL || "#")
                ]
            )
        );
    };

    obj.oauthHandler = function (e) {
        e.preventDefault();
        this.redirectToIDP(
            $(e.target).parents("[data-oauth=button]").attr("authorization_endpoint"),
            $(e.target).parents("[data-oauth=button]").attr("client_id"),
            $(e.target).parents("[data-oauth=button]").attr("scope"),
            $(e.target).parents("[data-oauth=button]").attr("value")
        );
    };
    // TODO: share providerPartialFromType between registration and login
    Handlebars.registerHelper("providerPartialFromType", function (type) {
        return "providers/_" + type;
    });

    obj.render = function (args, callback) {
        var oauthProviders = SocialDelegate.loginProviders(),
            params = Router.convertCurrentUrlToJSON().params;

        if (!_.isEmpty(params) &&
            _.has(params, "provider") &&
            _.has(params, "code") &&
            _.has(params, "redirect_uri")) {

            SocialDelegate.getAuthToken(params.provider, params.code, params.redirect_uri)
                .then(function (authToken) {
                    if (_.has(params, "gotoURL")) {
                        Configuration.gotoURL = params.gotoURL;
                    }
                    EventManager.sendEvent(Constants.EVENT_LOGIN_REQUEST, {
                        authToken: authToken.auth_token,
                        provider: params.provider,
                        failureCallback: (reason) => {
                            EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                                route: Router.configuration.routes.login,
                                args: ["&preventAutoLogin=true"]
                            });
                        }
                    });
                }, function () {
                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                        route: Router.configuration.routes.login,
                        args: ["&preventAutoLogin=true"]
                    });
                });

        } else {

            oauthProviders.then(_.bind(function (response) {
                let type = {
                    "action" : $.t("templates.socialIdentities.signIn")
                };

                _.each(response.providers, (provider) => {
                    provider.scope = provider.scope.join(" ");

                    provider.icon =  Handlebars.compile(provider.icon)(type);
                });

                this.data.providers = response.providers;

                if (this.data.providers.length === 1 &&
                    !_.has(params, "preventAutoLogin") &&
                    this.data.providers[0].name === "OPENAM") {
                    this.redirectToIDP(
                        this.data.providers[0].authorization_endpoint,
                        this.data.providers[0].client_id,
                        this.data.providers[0].scope,
                        this.data.providers[0].name
                    );
                } else {
                    commonLoginView.render.call(this, args, _.bind(function () {
                        if (callback) {
                            callback();
                        }
                    }, this));
                }

            }, this));

        }

    };

    return obj;
});
