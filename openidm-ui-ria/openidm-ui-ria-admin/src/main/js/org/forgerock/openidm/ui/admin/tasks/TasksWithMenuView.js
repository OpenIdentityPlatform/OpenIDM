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
define("org/forgerock/openidm/ui/admin/tasks/TasksWithMenuView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/admin/tasks/WorkflowDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/tasks/TasksMenuView",
    "org/forgerock/openidm/ui/admin/tasks/TaskDetailsView"
], function(AbstractView, workflowManager, eventManager, constants, TasksMenuView, taskDetailsView) {
    var TasksWithMenuView = AbstractView.extend({
        template: "templates/admin/tasks/TasksWithMenuTemplate.html",
                
        render: function(args) {
            var category, id;
            category = args[0];
            id = args[1];
            
            this.tasksMenuView = new TasksMenuView();
            this.registerListeners();
            this.parentRender(function() {
                this.tasksMenuView.render(category);
                
                if(id) {                    
                    taskDetailsView.render(id, category);
                } else {
                    $("#taskDetails").html('<div class="center shadowFrame"><h1>Choose a task</h1></div>');
                }
            });            
        },
        
        registerListeners: function() {
            eventManager.unregisterListener("showTaskDetailsRequest");
            eventManager.registerListener("showTaskDetailsRequest", _.bind(function(event) {
                taskDetailsView.render(event.id, event.category);
            }, this));
        }
    }); 

    return new TasksWithMenuView();
});


