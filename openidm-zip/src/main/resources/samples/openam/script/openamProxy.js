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

/*
    This endpoint script is intended to proxy requests to the openam server that
    has been specified in the authentication configuration. The reason for this 
    proxy is so that the details of the OpenAM server need not be exposed publicly,
    as well as the added benefit of making the OpenAM REST services available as
    part of the same domain as the other IDM endpoints - this makes AJAX requests
    from the browser easier to deal with (no CORS setup necessary).

    Example requests that we expect to come into this endpoint:

    GET  /openidm/endpoint/openam/json/serverinfo/* - getting details for the SSO token (cookie name and domain, for example)

    POST /openidm/endpoint/openam/json/authenticate?&_action=start [empty post body] - starting a new AM auth process

    POST /openidm/endpoint/openam/json/authenticate?_action=submitRequirements [post body with populated auth callbacks]

    POST /openidm/endpoint/openam/json/sessions?_action=logout [empty post body, but active SSO token as cookie]

    Whatever OpenAM normally responds with will be the response to these requests.
 */

(function () {

    var _ = require("lib/lodash"),
        authConfig = openidm.read("config/authentication"),
        authModule = _.find(authConfig.serverAuthContext.authModules, function (am) {
            return am.name === "OPENAM_SESSION" && am.properties.openamDeploymentUrl.length;
        }),
        proxyRequest = {
            "method" : context.http.method,
            "headers": {
                "Cookie": context.http.headers.Cookie
            }
        };


    if (authModule === null) {
        throw {
            "code" : 500,
            "message" : "Unable to find configured OPENAM_SESSION auth module"
        };
    }

    proxyRequest.url = authModule.properties.openamDeploymentUrl + "/" + request.resourcePath;

    if (request.action === "logout") {
        proxyRequest.url += "?_action=logout";
    } else {

        // turn the additionalParameters map into a url
        proxyRequest.url += "?" + _(request.additionalParameters)
                                    .pairs()
                                    .map(function (param) {
                                        return _.map(param, encodeURIComponent).join("=");
                                    })
                                    .value()
                                    .join("&");
    }

    if (request.content) {
        proxyRequest.body = request.content + ""; // implicit toString yields a stringified json value for request.content
    } else {
        proxyRequest.body = "{}";
    }


    return openidm.action("external/rest", "call", proxyRequest);

}());