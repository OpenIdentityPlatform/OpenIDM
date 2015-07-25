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

/*global define, $, _, Handlebars */

define("org/forgerock/openidm/ui/admin/workflow/ProcessHistoryView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/AbstractModel",
    "org/forgerock/commons/ui/common/main/AbstractCollection",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "backgrid"
], function(AdminAbstractView,
            ResourceDelegate,
            uiUtils,
            AbstractModel,
            AbstractCollection,
            eventManager,
            constants,
            BackgridUtils,
            router,
            Backgrid) {
    var ProcessHistoryView = AdminAbstractView.extend({
        template: "templates/admin/workflow/ProcessHistoryViewTemplate.html",
        events: {
            "change #processHistoryFilterType" : "filterType"
        },
        model : {
            userFilter: "anyone",
            processTypeFilter: "all"
        },
        element: "#processHistory",
        render: function(args, callback) {
            this.data.processDefinitions = args[0];

            this.parentRender(_.bind(function() {
                var processGrid,
                    ProcessModel = AbstractModel.extend({ "url": "/openidm/workflow/processinstance/history" }),
                    Process = AbstractCollection.extend({ model: ProcessModel });

                this.model.processes = new Process();

                this.model.processes.on('backgrid:sort', function(model) {
                    var cid = model.cid,
                        filtered = model.collection.filter(function (model) {
                            return model.cid !== cid;
                        });

                    _.each(filtered, function (model) {
                        model.set('direction', null);
                    });
                });

                this.model.processes.url = "/openidm/workflow/processinstance/history?_queryId=filtered-query&finished=true";
                this.model.processes.state.pageSize = null;
                this.model.processes.state.sortKey = "-startTime";

                processGrid = new Backgrid.Grid({
                    className: "table",
                    emptyText: $.t("templates.workflows.processes.noCompletedProcesses"),
                    columns: [
                        {
                            name: "processDefinitionId",
                            label: $.t("templates.workflows.processes.processInstance"),
                            cell: Backgrid.Cell.extend({
                                render: function () {
                                    this.$el.html('<a href="#workflow/processinstance/' +this.model.id +'">' +this.model.get("processDefinitionId") +'<small class="text-muted"> (' +this.model.id +')</small></a>');

                                    this.delegateEvents();
                                    return this;
                                }
                            }),
                            sortable: true,
                            editable: false
                        },
                        {
                            name: "startUserId",
                            label: $.t("templates.workflows.processes.startedBy"),
                            cell: "string",
                            sortable: true,
                            editable: false
                        },
                        {
                            name: "startTime",
                            label: $.t("templates.workflows.processes.created"),
                            cell: BackgridUtils.DateCell("startTime"),
                            sortable: true,
                            editable: false
                        },
                        {
                            name: "endTime",
                            label: "COMPLETED",
                            cell: BackgridUtils.DateCell("endTime"),
                            sortable: true,
                            editable: false
                        },
                        {
                            name: "",
                            cell: BackgridUtils.ButtonCell([
                                {
                                    className: "fa fa-pencil grid-icon",
                                    callback: function() {
                                        eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.processInstanceView, args: [this.model.id]});
                                    }
                                }
                            ]),
                            sortable: false,
                            editable: false
                        }],
                    collection: this.model.processes
                });

                this.$el.find("#processHistoryGridHolder").append(processGrid.render().el);

                this.model.processes.getFirstPage();

                this.$el.find("#processHistoryAssignedTo").selectize({
                    valueField: '_id',
                    labelField: 'userName',
                    searchField: ["userName","givenName", "sn"],
                    create: false,
                    preload: true,
                    onChange: _.bind(function(value) {
                        this.model.userFilter = value;

                        this.reloadGrid();
                    },this),
                    render : {
                        item: function(item, escape) {
                            var username = '<small class="text-muted"> (' +escape(item.userName) +')</small>';

                            if(item._id === "anyone") {
                                username = '';
                            }

                            return '<div>' +
                                '<span class="user-title">' +
                                '<span class="user-fullname">' + escape(item.givenName) +' ' +escape(item.sn) + '</span>' +
                                username +
                                '</span>' +
                                '</div>';
                        },
                        option: function(item, escape) {
                            var username = '<small class="text-muted"> (' +escape(item.userName) +')</small>';

                            if(item._id === "anyone") {
                                username = "";
                            }

                            return '<div>' +
                                '<span class="user-title">' +
                                '<span class="user-fullname">' + escape(item.givenName) +' ' +escape(item.sn) + '</span>' +
                                username +
                                '</span>' +
                                '</div>';
                        }
                    },
                    load: _.bind(function(query, callback) {
                        var queryFilter;

                        if (!query.length) {
                            queryFilter = "userName sw \"\" &_pageSize=10";
                        } else {
                            queryFilter = "givenName sw \"" +query +"\" or sn sw \"" +query +"\" or userName sw \"" +query +"\"";
                        }

                        ResourceDelegate.searchResource(queryFilter, "managed/user").then(function(search) {
                                callback(search.result);
                            },
                            function(){
                                callback();
                            }
                        );

                    }, this)
                });

                this.$el.find("#processHistoryAssignedTo")[0].selectize.addOption({
                    _id : "anyone",
                    userName: "Anyone",
                    givenName : "Anyone",
                    sn : ""
                });

                this.$el.find("#processHistoryAssignedTo")[0].selectize.setValue("anyone", true);

                if(callback) {
                    callback();
                }
            }, this));
        },

        filterType : function(event) {
            this.model.processTypeFilter = $(event.target).val();

            this.reloadGrid();
        },

        reloadGrid: function() {
            var filterString = "_queryId=filtered-query&finished=true";

            if(this.model.userFilter !== "anyone") {
                filterString = filterString +"&startUserId=" + this.model.userFilter;

                if(this.model.processTypeFilter !== "all") {
                    filterString = filterString + "&processDefinitionKey=" +this.model.processTypeFilter;
                }
            } else if (this.model.processTypeFilter !== "all") {
                filterString = filterString + "&processDefinitionKey=" + this.model.processTypeFilter;
            }

            this.model.processes.url = "/openidm/workflow/processinstance/history?" + filterString;

            this.model.processes.getFirstPage();
        }
    });

    return new ProcessHistoryView();
});