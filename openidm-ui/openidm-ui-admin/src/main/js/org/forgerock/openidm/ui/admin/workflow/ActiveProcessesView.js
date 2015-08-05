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

/*global define */

define("org/forgerock/openidm/ui/admin/workflow/ActiveProcessesView", [
    "jquery",
    "underscore",
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
], function($, _,
            AdminAbstractView,
            ResourceDelegate,
            uiUtils,
            AbstractModel,
            AbstractCollection,
            eventManager,
            constants,
            BackgridUtils,
            router,
            Backgrid) {
    var ActiveProcessesView = AdminAbstractView.extend({
        template: "templates/admin/workflow/ActiveProcessViewTemplate.html",
        events: {
            "change #processFilterType" : "filterType"
        },
        model : {
            userFilter: "anyone",
            processTypeFilter: "all"
        },
        element: "#activeProcesses",
        render: function(args, callback) {
            this.data.processDefinitions = args[0];

            this.parentRender(_.bind(function() {
                var processGrid,
                    ProcessModel = AbstractModel.extend({ "url": "/openidm/workflow/processinstance" }),
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

                this.model.processes.url = "/openidm/workflow/processinstance?_queryId=filtered-query";
                this.model.processes.state.pageSize = null;
                this.model.processes.state.sortKey = "-startTime";

                processGrid = new Backgrid.Grid({
                    className: "table",
                    emptyText: $.t("templates.workflows.processes.noActiveProcesses"),
                    columns: BackgridUtils.addSmallScreenCell([
                        {
                            name: "processDefinitionResourceName",
                            label: $.t("templates.workflows.processes.processInstance"),
                            cell: Backgrid.Cell.extend({
                                render: function () {
                                    this.$el.html('<a href="#workflow/processinstance/' +this.model.id +'">' +this.model.get("processDefinitionResourceName") +'<small class="text-muted"> (' +this.model.id +')</small></a>');

                                    this.delegateEvents();
                                    return this;
                                }
                            }),
                            sortable: false,
                            editable: false
                        },
                        {
                            name: "startUserId",
                            label: $.t("templates.workflows.processes.startedBy"),
                            cell: "string",
                            sortable: false,
                            editable: false
                        },
                        {
                            name: "startTime",
                            label: $.t("templates.workflows.processes.created"),
                            cell: BackgridUtils.DateCell("startTime"),
                            sortable: true,
                            editable: false,
                            sortType: "toggle"
                        },
                        {
                            name: "",
                            cell: BackgridUtils.ButtonCell([
                                {
                                    className: "fa fa-pencil grid-icon",
                                    callback: function() {
                                        eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.processInstanceView, args: [this.model.id]});
                                    }
                                },
                                {
                                    className: "fa fa-times grid-icon",
                                    callback: function(event){
                                        event.preventDefault();

                                        uiUtils.jqConfirm($.t("templates.workflows.processes.cancelProcessDialog"), _.bind(function(){
                                            this.model.destroy({
                                                success: _.bind(function() {
                                                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deletedActiveProcess");
                                                }, this)
                                            });
                                        }, this));
                                    }
                                }
                            ]),
                            sortable: false,
                            editable: false
                        }]),
                    collection: this.model.processes
                });

                this.$el.find("#processGridHolder").append(processGrid.render().el);

                this.model.processes.getFirstPage();

                this.$el.find("#processAssignedTo").selectize({
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
                            var userName = item.userName.length > 0 ? ' (' + escape(item.userName) + ')': "",
                                displayName = (item.displayName) ? item.displayName : item.givenName + " " + item.sn;


                            return '<div>' +
                                '<span class="user-title">' +
                                '<span class="user-fullname">' + escape(displayName) + userName + '</span>' +
                                '</span>' +
                                '</div>';
                        },
                        option: function(item, escape) {
                            var userName = item.userName.length > 0 ? ' (' + escape(item.userName) + ')': "",
                                displayName = (item.displayName) ? item.displayName : item.givenName + " " + item.sn;


                            return '<div>' +
                                '<span class="user-title">' +
                                '<span class="user-fullname">' + escape(displayName) + userName + '</span>' +
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

                this.$el.find("#processAssignedTo")[0].selectize.addOption({
                    _id : "anyone",
                    userName: "Anyone",
                    givenName : "Anyone",
                    sn : ""
                });

                this.$el.find("#processAssignedTo")[0].selectize.setValue("anyone", true);

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
            var filterString = "";

            if(this.model.userFilter !== "anyone") {
                filterString = "_queryId=filtered-query&startUserId=" + this.model.userFilter;

                if(this.model.processTypeFilter !== "all") {
                    filterString = filterString + "&processDefinitionKey=" +this.model.processTypeFilter;
                }
            } else if (this.model.processTypeFilter !== "all") {
                filterString = "_queryId=filtered-query&processDefinitionKey=" + this.model.processTypeFilter;
            }

            if(filterString.length > 0) {
                this.model.processes.url = "/openidm/workflow/processinstance?" + filterString;
            } else {
                this.model.processes.url = "/openidm/workflow/processinstance?_queryId=query-all-ids";
            }

            this.model.processes.getFirstPage();
        }
    });

    return new ActiveProcessesView();
});
