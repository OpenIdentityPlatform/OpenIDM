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

package org.forgerock.openidm.idp.impl.api;

import javax.validation.constraints.NotNull;
import java.util.List;

import org.forgerock.api.annotations.Description;
import org.forgerock.openidm.idp.config.ProviderConfig;

/**
 * Lists available authentication {@link ProviderConfig providers}.
 */
public class IdentityProviderServiceResource {

    private List<ProviderConfig> providers;

    /**
     * Gets list of available authentication providers.
     *
     * @return Available authentication providers
     */
    @NotNull
    @Description("Available authentication providers")
    public List<ProviderConfig> getProviders() {
        return providers;
    }

    /**
     * Sets list of available authentication providers.
     *
     * @param providers Available authentication providers
     */
    public void setProviders(List<ProviderConfig> providers) {
        this.providers = providers;
    }
}
