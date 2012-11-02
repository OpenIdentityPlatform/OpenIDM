/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

/*global define, $, form2js, _ */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/admin/workflow/tasks/TasksDashboard", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/admin/workflow/WorkflowDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/workflow/tasks/TasksMenuView",
    "org/forgerock/openidm/ui/apps/dashboard/BaseUserInfoView",
    "org/forgerock/openidm/ui/apps/dashboard/NotificationsView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/apps/delegates/NotificationDelegate",
    "org/forgerock/openidm/ui/admin/workflow/tasks/TaskDetailsView"
], function(AbstractView, workflowManager, eventManager, constants, TasksMenuView, baseUserInfoView, NotificationsView, conf, notificationDelegate, taskDetailsView) {
    var TasksDashboard = AbstractView.extend({
        template: "templates/admin/workflow/tasks/TasksDashboardTemplate.html",
                
        
        render: function(mode, args) {
            //decide whether to display notification and profile 
            this.data = {shouldDisplayNotificationsAndProfile: mode.adminMode !== "openidm-admin" };
            
            this.myTasks = new TasksMenuView();
            this.candidateTasks = new TasksMenuView();
            this.registerListeners();
            
            this.parentRender(function() {
                var notificationsView;
                
                this.candidateTasks.render("all", $("#candidateTasks"));                
                this.myTasks.render("assigned", $("#myTasks"));
                
                if (mode.adminMode === "admin") {
                    baseUserInfoView.render();
                    //notifications
                    notificationDelegate.getNotificationsForUser(conf.loggedUser._id, function(notifications) {
                        
                        notifications.sort(function(a, b) {
                            if (a.requestDate < b.requestDate) {
                                return 1;
                            }
                            if (a.requestDate > b.requestDate){
                                return -1;
                            }
                            return 0;
                        });
                        
                        notificationsView = new NotificationsView();
                        notificationsView.render({el: $("#notifications"), items: notifications});
                    });
                }
                if (args && args[0] && args[0] !== '') {
                    taskDetailsView.render(args[0]);
                }
            });    
        },
        
        registerListeners: function() {
            eventManager.unregisterListener("showTaskDetailsRequest");
            eventManager.registerListener("showTaskDetailsRequest", function(event) {
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "completeTask", args: [event.id], trigger: false});
                taskDetailsView.render(event.id);
            });
            
            eventManager.unregisterListener("refreshTasksMenu");
            eventManager.registerListener("refreshTasksMenu", _.bind(function(event) {
                this.candidateTasks.render("all", $("#candidateTasks"));                
                this.myTasks.render("assigned", $("#myTasks"));
            }, this));
            
            eventManager.unregisterListener("refreshMyTasksMenu");
            eventManager.registerListener("refreshMyTasksMenu", _.bind(function(event) {
                this.myTasks.render("assigned", $("#myTasks"));
            }, this));
        }
    });

    return new TasksDashboard();
});


