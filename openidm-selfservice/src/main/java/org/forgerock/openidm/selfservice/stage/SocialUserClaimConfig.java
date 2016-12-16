/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
 * the specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file
 * and include the License file at legal/CDDLv1.0.txt. If applicable, add the following
 * below the CDDL Header, with the fields enclosed by brackets [] replaced by your
 * own identifying information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openidm.selfservice.stage;

import java.util.List;
import java.util.Objects;

import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.selfservice.core.config.StageConfig;

/**
 * Configuration for the social user claim stage.
 *
 */
public class SocialUserClaimConfig implements StageConfig {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "socialUserClaim";

    private List<ProviderConfig> providers;
    private String identityServiceUrl;
    private String claimQueryFilter = "mail eq \"{{mail}}\"";
        ;

    /**
     * Get the list of supported providers with their configs.
     *
     * @return list of supported providers
     */
    public List<ProviderConfig> getProviders() {
        return providers;
    }

    /**
     * Set the list of supported providers with their configs.
     *
     * @param providers the list of supported providers
     */
    public void setProviders(final List<ProviderConfig> providers) {
        this.providers = providers;
    }

    /**
     * Get the identity service url for claimable resources.
     *
     * @return the claimable resource container
     */
    public String getIdentityServiceUrl() {
        return identityServiceUrl;
    }

    /**
     * Set the identity service url for claimable resources.  Path should neither start nor end with a root slash.
     * The resource must support an "idpData" attribute as a root-level attribute (see managed/user).
     *
     * @param identityServiceUrl the claimable resource container, e.g. managed/user
     */
    public void setIdentityServiceUrl(String identityServiceUrl) {
        this.identityServiceUrl = identityServiceUrl;
    }

    /**
     * Get the query filter used to identify claimable resources.
     *
     * @return the claim query filter
     */
    public String getClaimQueryFilter() {
        return claimQueryFilter;
    }

    /**
     * Set the query filter used to identify claimable resources.  The query filter may contain tokens in the form
     * of "{tokenname}" where the entire token will be replaced with the attribute value from the IdP profile
     * whose name is tokenname.
     *
     * Example: "mail eq \"{mail}\""
     * (This is the default value the claim code will use if there is no value specified in the config.)
     *
     * @param claimQueryFilter the claim query filter
     */
    public void setClaimQueryFilter(String claimQueryFilter) {
        this.claimQueryFilter = claimQueryFilter;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProgressStageClassName() {
        return SocialUserClaimStage.class.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof SocialUserClaimConfig)) {
            return false;
        }

        SocialUserClaimConfig that = (SocialUserClaimConfig) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getProgressStageClassName(), that.getProgressStageClassName())
                && Objects.equals(providers, that.providers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProgressStageClassName(), providers);
    }

}
