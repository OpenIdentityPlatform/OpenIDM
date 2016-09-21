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
    "org/forgerock/commons/ui/common/util/OAuth",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/delegates/SocialDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/UserModel"
], function($, _, bootstrap,
    BootstrapDialog,
    AbstractUserProfileTab,
    Configuration,
    Constants,
    EventManager,
    OAuth,
    Router,
    SocialDelegate,
    UIUtils,
    UserModel
) {
    var SocialIdentitiesView = AbstractUserProfileTab.extend({
        template: "templates/profile/SocialIdentitiesTab.html",
        events: _.extend({
            "click .social-toggle": "toggleAction",
            "click .closeErrorMsg": "closeErrorMsg"
        }, AbstractUserProfileTab.prototype.events),

        /**
         Expected by all dynamic user profile tabs - returns a map of details necessary to render the nav tab
         */
        getTabDetail: function() {
            return {
                "panelId": "socialIdentities",
                "label": $.t("templates.socialIdentities.socialIdentities")
            };
        },

        model: {},

        render: function(args, callback) {
            let params = Router.convertCurrentUrlToJSON().params;

            if (!_.isEmpty(params) && _.has(params, "provider") && _.has(params, "code") && _.has(params, "redirect_uri")) {

                opener.require("org/forgerock/openidm/ui/user/profile/SocialIdentitiesTab").oauthReturn(params);
                window.close();
                return;

            } else {

                this.data.user = Configuration.loggedUser.toJSON();
                SocialDelegate.providerList().then((response) => {
                    this.data.providers = response.providers;

                    _.each(this.data.providers, (provider, index) => {
                        switch (provider.name) {
                            case "google":
                                provider.faIcon = "google";
                                break;
                            case "facebook":
                                provider.faIcon = "facebook";
                                break;
                            case "linkedIn":
                                provider.faIcon = "linkedin";
                                break;
                            default:
                                provider.faIcon = "cloud";
                                break;
                        }

                        this.activateProviders(provider, index);
                    });

                    this.parentRender(() => {
                        this.$el.find("#idpUnbindError").hide();
                        if (callback) {
                            callback();
                        }
                    });
                });

            }
        },

        activateProviders: function(provider, index) {
            if (_.has(this.data.user, "idpData") &&
                _.has(this.data.user.idpData, provider.name) &&
                this.data.user.idpData[provider.name].enabled) {
                this.data.providers[index].active = true;
            }
        },

        toggleSocialProvider: function(card) {
            if (card.find("[type=checkbox]").prop("checked")) {
                card.toggleClass("disabled", false);
                card.find(".scopes").show();
            } else {
                card.toggleClass("disabled", true);
                card.find(".scopes").hide();
            }
        },

        getProviderName(card) {
            return $(card).find(".card-body").data("name");
        },

        getProviderObj(providerName) {
            return _.filter(this.data.providers, (obj) => {
                return obj.name === providerName;
            })[0];
        },

        toggleAction: function(event) {
            event.preventDefault();

            let card = $(event.target).parents(".card");
            let options = null;

            if (!card.find("[type=checkbox]").prop("checked")) {

                options = this.getOptions(card);
                this.oauthPopup(options);
                this.toggleSocialProvider(card);

            } else {
                this.disconnectDialog(card);
            }
        },

        oauthReturn: function(params) {
            let card = this.$el.find(`.card-body[data-name="${params.provider}"]`).parents(".card");

            Configuration.loggedUser.bindProvider(params.provider, params.code, params.redirect_uri).then(() => {
                card.find("[type=checkbox]").prop("checked", true);
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "saveSocialProvider");
                this.toggleSocialProvider(card);
            });
        },

        getOptions: function(card) {
            let options = {};

            options.windowName = this.getProviderName(card);
            options.path = this.getUrl(this.getProviderObj(options.windowName));

            return options;
        },

        oauthPopup: function(options) {
            let oauthWindow = null;
            let width = "";
            let height = "";

            width = screen.width * (2 / 3);
            height = screen.height * (2 / 3);

            options.windowName = options.windowName || 'ConnectWithOAuth';
            options.windowOptions = options.windowOptions || 'location=0,status=0,width=' + width + ',height=' + height;
            options.callback = options.callback || function() {
                window.location.reload();
            };

            oauthWindow = window.open(options.path, options.windowName, options.windowOptions);
        },

        disconnectDialog: function(card) {

            BootstrapDialog.show({
                title: $.t("templates.socialIdentities.confirmTitle") + _.capitalize(this.getProviderName(card)) + "?",
                type: BootstrapDialog.TYPE_DANGER,
                message: $.t("templates.socialIdentities.confirmMessage") + _.capitalize(this.getProviderName(card)) + ".",
                buttons: [{
                    label: $.t("common.form.cancel"),
                    id: "disconnectCancel",
                    action: function(dialogRef) {
                        dialogRef.close();

                        $(card).prop('checked', true);
                    }
                }, {
                    label: $.t('common.form.confirm'),
                    id: "disconnectConfirm",
                    cssClass: "btn-danger",
                    action: _.bind(function(dialogRef) {
                        dialogRef.close();

                        this.unbindProvider(card);
                    }, this)
                }]
            });

        },

        unbindProvider: function(card) {
            let providerName = this.getProviderName(card);

            Configuration.loggedUser.unbindProvider(providerName).then((result) => {
                this.data.user = result;
                card.find("[type=checkbox]").prop("checked", false);
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "removeSocialProvider");
                this.toggleSocialProvider(card);
            }, (err) => {
                this.alignProvidersArray();
                this.data.unbindText = $.t(err.responseJSON.message, { provider: _.startCase(providerName) });
                UIUtils.renderTemplate(this.template, this.$el, this.data, $.noop(), "replace");
                this.$el.find("#idpUnbindError").show();
                delete this.data.unbindText;
            });

        },

        alignProvidersArray: function() {
            _.each(this.data.providers, (provider, index) => {
                provider.enabled = this.data.user.idpData[provider.name].enabled;
            });
        },

        getUrl: function(provider) {
            let scopes = provider.scope.join(" ");
            let currentURL = Router.currentRoute;
            let state = Router.getLink(currentURL, [
                "&provider=" + provider.name +
                "&redirect_uri=" + OAuth.getRedirectURI()
            ]);

            return OAuth.getRequestURL(provider.authorization_endpoint, provider.client_id, scopes, state);
        },

        getCurrentRoute() {
            return Router.currentRoute;
        },

        closeErrorMsg: function(event) {
            if (event) {
                event.preventDefault();
            }
            this.$el.find("#idpUnbindError").hide();
        }

    });

    return new SocialIdentitiesView();
});
