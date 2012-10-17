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
    "org/forgerock/openidm/ui/admin/tasks/WorkflowDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "dataTable",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/apps/delegates/UserApplicationLnkDelegate",
    "org/forgerock/commons/ui/user/delegates/UserDelegate",
    "org/forgerock/openidm/ui/apps/delegates/ApplicationDelegate",
    "org/forgerock/commons/ui/common/util/DateUtil"
], function(workflowManager, eventManager, constants, dataTable, conf, uiUtils, userApplicationLnkDelegate, userDelegate, applicationDelegate, dateUtil) {
    var TasksMenuView = Backbone.View.extend({
        
        events: {
            "click .detailsLink": "showTask",
            "change select[name=assignedUser]": "claimTask",
            "click .choosable" : "markAsChoosen",
            "click .cancelLink": "resetChoosen",
            "click .saveLink": "save",
            "click .userLink": "showUser"
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
            
            var userName = $(event.target).next().val();
            
            if(userName) {
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "adminUserProfile", args: [userName]});
            }
        },
        
        render: function(category, element, callback) {
            if(!element) {
                this.setElement($("#tasksMenu"));
            } else {
                this.setElement(element);
            }
            
            this.$el.html('');
            
            this.category = category;
            
            if(category === "all") {
                workflowManager.getAllAvalibleTasksViewForUser(conf.loggedUser.userName, _.bind(this.displayTasks, this), _.bind(this.errorHandler, this));
            } else if(category === "assigned") {
                workflowManager.getAllTasksViewForUser(conf.loggedUser.userName, _.bind(this.displayTasks, this), _.bind(this.errorHandler, this));
            }
        },
        
        errorHandler: function() {
            this.$el.append('<b>No tasks</b>');
        },
        
        displayTasks: function(tasks) {
            var process, data, processName, task, taskName, actions, allLoadedCallback;
            
            allLoadedCallback = function(self) {
                if (self.counter === self.numberOfProcessesToDisplay) {
                    self.$el.accordion('destroy');
                    self.$el.accordion({collapsible: true, active: false, heightStyle: "content"});
                    self.refreshAssignedSelectors();
                }
            };
            
            this.counter = 0;
            this.numberOfProcessesToDisplay = 0;
            
            actions = this.getActions();     
            
            for(processName in tasks) {
                this.numberOfProcessesToDisplay++;
            }
            
            for(processName in tasks) {
                
                process = tasks[processName];
                
                for(taskName in process) {
                    task = process[taskName];
                    
                    data = {
                        processName: processName,
                        taskName: taskName,
                        count: task.tasks.length,
                        //TODO make it generic
                        headers: ["For", "Application", "Requested", "Actions"],
                        batchOperation: this.category === 'assigned',
                        tasks: []
                    };
                    
                    this.fetchTaskData(task, data, actions, allLoadedCallback);
                }    
            }          
        },
        
        counter: 0,
        
        numberOfProcessesToDisplay: 0,
        
        fetchTaskData: function(task, data, actions, callback) {
            var i, j = 0, params, fetchParametersCallback;
            
            fetchParametersCallback = function(userName, userNameId, appName, date, taskId, assignee) {
                //TODO make it generic
                data.tasks.push(this.prepareParams({"user": this.getUserLink(userName, userNameId), "app": appName, 
                    "date": dateUtil.formatDate(date), "actions": actions, "hidden": this.getHiddenParams({id: taskId, assignee: assignee})}));
                j++;
                
                if(j === task.tasks.length) {
                    this.$el.append(uiUtils.fillTemplateWithData("templates/admin/tasks/ProcessUserTaskTableTemplate.html", data));
                    this.counter++;
                    callback(this);
                }
            };
            
            for(i = 0; i < task.tasks.length; i++) {
                params = task.tasks[i];
                if(params.userApplicationLnkId) {
                    this.fetchParameters(params.userApplicationLnkId, params._id, params.assignee, _.bind(fetchParametersCallback, this));
                } 
            } 
        },
        
        getHiddenParams: function(params) {
            var ret = '';
                        
            ret += '<input type="hidden" value="'+ params.id +'" name="taskId" />';
            ret += '<input type="hidden" value="'+ params.assignee +'" name="assignedUser" />';
            
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
        
        getUserLink: function(userName, userNameId) {
            return "<a href='#' class='userLink'>"+ userName+ "</a><input type='hidden' name='userName' value='"+ userNameId +"' />";
        },
        
        fetchParameters: function(userAppLinkId, taskId, assignee, callback) {
            userApplicationLnkDelegate.readEntity(userAppLinkId, function(userAppLink) {                
                userDelegate.readEntity(userAppLink.userId, function(user) {
                    applicationDelegate.getApplicationDetails(userAppLink.applicationId, function(app) {
                        callback(user.givenName + ' ' + user.familyName, user.userName, app.name, userAppLink.lastTimeUsed, taskId, assignee);
                    });
                });   
            });
        },
        
        getActions: function() {
            if(this.category === 'all') {
                return '<select name="assignedUser"><option value="null">Unassigned</option><option value="me">Assign to me</option></select> <a href="#" class="buttonOrange detailsLink">Details</a>';
            } else if(this.category === 'assigned') {
                return '<a href="#" class="buttonOrange choosable" data-action="approveTask">Approve</a>' +
                    '<a href="#" class="buttonOrange choosable" data-action="denyTask">Deny</a>' +
                    '<a href="#" class="buttonOrange choosable" data-action="requeueTask">Requeue</a>' +
                    '<a href="#" class="buttonOrange detailsLink">Details</a>';
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
                   eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "claimedTask");
                   eventManager.sendEvent("refreshTasksMenu");
                }, this), function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unknown");
                });
            }
        },
        
        approveTask: function(id, callback) {
            event.preventDefault();
            
            if(id) {
                workflowManager.completeTask(id, {"decision": "accept"}, _.bind(function() {
                    callback(this);
                }, this), function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unknown");
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
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unknown");
                    callback(this);
                });
            }
        },
        
        requeueTask: function(id, callback) {
            if(id) {
                workflowManager.assignTaskToUser(id, "", _.bind(function() {
                    callback(this);
                }, this), function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unknown");
                    callback(this);
                });
            }
        },
        
        markAsChoosen: function(event) {
            event.preventDefault();
            
            if (!$(event.target).hasClass("choosen-decision")) {
                $(event.target).parent().find("a").removeClass("choosen-decision");
                $(event.target).addClass("choosen-decision");
                
                if ($(event.target).attr('data-action') === 'denyTask') {
                    $(event.target).parent().parent().after(this.getReasonInputRow());
                } else {
                    if($(event.target).parent().parent().next().find("input[name=denyReason]").length !== 0) {
                        $(event.target).parent().parent().next().remove();
                    }
                }
                
            } else {
                $(event.target).parent().find("a").removeClass("choosen-decision");
                if($(event.target).parent().parent().next().find("input[name=denyReason]").length !== 0) {
                    $(event.target).parent().parent().next().remove();
                }
            }
            
            this.$el.accordion('refresh');
            this.setSaveLinkAsActiveOrInactive($(event.target));
        },
        
        getReasonInputRow: function() {
            return '<tr><td colspan="4"><input name="denyReason" value="Enter a reason for denying this request" /></td></tr>';
        },
        
        setSaveLinkAsActiveOrInactive: function(element) {
            var taskContainer = element.parent().parent().parent();
            if (taskContainer.find(".choosen-decision").size() > 0) {
                taskContainer.parent().parent().find(".saveLink").removeClass("buttonGrey").addClass("buttonOrange");
            } else {
                taskContainer.parent().parent().find(".saveLink").removeClass("buttonOrange").addClass("buttonGrey");
            }
        },
        
        actionNumberToExecute: 0,
        
        save: function(event) {
            var actionsToRun = [], taskId, action, actionToRun, actionPointer, counter = 0, actionFinished, denyReason;
            
            event.preventDefault();
            
            $(event.target).parent().find("table").find("tbody").find("tr").each(function(index) {
                action = $(this).find(".choosen-decision").attr('data-action');
                taskId = $(this).find("input[name=taskId]").val();
                denyReason = $(this).find("input[name=denyReason]").val();
                
                if(action) {
                    actionsToRun.push({actionType: action, taskId: taskId, denyReason: denyReason});
                }
            });
            
            this.actionNumberToExecute = actionsToRun.length;
            
            actionFinished = function(self) {
                counter++;
                if (counter === self.actionNumberToExecute) {
                    eventManager.sendEvent("refreshTasksMenu");
                }
            };
            
            for (actionPointer in actionsToRun) {
                actionToRun = actionsToRun[actionPointer];
                this[actionToRun.actionType](actionToRun.taskId, actionFinished, actionToRun.denyReason);
            }
        },
        
        resetChoosen: function(event) {
           event.preventDefault();
           $(event.target).parent().find("a").removeClass("choosen-decision");
           this.setSaveLinkAsActiveOrInactive($(event.target));
           $(event.target).parent().find("[name=denyReason]").remove();
        },
        
        prepareParams: function(params) {
            return $.map(params, function(v, k) {
                return v;
            });
        }
    }); 
    
    return TasksMenuView;
});


