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

/*jslint regexp:false sub:true */
/*global httpAccessConfig */


//reinventing the wheel a bit, here; eventually move to underscore's isEqual method
function deepCompare(obj1, obj2) {
    var i,t1 = typeof(obj1), t2 = typeof(obj2);
    if (t1 !== t2) {
        return false; // mismatching types indicate a failed match
    } else if ((t1 === "string" || t1 === "number" || t1 === "boolean")) {
        return obj1 === obj2; // simple comparisons work in this case
    } else { // they must both be objects, so traverse them
        for (i in obj1) {
            if (!deepCompare(obj1[i], obj2[i])) { // recurse through the object, checking each child property
                return false;
            }
        }
        for (i in obj2) { // checks for any properties in obj2 which were not in obj1, and so missed above
            if (typeof(obj1[i]) === "undefined") {
                return false;
            }
        }
        return true;
    }
}

function matchesResourceIdPattern(id, pattern) {
    if (pattern === "*") {
        // Accept all patterns
        return true;
    } else if (id === pattern) {
        // pattern matches exactly
        return true;
    } else if (pattern.indexOf("/*", pattern.length - 2) !== -1) {
        // Ends with "/*" or "/"
        // See if parent pattern matches
        var parentResource = pattern.substring(0, pattern.length - 1);
        if (id.length >= parentResource.length && id.substring(0, parentResource.length) === parentResource) {
            return true;
        }
    }
    return false;
}

function containsIgnoreCase(a, o) {
    var i,str1,str2;
    if (typeof(a) !== 'undefined' && a !== null) {
        for (i = 0; i <= a.length; i++) {
            str1 = o;
            str2 = a[i];
            if (typeof(o) !== 'undefined' && o !== null) {
                str1 = o.toLowerCase();
            }
            if (typeof(a[i]) !== 'undefined' && a[i] !== null) {
                str2 = a[i].toLowerCase();
            }
            if (str1 === str2) {
                return true;
            }
        }
    }
    return false;
}

function containsItems(items, configItems) {
    var i;
    if ((typeof configItems === "string" && configItems === '*') || 
        (typeof configItems === "object" && configItems.length === 1 && configItems[0] === '*')) {
        return true;
    }

    for (i = 0; i < items.length; i++) {
        if (containsIgnoreCase(configItems, items[i])) {
            return true;
        }
    }
    return false;
}

function containsItem(item, configItems) {
    if ((typeof configItems === "string" && configItems === '*') || 
            (typeof configItems === "object" && configItems.length === 1 && configItems[0] === '*')) {
        return true;
    }
    return containsIgnoreCase(configItems.split(','), item);
}

function contains(a, o) {
    var i;
    if (typeof(a) !== 'undefined' && a !== null) {
        for (i = 0; i <= a.length; i++) {
            if (a[i] === o) {
                return true;
            }
        }
    }
    return false;
}

function isMyTask() {
    var taskInstanceId = request.resourceName.split("/")[2],
        taskInstance = openidm.read("workflow/taskinstance/" + taskInstanceId);
    
    return taskInstance.assignee === context.security.authenticationId;
}
function join (arr, delim) {
    var returnStr = "",i=0;
    for (i=0; i<arr.length; i++) {
        returnStr = returnStr + arr[i] + delim;
    }
    return returnStr.replace(new RegExp(delim + "$"), '');
}

function isUserCandidateForTask(taskInstanceId) {
    
    var userCandidateTasksQueryParams = {
            "_queryId": "filtered-query",
            "taskCandidateUser": context.security.authenticationId
        },
        userCandidateTasks = openidm.query("workflow/taskinstance", userCandidateTasksQueryParams).result,
        userGroupCandidateTasksQueryParams,
        userGroupCandidateTasks,
        i,roles,role;
    
    for (i = 0; i < userCandidateTasks.length; i++) {
        if (taskInstanceId === userCandidateTasks[i]._id) {
            return true;
        }
    }
        
    roles = "";
    for (i = 0; i < context.security.authorizationId.roles.length; i++) {
        role = context.security.authorizationId.roles[i];
        if (i === 0) {
            roles = role;
        } else {
            roles = roles + "," + role;
        }
    }
    
    userGroupCandidateTasksQueryParams = {
        "_queryId": "filtered-query",
        "taskCandidateGroup": ((typeof roles === "string") ? roles : join(roles, ","))
    };    
    userGroupCandidateTasks = openidm.query("workflow/taskinstance", userGroupCandidateTasksQueryParams).result;
    for (i = 0; i < userGroupCandidateTasks.length; i++) {
        if (taskInstanceId === userGroupCandidateTasks[i]._id) {
            return true;
        }
    }
    
    return false;
}

function canUpdateTask() {
    var taskInstanceId = request.resourceName.split("/")[2];
    return isMyTask() || isUserCandidateForTask(taskInstanceId);
}

function isProcessOnUsersList(processFilter) {
    var processesForUserQueryParams = {
            "_queryId": "query-processes-for-user",
            "userId": context.security.authorizationId.id
        },
        processesForUser = openidm.query("endpoint/getprocessesforuser", processesForUserQueryParams),
        isProcessOneOfUserProcesses = false,
        processForUser,
        i;
    
    for (i = 0; i < processesForUser.result.length; i++) {
        processForUser = processesForUser.result[i];
        if (processFilter(processForUser)) {
            isProcessOneOfUserProcesses = true;
        }
    }

    return isProcessOneOfUserProcesses;
}

function isAllowedToStartProcess() {
    var processDefinitionId = request.content._processDefinitionId;
    var key = request.content._key;
    return isProcessOnUsersList(function (process) { 
        return (process._id === processDefinitionId) || (process.key === key); 
    });
}

function isOneOfMyWorkflows() {
    var processDefinitionId = request.resourceName.split("/")[2];
    return isProcessOnUsersList(function (process) {return (process._id === processDefinitionId); });
}

function isQueryOneOf(allowedQueries) {
    if (
            allowedQueries[request.resourceName] &&
            contains(allowedQueries[request.resourceName], request.queryId)
       )
    {
        return true;
    }
    
    return false;
}

function checkIfUIIsEnabled(param) {
    var ui_config = openidm.read("config/ui/configuration"),
        returnVal = false;
    return (ui_config && ui_config.configuration && ui_config.configuration[param]);
}

function ownDataOnly() {
    var userId = context.security.authorizationId.id,
        component = context.security.authorizationId.component;

    // in the case of a literal read on themselves
    return (request.resourceName === component + "/" + userId);

}

function managedUserRestrictedToAllowedProperties(allowedPropertiesList) {
    var i = 0,requestedRoles = [],params = {},currentUser = {}, operations,
        getTopLevelProp = function (prop) {
            // removes a leading slash and only returns the first part of a string before a possible subsequent slash
            return prop.replace(/^\//, '').match(/^[^\/]+/)[0];
        };
    
    if (!request.resourceName.match(/^managed\/user/)) {
        return true;
    }
    
    // we could accept a csv list or an array of properties for the allowedPropertiesList arg.
    if (typeof allowedPropertiesList === "string") {
        allowedPropertiesList = allowedPropertiesList.split(',');
    }
    
    if (request.method === "patch" || (request.method === "action" && request.action === "patch")) {
    	if (request.method === "action") {
    		operations = request.content;
    	} else if (!request.patchOperations) {
            return true;
        } else {
        	operations = request.patchOperations
        }
        // check each of the fields they are attempting to patch and make sure they are approved
        for (i in operations) {
            if ((operations[i].field && !containsIgnoreCase(allowedPropertiesList, getTopLevelProp(operations[i].field)))) {
                return false;
            }
        }
    } else if (request.method === "update") {
        if (!request.content) {
            return true;
        }
        currentUser = openidm.read(request.resourceName);
        if (!currentUser) { // this would be odd, but just in case
            return false;
        }
        for (i in request.content) {
            // if the new value does not match the current value, then they must be updating it
            // if the field they are attempting to update isn't allowed for them, then reject request.
            if (!deepCompare(currentUser[i], request.content[i]) && !containsIgnoreCase(allowedPropertiesList,i)) {
                return false;
            }
        }
    } else if (request.method === "create") {
        if (!request.content) {
            return true;
        }
        for (i in request.content) {
            // they should only be providing parameters that they are allowed to define
            if (!containsIgnoreCase(allowedPropertiesList,i)) {
                return false;
            }
        }
    }

    return true;
}

function disallowQueryExpression() {
    return  !request.queryExpression;
}

function disallowCommandAction() {
    return  request.method !== "action" || request.action !== "command";
}

//////// Do not alter functions below here as part of your authz configuration

function passesAccessConfig(id, roles, method, action) {
    var i,j,config,pattern,excluded,ex;
    
    for (i = 0; i < httpAccessConfig.configs.length; i++) {
        config = httpAccessConfig.configs[i];
        pattern = config.pattern;
        // Check resource ID
        if (matchesResourceIdPattern(id, pattern)) {
            
            // Check excludePatterns
            ex = false;
            if (typeof(config.excludePatterns) !== 'undefined' && config.excludePatterns !== null) {
                excluded = config.excludePatterns.split(',');
                for (j = 0; j < excluded.length; j++) {
                    if (matchesResourceIdPattern(id, excluded[j])) {
                        ex = true;
                        break;
                    }
                }
            }
            if (!ex) {
                // Check roles
                if (containsItems(roles, config.roles.split(','))) {
                    // Check method
                    if (typeof method === 'undefined' || containsItem(method, config.methods)) {
                        // Check action
                        if (action === 'undefined' || action === "" || containsItem(action, config.actions)) {
                            if (typeof(config.customAuthz) !== 'undefined' && config.customAuthz !== null) {
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
    }
    return false;
}

function isAJAXRequest() {
    var headers = context.http.headers;

    // one of these custom headers must be present for all HTTP-based requests, to prevent CSRF attacks

    // X-Requested-With is common from AJAX libraries such as jQuery
    if (typeof (headers["X-Requested-With"]) !== "undefined" || 
        typeof (headers["x-requested-with"]) !== "undefined" || 

        // Basic auth headers are acceptible for convenience from cURL commands; 
        // We don't return the request header to prompt the browser to provide basic auth headers, 
        // so it will only be present if someone explicitly provides them, as in a cURL request.
        typeof (headers["Authorization"]) !== "undefined" || 
        typeof (headers["authorization"]) !== "undefined" || 

        // The custom authn headers for OpenIDM
        typeof (headers["X-OpenIDM-Username"]) !== "undefined" || 
        typeof (headers["x-openidm-username"]) !== "undefined") {

        return true;
    }
    return false;
}

function allow() {
    var roles,
        action;
    
    if (!context.caller.external) {
        return true;
    }
    
    roles = context.security.authorizationId.roles;
    action = "";
    if (request.action) {
        action = request.action;
    }
    
    // Check REST requests against the access configuration
    if (context.caller.external) {
        // We only need to block non-AJAX requests when the action is not "read"
        if (request.method !== "read" && !isAJAXRequest()) {
            return false;
        }
        
        logger.debug("Access Check for HTTP request for resource id: {}, role: {}, method: {}, action: {}", request.resourceName, roles, request.method, action);

        if (passesAccessConfig(request.resourceName, roles, request.method, action)) {

            logger.debug("Request allowed");
            return true;
        }
    }
}

// Load the access configuration script (httpAccessConfig obj)
load(identityServer.getProjectLocation() + "/script/access.js");

if (!allow()) {
//    console.log(request);
    throw { 
        "code" : 403,
        "message" : "Access denied"
    };
}
