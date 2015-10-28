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
 * Copyright 2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/common/delegates/OpenAMProxyDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(constants, AbstractDelegate, cookieHelper, conf) {

    var obj = new AbstractDelegate(constants.host + "/openidm/endpoint/openam" );

    obj.logout = function () {
        var headers = {};

        headers[conf.globalData.auth.cookieName] = cookieHelper.getCookie(conf.globalData.auth.cookieName);

        return obj.serviceCall({
            type: "POST",
            data: "{}",
            url: "/json/sessions?_action=logout",
            headers : headers,
            errorsHandlers: {"Bad Request": {status: 400}}
        });

    };

    //this function will only be called from the admin ui using admin credentials
    obj.serverinfo = function (openamDeploymentUrl) {
        return obj.serviceCall({
            type: "POST",
            serviceUrl: constants.host + "/openidm",
            url: "/external/rest?_action=call",
            data: JSON.stringify({
                "method": "GET",
                "url": openamDeploymentUrl + "/json/serverinfo/*"
            }),
            errorsHandlers: {"Bad Request": {status: 404}}
        });
    };

    return obj;
});
