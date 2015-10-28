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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate"
], function(constants, AbstractDelegate) {

    var obj = new AbstractDelegate(constants.host + "/openidm/scheduler");

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
        });
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

    return obj;
});
