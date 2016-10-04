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
 * Copyright 2016 ForgeRock AS.
 */

define(["lodash", "moment", "moment-timezone", "org/forgerock/openidm/ui/admin/scheduler/AbstractSchedulerView"],
function(_, moment, momentTimezone, AbstractSchedulerView) {

    var AddSchedulerView;

    AddSchedulerView = AbstractSchedulerView.extend({
        template: "templates/admin/scheduler/AddSchedulerViewTemplate.html",
        isNew: true,
        render: function(args, callback) {
            this.data.schedule = {
                "schedule": "0 0 * * * ?",
                "enabled": false,
                "persisted": true,
                "type": "cron",
                "misfirePolicy": "fireAndProceed",
                "invokeService": "sync",
                "invokeLogLevel": "info",
                // "timeZone": null,
                // "startTime": null,
                // "endTime": null,
                "concurrentExecution": false,
                "invokeContext": {
                    "action": "reconcile"
                }
            };
            this.schedule = _.cloneDeep(this.data.schedule);
            // this.data.timeZone = this.getTimeZone(this.data.schedule, moment);
            _.bindAll(this);

            this.renderForm();
        },

        resetSchedule: _.noop
    });

    return new AddSchedulerView();
});
