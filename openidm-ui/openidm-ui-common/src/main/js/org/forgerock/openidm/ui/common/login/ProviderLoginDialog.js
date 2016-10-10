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

define([
    "jquery",
    "underscore",
    "handlebars",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/openidm/ui/common/delegates/SocialDelegate",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/OAuth",
    "org/forgerock/openidm/ui/common/util/OAuthUtils",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/AbstractView"
], function( $, _,
             Handlebars,
             BootstrapDialog,
             SocialDelegate,
             Router,
             OAuth,
             OAuthUtils,
             UIUtils,
             Configuration,
             Constants,
             EventManager,
             AbstractView) {
    var ProviderLoginDialog = AbstractView.extend({
        template: "templates/login/ProviderLoginDialog.html",
        element: "#dialogs",
        events : {
            "click [data-oauth=button]": "oauthHandler"
        },
        model: {

        },
        render: function (options, userDetails) {
            this.model.authenticatedCallback = options.authenticatedCallback;

            UIUtils.preloadPartial("partials/login/_loginButtons.html");
            UIUtils.preloadPartial("partials/providers/_providerButton.html");

            SocialDelegate.loginProviders().then((configuredProviders) => {
                let type = {
                    "action" : $.t("templates.socialIdentities.signIn")
                };

                if(!_.isUndefined(userDetails.provider)) {
                    this.model.currentProviders = _.filter(configuredProviders.providers, (obj) => {
                        return obj.name === userDetails.provider;
                    });
                } else {
                    this.model.currentProviders = configuredProviders.providers;
                }

                _.each(configuredProviders.providers, (provider) => {
                    provider.scope = provider.scope.join(" ");

                    provider.icon =  Handlebars.compile(provider.icon)(type);
                });

                this.data.providers = configuredProviders.providers;

                var dialogBody = $('<div id="providerLoginDialog"></div>');

                this.$el.find('#dialogs').append(dialogBody);

                this.setElement(dialogBody);

                this.model.bootstrapDialog = BootstrapDialog.show({
                    closable: false,
                    title:  $.t("common.form.sessionExpired"),
                    type: BootstrapDialog.TYPE_DEFAULT,
                    message: dialogBody,
                    onshown: _.bind(function () {
                        UIUtils.renderTemplate(
                            this.template,
                            this.$el,
                            _.extend({}, Configuration.globalData, this.data),
                            _.noop,
                            "replace");
                    }, this)
                });
            });
        },

        oauthHandler : function(event) {
            var handler = $(event.currentTarget),
                providerName = handler.prop("title"),
                currentProvider = _.filter(this.model.currentProviders, (obj) => {
                    return obj.name === providerName;
                })[0],
                state = "login/&provider=" + providerName + "&redirect_uri=" + OAuth.getRedirectURI()+"&gotoURL=#openerHandler/loginDialog",
                url = OAuthUtils.getUrl(currentProvider, state),
                options = {
                    "redirect_uri" :  OAuth.getRedirectURI(),
                    "windowName" : providerName,
                    "path" : url + "&display=popup",
                    "callback" : () => {
                        this.model.bootstrapDialog.close();
                    }
                };

            OAuthUtils.oauthPopup(options);
        },

        oauthReturn : function() {
            EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, {
                anonymousMode: false
            });

            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loggedIn");

            this.model.bootstrapDialog.close();

            if (this.model.authenticatedCallback) {
                this.model.authenticatedCallback();
            }
        }
    });

    return new ProviderLoginDialog();
});
