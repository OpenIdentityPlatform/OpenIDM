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
    "org/forgerock/openidm/ui/admin/mapping/util/QueryFilterEditor",
    "selectize",
    "moment",
    "calHeatmap"
], function($, _,
            Handlebars,
            AbstractWidget,
            AuditDelegate,
            QueryFilterEditor,
            Selectize,
            moment,
            CalHeatMap) {

    var widgetInstance = {},
        Widget = AbstractWidget.extend({
            template: "templates/admin/dashboard/widgets/AuditDataOverTimeWidgetTemplate.html",
            model : {
                "overrideTemplate" : "dashboard/widget/_auditConfig",
                "constants": {
                    "DEFAULT_EVENT_TOPIC": "authentication"
                }
            },

            customSettingsSave: function(dialogRef, configuration, dashboardLocation, widgetLocation) {
                var displayType = dialogRef.$modalBody.find("#filterSelection").val(),
                    widget = configuration.adminDashboards[dashboardLocation].widgets[widgetLocation],
                    title = dialogRef.$modalBody.find("#title").val();

                delete widget.topic;
                delete widget.queryFilter;
                delete widget.filter;
                delete widget.timezone;
                delete widget.title;

                if (displayType === "queryFilter") {
                    widget.topic = dialogRef.$modalBody.find(".auditEvent").val();
                    widget.queryFilter = this.model.queryFilterEditor.getFilterString();

                } else if (displayType === "presetFilter") {
                    widget.topic = dialogRef.$modalBody.find(".auditEvent").val();
                    widget.filter = this.model.presetFilterSelects[widget.topic].val();

                } else if (displayType === "interactiveFilter") {
                    widget.topic = dialogRef.$modalBody.find(".auditEvent").val();
                }

                widget.timezone = dialogRef.$modalBody.find("#timezoneOffset").val();

                if (title.length > 0 ) {
                    widget.title = title;
                }

                this.saveWidgetConfiguration(configuration);
            },

            customSettingsLoad: function(dialogRef) {
                var widgetChanges = _.clone(this.data.widget),
                    self = this,
                    changeFunctions,
                    topicSelectize = null;

                self.model.presetFilterSelects = {};

                function setFilter() {
                    self.model.presetFilterSelects[dialogRef.$modalBody.find(".auditEvent").val()].next().show();

                    if (_.has(widgetChanges, "filter")) {
                        self.model.presetFilterSelects[dialogRef.$modalBody.find(".auditEvent").val()][0].selectize.setValue(widgetChanges.filter);
                    }
                }

                function setTopic() {
                    dialogRef.$modalBody.find(".sub-filter").hide();

                    if (_.isNull(topicSelectize)) {
                        topicSelectize = dialogRef.$modalBody.find(".auditEvent").selectize({placeholder: "Select a Value"});
                    }

                    if (_.has(widgetChanges, "topic")) {
                        topicSelectize[0].selectize.setValue(widgetChanges.topic);
                    }
                }

                changeFunctions = {
                    "interactive": () => {
                        dialogRef.$modalBody.find(".filter-sub-selection").hide();
                        dialogRef.$modalBody.find(".sub-filter").hide();
                    },
                    "interactiveFilter": () => {
                        dialogRef.$modalBody.find("#eventTopicsContainer").show();
                        dialogRef.$modalBody.find("#eventPresetFiltersContainer").hide();
                        dialogRef.$modalBody.find("#eventQueryFilterContainer").hide();

                        setTopic();
                    },
                    "presetFilter": () => {
                        dialogRef.$modalBody.find("#eventTopicsContainer").show();
                        dialogRef.$modalBody.find("#eventPresetFiltersContainer").show();
                        dialogRef.$modalBody.find("#eventQueryFilterContainer").hide();

                        setTopic();
                        setFilter();
                    },
                    "queryFilter": () => {
                        dialogRef.$modalBody.find("#eventTopicsContainer").show();
                        dialogRef.$modalBody.find("#eventQueryFilterContainer").show();
                        dialogRef.$modalBody.find("#eventPresetFiltersContainer").hide();
                        setTopic();
                    }
                };

                // Setup preset filter selects
                _.each(dialogRef.$modalBody.find("#eventPresetFiltersContainer .sub-filter"), (select) => {
                    self.model.presetFilterSelects[$(select).attr("data-audit-event")] = $(select).selectize({placeholder: "Select a Value"});
                });

                // Setup query filter widget
                this.model.queryFilterEditor = new QueryFilterEditor();
                this.model.queryFilterEditor.render({
                    element: "#eventQueryFilter",
                    data: {
                        config: {
                            ops: [
                                "and",
                                "or",
                                "not",
                                "expr"
                            ],
                            tags: [
                                "pr",
                                "equalityMatch",
                                "approxMatch",
                                "co",
                                "greaterOrEqual",
                                "gt",
                                "lessOrEqual",
                                "lt"
                            ]
                        },
                        showSubmitButton: false
                    },
                    queryFilter: widgetChanges.queryFilter || ""
                });

                // Setup change event for filter selection
                dialogRef.$modalBody.find("#filterSelection").bind("change", function(e) {
                    changeFunctions[$(e.currentTarget).val()]();
                });

                // Setup change event for topic selection
                dialogRef.$modalBody.find(".auditEvent").bind("change", function(e) {
                    dialogRef.$modalBody.find(".sub-filter").hide();
                    widgetChanges.topic = $(e.currentTarget).val();

                    if (widgetChanges.topic !== self.data.widget.topic) {
                        widgetChanges.filter = "";
                    }

                    setFilter();
                });

                // Setup filter selection
                let filterSelection = dialogRef.$modalBody.find("#filterSelection").selectize({placeholder: "Select a Value"});
                filterSelection[0].selectize.setValue(this.getFilterType(this.data.widget));
            },

            getFilterType: function(widget) {
                // These strings correspond to the value of the display settings options in the settings window
                var filter = "interactive";

                if (_.has(widget, "queryFilter")) {
                    filter = "queryFilter";
                } else if (_.has(widget, "filter")) {
                    filter = "presetFilter";
                } else if (_.has(widget, "topic")) {
                    filter = "interactiveFilter";
                }

                return filter;
            },

            widgetRender: function(args, callback) {
                this.data = _.clone(args, true);
                var filter = this.getFilterType(this.data.widget);

                this.data.toolbar = true;
                this.data.topic = true;
                this.data.filter = true;

                if (filter === "queryFilter" || filter === "presetFilter") {
                    this.data.topic = false;
                    this.data.filter = false;
                    this.data.toolbar = false;
                } else if (filter === "interactiveFilter") {
                    this.data.topic = false;
                }

                this.partials.push(
                    "partials/dashboard/widget/_auditConfig.html",
                    "partials/dashboard/widget/_filters.html",
                    "partials/dashboard/widget/_auditTopics.html"
                );

                this.events = _.extend(this.events, {
                    "change .auditEvent": "showSubFilter",
                    "change .sub-filter": "renderGraph",
                    "click .prevMonth": "prevMonth",
                    "click .thisMonth": "thisMonth",
                    "click .nextMonth": "nextMonth"
                });

                this.parentRender(_.bind(function() {
                    var title =  this.data.title;
                    if (_.has(this.data.widget, "title")) {
                        title = this.data.widget.title;
                    }

                    this.$el.parent().find(".widget-section-title .widget-title").text(title);

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
                    itemSelector: this.$el.find(".cal-heatmap")[0],
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

            getAuditEvent: function () {
                var eventTopic = this.model.constants.DEFAULT_EVENT_TOPIC;

                if (this.$el.find(".auditEvent").length) {
                    eventTopic = this.$el.find(".auditEvent").val();
                } else if (_.has(this.data.widget, "topic")) {
                    eventTopic = this.data.widget.topic;
                }

                return eventTopic;
            },

            showSubFilter: function() {
                var filter = this.getFilterType(this.data.widget),
                    event = this.getAuditEvent();

                this.$el.find(".event-type").html(event);

                if (filter === "interactive" || filter === "interactiveFilter") {
                    this.$el.find(".sub-filter").hide();
                    this.$el.find(".sub-filter[data-audit-event='" + event + "']").show();
                }
                this.renderGraph();
            },

            getFilter: function() {
                var dataFilter = "",
                    auditEvent = this.getAuditEvent();

                if (_.has(this.data.widget, "filter")) {
                    if (this.data.widget.filter === "ALL") {
                        dataFilter = "";
                    } else {
                        let operator = this.$el.find("[data-audit-event='"+this.data.widget.topic+"']").find("[value='"+this.data.widget.filter+"']").attr("data-filter");
                        dataFilter = `+and+${operator}+eq+"${this.data.widget.filter}"`;
                    }

                } else if (_.has(this.data.widget, "queryFilter")) {
                    dataFilter = "+and+" + this.data.widget.queryFilter;

                } else {
                    let filterValue = this.$el.find("[data-audit-event='"+auditEvent+"']").val();
                    if (filterValue === "ALL") {
                        dataFilter = "";
                    } else {
                        let operator = this.$el.find("[data-audit-event='"+auditEvent+"']").find(":selected").attr("data-filter");
                        dataFilter = `+and+${operator}+eq+"${filterValue}"`;
                    }

                }

                return dataFilter;
            },

            renderGraph: function(e) {
                if (e) {
                    e.preventDefault();
                }

                var viewedMonth = moment().subtract(this.model.monthOffset, 'month'),
                    monthTitle = viewedMonth.format("MMMM YYYY"),
                    days = {},
                    auditEvent = this.getAuditEvent(),
                    start = viewedMonth.startOf("month").format("YYYY-MM-DD"),
                    end = viewedMonth.add(1, 'month').startOf("month").format("YYYY-MM-DD"),
                    subFilter = this.getFilter();

                this.$el.find(".viewed-month").html(monthTitle);

                AuditDelegate.getTimestamps(auditEvent, start, end, subFilter).then((data) => {
                    this.$el.find(".result-count").html(_.get(data, "resultCount"));
                    _.each(_.map(data.result, "timestamp"), function (timestamp) {
                        let unixTime = moment(timestamp).startOf("hour").unix();
                        unixTime += (moment().zone() * 60);

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
