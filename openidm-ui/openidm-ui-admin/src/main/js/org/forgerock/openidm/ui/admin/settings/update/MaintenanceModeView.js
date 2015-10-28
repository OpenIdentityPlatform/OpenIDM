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

/*global define*/

define("org/forgerock/openidm/ui/admin/settings/update/MaintenanceModeView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate",
    "org/forgerock/openidm/ui/admin/delegates/MaintenanceDelegate"

], function($, _,
            AdminAbstractView,
            Constants,
            SchedulerDelegate,
            MaintenanceDelegate) {

    var VersionsView = AdminAbstractView.extend({
        template: "templates/admin/settings/update/MaintenanceModeTemplate.html",
        element: "#maintenanceModeView",
        noBaseTemplate: true,
        events: {},
        data: {
            "enterMaintenanceMode": false
        },
        model: {},

        /**
         *
         * @param configs {object}
         * @param configs.enterMaintenanceMode {boolean}
         * @param [callback]
         */
        render: function(configs, callback) {
            this.data.enterMaintenanceMode = configs.enterMaintenanceMode;
            this.model.success = configs.success;
            this.model.error = configs.error;

            this.parentRender(_.bind(function() {
                if (this.data.enterMaintenanceMode) {
                    this.model.archive = configs.archive;
                    this.enterMaintenanceMode();
                } else {
                    this.model.data = configs.data;
                    this.exitMaintenanceMode();
                }

                if (callback) {
                    callback();
                }
            }, this));
        },

        enterMaintenanceMode: function() {
            this.$el.find(".shuttingDownScheduler").show();

            SchedulerDelegate.pauseJobs().then(_.bind(function(schedulerResult) {
                this.$el.find(".remainingSchedules").show();

                if (schedulerResult.success) {

                    this.pollSchedules(_.bind(function() {

                        MaintenanceDelegate.enable().then(_.bind(function (data) {
                            if (data.maintenanceEnabled === true) {
                                this.$el.find(".enterMaintenanceMode").show();
                                this.$el.find(".gettingPreviewData").show();

                                MaintenanceDelegate.preview(this.model.archive).then(_.bind(function (files) {
                                    this.$el.find(".success").show();

                                    // Give a moment to read the success string
                                    _.delay(_.bind(function() {
                                        this.model.success(files, this.model.archiveModel);
                                    }, this), 300);

                                }, this),
                                    _.bind(function() {
                                        this.model.error();
                                    }, this)
                                );

                            } else {
                                this.model.error();
                            }
                        }, this));
                    }, this));

                } else {
                    this.model.error();
                }

            }, this));
        },

        // Wait until all schedules are completed to fire the callback
        pollSchedules: function(callback) {
            SchedulerDelegate.listCurrentlyExecutingJobs().then(_.bind(function(schedulerResult) {

                if (schedulerResult.length === 0) {
                    this.$el.find(".remainingSchedules").hide();
                    this.$el.find(".schedulerShutDownSuccess").show();
                    callback();
                } else {
                    this.$el.find(".remainingSchedules strong").html(schedulerResult.length);
                    _.delay(_.bind(function () {
                        this.pollSchedules(callback);
                    }, this), 500);
                }

            }, this));
        },

        exitMaintenanceMode: function() {
            MaintenanceDelegate.disable().then(_.bind(function(data) {

                this.$el.find(".resumeSchedules").show();

                if (data.maintenanceEnabled === false) {
                    SchedulerDelegate.resumeJobs().then(_.bind(function (schedulerResult) {

                        if (schedulerResult.success) {

                            this.$el.find(".success").show();

                            // Give a moment to read the success string
                            _.delay(_.bind(function() {
                                this.model.success(this.model.data);
                            }, this), 300);

                        } else {
                            this.model.error();
                        }
                    }, this));

                } else {
                    this.model.error();
                }

            }, this));
        }
    });

    return new VersionsView();
});
