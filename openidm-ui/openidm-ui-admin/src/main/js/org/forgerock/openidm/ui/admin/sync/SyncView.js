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

define("org/forgerock/openidm/ui/admin/sync/SyncView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate",
    "org/forgerock/openidm/ui/admin/util/Scheduler",
    "org/forgerock/openidm/ui/admin/sync/SituationPolicyDialog"
], function(AdminAbstractView, eventManager, constants, ConfigDelegate, SchedulerDelegate, Scheduler, SituationPolicyDialog) {

    var SyncView = AdminAbstractView.extend({
        template: "templates/admin/sync/SyncTemplate.html",
        events: {
            "click #situationalPolicyEditorButton": "configureSituationalPolicy",
            "click #addNew": "addReconciliation"
        },
        mapping: null,
        allPatterns: {},
        pattern: "",

        render: function (args, callback) {
            var schedules = [];

            ConfigDelegate.readEntity("sync").then(_.bind(function(data) {
                this.mapping = _(data.mappings).findWhere({name: args[0]});
                this.data.mappingName = args[0];

                this.parentRender(_.bind(function() {
                    SchedulerDelegate.availableSchedules().then(_.bind(function (schedules) {
                        _(schedules.result).each(function (scheduleId) {
                            SchedulerDelegate.specificSchedule(scheduleId._id).then(_.bind(function (schedule) {

                                if (schedule.invokeService.indexOf("provisioner") >= 0 && schedule.enabled && schedule.invokeContext.source === this.mapping.source) {
                                    console.log("ENABLE DISABLE LIVESYNC");

                                } else if (schedule.invokeService.indexOf("sync") >= 0) {
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
                            }, this));
                        }, this);
                    }, this));


                    if ($("#schedules .schedulerBody").length === 0) {
                        this.$el.find("#addNew").show();
                    }

                    this.setCurrentPolicyType();
                }, this));

            }, this));
        },

        reconDeleted: function() {
            $("#addNew").show();
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
                    "element": $("#schedules"),
                    "defaults": {},
                    "onDelete": this.reconDeleted,
                    "invokeService": newSchedule.invokeService,
                    "scheduleId": newSchedule._id
                });

                this.$el.find("#addNew").hide();

                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "scheduleCreated");
            }, this));
        },

        setCurrentPolicyType: function() {
            var currentPattern = [],
                currentPolicy = [],
                patternFound = false;

            function policySorter(policy) {
                return policy.situation;
            }

            $.getJSON("templates/admin/sync/situationalPolicyPatterns.json", _.bind(function(patterns) {
                this.allPatterns = patterns;

                _(patterns).each(_.bind(function(pattern, name) {

                    currentPattern = _.chain(pattern.policies)
                        .map(function(policy) {
                            return _.omit(policy, "options");
                        })
                        .sortBy(policySorter)
                        .value();

                    currentPolicy = _.sortBy(this.mapping.policies, policySorter);

                    if (_(currentPattern).isEqual(currentPolicy)) {
                        $("#policyPatternName").text(name);
                        $("#policyPatternDesc").text(pattern.description);
                        patternFound = true;
                    }
                },this));

                if (!patternFound) {
                    $("#policyPatternName").text("Custom");
                    $("#policyPatternDesc").text(this.allPatterns.Custom.description);
                }

            }, this));
        },

        configureSituationalPolicy: function() {
            this.setCurrentPolicyType();
            SituationPolicyDialog.render(this.mapping, $("#policyPatternName").text(), this.allPatterns, _.bind(function(data) {
                if (data) {
                    this.mapping.policies = data.policies;
                    $("#policyPatternName").text(data.patternName);
                    $("#policyPatternDesc").text(data.patternDescription);
                }
            }, this));
        }
    });

    return new SyncView();
});
