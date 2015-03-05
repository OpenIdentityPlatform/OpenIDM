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

define("org/forgerock/openidm/ui/admin/sync/ScheduleView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate",
    "org/forgerock/openidm/ui/admin/util/Scheduler",
    "org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate"
], function(AdminAbstractView,
            MappingBaseView,
            eventManager,
            constants,
            ConfigDelegate,
            SchedulerDelegate,
            Scheduler,
            BrowserStorageDelegate) {

    var ScheduleView = AdminAbstractView.extend({
        template: "templates/admin/sync/ScheduleTemplate.html",
        element: "#mappingContent",
        noBaseTemplate: true,
        events: {
            "click .saveLiveSync": "saveLiveSync",
            "click #addNew": "addReconciliation",
            "click .deleteSchedule" : "reconDeleted"
        },
        mapping: null,
        allPatterns: {},
        pattern: "",
        foundLiveSync: false,

        render: function (args, callback) {
            MappingBaseView.child = this;

            this.foundLiveSync = false;

            MappingBaseView.render(args,_.bind(function(){
                this.loadData(args, callback);
            }, this));
        },
        loadData: function(args, callback){
            var schedules = [],
                seconds = "",
                promises = [],
                tempPromises;
            this.sync = MappingBaseView.data.syncConfig;
            this.mapping = _.omit(MappingBaseView.currentMapping(),"recon");

            this.data.mappingName = this.mappingName = args[0];

            this.parentRender(_.bind(function() {
                MappingBaseView.moveSubmenu();

                this.$el.find(".noLiveSyncMessage").hide();
                this.$el.find(".systemObjectMessage").hide();

                if (this.mapping.hasOwnProperty("enableSync")) {
                    this.$el.find(".liveSyncEnabled").prop('checked', this.mapping.enableSync);
                } else {
                    this.$el.find(".liveSyncEnabled").prop('checked', true);
                }

                SchedulerDelegate.availableSchedules().then(_.bind(function (schedules) {
                    if (schedules.result.length > 0) {
                        _(schedules.result).each(function (scheduleId) {
                            tempPromises = SchedulerDelegate.specificSchedule(scheduleId._id);

                            promises.push(tempPromises);

                            tempPromises.then(_.bind(function (schedule) {
                                // There is a liveSync Scheduler and it is enabled and the source matches the source of the mapping
                                if (schedule.invokeService.indexOf("provisioner") >= 0 && schedule.enabled && schedule.invokeContext.source === this.mapping.source) {
                                    seconds = schedule.schedule.substr(schedule.schedule.indexOf("/") + 1);
                                    seconds = seconds.substr(0, seconds.indexOf("*") - 1);

                                    this.$el.find(".noLiveSyncMessage").hide();
                                    this.$el.find(".systemObjectMessage").show();
                                    this.$el.find(".managedSourceMessage").hide();
                                    this.$el.find(".liveSyncSeconds").text(seconds);

                                    this.foundLiveSync = true;

                                    // This is a recon schedule
                                } else if (schedule.invokeService.indexOf("sync") >= 0) {
                                    // The mapping is of a managed object

                                    if (this.mapping.source.indexOf("managed/") >= 0) {
                                        this.$el.find(".noLiveSyncMessage").hide();
                                        this.$el.find(".systemObjectMessage").hide();
                                        this.$el.find(".managedSourceMessage").show();
                                    } else {
                                        if(!this.foundLiveSync) {
                                            this.$el.find(".noLiveSyncMessage").show();
                                            this.$el.find(".systemObjectMessage").hide();
                                            this.$el.find(".managedSourceMessage").hide();
                                        }
                                    }

                                    if (schedule.invokeContext.mapping === this.mappingName) {
                                        Scheduler.generateScheduler({
                                            "element": $("#schedules"),
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
                                }

                            }, this));
                        }, this);

                    } else if (this.mapping.source.indexOf("managed/") >= 0) {
                        this.$el.find(".noLiveSyncMessage").hide();
                        this.$el.find(".systemObjectMessage").hide();
                        this.$el.find(".managedSourceMessage").show();
                    } else {
                        this.$el.find(".noLiveSyncMessage").show();
                        this.$el.find(".systemObjectMessage").hide();
                        this.$el.find(".managedSourceMessage").hide();
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

        saveLiveSync: function() {
            _.each(this.sync.mappings, function(map, key) {
                if (map.name === this.mappingName) {
                    this.sync.mappings[key].enableSync = this.$el.find(".liveSyncEnabled").prop("checked");
                }
            }, this);

            ConfigDelegate.updateEntity("sync", this.sync).then(function() {
                BrowserStorageDelegate.set("currentMapping", this.sync);
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "syncLiveSyncSaveSuccess");
            });
        },

        reconDeleted: function() {
            this.$el.find("#addNew").show();
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
                    "mapping": this.mapping.name
                }
            }).then(_.bind(function(newSchedule) {
                Scheduler.generateScheduler({
                    "element": this.$el.find("#schedules"),
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