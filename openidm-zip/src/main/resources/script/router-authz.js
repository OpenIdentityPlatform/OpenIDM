/*! @license 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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

/**
 * A configuration for allowed requests.  Each entry in the config contains a pattern to match
 * against the incoming request ID and, in the event of a match, the associated roles, methods,
 * and actions that are allowed for requests on that particular pattern.
 *
 * pattern:  A pattern to match against an incoming request's resource ID
 * roles:  A comma separated list of allowed roles
 * methods:  A comma separated list of allowed methods
 * actions:  A comma separated list of allowed actions
 * customAuthz: A custom function for additional authorization logic/checks (optional)
 *
 * A single '*' character indicates all possible values.  With patterns ending in "/*", the "*"
 * acts as a wild card to indicate the pattern accepts all resource IDs "below" the specified
 * pattern (prefix).  For example the pattern "managed/*" would match "managed/user" or anything
 * starting with "managed/".  Note: it would not match "managed", which would need to have its 
 * own entry in the config.
 */
var accessConfig = 
{ 
    "configs" : [

      // Anyone can read from these endpoints
        {  
           "pattern"    : "info/*",
           "roles"      : "openidm-reg,openidm-authorized",
           "methods"    : "read",
           "actions"    : "*"
        },
        {  
           "pattern"    : "config/ui/configuration",
           "roles"      : "openidm-reg,openidm-authorized",
           "methods"    : "read",
           "actions"    : "*"
        },
        // These options should only be available anonymously if selfReg is enabled
        {  
           "pattern"    : "config/ui/*",
           "roles"      : "openidm-reg",
           "methods"    : "read",
           "actions"    : "*",
           "customAuthz" : "checkIfUIIsEnabled('selfRegistration')"
        },
        {  
           "pattern"    : "managed/user/*",
           "roles"      : "openidm-reg",
           "methods"    : "create",
           "actions"    : "*",
           "customAuthz" : "checkIfUIIsEnabled('selfRegistration') && managedUserRestrictedToAllowedRoles('openidm-authorized')"
        },

        // Anonymous user can call the siteIdentification endpoint if it is enabled:
        {  
           "pattern"    : "endpoint/siteIdentification",
           "roles"      : "openidm-reg",
           "methods"    : "*",
           "actions"    : "*",
           "customAuthz" : "checkIfUIIsEnabled('siteIdentification')"
        },

        // Anonymous user can call the securityQA endpoint if it enabled:
        {  
           "pattern"    : "endpoint/securityQA",
           "roles"      : "openidm-reg",
           "methods"    : "*",
           "actions"    : "*",
           "customAuthz" : "checkIfUIIsEnabled('securityQuestions')"
        },
        // This is needed by both self reg and security questions
        {  
           "pattern"    : "policy/managed/user/*",
           "roles"      : "openidm-reg",
           "methods"    : "read,action",
           "actions"    : "*",
           "customAuthz" : "checkIfUIIsEnabled('selfRegistration') || checkIfUIIsEnabled('securityQuestions')"
        },

      // openidm-admin can request anything
        {  
            "pattern"   : "*",
            "roles"     : "openidm-admin",
            "methods"   : "*", // default to all methods allowed
            "actions"   : "*" // default to all actions allowed
        },
        
        // admin can request anything in managed/user
        {  
            "pattern"   : "managed/user/*",
            "roles"     : "admin",
            "methods"   : "*", // default to all methods allowed
            "actions"   : "*" // default to all actions allowed
        },
        {  
            "pattern"   : "managed/user",
            "roles"     : "admin",
            "methods"   : "*", // default to all methods allowed
            "actions"   : "*" // default to all actions allowed
        },
        
        // Additional checks for authenticated users
        {  
            "pattern"   : "policy/*",
            "roles"     : "openidm-authorized", // openidm-authorized is logged-in users
            "methods"   : "read,action",
            "actions"   : "*"
        },
        {  
            "pattern"   : "config/ui/*",
            "roles"     : "openidm-authorized",
            "methods"   : "read",
            "actions"   : "*"
        },
        {  
            "pattern"   : "authentication",
            "roles"     : "openidm-authorized",
            "methods"   : "action",
            "actions"   : "reauthenticate"
        },
        {   
            "pattern"   : "*",
            "roles"     : "openidm-authorized",
            "methods"   : "create,read,update,patch,action,query", // note the missing 'delete' - by default, users cannot delete things
            "actions"   : "*",
            "customAuthz" : "ownDataOnly() && managedUserRestrictedToAllowedRoles('openidm-authorized')"
        },

        // enforcement of which notifications you can read and delete is done within the endpoint 
        {
            "pattern"   : "endpoint/usernotifications",
            "roles"     : "openidm-authorized",
            "methods"   : "read,delete",
            "actions"   : "*"
        },
        
        // Workflow-related endpoints for authorized users
        {
            "pattern"   : "workflow/taskinstance/*",
            "roles"     : "openidm-authorized",
            "methods"   : "action",
            "actions"   : "complete",
            "customAuthz" : "isMyTask()"
        },
        {
            "pattern"   : "workflow/taskinstance/*",
            "roles"     : "openidm-authorized",
            "methods"   : "read,update",
            "actions"   : "*",
            "customAuthz" : "canUpdateTask()"
        },
        {
            "pattern"   : "workflow/processinstance/",
            "roles"     : "openidm-authorized",
            "methods"   : "action",
            "actions"   : "createProcessInstance",
            "customAuthz": "isAllowedToStartProcess()"
        },
        {
            "pattern"   : "workflow/processdefinition/*",
            "roles"     : "openidm-authorized",
            "methods"   : "*",
            "actions"   : "read",
            "customAuthz": "isOneOfMyWorkflows()"
        },

        // Clients authenticated via SSL mutual authentication
        {
            "pattern"   : "*",
            "roles"     : "openidm-cert",
            "methods"   : "",  // default to no methods allowed
            "actions"   : ""  // default to no actions allowed
        }
    ] 
};

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

//////// Do not alter functions below here as part of your authz configuration



function passesAccessConfig(id, roles, method, action) {
    for (var i = 0; i < accessConfig.configs.length; i++) {
        var config = accessConfig.configs[i];
        var pattern = config.pattern;
        // Check resource ID
        if (matchesResourceIdPattern(id, pattern)) {
            // Check roles
            if (containsItems(roles, config.roles.split(','))) {
                // Check method
                if (method == 'undefined' || containsItem(method, config.methods)) {
                    // Check action
                    if (action == 'undefined' || action == "" || containsItem(action, config.actions)) {
                        if (typeof(config.customAuthz) != 'undefined') {
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
        if (contains(configItems, items[i])) {
            return true;
        }
    }
    return false
}

function containsItem(item, configItems) {
    if (configItems == "*") {
        return true;
    }
    return contains(configItems.split(','), item);
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

if (!allow()) {
//    java.lang.System.out.println(request);
    throw { 
        "openidmCode" : 403, 
        "message" : "Access denied"
    } 
}
