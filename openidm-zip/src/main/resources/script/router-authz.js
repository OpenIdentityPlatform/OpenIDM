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
var accessConfig = { "configs" : [

      // Anyone can read from these endpoints
      {  "pattern" : "info/*",
          "roles" : "openidm-reg,openidm-authorized",
          "methods": "read",
          "actions" : "*"
      },
      {  "pattern" : "config/ui/configuration",
          "roles" : "openidm-reg,openidm-authorized",
          "methods": "read",
          "actions" : "*"
      },
      
      // These options should only be available anonymously if selfReg is enabled
      {  "pattern" : "config/ui/*",
          "roles" : "openidm-reg",
          "methods": "read",
          "customAuthz" : "checkIfUIIsEnabled('selfRegistration')",
          "actions" : "*"
      },
      {  "pattern" : "managed/user/*",
          "roles" : "openidm-reg",
          "methods": "create",
          "customAuthz" : "checkIfUIIsEnabled('selfRegistration')",
          "actions" : "*"
      },

      // Anonymous user can call the siteIdentification endpoint if it is enabled:
      {  "pattern" : "endpoint/siteIdentification",
          "roles" : "openidm-reg",
          "methods": "*",
          "customAuthz" : "checkIfUIIsEnabled('siteIdentification')",
          "actions" : "*"
      },

      // Anonymous user can call the securityQA endpoint if it enabled:
      {  "pattern" : "endpoint/securityQA",
          "roles" : "openidm-reg",
          "methods": "*",
          "customAuthz" : "checkIfUIIsEnabled('securityQuestions')",
          "actions" : "*"
      },
      // This is needed by both self reg and security questions
      {  "pattern" : "policy/managed/user/*",
          "roles" : "openidm-reg",
          "methods": "read,action",
          "customAuthz" : "checkIfUIIsEnabled('selfRegistration') || checkIfUIIsEnabled('securityQuestions')",
          "actions" : "*"
      },

      // admin can request anything
        {  "pattern" : "*",
            "roles" : "openidm-admin",
            "methods": "*", // default to all methods allowed
            "actions" : "*", // default to all actions allowed
            "customAuthz" : "disallowQueryExpression()" // default to only allowing parameterized queries
        },
        
        // Additional checks for authenticated users
        {  "pattern" : "policy/*",
            "roles" : "openidm-authorized",
            "methods": "read,action",
            "actions" : "*"
        },
        {  "pattern" : "config/ui/*",
            "roles" : "openidm-authorized",
            "methods": "read",
            "actions" : "*"
        },
        {  "pattern" : "authentication",
            "roles" : "openidm-authorized",
            "methods": "action",
            "actions" : "reauthenticate"
        },
        {   "pattern" : "*",
            "roles" : "openidm-authorized", // openidm-authorized is logged-in users
            "methods": "*",
            "actions" : "*",
            "customAuthz" : "ownDataOnly() || isQueryOneOf({'managed/user/': ['query-all']})" // query-all used by workflow
        },
        {
            "pattern" : "endpoint/getnotifications",
            "roles" : "openidm-authorized",
            "methods": "*",
            "actions" : "*"
        },
        {
            "pattern" : "endpoint/getnotifications/*",
            "roles" : "openidm-authorized",
            "methods": "*",
            "actions" : "*"
        },
        
        // workflow related endpoints.  We need to build these up with custom Authz functions
        {   
            "pattern" : "endpoint/getprocessesforuser",
            "roles" : "openidm-authorized",
            "methods": "*",
            "actions" : "*"
        },
        {   
            "pattern" : "workflow/processdefinition/*",
            "roles" : "openidm-authorized",
            "methods": "*",
            "actions" : "*"
        },
        {   
            "pattern" : "workflow/processinstance/*",
            "roles" : "openidm-authorized",
            "methods": "*",
            "actions" : "*"
        },

        // Clients authenticated via SSL mutual authentication
        {       "pattern" : "*",
                "roles" : "openidm-cert",
                "methods": "",  // default to no methods allowed
                "actions" : ""  // default to no actions allowed
        },

        ] };

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
    if (userId && userId.length == 2)
    {
        userId = userId[1];
    }
    else if (request.params && request.params.userId)
    {
        userId = request.params.userId;
    }
    else if (request.value && request.value.userId)
    {
        userId = request.value.userId;
    }
    
    return userId === request.parent.security.userid.id;

}

function disallowQueryExpression() {
    if (request.params && typeof request.params['_queryExpression'] != "undefined") {
        return false;
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
    throw { 
        "openidmCode" : 403, 
        "message" : "Access denied"
    } 
}
