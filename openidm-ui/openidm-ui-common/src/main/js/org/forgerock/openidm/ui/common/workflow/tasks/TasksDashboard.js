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
 * Copyright 2011-2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/common/workflow/tasks/TasksDashboard", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/common/workflow/WorkflowDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/workflow/tasks/TasksMenuView",
    "org/forgerock/openidm/ui/common/notifications/NotificationsView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/notifications/NotificationDelegate",
    "org/forgerock/openidm/ui/common/workflow/tasks/TaskDetailsView",
    "org/forgerock/openidm/ui/common/workflow/processes/StartProcessDashboardView"
], function($, _, AbstractView, workflowManager, eventManager, constants, TasksMenuView,
            NotificationsView, conf, notificationDelegate, taskDetailsView, startProcessView) {

    var TasksDashboard = AbstractView.extend({
        template: "templates/workflow/tasks/TasksDashboardTemplate.html",
        element: "#dashboardWorkflow",
        noBaseTemplate: true,
        data: {
            shouldDisplayNotifications: true,
            mode: "user"
        },
        render: function(args, callback) {

            this.myTasks = new TasksMenuView();
            this.candidateTasks = new TasksMenuView();
            this.registerListeners();

            this.parentRender(function() {
                var notificationsView,
                    notifications;

                this.candidateTasks.render("all", $("#candidateTasks"));
                this.myTasks.render("assigned", $("#myTasks"));
                startProcessView.render();

                //notifications
                notificationDelegate.getNotificationsForUser(function(notificationsList) {
                    notifications = notificationsList.notifications;

                    notifications.sort(function(a, b) {
                        if (a.requestDate < b.requestDate) {
                            return 1;
                        }
                        if (a.requestDate > b.requestDate){
                            return -1;
                        }
                        return 0;
                    });

                    NotificationsView.render({el: $("#notifications"), items: notifications});
                });

                if (callback) {
                    callback();
                }
            });
        },

        getDetailsRow: function() {
            return '<tr class="input-full"><td colspan="5"><div id="taskDetails"></div></td></tr>';
        },

        showDetails: function(event) {
            var root,
                listItem;

            root = event.category === "assigned" ? $("#myTasks") : $("#candidateTasks");
            listItem = root.find("[name=taskId][value="+event.id+"]").parents(".list-group-item");

            if(listItem.find("#listItem").length === 0) {
                $("#taskDetails").remove();

                listItem.append('<div id="taskDetails"></div>');

                taskDetailsView.render(event.task, event.definition, event.category, function() {
                    if(event.category === "all") {
                        //$("#taskDetails input:enabled, #taskDetails select:enabled").filter(function(){return $(this).val() === "";}).parent().hide();
                        $("#taskDetails input, #taskDetails select").attr("disabled", "true");
                        $("#taskDetails span").hide();
                    }

                    if(root.find("#taskContent").html() === "") {
                        root.find("#taskContent").css("text-align","left");
                        root.find("#taskContent").html($.t("openidm.ui.admin.tasks.StartProcessDashboardView.noDataRequired"));
                    }
                });
            } else {
                $("#taskDetails").remove();
            }
        },

        registerListeners: function() {
            eventManager.unregisterListener("showTaskDetailsRequest");
            eventManager.registerListener("showTaskDetailsRequest", _.bind(this.showDetails, this));

            eventManager.unregisterListener("refreshTasksMenu");
            eventManager.registerListener("refreshTasksMenu", _.bind(function(event) {
                this.refreshMenus();
                startProcessView.render();
            }, this));

            eventManager.unregisterListener("refreshMyTasksMenu");
            eventManager.registerListener("refreshMyTasksMenu", _.bind(function(event) {
                this.refreshMenus();
                startProcessView.render();
            }, this));
        },

        refreshMenus: function() {
            this.myTasks.render("assigned", $("#myTasks"));

            this.candidateTasks.render("all", $("#candidateTasks"));
        }
    });

    return new TasksDashboard();
});