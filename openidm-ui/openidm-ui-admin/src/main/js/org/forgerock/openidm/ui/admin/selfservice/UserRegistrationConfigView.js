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
    "handlebars",
    "form2js",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/delegates/SiteConfigurationDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/AdminUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/selfservice/SelfServiceStageDialogView",
    "org/forgerock/openidm/ui/common/delegates/SocialDelegate"
], function($, _,
            handlebars,
            form2js,
            AdminAbstractView,
            ConfigDelegate,
            SiteConfigurationDelegate,
            UiUtils,
            AdminUtils,
            EventManager,
            Constants,
            SelfServiceStageDialogView,
            SocialDelegate) {
    var UserRegistrationConfigView = AdminAbstractView.extend({
        template: "templates/admin/selfservice/UserRegistrationConfigTemplate.html",
        events: {
            "click .all-check" : "controlAllSwitch",
            "change .section-check" : "controlSectionSwitch",
            "click .save-config" : "saveConfig",
            "click .wide-card.active" : "showDetailDialog",
            "click li.disabled a" : "preventTab",
            "click #configureCaptcha": "configureCaptcha"
        },
        partials: [
            "partials/_toggleIconBlock.html",
            "partials/selfservice/_advancedoptions.html"
        ],
        model: {
            emailServiceAvailable: false,
            surpressSave: false,
            uiConfigurationParameter: "selfRegistration",
            configUrl: "selfservice/registration",
            msgType: "selfServiceUserRegistration",
            codeMirrorConfig: {
                lineNumbers: true,
                autofocus: false,
                viewportMargin: Infinity,
                theme: "forgerock",
                mode: "xml",
                htmlMode: true,
                lineWrapping: true
            },
            configList: [],
            "configDefault": {
                "stageConfigs" : [
                    {
                        "name" : "termsAndConditions",
                        "termsTranslations" : {
                            "en" : "Some fake terms",
                            "fr" : "More fake terms"
                        }
                    },
                    {
                        "name" : "captcha",
                        "recaptchaSiteKey": "",
                        "recaptchaSecretKey": "",
                        "recaptchaUri" : "https://www.google.com/recaptcha/api/siteverify"
                    },
                    {
                        "name" : "userDetails",
                        "identityEmailField" : "mail"
                    },
                    {
                        "name" : "emailValidation",
                        "identityEmailField" : "mail",
                        "emailServiceUrl": "external/email",
                        "from" : "info@admin.org",
                        "subject" : "Register new account",
                        "mimeType" : "text/html",
                        "subjectTranslations" : {
                            "en" : "Register new account",
                            "fr" : "Créer un nouveau compte"
                        },
                        "messageTranslations" : {
                            "en" : "<h3>This is your registration email.</h3><h4><a href=\"%link%\">Email verification link</a></h4>",
                            "fr" : "<h3>Ceci est votre mail d'inscription.</h3><h4><a href=\"%link%\">Lien de vérification email</a></h4>"
                        },
                        "verificationLinkToken" : "%link%",
                        "verificationLink" : "https://localhost:8443/#register/"
                    },
                    {
                        "name" : "kbaSecurityAnswerDefinitionStage",
                        "numberOfAnswersUserMustSet": 1,
                        "kbaConfig" : null
                    },
                    {
                        "name" : "selfRegistration",
                        "identityServiceUrl" : "managed/user"
                    }
                ],
                "snapshotToken" : {
                    "type" : "jwt",
                    "keyPairAlgorithm" : "RSA",
                    "keyPairSize" : 1024,
                    "jweAlgorithm" : "RSAES_PKCS1_V1_5",
                    "encryptionMethod" : "A128CBC_HS256",
                    "jwsAlgorithm" : "HS256",
                    "tokenExpiry" : 1800
                },
                "storage" : "stateless"
            },
            "saveConfig": {},
            data: {
                config: {},
                emailRequired: false
            }
        },

        render: function(args, callback) {
            //List broken for subsection consumption by the UI
            this.data.storageLookup = [{
                type: "userDetails",
                name: $.t("templates.selfservice.userDetails.identityResource"),
                details: $.t("templates.selfservice.userDetailsHelp"),
                editable: true,
                togglable: false,
                displayIcon: "database"
            }];

            this.data.userverification = [{
                type: "captcha",
                name: $.t("templates.selfservice.user.captchaTitle"),
                details: $.t("templates.selfservice.captcha.description"),
                editable: true,
                togglable: true,
                displayIcon: "shield"
            }, {
                type: "emailValidation",
                name: $.t("templates.selfservice.emailValidation"),
                details: $.t("templates.selfservice.emailValidationDescription"),
                editable: true,
                togglable: true,
                displayIcon: "envelope"
            }];

            this.data.securityQuestions = [{
                type: "kbaSecurityAnswerDefinitionStage",
                name: $.t("templates.selfservice.kbaSecurityAnswerDefinitionStageTitle"),
                details: $.t("templates.selfservice.kbaSecurityAnswerDefinitionStageHelp"),
                editable: false,
                togglable: true,
                displayIcon: "list-ol"
            }];

            this.data.licensingAndConsent = [{
                type: "termsAndConditions",
                name: $.t("templates.selfservice.termsAndConditions.title"),
                details: $.t("templates.selfservice.termsAndConditions.details"),
                editable: true,
                togglable: true,
                displayIcon: "file-text-o"
            }];

            //Master config list for controlling various states such as what is editable and what is enabled by default when turned on
            this.model.configList = [{
                type : "termsAndConditions",
                toggledOn: true
            }, {
                type: "captcha",
                enabledByDefault: false,
                toggledOn: false
            }, {
                type: "userDetails",
                enabledByDefault: true,
                toggledOn: true
            }, {
                type: "emailValidation",
                enabledByDefault: true,
                toggledOn: false
            }, {
                type: "kbaSecurityAnswerDefinitionStage",
                enabledByDefault: true,
                toggledOn: false
            }, {
                type : "selfRegistration",
                toggledOn: true
            }, {
                type: "socialUserDetails",
                enabledByDefault: false,
                toggledOn: true
            }];


            $.when(
                this.getResources(),
                ConfigDelegate.readEntity("ui/configuration"),
                ConfigDelegate.readEntityAlways(this.model.configUrl),
                ConfigDelegate.readEntityAlways("external.email"),
                SocialDelegate.providerList()
            ).then(_.bind(function(resources, uiConfig, selfServiceConfig, emailConfig, availableProviders) {
                this.model.emailServiceAvailable = !_.isUndefined(emailConfig) && _.has(emailConfig, "host");
                this.model.resources = resources;
                this.model.uiConfig = uiConfig;

                _.each(availableProviders.providers, (provider) => {
                    switch(provider.name) {
                        case "google":
                            provider.displayIcon = "google";
                            break;
                    }
                });

                this.data.socialProviders = {
                    providerList : availableProviders.providers,
                    type: "socialUserDetails",
                    editable: false,
                    togglable: true
                };

                if (selfServiceConfig) {
                    $.extend(true, this.model.saveConfig, selfServiceConfig);

                    this.data.enableSelfService = true;

                    this.parentRender(_.bind(function () {
                        this.$el.find(".all-check").prop("checked", true);
                        this.$el.find(".section-check").prop("disabled", false);

                        this.model.surpressSave = true;

                        //Set UI to stages to current active and available stages
                        _.each(selfServiceConfig.stageConfigs, function (stage) {
                            this.$el.find(".wide-card[data-type='" + stage.name + "']").toggleClass("disabled", false);

                            if(stage.name === "userDetails") {
                                this.$el.find(".wide-card[data-type='userDetails']").toggleClass("active", true);
                            } else if (stage.class === "org.forgerock.openidm.selfservice.stage.SocialUserDetailsConfig") {
                                this.$el.find(".wide-card[data-type='socialUserDetails'] .section-check").prop("checked", true).trigger("change");
                            } else {
                                this.$el.find(".wide-card[data-type='" + stage.name + "'] .section-check").prop("checked", true).trigger("change");

                            }
                        }, this);
                        this.showCaptchaWarning(this.model.saveConfig.stageConfigs);

                        this.model.surpressSave = false;

                        if (callback) {
                            callback();
                        }
                    }, this));

                } else {
                    $.extend(true, this.model.saveConfig, this.model.configDefault);

                    this.data.enableSelfService = false;

                    this.parentRender(_.bind(function () {
                        this.disableForm();
                        this.showCaptchaWarning(false);

                        if (callback) {
                            callback();
                        }
                    }, this));
                }
            }, this));
        },

        showDetailDialog: function(event) {
            var el,
                currentData = {},
                cardDetails;

            if ($(event.target).hasClass("self-service-card")) {
                el = $(event.target);
            } else {
                el = $(event.target).closest(".self-service-card");
            }

            cardDetails = this.getCardDetails(el);

            if (cardDetails.disabled && !this.$el.find(".all-check").val()) {
                return false;
            }

            if($(event.target).parents(".checkbox").length === 0 && cardDetails.editable === "true") {
                if(cardDetails.type === "userDetails") {
                    _.each(this.model.saveConfig.stageConfigs, (stage) => {
                        if(stage.name === "userDetails") {
                            currentData.identityEmailField = stage.identityEmailField;
                        } else if (stage.name === "selfRegistration") {
                            currentData.identityServiceUrl = stage.identityServiceUrl;
                            currentData.identityServiceOptions = this.model.resources;
                        }
                    });

                    AdminUtils.findPropertiesList(currentData.identityServiceUrl.split("/")).then(_.bind(function(properties) {
                        currentData.identityEmailOptions = _.chain(properties).keys().sortBy().value();

                        this.loadSelfServiceDialog(el, cardDetails.type, currentData);

                    }, this));
                } else {
                    currentData = _.filter(this.model.saveConfig.stageConfigs, {"name" : cardDetails.type})[0];

                    this.loadSelfServiceDialog(el, cardDetails.type, currentData);
                }
            }
        },

        loadSelfServiceDialog(el, type, data) {
            SelfServiceStageDialogView.render({
                "element" : el,
                "type" : type,
                "data" : data,
                "saveCallback" : () => {this.saveConfig();},
                "stageConfigs" : this.model.saveConfig.stageConfigs
            });
        },

        controlAllSwitch: function() {
            var check = this.$el.find(".all-check"),
                tempConfig,
                emailCheck;

            this.data.enableSelfService = check.is(":checked");

            if (this.data.enableSelfService) {
                this.enableForm();

                this.model.surpressSave = true;

                _.each(this.$el.find(".wide-card"), function(card) {
                    tempConfig = _.find(this.model.configList, function(config) {
                        return $(card).attr("data-type") === config.type;
                    }, this);

                    if (tempConfig.enabledByDefault) {
                        if(tempConfig.type === "userDetails") {
                            $(card).toggleClass("disabled");
                            $(card).toggleClass("active", true);
                        } else {
                            $(card).find(".section-check").prop("checked", true).trigger("change");
                        }
                    } else {
                        this.model.saveConfig.stageConfigs = _.reject(this.model.saveConfig.stageConfigs, function(stage) {
                            return stage.name === $(card).attr("data-type");
                        });
                    }
                }, this);

                this.model.surpressSave = false;

                this.model.uiConfig.configuration[this.model.uiConfigurationParameter] = true;

                this.createConfig().then(_.bind(function() {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.msgType +"Save");
                }, this));
            } else {
                this.disableForm();

                this.model.surpressSave = true;
                this.$el.find(".section-check:checked").prop("checked", false).trigger("change");
                this.model.surpressSave = false;
                this.model.uiConfig.configuration[this.model.uiConfigurationParameter] = false;

                this.deleteConfig().then(_.bind(function() {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.msgType +"Delete");
                }, this));
            }

            emailCheck = this.showHideEmailWarning(this.model.saveConfig.stageConfigs, this.model.emailServiceAvailable);

            this.data.emailRequired = emailCheck.emailRequired;
            this.$el.find("#emailStepWarning").toggle(emailCheck.showWarning);
        },

        controlSectionSwitch: function(event) {
            var check = $(event.target),
                card = check.parents(".wide-card"),
                cardDetails = this.getCardDetails(card),
                removeConfig = false,
                emailCheck;

            if(check.is(":checked")) {
                this.model.saveConfig.stageConfigs = this.setSwitchOn(card, this.model.saveConfig.stageConfigs, this.model.configList, this.model.configDefault.stageConfigs, cardDetails.type);
            } else {
                this.model.saveConfig.stageConfigs = this.setSwitchOff(card, this.model.saveConfig.stageConfigs, this.model.configList, cardDetails.type);
            }

            emailCheck = this.showHideEmailWarning(this.model.saveConfig.stageConfigs, this.model.emailServiceAvailable);

            this.data.emailRequired = emailCheck.emailRequired;
            this.$el.find("#emailStepWarning").toggle(emailCheck.showWarning);

            this.showCaptchaWarning(this.model.saveConfig.stageConfigs);

            if (!this.model.surpressSave && !removeConfig) {
                this.saveConfig();
            }
        },

        /**
         * @param stageConfigs {Array.<Object>}
         * @param emailServiceAvailable {boolean}
         *
         * @returns {{emailRequired: boolean, showWarning: boolean}}
         */
        showHideEmailWarning: function(stageConfigs, emailServiceAvailable) {
            var emailRequired = false,
                show = false;

            _.each(stageConfigs, (stage) => {
                if ("emailValidation" === stage.name) {
                    emailRequired = true;
                }
            });

            if (emailRequired && !emailServiceAvailable) {
                show = true;
            }

            return {
                "emailRequired": emailRequired,
                "showWarning": show
            };
        },

        showCaptchaWarning: function(stageConfigs) {
            if (typeof stageConfigs === "boolean") {
                this.$el.find("#captchaNotConfiguredWarning").toggle(stageConfigs);
            } else {
                this.$el.find("#captchaNotConfiguredWarning").toggle(this.checkCaptchaConfigs(stageConfigs));
            }
        },

        checkCaptchaConfigs: function(stageConfigs) {
            var captchaStage = stageConfigs.filter(function(value) { return value.name === "captcha";})[0];
            if (captchaStage && (!captchaStage.recaptchaSiteKey || ! captchaStage.recaptchaSecretKey)) {
                return true;
            } else {
                return false;
            }
        },

        configureCaptcha: function(event) {
            event.preventDefault();
            this.$el.find("[data-type='captcha']").trigger("click");
        },

        /**
         * @param card {object}
         * @param stages {Array.<Object>}
         * @param configList {Array.<Object>}
         * @param defaultStages {Array.<Object>}
         * @param type {string}
         *
         * @returns {Array.<Object>}
         *
         * This function updates the html to the on status for a stage and returns the updated stage list
         */
        setSwitchOn: function(card, stages, configList, defaultStages, type) {
            var configItem,
                saveOrder,
                defaultLocation;

            card.toggleClass("disabled", false);
            card.toggleClass("active", true);

            configItem = _.find(configList, function(config) { return config.type ===  type; });

            if(configItem) {
                configItem.toggledOn = true;
            }

            if(type !== "socialUserDetails") {
                if(_.filter(stages, {"name" : type}).length === 0) {
                    saveOrder = this.findPosition(configList, type);
                    defaultLocation = _.findIndex(defaultStages, function (stage) {
                        return stage.name === type;
                    });

                    stages.splice(saveOrder, 0, _.clone(defaultStages[defaultLocation]));
                }
            } else {
                stages[0].class = "org.forgerock.openidm.selfservice.stage.SocialUserDetailsConfig";
                delete stages[0].name;

                this.$el.find(".wide-card[data-type='userDetails']").toggleClass("active", false);
                this.$el.find(".wide-card[data-type='userDetails']").toggleClass("disabled", true);
            }

            return stages;
        },

        /**
         * @param card {object}
         * @param stages {Array.<Object>}
         * @param configList {Array.<Object>}
         * @param type {string}
         *
         * @returns {Array.<Object>}
         *
         * This function updates the html to the off status for a stage and returns the updated stage list
        */
        setSwitchOff: function(card, stages, configList, type) {
            var configItem;

            card.toggleClass("active", false);
            card.toggleClass("disabled", true);

            configItem = _.find(configList, function(config) { return config.type ===  type; });

            if(configItem) {
                configItem.toggledOn = false;
            }

            if(type !== "socialUserDetails") {
                return _.reject(stages, function (stage) {
                    return stage.name === type;
                });
            } else {
                stages[0].name = "userDetails";
                delete stages[0].class;

                this.$el.find(".wide-card[data-type='userDetails']").toggleClass("active", true);
                this.$el.find(".wide-card[data-type='userDetails']").toggleClass("disabled", false);

                return stages;
            }
        },

        /**
         * @param orderedList {Array.<Object>}
         * @param type {string}
         *
         * This function will find the correct position based on what stages are turned on vs its default recommended location
         */
        findPosition : function(orderedList, type) {
            var position = 0;

            _.each(orderedList, (item) => {
                if(item.type === type) {
                    return false;
                }

                if(item.toggledOn) {
                    position++;
                }
            });

            return position;
        },

        /**
         *
         * @param card {object}
         * @returns {{type: string, editable: boolean, disabled: boolean}}
         */
        getCardDetails: function(card) {
            return {
                "type" : card.attr("data-type"),
                "editable" : card.attr("data-editable"),
                "disabled" : card.hasClass("disabled")
            };
        },

        setKBADefinitionEnabled: function () {
            this.model.uiConfig.configuration.kbaDefinitionEnabled = !!_.find(this.model.saveConfig.stageConfigs, function (stage) {
                return stage.name === "kbaSecurityAnswerDefinitionStage";
            });
        },

        setKBAEnabled: function () {
            this.model.uiConfig.configuration.kbaEnabled =
                !!this.model.uiConfig.configuration.kbaDefinitionEnabled ||
                !!this.model.uiConfig.configuration.kbaVerificationEnabled;
        },

        getResources: function() {
            var resourcePromise = $.Deferred();

            if (!this.model.resources) {
                AdminUtils.getAvailableResourceEndpoints().then(_.bind(function (resources) {
                    resourcePromise.resolve(resources);
                }, this));
            } else {
                resourcePromise.resolve(this.model.resources);
            }

            return resourcePromise.promise();
        },

        createConfig: function () {
            this.setKBADefinitionEnabled();
            this.setKBAEnabled();

            return $.when(
                ConfigDelegate.createEntity(this.model.configUrl, this.model.saveConfig),
                ConfigDelegate.updateEntity("ui/configuration", this.model.uiConfig)
            ).then(function () {
                SiteConfigurationDelegate.updateConfiguration(function () {
                    EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);
                });
            });
        },

        deleteConfig: function () {
            this.setKBADefinitionEnabled();
            this.setKBAEnabled();

            return $.when(
                ConfigDelegate.deleteEntity(this.model.configUrl),
                ConfigDelegate.updateEntity("ui/configuration", this.model.uiConfig)
            ).then(function () {
                SiteConfigurationDelegate.updateConfiguration(function () {
                    EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);
                });
            });
        },

        saveConfig: function() {
            var formData = form2js("advancedOptions", ".", true),
                saveData = {};

            this.setKBADefinitionEnabled();
            this.setKBAEnabled();

            $.extend(true, saveData, this.model.saveConfig, formData);

            return $.when(
                ConfigDelegate.updateEntity(this.model.configUrl, saveData),
                ConfigDelegate.updateEntity("ui/configuration", this.model.uiConfig)
            ).then(_.bind(function() {
                SiteConfigurationDelegate.updateConfiguration(function () {
                    EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);
                });
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.msgType +"Save");
                this.showCaptchaWarning(this.model.saveConfig.stageConfigs);
            }, this));
        },

        disableForm: function() {
            this.$el.find(".section-check").prop("disabled", true);
            this.$el.find(".save-config").prop("disabled", true);
            this.$el.find("#identityServiceUrl").prop("disabled", true);
            this.$el.find(".self-service-card").toggleClass("disabled", true);
            this.$el.find(".selfservice-holder").toggleClass("disabled", true);

            this.$el.find("#advancedTab").toggleClass("disabled", true);
            this.$el.find("#advancedTab a").removeAttr("data-toggle");

            this.$el.find("#optionTab a").trigger("click");

            this.$el.find("#optionTab").toggleClass("disabled", true);
            this.$el.find("#optionTab a").removeAttr("data-toggle");

            this.$el.find(".all-check").val(false);
            this.$el.find(".all-check").prop("checked", false);

            this.$el.find("#emailStepWarning").hide();
        },

        enableForm: function() {
            this.$el.find(".section-check").prop("disabled", false);
            this.$el.find(".save-config").prop("disabled", false);
            this.$el.find("#identityServiceUrl").prop("disabled", false);
            this.$el.find(".selfservice-holder").toggleClass("disabled", false);

            this.$el.find(".notTogglable").toggleClass("disabled", false);

            this.$el.find("#advancedTab a").attr("data-toggle", "tab");
            this.$el.find("#advancedTab").toggleClass("disabled", false);

            this.$el.find("#optionTab a").attr("data-toggle", "tab");
            this.$el.find("#optionTab").toggleClass("disabled", false);

            this.$el.find(".all-check").val(true);
            this.$el.find(".all-check").prop("checked", true);
        },

        switchToUserDetails: function(registrationConfig) {
            delete registrationConfig.stageConfigs[0].class;
            registrationConfig.stageConfigs[0].name = "userDetails";

            ConfigDelegate.updateEntity("selfservice/registration", registrationConfig).then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "selfServiceUserRegistrationSave");
            });
        },

        preventTab: function(event) {
            event.preventDefault();
        }
    });

    return new UserRegistrationConfigView();
});
