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
var base64 = Packages.org.forgerock.util.encode.Base64url,
    id_token = (httpRequest.getHeaders().getFirst('authToken').toString()+""),
    provider = (httpRequest.getHeaders().getFirst('provider').toString()+""),
    resolverConfig = properties.resolvers.filter(function (r) {return r.name === provider;})[0],
    referer = httpRequest.getHeaders().getFirst('Referer').toString(),
    parts = id_token.split('.'),
    claimsContent = parts[1],
    claims = JSON.parse(new java.lang.String(base64.decode(claimsContent))),
    session_token = claims.sessionTokenId || id_token,
    modifiedMap = {};

//console.log(JSON.stringify(claims, null, 4))
try {
    var response = openidm.action("external/rest", "call", {
        "url": sessionValidationBaseEndpoint + session_token + "?_action=validate",
        "headers" : {
            "Accept-API-Version" : "protocol=1.0,resource=1.0"
        },
        "method": "POST"
    });

    if (!response.valid) {
        throw {
            "code": 401,
            "message": "OpenAM session invalid"
        };
    }
} catch (e) {
    throw {
        "code": 401,
        "message": "OpenAM session invalid"
    };
}


if (security.authenticationId === "amadmin") {
    security.authorization = {
        "id" : "openidm-admin",
        "component" : "repo/internal/user",
        "roles" : ["openidm-admin", "openidm-authorized"],
        "moduleId" : security.authorization.moduleId
    };
} else if (security.authorization.component !== "managed/user") {
    var _ = require('lib/lodash'),
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

    security.authorization = require('auth/customAuthz').setProtectedAttributes(security).authorization;
}

if (resolverConfig.end_session_endpoint) {
    Object.keys(security.authorization).forEach(function (k) {
        modifiedMap[k] = security.authorization[k];
    });
    modifiedMap.logoutUrl = resolverConfig.end_session_endpoint +
        "?id_token_hint=" + id_token +
        "&post_logout_redirect_uri=" + referer;
    security.authorization = modifiedMap;
}
security;
