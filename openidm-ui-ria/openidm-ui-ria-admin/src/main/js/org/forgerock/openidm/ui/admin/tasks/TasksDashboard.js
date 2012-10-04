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
define("org/forgerock/openidm/ui/admin/tasks/TasksDashboard", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/admin/tasks/WorkflowDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/tasks/TasksMenuView"
], function(AbstractView, workflowManager, eventManager, constants, TasksMenuView) {
    var TasksDashboard = AbstractView.extend({
        template: "templates/admin/tasks/TasksDashboardTemplate.html",
                
        render: function(args) {
            this.myTasks = new TasksMenuView();
            this.candidateTasks = new TasksMenuView();
            this.registerListeners();
            
            this.parentRender(function() {
                this.candidateTasks.render("all", $("#candidateTasks"));                
                this.myTasks.render("assigned", $("#myTasks"));
            });            
        },
        
        registerListeners: function() {
            eventManager.unregisterListener("showTaskDetailsRequest");
            eventManager.registerListener("showTaskDetailsRequest", function(event) {
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "tasksWithMenu", args: [event.category, event.id]});
            });
        }
    });

    return new TasksDashboard();
});


