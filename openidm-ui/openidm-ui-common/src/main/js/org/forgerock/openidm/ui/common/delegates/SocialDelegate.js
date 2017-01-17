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
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/OAuth"
], function($, _,
    AbstractDelegate,
    Configuration,
    Constants,
    OAuth) {

    var obj = new AbstractDelegate(Constants.host + "/openidm/identityProviders");

    obj.loginProviders = function () {
        var headers = {},
            promise = $.Deferred();
        headers[Constants.HEADER_PARAM_USERNAME] = "anonymous";
        headers[Constants.HEADER_PARAM_PASSWORD] = "anonymous";
        headers[Constants.HEADER_PARAM_NO_SESSION] = "true";

        return obj.serviceCall({
            url: "",
            serviceUrl: "/openidm/authentication",
            type: "get",
            headers: headers
        }).then((results) => {
            return results;
        });
    };

    obj.providerList = function() {
        var promise = $.Deferred();

        obj.serviceCall({
            url: "",
            type: "get",
            errorsHandlers: {
                "notfound": {status: 404}
            }
        }).then((results) => {
            promise.resolve(results);
        }, () => {
            promise.resolve({providers:[]});
        });
        return promise;
    };

    obj.availableProviders = function() {var headers = {},
        promise = $.Deferred();
        obj.serviceCall({
            url: "?_action=availableProviders",
            type: "post",
            errorsHandlers: {
                "notfound": {status: 404}
            }
        }).then((results) => {
            promise.resolve(results);
        }, () => {
            promise.resolve({providers:[]});
        });
        return promise;
    };

    obj.getAuthToken = function (provider, code, redirect_uri) {
        return this.serviceCall({
            "type": "POST",
            "serviceUrl": "/openidm/authentication",
            "url": "?_action=getIdPTokens",
            "data": JSON.stringify({
                provider: provider,
                code: code,
                redirect_uri: redirect_uri,
                nonce: OAuth.getCurrentNonce()
            })
        });
    };

    return obj;
});
