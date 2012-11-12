var processInstances = {};
var users = {};
var taskDefinitions = {};

getProcessInstance = function(processInstanceId) {
    if (!processInstances[processInstanceId]) {
        var processInstance = openidm.read("workflow/processinstance/"+processInstanceId);
        processInstances[processInstanceId] = processInstance;
    }
    return processInstances[processInstanceId];
}

getTaskDefinition = function(processDefinitionId, taskDefinitionKey) {
    if (!taskDefinitions[processDefinitionId+"|"+taskDefinitionKey]) {
        var taskDefinitionQueryParams = {
            "_query-id": "query-taskdefinition",
            "processDefinitionId": processDefinitionId,
            "taskDefinitionKey": taskDefinitionKey
        };
        var taskDefinition = openidm.query("workflow/taskdefinition", taskDefinitionQueryParams);
        taskDefinitions[processDefinitionId+"|"+taskDefinitionKey] = taskDefinition;
    }
    return taskDefinitions[processDefinitionId+"|"+taskDefinitionKey];
}

getUser = function(userId) {
    if (!users[userId]) {
        var user = openidm.read("managed/user/"+userId);
        if (!user) {
            var params = {
                "_query-id": "for-userName",
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

getDisplayableOf = function(user) {
    if (user.givenName || user.familyName) {
        return user.givenName + " " + user.familyName;
    } else {
        return user.userName ? user.userName : user._id;
    }
}




//code:
if (!request.params || (!request.params.userId && !request.params.userName)) {
    throw "Required params: userId or userName";
} else {
    if (request.params.userId) {
        var user = getUser(request.params.userId);
        userId = user._id; 
        roles = user.roles;
        userName = user.userName;
    } else {
        var user = getUser(request.params.userName);
        userId = user._id; 
        roles = user.roles;
        userName = user.userName;
    }
}

if (request.params.viewType === 'assignee') {
    var userAssignedTasksQueryParams = {
        "_query-id": "filtered-query",
        "assignee": userName
    };
    var tasks = openidm.query("workflow/taskinstance", userAssignedTasksQueryParams).result;
} else {
    var tasksUniqueMap = {};
    
    var userCandidateTasksQueryParams = {
      "_query-id": "filtered-query",
      "candidate": userName
    };
    var userCandidateTasks = openidm.query("workflow/taskinstance", userCandidateTasksQueryParams).result;
    for (i = 0; i < userCandidateTasks.length; i++) {
        tasksUniqueMap[userCandidateTasks[i]._id] = userCandidateTasks[i];
    }
    
    var userGroupCandidateTasksQueryParams = {
      "_query-id": "filtered-query",
      "candidate-group": roles
    };
    var userGroupCandidateTasks = openidm.query("workflow/taskinstance", userGroupCandidateTasksQueryParams).result;
    for (i = 0; i < userGroupCandidateTasks.length; i++) {
        tasksUniqueMap[userGroupCandidateTasks[i]._id] = userGroupCandidateTasks[i];
    }
    
    var tasks = [];
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
    }
}

//return value
view
