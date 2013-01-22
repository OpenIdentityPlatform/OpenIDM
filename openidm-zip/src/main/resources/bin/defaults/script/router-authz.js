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
/*global httpAccessConfig, allowedOrigins */


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
    var taskInstanceId = request.id.split("/")[2],
        taskInstance = openidm.read("workflow/taskinstance/" + taskInstanceId);
    
    return taskInstance.assignee === request.parent.security.username;
}

function isUserCandidateForTask(taskInstanceId) {
    
    var userCandidateTasksQueryParams = {
            "_queryId": "filtered-query",
            "taskCandidateUser": request.parent.security.username
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
    for (i = 0; i < request.parent.security['openidm-roles'].length; i++) {
        role = request.parent.security['openidm-roles'][i];
        if (i === 0) {
            roles = role;
        } else {
            roles = roles + "," + role;
        }
    }
    
    userGroupCandidateTasksQueryParams = {
        "_queryId": "filtered-query",
        "taskCandidateGroup": roles
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
    var taskInstanceId = request.id.split("/")[2];
    return isMyTask() || isUserCandidateForTask(taskInstanceId);
}

function isProcessOnUsersList(processDefinitionId) {
    var processesForUserQueryParams = {
            "_queryId": "query-processes-for-user",
            "userId": request.parent.security.userid.id
        },
        processesForUser = openidm.query("endpoint/getprocessesforuser", processesForUserQueryParams),
        isProcessOneOfUserProcesses = false,
        processForUser,
        i;
    
    for (i = 0; i < processesForUser.length; i++) {
        processForUser = processesForUser[i];
        if (processDefinitionId === processForUser._id) {
            isProcessOneOfUserProcesses = true;
        }
    }
    return isProcessOneOfUserProcesses;
}

function isAllowedToStartProcess() {
    var processDefinitionId = request.value._processDefinitionId;
    return isProcessOnUsersList(processDefinitionId);
}

function isOneOfMyWorkflows() {
    var processDefinitionId = request.id.split("/")[2];
    return isProcessOnUsersList(processDefinitionId);
}

function isQueryOneOf(allowedQueries) {
    if (
            allowedQueries[request.id] &&
            contains(allowedQueries[request.id], request.params._queryId)
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

function managedUserRestrictedToAllowedProperties(allowedPropertiesList) {
    var i = 0,requestedRoles = [],params = {},currentUser = {},
        getTopLevelProp = function (prop) {
            // removes a leading slash and only returns the first part of a string before a possible subsequent slash
            return prop.replace(/^\//, '').match(/^[^\/]+/)[0];
        };
    
    if (!request.id.match(/^managed\/user/)) {
        return true;
    }

    if (request.value) {
        params = request.value;
    }
    else { // this would be strange, but worth checking
        return true; // true because they don't appear to be setting anything
    }
    
    // we could accept a csv list or an array of properties for the allowedPropertiesList arg.
    if (typeof allowedPropertiesList === "string") {
        allowedPropertiesList = allowedPropertiesList.split(',');
    }
    
    if (request.method === "patch" || (request.method === "action" && request.params._action === "patch")) {
        // check each of the fields they are attempting to patch and make sure they are approved
        for (i in params) {
            if ((params[i].test && !containsIgnoreCase(allowedPropertiesList, getTopLevelProp(params[i].test))) ||
                (params[i].add && !containsIgnoreCase(allowedPropertiesList, getTopLevelProp(params[i].add))) || 
                (params[i].replace && !containsIgnoreCase(allowedPropertiesList, getTopLevelProp(params[i].replace)))) {
                return false;
            }
        }
    } else if (request.method === "update") {
        currentUser = openidm.read(request.id);
        if (!currentUser) { // this would be odd, but just in case
            return false;
        }
        for (i in params) {
            // if the new value does not match the current value, then they must be updating it
            // if the field they are attempting to update isn't allowed for them, then reject request.
            if (!deepCompare(currentUser[i],params[i]) && !containsIgnoreCase(allowedPropertiesList,i)) {
                return false;
            }
        }
    } else if (request.method === "create") {
        for (i in params) {
            // they should only be providing parameters that they are allowed to define
            if (!containsIgnoreCase(allowedPropertiesList,i)) {
                return false;
            }
        }
    }

    return true;
}

function disallowQueryExpression() {
    if (request.params && typeof request.params._queryExpression !== "undefined") {
        return false;
    }
    return true;
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
                    if (method === 'undefined' || containsItem(method, config.methods)) {
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


function passesOriginVerification() {
    var headers = request.parent.headers;

    if (typeof (headers["X-Requested-With"]) !== "undefined" || 
        typeof (headers["Authorization"]) !== "undefined" || 
        typeof (headers["X-OpenIDM-Username"]) !== "undefined") {
        
        // CORS requests will have the Origin header included; verify that the origin given is allowed.
        if (typeof (headers["Origin"]) !== "undefined" && typeof allowedOrigins !== "undefined" &&
                !contains(allowedOrigins, headers.Origin) ) {
            return false;
        } else {
            return true;
        }
    }
    return false;
}

function allow() {
    var roles,
        action;
    
    if (request.parent === null || request.parent === undefined || request.parent.type !== 'http') {
        return true;
    }
    
    roles = request.parent.security['openidm-roles'];
    action = "";
    if (request.params && request.params._action) {
        action = request.params._action;
    }
    
    // Check REST requests against the access configuration
    if (request.parent.type === 'http') {
        if (!passesOriginVerification()) {
            return false;
        }
        
        logger.debug("Access Check for HTTP request for resource id: " + request.id);
        if (passesAccessConfig(request.id, roles, request.method, action)) {
            logger.debug("Request allowed");
            return true;
        }
    }
}

// Load the access configuration script (httpAccessConfig obj)
load("script/access.js");

if (!allow()) {
//    java.lang.System.out.println(request);
    throw { 
        "openidmCode" : 403, 
        "message" : "Access denied"
    };
}
