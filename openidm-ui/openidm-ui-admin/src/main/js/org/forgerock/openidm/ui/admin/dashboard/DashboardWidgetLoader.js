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
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/dashboard/widgets/MemoryUsageWidget",
    "org/forgerock/openidm/ui/common/dashboard/widgets/CPUUsageWidget",
    "org/forgerock/openidm/ui/common/dashboard/widgets/FullHealthWidget",
    "org/forgerock/openidm/ui/admin/dashboard/widgets/MappingReconResultsWidget",
    "org/forgerock/openidm/ui/admin/dashboard/widgets/ResourceListWidget",
    "org/forgerock/openidm/ui/common/dashboard/widgets/QuickStartWidget",
    "org/forgerock/openidm/ui/admin/dashboard/widgets/FrameWidget",
    "org/forgerock/openidm/ui/admin/dashboard/widgets/RelationshipWidget"
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
            QuickStartWidget,
            FrameWidget,
            RelationshipWidget) {
    var dwlInstance = {},
        widgetList = {
            lifeCycleMemoryHeap: {
                name : $.t("dashboard.memoryUsageHeap"),
                widget : MemoryUsageWidget,
                desc : $.t("dashboard.widgetDescriptions.lifeCycleMemoryHeap"),
                defaultSize: "small"
            },
            lifeCycleMemoryNonHeap: {
                name : $.t("dashboard.memoryUsageNonHeap"),
                widget : MemoryUsageWidget,
                desc : $.t("dashboard.widgetDescriptions.lifeCycleMemoryNonHeap"),
                defaultSize: "small"
            },
            systemHealthFull : {
                name : $.t("dashboard.systemHealth"),
                widget : FullHealthWidget,
                desc : $.t("dashboard.widgetDescriptions.systemHealthFull"),
                defaultSize: "large"
            },
            cpuUsage: {
                name: $.t("dashboard.cpuUsage"),
                widget : CPUUsageWidget,
                desc : $.t("dashboard.widgetDescriptions.cpuUsage"),
                defaultSize: "small"
            },
            lastRecon : {
                name : $.t("dashboard.lastReconciliation"),
                widget : MappingReconResultsWidget,
                desc : $.t("dashboard.widgetDescriptions.lastRecon"),
                defaultSize: "large"
            },
            resourceList : {
                name : $.t("dashboard.resources"),
                widget : ResourceListWidget,
                desc : $.t("dashboard.widgetDescriptions.resourceList"),
                defaultSize: "large"
            },
            quickStart: {
                name: $.t("dashboard.quickStart.quickStartTitle"),
                widget : QuickStartWidget,
                desc : $.t("dashboard.widgetDescriptions.quickStart"),
                defaultSize: "large"
            },
            frame: {
                name : $.t("dashboard.frameWidget.frameWidgetTitle"),
                widget : FrameWidget,
                desc : $.t("dashboard.widgetDescriptions.frame"),
                defaultSize: "large"
            },
            relationship : {
                name: $.t("dashboard.relationshipWidget.relationshipTitle"),
                widget : RelationshipWidget,
                desc : $.t("dashboard.widgetDescriptions.relationship"),
                defaultSize: "large"
            }
        },
        DashboardWidgetLoader = AdminAbstractView.extend({
            template: "templates/dashboard/DashboardWidgetLoaderTemplate.html",
            noBaseTemplate: true,
            model: {},
            data: {},
            
            /*
             Available Widgets:
             lifeCycleMemoryHeap - Current heap memory
             lifeCycleMemoryNonHeap - Current none heap memory
             systemHealthFull - Load full widget of health information
             reconUsage - Displays current recons in process. Polls every few seconds with updated information.
             cpuUsage - Shows current CPU usage of the system
             lastRecon - Widget to display the last recon per mapping
                - barChart - Variable for last recon to turn on and off the barchart showing detailed recon results
             resourceList - Displays the top 4 resources for connectors, mappings, and managed objects
             quickStart - Widget displaying quick start cards to help users get started with core functionality
             relationship - Widget to display a any resource's relationships throughout the system
             frame - Iframe widget that provide an iframe for you to point to any URL
             */
            render: function(args, callback) {
                this.element = args.element;

                this.data.widgetType = args.widget.type;
                this.data.widget = widgetList[args.widget.type];

                this.parentRender(_.bind(function(){
                    args.element = this.$el.find(".widget");
                    args.title = this.data.widget.name;
                    args.showConfigButton = true;

                    this.model.widget = widgetList[this.data.widgetType].widget.generateWidget(args, callback);
                }, this));
            }
        });

    dwlInstance.generateWidget = function(loadingObject, callback) {
        var widget = {};

        $.extend(true, widget, new DashboardWidgetLoader());

        widget.render(loadingObject, callback);

        return widget;
    };

    dwlInstance.getWidgetList = function() {
        return widgetList;
    };

    return dwlInstance;
});