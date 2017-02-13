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
 * Copyright 2017 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "handlebars",
    "org/forgerock/openidm/ui/common/dashboard/widgets/AbstractWidget",
    "org/forgerock/openidm/ui/admin/delegates/AuditDelegate",
    "selectize",
    "moment",
    "calHeatmap"
], function($, _,
            Handlebars,
            AbstractWidget,
            AuditDelegate,
            Selectize,
            moment,
            CalHeatMap) {

    var widgetInstance = {},
        Widget = AbstractWidget.extend({
            template: "templates/admin/dashboard/widgets/AuditDataOverTimeWidgetTemplate.html",
            model : {
                "overrideTemplate" : "dashboard/widget/_frameConfig"
            },
            events: {
                "change .auditEvent": "showSubFilter",
                "change .sub-filter": "filterData",
                "click .prevMonth": "prevMonth",
                "click .thisMonth": "thisMonth",
                "click .nextMonth": "nextMonth"
            },

            widgetRender: function(args, callback) {
                this.data.height = args.widget.height;
                this.data.width = args.widget.width;

                this.partials.push("partials/dashboard/widget/_frameConfig.html");

                this.parentRender(_.bind(function() {
                    this.$el.parent().find(".widget-section-title .widget-title").text(args.widget.title);

                    this.model.monthOffset = 0;

                    this.initCal();

                    if (callback) {
                        callback();
                    }
                }, this));
            },

            initCal: function() {
                if (this.model.cal) {
                    this.model.cal = this.model.cal.destroy();
                }

                this.model.cal = new CalHeatMap();
                var range = moment().subtract(this.model.monthOffset, 'month').daysInMonth();

                this.model.cal.init({
                    domain: 'day',
                    subDomain: "hour",
                    cellSize: 13,
                    tooltip: false,
                    start: moment().subtract(this.model.monthOffset, 'month').startOf("M").toDate(),
                    itemName: [$.t("dashboard.auditData.event"), $.t("dashboard.auditData.events")],
                    displayLegend: true,
                    label: {
                        offset: {x:-1, y:0}
                    },
                    colLimit: 1,
                    domainLabelFormat: "%d",
                    domainGutter: 12,
                    cellPadding: 3,
                    data: {},
                    legendColors: {
                        min: "#b0d4cd",
                        max: "#24423c",
                        base: "#eee"
                    },
                    legendVerticalPosition: "top",
                    animationDuration: 0,
                    legend: [10, 30, 90, 270, 810],
                    range: range
                });
                this.$el.find(".audit-cal-heatmap-container").width(Math.ceil(27.5 * range) + "px");

                this.showSubFilter();
            },

            prevMonth: function(e) {
                e.preventDefault();
                this.model.monthOffset++;
                this.initCal();

                this.$el.find(".thisMonth").toggleClass("disabled", this.model.monthOffset === 0);
                this.$el.find(".nextMonth").toggleClass("disabled", this.model.monthOffset === 0);
            },

            thisMonth: function(e) {
                e.preventDefault();
                if ($(e.currentTarget).hasClass("disabled")) {
                    return false;
                }

                this.model.monthOffset = 0;
                this.initCal();
                this.$el.find(".thisMonth").toggleClass("disabled", true);
                this.$el.find(".nextMonth").toggleClass("disabled", true);
            },

            nextMonth: function(e) {
                e.preventDefault();

                if (this.model.monthOffset <= 0 || $(e.currentTarget).hasClass("disabled")) {
                    this.model.monthOffset = 0;
                    this.$el.find(".nextMonth").toggleClass("disabled", true);
                    this.$el.find(".thisMonth").toggleClass("disabled", true);
                    return false;
                }

                this.model.monthOffset--;
                this.initCal();
                this.$el.find(".thisMonth").toggleClass("disabled", this.model.monthOffset === 0);
                this.$el.find(".nextMonth").toggleClass("disabled", this.model.monthOffset === 0);
            },

            showSubFilter: function() {
                var event = this.$el.find(".auditEvent").val();
                this.$el.find(".event-type").html(event);
                this.$el.find(".sub-filter").hide();
                this.$el.find(".sub-filter[data-audit-event='"+ event +"']").show();
                this.renderGraph(true);
            },

            filterData: function(e) {
                var filterValue = $(e.currentTarget).val(),
                    filter = $(e.currentTarget).find(":selected").attr("data-filter");

                if (filterValue === "ALL") {
                    this.renderGraph(true);
                } else {
                    this.renderGraph(false, `+and+${filter}+eq+"${filterValue}"`);
                }
            },

            renderGraph: function(all, filter) {
                var viewedMonth = moment().subtract(this.model.monthOffset, 'month'),
                    monthTitle = viewedMonth.format("MMMM YYYY"),
                    days = {},
                    auditEvent = this.$el.find(".auditEvent").val(),
                    start = viewedMonth.startOf("month").format("YYYY-MM-DD"),
                    end = viewedMonth.add(1, 'month').startOf("month").format("YYYY-MM-DD"),
                    subFilter = filter || "";

                this.$el.find(".viewed-month").html(monthTitle);

                AuditDelegate.getTimestamps(auditEvent, start, end, subFilter).then((data) => {
                    this.$el.find(".result-count").html(_.get(data, "resultCount"));

                    _.each(_.map(data.result, "timestamp"), function (timestamp) {
                        let unixTime = moment(timestamp).startOf("hour").unix();

                        if (_.has(days, unixTime)) {
                            days[unixTime]++;
                        } else {
                            days[unixTime] = 1;
                        }
                    });

                    this.model.cal.update(days);
                });
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
