/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define, $, _, Handlebars, cron */

define("org/forgerock/openidm/ui/admin/util/Scheduler", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager"
], function(AbstractView, constants, SchedulerDelegate, uiUtils, eventManager) {
    var schedulerInstance = {},
        Scheduler = AbstractView.extend({
            template: "templates/admin/util/Scheduler.html",
            noBaseTemplate: true,
            events: {
                "keyup .complexExpression": "expressionChange",
                "change .persisted": "persistedChange",
                "click .saveSchedule": "saveSchedule",
                "click .deleteSchedule": "deleteSchedule"
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
             *          misfirePolicy - enum "fireAndProceed" or "doNothing"
             *      }
             *      invokeService -  enum "sync" for recon or "provisioner" for livesync
             *      scheduleId,
             *      onDelete - called on delete of this widget
             *  }
             */
            render: function (args, callback) {
                this.element = args.element;
                this.defaults = args.defaults || null;

                args.invokeService = args.invokeService.split(".");
                args.invokeService = $(args.invokeService).last()[0];

                if (args.invokeService && args.invokeService === "sync") {
                    this.data.recon = true;
                    this.data.scheduleType = $.t("templates.scheduler.reconciliation");

                } else if (args.invokeService && args.invokeService === "provisioner") {
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

                this.parentRender(_.bind(function() {
                    if (args.invokeService && args.invokeService === "sync") {

                        this.$el.find(".tabs").tabs({
                            activate: _.bind(function(event, ui) {
                                if (this.cron && this.$el.find(".tabs").tabs("option", "active") === 1) {
                                    this.$el.find(".complexExpression").val(this.cron.cron("convertCronVal", this.cron.cron("value")));
                                }
                            }, this)
                        });

                        this.cron = this.$el.find(".cronField").cron();
                        var defaultCronValue = this.cron.cron("value", this.cron.cron("convertCronVal", this.defaults.schedule));

                        if (defaultCronValue === false) {
                            this.$el.find(".tabs" ).tabs({ active: 1 });
                            this.$el.find(".tabs" ).tabs({ disabled: [0] });
                            this.$el.find(".complex").show();
                            this.$el.find(".complexExpression").val(this.defaults.schedule);
                        } else {
                            this.$el.find(".advancedText").hide();
                        }
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

            expressionChange: function() {
                var cronValue = this.cron.cron("value", this.cron.cron("convertCronVal", this.$el.find(".complexExpression").val()));

                if (_(cronValue).isObject()) {
                    this.$el.find(".tabs" ).tabs({ disabled: [] });
                    this.$el.find(".complex").hide();
                    this.$el.find(".advancedText").hide();
                } else {
                    this.$el.find(".tabs" ).tabs({ disabled: [0] });
                    this.$el.find(".complex").show();
                    this.$el.find(".advancedText").show();
                }
            },

            persistedChange: function() {
                this.$el.find(".misfirePolicyBlock").toggleClass('misfireHidden', !this.$el.find(".persisted").is(":checked"));
            },

            saveSchedule: function() {
                SchedulerDelegate.specificSchedule(this.scheduleId).then(_.bind(function(schedule) {
                    schedule.enabled = this.$el.find(".enabled").is(":checked");
                    schedule.misfirePolicy = this.$el.find(".misfirePolicy").val();
                    schedule.persisted = this.$el.find(".persisted").is(":checked");

                    if (this.data.scheduleType === $.t("templates.scheduler.reconciliation") && this.$el.find(".tabs").tabs("option", "active") === 1) {
                        schedule.schedule = this.$el.find(".complexExpression").val();
                    } else {
                        schedule.schedule = this.cron.cron("convertCronVal", this.cron.cron("value"));
                    }

                    SchedulerDelegate.saveSchedule(this.scheduleId, schedule).then(_.bind(function(){
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "scheduleSaved");
                    }, this));
                }, this));
            },

            deleteSchedule: function() {
                SchedulerDelegate.deleteSchedule(this.scheduleId).then(_.bind(function() {
                    this.$el.find(".schedulerBody").remove();
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "scheduleDeleted");
                    this.onDelete();
                }, this));
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

