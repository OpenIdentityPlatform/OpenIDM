var processInstances = {};
var users = {};

getProcessInstance = function(processInstanceId) {
    if (!processInstances[processInstanceId]) {
        processInstance = openidm.read("workflow/processinstance/"+processInstanceId);
        processInstances[processInstanceId] = processInstance;
    }
    return processInstances[processInstanceId];
}

getUser = function(userId) {
    if (!users[userId]) {
        user = openidm.read("managed/user/"+userId);
        if (!user) {
            var params = {
                "_query-id": "for-userName",
                "uid": userId
            };
            result = openidm.query("managed/user", params);
            if (result.result && result.result.length == 1) {
                user = result.result[0];
            }
            if (!user) {
                user = openidm.read("repo/internal/user/"+userId);
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





if (!request.params || (!request.params.userId && !request.params.userName)) {
    throw "Required params: userId or userName";
} else {
    if (request.params.userId) {
        user = getUser(request.params.userId);
        userId = user._id; 
        roles = user.roles;
        userName = user.userName;
    } else {
        user = getUser(request.params.userName);
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
    tasks = openidm.query("workflow/taskinstance", userAssignedTasksQueryParams).result;
} else {
    var tasksUniqueMap = {};
    
    var userCandidateTasksQueryParams = {
      "_query-id": "filtered-query",
      "candidate": userName
    };
    userCandidateTasks = openidm.query("workflow/taskinstance", userCandidateTasksQueryParams).result;
    for (i = 0; i < userCandidateTasks.length; i++) {
        tasksUniqueMap[userCandidateTasks[i]._id] = userCandidateTasks[i];
    }
    
    var userGroupCandidateTasksQueryParams = {
      "_query-id": "filtered-query",
      "candidate-group": roles
    };
    userGroupCandidateTasks = openidm.query("workflow/taskinstance", userGroupCandidateTasksQueryParams).result;
    for (i = 0; i < userGroupCandidateTasks.length; i++) {
        tasksUniqueMap[userGroupCandidateTasks[i]._id] = userGroupCandidateTasks[i];
    }
    
    tasks = [];
    for (taskId in tasksUniqueMap) {
        tasks.push(tasksUniqueMap[taskId]);
    }
}






var view = {};

for (i = 0; i < tasks.length; i++) {
    taskId = tasks[i]._id;
    task = openidm.read("workflow/taskinstance/"+taskId);
    
    if (!view[task.processDefinitionId+"|"+task.taskDefinitionKey]) {
        view[task.processDefinitionId+"|"+task.taskDefinitionKey] = {name : task.name};
    }
    view[task.processDefinitionId+"|"+task.taskDefinitionKey][task._id] = {task : task};
}

for (taskDefinition in view) {
    for (taskInstanceId in view[taskDefinition]) {
        if (taskInstanceId !== 'name') {
            taskInstance = view[taskDefinition][taskInstanceId];
            processInstance = getProcessInstance(taskInstance.task.processInstanceId);
            view[taskDefinition][taskInstanceId].businessKey = processInstance.businessKey;
            view[taskDefinition][taskInstanceId].startTime = processInstance.startTime;
            view[taskDefinition][taskInstanceId].startUserId = processInstance.startUserId;
            view[taskDefinition][taskInstanceId].startUserDisplayable = getDisplayableOf(getUser(processInstance.startUserId));
            view[taskDefinition][taskInstanceId].processDefinitionId = processInstance.processDefinitionId;
        }
    }
}



view