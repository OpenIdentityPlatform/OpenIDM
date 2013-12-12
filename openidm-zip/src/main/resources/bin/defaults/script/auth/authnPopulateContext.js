/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 */
/*global security */

logger.debug("Augment context for: {}", security.username);

var userDetail,
    params,
    role,
    resource = request.attributes.get("org.forgerock.security.context").get("passThroughAuth"),
    propertyMapping = request.attributes.get("org.forgerock.security.context").get("propertyMapping"),
    userIdProperty = propertyMapping ? propertyMapping.get("userId") : null,
    groupMembershipProperty = propertyMapping ? propertyMapping.get("groupMembership") : null,
    groupRoleMapping = {
        "openidm-admin": [ ]
    };

// temporary until we have some better options for implementation
groupRoleMapping["openidm-admin"] = openidm.read("config/authentication").adminGroups;

function indexOf (array,value) {
    var i;
    for (i=0;i<array.length;i++) {
        if (array[i] === value) {
            return i;
        }
    }
    return -1;
}

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

if (resource && userIdProperty && security && typeof security.username === "string" && security.username.length > 0 && security.username !== "anonymous" && 
    indexOf(security["openidm-roles"], "openidm-groupRolesProcessed") === -1) {

    userDetail = openidm.query(resource, {
        'query' : {
            'Equals' : {
                'field' : userIdProperty,
                'values' : [
                    security.username
                ]
            }
        }
    });

    if (userDetail && userDetail.result && userDetail.result.length === 1) {
        // Only augment userid if missing
        if (!security.userid || !security.userid.id) {
            security.userid = {"component" : resource, "id" : userDetail.result[0]._id };
        }

        if (typeof security["openidm-roles"] === "undefined") {
            security["openidm-roles"] = [ ];
        }

        if (typeof userDetail.result[0][groupMembershipProperty] !== "undefined") {
            for (role in groupRoleMapping) {
                if (isMemberOfRoleGroups(groupRoleMapping[role], userDetail.result[0][groupMembershipProperty])) {
                    // push the current role onto openidm-roles array
                    security["openidm-roles"][security["openidm-roles"].length] = role;
                }
            }
        }

        logger.debug("Augmented context for {} with userid : {}, roles : {}", security.username, security.userid, security["openidm-roles"]);

    } else {
        if (userDetail && userDetail.result && userDetail.result.length > 1) {
            throw {
                "openidmCode" : 401,
                "message" : "Access denied, user detail retrieved ambiguous"
            };
        } else {
            throw {
                "openidmCode" : 401,
                "message" : "Access denied, no user detail could be retrieved"
            };
        }
    }

    security["openidm-roles"][security["openidm-roles"].length] = "openidm-groupRolesProcessed";
}
