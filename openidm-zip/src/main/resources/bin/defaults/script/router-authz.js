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

/*
 * This script is called from the router "onRequest" trigger, to enforce a central
 * set of authorization rules.
 * 
 * This default implemention simply restricts requests via HTTP to users that are assigned
 * an "openidm-admin" role, and optionally to those that authenticate with TLS mutual
 * authentication (assigned an "openidm-cert" role).
 */

function isMyTask() {
    var taskInstanceId = request.id.split("/")[2];
    var taskInstance = openidm.read("workflow/taskinstance/" + taskInstanceId);
    return taskInstance.assignee === request.parent.security.username;
}

function canUpdateTask() {
    var taskInstanceId = request.id.split("/")[2];
    return isMyTask() || isUserCandidateForTask(taskInstanceId);
}

function isUserCandidateForTask(taskInstanceId) {
    
    var userCandidateTasksQueryParams = {
        "_queryId": "filtered-query",
        "taskCandidateUser": request.parent.security.username
    };
    var userCandidateTasks = openidm.query("workflow/taskinstance", userCandidateTasksQueryParams).result;
    for (var i = 0; i < userCandidateTasks.length; i++) {
        if (taskInstanceId === userCandidateTasks[i]._id) {
            return true;
        }
    }
        
    var roles = "";
    for (var i = 0; i < request.parent.security['openidm-roles'].length; i++) {
        var role = request.parent.security['openidm-roles'][i];
        if (i === 0) {
            roles = role;
        } else {
            roles = roles + "," + role;
        }
    }
    
    var userGroupCandidateTasksQueryParams = {
        "_queryId": "filtered-query",
        "taskCandidateGroup": roles
    };    
    var userGroupCandidateTasks = openidm.query("workflow/taskinstance", userGroupCandidateTasksQueryParams).result;
    for (var i = 0; i < userGroupCandidateTasks.length; i++) {
        if (taskInstanceId === userGroupCandidateTasks[i]._id) {
            return true;
        }
    }
    
    return false;
}

function isAllowedToStartProcess() {
    var processDefinitionId = request.value._processDefinitionId;
    return isProcessOnUsersList(processDefinitionId);
}

function isOneOfMyWorkflows() {
    var processDefinitionId = request.id.split("/")[2];
    return isProcessOnUsersList(processDefinitionId);
}

function isProcessOnUsersList(processDefinitionId) {
    var processesForUserQueryParams = {
        "_queryId": "query-processes-for-user",
        "userId": request.parent.security.userid.id
    };
    var processesForUser = openidm.query("endpoint/getprocessesforuser", processesForUserQueryParams);
      
    var isProcessOneOfUserProcesses = false;
    for (var i = 0; i < processesForUser.length; i++) {
        var processForUser = processesForUser[i];
        if (processDefinitionId === processForUser._id) {
            isProcessOneOfUserProcesses = true;
        }
    }
    return isProcessOneOfUserProcesses;
}

function isQueryOneOf(allowedQueries) {
    if (
            request.method === "query" &&
            allowedQueries[request.id] &&
            contains(allowedQueries[request.id], request.params["_queryId"])
       )
    {
        return true
    }
    
    return false;
}

function checkIfUIIsEnabled(param) {
    var ui_config = openidm.read("config/ui/configuration");
    var returnVal = false;
    return (ui_config && ui_config.configuration && ui_config.configuration[param]);
}

function ownDataOnly() {
    var userId = "";
    
    userId = request.id.match(/managed\/user\/(.*)/i);
    if (userId && userId.length === 2)
    {
        userId = userId[1];
    }
    
    if (request.params && request.params.userId)
    {   
        // something funny going on if we have two different values for userId
        if (userId !== null && userId.length && userId !== request.params.userId) {
            return false;
        } 
        userId = request.params.userId;
    }
    
    if (request.value && request.value.userId)
    {
        // something funny going on if we have two different values for userId
        if (userId !== null  && userId.length && userId !== request.params.userId) {
            return false;
        } 
        userId = request.value.userId;
    }
    
    return userId === request.parent.security.userid.id;

}

function managedUserRestrictedToAllowedRoles(allowedRolesList) {
    var i = 0,requestedRoles = [],params = {};
    
    if (!request.id.match(/^managed\/user/)) {
        return true;
    }

    if (request.value) {
        params = request.value;
    }
    else { // this would be strange, but worth checking
        return true; // true because they don't appear to be setting anything
    }

    
    if (request.method === "patch" || (request.method === "action" && request.params["_action"] === "patch")) {
        for (i in params) {
            if ((params[i].test && params[i].test.match(/^\/?roles$/)) ||
                (params[i].add && params[i].add.match(/^\/?roles$/)) || 
                (params[i].replace && params[i].replace.match(/^\/?roles$/))) {
                
                requestedRoles = requestedRoles.concat(params[i].value.split(','))
            }
        }
    } else if ((request.method === "create" || request.method === "update") && 
                params && (params.roles || params["/roles"])) {
        
        if (typeof params.roles !== "string" && typeof params["/roles"] !== "string") { // this would also be strange, but worth checking
            return false; // false because I don't know (and so don't trust) what they are trying to set.
        }
        
        if (params.roles) {
            requestedRoles = requestedRoles.concat(params.roles.split(","));
        }
        if (params["/roles"]) {
            requestedRoles = requestedRoles.concat(params['/roles'].split(","));
        }
    }
    
    if (requestedRoles.length) { // if there are no requested roles, then no problem

        // we could accept a csv list or an array of roles for the rolesList arg.
        if (typeof allowedRolesList === "string") {
            allowedRolesList = allowedRolesList.split(',');
        }
        
        for (i in requestedRoles) {
            if (! contains(allowedRolesList, requestedRoles[i])) {
                return false;
            }
        }
    }
    return true;
}

function disallowQueryExpression() {
    if (request.params && typeof request.params['_queryExpression'] != "undefined") {
        return false;
    }
    return true;
}

//////// Do not alter functions below here as part of your authz configuration



function passesAccessConfig(id, roles, method, action) {
    for (var i = 0; i < httpAccessConfig.configs.length; i++) {
        var config = httpAccessConfig.configs[i];
        var pattern = config.pattern;
        // Check resource ID
        if (matchesResourceIdPattern(id, pattern)) {
            // Check excludePatterns
            if (typeof(config.excludePatterns) != 'undefined' && config.excludePatterns != null) {
                var excluded = config.excludePatterns.split(',');
                var ex = false;
                for (var j = 0; j < excluded.length; j++) {
                    if (matchesResourceIdPattern(id, excluded[j])) {
                        ex = true;
                        break;
                    }
                }
                if (ex) {
                    continue;
                }
            }
            // Check roles
            if (containsItems(roles, config.roles.split(','))) {
                // Check method
                if (method == 'undefined' || containsItem(method, config.methods)) {
                    // Check action
                    if (action == 'undefined' || action == "" || containsItem(action, config.actions)) {
                        if (typeof(config.customAuthz) != 'undefined' && config.customAuthz != null) {
                            if (eval(config.customAuthz)) {
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }
                }
            }
        }
    }
    return false;
}

function matchesResourceIdPattern(id, pattern) {
    if (pattern == "*") {
        // Accept all patterns
        return true;
    } else if (id == pattern) {
        // pattern matches exactly
        return true;
    } else if (pattern.indexOf("/*", pattern.length - 2) !== -1) {
        // Ends with "/*" or "/"
        // See if parent pattern matches
        var parentResource = pattern.substring(0, pattern.length - 1);
        if (id.length >= parentResource.length && id.substring(0, parentResource.length) == parentResource) {
            return true
        }
    }
    return false;
}

function containsItems(items, configItems) {
    if (configItems == "*") {
        return true;
    }
    for (var i = 0; i < items.length; i++) {
        if (containsIgnoreCase(configItems, items[i])) {
            return true;
        }
    }
    return false
}

function containsItem(item, configItems) {
    if (configItems == "*") {
        return true;
    }
    return containsIgnoreCase(configItems.split(','), item);
}

function containsIgnoreCase(a, o) {
    if (typeof(a) != 'undefined' && a != null) {
        for (var i = 0; i <= a.length; i++) {
            var str1 = o, str2 = a[i];
            if (typeof(o) != 'undefined' && o != null) {
                str1 = o.toLowerCase();
            }
            if (typeof(a[i]) != 'undefined' && a[i] != null) {
                str2 = a[i].toLowerCase();
            }
            if (str1 === str2) {
                return true;
            }
        }
    }
    return false;
}

function contains(a, o) {
    if (typeof(a) != 'undefined' && a != null) {
        for (var i = 0; i <= a.length; i++) {
            if (a[i] === o) {
                return true;
            }
        }
    }
    return false;
}

function allow() {
    if (request.parent == null || request.parent == undefined || request.parent.type != 'http') {
        return true;
    }
    
    var roles = request.parent.security['openidm-roles'];
    var action = "";
    if (request.params && request.params['_action']) {
        action = request.params['_action'];
    }
    
    // Check REST requests against the access configuration
    if (request.parent.type == 'http') {
        logger.debug("Access Check for HTTP request for resource id: " + request.id);
        if (passesAccessConfig(request.id, roles, request.method, action)) {
            logger.debug("Request allowed");
            return true;
        }
    }
}

// Load the access configuration script
load("script/access.js");

if (!allow()) {
//    java.lang.System.out.println(request);
    throw { 
        "openidmCode" : 403, 
        "message" : "Access denied"
    } 
}
