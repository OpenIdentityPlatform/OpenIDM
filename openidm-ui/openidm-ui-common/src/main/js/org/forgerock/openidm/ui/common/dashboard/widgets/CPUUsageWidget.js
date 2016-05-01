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
    "dimple",
    "org/forgerock/openidm/ui/common/dashboard/widgets/AbstractWidget",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/delegates/SystemHealthDelegate"
], function($, _,
            dimple,
            AbstractWidget,
            eventManager,
            constants,
            conf,
            SystemHealthDelegate) {
    var widgetInstance = {},
        Widget = AbstractWidget.extend({
            template: "templates/dashboard/widget/DashboardSingleWidgetTemplate.html",
            model: {
                cpuChart: null,
                chartX: 0,
                chartY: 0,
                chartWidth: "100%",
                chartHeight: 180,
                drawTime: 1000,
                canvasWidth: "100%",
                canvasHeight: 190,
                warningThreshold: "60",
                warningChartColor: "#f0ad4e",
                dangerThreshold: "85",
                dangerChartColor: "#a94442",
                defaultChartColor: "#519387"
            },

            widgetRender: function(args, callback) {
                this.data.widgetTextDetails = $.t("dashboard.cpuUsage");

                this.events["click .refresh-health-info"] = "refresh";

                if(args.widget.simpleWidget) {
                    this.data.simpleWidget = true;
                } else {
                    this.data.simpleWidget = false;
                }

                this.data.menuItems = [{
                    "icon" : "fa-refresh",
                    "menuClass" : "refresh-health-info",
                    "title" : "Refresh"
                }];

                this.parentRender(_.bind(function(){
                    this.model.currentData = [];

                    SystemHealthDelegate.getOsHealth().then(_.bind(function(widgetData){
                        this.$el.find(".dashboard-details").show();

                        var svg = dimple.newSvg(this.$el.find(".widget-chart")[0], this.model.canvasWidth, this.model.canvasHeight),
                            pieChart,
                            cpuData = [
                                {
                                    "memory" : widgetData.availableProcessors - widgetData.systemLoadAverage,
                                    "type" : "Free"
                                },
                                {
                                    "memory" : widgetData.systemLoadAverage,
                                    "type" : "Used"
                                }
                            ],
                            percent = Math.round((widgetData.systemLoadAverage / widgetData.availableProcessors) * 100),
                            color = this.model.defaultChartColor,
                            percentClass = "text-primary";

                        this.model.cpuChart =  new dimple.chart(svg, cpuData);
                        this.model.cpuChart.setBounds(this.model.chartX, this.model.chartY, this.model.chartWidth, this.model.chartHeight);
                        this.model.cpuChart.addMeasureAxis("p", "memory");

                        if(percent > this.model.dangerThreshold) {
                            color = this.model.dangerChartColor;
                            percentClass = "danger";
                        } else if (percent > this.model.warningThreshold) {
                            color = this.model.warningChartColor;
                            percentClass = "warning";
                        }

                        this.model.cpuChart.assignColor("Free", "#dddddd", "#f7f7f7");
                        this.model.cpuChart.assignColor("Used", color, "#f7f7f7");
                        this.model.cpuChart.assignClass("Used", "used-cpu");

                        pieChart = this.model.cpuChart.addSeries("type", dimple.plot.pie);
                        pieChart.addOrderRule("type", true);
                        pieChart.innerRadius = "85%";

                        pieChart.addEventHandler("mouseover", _.noop);

                        this.model.cpuChart.draw();

                        //widget-header
                        this.$el.find(".widget-header").toggleClass("donut-header", true);
                        this.$el.find(".widget-header").html('<div class="header">' +'USED' +'</div>'
                            + '<div class="percent ' +percentClass +'">' +percent +'%</div>');

                        this.$el.find(".widget-header").show();

                        if (callback) {
                            callback();
                        }
                    }, this));
                }, this));
            },

            refresh: function(event) {
                if(event) {
                    event.preventDefault();
                }

                SystemHealthDelegate.getOsHealth().then(_.bind(function(widgetData) {
                    var percent = Math.round((widgetData.systemLoadAverage / widgetData.availableProcessors) * 100),
                        usedCpu = this.$el.find(".used-cpu");

                    this.$el.find(".percent").html(percent +"%");
                    this.$el.find(".percent").toggleClass("danger", false);
                    this.$el.find(".percent").toggleClass("warning", false);

                    if(percent > this.model.dangerThreshold) {
                        usedCpu.attr("fill", this.model.dangerChartColor);
                        usedCpu.css("fill", this.model.dangerChartColor);

                        this.$el.find(".percent").toggleClass("danger", true);
                    } else if (percent > this.model.warningThreshold) {
                        usedCpu.attr("fill", this.model.warningChartColor);
                        usedCpu.css("fill", this.model.warningChartColor);

                        this.$el.find(".percent").toggleClass("warning", true);
                    } else {
                        usedCpu.attr("fill", this.model.defaultChartColor);
                        usedCpu.css("fill", this.model.defaultChartColor);
                    }

                    this.model.cpuChart.data = [{
                        "memory" : widgetData.availableProcessors - widgetData.systemLoadAverage,
                        "type" : "Free"
                    }, {
                        "memory" : widgetData.systemLoadAverage,
                        "type" : "Used"
                    }];

                    this.model.cpuChart.draw(1000);
                }, this));
            },

            resize: function() {
                if(this.model.cpuChart) {
                    this.model.cpuChart.draw(0, true);
                }
            },

            cpuUsageLoad: function() {
                SystemHealthDelegate.getOsHealth().then(_.bind(function(widgetData){
                    //Temp fix for multi core processing exceeding the availableProcessors
                    if(widgetData.availableProcessors > widgetData.systemLoadAverage) {
                        widgetData.systemLoadAverage = widgetData.availableProcessors;
                    }

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