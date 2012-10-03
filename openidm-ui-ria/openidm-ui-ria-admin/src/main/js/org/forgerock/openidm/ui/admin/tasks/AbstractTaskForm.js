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

/*global define, $, form2js, _, js2form, document */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/admin/tasks/AbstractTaskForm", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/tasks/WorkflowDelegate",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractView, validatorsManager, eventManager, constants, workflowManager, conf) {
    var AbstractTaskForm = AbstractView.extend({
        template: "templates/admin/tasks/DefaultTaskTemplate.html",
        element: "#taskContent",
        
        events: {
            "click input[name=saveButton]": "formSubmit",
            "click input[name=claimButton]": "claimTask",
            "onValidate": "onValidate"
        },
        
        formSubmit: function(event) {
            event.preventDefault();
            
            var params = form2js(this.$el.attr("id"), '.', false);
            console.log(params);
            
            workflowManager.completeTask(this.task._id, params, _.bind(function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "completedTask");
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "tasksWithMenu", args: [this.category]});
            }, this), function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unknown");
            });
        },
        
        claimTask: function(event) {
            event.preventDefault();
            
            var userName, categoryToGo, message;
            
            if(this.category === "all") {
                userName = conf.loggedUser.userName;
                categoryToGo = "assigned";
                message = "claimedTask";
            } else {
                userName = "";
                categoryToGo = "all";
                message = "unclaimedTask";
            }
            
            workflowManager.assignTaskToUser(this.task._id, userName, _.bind(function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, message);
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "tasksWithMenu", args: [categoryToGo, this.task._id]});
            }, this), function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unknown");
            });
        },
        
        render: function(task, category, callback) { 
            this.setElement(this.element);
            this.$el.unbind();
            this.delegateEvents();
            this.task = task;
            this.category = category;
            
            this.parentRender(function() {      
                validatorsManager.bindValidators(this.$el);                
                this.reloadData();
                
                if(callback) {
                    callback();
                }
            });            
        },
        
        reloadData: function() {
            js2form(document.getElementById(this.$el.attr("id")), this.task);
            this.$el.find("input[name=saveButton]").val("Update");
            this.$el.find("input[name=backButton]").val("Back");
        }
    }); 
    
    return AbstractTaskForm;
});


