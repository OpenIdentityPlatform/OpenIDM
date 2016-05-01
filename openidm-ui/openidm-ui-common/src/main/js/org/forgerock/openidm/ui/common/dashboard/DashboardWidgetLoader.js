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
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/delegates/SystemHealthDelegate",
    "org/forgerock/openidm/ui/common/dashboard/widgets/MemoryUsageWidget",
    "org/forgerock/openidm/ui/common/dashboard/widgets/ReconProcessesWidget",
    "org/forgerock/openidm/ui/common/dashboard/widgets/CPUUsageWidget",
    "org/forgerock/openidm/ui/common/dashboard/widgets/QuickStartWidget",
    "org/forgerock/openidm/ui/common/dashboard/widgets/FullHealthWidget"
], function($, _,
            AbstractView,
            eventManager,
            constants,
            conf,
            SystemHealthDelegate,
            MemoryUsageWidget,
            ReconProcessesWidget,
            CPUUsageWidget,
            QuickStartWidget,
            FullHealthWidget) {
    var dwlInstance = {},
        DashboardWidgetLoader = AbstractView.extend({
            template: "templates/dashboard/DashboardWidgetLoaderTemplate.html",
            noBaseTemplate: true,
            model: {

            },
            data: {

            },
            events: {

            },
            /*
             Available Widgets:
                lifeCycleMemoryHeap - Current heap memory
                lifeCycleMemoryNonHeap - Current none heap memory
                cpuUsage - Shows current CPU usage of the system
                systemHealthFull - Load full widget of health information
                reconUsage - Displays current recons in process. Polls every few seconds with updated information.
                quickStart - Widget displaying quick start cards to help users get start with core functionality
             */
            render: function(args, callback) {
                this.element = args.element;

                this.model.widgetList = {
                    lifeCycleMemoryHeap: {
                        name : $.t("dashboard.memoryUsageHeap"),
                        widget : MemoryUsageWidget
                    },
                    lifeCycleMemoryNonHeap: {
                        name : $.t("dashboard.memoryUsageNonHeap"),
                        widget : MemoryUsageWidget
                    },
                    reconUsage: {
                        name: $.t("dashboard.reconProcesses"),
                        widget : ReconProcessesWidget
                    },
                    cpuUsage: {
                        name: $.t("dashboard.cpuUsage"),
                        widget : CPUUsageWidget
                    },
                    quickStart: {
                        name: $.t("dashboard.quickStart.quickStartTitle"),
                        widget : QuickStartWidget
                    }
                };

                this.data.widgetType = args.widget.type;

                this.data.widget = this.model.widgetList[args.widget.type];

                this.parentRender(_.bind(function(){
                    args.element = this.$el.find(".widget");
                    args.title = this.data.widget.name;
                    args.showConfigButton = false;

                    this.model.widget = this.model.widgetList[this.data.widgetType].widget.generateWidget(args, callback);
                }, this));
            }
        });

    dwlInstance.generateWidget = function(loadingObject, callback) {
        var widget = {};

        $.extend(true, widget, new DashboardWidgetLoader());

        widget.render(loadingObject, callback);

        return widget;
    };

    return dwlInstance;
});
