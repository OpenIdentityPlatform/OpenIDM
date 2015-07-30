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

/*global define, window*/

define("org/forgerock/openidm/ui/common/dashboard/widgets/ReconProcessesWidget", [
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
            template: "templates/dashboard/widget/DashboardSingleWidgetTemplate.html",
            model: {
                reconChart: null,
                uniqueCount: 0,
                chartData: [],
                chartX: 20,
                chartY: 20,
                chartWidth: 480,
                chartHeight: 355,
                drawTime: 1000,
                canvasWidth: "100%",
                canvasHeight: 390,
                refreshSpeed : 5000
            },
            data: {

            },
            render: function(args, callback) {
                this.element = args.element;
                this.data.widgetType = args.type;
                this.model.menu = args.menu;

                this.reconUsageWidget(callback);
            },
            reconUsageWidget: function(callback) {
                var areaSeries,
                    svg;

                this.parentRender(_.bind(function(){
                    this.model.currentData = [];

                    this.$el.find(".dashboard-loading-message").show();

                    svg = dimple.newSvg(this.$el[0], "100%", 390);

                    if(this.model.menu) {
                        this.model.menu.find(".pause").show();

                        this.model.menu.find(".start").bind("click", _.bind(function(){
                            this.startPoll();
                        }, this));

                        this.model.menu.find(".pause").bind("click", _.bind(function(){
                            this.pausePoll();
                        }, this));
                    }

                    this.model.reconChart = new dimple.chart(svg, []);
                    this.model.reconChart.setBounds(this.model.chartX, this.model.chartY, this.model.chartWidth, this.model.chartHeight);

                    this.model.x = this.model.reconChart.addCategoryAxis("x", "id");
                    this.model.x.hidden = true;
                    this.model.y = this.model.reconChart.addMeasureAxis("y", "activeThreads");

                    areaSeries = this.model.reconChart.addSeries(null, dimple.plot.area);

                    areaSeries.getTooltipText = _.bind(function(e) {
                        var data = this.model.chartData[e.x];

                        return [
                                "Timestamp: " +  data.timestamp,
                                "Active Recons : " + data.activeThreads
                        ];

                    }, this);

                    this.model.reconChart.draw();

                    this.reconUsagePolling();

                    window.onresize = _.bind(function () {
                        if(this.model.reconChart) {
                            this.model.reconChart.draw(0, true);
                        }
                    }, this);

                    if (callback) {
                        callback();
                    }
                }, this));
            },

            reconUsagePolling: function() {
                var date;

                SystemHealthDelegate.getReconHealth().then(_.bind(function(widgetData) {
                    date = new Date (Date.now());
                    widgetData.timestamp = date.getHours() +":" +date.getMinutes() +":" +date.getSeconds();

                    widgetData.id = this.model.uniqueCount;

                    this.model.uniqueCount++;

                    this.model.chartData.push(widgetData);

                    this.model.reconChart.data = _.clone(this.model.chartData);

                    if(this.model.reconChart.data.length > 1) {
                        this.$el.find(".dashboard-loading-message").hide();

                        this.model.y.overrideMin = 0;
                        this.model.y.overrideMax = widgetData.maximumPoolSize;

                        this.model.reconChart.draw(this.model.drawTime);
                    }

                    if(window.location.hash === "#dashboard/" || window.location.hash === "") {
                        this.model.pollTimer = _.delay(_.bind(function(){
                            this.reconUsagePolling();
                        }, this), this.model.refreshSpeed);
                    }
                }, this));
            },

            pausePoll: function() {
                this.model.menu.find(".start").show();
                this.model.menu.find(".pause").hide();

                clearTimeout(this.model.pollTimer);
            },

            startPoll: function() {
                this.model.menu.find(".start").hide();
                this.model.menu.find(".pause").show();

                this.reconUsagePolling();
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
