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

define("org/forgerock/openidm/ui/admin/mapping/ScheduleView", [
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/openidm/ui/admin/mapping/scheduling/SchedulerView",
    "org/forgerock/openidm/ui/admin/mapping/scheduling/LiveSyncView",
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate"

], function(_,
            MappingAdminAbstractView,
            SchedulerView,
            LiveSyncView,
            SchedulerDelegate) {

    var ScheduleView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/ScheduleTemplate.html",
        element: "#mappingContent",
        noBaseTemplate: true,

        render: function (args, callback) {

            this.parentRender(_.bind(function () {

                SchedulerDelegate.availableSchedules().then(_.bind(function (schedules) {
                    SchedulerView.render({"schedules": schedules});
                    LiveSyncView.render({"schedules": schedules});

                    if (callback) {
                        callback();
                    }
                }, this));

            }, this));
        }
    });

    return new ScheduleView();
});
