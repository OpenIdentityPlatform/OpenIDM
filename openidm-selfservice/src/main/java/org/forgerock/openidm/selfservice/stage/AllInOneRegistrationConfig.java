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

import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.selfservice.core.config.StageConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for the AllInOneRegistration stage
 */
public final class AllInOneRegistrationConfig implements StageConfig {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "allInOneRegistration";

    private List<StageConfig> configs = new ArrayList<>();
    private List<ProviderConfig> providers = new ArrayList<>();

    public List<StageConfig> getConfigs() {
        return configs;
    }

    public AllInOneRegistrationConfig setConfigs(List<StageConfig> configs) {
        this.configs = configs;
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
    public AllInOneRegistrationConfig setProviders(final List<ProviderConfig> providers) {
        this.providers = providers;
        return this;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProgressStageClassName() {
        return AllInOneRegistrationStage.class.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AllInOneRegistrationConfig)) {
            return false;
        }

        AllInOneRegistrationConfig that = (AllInOneRegistrationConfig) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getProgressStageClassName(), that.getProgressStageClassName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProgressStageClassName(), configs);
    }
}
