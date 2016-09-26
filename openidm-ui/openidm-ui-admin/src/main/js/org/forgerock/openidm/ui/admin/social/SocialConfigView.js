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
    "org/forgerock/openidm/ui/admin/delegates/SiteConfigurationDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/selfservice/UserRegistrationConfigView",
    "bootstrap-dialog",
    "selectize",
    "libs/codemirror/lib/codemirror",
    "libs/codemirror/mode/xml/xml"
], function($, _,
            handlebars,
            form2js,
            AdminAbstractView,
            ConfigDelegate,
            SocialDelegate,
            EventManager,
            Constants,
            AdminUtils,
            SiteConfigurationDelegate,
            UIUtils,
            UserRegistrationConfigView,
            BootstrapDialog,
            selectize,
            codemirror) {
    var SocialConfigView = AdminAbstractView.extend({
        template: "templates/admin/social/SocialConfigTemplate.html",
        events: {
            "change .section-check" : "controlSectionSwitch",
            "click .btn-link" : "editConfig"
        },
        model: {
            "iconCode" : null
        },
        partials: [
            "partials/_toggleIconBlock.html",
            "partials/social/_google.html",
            "partials/social/_facebook.html",
            "partials/social/_linkedIn.html",
            "partials/social/_oAuth2.html",
            "partials/social/_callback.html",
            "partials/form/_basicInput.html",
            "partials/form/_tagSelectize.html",
            "partials/_alert.html"
        ],

        render: function(args, callback) {
            this.model.userRegistration = null;

            $.when(
                SocialDelegate.providerList(),
                SocialDelegate.availableProviders(),
                ConfigDelegate.readEntityAlways("selfservice/registration"),
                ConfigDelegate.readEntityAlways("authentication")
            ).then((currentProviders, availableProviders, userRegistration, authentication) => {
                availableProviders.providers = _.map(availableProviders.providers, (p) => {
                    p.enabled = false;
                    return p;
                });
                this.data.providers = _.cloneDeep(availableProviders.providers);

                this.model.providers = _.cloneDeep(availableProviders.providers);
                this.model.authentication = authentication;

                this.model.AuthModuleEnabled = _.some(authentication.serverAuthContext.authModules, (module) => {
                    if (module.name === "SOCIAL_PROVIDERS" && module.enabled) {
                        return true;
                    }
                });

                if (userRegistration) {
                    this.model.userRegistration = userRegistration;
                }

                _.each(this.data.providers, (provider, index) => {
                    provider.togglable = true;
                    provider.editable = true;
                    provider.details = $.t("templates.socialProviders.configureProvider");

                    switch (provider.name) {
                        case "google":
                            provider.displayIcon = "google";
                            break;
                        case "facebook":
                            provider.displayIcon = "facebook";
                            break;
                        case "linkedIn":
                            provider.displayIcon = "linkedin";
                            break;
                        default:
                            provider.displayIcon = "cloud";
                            break;
                    }

                    _.each(currentProviders.providers, (currentProvider) => {
                        if (provider.name === currentProvider.name) {
                            _.extend(this.model.providers[index], currentProvider);

                            provider.enabled = true;
                        }
                    });
                });

                this.parentRender(() => {
                    var messageResult = this.getMessageState(currentProviders.providers.length, this.model.userRegistration, this.model.AuthModuleEnabled);

                    if(messageResult.login) {
                        this.$el.find("#socialNoAuthWarningMessage").show();
                    } else {
                        this.$el.find("#socialNoAuthWarningMessage").hide();
                    }

                    if(messageResult.registration) {
                        this.$el.find("#socialNoRegistrationWarningMessage").show();
                    } else {
                        this.$el.find("#socialNoRegistrationWarningMessage").hide();
                    }
                });
            });
        },

        getMessageState: function(providerCount, userRegistration, AuthModuleEnabled) {
            var messageDisplay = {
                "login" : false,
                "registration" : false
            };

            if (providerCount === 0 &&
                userRegistration &&
                userRegistration.stageConfigs[0].name === "socialUserDetails") {

                UserRegistrationConfigView.switchToUserDetails(this.model.userRegistration);

                messageDisplay.registration = false;
            } else if (_.isNull(userRegistration) && providerCount > 0) {
                messageDisplay.registration = true;
            } else if(providerCount > 0 &&
                userRegistration &&
                userRegistration.stageConfigs[0].name === "userDetails") {
                messageDisplay.registration = true;
            } else if (providerCount === 0) {
                messageDisplay.registration = false;
            }

            if (!AuthModuleEnabled && providerCount > 0) {
                messageDisplay.login = true;
            } else {
                messageDisplay.login = false;
            }

            return messageDisplay;
        },

        controlSectionSwitch: function(event) {
            event.preventDefault();
            var originalVal = !$(event.currentTarget).is(":checked"),
                toggle = $(event.target),
                card = toggle.parents(".wide-card"),
                cardDetails = this.getCardDetails(card),
                index = this.$el.find(".wide-card").index(card),
                enabled;

            function configSocialProvider() {
                var providerCount,
                    messageResult,
                    managedConfigPromise = ConfigDelegate.readEntity("managed");

                card.toggleClass("disabled");
                enabled = !card.hasClass("disabled");

                this.model.providers[index].enabled = enabled;

                providerCount = _.filter(this.model.providers, function(provider) {
                    return provider.enabled;
                }).length;

                if (enabled) {
                    this.createConfig(this.model.providers[index]).then(() => {
                        managedConfigPromise
                            .then((managedConfig) => {
                                if (providerCount === 1) {
                                    return this.addBindUnbindBehavior(
                                        this.addIDPsProperty(managedConfig)
                                    );
                                } else {
                                    return managedConfig;
                                }
                            })
                            .then((managedConfig) =>
                                this.addManagedObjectForIDP(this.model.providers[index], managedConfig)
                            )
                            .then((managedConfig) =>
                                ConfigDelegate.updateEntity("managed", managedConfig)
                            ).then(() => {
                                SiteConfigurationDelegate.updateConfiguration(function () {
                                    EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);
                                });
                            });

                        card.find(".btn-link").trigger("click");
                    });
                } else {
                    this.deleteConfig(this.model.providers[index]).then(() => {
                        managedConfigPromise
                            .then((managedConfig) => {
                                if (providerCount === 0) {
                                    return this.removeBindUnbindBehavior(
                                        this.removeIDPsProperty(managedConfig)
                                    );
                                } else {
                                    return managedConfig;
                                }
                            })
                            .then((managedConfig) =>
                                this.removeManagedObjectForIDP(this.model.providers[index], managedConfig)
                            )
                            .then((managedConfig) =>
                                ConfigDelegate.updateEntity("managed", managedConfig)
                            ).then(() => {
                                SiteConfigurationDelegate.updateConfiguration(function () {
                                    EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);
                                });
                            });
                    });
                }

                messageResult = this.getMessageState(providerCount, this.model.userRegistration, this.model.AuthModuleEnabled);

                if(messageResult.login) {
                    this.$el.find("#socialNoAuthWarningMessage").show();
                } else {
                    this.$el.find("#socialNoAuthWarningMessage").hide();
                }

                if(messageResult.registration) {
                    this.$el.find("#socialNoRegistrationWarningMessage").show();
                } else {
                    this.$el.find("#socialNoRegistrationWarningMessage").hide();
                }
            }

            // If you are disabling a social provider
            if (!card.hasClass("disabled")) {
                let numOfEnabledProviders = _.filter(this.model.providers, function(provider) {
                    return provider.enabled;
                }).length - 1; //removing the current provider from the calculation

                // If the social provider to be disabled is the only enabled provider
                if (numOfEnabledProviders === 0) {
                    let self = this;

                    if (this.model.AuthModuleEnabled) {

                        BootstrapDialog.show({
                            title: $.t('common.form.confirm'),
                            type: "type-danger",
                            message: $.t("templates.socialProviders.disableSocialAuthModule"),
                            id: "frConfirmationDialog",
                            buttons: [
                                {
                                    label: $.t('common.form.cancel'),
                                    id: "frConfirmationDialogBtnClose",
                                    action: function(dialog) {
                                        $(event.currentTarget).prop("checked", originalVal);
                                        dialog.close();
                                    }
                                },
                                {
                                    label: $.t('common.form.ok'),
                                    cssClass: "btn-danger",
                                    id: "frConfirmationDialogBtnOk",
                                    action: function(dialog) {
                                        _.each(self.model.authentication.serverAuthContext.authModules, (module) => {
                                            if (module.name === "SOCIAL_PROVIDERS") {
                                                module.enabled = false;
                                                self.model.AuthModuleEnabled = false;
                                            }
                                        });

                                        ConfigDelegate.updateEntity("authentication", self.model.authentication).then(function() {
                                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "authSaveSuccess");
                                        });

                                        configSocialProvider.call(self);

                                        dialog.close();
                                    }
                                }
                            ]
                        });
                    } else {
                        configSocialProvider.call(this);
                    }
                } else {
                    configSocialProvider.call(this);
                }
            } else {
                configSocialProvider.call(this);
            }
        },

        editConfig: function(event) {
            event.preventDefault();

            var card = $(event.target).parents(".wide-card"),
                cardDetails = this.getCardDetails(card),
                index = this.$el.find(".wide-card").index(card),
                dialogDetails,
                additionalDisplayData = {};

            ConfigDelegate.readEntity("identityProvider/" +cardDetails.name).then((providerConfig) => {
                additionalDisplayData.adminCallback = window.location.protocol+"//"+window.location.host + "/admin/oauthReturn.html";
                additionalDisplayData.enduserCallback =  window.location.protocol+"//"+window.location.host + "/oauthReturn.html";

                try {
                    dialogDetails = $(handlebars.compile("{{> social/_" + cardDetails.name + "}}")(_.extend(additionalDisplayData, providerConfig)));
                } catch (e) {
                    additionalDisplayData.configureGeneral = $.t("templates.socialProviders.configureGeneral", {"providerType" : providerConfig.name});
                    additionalDisplayData.generalHelp =  $.t("templates.socialProviders.generalHelp", {"providerType" : providerConfig.name});

                    dialogDetails = $(handlebars.compile("{{> social/_oAuth2}}")(_.extend(additionalDisplayData, providerConfig)));
                }

                this.dialog = BootstrapDialog.show({
                    title: AdminUtils.capitalizeName(cardDetails.name) + " " + $.t("templates.socialProviders.provider"),
                    type: BootstrapDialog.TYPE_DEFAULT,
                    size: BootstrapDialog.SIZE_WIDE,
                    message: dialogDetails,
                    onshow: (dialogRef) => {
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

                        this.model.iconCode = codemirror.fromTextArea(dialogRef.$modalBody.find(".button-html")[0], {
                            lineNumbers: true,
                            viewportMargin: Infinity,
                            theme: "forgerock",
                            mode: "xml",
                            htmlMode: true,
                            lineWrapping: true
                        });

                        dialogRef.$modalBody.find("#advancedOptions").on("shown.bs.collapse", _.bind(function (e) {
                            this.model.iconCode.refresh();
                        }, this));

                        dialogRef.$modalBody.find(".advanced-options-toggle").bind("click", (event) => {this.advancedOptionToggle(event);});
                    },
                    onshown: () => {
                        this.model.iconCode.refresh();
                    },
                    onclose: (dialogRef) => {
                        if(this.model.iconCode) {
                            this.model.iconCode = null;
                        }
                    },
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

                                if(this.model.iconCode) {
                                    saveData.icon = this.model.iconCode.getValue();
                                }

                                if(saveData.client_id && saveData.client_id.length) {
                                    saveData.client_id = saveData.client_id.trim();
                                }

                                if(saveData.client_secret && saveData.client_secret.length) {
                                    saveData.client_secret = saveData.client_secret.trim();
                                }

                                this.saveConfig(saveData);

                                this.model.providers[index] = saveData;

                                dialogRef.close();
                            }
                        }
                    ]
                });
            });
        },

        advancedOptionToggle: function(event) {
            event.preventDefault();

            var link = $(event.target);

            if (link.hasClass("collapsed")) {
                link.text($.t("templates.socialProviders.hideAdvanced"));
            } else {
                link.text($.t("templates.socialProviders.showAdvanced"));
            }
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
            return ConfigDelegate.createEntity("identityProvider/"+config.name, config).then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "saveSocialProvider");
            });
        },

        deleteConfig: function(config) {
            return ConfigDelegate.deleteEntity("identityProvider/"+config.name).then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteSocialProvider");
            });
        },

        saveConfig: function(config) {
            return ConfigDelegate.updateEntity("identityProvider/"+config.name, config).then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "saveSocialProvider");
            });
        },

        addBindUnbindBehavior: function (managedConfig) {
            let updatedManagedConfig = _.cloneDeep(managedConfig),
                managedUser = _.find(updatedManagedConfig.objects, (o) => o.name === "user");

            if (!_.has(managedUser, "actions")) {
                managedUser.actions = {};
            }
            if (!_.has(managedUser.actions, "unbind")) {
                managedUser.actions.unbind = {
                    "type" : "text/javascript",
                    "file" : "ui/unBindBehavior.js"
                };
            }
            if (!_.has(managedUser.actions, "bind")) {
                managedUser.actions.bind = {
                    "type" : "text/javascript",
                    "file" : "ui/bindBehavior.js"
                };
            }
            return updatedManagedConfig;
        },

        removeBindUnbindBehavior: function (managedConfig) {
            let updatedManagedConfig = _.cloneDeep(managedConfig),
                managedUser = _.find(updatedManagedConfig.objects, (o) => o.name === "user");

            if (_.has(managedUser, "actions.unbind")) {
                delete managedUser.actions.unbind;
            }
            if (_.has(managedUser, "actions.bind")) {
                delete managedUser.actions.bind;
            }
            return updatedManagedConfig;
        },

        /**
         *
         */
        addManagedObjectForIDP : (provider, managedConfig) => {
            let updatedManagedConfig = _.cloneDeep(managedConfig),
                managedUser = _.find(updatedManagedConfig.objects, (o) => o.name === "user");

            managedUser.schema.properties.idps.items.resourceCollection.push({
                "path" : "managed/" + provider.name,
                "label" : provider.name + " Info",
                "query" : {
                    "queryFilter" : "true",
                    // take the top three properties from the propertyMap config as the default
                    // representative values for the resourceCollection
                    "fields" : provider.propertyMap.map((prop) => prop.source).slice(0,3),
                    "sortKeys" : provider.propertyMap.map((prop) => prop.source).slice(0,3)
                }
            });

            if (!_.find(updatedManagedConfig.objects, (o) =>
                o.name === provider.name
            )) {
                updatedManagedConfig.objects.push({
                    "name": provider.name,
                    "title": provider.name + " User Info",
                    "schema": {
                        "id" : "http://jsonschema.net",
                        "title" : provider.name,
                        "viewable" : true,
                        "type" : "object",
                        "$schema" : "http://json-schema.org/draft-03/schema",
                        "properties": provider.propertyMap.reduce(
                            function(result, prop) {
                                result[prop.source] = ({
                                    "title" : prop.source,
                                    "viewable" : true,
                                    "type" : "string",
                                    "searchable" : true,
                                    "userEditable" : false
                                });
                                return result;
                            },
                            {
                                "user" : {
                                    "viewable" : true,
                                    "searchable" : false,
                                    "type" : "relationship",
                                    "reverseRelationship" : true,
                                    "reversePropertyName" : "idps",
                                    "validate" : true,
                                    "properties" : {
                                        "_ref" : {
                                            "type" : "string"
                                        },
                                        "_refProperties" : {
                                            "type" : "object",
                                            "properties" : { }
                                        }
                                    },
                                    "resourceCollection" : [
                                        {
                                            "path" : "managed/user",
                                            "label" : "User",
                                            "query" : {
                                                "queryFilter" : "true",
                                                "fields" : [
                                                    "userName",
                                                    "givenName",
                                                    "sn"
                                                ],
                                                "sortKeys" : [
                                                    "userName"
                                                ]
                                            }
                                        }
                                    ]
                                }
                            }
                        ),
                        "order" : provider.propertyMap.map((prop) => prop.source).concat(["user"]),
                        "required": []
                    }
                });
            }
            return updatedManagedConfig;
        },
        removeManagedObjectForIDP : (provider, managedConfig) => {
            let updatedManagedConfig = _.cloneDeep(managedConfig),
                managedUser = _.find(updatedManagedConfig.objects, (o) => o.name === "user"),
                idps = managedUser.schema.properties.idps;

            // idps may be undefined if all of them have been removed
            if (idps) {
                idps.items.resourceCollection =
                _.filter(idps.items.resourceCollection, (r) => r.path !== 'managed/' + provider.name);
            }

            updatedManagedConfig.objects = _.filter(updatedManagedConfig.objects, (o) =>
                o.name !== provider.name
            );
            return updatedManagedConfig;
        },
        addIDPsProperty : (managedConfig) => {
            let updatedManagedConfig = _.cloneDeep(managedConfig),
                managedUser = _.find(updatedManagedConfig.objects, (o) => o.name === "user");

            managedUser.schema.properties.idps = {
                "title" : "Identity Providers",
                "viewable" : true,
                "searchable" : false,
                "userEditable" : false,
                "returnByDefault" : false,
                "type" : "array",
                "items" : {
                    "type" : "relationship",
                    "reverseRelationship" : true,
                    "reversePropertyName" : "user",
                    "validate" : true,
                    "properties" : {
                        "_ref" : {
                            "type" : "string"
                        },
                        "_refProperties" : {
                            "type" : "object",
                            "properties" : { }
                        }
                    },
                    "resourceCollection" : []
                }
            };

            if (_.indexOf(managedUser.schema.order, "idps") === -1) {
                managedUser.schema.order.push("idps");
            }

            return updatedManagedConfig;
        },
        removeIDPsProperty : (managedConfig) => {
            let updatedManagedConfig = _.cloneDeep(managedConfig),
                managedUser = _.find(updatedManagedConfig.objects, (o) => o.name === "user");

            managedUser.schema.order = _.filter(managedUser.schema.order, (o) => o !== "idps");

            delete managedUser.schema.properties.idps;

            return updatedManagedConfig;
        }

    });

    return new SocialConfigView();
});
