
/*global security, properties */


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
    resource = properties.passThroughAuth,
    propertyMapping = properties.propertyMapping,
    userIdProperty = propertyMapping.userId,
    groupMembershipProperty = propertyMapping.groupMembership,
    defaultUserRoles = properties.defaultUserRoles,
    groupRoleMapping = properties.groupRoleMapping,
    managedUserId,
    managedUser;

function isMemberOfRoleGroups(groupsForRole, assignedGroups) {
    var i,j;

    for (i=0;i<groupsForRole.length;i++) {
        for (j=0;j<assignedGroups.length;j++) {
            // ldap is case (and to some degree whitespace) insensitive, so we have to be too:
            if (assignedGroups[j].toLowerCase().replace(/\s*(^|$|,|=)\s*/g, "$1") === groupsForRole[i].toLowerCase().replace(/\s*(^|$|,|=)\s*/g, "$1")) {
                return true;
            }
        }
    }
    return false;
}

userDetail = openidm.query(resource, { '_queryFilter' : userIdProperty + ' eq "' + security.authenticationId  + '"' });

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

if (typeof properties.managedUserLink === "string" && properties.managedUserLink.length) {

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

} else {

    // Only augment authorizationId if missing
    if (!security.authorizationId) {
        security.authorizationId = {};
    }

    security.authorizationId.component = resource;
    security.authorizationId.roles = [];
    for (i = 0; i < defaultUserRoles.length; i++) {
        security.authorizationId.roles[i] = defaultUserRoles[i];
    }

    if (!security.authorizationId.id) {
        security.authorizationId.id = userDetail.result[0]._id;
    }

    if (typeof userDetail.result[0][groupMembershipProperty] !== "undefined") {
        for (role in groupRoleMapping) {
            if (isMemberOfRoleGroups(groupRoleMapping[role], userDetail.result[0][groupMembershipProperty])) {
                // push the current role onto roles array
                security.authorizationId.roles.push(role);
            }
        }
    }

    logger.debug("Augmented context for {} with userid : {}, roles : {}", security.authenticationId, security.authorizationId.id, security.authorizationId.roles);

}


