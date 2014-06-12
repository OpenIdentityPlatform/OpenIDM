
/*global security */

logger.debug("Augment context for: {}", security.authenticationId);

var userDetail,
    params,
    rolesArr,
    resource = "managed/user";

if (security && security.authenticationId) { 

    params = {"_queryId" : "credential-query", "username" : security.authenticationId};

    userDetail = openidm.query(resource, params);
    if (!userDetail || !userDetail.result || userDetail.result.length === 0) {
        // If not found in managed, look in internal table 
        logger.debug("User detail for {} not found in {}, query next", security.authenticationId, resource);
        resource = "repo/internal/user";

        params = {"_queryId" : "credential-internaluser-query", "username" : security.authenticationId};
        userDetail = openidm.query(resource, params);
    }
}

if (userDetail && userDetail.result && userDetail.result.length === 1) {
    // Only augment authorizationId if missing
    if (!security.authorizationId) {
        security.authorizationId = {"component" : resource, "id" : userDetail.result[0]._id };
    }
    // Only augment roles if missing
    if (!security.authorizationId.roles) {
        security.authorizationId.roles = userDetail.result[0].roles;
    }
    logger.debug("Augmented context for {} with authorizationId.id : {}, roles : {}", security.authenticationId, security.authorizationId, security.authorizationId.roles);
    
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

