/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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

/*global define, $, _, JSONEditor, form2js */

/**
 * @author huck.elliott
 */
define("org/forgerock/openidm/ui/admin/workflow/ProcessInstanceView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/AbstractModel",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractCollection",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils"
], function(AbstractView, eventManager, constants, UIUtils, AbstractModel, messagesManager, AbstractCollection, Backgrid, BackgridUtils) {
    var ProcessInstanceModel = AbstractModel.extend({ url: "/openidm/workflow/processinstance" }),
        ProcessDefinitionModel = AbstractModel.extend({ url: "/openidm/workflow/processdefinition" }),
        UserModel = AbstractModel.extend({ url: "/openidm/managed/user" }),
        TaskInstanceCollection = AbstractCollection.extend({ 
            mode: "client"
        }),
        ProcessInstanceView = AbstractView.extend({
        template: "templates/admin/workflow/ProcessInstanceViewTemplate.html",
        
        events: {
            "click #cancelProcessBtn" : "cancelProcess"
        },
        cancelProcess: function(e) {
            if (e) {
                e.preventDefault();
            }
            
            UIUtils.jqConfirm($.t("templates.processInstance.cancelConfirmation"), _.bind(function() {
                this.model.destroy({
                    success: function() {
                        messagesManager.messages.addMessage({"message": $.t("templates.processInstance.cancelProcessSuccess")});
                        eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "processListView"});
                    }
                });
            }, this));
        },
        render: function(args, callback) {
            var processDefinition = new ProcessDefinitionModel(),
                startedBy = new UserModel(),
                owner = new UserModel();
            
            this.model = new ProcessInstanceModel();
            
            this.model.id = args[0];
            
            this.model.fetch().then(_.bind(function(){
                var fetchArr = [];
                this.data.processInstance = this.model.toJSON();
                
                if (this.data.processInstance.startUserId) {
                    startedBy.id = this.data.processInstance.startUserId;
                    fetchArr.push(startedBy.fetch());
                }
                
                processDefinition.id = this.data.processInstance.processDefinitionId;
                fetchArr.push(processDefinition.fetch());
                
                $.when.apply($, fetchArr).done(_.bind(function(){
                    
                    this.data.processDefinition = processDefinition.toJSON();
                    this.data.startedBy = startedBy.toJSON();

                    this.parentRender(_.bind(function(){
                        
                        this.buildTasksGrid();
                        
                        if(callback) {
                            callback();
                        }
                    },this));
                    
                },this));
                
            },this));
        },
        buildTasksGrid: function () {
            var processTasks = new TaskInstanceCollection(_.sortBy(this.model.attributes.tasks, "startTime").reverse()),
                cols = [
                    {
                        name: "name",
                        label: "Task",
                        editable: false,
                        cell: "string",
                        sortable: false
                    },
                    {
                        name: "assignee",
                        label: "Assignee",
                        editable: false,
                        cell: "string",
                        sortable: false
                    },
                    {
                        name: "dueDate",
                        label: "Due",
                        editable: false,
                        cell: BackgridUtils.DateCell("dueDate"),
                        sortable: false
                    },
                    {
                        name: "startTime",
                        label: "Created",
                        editable: false,
                        cell: BackgridUtils.DateCell("startTime"),
                        sortable: false
                    },
                    {
                        name: "endTime",
                        label: "Completed",
                        editable: false,
                        cell: BackgridUtils.DateCell("endTime"),
                        sortable: false
                    },
                    {
                        editable: false,
                        cell: Backgrid.Cell.extend({
                            events: {
                                "click .editTask" : "editTask"
                            },
                            editTask: function () {
                                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "taskInstanceView", args: [this.model.id]});
                            },
                            render: function () {
                                if (!this.model.attributes.endTime) {
                                    var html = '<i class="btn fa fa-pencil grid-icon editTask" title="Edit task"></i>';
                                    this.$el.html(html);
                                }
                                return this;
                            }
                        })
                    }
                ],
                tasksGrid = new Backgrid.Grid({
                    columns: cols,
                    collection: processTasks,
                    className: "table"
                });
            
            this.$el.find("#tasksGrid").append(tasksGrid.render().el);

        }
    }); 
    
    return new ProcessInstanceView();
});


