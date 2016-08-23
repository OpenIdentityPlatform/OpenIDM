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
package org.forgerock.openidm.auth.modules.oauth.resolvers;

import org.forgerock.openidm.auth.modules.oauth.exceptions.OAuthVerificationException;

/**
 * Validation of OAuth access token verification.
 *
 * Validation of the access token is made by making
 * a request to the identity provider to retrieve a user profile.
 * if a user profile is returned we can be comfortable that that access
 * token is valid for the identity provider and can continue the
 * authentication flow.
 *
 * {@link OAuthResolver#validateIdentity(String)} performs all individual checks.
 */
public interface OAuthResolver {

    /**
     * Lookup key for the 'subject' of the user.
     */
    String AUTHENTICATION_ID = "authenticationId";

    /**
     * Lookup key for the user info endpoint of the identity provider.
     */
    String USER_INFO_ENDPOINT = "userinfo_endpoint";


    /**
     * Validates the supplied access token against an identity provider(Idp).
     *
     * @param accessToken access token to verify
     * @throws OAuthVerificationException if the accessToken is unable to be verified
     */
    void validateIdentity(final String accessToken) throws OAuthVerificationException;

    /**
     * Returns the subject used to link the identity provider account to known account information.
     *
     * @return the subject which to associate the user to
     */
    String getSubject();

    /**
     * Returns identity provider for which this resolver will resolve identities.
     *
     * @return the name of the identity provider
     */
    String getName();
}
