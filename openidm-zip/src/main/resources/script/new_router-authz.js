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

//NOT USED IN THIS MOMENT

var accessConfig = { "configs" : [
                                  
    // openidm-admin and admin can do anything                                  
	{	"pattern" : "*", 
		"roles" : "openidm-admin, admin", 
		"methods": "*", // default to all methods allowed
		"actions" : "*" // default to all actions allowed
	},
	// Clients authenticated via SSL mutual authentication
	{	"pattern" : "*", 
		"roles" : "openidm-cert", 
		"methods": "",	// default to no methods allowed
		"actions" : ""  // default to no actions allowed
	},
	
	// Additional checks for anonymous and authenticated users
	
	//         ########        managed/user        ##########
	
	// Authorized user can:
	// * create user using POST with action=create
    // * get only own data
	// * modify only data in own account, but not roles (using patch)
    // 
	{	"pattern" : "managed/user", 
		"roles" : "openidm-authorized, openidm-reg", 
		"methods": "POST",
		"actions" : "create",
	},
	// PROBLEM: it should be in onResponse as we need to have response data to check it
	// or we need to get user by id to check if allow user to get this data 
	{  "pattern" : "managed/user/*", 
        "roles" : "openidm-authorized, openidm-reg", 
        "methods": "GET",
        "actions" : "*",
        "customAuthz" : "checkIfOwn()"
    },
    {  "pattern" : "managed/user", 
        "roles" : "openidm-authorized, openidm-reg", 
        "methods": "POST",
        "actions" : "patch",
        "customAuthz" : "checkIfModifyingOwnDataAndNotRoles()"
    },
	
	// Anonymous user can:
    // * create user using POST with action=create
	// * invoke some queries which are public:
	//     check-userName-availability,
	//     for-security-answer,
	//     for-credentials, 
	//     get-security-question, 
	//     set-newPassword-for-userName-and-security-answer
	{  "pattern" : "managed/user/*", 
        "roles" : "anonymous", 
        "methods": "GET",
        "actions" : "*",
        "customAuthz" : "checkIfIsPublicQuery()"
    },
    {  "pattern" : "managed/user", 
        "roles" : "anonymous", 
        "methods": "POST",
        "actions" : "create",
    },
    
    
    
    //     ########        managed/user_application_lnk        ##########
	
    
    // Anonymous user can:
    // NOTHING
    //
    
    // Authorized user can:
    // * create link for own account
    // * get only own links
    // * delete own link
    // * invoke some queries with own uid:
    //      user_application_lnk-for-user
    // 
    {   "pattern" : "managed/user_application_lnk", 
        "roles" : "openidm-authorized, openidm-reg", 
        "methods": "POST",
        "actions" : "create",
        "customAuthz" : "checkIfAddingUserApplicationLinkToOwnAccount()"
    },
    // PROBLEM: it should be in onResponse as we need to have response data to check it
    // or we need to get user application link by id to check if allow user to get this data 
    {  "pattern" : "managed/user_application_lnk/*", 
        "roles" : "openidm-authorized, openidm-reg", 
        "methods": "GET, DELETE",
        "actions" : "*",
        "customAuthz" : "checkIfUserApplicationLinkIsOwn()"
    },
    {  "pattern" : "managed/user/*", 
        "roles" : "openidm-authorized, openidm-reg", 
        "methods": "GET",
        "actions" : "*",
        "customAuthz" : "checkIfUserCanCallUserApplicationLinkQuery()"
    },
    
    
    //    ########        repo/ui/notification        ##########
    
    
    // Anonymous user can:
    // NOTHING
    //
    
    // Authorized user can:
    // * get own
    // * delete own notification
    // * invoke some queries with own uid:
    //     notifications-for-user
    // 
    // PROBLEM: it should be in onResponse as we need to have response data to check it
    // or we need to get notification by id to check if allow user to get this data 
    {  "pattern" : "repo/ui/notification/*", 
        "roles" : "openidm-authorized, openidm-reg", 
        "methods": "GET, DELETE",
        "actions" : "*",
        "customAuthz" : "checkIfApplicationLinkIsOwn()"
    },
    {  "pattern" : "repo/ui/notification/*", 
        "roles" : "openidm-authorized, openidm-reg", 
        "methods": "GET",
        "actions" : "*",
        "customAuthz" : "checkIfUserCanCallNotificationQuery()"
    },
    
    
    //    ########        config/ui/applications        ##########
    
    
    // Anonymous user can:
    // NOTHING
    //
    
    // Authorized user can:
    // * get all 
    // 
    {  "pattern" : "config/ui/applications", 
        "roles" : "openidm-authorized, openidm-reg", 
        "methods": "GET",
        "actions" : "*",
    },
    
    //    ########        config/ui/countries        ##########
    
    
    // Anonymous user can:
    // NOTHING
    //
    
    // Authorized user can:
    // * get all 
    // 
    {  "pattern" : "config/ui/countries", 
        "roles" : "openidm-authorized, openidm-reg", 
        "methods": "GET",
        "actions" : "*",
    },
    
    //    ########        config/ui/secquestions        ##########
    
    
    // Anonymous user can:
    // * get all 
    // 
    {  "pattern" : "config/ui/secquestions", 
        "roles" : "openidm-authorized, openidm-reg", 
        "methods": "GET",
        "actions" : "*",
    },
    
    // Authorized user can:
    // * get all 
    // 
    {  "pattern" : "config/ui/secquestions", 
        "roles" : "anonymous", 
        "methods": "GET",
        "actions" : "*",
    }
    
    
	] };


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
							return eval(config.customAuthz);
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

function requestIsAQueryOfName(queryName) {
    return request.params && request.params['_query-id'] && request.params['_query-id'] == queryName;
}

function authorizedUsernameEquals(userName) {
    return request.parent.security['user'] == userName;
}

function requestIsAnActionOfName(actionName) {
    return request.value && request.params && request.params['_action'] && request.params['_action'] == actionName;
}

function requestValueContainsReplaceValueWithKeyOfName(valueKeyName) {
    var key = "replace";
    
    for (var i = 0; i < request.value.length; i++) {
        if (request.value[i][key] == valueKeyName) {
            return true;
        }
    }
}

function allow() {
    //Allow all internal calls
    if (typeof(request.parent) === 'undefined' || request.parent.type != 'http') {
        logger.debug("Internal request allowed");
        return true;
    } else {
        
        var roles = request.parent.security['openidm-roles'];
        var action = "";
        if (request.params && request.params['_action']) {
            action = request.params['_action'];
        }
        
        if (request.parent.type == 'http') {
            logger.debug("Access Check for HTTP request for resource id: " + request.id);
            if (passesAccessConfig(request.id, roles, request.method, action)) {
                logger.debug("Request allowed");
                return true;
            }
        }
        
    }
}

if (!allow()) {
    throw "Access denied";
}
