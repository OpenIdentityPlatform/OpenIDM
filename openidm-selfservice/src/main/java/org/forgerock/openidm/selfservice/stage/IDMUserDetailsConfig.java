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
 * Copyright 2016-2017 ForgeRock AS.
 */

package org.forgerock.openidm.selfservice.stage;

import java.util.List;
import java.util.Objects;

import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.selfservice.core.config.StageConfig;

/**
 * Configuration for the IDM user details stage.
 *
 */
public final class IDMUserDetailsConfig implements StageConfig {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "idmUserDetails";

    private String identityEmailField;
    private List<ProviderConfig> providers;
    private boolean socialRegistrationEnabled = false;

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
    public IDMUserDetailsConfig setIdentityEmailField(final String identityEmailField) {
        this.identityEmailField = identityEmailField;
        return this;
    }

    /**
     * Fetch the list of provider configurations.
     *
     * @return list of provider configs
     */
    public List<ProviderConfig> getProviders() {
        return providers;
    }

    /**
     * Set the list of provider configurations.
     *
     * @param providers list of provider configs
     */
    public void setProviders(final List<ProviderConfig> providers) {
        this.providers = providers;
    }

    /**
     * Test whether social registration is enabled.
     *
     * @return true iff social registration is enabled
     */
    public boolean isSocialRegistrationEnabled() {
        return socialRegistrationEnabled;
    }

    /**
     * Set whether social registration is enabled.
     *
     * @param socialRegistrationEnabled true iff social registration should be enabled
     */
    public void setSocialRegistrationEnabled(boolean socialRegistrationEnabled) {
        this.socialRegistrationEnabled = socialRegistrationEnabled;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProgressStageClassName() {
        return IDMUserDetailsStage.class.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof IDMUserDetailsConfig)) {
            return false;
        }

        IDMUserDetailsConfig that = (IDMUserDetailsConfig) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getProgressStageClassName(), that.getProgressStageClassName())
                && Objects.equals(identityEmailField, that.identityEmailField)
                && Objects.equals(providers, that.providers)
                && Objects.equals(socialRegistrationEnabled, that.socialRegistrationEnabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProgressStageClassName(), identityEmailField, providers,
                socialRegistrationEnabled);
    }

}
