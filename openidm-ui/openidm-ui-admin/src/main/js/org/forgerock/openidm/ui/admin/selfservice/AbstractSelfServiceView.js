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
 * Copyright 2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/selfservice/AbstractSelfServiceView", [
    "jquery",
    "underscore",
    "bootstrap",
    "handlebars",
    "form2js",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "bootstrap-dialog",
    "selectize"
], function($, _,
            bootstrap,
            handlebars,
            form2js,
            AdminAbstractView,
            ConfigDelegate,
            UiUtils,
            EventManager,
            Constants,
            BootstrapDialog,
            selectize) {
    var AbstractSelfServiceView = AdminAbstractView.extend({
        controlAllSwitch: function(event) {
            var check = $(event.target);

            this.data.enableSelfService = check.is(':checked');

            if(check.is(':checked')) {
                this.enableForm();

                this.model.surpressSave = true;
                this.$el.find(".section-check:not(:checked)").prop('checked', true).trigger("change");
                this.model.surpressSave = false;

                ConfigDelegate.createEntity(this.model.configUrl, this.model.saveConfig).then(_.bind(function() {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.msgType +"Save");
                }, this));
            } else {
                this.disableForm();

                this.model.surpressSave = true;
                this.$el.find(".section-check:checked").prop('checked', false).trigger("change");
                this.model.surpressSave = false;

                ConfigDelegate.deleteEntity(this.model.configUrl).then(_.bind(function() {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.msgType +"Delete");
                }, this));
            }
        },
        disableForm: function() {
            this.$el.find(".section-check").attr("disabled", true);
            this.$el.find(".save-config").attr("disabled", true);

            this.$el.find("#advancedTab").toggleClass("disabled", true);
            this.$el.find("#advancedTab a").removeAttr("data-toggle");

            this.$el.find("#optionTab").toggleClass("disabled", true);
            this.$el.find("#optionTab a").removeAttr("data-toggle");
        },
        enableForm: function() {
            this.$el.find(".section-check").attr("disabled", false);
            this.$el.find(".save-config").attr("disabled", false);

            this.$el.find("#advancedTab a").attr("data-toggle", "tab");
            this.$el.find("#advancedTab").toggleClass("disabled", false);

            this.$el.find("#optionTab a").attr("data-toggle", "tab");
            this.$el.find("#optionTab").toggleClass("disabled", false);
        },
        controlSectionSwitch: function(event) {
            var check = $(event.target),
                card = check.parents(".wide-card"),
                type = card.attr("data-type"),
                removeConfig = false;

            if(check.is(':checked')) {
                card.toggleClass("disabled", false);
                card.toggleClass("active", true);

                if(_.findWhere(this.model.saveConfig.stageConfigs, {"name" : type}) === undefined) {
                    this.model.saveConfig.stageConfigs.push(_.clone(_.findWhere(this.model.configDefault.stageConfigs, {"name" : type})));
                }
            } else {
                card.toggleClass("active", false);
                card.toggleClass("disabled", true);

                if(this.$el.find(".section-check:checked").length === 0 && this.$el.find(".all-check:checked").length !== 0) {
                    this.$el.find(".all-check").prop('checked', false).trigger("change");
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
            var type = $(event.target).parents(".wide-card").attr("data-type"),
                currentData = _.findWhere(this.model.saveConfig.stageConfigs, {"name" : type}),
                self = this;

            if($(event.target).parents(".checkbox").length === 0) {
                this.dialog = BootstrapDialog.show({
                    title: $.t("templates.selfservice." + this.model.serviceType + "." + type + "Title"),
                    type: BootstrapDialog.TYPE_DEFAULT,
                    size: BootstrapDialog.SIZE_WIDE,
                    message: $(handlebars.compile("{{> selfservice/" + this.model.serviceType + "/_" + type + "}}")(currentData)),
                    onshown: function (dialogRef) {
                        dialogRef.$modalBody.find('.array-selection').selectize({
                            delimiter: ',',
                            persist: false,
                            create: function (input) {
                                return {
                                    value: input,
                                    text: input
                                };
                            }
                        });
                    },
                    buttons: [
                        {
                            label: $.t('common.form.close'),
                            action: function (dialogRef) {
                                dialogRef.close();
                            }
                        },
                        {
                            label: $.t('common.form.save'),
                            cssClass: "btn-primary",
                            id: "saveUserConfig",
                            action: function (dialogRef) {
                                var formData = form2js('configDialogForm', '.', true),
                                    tempName;

                                _.extend(currentData, formData);

                                //Check for array items and set the values
                                _.each(dialogRef.$modalBody.find('input.array-selection'), function (arraySelection) {
                                    tempName = $(arraySelection).attr("data-formName");
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
        selfServiceRender: function(args, callback) {
            ConfigDelegate.readEntity(this.model.configUrl).then(_.bind(function(result){
                    $.extend(true, this.model.saveConfig, result);
                    $.extend(true, this.data.config, result);

                    this.data.hideAdvanced = false;

                    this.parentRender(_.bind(function(){
                        this.$el.find(".all-check").prop('checked', true);
                        this.$el.find(".section-check").attr('disabled', false);

                        this.model.surpressSave = true;

                        _.each(result.stageConfigs, function(stage){
                            this.$el.find("div[data-type='" +stage.name +"']").toggleClass("disabled", false);
                            this.$el.find("div[data-type='" +stage.name +"'] .section-check").prop('checked', true).trigger("change");
                        }, this);

                        this.model.surpressSave = false;
                        if(callback) {
                            callback();
                        }
                    }, this));
                }, this),
                _.bind(function(){
                    $.extend(true, this.model.saveConfig, this.model.configDefault);
                    $.extend(true, this.data.config, this.model.configDefault);

                    this.parentRender(_.bind(function(){
                        this.disableForm();

                        if(callback) {
                            callback();
                        }
                    }, this));
                },this));
        },
        saveConfig: function() {
            var formData = form2js('advancedOptions', '.', true),
                saveData = {};

            $.extend(true, saveData, this.model.saveConfig, formData);

            ConfigDelegate.updateEntity(this.model.configUrl, saveData).then(_.bind(function() {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.msgType +"Save");
            }, this));
        },
        preventTab: function(event) {
            event.preventDefault();
        }
    });

    return AbstractSelfServiceView;
});