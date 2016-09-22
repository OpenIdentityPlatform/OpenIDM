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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "handlebars"
], function ($, _,
             handlebars) {

    var obj = {};

    /**
     * Determines type of scheduler job and puts together an object
     * with info including "type", "display" (version used for display purposes),
     * "meta" (meta-data about the specific type of object),
     * and "metaSource" (the job.invokeContext property being used to get the meta property)
     *
     * @param {object} schedule
     * @returns {object} - an object representing schedule type data example:
     * {
     *     "type": "recon",
     *     "display": "Reconciliation",
     *     "meta": "managedUser_systemLdapAccounts",
     *     "metaSource": "mapping"
     * }
     */
    obj.getScheduleTypeData = function (schedule) {
        var scheduleTypeData,
            action = schedule.invokeContext.action,
            script = schedule.invokeContext.script,
            maxMetaLength = 45;

        if (action && action === "liveSync") {
            scheduleTypeData = {
                type : "liveSync",
                display: $.t("templates.scheduler.liveSync"),
                meta: schedule.invokeContext.source,
                metaSource: "source"
            };
        } else if (action && action === "reconcile") {
            scheduleTypeData = {
                type : "recon",
                display: $.t("templates.scheduler.reconciliation"),
                meta: schedule.invokeContext.mapping,
                metaSource: "mapping"
            };
        } else if (_.has(schedule.invokeContext,"task")) {
            scheduleTypeData = {
                type : "taskScanner",
                display: $.t("templates.scheduler.taskScanner"),
                meta: schedule.invokeContext.scan.object,
                metaSource: "scan.object"
            };
        } else if (script && script.source && script.source.indexOf("roles/onSync-roles") > -1) {
            scheduleTypeData = {
                type : "temporalConstraintsOnRole",
                display: $.t("templates.scheduler.temporalConstraintsOnRole"),
                meta: script.globals.object.name,
                metaSource: "script.globals.object.name"
            };
        }  else if (script && script.source && script.source.indexOf("triggerSyncCheck") > -1) {
            scheduleTypeData = {
                type : "temporalConstraintsOnGrant",
                display: $.t("templates.scheduler.temporalConstraintsOnGrant"),
                meta: script.globals.userId,
                metaSource: "script.globals.userId"
            };
        } else if (script) {
            scheduleTypeData = {
                type : "genericScript",
                display: $.t("templates.scheduler.script")
            };
            if (script.source) {
                scheduleTypeData.meta = script.source;
                scheduleTypeData.metaSource = "script.source";
            }
            if (script.file) {
                scheduleTypeData.meta = script.file;
                scheduleTypeData.metaSource = "script.file";
            }
        }
        //make sure the meta data is truncated for display purposes
        //script type meta could be a very long string
        if (scheduleTypeData.meta.length > maxMetaLength) {
            scheduleTypeData.meta = scheduleTypeData.meta.substring(0,maxMetaLength) + "...";
        }

        return scheduleTypeData;
    };

    return obj;
});
