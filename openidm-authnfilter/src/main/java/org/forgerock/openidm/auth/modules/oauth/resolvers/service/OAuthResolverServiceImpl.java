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

import static org.forgerock.caf.authentication.framework.AuthenticationFramework.LOG;
import static org.forgerock.http.handler.HttpClientHandler.OPTION_LOADER;

import org.forgerock.http.Client;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.apache.async.AsyncHttpClientProvider;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.spi.Loader;
import org.forgerock.openidm.auth.modules.oauth.resolvers.OAuthResolverImpl;
import org.forgerock.util.Options;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Populates a Map of current configured resolvers.
 *
 * Each resolver will have their own {@link Client} that will
 * allow them to make http requests to the Idp to help validate
 * the an access token.
 */
public class OAuthResolverServiceImpl implements OAuthResolverService {

    private final ConcurrentMap<String, OAuthResolverImpl> oauthResolvers = new ConcurrentHashMap<>();

    @Override
    public OAuthResolverImpl getResolver(final String resolverName) {
        return oauthResolvers.get(resolverName);
    }

    @Override
    public boolean configureOAuthResolver(final Map config) {
        try {
            final String resolverName = config.get(RESOLVER_NAME).toString();
            final OAuthResolverImpl impl = new OAuthResolverImpl(resolverName, config, newHttpClient());
            oauthResolvers.put(resolverName, impl);
        } catch (IllegalArgumentException e) {
            LOG.debug("Could not configure OAuth resolver instance", e);
            return false;
        } catch (HttpApplicationException e) {
            LOG.debug("Could not instantiate http client.", e);
            return false;
        }
        return true;
    }

    /** HTTP Client used for OAuth Http Requests.
     *
     * @return an instance of {@link Client}
     * @throws HttpApplicationException
     */
    private Client newHttpClient() throws HttpApplicationException {
        return new Client(
                new HttpClientHandler(
                        Options.defaultOptions()
                                .set(OPTION_LOADER, new Loader() {
                                    @Override
                                    public <S> S load(Class<S> service, Options options) {
                                        return service.cast(new AsyncHttpClientProvider());
                                    }
                                })));
    }
}
