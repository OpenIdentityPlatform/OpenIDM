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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.auth;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_REVISION;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;


/**
 * Authenticates a static user against the provided configuration properties.
 */
class StaticAuthenticator implements Authenticator {

    /** Always use a _rev of 1 */
    private static final String RESOURCE_REV = "1";
    /** username resource content property */
    private static final String RESOURCE_FIELD_USERNAME = "userName";
    /** password resource content property */
    private static final String RESOURCE_FIELD_PASSWORD = "password";

    /** The static "resource" */
    private final ResourceResponse resource;

    /**
     * Constructs an instance of the StaticAuthenticator.
     *
     * @param username The static username.
     * @param password The static password.
     */
    public StaticAuthenticator(String username, String password) {

        Reject.ifNull(username, "username was not specified");
        Reject.ifNull(password, "password was not specified");

        resource = newResourceResponse(username, RESOURCE_REV,
                json(object(
                        field(FIELD_CONTENT_ID, username),
                        field(FIELD_CONTENT_REVISION, RESOURCE_REV),
                        field(RESOURCE_FIELD_USERNAME, username),
                        field(RESOURCE_FIELD_PASSWORD, password))));
    }

    /**
     * Performs the authentication against the configured username and password.
     *
     * @param username The username.
     * @param password The password.
     * @param context the Context to use
     * @return True if authentication is successful, otherwise false.
     */
    public AuthenticatorResult authenticate(String username, String password, Context context) throws ResourceException {

        Reject.ifNull(username, "Provided username was null");
        Reject.ifNull(context, "Router context was null");

        if (resource.getContent().get(RESOURCE_FIELD_USERNAME).asString().equals(username)
                && resource.getContent().get(RESOURCE_FIELD_PASSWORD).asString().equals(password)) {
            return AuthenticatorResult.authenticationSuccess(resource);
        } else {
            return AuthenticatorResult.FAILED;
        }
    }
}

