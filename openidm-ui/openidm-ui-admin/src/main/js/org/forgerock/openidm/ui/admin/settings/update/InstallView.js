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

/*global define, window*/

define("org/forgerock/openidm/ui/admin/settings/update/InstallView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/MaintenanceDelegate"

], function($, _,
            AdminAbstractView,
            Constants,
            MaintenanceDelegate) {

    var VersionsView = AdminAbstractView.extend({
        template: "templates/admin/settings/update/InstallTemplate.html",
        element: "#install",
        noBaseTemplate: true,
        events: {
          "click .restartNow": "restartNow"
        },
        data: {
            "successful": false,
            "msg": "",
            "percent": 0,
            "responseB64": ""
        },
        model: {},

        /**
         * @param configs
         * @param configs.runningID {string}
         * @param configs.archiveModel {object}
         * @param configs.success {function}
         * @param configs.error {function}
         * @param [callback]
         */
        render: function(configs, callback) {

            this.model = configs;
            this.data = _.extend(this.data, _.pick(this.model, ["successful", "msg", "percent", "responseB64"]));

            function finishRender() {
                $("#menu, #footer, #settingsBody").hide();
                this.$el.show();

                this.parentRender(_.bind(function() {
                    if (this.data.successful) {
                        this.restarting();
                    } else if (this.model.runningID) {
                        this.pollInstall(this.model.runningID);
                    }

                    if (callback) {
                        callback();
                    }

                }, this));
            }

            function preRender() {
                if (this.model.response) {
                    this.data.responseB64 = window.btoa(JSON.stringify(this.model.response));
                }

                if (this.model.archiveModel && !this.model.runningID) {

                    this.model.version = this.model.archiveModel.get("version");

                    MaintenanceDelegate.update(this.model.archiveModel.get("archive")).then(_.bind(function(response) {
                        this.model.runningID = response._id;
                        _.bind(finishRender, this)();

                    }, this), _.bind(function() {
                        this.showUI();
                        this.model.error(this.model.runningID);
                    }, this));

                } else {
                    _.bind(finishRender, this)();
                }
            }

            if (!_.has(this.model, "lastUpdateId")) {
                MaintenanceDelegate.getLastUpdateId().then(_.bind(function(response) {
                    this.model.lastUpdateId = response.lastUpdateId;
                    _.bind(preRender, this)();
                }, this));

            } else {
                _.bind(preRender, this)();
            }
        },

        showUI: function() {
            $("#menu, #footer, #settingsBody").show();
            this.$el.hide();
        },

        /**
         * Restarts IDM if it wasn't triggered naturally thought the countdown. Then waits for the last update id.
         * @param e
         */
        restartNow: function(e) {
            if (e) {
                e.preventDefault();
            }

            if (!this.model.restarted) {
                this.$el.find(".restart").text($.t("templates.update.install.restarting"));

                MaintenanceDelegate.restartIDM();

                this.waitForLastUpdateID(_.bind(function() {
                    this.showUI();
                    this.model.success({
                        "response": this.model.response,
                        "runningID": this.model.runningID,
                        "version": this.model.version
                    });
                }, this));
            }
        },

        /**
         * Counts down from 30, once the countdown completes we begin to wait for the last update ID
         */
        restarting: function () {
            var countDown;

            countDown = function(seconds) {
                this.$el.find(".restart span").text(seconds);

                if (seconds > 0 && !this.model.restarted) {
                    _.delay(_.bind(function () {
                        _.bind(countDown, this)(seconds-1);
                    }, this), 1000);

                } else {
                    if (!this.model.restarted) {
                        this.$el.find(".restart").text($.t("templates.update.install.restarting"));

                        this.waitForLastUpdateID(_.bind(function() {
                            this.showUI();
                            this.model.success({
                                "runningID": this.model.runningID,
                                "version": this.model.version,
                                "response": this.model.response
                            });
                        }, this));
                    }
                }
            };

            _.bind(countDown, this)(30);
        },

        /**
         * If the UI has not tracked a restart before then we assume this is the first time we have begun waiting on a last update ID.
         *
         * We set a 60 second timeout, if IDM is going to restart it is going to do it in 60 seconds or less.
         * We can assume an error if this timeout is allowed to complete.
         *
         * If the restart hasn't timed out we poll the lastupdate id and compare it with a previous copy.
         * These values should differ when the restart is complete.
         *
         * @param callback
         */
        waitForLastUpdateID: function(callback) {
            if (!this.model.restarted) {
                this.model.restarted = true;

                _.delay(_.bind(function () {
                    this.model.timeout = true;
                }, this), 60000);
            }

            if (!this.model.timeout) {
                MaintenanceDelegate.getLastUpdateId().then(_.bind(function (response) {
                    if (this.model.lastUpdateId === response.lastUpdateId) {
                        // Wait a short period so we don't bog down the ui with AJAX calls.
                        _.delay(_.bind(function () {
                            this.waitForLastUpdateID(callback);
                        }, this), 500);
                    } else {
                        callback();
                    }
                }, this), _.bind(function () {
                    this.waitForLastUpdateID(callback);
                }, this));

            } else {
                this.model.error("Restart timed out.");
            }
        },

        /**
         * While the install is "IN_PROGRESS" we keep polling for updates.
         * @param id
         */
        pollInstall: function(id) {

            MaintenanceDelegate.getLogDetails(id).then(_.bind(function(response) {

                if (response && response.status === "IN_PROGRESS") {
                    // This delay is to given a moment to take in the status
                    _.delay(_.bind(function() {
                        this.render(_.extend(this.model, {
                            "runningID": this.model.runningID,
                            "percent": Math.floor((response.completedTasks / response.totalTasks ) * 10),
                            "msg": response.statusMessage,
                            "version": this.model.version
                        }));
                    }, this), 500);

                } else if (response && response.status === "COMPLETE") {
                    this.$el.find("#updateInstallerContainer .progress-bar").css("width", "100%");

                    // This delay is to given a moment to take in the completion status
                    _.delay(_.bind(function() {
                        this.render(_.extend(this.model, {
                            "successful": true,
                            "runningID": this.model.runningID,
                            "files": response.files,
                            "response": response,
                            "version": this.model.version
                        }));
                    }, this), 1000);

                } else if (response && response.status === "REVERTED") {
                    this.showUI();
                    this.model.error($.t("templates.update.install.reverted"));

                } else {
                    this.showUI();
                    this.model.error($.t("templates.update.install.failedStatus"));
                }

            }, this), _.bind(function() {
                this.showUI();
                this.model.error($.t("templates.update.install.genericFail"));
            }, this));
        }
    });

    return new VersionsView();
});
