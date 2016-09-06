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
            UserModel
        ) {
    var SocialIdentitiesView = AbstractUserProfileTab.extend({
        template: "templates/profile/SocialIdentitiesTab.html",
        events: _.extend({
            "click .social-toggle" : "toggleAction"
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

                    this.parentRender(callback);
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

        toggleSocialProvider: function(toggle) {

            if(toggle.find("[type=checkbox]").prop("checked")) {
                toggle.toggleClass("disabled", false);
                toggle.find(".scopes").show();
            } else {
                toggle.toggleClass("disabled", true);
                toggle.find(".scopes").hide();
            }
        },

        getProviderName(toggle) {
            return $(toggle).find(".card-body").data("name");
        },

        getProviderObj(providerName) {
            return _.filter(this.data.providers, (obj) => {
                return obj.name === providerName;
            })[0];
        },

        toggleAction: function(event) {
            event.preventDefault();

            let toggle = $(event.target).parents(".card");
            let options = null;

            if (!toggle.find("[type=checkbox]").prop("checked")) {

                options = this.getOptions(toggle, true);
                this.oauthPopup(options);
                this.toggleSocialProvider(toggle);

            } else {
                this.disconnectDialog(toggle);
            }
        },

        oauthReturn: function (params) {
            Configuration.loggedUser.bindProvider(params.provider, params.code, params.redirect_uri).then(() => {
                let toggle = this.$el.find(`.card-body[data-name="${params.provider}"]`).parents(".card");

                toggle.find("[type=checkbox]").prop("checked", true);
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "saveSocialProvider");
                this.toggleSocialProvider(toggle);
            });
        },

        getOptions: function(toggle, isBind) {
            let options = {};
            let urls = {};
            let code = "";
            let authToken = "";
            let providerName = this.getProviderName(toggle);
            let redirect_uri = OAuth.getRedirectURI();

            options.windowName = this.getProviderName(toggle);
            urls = this.getUrls(this.getProviderObj(options.windowName));
            options.path = urls.reqUrl + "&display=popup";
            options.return = urls.resUrl;

            return options;
        },

        oauthPopup: function (options) {
            let oauthWindow = null;
            let oauthInterval = null;
            let width = "";
            let height = "";
            let fragments = [];

            width = screen.width * (2/3);
            height = screen.height * (2/3);

            options.windowName = options.windowName ||  'ConnectWithOAuth';
            options.windowOptions = options.windowOptions || 'location=0,status=0,width=' + width + ',height=' + height;
            options.callback = options.callback || function(){ window.location.reload(); };

            oauthWindow = window.open(options.path, options.windowName, options.windowOptions);

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

                        this.unbindProvider(toggle);
                    }, this)
                }]
            });

        },

        unbindProvider: function(toggle) {
            let id = this.data.user._id;
            let providerName = this.getProviderName(toggle);

            Configuration.loggedUser.unbindProvider(providerName).then(() => {
                toggle.find("[type=checkbox]").prop("checked", false);
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "saveSocialProvider");
                this.toggleSocialProvider(toggle);
            });
        },

        getUrls: function(provider) {
            let scopes = provider.scope.join(" ");
            let currentURL = Router.currentRoute;
            let state = Router.getLink(currentURL,
                [
                    "&provider=" + provider.name +
                    "&redirect_uri=" + OAuth.getRedirectURI()
                ]);
            let returnURL = Router.getCurrentUrlBasePart() + "/#" + state;

            return {
                reqUrl: OAuth.getRequestURL(provider.authorization_endpoint, provider.client_id, scopes, state),
                resUrl: returnURL
            };
        }

    });

    return new SocialIdentitiesView();
});
