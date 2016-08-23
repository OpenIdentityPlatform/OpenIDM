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

import java.util.List;
import java.util.Map;

/**
 * Interface directing how to configure
 * ({@link OAuthResolverServiceConfigurator#configureService(OAuthResolverService, List)})
 * an {@link OAuthResolverService}.
 *
 * Implementing classes must be aware that the configurations are "flat" and enter
 * this method call unverified and unchecked - thus can contain
 * invalid combinations of values.
 */
public interface OAuthResolverServiceConfigurator {

    /**
     * Configures a provided {@link OAuthResolverService} using the resolver information held
     * in a {@link List} of {@link Map}.
     *
     * @param service to configure
     * @param resolvers the configuration for each individual resolver
     * @return false if any resolver configuration fails; true otherwise
     */
    boolean configureService(final OAuthResolverService service, final List<Map<String, String>> resolvers);
}
