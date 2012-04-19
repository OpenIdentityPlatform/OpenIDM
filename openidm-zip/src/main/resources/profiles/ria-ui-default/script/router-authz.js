/*! @license 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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

// If true, then allows HTTP requests from "openidm-cert" role.
const allowCert = false;

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

function isPublicMethodInvocation() {
	java.lang.System.out.println("request.parent.path=" + request.parent.path);
	java.lang.System.out.println("request.parent.method=" + request.parent.method);
	
	//TODO delete for userName, implement forgotten password using process
	if(request.parent.path.match('^/openidm/managed/user') == '/openidm/managed/user') {
		java.lang.System.out.println("Resource==user");
		if(request.parent.method != 'PUT') {
			java.lang.System.out.println("This is not create request. Selected allowed only. Checking queries.");
			var publicQueries = ['check-userName-availability','for-security-answer','for-credentials', 'get-security-question', 'for-userName'];
			if(request.params) {
				var queryName = request.params['_query-id'];
			}

			if(queryName &&  (publicQueries.indexOf(queryName) > -1)) {
				java.lang.System.out.println("Query " + queryName + " found in the list");
				return true;
			} else {
				java.lang.System.out.println("Query " + queryName + " hasn't been found in a query");
				return false;
			}
		} else {
			java.lang.System.out.println("PUT request detected");
			return true;
		}
	} else {
		java.lang.System.out.println("Anonymous access not allowed for resources other than user");
		return false;
	}
}

function allow() {
    if (typeof(request.parent) === 'undefined' || request.parent.type != 'http') {
        return true;
    }
    var roles = request.parent.security['openidm-roles'];
    if (contains(roles, 'openidm-admin')) {
        return true;
    } else if (allowCert && contains(roles, 'openidm-cert')) {
        return true;
    } else {
		return isPublicMethodInvocation();
    }
}

if (!allow()) {
    throw "Access denied";
}
