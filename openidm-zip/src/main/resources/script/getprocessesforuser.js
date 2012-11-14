var users = {};

getUser = function(userId) {
    if (!users[userId]) {
        var user = openidm.read("managed/user/"+userId);
        if (!user) {
            var params = {
                "_queryId": "for-userName",
                "uid": userId
            };
            result = openidm.query("managed/user", params);
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

contains = function(object, comaseparatedList) {
    var items = comaseparatedList.split(',');
    for (var i = 0; i < items.length; i++) {
        if (items[i] === object) {
            return true;
        }
    }
    return false;
}

isProcessAvalibleForUser = function(processAccessPolicies, processDefinition, userRoles) {
    for (var i = 0; i < processAccessPolicies.length; i++) {
        var props =  processAccessPolicies[i].properties;
        var key = props.key;
        var matches = props.matches;
        var requiresPrivilage = props.requiresPrivilage;
        
        if (processDefinition[key].match(matches)) {
            if (!contains(requiresPrivilage, userRoles)) {
                return false;
            }
        }
    }
    return true;
}

getProcessesAvalibleForUser = function(processDefinitions, userRoles) {
    var processesAvalibleToUser = [];
    var processAccessPolicies = openidm.read("config/process/access").policies;
    for (var i = 0; i < processDefinitions.length; i++) {
        var processDefinition = processDefinitions[i];
        if (isProcessAvalibleForUser(processAccessPolicies, processDefinition, userRoles)) {
            processesAvalibleToUser.push(processDefinition);
        }
    }
    return processesAvalibleToUser;
}

if (!request.params || (!request.params.userId && !request.params.userName)) {
    throw "Required params: userId or userName";
} else {
    if (request.params.userId) {
        user = getUser(request.params.userId);
        roles = user.roles;
    } else {
        user = getUser(request.params.userName);
        roles = user.roles;
    }
}





//code:
var processDefinitions = {};
var users = {};
var processesForUser = [];


var processDefinitionsQueryParams = {
    "_queryId": "query-all-ids",
};

processDefinitions = openidm.query("workflow/processdefinition", processDefinitionsQueryParams).result;

processesForUser = getProcessesAvalibleForUser(processDefinitions, roles);

//return value
processesForUser
