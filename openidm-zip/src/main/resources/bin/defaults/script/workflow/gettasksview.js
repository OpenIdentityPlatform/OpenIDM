/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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
        "openidmCode" : 403, 
        "message" : "Access denied"
    } 
}

var processInstances = {};
var users = {};
var taskDefinitions = {};
var usersWhoCanBeAssignedToTask = {};

var getProcessInstance = function(processInstanceId) {
    if (!processInstances[processInstanceId]) {
        var processInstance = openidm.read("workflow/processinstance/"+processInstanceId);
        processInstances[processInstanceId] = processInstance;
    }
    return processInstances[processInstanceId];
}

var getTaskDefinition = function(processDefinitionId, taskDefinitionKey) {
    if (!taskDefinitions[processDefinitionId+"|"+taskDefinitionKey]) {
        var taskDefinitionQueryParams = {
            "_queryId": "query-taskdefinition",
            "processDefinitionId": processDefinitionId,
            "taskDefinitionKey": taskDefinitionKey
        };
        var taskDefinition = openidm.query("workflow/taskdefinition", taskDefinitionQueryParams);
        taskDefinitions[processDefinitionId+"|"+taskDefinitionKey] = taskDefinition;
    }
    return taskDefinitions[processDefinitionId+"|"+taskDefinitionKey];
}

var getUsersWhoCanBeAssignedToTask = function(taskId) {
    if (!usersWhoCanBeAssignedToTask[taskId]) {
        var usersWhoCanBeAssignedToTaskQueryParams = {
                "_queryId": "query-by-task-id",
                "taskId": taskId
            };
        
        var isTaskManager = false;
        for(var i = 0; i < request.parent.security['openidm-roles'].length; i++) {
            if(request.parent.security['openidm-roles'][i] === 'tasks-manager') {
                isTaskManager = true;
                break;
            }
        }        
        
        var usersWhoCanBeAssignedToTaskResult = { users : []};
        if(isTaskManager) {        
            usersWhoCanBeAssignedToTaskResult = openidm.query("endpoint/getavailableuserstoassign", usersWhoCanBeAssignedToTaskQueryParams);
        }
        usersWhoCanBeAssignedToTask[taskId] = usersWhoCanBeAssignedToTaskResult;
    }
    return usersWhoCanBeAssignedToTask[taskId];
}

var getUser = function(userId) {
    if (!users[userId]) {
        var user = openidm.read("managed/user/"+userId);
        if (!user) {
            var params = {
                "_queryId": "for-userName",
                "uid": userId
            };
            var result = openidm.query("managed/user", params);
            if (result.result && result.result.length == 1) {
                var user = result.result[0];
            }
            if (!user) {
                var user = openidm.read("repo/internal/user/"+userId);
            }
            
            if(!user) {
                throw "Bad userId"; 
            }
        }
        users[userId] = user;
    }
    return users[userId];
}

var getDisplayableOf = function(user) {
    if (user.givenName || user.familyName) {
        return user.givenName + " " + user.familyName;
    } else {
        return user.userName ? user.userName : user._id;
    }
}




//code:
var userId, roles, userName;

if (!request.params || (!request.params.userId && !request.params.userName)) {
    throw "Required params: userId or userName";
} else {
    if (request.params.userId) {
        var user = getUser(request.params.userId);
        userId = user._id; 
        roles = user.roles;
        userName = user.userName ? user.userName : user._id;
    } else {
        var user = getUser(request.params.userName);
        userId = user._id; 
        roles = user.roles;
        userName = user.userName ? user.userName : user._id;
    }
}

var tasks;
if (request.params.viewType === 'assignee') {
    var userAssignedTasksQueryParams = {
        "_queryId": "filtered-query",
        "assignee": userName
    };
    tasks = openidm.query("workflow/taskinstance", userAssignedTasksQueryParams).result;
} else {
    var tasksUniqueMap = {};
    
    var userCandidateTasksQueryParams = {
      "_queryId": "filtered-query",
      "taskCandidateUser": userName
    };
    var userCandidateTasks = openidm.query("workflow/taskinstance", userCandidateTasksQueryParams).result;
    for (var i = 0; i < userCandidateTasks.length; i++) {
        tasksUniqueMap[userCandidateTasks[i]._id] = userCandidateTasks[i];
    }
    
    var userGroupCandidateTasksQueryParams = {
      "_queryId": "filtered-query",
      "taskCandidateGroup": roles
    };
    var userGroupCandidateTasks = openidm.query("workflow/taskinstance", userGroupCandidateTasksQueryParams).result;
    for (var i = 0; i < userGroupCandidateTasks.length; i++) {
        tasksUniqueMap[userGroupCandidateTasks[i]._id] = userGroupCandidateTasks[i];
    }
    
    tasks = [];
    for (taskId in tasksUniqueMap) {
        tasks.push(tasksUniqueMap[taskId]);
    }
}


//building view
var view = {};

for (var i = 0; i < tasks.length; i++) {
    var taskId = tasks[i]._id;
    var task = openidm.read("workflow/taskinstance/"+taskId);
    
    if (!view[task.processDefinitionId+"|"+task.taskDefinitionKey]) {
        view[task.processDefinitionId+"|"+task.taskDefinitionKey] = {name : task.name, tasks : []};
    }
    view[task.processDefinitionId+"|"+task.taskDefinitionKey].tasks.push(task);
}

for (var taskDefinition in view) {
    for (var i = 0; i < view[taskDefinition].tasks.length; i++) {
        var taskInstance = view[taskDefinition].tasks[i];
        var processInstance = getProcessInstance(taskInstance.processInstanceId);
        view[taskDefinition].tasks[i].businessKey = processInstance.businessKey;
        view[taskDefinition].tasks[i].startTime = processInstance.startTime;
        view[taskDefinition].tasks[i].startUserId = processInstance.startUserId;
        view[taskDefinition].tasks[i].startUserDisplayable = getDisplayableOf(getUser(processInstance.startUserId));
        view[taskDefinition].tasks[i].processDefinitionId = processInstance.processDefinitionId;
        view[taskDefinition].tasks[i].taskDefinition = getTaskDefinition(taskInstance.processDefinitionId, taskInstance.taskDefinitionKey);
        view[taskDefinition].tasks[i].usersToAssign = getUsersWhoCanBeAssignedToTask(taskInstance._id);
    }
}

//return value
view