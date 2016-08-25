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
    "underscore",
    "bootstrap",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/user/profile/AbstractUserProfileTab",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/login/LoginView",
    "org/forgerock/commons/ui/common/util/OAuth",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/delegates/SocialDelegate"
], function($, _, bootstrap,
            BootstrapDialog,
            AbstractUserProfileTab,
            Configuration,
            Constants,
            EventManager,
            LoginView,
            OAuth,
            Router,
            SocialDelegate) {
    var SocialIdentitiesView = AbstractUserProfileTab.extend({
        template: "templates/profile/SocialIdentitiesTab.html",
        events: _.extend({
            "change .social-toggle" : "toggleAction"
        }, AbstractUserProfileTab.prototype.events),

        /**
         Expected by all dynamic user profile tabs - returns a map of details necessary to render the nav tab
         */
        getTabDetail : function () {
            return {
                "panelId": "socialIdentities",
                "label": $.t("templates.socialIdentities.socialIdentities")
            };
        },

        model: {},

        render: function(args, callback) {
            this.data.user = Configuration.loggedUser.toJSON();

            SocialDelegate.providerList().then((response) => {
                this.data.providers = response.providers;

                _.each(this.data.providers, (provider, index) => {
                    this.data.providers[index].faIcon = provider.name.toLowerCase();
                    this.activateProviders(provider, index);
                });

                this.parentRender(callback);
            });
        },

        activateProviders: function(provider, index) {
            if (_.has(this.data.user, "idpData") &&
                _.has(this.data.user.idpData, provider.name) && this.data.user.idpData[provider.name].enabled
            ) {
                    this.data.providers[index].active = true;
            }
        },

        toggleSocialProvider: function(toggle) {

            var check = $(toggle),
                card = check.parents(".card");

            if(check.is(":checked")) {
                card.toggleClass("disabled", false);
                card.find(".scopes").show();
            } else {
                card.toggleClass("disabled", true);
                card.find(".scopes").hide();
            }
        },

        getProviderName(toggle) {
            return $(toggle).parents().eq(3).data("name");
        },

        getProviderObj(providerName) {
            return _.filter(this.data.providers, (obj) => {
                return obj.name === providerName;
            })[0];
        },

        toggleAction: function(event) {
            event.preventDefault();

            let toggle = event.target,
                options = null;

            if (toggle.checked) {

                options = this.getOptions(toggle, true);
                this.oauthPopup(options);
                this.toggleSocialProvider(toggle);

            } else {
                this.disconnectDialog(toggle);
            }
        },

        getOptions: function(toggle, isBind) {
            let options = {},
                urls = {},
                code = "",
                authToken = "",
                providerName = this.getProviderName(toggle),
                redirect_uri = OAuth.getRedirectURI();

            options.windowName = this.getProviderName(toggle);
            urls = this.getUrls(this.getProviderObj(options.windowName));
            options.path = urls.reqUrl + "&display=popup";
            options.return = urls.resUrl;

            options.callback = (code) => {
                if (code) {
                    if (isBind) {
                        this.bindProvider(toggle, code);
                    } else {
                        this.unbindProvider(toggle, code);
                    }

                } else {
                    this.restoreToggle(toggle);
                }
            }

            return options;
        },

        getReturnUrl(url) {

        },

        oauthPopup: function (options) {
            let oauthWindow = null,
                oauthInterval = null,
                width = "",
                height = "",
                fragments = [];

            width = screen.width * (2/3);
            height = screen.height * (2/3);

            options.windowName = options.windowName ||  'ConnectWithOAuth';
            options.windowOptions = options.windowOptions || 'location=0,status=0,width=' + width + ',height=' + height;
            options.callback = options.callback || function(){ window.location.reload(); };

            oauthWindow = window.open(options.path, options.windowName, options.windowOptions);

            oauthInterval = window.setInterval(() => {
                if (oauthWindow && oauthWindow.document) {
                    fragments = oauthWindow.document.URL.split("&code=");
                    if (fragments[0] === options.return) {
                        window.clearInterval(oauthInterval);
                        options.callback(fragments[1]);
                        oauthWindow.close();
                    }
                } else if (oauthWindow && oauthWindow.closed) {
                    window.clearInterval(oauthInterval);
                    if (!oauthWindow.document || ! oauthWindow.document.URL) {
                        options.callback(null);
                    }
                }

            }, 1000);
        },

        restoreToggle: function(toggle) {
            $(toggle).prop("checked", false);
            this.toggleSocialProvider(toggle);
        },

        disconnectDialog: function(toggle) {

            BootstrapDialog.show({
                title: $.t("templates.socialIdentities.confirmTitle") + _.capitalize(this.getProviderName(toggle)) + "?",
                type: BootstrapDialog.TYPE_DANGER,
                message: $.t("templates.socialIdentities.confirmMessage") + _.capitalize(this.getProviderName(toggle)) + ".",
                buttons: [{
                    label: $.t("common.form.cancel"),
                    id:"disconnectCancel",
                    action: function(dialogRef) {
                        dialogRef.close();

                        $(toggle).prop('checked', true);
                    }
                }, {
                    label: $.t('common.form.confirm'),
                    id:"disconnectConfirm",
                    cssClass: "btn-danger",
                    action: _.bind(function(dialogRef) {
                        dialogRef.close();

                        let options = this.getOptions(toggle, false);

                        this.oauthPopup(options);
                    }, this)
                }]
            });

        },

        bindProvider: function(toggle, code) {
            let id = this.data.user._id,
                providerName = this.getProviderName(toggle),
                redirect_uri = OAuth.getRedirectURI();

            SocialDelegate.bindProvider(id, providerName, code, redirect_uri).then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "saveSocialProvider");
                this.toggleSocialProvider(toggle);
            });
        },

        unbindProvider: function(toggle, code) {
            let id = this.data.user._id,
                providerName = this.getProviderName(toggle),
                redirect_uri = OAuth.getRedirectURI();

            SocialDelegate.unbindProvider(id, providerName, code, redirect_uri).then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "saveSocialProvider");
                this.toggleSocialProvider(toggle);
            });
        },

        getUrls: function(provider) {
            let scopes = provider.scope.join(" "),
                currentURL = Router.currentRoute,
                state = Router.getLink(currentURL,
                    [
                        "&provider=" + provider.name +
                        "&redirect_uri=" + OAuth.getRedirectURI()
                    ]);
                returnURL = Router.getCurrentUrlBasePart() + "/#" + state;

            return {
                reqUrl: OAuth.getRequestURL(provider.authorization_endpoint, provider.client_id, scopes, state),
                resUrl: returnURL
            };
        }

    });

    return new SocialIdentitiesView();
});
