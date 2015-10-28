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
 * Copyright 2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/workflow/TaskListView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/util/ModuleLoader",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/main/AbstractModel",
    "org/forgerock/commons/ui/common/main/AbstractCollection",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/admin/util/WorkflowUtils",
    "backgrid"

], function($, _,
            AdminAbstractView,
            ModuleLoader,
            ResourceDelegate,
            AbstractModel,
            AbstractCollection,
            CustomCells,
            EventManager,
            Constants,
            Router,
            WorkflowUtils,
            Backgrid) {

    var TaskListView = AdminAbstractView.extend({
        template: "templates/admin/workflow/TaskListViewTemplate.html",
        events: {
            "click .assignTask": "assignTask"
        },
        model: {},
        render: function(args, callback) {
            this.parentRender(_.bind(function() {
                var tasksGrid,
                    TaskInstanceModel = AbstractModel.extend({ url: "/openidm/workflow/taskinstance" }),
                    TaskModel = AbstractCollection.extend({
                        model: TaskInstanceModel,
                        url: "/openidm/workflow/taskinstance?_queryId=filtered-query"
                    }),
                    Tasks = new TaskModel();

                this.model = new TaskInstanceModel();

                Tasks.url = "/openidm/workflow/taskinstance?_queryId=filtered-query";
                Tasks.setSorting("-createTime");
                Tasks.state.pageSize = null;

                tasksGrid = new Backgrid.Grid({
                    className: "table backgrid",
                    emptyText: $.t("templates.workflows.tasks.noActiveTasks"),
                    columns: CustomCells.addSmallScreenCell([{
                        label: $.t("templates.workflows.tasks.task"),
                        name: "_id",
                        cell: CustomCells.DisplayNameCell("name"),
                        sortable: true,
                        editable: false,
                        sortType: "toggle"
                    }, {
                        label: $.t("templates.workflows.tasks.process"),
                        name: "processDefinitionId",
                        cell: Backgrid.Cell.extend({
                            render: function () {
                                this.$el.html("<a href='#workflow/processinstance/"+this.model.get("processInstanceId")+"'>" + this.model.get("processDefinitionId")+ " <small class='text-muted'>(" + this.model.id + ")</small></a>");
                                return this;
                            }
                        }),
                        sortable: false,
                        editable: false
                    }, {
                        label: $.t("templates.workflows.tasks.assignee"),
                        name: "assignee",
                        cell: Backgrid.Cell.extend({
                            render: function() {
                                var username = this.model.get("assignee") || "unassigned",
                                    className = "badge assignTask",
                                    html;

                                if (_.isNull(this.model.get("assignee"))) {
                                    className += " unassigned";
                                }

                                html = '<a class="' + className + '" data-id="' + this.model.id + '">' + username + '</a>';

                                this.$el.html(html);

                                return this;
                            }
                        }),
                        sortable: true,
                        editable: false,
                        sortType: "toggle"
                    }, {
                        name: "createTime",
                        label: $.t("templates.workflows.tasks.created"),
                        cell: CustomCells.DateCell("createTime"),
                        sortable: true,
                        editable: false,
                        sortType: "toggle"
                    }, {
                        name: "dueDate",
                        label: $.t("templates.workflows.tasks.due"),
                        cell: CustomCells.DateCell("dueDate"),
                        sortable: true,
                        editable: false,
                        sortType: "toggle"
                    }, {
                        name: "",
                        cell: CustomCells.ButtonCell([{
                            className: "fa fa-pencil grid-icon",
                            callback: function() {
                                EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: Router.configuration.routes.taskInstanceView, args: [this.model.id]});
                            }
                        }]),
                        sortable: false,
                        editable: false
                    }]),
                    collection: Tasks
                });

                this.$el.find("#taskGridHolder").append(tasksGrid.render().el);
                Tasks.getFirstPage();

                this.$el.find("#taskAssignedTo").selectize({
                    valueField: '_id',
                    labelField: 'userName',
                    searchField: ["given", "sn", "userName"],
                    create: false,
                    preload: true,
                    onChange: _.bind(function(value) {
                        if(value === "anyone") {
                            Tasks.url = "/openidm/workflow/taskinstance?_queryId=filtered-query";
                        } else if(value === "unassigned") {
                            Tasks.url = "/openidm/workflow/taskinstance?_queryId=filtered-query&unassigned=true";
                        } else {
                            Tasks.url = "/openidm/workflow/taskinstance?_queryId=filtered-query&assignee=" + value;
                        }

                        Tasks.getFirstPage();
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
                            queryFilter = "displayName co \"" + query + "\" or userName co \"" + query + "\"";
                        }

                        ResourceDelegate.searchResource(queryFilter, "managed/user").then(function (search) {
                                callback(search.result);
                            },
                            function() {
                                callback();
                            }
                        );
                    }, this)
                });

                this.$el.find("#taskAssignedTo")[0].selectize.addOption({_id : "anyone", userName: "", displayName : $.t("templates.workflows.tasks.anyone")});
                this.$el.find("#taskAssignedTo")[0].selectize.addOption({_id : "unassigned", userName: "", displayName : $.t("templates.workflows.tasks.unassigned")});

                this.$el.find("#taskAssignedTo")[0].selectize.setValue("anyone");

                if(callback) {
                    callback();
                }

            }, this));
        },

        assignTask: function(e) {
            if (e) {
                e.preventDefault();
            }
            this.model.id = $(e.currentTarget).attr("data-id");
            this.model.fetch().then(_.bind(function() {
                WorkflowUtils.showCandidateUserSelection(this);
            }, this));
        }
    });

    return new TaskListView();
});