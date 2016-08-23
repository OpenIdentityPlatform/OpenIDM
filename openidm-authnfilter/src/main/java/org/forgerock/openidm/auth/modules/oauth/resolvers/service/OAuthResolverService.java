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
package org.forgerock.openidm.auth.modules.oauth.resolvers.service;

import org.forgerock.openidm.auth.modules.oauth.resolvers.OAuthResolverImpl;

import java.util.Map;

/**
 * Interface through which OAuthResolvers are obtained, and the service providing
 * them is configured.
 *
 * A resolver can be configured via a configuration Map that holds all the necessary
 * oauth endpoints required for validating a token.
 *
 * The service will then provide access to the specific resolver needed at the point of
 * verification by keying on its resolver name value.
 *
 */
public interface OAuthResolverService {

    /**
     * Lookup key for the resolver in the configuration.
     */
    String RESOLVER_NAME = "name";

    /**
     * Returns the appropriate resolver for the given identity provider - if it exists;
     *
     * @param resolverName the name of the identity provider to engage the oauth flow with
     * @return A resolver which can handle verification of an OAuth access token
     */
    OAuthResolverImpl getResolver(String resolverName);

    /**
     * Configures an OAuthResolver with the provided configuration.
     *
     * @param config identity provider configuration details
     * @return true if the resolver is configured correctly; false otherwise
     */
    boolean configureOAuthResolver(Map config);
}
