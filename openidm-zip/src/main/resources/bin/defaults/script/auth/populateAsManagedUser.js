
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

    // This is needed to switch the context of an authenticated user from their original security context
    // to a context that is based on the related managed/user account. This is helpful for UI interaction.
    if (typeof properties.managedUserLink === "string" && properties.managedUserLink.length) {

        userDetail = openidm.query(resource, { '_queryFilter' : userIdPropertyName + ' eq "' + security.authenticationId  + '"' });

        if (!userDetail.result || userDetail.result.length === 0) {
            throw {
                "code" : 401,
                "message" : "Access denied, no user detail could be retrieved"
            };
        }

        if (userDetail.result.length > 1) {
            throw {
                "code" : 401,
                "message" : "Access denied, user detail retrieved ambiguous"
            };
        }

        managedUserId = openidm.query("repo/link", {
            "_queryId": "links-for-firstId",
            "linkType": properties.managedUserLink,
            "firstId" : userDetail.result[0]._id
        });

        if (managedUserId.result.length !== 1) {
            throw {
                "code" : 401,
                "message" : "Access denied, unable to find linked managed/user entry"
            };
        }

        managedUser = openidm.read("managed/user/" + managedUserId.result[0].secondId);

        if (managedUser === null) {
            throw {
                "code" : 401,
                "message" : "Access denied, linked managed/user entry is MISSING"
            };        
        }

        if (managedUser.accountStatus === "inactive") {
            throw {
                "code" : 401,
                "message" : "Access denied, user inactive"
            };        
        }

        security.authorization = {
            "id": managedUser._id,
            "component": "managed/user",
            "roles": managedUser.roles ?
                         _.uniq(security.authorization.roles.concat(managedUser.roles)) : 
                         security.authorization.roles
        };

    }

    return security;

}());
