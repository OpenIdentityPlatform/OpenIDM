/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
define("org/forgerock/openidm/ui/admin/tasks/TasksView", [
    "org/forgerock/openidm/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/common/main/WorkflowManager",
    "org/forgerock/openidm/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/util/Constants",
    "dataTable"
], function(AbstractView, workflowManager, eventManager, constants, dataTable) {
    var TasksView = AbstractView.extend({
        template: "templates/admin/tasks/TasksTemplate.html",
        
        events: {
            "click tr": "showTask",
            "click checkbox": "select"
        },
        
        select: function(event) {
            console.log("user selected");
            event.stopPropagation();
        },
        
        showTask: function(event) {
            var id = $(event.target).parent().find("input[name=id]").val();
            
            if(id) {
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "taskDetails", args: [id]});
            }
        },
        
        render: function() {
            this.parentRender(function() {
                $('#tasksTable').dataTable( {
                    "bProcessing": true,
                    "sAjaxSource": "",
                    "fnServerData": function(sUrl, aoData, fnCallback, oSettings) {
                        workflowManager.getAllTasks(function(tasks) {
                            var data = {aaData: tasks}, i;
                            
                            for(i = 0; i < data.aaData.length; i++) {
                                data.aaData[i].selector = '<input type="checkbox" /><input type="hidden" name="id" value="'+data.aaData[i]._id +'">';
                            }
                            
                            fnCallback(data);
                        }, function() {
                            
                        });
                    },
                    "aoColumns": [
                        {
                            "mData": "selector",
                            "bSortable": false
                        },
                        { 
                            "mData": "name" 
                        }
                    ],
                    "oLanguage": {
                        "sLengthMenu": "Display _MENU_ per page"
                    },
                    "sDom": 'l<"addButton">f<"clear">rt<"clear">ip<"clear">',
                    "sPaginationType": "full_numbers",
                    "fnInitComplete": function(oSettings, json) {
                        //$(".addButton").html('<a href="#users/add/" class="buttonOrange" style="margin-left: 15px; float: left;">Add user</a>');
                    }
                });
            });
            
        }   
    }); 
    
    return new TasksView();
});


