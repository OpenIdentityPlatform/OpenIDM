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
    "org/forgerock/commons/ui/common/main/WorkflowManager"
], function(AbstractView, validatorsManager, eventManager, constants, workflowManager) {
    var AbstractTaskForm = AbstractView.extend({
        template: "templates/admin/tasks/DefaultTaskTemplate.html",
        element: "#taskContent",
        
        events: {
            "click input[name=saveButton]": "formSubmit",
            "click input[name=backButton]": "back",
            "onValidate": "onValidate"
        },
        
        formSubmit: function(event) {
            event.preventDefault();
            
            var params = form2js(this.$el.attr("id"), '.', false);
            console.log(params);
            
            workflowManager.completeTask(this.task._id, params, function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "completedTask");
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "tasks"});
            }, function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unknown");
            });
        },
        
        render: function(task, callback) { 
            this.setElement(this.element);
            this.$el.unbind();
            this.delegateEvents();
            this.task = task;
            
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
        },

        back: function() {
            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "tasks"});
        }
    }); 
    
    return AbstractTaskForm;
});


