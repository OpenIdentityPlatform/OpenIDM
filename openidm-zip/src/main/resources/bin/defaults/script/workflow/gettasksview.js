/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 ForgeRock AS. All Rights Reserved
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

if (request.method !== "query") {
    throw {
        "code" : 403,
        "message" : "Access denied"
    };
}

(function () {
    var processInstances = {},
        users = {},
        taskDefinitions = {},
        usersWhoCanBeAssignedToTask = {},
        getProcessInstance = function(processInstanceId) {
            var processInstance;
            if (!processInstances[processInstanceId]) {
                processInstance = openidm.read("workflow/processinstance/"+processInstanceId);
                processInstances[processInstanceId] = processInstance;
            }
            return processInstances[processInstanceId];
        },

        getTaskDefinition = function(processDefinitionId, taskDefinitionKey) {
            var taskDefinitionQueryParams,taskDefinition;
            if (!taskDefinitions[processDefinitionId+"|"+taskDefinitionKey]) {
                taskDefinition = openidm.read("workflow/processdefinition/" + processDefinitionId + "/taskdefinition/" + taskDefinitionKey)
                taskDefinitions[processDefinitionId+"|"+taskDefinitionKey] = taskDefinition;
            }
            return taskDefinitions[processDefinitionId+"|"+taskDefinitionKey];
        },
        getUsersWhoCanBeAssignedToTask = function(taskId) {
            var usersWhoCanBeAssignedToTaskQueryParams = {
                    "_queryId": "query-by-task-id",
                    "taskId": taskId
                },
                isTaskManager = false,
                i,
                usersWhoCanBeAssignedToTaskResult = { users : [] };

            if (!usersWhoCanBeAssignedToTask[taskId]) {

                for(i = 0; i < context.security.authorization.roles.length; i++) {
                    if(context.security.authorization.roles[i] === 'openidm-tasks-manager') {
                        isTaskManager = true;
                        break;
                    }
                }

                if(isTaskManager) {
                    usersWhoCanBeAssignedToTaskResult = openidm.query("endpoint/getavailableuserstoassign", usersWhoCanBeAssignedToTaskQueryParams);
                }
                usersWhoCanBeAssignedToTask[taskId] = usersWhoCanBeAssignedToTaskResult;
            }
            return usersWhoCanBeAssignedToTask[taskId];
        },

        join = function (arr, delim) {
            var returnStr = "",i=0;
            for (i=0; i<arr.length; i++) {
                returnStr = returnStr + arr[i] + delim;
            }
            return returnStr.replace(new RegExp(delim + "$"), '');
        },
        roles = context.security.authorization.roles.join(","),
        userName = context.security.authenticationId,
        tasks,
        taskId,
        task,
        userAssignedTasksQueryParams,
        tasksUniqueMap,
        userCandidateTasksQueryParams,
        userCandidateTasks,
        userGroupCandidateTasksQueryParams,
        userGroupCandidateTasks,
        taskDefinition,
        taskInstance,
        processInstance,
        i,
        view = {};

    if (request.additionalParameters.viewType === 'assignee') {
        userAssignedTasksQueryParams = {
            "_queryId": "filtered-query",
            "assignee": userName
        };
        tasks = openidm.query("workflow/taskinstance", userAssignedTasksQueryParams).result;
    } else {
        tasksUniqueMap = {};

        userCandidateTasksQueryParams = {
          "_queryId": "filtered-query",
          "taskCandidateUser": userName
        };
        userCandidateTasks = openidm.query("workflow/taskinstance", userCandidateTasksQueryParams).result;
        for (i = 0; i < userCandidateTasks.length; i++) {
            tasksUniqueMap[userCandidateTasks[i]._id] = userCandidateTasks[i];
        }

        userGroupCandidateTasksQueryParams = {
          "_queryId": "filtered-query",
          "taskCandidateGroup": roles
        };
        userGroupCandidateTasks = openidm.query("workflow/taskinstance", userGroupCandidateTasksQueryParams).result;
        for (i = 0; i < userGroupCandidateTasks.length; i++) {
            tasksUniqueMap[userGroupCandidateTasks[i]._id] = userGroupCandidateTasks[i];
        }

        tasks = [];
        for (taskId in tasksUniqueMap) {
            if (tasksUniqueMap.hasOwnProperty(taskId)) {
                tasks.push(tasksUniqueMap[taskId]);
            }
        }
    }

    //building view

    for (i = 0; i < tasks.length; i++) {
        taskId = tasks[i]._id;
        task = openidm.read("workflow/taskinstance/"+taskId);

        if (!view[task.processDefinitionId+"|"+task.taskDefinitionKey]) {
            view[task.processDefinitionId+"|"+task.taskDefinitionKey] = {name : task.name, tasks : []};
        }
        view[task.processDefinitionId+"|"+task.taskDefinitionKey].tasks.push(task);
    }

    for (taskDefinition in view) {
        if (view.hasOwnProperty(taskDefinition)) {
            for (i = 0; i < view[taskDefinition].tasks.length; i++) {
                taskInstance = view[taskDefinition].tasks[i];
                processInstance = getProcessInstance(taskInstance.processInstanceId);
                view[taskDefinition].tasks[i].businessKey = processInstance.businessKey;
                view[taskDefinition].tasks[i].startTime = processInstance.startTime;
                view[taskDefinition].tasks[i].startUserId = processInstance.startUserId;
                view[taskDefinition].tasks[i].startUserDisplayable = userName;
                view[taskDefinition].tasks[i].processDefinitionId = processInstance.processDefinitionId;
                view[taskDefinition].tasks[i].taskDefinition = getTaskDefinition(taskInstance.processDefinitionId, taskInstance.taskDefinitionKey);
                view[taskDefinition].tasks[i].usersToAssign = getUsersWhoCanBeAssignedToTask(taskInstance._id);
            }
        }
    }

    //return value
    return [view];

}());
