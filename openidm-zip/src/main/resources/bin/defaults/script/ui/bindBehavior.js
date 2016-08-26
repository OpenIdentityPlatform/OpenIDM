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

/**
    Implements an action request which will augment the currently-running user
    with a profile read from an identity provider.

    Expects three "additionalParameters" provided as part of the request:
        code : the authorization code obtained from the authorization endpoint (likely via a browser)
        redirect_uri: the return endpoint provided by the browser during the authorization request
        provider: the name of the identity provider which is being interacted with

    Returns the modified user account
*/
(function () {

    var user = openidm.read(
        context.security.authorization.component + "/" + context.security.authorization.id
    );

    if (!user.idpData) {
        user.idpData = {};
    }

    user.idpData[request.additionalParameters.provider] = openidm.action(
        "identityProviders", "getProfile",
        {
            redirect_uri: request.additionalParameters.redirect_uri,
            code: request.additionalParameters.code,
            provider: request.additionalParameters.provider
        }
    );

    return openidm.update(
        context.security.authorization.component + "/" + context.security.authorization.id,
        user._rev,
        user
    );

}());
