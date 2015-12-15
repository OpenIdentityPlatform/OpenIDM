
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
        userDetail,
        resource = properties.queryOnResource,
        propertyMapping = properties.propertyMapping,
        userIdPropertyName = propertyMapping.authenticationId,
        managedUserId,
        managedUser;


    managedUser = openidm.query("managed/user", { '_queryFilter' : '/userName eq "' + security.authenticationId  + '"' }, ["*","authzRoles"]);

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

    return security;

}());
