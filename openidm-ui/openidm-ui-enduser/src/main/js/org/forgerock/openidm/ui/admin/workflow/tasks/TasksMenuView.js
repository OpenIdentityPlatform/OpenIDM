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
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "UserDelegate",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/commons/ui/common/components/popup/PopupCtrl"
], function(workflowManager, eventManager, constants, conf, uiUtils, userDelegate, dateUtil, popupCtrl) {
    var TasksMenuView = Backbone.View.extend({

        events: {
            "click .detailsLink": "showTask",
            "change select[name=assignedUser]": "claimTask",
            "mouseenter .userLink": "showUser",
            "click .closeLink" : "hideDetails",
            "click [name=requeueButton]" : "requeueTaskHandler"
        },

        tasks: {},
        processes: {},
        model: {},

        requeueTaskHandler: function(event) {
            event.preventDefault();

            var id = $(event.target).closest("tr").prev().find("[name=taskId]").val();

            this.requeueTask(id, _.bind(function() {
                this.hideDetails();
                eventManager.sendEvent("refreshTasksMenu");
            }, this));
        },

        hideDetails: function(event) {
            if(event) {
                event.preventDefault();
            }

            $("#taskDetails").closest("tr").remove();
        },

        showTask: function(event) {
            event.preventDefault();

            var id = $(event.target).parent().parent().find("input[name=taskId]").val(), task;

            if(id) {
                task = this.getTaskFromCacheById(id);

                if(task) {
                    eventManager.sendEvent("showTaskDetailsRequest", {
                        "task": task,
                        "definition": task.taskDefinition,
                        "category": this.category,
                        "id": id
                    });
                }
            }
        },

        getTaskFromCacheById: function(id) {
            var processName, process, i;

            for(processName in this.tasks) {
                process = this.tasks[processName];

                for(i = 0; i < process.tasks.length; i++) {
                    if(process.tasks[i]._id === id) {
                        return process.tasks[i];
                    }
                }
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
            this.callback = callback;

            this.$el.prev().show();
            this.$el.show();

            if(category === "all") {
                workflowManager.getAllTaskUsingEndpoint(conf.loggedUser._id, _.bind(this.displayTasks, this), _.bind(this.errorHandler, this));
            } else if(category === "assigned") {
                workflowManager.getMyTaskUsingEndpoint(conf.loggedUser._id, _.bind(this.displayTasks, this), _.bind(this.errorHandler, this));
            }
        },

        errorHandler: function() {
            this.$el.html('');
            if(this.category === "assigned") {
                this.$el.append('<h5 class="text-center">' + $.t("openidm.ui.admin.tasks.TasksMenuView.noTasksAssigned") + '</h5>');
            } else {
                this.$el.append('<h5 class="text-center">' + $.t("openidm.ui.admin.tasks.TasksMenuView.noTasksInGroupQueue") + '</h5>');
            }
        },

        displayTasks: function(tasks) {
            var process, data, processName, taskType, taskName, actions, i, task, active, before, types = 0;
            before = this.$el.find(".ui-accordion-header").length;
            this.tasks = tasks;
            this.$el.html('');

            for(processName in tasks) {
                process = tasks[processName];

                types++;

                data = {
                    processName: process.name,
                    taskName: process.name,
                    count: process.tasks.length,
                    headers: this.getParamsForTaskType(taskType),
                    batchOperation: this.category === 'assigned',
                    id: process.name.replace(/\s/g, '') + this.category + types,
                    taskCount: process.tasks.length,
                    tasks: []
                };

                for(i = 0; i < process.tasks.length; i++) {
                    task = process.tasks[i];
                    data.tasks.push(this.prepareParamsFromTask(task));
                }

                this.$el.append(uiUtils.fillTemplateWithData("templates/admin/workflow/tasks/ProcessUserTaskTableTemplate.html", data));
            }

            active = false;
            if(this.$el.hasClass('ui-accordion') && before === types) {
                active = this.$el.accordion("option", "active");
            }

            this.$el.accordion({header: "table-header", heightStyle: "content", collapsible: true, autoHeight: false, active: active});

            this.refreshAssignedSelectors();

            if(this.callback) {
                this.callback();
            }
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
                "initiator": task.startUserDisplayable + " ",
                "key" : task.businessKey + " ",
                "requested" : dateUtil.formatDate(task.startTime),
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
            _.each($("select[name=assignedUser]"), _.bind(function(target) {
                var assignedUser = $(target).parent().parent().find('input[name=assignedUser]').val(), task, i, user;

                task = this.getTaskFromCacheById($(target).closest("tr").find("input[name=taskId]").val());

                if(task) {
                    for(i = 0; i < task.usersToAssign.users.length; i++) {
                        user = task.usersToAssign.users[i];

                        if($(target).find("option[value='"+ user.username +"']").length === 0 && user.username !== conf.loggedUser.userName) {
                            $(target).append('<option value="'+ user.username +'">'+ user.displayableName +'</option');
                        }
                    }
                }

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
            }, this));

            console.log("refreshing selectors");
        },

        getUserLink: function(userName, userNameId, requesterDisplayName) {
            return "<a href='#' class='userLink'>"+ userName+ "</a><input type='hidden' name='uid' value='"+ userNameId +"' /><input type='hidden' name='requesterDisplayName' value='"+ requesterDisplayName +"' />";
        },

        getActions: function(task) {
            if(this.category === 'all') {
                return '<select name="assignedUser" class="select-static-medium"><option value="null">' + $.t("common.task.unassigned")
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
                workflowManager.assignTaskToUser(id, null, _.bind(function() {
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