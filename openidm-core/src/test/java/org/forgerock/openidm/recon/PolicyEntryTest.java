/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.recon;

import java.util.Map;
import java.util.Collection;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import static org.fest.assertions.Assertions.assertThat;

import org.forgerock.openidm.recon.impl.ReconciliationConfiguration;
import org.forgerock.openidm.recon.impl.ReconciliationConfigurationEntry;

/**
 * Test the parsing, required and default values for policy entries.
 */
public class PolicyEntryTest {

    private Map map;
    private ReconciliationConfiguration reconciliationConfiguration;
    private Collection<ReconciliationConfigurationEntry> reconciliationConfigurationEntries;

    @BeforeClass
    public void loadConfiguration() throws Exception {
        Map<String, Object> config = null;
        ConfigurationLoader loader = new ConfigurationLoader();
        config = loader.loadConfiguration(ConfigurationConstants.reconciliationTestConfig());
        map = (Map) config.get("reconciliation");
        fullConfigurationParse();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fullConfigurationParse() throws Exception {

        reconciliationConfiguration = new ReconciliationConfiguration(map);
        reconciliationConfigurationEntries = reconciliationConfiguration.getReconciliationConfigurationEntries();

    }

    @Test
    public void nameMustExist() {
        for (ReconciliationConfigurationEntry entry : reconciliationConfigurationEntries) {
            assertThat(entry.getName()).isNotNull();
        }
    }

    @Test
    public void sourceObjectMustExist() {
        for (ReconciliationConfigurationEntry entry : reconciliationConfigurationEntries) {
            assertThat(entry.getSourceObject()).isNotNull();
        }
    }

    @Test
    public void targetObjectMustExist() {
        for (ReconciliationConfigurationEntry entry : reconciliationConfigurationEntries) {
            assertThat(entry.getTargetObject()).isNotNull();
        }
    }

    @Test
    public void policyNameMustExist() {
        for (ReconciliationConfigurationEntry entry : reconciliationConfigurationEntries) {
            assertThat(entry.getPolicyName()).isNotNull();
        }
    }

    @Test
    public void reconciliationTypeMustExist() {
        for (ReconciliationConfigurationEntry entry : reconciliationConfigurationEntries) {
            assertThat(entry.getReconciliationType()).isNotNull();
        }
    }

    @Test
    public void enabledMustExist() {
        for (ReconciliationConfigurationEntry entry : reconciliationConfigurationEntries) {
            assertThat(entry.isEnabled()).isNotNull();
        }
    }
}
