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

define("org/forgerock/openidm/ui/common/dashboard/widgets/MemoryUsageWidget", [
    "jquery",
    "underscore",
    "dimple",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/delegates/SystemHealthDelegate"
], function($, _, dimple, AbstractView, eventManager, constants, conf, SystemHealthDelegate) {
    var widgetInstance = {},
        Widget = AbstractView.extend({
            noBaseTemplate: true,

            model: {
                heapChart: null,
                nonHeapChart: null,
                chartX: 20,
                chartY: 40,
                chartWidth: 460,
                chartHeight: 310,
                legendX: 20,
                legendY: 20,
                legendWidth: 90,
                legendHeight: 200,
                drawTime: 1000,
                canvasWidth: "100%",
                canvasHeight: 390
            },
            events: {

            },
            data: {

            },
            render: function(args, callback) {
                this.element = args.element;
                this.data.widgetType = args.type;
                this.model.menu = args.menu;

                if(args.type === "lifeCycleMemoryBoth") {
                    this.template = "templates/dashboard/widget/DashboardDoubleWidgetTemplate.html";
                } else {
                    this.template = "templates/dashboard/widget/DashboardSingleWidgetTemplate.html";
                }

                this.memoryUsageWidget(callback);
            },
            drawHeap: function(svg, data) {
                var ring;

                this.model.heapChart =  new dimple.chart(svg, data);
                this.model.heapChart.setBounds(this.model.chartX, this.model.chartY, this.model.chartWidth, this.model.chartHeight);
                this.model.heapChart.addMeasureAxis("p", "memory");

                this.model.heapChart.addLegend(this.model.legendX, this.model.legendY, this.model.legendWidth, this.model.legendHeight, "left");

                ring = this.model.heapChart.addSeries("type", dimple.plot.pie);
                ring.innerRadius = "50%";
                ring.addOrderRule("type");

                this.model.heapChart.draw();
            },
            drawNonHeap: function(svg, data) {
                var ring;

                this.model.nonHeapChart =  new dimple.chart(svg, data);
                this.model.nonHeapChart.setBounds(this.model.chartX, this.model.chartY, this.model.chartWidth, this.model.chartHeight);
                this.model.nonHeapChart.addMeasureAxis("p", "memory");

                ring = this.model.nonHeapChart.addSeries("type", dimple.plot.pie);
                ring.innerRadius = "50%";
                ring.addOrderRule("type");

                this.model.nonHeapChart.addLegend(this.model.legendX, this.model.legendY, this.model.legendWidth, this.model.legendHeight, "left");

                this.model.nonHeapChart.draw();
            },
            memoryUsageWidget: function(callback) {
                this.model.currentData = [];

                this.parentRender(_.bind(function() {
                    SystemHealthDelegate.getMemoryHealth().then(_.bind(function (widgetData) {

                        if(this.model.menu) {
                            this.model.menu.find(".refresh").show();

                            this.model.menu.find(".refresh").bind("click", _.bind(function () {
                                this.refreshPoll();
                            }, this));
                        }

                        var svg = [],
                            leftRing,
                            rightRing,
                            heapData = [
                                {
                                    "memory": widgetData.heapMemoryUsage.used,
                                    "type": "Used"
                                },
                                {
                                    "memory": widgetData.heapMemoryUsage.max - widgetData.heapMemoryUsage.used,
                                    "type": "Free"
                                }
                            ],
                            nonHeapData = [
                                {
                                    "memory": widgetData.nonHeapMemoryUsage.used,
                                    "type": "Used"
                                },
                                {
                                    "memory": widgetData.nonHeapMemoryUsage.max - widgetData.nonHeapMemoryUsage.used,
                                    "type": "Free"
                                }
                            ];

                        if (this.data.widgetType === "lifeCycleMemoryHeap") {
                            svg.push(dimple.newSvg(this.$el[0], this.model.canvasWidth, this.model.canvasHeight));
                            this.drawHeap(svg[0], heapData);
                        } else if (this.data.widgetType === "lifeCycleMemoryNonHeap") {
                            svg.push(dimple.newSvg(this.$el[0], this.model.canvasWidth, this.model.canvasHeight));
                            this.drawNonHeap(svg[0], nonHeapData);
                        } else {
                            svg.push(dimple.newSvg(this.$el.find(".left-chart")[0], this.model.canvasWidth, this.model.canvasHeight));
                            svg.push(dimple.newSvg(this.$el.find(".right-chart")[0], this.model.canvasWidth, this.model.canvasHeight));

                            this.drawHeap(svg[0], heapData);
                            this.drawNonHeap(svg[1], nonHeapData);
                        }

                        window.onresize = _.bind(function () {
                            if (this.model.nonHeapChart) {
                                this.model.nonHeapChart.draw(0, true);
                            }

                            if (this.model.heapChart) {
                                this.model.heapChart.draw(0, true);
                            }
                        }, this);

                        if (callback) {
                            callback();
                        }
                    }, this));
                }, this));
            },

            memoryUsageLoad: function() {
                SystemHealthDelegate.getMemoryHealth().then(_.bind(function(widgetData){
                    if(this.model.heapChart) {
                        this.model.heapChart.data = [
                            {
                                "memory" : widgetData.heapMemoryUsage.used,
                                "type" : "Used"
                            },
                            {
                                "memory" : widgetData.heapMemoryUsage.max - widgetData.heapMemoryUsage.used,
                                "type" : "Free"
                            }
                        ];

                        this.model.heapChart.draw(this.model.drawTime);
                    }

                    if(this.model.nonHeapChart) {

                        this.model.nonHeapChart.data = [
                            {
                                "memory" : widgetData.nonHeapMemoryUsage.used,
                                "type" : "Used"
                            },
                            {
                                "memory" : widgetData.nonHeapMemoryUsage.max - widgetData.nonHeapMemoryUsage.used,
                                "type" : "Free"
                            }
                        ];

                        this.model.nonHeapChart.draw(this.model.drawTime);
                    }
                }, this));
            },

            refreshPoll: function() {
                this.memoryUsageLoad();
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
