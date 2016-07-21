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

package org.forgerock.openidm.selfservice.stage;

import java.util.List;
import java.util.Objects;

import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.selfservice.core.config.StageConfig;

/**
 * Configuration for the social user details stage.
 *
 */
public final class SocialUserDetailsConfig implements StageConfig {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "socialUserDetails";

    private String identityEmailField;

    private List<ProviderConfig> providers;

    /**
     * Gets the field name for the identity email address.
     *
     * @return the identity email address field name
     */
    public String getIdentityEmailField() {
        return identityEmailField;
    }

    /**
     * Sets the field name for the identity email address.
     *
     * @param identityEmailField
     *         the identity email address field name
     *
     * @return this config instance
     */
    public SocialUserDetailsConfig setIdentityEmailField(String identityEmailField) {
        this.identityEmailField = identityEmailField;
        return this;
    }

    public List<ProviderConfig> getProviders() {
        return providers;
    }

    public void setProviders(List<ProviderConfig> providers) {
        this.providers = providers;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProgressStageClassName() {
        return SocialUserDetailsStage.class.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof SocialUserDetailsConfig)) {
            return false;
        }

        SocialUserDetailsConfig that = (SocialUserDetailsConfig) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getProgressStageClassName(), that.getProgressStageClassName())
                && Objects.equals(identityEmailField, that.identityEmailField)
                && Objects.equals(providers, that.providers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProgressStageClassName(), identityEmailField, providers);
    }

}
