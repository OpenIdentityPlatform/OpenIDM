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
 * Copyright 2014-2016 ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate"
], function(constants, AbstractDelegate) {

    var obj = new AbstractDelegate(constants.host + "/openidm/endpoint/oauthproxy");

    obj.getToken = function(id, authCode, redirectUri, tokenUrl, connectorLocation) {
        var googleDetails = "grant_type=authorization_code&code=" +authCode +"&client_id=" +id  +"&redirect_uri=" +redirectUri,
            restDetails = {
                "url" : tokenUrl,
                "method" : "POST",
                "body" : googleDetails,
                "contentType" : "application/x-www-form-urlencoded",
                "connectorLocation" : connectorLocation
            };

        return obj.serviceCall({
            url: "?_action=getAuthZCode",
            type: "POST",
            data: JSON.stringify(restDetails)
        });
    };

    return obj;
});
