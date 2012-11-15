getUserById = function(userId) {
    var user = openidm.read("managed/user/"+userId);
    if (!user) {
        var user = openidm.read("repo/internal/user/"+userId);
    }
    return user;
}

getUserByName = function(userName) {
    var params = {
        "_queryId": "for-userName",
        "uid": userName
    };
    result = openidm.query("managed/user", params);
    if (result.result && result.result.length == 1) {
        var user = result.result[0];
    }
    if (!user) {
        var user = openidm.read("repo/internal/user/"+userName);
    }
    return user;
}

getDisplayableOf = function(user) {
    if (user.givenName || user.familyName) {
        return user.givenName + " " + user.familyName;
    } else {
        return user.userName ? user.userName : user._id;
    }
}


if (!request.params || !request.params.taskId) {
    throw "Required param: taskId";
}

var task = openidm.read("workflow/taskinstance/" + request.params.taskId);
if (!task) {
    throw "Task Not Found";
}
var taskDefinitionQueryParams = {
    "_queryId": "query-taskdefinition",
    "processDefinitionId": task.processDefinitionId,
    "taskDefinitionKey": task.taskDefinitionKey
};
var taskDefinition = openidm.query("workflow/taskdefinition", taskDefinitionQueryParams);

var usersToAdd = {};
var candidateUsers = [];
var candidateGroups = [];

var taskCandidateUserArray = taskDefinition.taskCandidateUser.toArray();
for (var i = 0; i < taskCandidateUserArray.length; i++) {
    var candidateUserTaskDefinition = taskCandidateUserArray[i];
    candidateUsers.push(candidateUserTaskDefinition.expressionText);
}

var taskCandidateGroupArray = taskDefinition.taskCandidateGroup.toArray();
for (var i = 0; i < taskCandidateGroupArray.length; i++) {
    var candidateGroupTaskDefinition = taskCandidateGroupArray[i];
    candidateGroups.push(candidateGroupTaskDefinition.expressionText);
}


for (var i = 0; i < candidateGroups.length; i++) {
    var candidateGroup = candidateGroups[i];
    var params = {
        "_queryId": "get-users-of-role",
        "role": candidateGroup
    };
    var result = openidm.query("managed/user", params);

    if (result.result && result.result.length > 0) {
        for (var j = 0; j < result.result.length; j++) {
            var user = result.result[j];
            usersToAdd[user.userName] = user;
        }
    }
    
    result = openidm.query("repo/internal/user", params);

    if (result.result && result.result.length > 0) {
        for (var j = 0; j < result.result.length; j++) {
            var user = result.result[j];
            var username = user.userName ? user.userName : user._id;
            usersToAdd[username] = user;
        }
    }
}

for (var i = 0; i < candidateUsers.length; i++) {
    var candidateUser = candidateUsers[i];
    usersToAdd[candidateUser] = user;
}


var avalibleUsersToAssign = { users : []};
for (var userName in usersToAdd) {
    var user = getUserByName(userName);
    if (user) {
        avalibleUsersToAssign.users.push({_id: user._id, username: userName, displayableName: getDisplayableOf(user)});
    }
}

var assigneeUserName = task.assignee;
if (assigneeUserName && assigneeUserName !== '') {
    var user = getUserByName(assigneeUserName);
    if (user) {
        avalibleUsersToAssign.assignee = {_id: user._id, username: assigneeUserName, displayableName: getDisplayableOf(user)};
    }
}

avalibleUsersToAssign
