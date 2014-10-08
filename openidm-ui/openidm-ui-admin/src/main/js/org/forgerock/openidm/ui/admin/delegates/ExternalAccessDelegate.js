/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global $, define, _ */
define("org/forgerock/openidm/ui/admin/delegates/ExternalAccessDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate"
], function(constants, AbstractDelegate) {

    var obj = new AbstractDelegate(constants.host + "/openidm/external/rest");

    obj.getToken = function(secret, id, authCode, redirectUri, tokenUrl) {
        var googleDetails = "grant_type=authorization_code&code=" +authCode +"&client_id=" +id +"&client_secret=" +secret +"&redirect_uri=" +redirectUri,
            restDetails = {
            "url" : tokenUrl,
            "method" : "POST",
            "body" : googleDetails,
            "contentType" : "application/x-www-form-urlencoded"
        };

        return obj.serviceCall({
            url: "?_action=call",
            type: "POST",
            data: JSON.stringify(restDetails)
        });
    };

    return obj;
});