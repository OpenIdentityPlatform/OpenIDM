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
 * Copyright 2014-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "cron",
    "gentleSelect"
], function($, _, AbstractView, constants, SchedulerDelegate, uiUtils, eventManager) {
    var schedulerInstance = {},
        Scheduler = AbstractView.extend({
            template: "templates/admin/util/Scheduler.html",
            noBaseTemplate: true,
            events: {
                "keyup .complexExpression": "expressionChange",
                "change .persisted": "persistedChange",
                "click .saveSchedule": "saveSchedule",
                "click .addSchedule": "addSchedule",
                "click .deleteSchedule": "deleteSchedule",
                "click .nav-tabs li" : "disabled"
            },
            data: {
                "id": 0
            },
            cron: null,
            scheduleId: null,

            /**
             *  args {
             *      element - jquery selector where the view is rendered
             *      defaults {  - default object of values for the schedules
             *          enabled - boolean
             *          schedule - quartz string
             *          persisted - boolean
             *          misfirePolicy - enum "fireAndProceed" or "doNothing",
             *      }
             *      invokeService -  enum "sync" for recon or "provisioner" for livesync
             *      scheduleId,
             *      onDelete - called on delete of this widget
             *      source
             *      addFunction
             *  }
             */
            render: function (args, callback) {
                var defaultCronValue, i;

                this.element = args.element;
                this.defaults = args.defaults || null;

                this.invokeService = args.invokeService.split(".");
                this.invokeService = $(this.invokeService).last()[0];

                if (this.invokeService && this.invokeService === "sync") {
                    this.data.recon = true;
                    this.data.scheduleType = $.t("templates.scheduler.reconciliation");

                } else if (this.invokeService && this.invokeService === "provisioner") {
                    this.data.liveSync = true;
                    this.data.scheduleType = $.t("templates.scheduler.liveSync");

                } else {
                    console.error("The specified schedule type does not exist.");
                    return false;
                }

                if (args.scheduleId) {
                    this.scheduleId = this.data.id = args.scheduleId;
                }

                if (args.onDelete) {
                    this.onDelete = args.onDelete;
                }

                if (args.source) {
                    this.source = this.data.source = args.source;
                    this.data.sourceCleaned = this.source.split("/").join("");
                }

                if (args.newSchedule) {
                    this.newSchedule = args.newSchedule;
                    this.data.add = true;
                }

                this.parentRender(_.bind(function() {
                    if (this.invokeService && this.invokeService === "sync") {
                        var tabs = this.$el.find(".nav-tabs li"),
                            tabBodies = this.$el.find(".tab-content .tab-pane");

                        this.$el.find('a[data-toggle="tab"]').on('shown.bs.tab', _.bind(function(e){
                            var currentTab = $(e.target).text();

                            if(currentTab === "Advanced") {
                                this.$el.find(".complexExpression").val(this.cron.cron("convertCronVal", this.cron.cron("value")));
                            }
                        }, this));

                        this.cron = this.$el.find(".cronField").cron();

                        defaultCronValue = this.cron.cron("value", this.cron.cron("convertCronVal", this.defaults.schedule));

                        if (defaultCronValue === false) {
                            this.showTab(tabs[1], tabBodies[1]);
                            this.disableTab(tabs[0]);

                            this.$el.find(".complex").show();
                            this.$el.find(".complexExpression").val(this.defaults.schedule);
                        } else {
                            this.enableTab(tabs[0]);
                            this.showTab(tabs[0], tabBodies[0]);

                            this.$el.find(".advancedText").hide();
                        }
                    } else if (this.invokeService && this.invokeService === "provisioner") {
                        for (i = 1; i < 60; i++) {
                            if (60 % i === 0) {
                                this.$el.find('.liveSyncSchedule').append("<option value='" + i + "'>" + i + "</option>");
                            }
                        }

                        if (this.defaults.liveSyncSeconds) {
                            this.defaults.liveSyncSeconds = this.defaults.liveSyncSeconds.substr(this.defaults.liveSyncSeconds.indexOf("/") + 1);
                            this.defaults.liveSyncSeconds = this.defaults.liveSyncSeconds.substr(0, this.defaults.liveSyncSeconds.indexOf("*") - 1);
                            this.defaults.liveSyncSeconds = parseInt(this.defaults.liveSyncSeconds, 10);

                            this.$el.find(".liveSyncSchedule").val(this.defaults.liveSyncSeconds);
                        }

                        this.$el.find('.liveSyncSchedule').gentleSelect({
                            title: "(n) seconds",
                            columns: 3,
                            itemWidth: 20,
                            openSpeed: 400,
                            closeSpeed: 400,
                            openEffect: "slide",
                            closeEffect: "slide",
                            hideOnMouseOut: true
                        });
                    }

                    if (this.defaults.enabled) {
                        this.$el.find(".enabled").prop("checked", true);
                    }

                    if (this.defaults.persisted) {
                        this.$el.find(".persisted").prop("checked", true).change();
                    }

                    if (this.defaults.misfirePolicy) {
                        this.$el.find(".misfirePolicy").val(this.defaults.misfirePolicy);
                    }

                    if(callback) {
                        callback();
                    }
                }, this));
            },
            showTab: function(tab, tabBody) {
                $(tab).toggleClass("active", true);
                $(tabBody).toggleClass("active", true);
            },
            disableTab: function(tab) {
                $(tab).find("a").attr("data-toggle", "");
                $(tab).toggleClass("disabled", true);
            },
            enableTab: function(tab){
                $(tab).find("a").attr("data-toggle", "tab");
                $(tab).toggleClass("disabled", false);
            },
            disabled: function(event) {
                event.preventDefault();
            },

            expressionChange: function() {
                var tabs = this.$el.find(".nav-tabs li"),
                    cronValue = this.cron.cron("value", this.cron.cron("convertCronVal", this.$el.find(".complexExpression").val()));

                if (_.isObject(cronValue)) {
                    $(tabs[0]).find("a").attr("data-toggle", "tab");
                    $(tabs[0]).toggleClass("disabled", false);
                    this.$el.find(".complex").hide();
                    this.$el.find(".advancedText").hide();
                } else {
                    $(tabs[0]).find("a").attr("data-toggle", "");
                    $(tabs[0]).toggleClass("disabled", true);
                    this.$el.find(".complex").show();
                    this.$el.find(".advancedText").show();
                }
            },

            persistedChange: function() {
                this.$el.find(".misfirePolicyBlock").toggle();
            },

            saveSchedule: function() {
                SchedulerDelegate.specificSchedule(this.scheduleId).then(_.bind(function(schedule) {
                    schedule.enabled = this.$el.find(".enabled").is(":checked");
                    schedule.misfirePolicy = this.$el.find(".misfirePolicy").val();
                    schedule.persisted = this.$el.find(".persisted").is(":checked");

                    if (this.data.scheduleType === "Reconciliation" && this.$el.find("#advancedTab").hasClass("active")) {
                        schedule.schedule = this.$el.find(".complexExpression").val();
                    } else if(this.data.scheduleType === "Reconciliation") {
                        schedule.schedule = this.cron.cron("convertCronVal", this.cron.cron("value"));
                    } else if(this.data.scheduleType === "LiveSync") {
                        schedule.schedule = "0/" + this.$el.find(".liveSyncSchedule").val() + " * * * * ?";
                    }

                    SchedulerDelegate.saveSchedule(this.scheduleId, schedule).then(_.bind(function(){
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "scheduleSaved");
                    }, this));
                }, this));
            },

            addSchedule: function() {
                if (this.newSchedule) {
                    SchedulerDelegate.addSchedule({
                        "type": "cron",
                        "invokeService": this.invokeService,
                        "schedule": "0/" + this.$el.find(".liveSyncSchedule").val() + " * * * * ?",
                        "persisted": this.$el.find(".persisted").is(":checked"),
                        "misfirePolicy": this.$el.find(".misfirePolicy").val(),
                        "enabled": this.$el.find(".enabled").is(":checked"),
                        "invokeContext": {
                            "action": "liveSync",
                            "source": this.source
                        }
                    }).then(_.bind(function(newSchedule) {
                        this.$el.find(".addSchedule").hide();
                        this.$el.find(".saveSchedule").show();

                        // Set global variables
                        this.scheduleId = newSchedule._id;

                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "scheduleCreated");
                    }, this));
                }
            },

            deleteSchedule: function() {
                if (this.$el.find(".saveSchedule:visible").length > 0) {
                    SchedulerDelegate.deleteSchedule(this.scheduleId).then(_.bind(function () {
                        this.$el.find("#schedules").empty();
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "scheduleDeleted");
                        this.onDelete(this.scheduleId, this.source, this.$el);
                    }, this));
                } else {
                    this.$el.find("#schedules").empty();
                    this.onDelete(this.scheduleId, this.source, this.$el);
                }
            }
        });

    schedulerInstance.generateScheduler = function(data) {
        var scheduler = {};
        $.extend(true, scheduler, new Scheduler());
        scheduler.render(data);
        return scheduler;
    };

    return schedulerInstance;
});
