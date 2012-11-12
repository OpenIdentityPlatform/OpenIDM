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

/*global define, $, form2js, _, Backbone, moment */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/admin/workflow/tasks/TasksMenuView", [
    "org/forgerock/openidm/ui/admin/workflow/WorkflowDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "dataTable",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/apps/delegates/UserApplicationLnkDelegate",
    "org/forgerock/commons/ui/user/delegates/UserDelegate",
    "org/forgerock/openidm/ui/apps/delegates/ApplicationDelegate",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/commons/ui/common/components/popup/PopupCtrl"
], function(workflowManager, eventManager, constants, dataTable, conf, uiUtils, userApplicationLnkDelegate, userDelegate, applicationDelegate, dateUtil, popupCtrl) {
    var TasksMenuView = Backbone.View.extend({
        
        events: {
            "click .detailsLink": "showTask",
            "change select[name=assignedUser]": "claimTask",
            "mouseenter .userLink": "showUser",
            "click .closeLink" : "hideDetails",
            "click .requeueLink" : "requeueTaskHandler"
        },
        
        tasks: {},
        processes: {},
        
        requeueTaskHandler: function(event) {
            event.preventDefault();
            
            var id = $(event.target).closest("tr").prev().find("[name=taskId]").val();
            
            this.requeueTask(id, _.bind(function() {
                this.hideDetails();
            }, this));
        },
        
        hideDetails: function(event) {
            if(event) {
                event.preventDefault();
            }
            
            $("#taskDetails").closest("tr").remove();
            $("#myTasks").accordion("resize");   
            $("#candidateTasks").accordion("resize");
        },
        
        showTask: function(event) {
            event.preventDefault();
            
            var id = $(event.target).parent().parent().find("input[name=taskId]").val();
            
            if(id) {
                eventManager.sendEvent("showTaskDetailsRequest", {"category": this.category, "id": id});
            }
        },
        
        showUser: function(event) {
            event.preventDefault();
            
            var userId = $(event.target).next().val(), data = {}, requesterDisplayName = $(event.target).next().next().val(), user, taskId;
            
            taskId = $(event.target).parent().parent().find("input[name=taskId]").val();
            user = this.getParamForTask("user", taskId);

            data = {userDisplayName: user.givenName + " "+ user.familyName, userName: user.userName, requesterDisplayName: requesterDisplayName};
            popupCtrl.showBy(uiUtils.fillTemplateWithData("templates/admin/workflow/tasks/ShowUserProfile.html", data),$(event.target));
                
        },
        
        getParamForTask: function(paramName, taskId) {
            var processName, taskName, taskType, task, i, process;
            for(processName in this.tasks) {
                process = this.tasks[processName];
                for(taskName in process) {
                    taskType = process[taskName];
                    for(i = 0; i < taskType.tasks.length; i++) {
                        task = taskType.tasks[i];
                        if (task._id === taskId) {
                            return task.variables[paramName];
                        }
                    }
                }
            }
        },
        
        render: function(category, element, callback) {
            if(!element) {
                this.setElement($("#tasksMenu"));
            } else {
                this.setElement(element);
            }
                        
            this.category = category;
            
            if(category === "all") {
                workflowManager.getAllAvalibleTasksViewForUser(conf.loggedUser.userName, _.bind(this.fetchProcessInstancesAndThenDisplayTasks, this), _.bind(this.errorHandler, this));
            } else if(category === "assigned") {
                workflowManager.getAllTasksViewForUser(conf.loggedUser.userName, _.bind(this.fetchProcessInstancesAndThenDisplayTasks, this), _.bind(this.errorHandler, this));
            }
        },
        
        getTasksSize: function(tasks) {
            var processName, taskName, taskType, task, process, size = 0;
            
            for(processName in tasks) {
                process = tasks[processName];
                for(taskName in process) {
                    taskType = process[taskName];                    
                    size += taskType.tasks.length;
                }
            }
            
            return size;
        },
        
        fetchProcessInstancesAndThenDisplayTasks: function(tasks) {        
            this.tasks = tasks;
            
            var display = _.after(this.getTasksSize(this.tasks), _.bind(this.displayTasks, this, this.tasks)), getProcess;
            
            getProcess = _.bind(function(processInstance) {
                this.processes[processInstance._id] = processInstance;
                display();
            }, this);
                      
            _.each(tasks, _.bind(function(process, processName) {
                var taskName, task, i;
                
                for(taskName in process) {
                    for(i = 0; i < process[taskName].tasks.length; i++) {
                        task = process[taskName].tasks[i];
                        
                        if(this.processes[task.processInstanceId] === undefined) {
                            workflowManager.getProcess(task.processInstanceId, getProcess, display);
                        } else {
                            display();
                        }
                    }
                }
            }, this));
        },
        
        errorHandler: function() {
            this.$el.html('');
            if(this.category === "assigned") {
                this.$el.append('<b>' + $.t("openidm.ui.admin.tasks.TasksMenuView.noTasksAssigned") + '</b>');
            } else {
                this.$el.append('<b>' + $.t("openidm.ui.admin.tasks.TasksMenuView.noTasksInGroupQueue") + '</b>');
            }
        },
        
        displayTasks: function(tasks) {
            var process, data, processName, taskType, taskName, actions, i, task;
            
            this.tasks = tasks;
            this.$el.html('');
            
            for(processName in tasks) {
                process = tasks[processName];
                
                for(taskName in process) {
                    taskType = process[taskName];
                    
                    data = {
                        processName: processName,
                        taskName: taskName,
                        count: taskType.tasks.length,
                        headers: this.getParamsForTaskType(taskType),
                        batchOperation: this.category === 'assigned',
                        tasks: []
                    };
                    
                    for(i = 0; i < taskType.tasks.length; i++) {
                        task = taskType.tasks[i];
                        data.tasks.push(this.prepareParamsFromTask(task));
                    }
                }  

                this.$el.append(uiUtils.fillTemplateWithData("templates/admin/workflow/tasks/ProcessUserTaskTableTemplate.html", data));
            } 
            
            this.$el.accordion('destroy');
            this.$el.accordion({collapsible: true, active: false, heightStyle: "content", event: "noevent"});
            
            this.$el.find(".ui-accordion-header").on('mouseenter', function(event) {                
                $.doTimeout('tasksAccordion', 150, _.bind(function() {
                    $(".ui-accordion").not($(this).parent()).accordion({active: false});
                    
                    if(!$(this).hasClass('ui-state-active')) {
                        $(this).parent().accordion({active: $(this).index() / 2});
                    }
                }, this));
            });
            
            this.$el.find(".ui-accordion-header").click(function(event) {
                event.preventDefault();
            });
            
            this.refreshAssignedSelectors();
        },
        
        getParamsForTaskType: function(taskType) {
            return [
                    $.t("openidm.ui.admin.tasks.TasksMenuView.headers.initiator"),
                    $.t("openidm.ui.admin.tasks.TasksMenuView.headers.key"),
                    $.t("openidm.ui.admin.tasks.TasksMenuView.headers.requested"),
                    $.t("openidm.ui.admin.tasks.TasksMenuView.headers.inQueue"),
                    $.t("openidm.ui.admin.tasks.TasksMenuView.headers.actions")
                ];
        },
        
        prepareParamsFromTask: function(task) {
            var actions = this.getActions(task);
            
            return this.prepareParams({
                /*"user": this.getUserLink(task.variables.user.givenName + ' ' + task.variables.user.familyName, task.variables.user._id, task.variables.userApplicationLnk.requester)*/
                "initiator": this.processes[task.processInstanceId].startUserId + " ",
                "key" : this.processes[task.processInstanceId].businessKey + " ",
                "requested" : dateUtil.formatDate(this.processes[task.processInstanceId].startTime),
                "inQueue" : moment(task.createTime).fromNow(true),
                "actions": actions + this.getHiddenParams(task)
            });
        },
        
        getHiddenParams: function(task) {
            var ret = '';
                        
            ret += '<input type="hidden" value="'+ task._id +'" name="taskId" />';
            ret += '<input type="hidden" value="'+ task.assignee +'" name="assignedUser" />';
            
            return ret;
        },
        
        refreshAssignedSelectors: function() {
            _.each($("select[name=assignedUser]"), function(target) {
                var assignedUser = $(target).parent().parent().find('input[name=assignedUser]').val();
                
                //TODO load users which can be assigned to this task
                //TODO add options
                
                if(conf.loggedUser.userName === assignedUser) {
                    $(target).val('me');
                    
                    if(conf.loggedUser.givenName) {
                        $(target).find('option[value=me]').html(conf.loggedUser.givenName + ' ' + conf.loggedUser.familyName);
                    } else {
                        $(target).find('option[value=me]').html(conf.loggedUser.userName);
                    }
                } else if(assignedUser !== "null") {
                    $(target).val(assignedUser);
                }
            });
            
            console.log("refresing selectors");
        },
        
        getUserLink: function(userName, userNameId, requesterDisplayName) {
            return "<a href='#' class='userLink'>"+ userName+ "</a><input type='hidden' name='uid' value='"+ userNameId +"' /><input type='hidden' name='requesterDisplayName' value='"+ requesterDisplayName +"' />";
        },
        
        getActions: function(task) {
            if(this.category === 'all') {
                return '<select name="assignedUser" style="width: 180px"><option value="null">' + $.t("common.task.unassigned") 
                    + '</option><option value="me">' + $.t("common.task.assignToMe")
                    + '</option></select> <a href="#" class="button choosable choosable-static detailsLink">' + $.t("common.form.details")
                    + '</a>';
            } else if(this.category === 'assigned') {
                return '<a href="#" class="button choosable choosable-static detailsLink">' + $.t("common.form.details") + '</a>';
            }
        },
       
        claimTask: function(event) {
            event.preventDefault();
            
            var id = $(event.target).parent().parent().find("input[name=taskId]").val(), newAssignee, assignee;
            
            newAssignee = $(event.target).val();
            assignee = $(event.target).parent().parent().find("input[name=assignedUser]").val();
            
            if(newAssignee === "me") {
                newAssignee = conf.loggedUser.userName;
            }
            
            if(!assignee) {
                assignee = "null";
            }
            
            if(id && newAssignee !== assignee) {                
                workflowManager.assignTaskToUser(id, newAssignee, _.bind(function() {
                    $(event.target).parent().parent().find("input[name=assignedUser]").val(newAssignee);
                    eventManager.sendEvent("refreshMyTasksMenu");
                }, this), function() {
                    if(assignee === conf.loggedUser.userName) {
                        $(event.target).val("me");
                    } else {
                        $(event.target).val(assignee);
                    }
                });
            }
        },
        
        approveTask: function(id, callback) {
            event.preventDefault();
            
            if(id) {
                workflowManager.completeTask(id, {"decision": "accept"}, _.bind(function() {
                    callback(this);
                }, this), function() {
                    callback(this);
                });
            }
        },
        
        denyTask: function(id, callback, denyReason) {
            event.preventDefault();
            
            if(id) {
                workflowManager.completeTask(id, {"decision": "reject", "reason": denyReason}, _.bind(function() {
                    callback(this);
                }, this), function() {
                    callback(this);
                });
            }
        },
        
        requeueTask: function(id, callback) {
            if(id) {
                workflowManager.assignTaskToUser(id, "", _.bind(function() {
                    callback(this);
                }, this), function() {
                    callback(this);
                });
            }
        },
        
        prepareParams: function(params) {
            return $.map(params, function(v, k) {
                return v;
            });
        }
    }); 
    
    return TasksMenuView;
});


