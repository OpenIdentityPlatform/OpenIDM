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
 * @author jdabrowski
 */
define("org/forgerock/openidm/ui/admin/workflow/processes/AbstractProcessForm", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/workflow/WorkflowDelegate",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractView, validatorsManager, eventManager, constants, workflowManager, conf) {
    var AbstractProcessForm = AbstractView.extend({
        
        template: "templates/common/EmptyTemplate.html",
        
        events: {
            "click input[name=startProcessButton]": "formSubmit",
            "onValidate": "onValidate"
        },
        
        formSubmit: function(event) {
            event.preventDefault();
            
            var params = form2js(this.$el.attr("id"), '.', false);
            delete params.startProcessButton;
            
            workflowManager.startProcessById(this.processDefinition._id, params, _.bind(function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "startedProcess");
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "processesDashboard", trigger: true});
            }, this));
        },
        
        postRender: function() {
            
        },
        
        render: function(processDefinition, category, args, callback) { 
            this.setElement(this.element);
            this.$el.unbind();
            this.delegateEvents();
            this.processDefinition = processDefinition;
            this.category = category;
            this.args = args;
            
            this.parentRender(function() {      
                validatorsManager.bindValidators(this.$el);                
                
                this.postRender();
                this.reloadData();
                
                if(callback) {
                    callback();
                }
            });            
        },
        
        reloadData: function() {
        }
        
    }); 
    
    return AbstractProcessForm;
});


