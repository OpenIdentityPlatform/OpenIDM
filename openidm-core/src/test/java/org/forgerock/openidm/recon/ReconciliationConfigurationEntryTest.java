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
import org.forgerock.openidm.recon.impl.ReconciliationPolicyEntry;

/**
 * Test the parsing, required and default values of reconciliation configuration.
 */
public class ReconciliationConfigurationEntryTest {

    private Map map;
    private ReconciliationConfiguration reconciliationConfiguration;
    private Collection<ReconciliationPolicyEntry> reconciliationPolicyEntries;

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
        reconciliationPolicyEntries = reconciliationConfiguration.getReconciliationPolicyEntries();

    }

    @Test
    public void nameMustExist() {
        for (ReconciliationPolicyEntry entry : reconciliationPolicyEntries) {
            assertThat(entry.getName()).isNotNull();
        }
    }

    @Test
    public void batchRecordsMustExist() {
        for (ReconciliationPolicyEntry entry : reconciliationPolicyEntries) {
            assertThat(entry.getBatchRecords()).isNotNull();
        }
    }

    @Test
    public void errorsBeforeFailingMustExist() {
        for (ReconciliationPolicyEntry entry : reconciliationPolicyEntries) {
            assertThat(entry.getErrorsBeforeFailing()).isNotNull();
        }
    }

    @Test
    public void serviceAccountMustExist() {
        for (ReconciliationPolicyEntry entry : reconciliationPolicyEntries) {
            assertThat(entry.getServiceAccount()).isNotNull();
        }
    }

    @Test
    public void detectNativeChangesToAccountsMustExist() {
        for (ReconciliationPolicyEntry entry : reconciliationPolicyEntries) {
            assertThat(entry.getDetectNativeChangesToAccounts()).isNotNull();
        }
    }

    @Test
    public void preReconciliationScriptMustExist() {
        for (ReconciliationPolicyEntry entry : reconciliationPolicyEntries) {
            assertThat(entry.getPreReconciliationScript()).isNotNull();
        }
    }

    @Test
    public void perObjectScritMustExist() {
        for (ReconciliationPolicyEntry entry : reconciliationPolicyEntries) {
            assertThat(entry.getPerObjectScript()).isNotNull();
        }
    }

    @Test
    public void postReconciliationScriptMustExist() {
        for (ReconciliationPolicyEntry entry : reconciliationPolicyEntries) {
            assertThat(entry.getPostReconciliationScript()).isNotNull();
        }
    }

    @Test
    public void correlationScriptMustExist() {
        for (ReconciliationPolicyEntry entry : reconciliationPolicyEntries) {
            assertThat(entry.getCorrelationScript()).isNotNull();
        }
    }

    @Test
    public void correlationQueryMustExist() {
        for (ReconciliationPolicyEntry entry : reconciliationPolicyEntries) {
            assertThat(entry.getCorrelationQuery()).isNotNull();
        }
    }

    @Test
    public void filterScriptMustExist() {
        for (ReconciliationPolicyEntry entry : reconciliationPolicyEntries) {
            assertThat(entry.getFilter()).isNotNull();
        }
    }

    @Test
    public void situationsMustExist() {
        for (ReconciliationPolicyEntry entry : reconciliationPolicyEntries) {
            assertThat(entry.getSituationMap()).isNotNull();
        }
    }

    @Test
    public void actionsMustExist() {
        for (ReconciliationPolicyEntry entry : reconciliationPolicyEntries) {
            assertThat(entry.getActionMap()).isNotNull();
        }
    }

}
