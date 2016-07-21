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
    "form2js",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/common/delegates/SocialDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/util/AdminUtils",
    "bootstrap-dialog",
    "selectize"
], function($, _,
            handlebars,
            form2js,
            AdminAbstractView,
            ConfigDelegate,
            SocialDelegate,
            EventManager,
            Constants,
            AdminUtils,
            BootstrapDialog,
            selectize) {
    var SocialConfigView = AdminAbstractView.extend({
        template: "templates/admin/settings/SocialConfigTemplate.html",
        element: "#socialContainer",
        noBaseTemplate: true,
        events: {
            "change .section-check" : "controlSectionSwitch",
            "click .btn-link" : "editConfig"
        },
        model: {

        },
        partials: [
            "partials/_toggleIconBlock.html",
            "partials/settings/social/_openid_connect.html",
            "partials/form/_basicInput.html",
            "partials/form/_tagSelectize.html"
        ],
        render: function(args, callback) {
            $.when(
                SocialDelegate.providerList(),
                SocialDelegate.availableProviders()
            ).then((providerList, currentProviders) => {
                this.data.providers = _.cloneDeep(providerList.providers);
                this.model.providers = _.cloneDeep(providerList.providers);

                _.each(this.data.providers, (provider, index) => {
                    provider.togglable = true;
                    provider.editable = true;
                    provider.details = $.t("templates.socialProviders.configureProvider");
                    provider.enabled = false;

                    switch(provider.name) {
                        case "google":
                            provider.displayIcon = "google";
                            break;
                    }


                    _.each(currentProviders.providers, (currentProvider) => {
                        if(provider.name === currentProvider.name) {
                            _.extend(this.model.providers[index], currentProvider);

                            provider.enabled = true;
                        }
                    });
                });

                this.parentRender(() => {});
            });
        },

        controlSectionSwitch: function(event) {
            event.preventDefault();

            var toggle = $(event.target),
                card = toggle.parents(".wide-card"),
                index = this.$el.find(".wide-card").index(card),
                enabled;

            card.toggleClass("disabled");
            enabled = !card.hasClass("disabled");

            this.model.providers[index].enabled = enabled;

            if(enabled) {
                this.createConfig(this.model.providers[index]);
            } else {
                this.deleteConfig(this.model.providers[index]);
            }
        },

        editConfig: function(event) {
            event.preventDefault();

            var card = $(event.target).parents(".wide-card"),
                cardDetails = this.getCardDetails(card),
                index = this.$el.find(".wide-card").index(card);

            ConfigDelegate.readEntity("identityProvider/" +cardDetails.name).then((providerConfig) => {
                this.dialog = BootstrapDialog.show({
                    title: AdminUtils.capitalizeName(cardDetails.name) + " " + $.t("templates.socialProviders.provider"),
                    type: BootstrapDialog.TYPE_DEFAULT,
                    size: BootstrapDialog.SIZE_WIDE,
                    message: $(handlebars.compile("{{> settings/social/_" + cardDetails.type + "}}")(providerConfig)),
                    onshown: _.bind(function (dialogRef) {
                        dialogRef.$modalBody.find(".array-selection").selectize({
                            delimiter: ",",
                            persist: false,
                            create: function (input) {
                                return {
                                    value: input,
                                    text: input
                                };
                            }
                        });
                    }, this),
                    buttons: [
                        {
                            label: $.t("common.form.close"),
                            action: function (dialogRef) {
                                dialogRef.close();
                            }
                        },
                        {
                            label: $.t("common.form.save"),
                            cssClass: "btn-primary",
                            id: "saveUserConfig",
                            action: (dialogRef) => {
                                var formData = form2js("socialDialogForm", ".", true),
                                    saveData = this.generateSaveData(formData, providerConfig);

                                this.saveConfig(saveData);

                                this.model.providers[index] = saveData;

                                dialogRef.close();
                            }
                        }
                    ]
                });
            });
        },

        generateSaveData: function(formData, currentData) {
            var secret = currentData.client_secret;

            _.extend(currentData, formData);

            if(_.isNull(currentData.client_secret)) {
                currentData.client_secret = secret;
            }

            return currentData;
        },

        getCardDetails: function(card) {
            var cardDetails = {};

            cardDetails.type = card.attr("data-type");
            cardDetails.name = card.attr("data-name");

            return cardDetails;
        },

        createConfig: function(config) {
            ConfigDelegate.createEntity("identityProvider/"+config.name, config).then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "saveSocialProvider");
            });
        },

        deleteConfig: function(config) {
            ConfigDelegate.deleteEntity("identityProvider/"+config.name).then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteSocialProvider");
            });
        },

        saveConfig: function(config) {
            ConfigDelegate.updateEntity("identityProvider/"+config.name, config).then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "saveSocialProvider");
            });
        }
    });

    return new SocialConfigView();
});
