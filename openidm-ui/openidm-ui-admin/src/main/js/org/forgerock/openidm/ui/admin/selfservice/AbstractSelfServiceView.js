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
    "handlebars",
    "form2js",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/delegates/SiteConfigurationDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/AdminUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "bootstrap-dialog",
    "selectize",
    "org/forgerock/commons/ui/common/util/AutoScroll",
    "dragula"
], function($, _,
            bootstrap,
            handlebars,
            form2js,
            AdminAbstractView,
            ConfigDelegate,
            SiteConfigurationDelegate,
            UiUtils,
            AdminUtils,
            EventManager,
            Constants,
            BootstrapDialog,
            selectize,
            AutoScroll,
            dragula) {

    var AbstractSelfServiceView = AdminAbstractView.extend({
        events: {
            "change .all-check" : "controlAllSwitch",
            "change .checkbox-slider-primary" : AdminUtils.toggleValue,
            "change .section-check" : "controlSectionSwitch",
            "change #identityServiceUrl": "updateIdentityServiceURL",
            "click .save-config" : "saveConfig",
            "click .wide-card.active" : "showDetailDialog",
            "click li.disabled a" : "preventTab"
        },
        partials : [
            "partials/selfservice/_identityServiceUrl.html",
            "partials/selfservice/_translationMap.html",
            "partials/selfservice/_translationItem.html",
            "partials/selfservice/_steps.html",
            "partials/selfservice/_advancedoptions.html",
            "partials/selfservice/_selfserviceblock.html",
            "partials/form/_basicInput.html",
            "partials/form/_basicSelectize.html",
            "partials/form/_tagSelectize.html"
        ],
        data: {
            hideAdvanced: true,
            config: {},
            configList: [],
            resources: null
        },
        addTranslation: function (e) {
            e.preventDefault();

            var translationMapGroup = $(e.target).closest(".translationMapGroup"),
                currentStageConfig = e.data.currentStageConfig,
                field = translationMapGroup.attr("field"),
                locale = translationMapGroup.find(".newTranslationLocale"),
                text = translationMapGroup.find(".newTranslationText");

            if (!_.has(currentStageConfig[field][locale.val()])) {
                currentStageConfig[field][locale.val()] = text.val();
                translationMapGroup
                    .find("ul")
                    .append(
                        handlebars.compile("{{> selfservice/_translationItem}}")({
                            locale: locale.val(),
                            text: text.val()
                        })
                    );
                text.val("");
                locale.val("").focus();
            }
        },
        deleteTranslation: function (e) {
            e.preventDefault();

            var translationMapGroup = $(e.target).closest(".translationMapGroup"),
                currentStageConfig = e.data.currentStageConfig,
                field = translationMapGroup.attr("field"),
                localeField = translationMapGroup.find(".newTranslationLocale"),
                textField = translationMapGroup.find(".newTranslationText"),
                localeValue = $(e.target).closest("li").attr("locale"),
                textValue = $(e.target).closest("li").find(".localizedText").text();

            delete currentStageConfig[field][localeValue];
            translationMapGroup.find("li[locale='"+localeValue+"']").remove();

            localeField.val(localeValue);
            textField.val(textValue).focus();

        },
        createConfig: function () {
            this.setKBAEnabled();
            return $.when(
                ConfigDelegate.createEntity(this.model.configUrl, this.orderCheck()),
                ConfigDelegate.updateEntity("ui/configuration", this.model.uiConfig)
            ).then(function () {
                SiteConfigurationDelegate.updateConfiguration(function () {
                    EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);
                });
            });
        },
        deleteConfig: function () {
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
        controlAllSwitch: function(event) {
            var check = $(event.target),
                tempConfig;

            this.data.enableSelfService = check.is(":checked");

            if(check.is(":checked")) {
                this.enableForm();

                this.model.surpressSave = true;

                _.each(this.$el.find(".wide-card"), function(card) {
                    tempConfig = _.find(this.data.configList, function(config) {
                        return $(card).attr("data-type") === config.type;
                    }, this);

                    if (tempConfig.enabledByDefault) {
                        $(card).find(".section-check").prop("checked", true).trigger("change");
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
        },
        disableForm: function() {
            this.$el.find(".section-check").prop("disabled", true);
            this.$el.find(".save-config").prop("disabled", true);
            this.$el.find("#identityServiceUrl").prop("disabled", true);
            this.$el.find(".self-service-card").toggleClass("disabled", true);


            this.$el.find("#advancedTab").toggleClass("disabled", true);
            this.$el.find("#advancedTab a").removeAttr("data-toggle");

            this.$el.find("#optionTab a").trigger("click");

            this.$el.find("#optionTab").toggleClass("disabled", true);
            this.$el.find("#optionTab a").removeAttr("data-toggle");

            this.$el.find(".all-check").val(false);
            this.$el.find(".all-check").prop("checked", false);
        },
        enableForm: function() {
            this.$el.find(".section-check").prop("disabled", false);
            this.$el.find(".save-config").prop("disabled", false);
            this.$el.find("#identityServiceUrl").prop("disabled", false);

            this.$el.find(".notTogglable").toggleClass("disabled", false);

            this.$el.find("#advancedTab a").attr("data-toggle", "tab");
            this.$el.find("#advancedTab").toggleClass("disabled", false);

            this.$el.find("#optionTab a").attr("data-toggle", "tab");
            this.$el.find("#optionTab").toggleClass("disabled", false);

            this.$el.find(".all-check").val(true);
            this.$el.find(".all-check").prop("checked", true);

            this.updateIdentityServiceURL();
        },
        controlSectionSwitch: function(event) {
            var check = $(event.target),
                card = check.parents(".wide-card"),
                type = card.attr("data-type"),
                removeConfig = false,
                orderPosition,
                configPosition = _.findIndex(this.model.configDefault.stageConfigs, function (defaultStage) {
                    return defaultStage.name === type;
                });

            if(check.is(":checked")) {
                card.toggleClass("disabled", false);
                card.toggleClass("active", true);

                orderPosition = this.$el.find(".selfservice-holder .wide-card:not(.disabled)").index(card);

                if(_.filter(this.model.saveConfig.stageConfigs, {"name" : type}).length === 0) {
                    this.model.saveConfig.stageConfigs.splice(orderPosition, 0, _.clone(this.model.configDefault.stageConfigs[configPosition]));
                }
            } else {
                card.toggleClass("active", false);
                card.toggleClass("disabled", true);

                if(this.$el.find(".section-check:checked").length === 0 && this.$el.find(".all-check:checked").length !== 0) {
                    this.$el.find(".all-check").prop("checked", false).trigger("change");
                    removeConfig = true;
                }

                this.model.saveConfig.stageConfigs = _.reject(this.model.saveConfig.stageConfigs, function(stage) {
                    return stage.name === type;
                });
            }

            if(!this.model.surpressSave && !removeConfig) {
                this.saveConfig();
            }
        },

        showDetailDialog: function(event) {
            var el,
                type = $(event.target).parents(".wide-card").attr("data-type"),
                editable = $(event.target).parents(".wide-card").attr("data-editable"),
                currentData = _.filter(this.model.saveConfig.stageConfigs, {"name" : type})[0],
                defaultConfig = _.filter(this.data.configList, { "type": type })[0],
                orderPosition = $(event.target).closest(".self-service-card.active").index(),
                self = this;

            if ($(event.target).hasClass("self-service-card")) {
                el = $(event.target);
            } else {
                el = $(event.target).closest(".self-service-card");
            }

            if (el.hasClass("disabled")) {
                return false;
            }

            // If there is no data for the selected step and icon property is present, icon property indicates the step is mandatory
            if (!currentData && defaultConfig.icon.length > 0 ) {
                defaultConfig = _.clone(_.filter(this.model.configDefault.stageConfigs, {"name" : type})[0]);

                if (_.filter(this.model.saveConfig.stageConfigs, {"name" : type}).length === 0) {
                    this.model.saveConfig.stageConfigs.splice(orderPosition, 0, defaultConfig);
                }

                currentData = _.filter(this.model.saveConfig.stageConfigs, {"name" : type})[0];
            }

            currentData.identityServiceProperties = this.data.identityServiceProperties;

            if($(event.target).parents(".checkbox").length === 0 && editable === "true") {
                this.dialog = BootstrapDialog.show({
                    title: $.t("templates.selfservice." + this.model.serviceType + "." + type + "Title"),
                    type: BootstrapDialog.TYPE_DEFAULT,
                    size: BootstrapDialog.SIZE_WIDE,
                    message: $(handlebars.compile("{{> selfservice/_" + type + "}}")(currentData)),
                    onshown: _.bind(function (dialogRef) {
                        dialogRef.$modalBody.find(".basic-selectize-field").selectize({
                            "create": true,
                            "persist": false,
                            "allowEmptyOption": true
                        });
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

                        dialogRef.$modalBody.on("submit", "form", function (e) {
                            e.preventDefault();
                            return false;
                        });
                        dialogRef.$modalBody.on("click", ".translationMapGroup button.add",
                            {currentStageConfig: currentData},
                            _.bind(this.addTranslation, this));

                        dialogRef.$modalBody.on("click", ".translationMapGroup button.delete",
                            {currentStageConfig: currentData},
                            _.bind(this.deleteTranslation, this));
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
                            action: function (dialogRef) {
                                var formData = form2js("configDialogForm", ".", true),
                                    tempName;

                                _.extend(currentData, formData);

                                //Check for array items and set the values
                                _.each(dialogRef.$modalBody.find("input.array-selection"), function (arraySelection) {
                                    tempName = $(arraySelection).prop("name");
                                    currentData[tempName] = $(arraySelection)[0].selectize.getValue().split(",");
                                }, this);

                                self.saveConfig();

                                dialogRef.close();
                            }
                        }
                    ]
                });
            }
        },

        getResources: function() {
            var resourcePromise = $.Deferred();

            if (!this.data.resources) {
                AdminUtils.getAvailableResourceEndpoints().then(_.bind(function (resources) {
                    resourcePromise.resolve(resources);
                }, this));
            } else {
                resourcePromise.resolve(this.data.resources);
            }

            return resourcePromise.promise();
        },

        getSelfServiceConfig: function() {
            var promise = $.Deferred();

            ConfigDelegate.readEntity(this.model.configUrl).always(function(result) {
                promise.resolve(result);
            });

            return promise.promise();
        },

        selfServiceRender: function(args, callback) {
            var disabledList,
                configList = [];

            this.data.defaultIdentityServiceURL = "";

            $.when(
                this.getResources(),
                ConfigDelegate.readEntity("ui/configuration"),
                this.getSelfServiceConfig()

            ).then(_.bind(function(resources, uiConfig, selfServiceConfig) {
                this.data.resources = resources;
                this.model.uiConfig = uiConfig;

                if (selfServiceConfig) {

                    $.extend(true, this.model.saveConfig, selfServiceConfig);
                    $.extend(true, this.data.config, selfServiceConfig);

                    this.data.hideAdvanced = false;

                    _.each(this.data.configList, function (config, pos) {
                        config.index = pos;
                    });

                    disabledList = _.filter(this.data.configList, function (config) {
                        var filterCheck = true;

                        _.each(selfServiceConfig.stageConfigs, function (stage) {
                            if (stage.name === config.type) {
                                filterCheck = false;
                            }
                        }, this);

                        return filterCheck;
                    });

                    _.each(selfServiceConfig.stageConfigs, function (stage) {
                        _.each(this.data.configList, function (config) {
                            if (stage.name === config.type) {
                                configList.push(config);
                            }
                        }, this);
                    }, this);

                    _.each(disabledList, function (config) {
                        configList.splice(config.index, 0, config);
                    }, this);

                    this.data.configList = configList;
                    this.data.enableSelfService = true;

                    // The value of the first identity service url save location
                    this.data.defaultIdentityServiceURL = _.filter(selfServiceConfig.stageConfigs, {
                        "name": this.model.identityServiceURLSaveLocations[0].stepName
                    })[0][this.model.identityServiceURLSaveLocations[0].stepProperty];

                    this.parentRender(_.bind(function () {
                        this.$el.find(".all-check").prop("checked", true);
                        this.$el.find(".all-check").val(true);
                        this.$el.find(".section-check").prop("disabled", false);

                        this.updateIdentityServiceURL();

                        this.model.surpressSave = true;
                        this.setSortable();

                        _.each(selfServiceConfig.stageConfigs, function (stage) {
                            this.$el.find(".wide-card[data-type='" + stage.name + "']").toggleClass("disabled", false);
                            this.$el.find(".wide-card[data-type='" + stage.name + "'] .section-check").prop("checked", true).trigger("change");
                        }, this);

                        this.model.surpressSave = false;

                        if (callback) {
                            callback();
                        }
                    }, this));

                } else {
                    $.extend(true, this.model.saveConfig, this.model.configDefault);
                    $.extend(true, this.data.config, this.model.configDefault);

                    this.data.enableSelfService = false;

                    this.parentRender(_.bind(function () {
                        this.disableForm();
                        this.setSortable();

                        if (callback) {
                            callback();
                        }
                    }, this));
                }

            }, this));
        },

        setSortable: function() {
            var start,
                dragDropInstance = dragula([$(".selfservice-holder")[0]]);

            dragDropInstance.on("drag", _.bind(function(el, container) {
                start = this.$el.find(".selfservice-holder .card").not(".disabled").index($(el));

                AutoScroll.startDrag();
            }, this));

            dragDropInstance.on("dragend", _.bind(function(el) {
                var stop = this.$el.find(".selfservice-holder .card").not(".disabled").index($(el));

                AutoScroll.endDrag();

                this.setOrder(start, stop);
            }, this));
        },

        setOrder: function(start, end) {
            var movedElement = this.model.saveConfig.stageConfigs[start];

            if(start !== end) {
                this.model.saveConfig.stageConfigs.splice(start, 1);
                this.model.saveConfig.stageConfigs.splice(end, 0, movedElement);
            }

            if(!_.isUndefined(movedElement)){
                this.saveConfig();
            }
        },

        orderCheck: function() {
            var tempConfig = {},
                stageOrder = [];

            $.extend(true, tempConfig, this.model.saveConfig);

            _.each(this.$el.find(".selfservice-holder .wide-card"), function(config) {
                _.each(tempConfig.stageConfigs, function(stage){
                    if(stage.name === $(config).attr("data-type")) {
                        stageOrder.push(_.clone(stage));
                    }
                }, this);
            }, this);

            tempConfig.stageConfigs = stageOrder;

            return tempConfig;
        },

        updateIdentityServiceURL: function(e) {
            this.data.defaultIdentityServiceURL = this.$el.find("#identityServiceUrl").val();

            AdminUtils.findPropertiesList(this.data.defaultIdentityServiceURL.split("/")).then(_.bind(function(properties) {
                this.data.identityServiceProperties = _.chain(properties).keys().sortBy().value();
            }, this));

            if (e) {
                this.saveConfig();
            }
        },

        saveConfig: function() {
            var formData = form2js("advancedOptions", ".", true),
                saveData = {},
                tempStepConfig;

            // For each key/property location that the identity service URL should be saved
            // find the corresponding location and set it.
            _.each(this.model.identityServiceURLSaveLocations, function(data) {
                tempStepConfig = _.filter(this.model.saveConfig.stageConfigs, {"name": data.stepName})[0];
                tempStepConfig[data.stepProperty] = this.data.defaultIdentityServiceURL;
            }, this);

            $.extend(true, saveData, this.model.saveConfig, formData);

            _.each(saveData.stageConfigs, function(step) {
                if (_.has(step, "identityServiceProperties")) {
                    delete step.identityServiceProperties;
                }
            });

            this.setKBAEnabled();
            return $.when(
                ConfigDelegate.updateEntity(this.model.configUrl, saveData),
                ConfigDelegate.updateEntity("ui/configuration", this.model.uiConfig)
            ).then(_.bind(function() {
                SiteConfigurationDelegate.updateConfiguration(function () {
                    EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);
                });
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.msgType +"Save");
            }, this));
        },
        setKBAEnabled: function () {
            this.model.uiConfig.configuration.kbaEnabled =
                !!this.model.uiConfig.configuration.kbaDefinitionEnabled ||
                !!this.model.uiConfig.configuration.kbaVerificationEnabled;
        },
        preventTab: function(event) {
            event.preventDefault();
        }
    });

    return AbstractSelfServiceView;
});
