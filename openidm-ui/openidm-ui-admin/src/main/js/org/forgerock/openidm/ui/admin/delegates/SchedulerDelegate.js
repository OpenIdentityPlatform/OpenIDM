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
    "lodash",
    "jquery",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate"
], function(_, $, constants, AbstractDelegate) {

    var obj = new AbstractDelegate(constants.host + "/openidm/scheduler"),
        queryAllSchedules = function () {
            // Get all schedule IDS
            return obj.availableSchedules().then(_.bind(function (schedules) {
                var schedulerPromises = [];

                _.each(schedules.result, function (index) {
                    // Get the schedule of each ID
                    schedulerPromises.push(obj.specificSchedule(index._id));
                }, this);

                return $.when.apply($, schedulerPromises).then(_.bind(function () {
                    return _.toArray(arguments);
                }, this));
            }, this));
        };

    obj.availableSchedules = function() {
        return obj.serviceCall({
            url: "?_queryId=query-all-ids",
            type: "GET"
        });
    };

    obj.specificSchedule = function(scheduleId) {
        return obj.serviceCall({
            url: "/" + scheduleId,
            type: "GET"
        }).then((resp) => resp);
    };

    obj.saveSchedule = function(scheduleId, schedule) {
        return obj.serviceCall({
            url: "/" + scheduleId,
            type: "PUT",
            data: JSON.stringify(schedule)
        });
    };

    obj.deleteSchedule = function(scheduleId) {
        return obj.serviceCall({
            url: "/" + scheduleId,
            type: "DELETE"
        });
    };

    obj.addSchedule = function(schedule) {
        return obj.serviceCall({
            url: "?_action=create",
            type: "POST",
            data: JSON.stringify(schedule)
        });
    };

    obj.pauseJobs = function() {
        return obj.serviceCall({
            url: "?_action=pauseJobs",
            type: "POST"
        });
    };

    obj.resumeJobs = function() {
        return obj.serviceCall({
            url: "?_action=resumeJobs",
            type: "POST"
        });
    };

    obj.listCurrentlyExecutingJobs = function() {
        return obj.serviceCall({
            url: "?_action=listCurrentlyExecutingJobs",
            type: "POST"
        });
    };

    obj.getReconSchedulesByMappingName = function (mappingName) {
        //////////////////////////////////////////////////////////////////////////////////////////////////
        //                                                                                              //
        // TODO: Use queryFilters to avoid having to pull back all schedules and sifting through them.  //
        //                                                                                              //
        //////////////////////////////////////////////////////////////////////////////////////////////////
        return queryAllSchedules().then((scheduledTasks) => {
            return _.filter(scheduledTasks, function (sched) {
                return sched.invokeContext && sched.invokeContext.mapping && sched.invokeContext.mapping === mappingName;
            });
        });
    };

    obj.getLiveSyncSchedulesByConnectorName = function (connectorName) {
        //////////////////////////////////////////////////////////////////////////////////////////////////
        //                                                                                              //
        // TODO: Use queryFilters to avoid having to pull back all schedules and sifting through them.  //
        //                                                                                              //
        //////////////////////////////////////////////////////////////////////////////////////////////////
        return queryAllSchedules().then((scheduledTasks) => {
            return _.filter(scheduledTasks, function (sched) {
                var nameFilter = sched.invokeContext && sched.invokeContext.source && sched.invokeContext.source.split("/")[1] === connectorName,
                    liveSyncFilter = sched.invokeContext && sched.invokeContext.action && sched.invokeContext.action === "liveSync";

                return nameFilter && liveSyncFilter;
            });
        });
    };

    return obj;
});
