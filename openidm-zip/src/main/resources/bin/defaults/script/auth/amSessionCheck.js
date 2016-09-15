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
    id_token = (httpRequest.getHeaders().getFirst('authToken').toString() + ""),
    parts = id_token.split('.'),
    claimsContent = parts[1],
    claims = JSON.parse(new java.lang.String(base64.decode(claimsContent))),
    session_token = claims.sessionTokenId || id_token;

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
}

security;
