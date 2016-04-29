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
package org.forgerock.openidm.router;

import org.forgerock.json.resource.Filter;

/**
 * Interface for registering CREST router filters on the CREST FilterChain.
 */
public interface RouterFilterRegistration {

    /**
     * Adds the given router filter to the filter chain.
     *
     * @param filter the router filter
     */
    void addFilter(Filter filter);

    /**
     * Removes the router filter from the filter chain.
     *
     * @param filter the registered Filter
     */
    void removeFilter(Filter filter);

    /**
     * Instructs the filter registration service that all router filters have been added and the service is "ready".
     */
    void setRouterFilterReady();

    /**
     * Instructs the filter registration service that router filters are not ready and the service should prevent
     * inbound requests.
     */
    void setRouterFilterNotReady();
}
