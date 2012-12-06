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
        var props =  processAccessPolicies[i].propertiesCheck;
        var property = props.property;
        var matches = props.matches;
        var requiresRole = props.requiresRole;
        
        if (processDefinition[property].match(matches)) {
            if (contains(requiresRole, userRoles)) {
                return true;
            }
        }
    }
    return false;
}

getProcessesAvalibleForUser = function(processDefinitions, userRoles) {
    var processesAvalibleToUser = [];
    var processAccessPolicies = openidm.read("config/process/access").workflowAccess;
    for (var i = 0; i < processDefinitions.length; i++) {
        var processDefinition = processDefinitions[i];
        if (isProcessAvalibleForUser(processAccessPolicies, processDefinition, userRoles)) {
            processesAvalibleToUser.push(processDefinition);
        }
    }
    return processesAvalibleToUser;
}







//code:

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
