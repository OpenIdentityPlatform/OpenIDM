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

/*global define, $, form2js, _, js2form, document, require */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/admin/tasks/TaskDetailsView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/tasks/WorkflowDelegate",
    "org/forgerock/openidm/ui/admin/tasks/TasksFormManager"
], function(AbstractView, validatorsManager, eventManager, constants, workflowManager, tasksFormManager) {
    var TaskDetailsView = AbstractView.extend({
        template: "templates/admin/tasks/TaskDetailsTemplate.html",
        
        element: "#taskDetails",
        
        render: function(id, category) {  
            
            this.parentRender(function() {      
                validatorsManager.bindValidators(this.$el);
                
                workflowManager.getTask(id, _.bind(function(task) {
                    this.task = task;
                    
                    var view = require(tasksFormManager.getViewForForm("applicationAcceptance"));
                    view.render(task, category);
                }, this));
            });            
        }
    }); 
    
    return new TaskDetailsView();
});


