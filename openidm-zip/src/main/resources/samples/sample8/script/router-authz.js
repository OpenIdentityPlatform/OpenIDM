/*
 * This script is called from the router "onRequest" trigger, to enforce a central
 * set of authorization rules.
 * 
 * This default implemention simply restricts requests via HTTP to users that are assigned
 * an "openidm-admin" role, and optionally to those that authenticate with TLS mutual
 * authentication (assigned an "openidm-cert" role).
 */

// If true, then allows HTTP requests from "openidm-cert" role.
var allowCert = false;

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

function allow() {
    var roles;
    
    if (typeof(request.parent) === 'undefined' || request.parent.type !== 'http') {
        return true;
    }
    roles = request.parent.security['openidm-roles'];
    if (contains(roles, 'openidm-admin')) {
        return true;
    } else if (allowCert && contains(roles, 'openidm-cert')) {
        return true;
    } else {
        return false;
    }
}

if (!allow()) {
    throw "Access denied";
}
