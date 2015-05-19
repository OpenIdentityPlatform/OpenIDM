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

/*global _ define $ window, dimple*/

define("org/forgerock/openidm/ui/common/dashboard/widgets/CPUUsageWidget", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/delegates/SystemHealthDelegate",
    "org/forgerock/commons/ui/common/util/ModuleLoader"
], function(AbstractView, eventManager, constants, conf, SystemHealthDelegate, ModuleLoader) {
    var widgetInstance = {},
        Widget = AbstractView.extend({
            noBaseTemplate: true,
            template: "templates/dashboard/widget/DashboardSingleWidgetTemplate.html",
            model: {
                cpuChart: null,
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

                ModuleLoader.load("dimple").then(_.bind(function(dimple){
                    this.cpuUsageWidget(dimple);


                    if(callback) {
                        callback();
                    }
                }, this));
            },
            cpuUsageWidget: function(dimple) {
                this.parentRender(_.bind(function(){
                    this.model.currentData = [];
                    SystemHealthDelegate.getOsHealth().then(_.bind(function(widgetData){

                        if(this.model.menu) {
                            this.model.menu.find(".refresh").show();

                            this.model.menu.find(".refresh").bind("click", _.bind(function(){
                                this.refreshPoll();
                            }, this));
                        }

                        var svg = dimple.newSvg(this.$el[0], this.model.canvasWidth, this.model.canvasHeight),
                            pieChart,
                            cpuData = [
                                {
                                    "memory" : widgetData.availableProcessors,
                                    "type" : "Free"
                                },
                                {
                                    "memory" : widgetData.systemLoadAverage,
                                    "type" : "Used"
                                }
                            ];

                        this.model.cpuChart =  new dimple.chart(svg, cpuData);
                        this.model.cpuChart.setBounds(this.model.chartX, this.model.chartY, this.model.chartWidth, this.model.chartHeight);
                        this.model.cpuChart.addMeasureAxis("p", "memory");

                        pieChart = this.model.cpuChart.addSeries("type", dimple.plot.pie);
                        pieChart.addOrderRule("type");
                        //pieChart.innerRadius = "50%";

                        this.model.cpuChart.addLegend(this.model.legendX, this.model.legendY, this.model.legendWidth, this.model.legendHeight, "left");

                        this.model.cpuChart.draw();

                        window.onresize = _.bind(function () {
                            if(this.model.cpuChart) {
                                this.model.cpuChart.draw(0, true);
                            }
                        }, this);
                    }, this));
                }, this));
            },

            cpuUsageLoad: function() {
                SystemHealthDelegate.getOsHealth().then(_.bind(function(widgetData){
                    if(this.model.cpuChart) {
                        this.model.cpuChart.data = [
                            {
                                "memory" : widgetData.availableProcessors,
                                "type" : "Free"
                            },
                            {
                                "memory" : widgetData.systemLoadAverage,
                                "type" : "Used"
                            }
                        ];

                        this.model.cpuChart.draw(this.model.drawTime);
                    }
                }, this));
            },

            refreshPoll: function() {
                this.cpuUsageLoad();
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