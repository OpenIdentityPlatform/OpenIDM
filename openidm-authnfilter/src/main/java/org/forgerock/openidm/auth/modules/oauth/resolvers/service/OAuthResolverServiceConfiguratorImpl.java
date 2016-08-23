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

import org.forgerock.openidm.auth.modules.oauth.resolvers.OAuthResolver;


import java.util.List;
import java.util.Map;

/**
 * Implementation of the {@link OAuthResolverServiceConfigurator} interface which
 * configures an OAuthResolver
 */
public class OAuthResolverServiceConfiguratorImpl implements OAuthResolverServiceConfigurator {

    @Override
    public boolean configureService(OAuthResolverService service, List<Map<String, String>> resolvers) {
        if (resolvers == null || resolvers.size() < 1) {
            return false;
        }

        boolean configured = false;

        for (Map<String, String> resolverConfig : resolvers) {
            final String userinfo_endpoint = resolverConfig.get(OAuthResolver.USER_INFO_ENDPOINT);
            if (userinfo_endpoint != null) {
                service.configureOAuthResolver(resolverConfig);
                configured = true;
            }
        }
        return configured;
    }
}
