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

/*global define, $, form2js, _, Backbone */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/admin/tasks/TasksMenuView", [
    "org/forgerock/commons/ui/common/main/WorkflowManager",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "dataTable",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function(workflowManager, eventManager, constants, dataTable, conf, uiUtils) {
    var TasksMenuView = Backbone.View.extend({
        
        events: {
            "click tr": "showTask"
        },
        
        showTask: function(event) {
            var id = $(event.target).parent().find("input[type=hidden]").val();
            
            if(id) {
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "tasksWithMenu", args: [this.category, id]});
            }
        },
                
        render: function(category) {
            this.setElement($("#tasksMenu"));
            this.category = category;
            
            if(category === "all") {
                workflowManager.getAllAvalibleTasksViewForUser(conf.loggedUser.userName, this.displayTasks, this.errorHandler);
            } else if(category === "assigned") {
                workflowManager.getAllTasksViewForUser(conf.loggedUser.userName, this.displayTasks, this.errorHandler);
            }
        },
        
        errorHandler: function() {
            $("#tasksMenu").append('No tasks');
        },
        
        displayTasks: function(tasks) {
            var process, data, processName, task, taskName, taskId, params;
            
            for(processName in tasks) {
                process = tasks[processName];
                
                for(taskName in process) {
                    task = process[taskName];
                    
                    data = {
                        processName: processName,
                        taskName: taskName,
                        count: task.tasks.length,
                        headers: ["AppLink", ""],
                        tasks: []
                    };
                    
                    for(taskId in task.tasks) {
                        params = task.tasks[taskId];
                        
                        data.tasks.push(this.prepareParams(params));
                    }
                    
                    $("#tasksMenu").append(uiUtils.fillTemplateWithData("templates/admin/tasks/ProcessUserTaskTableTemplate.html", data)); 
                }    
            }
            
            $("#tasksMenu").accordion();
        },
        
        prepareParams: function(params) {
            return $.map(params, function(v, k) {
                if( k === "_id" ) {
                    return '<input type="hidden" value="'+ v +'" />';
                }
                
                return v;
            });
        }
    }); 
    
    return new TasksMenuView();
});


