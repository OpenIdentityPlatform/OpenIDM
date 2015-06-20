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

define("org/forgerock/openidm/ui/admin/mapping/scheduling/LiveSyncView", [
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate",
    "org/forgerock/openidm/ui/admin/util/Scheduler",
    "org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate"

], function(MappingAdminAbstractView,
            MappingBaseView,
            eventManager,
            constants,
            ConfigDelegate,
            SchedulerDelegate,
            Scheduler,
            BrowserStorageDelegate) {

    var ScheduleView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/scheduling/LiveSyncTemplate.html",
        element: "#liveSyncView",
        noBaseTemplate: true,
        events: {
            "click .saveLiveSync": "saveLiveSync"
        },
        model: {
            mapping: {},
            sync: {}
        },

        render: function (args, callback) {
            var seconds = "",
                promises = [],
                tempPromises,
                foundLiveSync= false;

            this.model.mapping = _.omit(this.getCurrentMapping(), "recon");
            this.model.sync = this.getSyncConfig();

            this.parentRender(_.bind(function() {
                this.$el.find(".noLiveSyncMessage").hide();
                this.$el.find(".systemObjectMessage").hide();

                if (this.model.mapping.hasOwnProperty("enableSync")) {
                    this.$el.find(".liveSyncEnabled").prop('checked', this.model.mapping.enableSync);
                } else {
                    this.$el.find(".liveSyncEnabled").prop('checked', true);
                }

                if (args.schedules.result.length > 0) {
                    _(args.schedules.result).each(function (scheduleId) {
                        tempPromises = SchedulerDelegate.specificSchedule(scheduleId._id);

                        promises.push(tempPromises);

                        tempPromises.then(_.bind(function (schedule) {

                            // There is a liveSync Scheduler and it is enabled and the source matches the source of the mapping
                            if (schedule.invokeService.indexOf("provisioner") >= 0 && schedule.enabled && schedule.invokeContext.source === this.model.mapping.source) {
                                seconds = schedule.schedule.substr(schedule.schedule.indexOf("/") + 1);
                                seconds = seconds.substr(0, seconds.indexOf("*") - 1);

                                this.$el.find(".noLiveSyncMessage").hide();
                                this.$el.find(".systemObjectMessage").show();
                                this.$el.find(".managedSourceMessage").hide();
                                this.$el.find(".liveSyncSeconds").text(seconds);

                                foundLiveSync = true;

                            // This is a recon schedule
                            } else if (schedule.invokeService.indexOf("sync") >= 0) {

                                // The mapping is of a managed object
                                if (this.model.mapping.source.indexOf("managed/") >= 0) {
                                    this.$el.find(".noLiveSyncMessage").hide();
                                    this.$el.find(".systemObjectMessage").hide();
                                    this.$el.find(".managedSourceMessage").show();
                                } else if (!foundLiveSync) {
                                    this.$el.find(".noLiveSyncMessage").show();
                                    this.$el.find(".systemObjectMessage").hide();
                                    this.$el.find(".managedSourceMessage").hide();
                                }
                            }

                        }, this));
                    }, this);

                } else if (this.model.mapping.source.indexOf("managed/") >= 0) {
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
        },

        saveLiveSync: function() {
            _.each(this.model.sync.mappings, function(map, key) {
                if (map.name === this.model.mapping.name) {
                    this.model.sync.mappings[key].enableSync = this.$el.find(".liveSyncEnabled").prop("checked");
                }
            }, this);

            ConfigDelegate.updateEntity("sync", this.model.sync).then(function() {
                BrowserStorageDelegate.set("currentMapping", this.model.sync);
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "syncLiveSyncSaveSuccess");
            });
        }
    });

    return new ScheduleView();
});