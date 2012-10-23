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
        {       "pattern" : "*",
                "roles" : "openidm-admin",
                "methods": "*", // default to all methods allowed
                "actions" : "*", // default to all actions allowed
                "customAuthz" : "disallowQueryExpression()" // default to only allowing parameterized queries
        },
        // Clients authenticated via SSL mutual authentication
        {       "pattern" : "*",
                "roles" : "openidm-cert",
                "methods": "",  // default to no methods allowed
                "actions" : ""  // default to no actions allowed
        },
        // Additional checks for authenticated users
        {       "pattern" : "*",
                "roles" : "openidm-authorized", // openidm-authorized is anonymous
                "methods": "*",
                "actions" : "*",
                "customAuthz" : "ownDataOnly()" // Custom auth function
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
            "roles" : "openidm-reg",
            "methods": "read,query",
            //"customAuthz" : "checkIfIsPublicQuery()",
            "actions" : "*"
        },
        {  "pattern" : "managed/user/*",
            "roles" : "openidm-reg",
            "methods": "create",
            "actions" : "*"
        },
        {  "pattern" : "config/ui/*",
            "roles" : "openidm-reg",
            "methods": "read",
            "actions" : "*"
        }

        ] };


function ownDataOnly() {
    return true; // temporarily bypass authz until we have a method for comparing requested userId against secured userId value. 
    var roles = request.parent.security['openidm-roles'];

    if (
                (
                requestIsAQueryOfName('for-credentials') ||
                requestIsAQueryOfName('for-internalcredentials')
                )
                && userIsAuthorized(roles)
        )
        {
            return true;
        }

    // Additional Checks (if failed access configuration check)
    if (
        (
                requestIsAQueryOfName('for-credentials') ||
                requestIsAQueryOfName('for-internalcredentials') ||
                requestIsAQueryOfName('notifications-for-user') ||
                requestIsAQueryOfName('user_application_lnk-for-user') ||
                requestIsAQueryOfName('for-userName')

        ) && userIsAuthorized(roles)){

        //authenticated user can only manage his data and cannot change a role attribute.
                java.lang.System.out.println(DumpObjectIndented(request));
        var requestedUserNameDataIdentificator = request.params['user'];

        if (authorizedUsernameEquals(requestedUserNameDataIdentificator)) {
            logger.debug("User manipulation with own data");

            if (requestIsAnActionOfName('patch')) {
                logger.debug("Request is a patch. Checking if trying to change own role");

                if (requestValueContainsReplaceValueWithKeyOfName('/role')) {
                    logger.debug("Trying to change own role is forbidden for standard user");
                    return false;
                }
            }
        } else {
            logger.debug("Manipulation with data of user not equal to logged-in is forbidden");
            throw "Access denied (Manipulation with data of user not equal to logged-in is forbidden)";
        }
        return true;
    } else {
        return isPublicMethodInvocation();
    }
}

function disallowQueryExpression() {
    if (request.params && typeof request.params['_query-expression'] != "undefined") {
        return false;
    }
    return true;
}

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

/**
 * Public methods are accessible by anonymous user. They are used
 * during registration and forgotten password process.
 */
function isPublicMethodInvocation() {
    logger.debug("request.parent.path = {}", request.parent.path);
    logger.debug("request.parent.method = {}", request.parent.method);
    
    if (request.parent.path.match('^/openidm/managed/user') == '/openidm/managed/user') {
        logger.debug("Resource == user");
        
        if (request.parent.method == 'GET') {            
            logger.debug("This is GET request. Selected allowed only. Checking queries.");
           
            var publicQueries = ['check-userName-availability','for-security-answer','for-credentials', 'get-security-question', 'set-newPassword-for-userName-and-security-answer'];
            
            if (request.params) {
                var queryName = request.params['_query-id'];
            }
            
            if (queryName && (publicQueries.indexOf(queryName) > -1)) {
                logger.debug("Query {} found in the list", queryName);
                return true;
            } else {
                logger.debug("Query {} hasn't been found in a query", queryName);
                return false;
            }            
        } else if (request.parent.method == 'PUT') {
            logger.debug("PUT request detected");
            return true;
        } else {
            logger.debug("Anonymous POST and DELETE methods are not allowed");
        }
    } else {
        logger.debug("Anonymous access not allowed for resources other than user");
        return false;
    }
}

function userIsAuthorized(roles) {
    return contains(roles, 'openidm-authorized');
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
    if (typeof(request.parent) === 'undefined' || request.parent.type != 'http') {
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
    throw "Access denied";
}
