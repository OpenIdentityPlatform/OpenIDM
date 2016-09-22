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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "handlebars",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/scheduler/SchedulerCollection",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/openidm/ui/admin/util/AdminUtils",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/openidm/ui/admin/util/Scheduler",
    "org/forgerock/openidm/ui/admin/util/SchedulerUtils",
    "backgrid-paginator"
], function($, _,
            handlebars,
            AdminAbstractView,
            eventManager,
            constants,
            SchedulerCollection,
            ConnectorDelegate,
            AdminUtils,
            Backgrid,
            BackgridUtils,
            scheduler,
            SchedulerUtils
        ) {
    var SchedulerListView = AdminAbstractView.extend({
        template: "templates/admin/scheduler/SchedulerListViewTemplate.html",
        events: {
            "change #typeFilter" : "showSubFilters",
            "change .subFilters select" : "buildGrid"
        },
        partials: [
            "partials/scheduler/_ScheduleTypeDisplay.html"
        ],
        model: {},
        render: function(args, callback) {
            $.when(
                AdminUtils.getAvailableResourceEndpoints(),
                ConnectorDelegate.currentConnectors()
            ).then((availableEnpoints,currentConnectors) => {
                this.data.availableConnectorTypes = _.map(currentConnectors, (connector) => {
                    var bundleNameArray = connector.connectorRef.bundleName.split(".");
                    return {
                        type : connector.connectorRef.bundleName,
                        display: bundleNameArray[bundleNameArray.length - 1]
                    };
                });
                this.data.availableEnpoints = availableEnpoints;
                this.parentRender(() => {
                    this.buildGrid().then(() => {
                        if (callback) {
                            callback();
                        }
                    });
                });
            });
        },
        buildGrid: function (e) {
            var _this = this,
                url = "/" + constants.context + "/scheduler/job",
                paginator,
                schedulerGrid;

            if (e) {
                e.preventDefault();
            }

            return this.constructQueryFilter().then((queryFilter) => {
                this.model.scheduleCollection = new SchedulerCollection([], {
                    url: url,
                    state: BackgridUtils.getState("invokeService"),
                    _queryFilter: queryFilter
                });

                this.$el.find("#schedulerGrid").empty();
                this.$el.find("#schedulerGrid-paginator").empty();

                schedulerGrid = new Backgrid.Grid({
                    className: "table backgrid table-hover",
                    emptyText: $.t("templates.admin.ResourceList.noData"),
                    row: BackgridUtils.ClickableRow.extend({
                        callback: function(e) {
                            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "editSchedulerView", args: [this.model.attributes._id]});
                        }
                    }),
                    columns: BackgridUtils.addSmallScreenCell([
                        {
                            name: "",
                            label: $.t("templates.scheduler.type"),
                            sortable: false,
                            editable: false,
                            cell: Backgrid.Cell.extend({
                                render: function () {
                                    var icon = "fa fa-calendar",
                                        display,
                                        scheduleName = _this.getScheduleTypeDisplay(this.model.attributes);

                                    display = '<div class="image circle">'
                                        + '<i class="' +icon +'"></i></div>' + scheduleName;

                                    this.$el.html(display);

                                    return this;
                                }
                            })
                        },
                        {
                            name: "",
                            label: $.t("templates.scheduler.schedule"),
                            sortable: false,
                            editable: false,
                            cell: Backgrid.Cell.extend({
                                render: function () {
                                    var displayText = scheduler.cronToHumanReadable(this.model.attributes.schedule);

                                    this.$el.html(displayText);

                                    return this;
                                }
                            })
                        },
                        {
                            name: "nextRunDate",
                            label: $.t("templates.scheduler.nextScheduledRun"),
                            sortable: false,
                            editable: false,
                            cell: Backgrid.Cell.extend({
                                render: function () {
                                    var nextRunDate = this.model.attributes.nextRunDate,
                                        isRunning = this.model.attributes.triggers[0] && this.model.attributes.triggers[0].nodeId && this.model.attributes.triggers[0].state > 0;

                                    if (isRunning) {
                                        this.$el.html($.t("templates.scheduler.runningNow") + " - <span class='text-muted'>" + this.model.attributes.triggers[0].nodeId + "</span>");
                                    } else if (nextRunDate) {
                                        this.$el.html(new Date(nextRunDate).toUTCString());
                                    } else {
                                        this.$el.html($.t("templates.scheduler.unavailable"));
                                    }

                                    return this;
                                }
                            })
                        },
                        {
                            name: "",
                            label: $.t("templates.scheduler.status"),
                            sortable: false,
                            editable: false,
                            cell: Backgrid.Cell.extend({
                                render: function () {
                                    var iconClass = "fa-check-circle",
                                        txtClass = "text-success",
                                        txt = $.t("templates.scheduler.enabled");

                                    if (!this.model.attributes.enabled) {
                                        iconClass = "fa-ban";
                                        txtClass = "text-warning";
                                        txt = $.t("templates.scheduler.disabled");
                                    }

                                    this.$el.html('<div class="' + txtClass + '"><i class="fa fa-lg ' + iconClass + '"></i> ' + txt + '</div>');

                                    return this;
                                }
                            })
                        }
                    ]),
                    collection: this.model.scheduleCollection
                });

                paginator = new Backgrid.Extension.Paginator({
                    collection: this.model.scheduleCollection,
                    goBackFirstOnSort: false,
                    windowSize: 0
                });

                this.$el.find("#schedulerGrid").append(schedulerGrid.render().el);
                this.$el.find("#schedulerGrid-paginator").append(paginator.render().el);

                return this.model.scheduleCollection.getFirstPage();
            });
        },
        /**
         * Toggles on/of subFilters based on the value in the typeFilter dropdown
         * and fires build grid to repopulate the grid with a new filtered query
         *
         * @param {object} event - optional event object
         */
        showSubFilters : function (e) {
            var typeFilter = $(e.target).val();

            e.preventDefault();

            this.$el.find(".subFilters").hide();
            this.$el.find("#persistedSchedulesNote").show();

            switch(typeFilter) {
                case "reconciliation":
                    this.$el.find("#mappingSubfilter").show();
                    break;
                case "liveSync":
                    this.$el.find("#connectorTypeSubfilter").show();
                    break;
                case "taskScanner":
                    this.$el.find("#resourceSubfilter").show();
                    break;
                case "inMemory":
                    this.$el.find("#persistedSchedulesNote").hide();
                    break;
            }

            this.buildGrid();
        },
        /**
         * Compiles scheduler type
         *
         * @param {string} type
         * @param {string} descriptor
         * @returns html string
         */
        renderTypePartial : function (type, descriptor) {
            return $(handlebars.compile("{{> scheduler/_ScheduleTypeDisplay}}")({
                type : type,
                descriptor : descriptor
            })).html().toString();
        },
        /**
         * Builds a the display for the cells in the "Type" column
         * each scheduler "Type" has it's own specific meta-data
         *
         * @param {object} schedule
         * @returns html string
         */
        getScheduleTypeDisplay: function (schedule) {
            var scheduleName = schedule._id,
                scheduleTypeData = SchedulerUtils.getScheduleTypeData(schedule);


            return this.renderTypePartial(scheduleTypeData.display, scheduleTypeData.meta);
        },
        /**
         * Reads the value of the filter selections
         *
         * @returns - object
         */
        getFilters: function () {
            return {
                typeFilter: this.$el.find("#typeFilter").val(),
                resourceSubfilter: this.$el.find("#resourceSubfilter select").val(),
                connectorTypeSubfilter: this.$el.find("#connectorTypeSubfilter select").val()
            };
        },
        /**
         * Creates a queryFilter string to be used when querying for the list of schedules
         *
         * @param {object} filters - object containing the filters defined in the page's filter select fields
         * @param {array} connectors - **OPTIONAL** array of connector objects
         * @returns string
         */
        getQueryFilterString: function (filters, connectors) {
            var queryFilter = "persisted eq true",//we only want persisted schedules by default
                orClauseArray;

            switch(filters.typeFilter) {
                case "liveSync":
                    queryFilter += " and invokeContext/action/ eq 'liveSync'";
                    if (connectors) {
                        orClauseArray = _.map(connectors, (connector) => {
                            return "invokeContext/source/ co 'system/" + connector.name + "/'";
                        });

                        queryFilter += " and (";
                        queryFilter += orClauseArray.join(" or ");
                        queryFilter += ")";
                    }
                    break;
                case "userDefined":
                    queryFilter += " and !(invokeContext/script/source co 'roles/onSync-roles') and !(invokeContext/script/source co 'triggerSyncCheck')";
                    break;
                case "reconciliation":
                    queryFilter += " and invokeContext/action/ eq 'reconcile'";
                    break;
                case "taskScanner":
                    queryFilter += " and invokeContext/task/ pr";
                    if (filters.resourceSubfilter) {
                        queryFilter += " and invokeContext/scan/object/ eq '" + filters.resourceSubfilter + "'";
                    }
                    break;
                case "temporalConstraintsOnRole":
                    queryFilter += " and invokeContext/script/source co 'roles/onSync-roles'";
                    break;
                case "temporalConstraintsOnGrant":
                    queryFilter += " and invokeContext/script/source co 'triggerSyncCheck'";
                    break;
                case "script":
                    queryFilter += " and invokeContext/script/ pr and !(invokeContext/script/source co 'roles/onSync-roles') and !(invokeContext/script/source co 'triggerSyncCheck')";
                    break;
                case "inMemory":
                    queryFilter = "persisted eq false";
                    break;
                case "runningNow":
                    queryFilter += " and triggers/0/nodeId pr and triggers/0/state gt 0";
                    break;
            }

            return queryFilter;
        },
        /**
         * Creates a queryFilter based on typeFilter and it's relative subType
         *
         * **Note**liveSync subFilter (Connector Type) is a special case and requires
         * some extra information to put together the correct queryFilter
         *
         * @returns - promise
         */
        constructQueryFilter: function () {
            var prom = $.Deferred(),
                filters = this.getFilters();

            if (filters.typeFilter === "liveSync" && filters.connectorTypeSubfilter) {
                ConnectorDelegate.getConnectorsOfType(filters.connectorTypeSubfilter).then((connectors) => {
                    prom.resolve(this.getQueryFilterString(filters,connectors));
                });
            } else {
                prom.resolve(this.getQueryFilterString(filters));
            }

            return prom;
        }
    });

    return new SchedulerListView();
});
