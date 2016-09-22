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

define(["jquery", "lodash", "moment", "moment-timezone",
    "org/forgerock/openidm/ui/admin/scheduler/AbstractSchedulerView",
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate",
    "org/forgerock/openidm/ui/admin/util/SchedulerUtils",
    "org/forgerock/openidm/ui/admin/util/ClusterUtils",
    "org/forgerock/openidm/ui/admin/delegates/ClusterDelegate"
], function($, _,
    moment,
    momentTimezone,
    AbstractSchedulerView,
    SchedulerDelegate,
    SchedulerUtils,
    ClusterUtils,
    ClusterDelegate
) {

    var EditSchedulerView;

    EditSchedulerView = AbstractSchedulerView.extend({
        template: "templates/admin/scheduler/EditSchedulerViewTemplate.html",
        isNew: false,
        events: _.extend({
            "click #nodeDetailsButton": "openNodeDetailDialog"
        }, AbstractSchedulerView.prototype.events),
        render: function(args, callback) {
            this.data.schedulerId = args[0];

            SchedulerDelegate.specificSchedule(args[0]).then((schedule) => {
                if (schedule.triggers.length > 0) {
                    this.data.nodeId = schedule.triggers[0].nodeId;
                }
                schedule = _.set(schedule, "invokeService", this.serviceType(schedule.invokeService));
                schedule = _.omit(schedule, "triggers", "nextRunDate");
                this.data.schedule = _.cloneDeep(schedule);
                this.schedule = _.cloneDeep(schedule);
                this.data.scheduleJSON = JSON.stringify(schedule, null, 4);
                this.data.sheduleTypeData = SchedulerUtils.getScheduleTypeData(schedule);

                this.renderForm(() => {
                    this.disable(".save-cancel-btn");
                });

            });
        },
        /**
        * This function is called on node row click and opens up a BootstrapDialog which loads node details
        **/
        openNodeDetailDialog: function (e) {
            var nodeId = $(e.target).closest("a").attr("nodeId");

            e.preventDefault();

            ClusterDelegate.getIndividualNode(nodeId).then( (node) => {
                ClusterUtils.getDetailsForNodes([node]).then( (nodes) => {
                    //since we are passing in only one node we need node[0]
                    ClusterUtils.openNodeDetailDialog(nodes[0]);
                });
            });
        }

    });

    return new EditSchedulerView();
});
