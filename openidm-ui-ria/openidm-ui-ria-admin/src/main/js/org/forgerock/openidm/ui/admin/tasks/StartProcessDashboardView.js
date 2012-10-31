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
define("org/forgerock/openidm/ui/admin/tasks/StartProcessDashboardView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/admin/tasks/WorkflowDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/tasks/StartProcessView"
], function(AbstractView, workflowManager, eventManager, constants, startProcessView) {
    var StartProcessDashboardView = AbstractView.extend({
        
        template: "templates/admin/tasks/StartProcessDashboardTemplate.html",
        
        events: {
            "click .processName": "showStartProcessView"
        },
        
        render: function() {
            var i;
            this.parentRender(function() {
                this.registerListeners();
                this.clearStartProcessView();
                workflowManager.getAllUniqueProcessDefinitions( function(processDefinitions) {
                    for (i = 0; i < processDefinitions.length; i++) {
                        $("#processList").append("<div><a href='#' class='processName'>" + processDefinitions[i].name + "</a> "
                                + "<input type='hidden' name='id' value='" + processDefinitions[i]._id +"' />"
                                + "<div>");
                    }
                });
            });            
        },
        
        showStartProcessView: function(event) {
            event.preventDefault();
            var id = $(event.target).parent().find('[name="id"]').val();
            
            if(id) {                    
                startProcessView.render(id);
            } else {
                this.clearStartProcessView();
            }
        },
        
        clearStartProcessView: function() {
            $("#startProcessForm").html('');
        },
        
        registerListeners: function() {
            eventManager.unregisterListener("clearStartProcessTemplateView");
            eventManager.registerListener("clearStartProcessTemplateView", _.bind(function(event) {
                this.clearStartProcessView();
            }, this));
        }
        
    }); 
    

    return new StartProcessDashboardView();
});


