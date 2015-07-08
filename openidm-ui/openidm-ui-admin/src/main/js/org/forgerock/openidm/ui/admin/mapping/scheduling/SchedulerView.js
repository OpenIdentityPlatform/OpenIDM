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

/*global define, $, _, Handlebars */

define("org/forgerock/openidm/ui/admin/mapping/scheduling/SchedulerView", [
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate",
    "org/forgerock/openidm/ui/admin/util/Scheduler"
], function(MappingAdminAbstractView,
            eventManager,
            constants,
            ConfigDelegate,
            SchedulerDelegate,
            Scheduler) {

    var ScheduleView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/scheduling/SchedulerTemplate.html",
        element: "#schedulerView",
        noBaseTemplate: true,
        events: {
            "click #addNew": "addReconciliation"
        },
        model: {
            mapping: {}
        },

        render: function (args, callback) {
            var promises = [],
                tempPromises;

            this.model.mapping = _.omit(this.getCurrentMapping(), "recon");

            this.parentRender(_.bind(function() {

                SchedulerDelegate.availableSchedules().then(_.bind(function (schedules) {
                    if (schedules.result.length > 0) {
                        _(schedules.result).each(function (scheduleId) {
                            tempPromises = SchedulerDelegate.specificSchedule(scheduleId._id);

                            promises.push(tempPromises);

                            tempPromises.then(_.bind(function (schedule) {
                                if (schedule.invokeService.indexOf("sync") >= 0 && schedule.invokeContext.mapping === this.model.mapping.name) {
                                    Scheduler.generateScheduler({
                                        "element": this.$el.find("#scheduleList"),
                                        "defaults": {
                                            enabled: schedule.enabled,
                                            schedule: schedule.schedule,
                                            persisted: schedule.persisted,
                                            misfirePolicy: schedule.misfirePolicy
                                        },
                                        "onDelete": this.reconDeleted,
                                        "invokeService": schedule.invokeService,
                                        "scheduleId": scheduleId._id
                                    });
                                    this.$el.find("#addNew").hide();
                                }


                            }, this));
                        }, this);
                    }

                    if(promises.length !== 0) {
                        $.when.apply($, promises).then(_.bind(function () {
                            this.$el.find(".schedule-input-body").show();

                            if (callback) {
                                callback();
                            }
                        }, this));
                    } else {
                        this.$el.find(".schedule-input-body").show();

                        if (callback) {
                            callback();
                        }
                    }

                }, this));
            }, this));
        },

        reconDeleted: function(id, name, element) {
            element.parent().find("#addNew").show();
            element.remove();
        },

        addReconciliation: function() {
            SchedulerDelegate.addSchedule({
                "type": "cron",
                "invokeService": "sync",
                "schedule": "* * * * * ?",
                "persisted": true,
                "enabled": false,
                "invokeContext": {
                    "action": "reconcile",
                    "mapping": this.model.mapping.name
                }
            }).then(_.bind(function(newSchedule) {
                Scheduler.generateScheduler({
                    "element": this.$el.find("#scheduleList"),
                    "defaults": {},
                    "onDelete": this.reconDeleted,
                    "invokeService": newSchedule.invokeService,
                    "scheduleId": newSchedule._id
                });

                this.$el.find("#addNew").hide();

                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "scheduleCreated");
            }, this));
        }
    });

    return new ScheduleView();
});