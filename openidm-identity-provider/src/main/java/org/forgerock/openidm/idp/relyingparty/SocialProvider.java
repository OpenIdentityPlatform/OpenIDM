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
package org.forgerock.openidm.idp.relyingparty;

import org.forgerock.json.resource.ResourceException;

/**
 * Abstraction of a "social provider"
 */
public interface SocialProvider {

    /**
     * Retrieves authorization details used to retrieve social profile.
     *
     * @param code used for OAuth 2.0 flow
     * @param redirectUri uri that the identity provider will
     *                    redirect to once successful authorization is complete
     * @return {@link AuthDetails} which includes an access token used to make
     *         requests via the OAuth 2.0 flow
     *
     * @throws ResourceException when authorization error occurs
     */
    AuthDetails getAuthDetails(String code, String redirectUri) throws ResourceException;

    /**
     * Retrieves a social user profile.
     *
     * @param code the authorization code
     * @param redirectUri uri that the identity provider will
     *                    redirect to once successful authorization is complete
     * @return {@link SocialUser} profile
     *
     * @throws ResourceException when authorization error occurs
     */
    SocialUser getSocialUser(String code, String redirectUri) throws ResourceException;

}