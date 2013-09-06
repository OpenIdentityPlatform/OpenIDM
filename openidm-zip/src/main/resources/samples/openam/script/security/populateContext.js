
/*global security */

logger.debug("Augment context for: {}", security.username);

var userDetail,
    params,
    rolesArr,
    resource = "managed/user";

if (security && security.username) { 

    params = {"_queryId" : "credential-query", "username" : security.username};

    userDetail = openidm.query(resource, params);
    if (!userDetail || !userDetail.result || userDetail.result.length === 0) {
        // If not found in managed, look in internal table 
        logger.debug("User detail for {} not found in {}, query next", security.username, resource);
        resource = "internal/user";

        params = {"_queryId" : "credential-internaluser-query", "username" : security.username};
        userDetail = openidm.query("repo/" + resource, params);
    }
}

if (userDetail && userDetail.result && userDetail.result.length === 1) {
    // Only augment userid if missing
    if (!security.userid) {
        security.userid = {"component" : resource, "id" : userDetail.result[0]._id };
    }
    // Only augment roles if missing
    if (!security["openidm-roles"]) {
        rolesArr = userDetail.result[0].roles.split(',');
        security["openidm-roles"] = rolesArr;
    }
    logger.debug("Augmented context for {} with userid : {}, roles : {}", security.username, security.userid, security["openidm-roles"]);
} else {
    if (userDetail && userDetail.result && userDetail.result.length > 1) {
        throw { 
            "code" : 403,
            "message" : "Access denied, user detail retrieved ambiguous"
        };
    } else {
        throw {
            "code" : 403,
            "message" : "Access denied, no user detail could be retrieved"
        };
    }
}

