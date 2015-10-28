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
 * Copyright 2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/dashboard/DashboardWidgetLoader", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/dashboard/widgets/MemoryUsageWidget",
    "org/forgerock/openidm/ui/common/dashboard/widgets/CPUUsageWidget",
    "org/forgerock/openidm/ui/common/dashboard/widgets/FullHealthWidget",
    "org/forgerock/openidm/ui/admin/dashboard/widgets/MappingReconResultsWidget",
    "org/forgerock/openidm/ui/admin/dashboard/widgets/ResourceListWidget",
    "org/forgerock/openidm/ui/common/dashboard/widgets/QuickStartWidget"
], function($, _,
            AdminAbstractView,
            eventManager,
            constants,
            conf,
            MemoryUsageWidget,
            CPUUsageWidget,
            FullHealthWidget,
            MappingReconResultsWidget,
            ResourceListWidget,
            QuickStartWidget) {
    var dwlInstance = {},
        DashboardWidgetLoader = AdminAbstractView.extend({
            template: "templates/dashboard/DashboardWidgetLoaderTemplate.html",
            noBaseTemplate: true,
            model: {

            },
            data: {

            },
            /*
             Available Widgets:
             lifeCycleMemoryHeap - Current heap memory
             lifeCycleMemoryNonHeap - Current none heap memory
             systemHealthFull - Load full widget of health information
             reconUsage - Displays current recons in process. Polls every few seconds with updated information.
             cpuUsage - Shows current CPU usage of the system
             lastRecon - Widget to display the last recon per mapping
             barChart - Variable for last recon to turn on and off the barchart showing detailed recon results
             resourceList - Displays the top 4 resources for connectors, mappings, and managed objects
             quickStart - Widget displaying quick start cards to help users get started with core functionality
             */
            render: function(args, callback) {
                this.element = args.element;

                this.model.widgetList = {
                    lifeCycleMemoryBoth: {
                        name : $.t("dashboard.memoryUsageBoth")
                    },
                    lifeCycleMemoryHeap: {
                        name : $.t("dashboard.memoryUsageHeap")
                    },
                    lifeCycleMemoryNonHeap: {
                        name : $.t("dashboard.memoryUsageNonHeap")
                    },
                    systemHealthFull : {
                        name : $.t("dashboard.systemHealth")
                    },
                    cpuUsage: {
                        name: $.t("dashboard.cpuUsage")
                    },
                    lastRecon : {
                        name : $.t("dashboard.lastReconciliation")
                    },
                    resourceList : {
                        name : $.t("dashboard.resources")
                    },
                    quickStart: {
                        name: $.t("dashboard.quickStart.quickStartTitle")
                    }
                };

                this.data.widgetType = args.widget.type;
                this.data.widget = this.model.widgetList[args.widget.type];

                this.parentRender(_.bind(function(){
                    args.element = this.$el.find(".widget-body");

                    switch(args.widget.type) {
                        case "lifeCycleMemoryHeap":
                        case "lifeCycleMemoryNonHeap":
                            this.model.widget = MemoryUsageWidget.generateWidget(args, callback);
                            break;
                        case "cpuUsage":
                            this.model.widget = CPUUsageWidget.generateWidget(args, callback);
                            break;
                        case "systemHealthFull":
                            this.model.widget = FullHealthWidget.generateWidget(args, callback);
                            break;
                        case "lastRecon":
                            this.model.widget = MappingReconResultsWidget.generateWidget(args, callback);
                            break;
                        case "resourceList":
                            this.model.widget = ResourceListWidget.generateWidget(args, callback);
                            break;
                        case "quickStart":
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