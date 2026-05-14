/**
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

(function () {
    if(request.method === "action") {
        var oAuthConfig,
            decryptedClientSecret,
            connectorLocation;

        if (request.action === "getAuthZCode") {
            connectorLocation = request.content.connectorLocation.replace("_", "/");

            oAuthConfig = openidm.read("config/" + connectorLocation);

            if (oAuthConfig) {
                if (typeof oAuthConfig.configurationProperties.clientSecret === "object") {
                    decryptedClientSecret = openidm.decrypt(oAuthConfig.configurationProperties.clientSecret);

                    request.content.body += "&client_secret=" + decryptedClientSecret;
                } else {
                    request.content.body += "&client_secret=" + oAuthConfig.configurationProperties.clientSecret;
                }
            }
        }

        request.content["contentType"] = "application/x-www-form-urlencoded";

        return openidm.action("external/rest", "call", request.content);
    } else {
        throw {
            "code" : 400,
            "message" : "Bad request only support _action methods getAuthZCode"
        };
    }
}());