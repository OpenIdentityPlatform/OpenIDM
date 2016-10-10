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
 * Copyright 2016 ForgeRock AS.
 */

/*global security, properties, openidm */

/**
 * This security context population script is called when the auth module authenticates a
 * user from a security context which is related to managed/idpData, and we wish to aggregate
 * the current security context with the one for the linked managed/user record (if found).
 *
 * global properties - auth module-specific properties from authentication.json for the
 *                     a potential OAUTH or OPENID_CONNECT auth module
 *      {
 *          "name" : "",
 *          "properties" : {
 *              "augmentSecurityContext": {
 *                  "type" : "text/javascript",
 *                  "file" : "auth/populateAsManagedUserFromRelationship.js"
 *              },
 *              "queryOnResource" : "managed/google",
 *              "propertyMapping" : {
 *                  "userRoles" : "authzRoles",
 *                  "authenticationId" : "_id"
 *              }
 *          },
 *          "defaultUserRoles" : [
 *              "openidm-authorized"
 *          ],
 *          "resolvers" : [
 *          ...
 *          ],
 *          "authTokenHeader" : "authToken",
 *          "authResolverHeader" : "provider"
 *          ...
 *      }
 *
 * global security - map of security context details as have been determined thus far
 *
 *      {
 *          "authorization": {
 *              "id": "jsmith",
 *              "component": "managed/google",
 *              "roles": [ "openidm-authorized" ]
 *          },
 *          "authenticationId": "1234567",
 *      }
 */

(function () {
    logger.debug("Augment context for: {}", security.authenticationId);

    var _ = require("lib/lodash"),
        managedUserRef = openidm.read(security.authorization.component + "/" + security.authorization.id, null, ["*","user"]).user;

    if (!managedUserRef) {
        throw {
            "code" : 401,
            "message" : "Access denied"
        };
    }

    var managedUser = openidm.read(managedUserRef._ref, null, ["*", "authzRoles"]);

    if (managedUser.accountStatus !== "active") {
        throw {
            "code" : 401,
            "message" : "Access denied, user inactive"
        };
    }

    security.authorization = {
        "id": managedUser._id,
        "component": "managed/user",
        "moduleId" : security.authorization.moduleId,
        "roles": managedUser.authzRoles ?
            _.uniq(
                security.authorization.roles.concat(
                    _.map(managedUser.authzRoles, function (r) {
                        // appending empty string gets the value from java into a format more familiar to JS
                        return org.forgerock.json.resource.ResourcePath.valueOf(r._ref).leaf() + "";
                    })
                )
            ) :
            security.authorization.roles
    };

    return security;

}());
