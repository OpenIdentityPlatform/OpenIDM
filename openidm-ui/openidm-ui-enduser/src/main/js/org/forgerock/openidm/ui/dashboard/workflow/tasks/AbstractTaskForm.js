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
define("org/forgerock/openidm/ui/dashboard/workflow/tasks/AbstractTaskForm", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/dashboard/workflow/WorkflowDelegate",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractView, validatorsManager, eventManager, constants, workflowManager, conf) {
    var AbstractTaskForm = AbstractView.extend({
        template: "templates/common/EmptyTemplate.html",
        element: "#taskContent",
        
        events: {
            "onValidate": "onValidate"
        },
        
        postRender: function(callback) {
            if(callback) {
                callback();
            }
        },
        
        render: function(task, category, args, callback) { 
            this.setElement(this.element);
            this.$el.unbind();
            this.delegateEvents();
            this.task = task;
            this.category = category;
            this.args = args;
            
            this.parentRender(function() {      
                this.postRender(callback);
                this.reloadData();
            });            
        },
        
        reloadData: function() {
        }
    }); 
    
    return AbstractTaskForm;
});


