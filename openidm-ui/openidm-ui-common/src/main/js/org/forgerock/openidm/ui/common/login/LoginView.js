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
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/LoginView",
    "org/forgerock/commons/ui/common/util/OAuth",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/util/AMLoginUtils"
], function($, _, Handlebars,
    SocialDelegate,
    Constants,
    EventManager,
    commonLoginView,
    OAuth,
    Router,
    amLoginUtils) {

    var LoginView = function () {},
        obj;

    LoginView.prototype = commonLoginView;

    obj = new LoginView();

    obj.partials = (commonLoginView.partials || []).concat([
        "partials/login/_loginButtons.html",
        "partials/providers/_OAuth.html"
    ]);

    _.extend(obj.events, {
        "click .oauth": "oauthHandler"
    });

    // TODO: share oauthHandler logic between registration and login
    obj.oauthHandler = function (e) {
        e.preventDefault();
        window.location.href = OAuth.getRequestURL(
            $(e.target).attr("authorization_endpoint"),
            $(e.target).attr("client_id"),
            ["openid"],
            Router.getLink(Router.currentRoute,
                [
                    "&provider=" + $(e.target).val() +
                    "&redirect_uri=" + OAuth.getRedirectURI()
                ]
            )
        );
    };
    // TODO: share providerPartialFromType between registration and login
    Handlebars.registerHelper("providerPartialFromType", function (type) {
        return "providers/_" + type;
    });

    obj.render = function (args, callback) {
        var amCallback = amLoginUtils.init(this,true),
            oauthProviders = SocialDelegate.providerList(),
            params = Router.convertCurrentUrlToJSON().params;

        if (!_.isEmpty(params) &&
            _.has(params, "provider") &&
            _.has(params, "code") &&
            _.has(params, "redirect_uri")) {

            SocialDelegate.getAuthToken(params.provider, params.code, params.redirect_uri)
                .then(function (authToken) {
                    EventManager.sendEvent(Constants.EVENT_LOGIN_REQUEST, {
                        authToken: authToken.auth_token
                    });
                });
        }

        commonLoginView.render.call(this, args, _.bind(function () {

            oauthProviders.then(_.bind(function (response) {
                this.$el.find("[name=loginButton]").after(
                    Handlebars.compile("{{> login/_loginButtons}}")({providers: response.providers})
                );
            }, this));

            if (callback) {
                callback();
            }

            if (amCallback) {
                amCallback();
            }

        }, this));
    };

    return obj;
});
