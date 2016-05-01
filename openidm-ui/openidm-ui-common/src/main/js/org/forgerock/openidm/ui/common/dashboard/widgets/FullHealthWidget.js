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
    "org/forgerock/openidm/ui/common/dashboard/widgets/AbstractWidget",
    "org/forgerock/openidm/ui/common/dashboard/widgets/MemoryUsageWidget",
    "org/forgerock/openidm/ui/common/dashboard/widgets/CPUUsageWidget"
], function($, _,
            AbstractWidget,
            MemoryUsageWidget,
            CPUUsageWidget) {
    var widgetInstance = {},
        Widget = AbstractWidget.extend({
            template: "templates/dashboard/widget/DashboardTripleWidgetTemplate.html",

            widgetRender: function(args, callback) {
                this.data.menuItems = [{
                    "icon" : "fa-refresh",
                    "menuClass" : "refresh-health-info",
                    "title" : "Refresh"
                }];

                this.events["click .refresh-health-info"] = "refreshHealth";

                this.parentRender(_.bind(function(){
                    this.model.cpuWidget = CPUUsageWidget.generateWidget({
                        element: this.$el.find(".left-chart"),
                        widget: {
                            type: "cpuUsage",
                            simpleWidget: true
                        }
                    });

                    this.model.memoryHeapWidget = MemoryUsageWidget.generateWidget({
                        element: this.$el.find(".center-chart"),
                        widget: {
                            type: "lifeCycleMemoryHeap",
                            simpleWidget: true
                        }
                    });

                    this.model.memoryNonHeapWidget = MemoryUsageWidget.generateWidget({
                        element: this.$el.find(".right-chart"),
                        widget: {
                            type: "lifeCycleMemoryNonHeap",
                            simpleWidget: true
                        }
                    });

                    if(callback) {
                        callback();
                    }
                }, this));
            },

            resize : function() {
                this.model.cpuWidget.resize();
                this.model.memoryHeapWidget.resize();
                this.model.memoryNonHeapWidget.resize();
            },

            refreshHealth: function(event) {
                event.preventDefault();

                this.model.cpuWidget.refresh();
                this.model.memoryHeapWidget.refresh();
                this.model.memoryNonHeapWidget.refresh();
            }
        });

    widgetInstance.generateWidget = function(loadingObject, callback) {
        var widget = {};

        $.extend(true, widget, new Widget());

        widget.render(loadingObject, callback);

        return widget;
    };

    return widgetInstance;
});