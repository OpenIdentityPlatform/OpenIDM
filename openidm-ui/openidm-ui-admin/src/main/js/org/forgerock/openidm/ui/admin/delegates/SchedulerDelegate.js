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

    var obj = new AbstractDelegate(constants.host + "/openidm/scheduler/job");

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
        return obj.serviceCall({
            url: "?_queryFilter=invokeContext/action/ eq 'reconcile' and invokeContext/mapping/ eq '" + mappingName + "'",
            type: "GET"
        }).then((response) => {
            return response.result;
        });
    };

    obj.getLiveSyncSchedulesByConnectorName = function (connectorName) {
        return obj.serviceCall({
            url: "?_queryFilter=invokeContext/action/ eq 'liveSync'",
            type: "GET"
        }).then((response) => {
            return _.filter(response.result, (sched) => {
                return sched.invokeContext.source.split("/")[1] === connectorName;
            });
        });
    };
    /**
    * This function takes an array of nodeIds, loops over each calling out to the
    * scheduler/jobs endpoint looking for any jobs with triggers that have been aquired
    * by that node then returns a promise with an array of all the running jobs from
    * the list of nodeIds
    * @param nodeIds {array} - list of nodeIds
    * @returns {promise} - an array of scheduler jobs that are currently running on the
    *                      cluster nodes with the ids provided to this function
    **/
    obj.getSchedulerTriggersByNodeIds = function (nodeIds) {
        var promiseArray = [],
            resultsArray = [],
            jobsPromise = (nodeId) => {
                return obj.serviceCall({
                    url: "?_queryFilter=persisted eq true and triggers/0/nodeId pr and triggers/0/state gt 0 and triggers/0/nodeId eq '" + nodeId +"'",
                    type: "GET",
                    suppressSpinner: true
                }).then((response) => {
                    _.each(response.result, (job) => {
                        resultsArray.push(job);
                    });

                    return;
                });
            }
            ;
        //loop over each node and push the individual promises onto the promisesArray
        //once the last search promise is resolved the main promise
        //returned by this funciton will be resolved
        _.each(nodeIds, function (nodeId) {
            promiseArray.push(jobsPromise(nodeId));
        });

        return $.when.apply($, promiseArray).then(() => {
            return resultsArray;
        });
    };

    obj.validate = function (cronString){
        return obj.serviceCall({
            url: "?_action=validateQuartzCronExpression",
            type: "POST",
            data: JSON.stringify({ "cronExpression" : cronString })
        });
    };

    return obj;
});
