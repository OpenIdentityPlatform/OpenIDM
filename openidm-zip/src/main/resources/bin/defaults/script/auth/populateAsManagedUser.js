
/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Portions copyright 2026 3A Systems, LLC
 */

/*global security, properties, openidm */


/**
 * This security context population script is called when the auth module authenticates a
 * user from a security context which is related to managed/user, and we wish to aggregate
 * the current security context with the one for the linked managed/user record (if found).
 *
 * global properties - auth module-specific properties from authentication.json for the
 *                     passthrough user auth module
 *
 *      {
 *          "authnPopulateContextScript" : "auth/populateAsManagedUser.js",
 *          "queryOnResource" : "system/AD/account",
 *          "propertyMapping" : {
 *              "groupMembership" : "memberOf"
 *              "authenticationId" : "sAMAccountName"
 *          },
 *          "managedUserLink" : "systemAdAccounts_managedUser",
 *          "defaultUserRoles" : [
 *              "openidm-authorized"
 *          ]
 *      }
 *
 * global security - map of security context details as have been determined thus far
 *
 *      {
 *          "authorization": {
 *              "id": "jsmith",
 *              "component": "passthrough",
 *              "roles": [ "openidm-authorized" ]
 *          },
 *          "authenticationId": "jsmith",
 *      }
 */

(function () {
    logger.debug("Augment context for: {}", security.authenticationId);

    var _ = require("lib/lodash"),
        queryFilter = require("auth/queryFilter"),
        userDetail,
        resource = properties.queryOnResource,
        propertyMapping = properties.propertyMapping,
        userIdPropertyName = propertyMapping.authenticationId,
        managedUserId,
        managedUser;


    // Escape the untrusted authenticationId so it cannot break out of the query filter string
    // literal and inject additional predicates (e.g. ' or /userName eq "victim').
    managedUser = openidm.query("managed/user", { '_queryFilter' : '/userName eq "' + queryFilter.escapeStringValue(security.authenticationId)  + '"' }, ["*","authzRoles"]);

    if (managedUser.result.length === 0) {
        throw {
            "code" : 401,
            "message" : "Access denied, managed/user entry is not found"
        };
    }

    if (managedUser.result[0].accountStatus !== "active") {
        throw {
            "code" : 401,
            "message" : "Access denied, user inactive"
        };
    }

    security.authorization = {
        "id": managedUser.result[0]._id,
        "moduleId" : security.authorization.moduleId,
        "component": "managed/user",
        "roles": managedUser.result[0].authzRoles ?
                     _.uniq(
                         security.authorization.roles.concat(
                             _.map(managedUser.result[0].authzRoles, function (r) {
                                 // appending empty string gets the value from java into a format more familiar to JS
                                 return org.forgerock.json.resource.ResourcePath.valueOf(r._ref).leaf() + "";
                             })
                        )
                    ) :
                     security.authorization.roles
    };

    return require('auth/customAuthz').setProtectedAttributes(security);

}());
