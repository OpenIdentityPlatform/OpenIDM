
/*global security, properties, openidm */


/**
 * This context population script is called when the passthrough auth module was used
 * to successfully authenticate a user
 *
 * global properties - auth module-specific properties from authentication.json for the
 *                     passthrough user auth module
 *
 *      {
 *          "authnPopulateContextScript" : "auth/passthroughAuthnPopulateContext.js",
 *          "passThroughAuth" : "system/AD/account",
 *          "propertyMapping" : {
 *              "userRoles" : "roles"
 *          },
 *          "defaultUserRoles" : [ ]
 *      }
 *
 * global security - map of security context details as have been determined thus far
 *
 *      {
 *          "authorizationId": {
 *              "id": "jsmith",
 *              "component": "passthrough",
 *              "roles": [ "openidm-authorized" ]
 *          },
 *          "authenticationId": "jsmith",
 *      }
 */
 
logger.debug("Augment context for: {}", security.authenticationId);

var userDetail,
    i,
    role,
    resource = properties.queryOnResource,
    propertyMapping = properties.propertyMapping,
    userIdPropertyName = propertyMapping.authenticationId,
    groupMembershipProperty = propertyMapping.groupMembership,
    defaultUserRoles = properties.defaultUserRoles,
    groupRoleMapping = properties.groupRoleMapping,
    managedUserId,
    managedUser;

// This is needed to switch the context of a pass-through authenticated user from their original security context
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
            "message" : "Access denied, unabled to find linked managed/user entry"
        };        
    }

    managedUser = openidm.read("managed/user/" + managedUserId.result[0].secondId);

    if (managedUser.accountStatus === "inactive") {
        throw {
            "code" : 401,
            "message" : "Access denied, user inactive"
        };        
    }

    security.authorizationId = {
        "id": managedUser._id,
        "component": "managed/user",
        "roles": managedUser.roles ? managedUser.roles : defaultUserRoles
    };

}
