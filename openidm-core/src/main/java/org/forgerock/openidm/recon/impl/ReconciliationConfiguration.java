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

package org.forgerock.openidm.recon.impl;

import java.util.Map;
import java.util.HashMap;

import java.util.List;
import java.util.Iterator;
import java.util.Collection;

import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

/**
 * A bean like wrapper for all reconciliation configuration and nested
 * json structures.
 */
public class ReconciliationConfiguration {

    private final static long serialVersionUID = 1l;

    Map<String, ReconciliationConfigurationEntry> reconciliationNameToEntryMap;
    Map<String, ReconciliationPolicyEntry> reconciliationNamePolicyMap;

    /**
     * Build out the reconciliation configuration based on the provided configuration
     * json map.
     *
     * @param reconciliationConfigurationMap
     * @throws JsonNodeException if there is a parsing error or a required property is missing
     */
    public ReconciliationConfiguration(Map<String, Object> reconciliationConfigurationMap) throws JsonNodeException {
        JsonNode node = new JsonNode(reconciliationConfigurationMap);
        reconciliationNameToEntryMap = buildReconciliationEntries(reconciliationConfigurationMap);
        List policyEntries = node.get("policies").required().asList();
        reconciliationNamePolicyMap = mapReconciliationPolicies(policyEntries);
    }

    /**
     * Build out individual {@link ReconciliationConfigurationEntry}'s that have been defined.
     *
     * @param reconciliationConfigurationMap
     * @return
     * @throws JsonNodeException
     */
    @SuppressWarnings("unchecked")
    private Map<String, ReconciliationConfigurationEntry>
    buildReconciliationEntries(Map<String, Object> reconciliationConfigurationMap) throws JsonNodeException {
        Map<String, ReconciliationConfigurationEntry> map = new HashMap<String, ReconciliationConfigurationEntry>();
        JsonNode node = new JsonNode(reconciliationConfigurationMap);
        List reconciliationConfigurations = node.get("reconciliation-configurations").asList();
        if (reconciliationConfigurations != null) {
            Iterator<Map> it = reconciliationConfigurations.iterator();
            while (it.hasNext()) {
                ReconciliationConfigurationEntry entry = new ReconciliationConfigurationEntry(it.next());
                map.put(entry.getName(), entry);
            }
        }
        return map;
    }

    /**
     * Build out the individual {@link org.forgerock.openidm.recon.impl.ReconciliationPolicyEntry}'s that have been defined.
     *
     * @param policies list
     * @return
     * @throws JsonNodeException
     */
    @SuppressWarnings("unchecked")
    private Map<String, ReconciliationPolicyEntry> mapReconciliationPolicies(List policies) throws JsonNodeException {
        reconciliationNamePolicyMap = new HashMap<String, ReconciliationPolicyEntry>();
        Iterator<Map> it = policies.iterator();
        while (it.hasNext()) {
            Map policyMap = it.next();
            JsonNode node = new JsonNode(policyMap);
            ReconciliationPolicyEntry entry = new ReconciliationPolicyEntry(policyMap);
            String name = node.get("name").required().asString();
            reconciliationNamePolicyMap.put(name, entry);
        }
        return reconciliationNamePolicyMap;
    }

    /**
     * Get all {@link ReconciliationConfigurationEntry}'s that have been defined.
     *
     * @return reconciliationNameToEntryMap.values();
     */
    public Collection<ReconciliationConfigurationEntry> getReconciliationConfigurationEntries() {
        return reconciliationNameToEntryMap.values();
    }

    public ReconciliationConfigurationEntry getReconciliationConfigurationEntry(String configurationName) {
        return reconciliationNameToEntryMap.get(configurationName);
    }

    public ReconciliationPolicyEntry getReconciliationPolicyEntry(String policyName) {
        return reconciliationNamePolicyMap.get(policyName);
    }

    /**
     * Get all {@link ReconciliationPolicyEntry}'s that have been defined.
     *
     * @return reconciliationNamePolicyMap.values();
     */
    public Collection<ReconciliationPolicyEntry> getReconciliationPolicyEntries() {
        return reconciliationNamePolicyMap.values();
    }

    /**
     * Debugging toString.
     *
     * @return nested toString values
     */
    @Override
    public String toString() {
        return "ReconciliationConfiguration{" +
                "reconciliationNameToEntryMap=" + reconciliationNameToEntryMap +
                ", reconciliationNamePolicyMap=" + reconciliationNamePolicyMap +
                '}';
    }
}
