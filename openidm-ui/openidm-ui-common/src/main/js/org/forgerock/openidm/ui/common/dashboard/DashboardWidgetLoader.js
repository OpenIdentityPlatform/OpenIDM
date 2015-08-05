/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

/*global define */

define("org/forgerock/openidm/ui/common/dashboard/DashboardWidgetLoader", [
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
    "org/forgerock/openidm/ui/common/dashboard/widgets/QuickStartWidget"
], function($, _,
            AbstractView,
            eventManager,
            constants,
            conf,
            SystemHealthDelegate,
            MemoryUsageWidget,
            ReconProcessesWidget,
            CPUUsageWidget,
            QuickStartWidget) {
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
             Availavble Widgets:

             lifeCycleMemoryHeap - Current heap memory
             lifeCycleMemoryNonHeap - Current none heap memory
             lifeCycleMemoryBoth - Displays both heap and none heap memory charts
             reconUsage - Displays current recons in process. Polls every few seconds with updated information.
             cpuUsage - Shows current CPU usage of the system
             quickStart - Widget displaying quick start cards to help users get start with core functionality
             */
            render: function(args, callback) {
                this.element = args.element;

                this.model.widgetList = {
                    lifeCycleMemoryBoth: {
                        name : $.t("dashboard.memoryUsageBoth"),
                        icon : "fa-heartbeat"
                    },
                    lifeCycleMemoryHeap: {
                        name : $.t("dashboard.memoryUsageHeap"),
                        icon : "fa-heartbeat"
                    },
                    lifeCycleMemoryNonHeap: {
                        name : $.t("dashboard.memoryUsageNonHeap"),
                        icon : "fa-heartbeat"
                    },
                    reconUsage: {
                        name: $.t("dashboard.reconProcesses"),
                        icon: "fa-eye"
                    },
                    cpuUsage: {
                        name: $.t("dashboard.cpuUsage"),
                        icon : "fa-pie-chart"
                    },
                    quickStart: {
                        name: "Quick Start",
                        icon: "fa-key"
                    }
                };

                this.data.widgetType = args.widget.type;
                this.data.widget = this.model.widgetList[args.widget.type];

                this.parentRender(_.bind(function(){
                    args.menu = this.$el.find(".dropdown-menu");
                    args.element = this.$el.find(".widget-body");

                    switch(args.widget.type) {
                        case "lifeCycleMemoryHeap":
                        case "lifeCycleMemoryBoth":
                        case "lifeCycleMemoryNonHeap":
                            this.model.widget = MemoryUsageWidget.generateWidget(args, callback);
                            break;
                        case "reconUsage":
                            this.model.widget = ReconProcessesWidget.generateWidget(args, callback);
                            break;
                        case "cpuUsage":
                            this.model.widget = CPUUsageWidget.generateWidget(args, callback);
                            break;
                        case "quickStart":
                            this.$el.find(".dropdown-toggle").hide();

                            args.icons = args.widget.icons;

                            this.model.widget = QuickStartWidget.generateWidget(args, callback);
                            break;
                    }
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
