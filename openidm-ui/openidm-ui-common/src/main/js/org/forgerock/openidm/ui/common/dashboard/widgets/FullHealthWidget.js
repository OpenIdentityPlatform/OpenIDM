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

/*global define, window */

define("org/forgerock/openidm/ui/common/dashboard/widgets/FullHealthWidget", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/common/dashboard/widgets/MemoryUsageWidget",
    "org/forgerock/openidm/ui/common/dashboard/widgets/CPUUsageWidget"
], function($, _,
            AbstractView,
            MemoryUsageWidget,
            CPUUsageWidget) {
    var widgetInstance = {},
        Widget = AbstractView.extend({
            noBaseTemplate: true,
            template: "templates/dashboard/widget/DashboardTripleWidgetTemplate.html",
            model : {

            },
            events: {
                "click .refresh-health-info": "refreshHealth"
            },
            render: function(args, callback) {
                this.element = args.element;

                this.data.icons = [{
                    "icon" : "fa-refresh",
                    "iconClass": "refresh-health-info"
                }];

                this.parentRender(_.bind(function(){
                    this.model.cpuWidget = CPUUsageWidget.generateWidget({
                        element: this.$el.find(".left-chart"),
                        widget: args.widget
                    });

                    this.model.memoryHeapWidget = MemoryUsageWidget.generateWidget({
                        element: this.$el.find(".center-chart"),
                        widget: {
                            type: "lifeCycleMemoryHeap"
                        }
                    });

                    this.model.memoryNonHeapWidget = MemoryUsageWidget.generateWidget({
                        element: this.$el.find(".right-chart"),
                        widget: {
                            type: "lifeCycleMemoryNonHeap"
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
            refreshHealth: function() {
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