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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openidm.jaspi.auth;

import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.Context;

/**
 * Authenticates a user given a username and password combination.
 */
public interface Authenticator {

    /**
     * The result of the Authenticator#authenticate.
     */
    class AuthenticatorResult {
        private final boolean authenticated;
        private final ResourceResponse resource;

        private AuthenticatorResult(boolean authenticated, ResourceResponse resource) {
            this.authenticated = authenticated;
            this.resource = resource;
        }

        /**
         * Return whether the authentication was successful.
         * @return whether the user is authenticated
         */
        public boolean isAuthenticated() {
            return authenticated;
        }

        /**
         * Return the resource for the authenticated user.
         * @return the resource for the authenticated user.
         */
        public ResourceResponse getResource() {
            return resource;
        }

        /** a failed authentication */
        public static final AuthenticatorResult FAILED = new AuthenticatorResult(false, null);

        /** a successful authentication */
        public static final AuthenticatorResult SUCCESS = new AuthenticatorResult(true, null);

        /** create a successful authentication with a resource */
        public static AuthenticatorResult authenticationSuccess(ResourceResponse resource) {
            return new AuthenticatorResult(true, resource);
        }
    }

    /**
     * Delegates authentication to the implemented endpoint, repository, or service.
     *
     * @param username The user's username
     * @param password The user's password.
     * @param context the Context to use when making requests on the router
     * @return the result of the authentication (success/failure and, optionally, the associated resource)
     * @throws ResourceException if there is a problem whilst attempting to authenticate the user.
     */
    AuthenticatorResult authenticate(String username, String password, Context context) throws ResourceException;
}
