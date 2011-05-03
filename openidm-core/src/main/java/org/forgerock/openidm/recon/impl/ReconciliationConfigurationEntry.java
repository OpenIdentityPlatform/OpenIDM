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

import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

/**
 * A bean like wrapper for {@code reconciliation-configuration} json data.
 * </p>
 * The following default values are applied if they are missing from configuration.
 * </p>
 * {@code reconciliationType = "incremental"}
 * {@code enabled = false}
 * </p>
 * Policy defaults are outlined in {@link ReconciliationPolicyEntry}
 */
public class ReconciliationConfigurationEntry {

    private static final long serialVersionUID = 1L;

    private String name;
    private String sourceObject;
    private String targetObject;
    private String policyName;
    private String reconciliationType;
    private Boolean enabled;

    /**
     * Construct a reconciliation configuration from the given json configuration, the schema
     * will be validated and enforced. If there is a schema violation a {@link ReconciliationException}
     * will be raised.
     *
     * @param reconciliationConfigMap json configuration
     * @throws JsonNodeException if there are schema violations or any underlying exceptions
     */
    public ReconciliationConfigurationEntry(Map<String, Object> reconciliationConfigMap) throws JsonNodeException {
        mapProperties(reconciliationConfigMap);
    }

    private void mapProperties(Map<String, Object> reconciliationConfigMap) throws JsonNodeException {
        JsonNode node = new JsonNode(reconciliationConfigMap);
        setName(node.required().get("name").required().asString());
        setSourceObject(node.get("sourceObject").required().asString());
        setTargetObject(node.get("targetObject").required().asString());
        setPolicyName(node.get("policyName").required().asString());
        setReconciliationType(node.get("reconciliationType").defaultTo("incremental").asString());
        Boolean isEnabled = node.get("enabled").defaultTo(false).asBoolean();
        setEnabled(isEnabled);
    }

    /**
     * Get the name of this reconciliation configuration.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this policy.
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the sourceObject identifier.
     *
     * @return sourceObject
     */
    public String getSourceObject() {
        return sourceObject;
    }

    /**
     * Set teh sourceObject identifier.
     *
     * @param sourceObject
     */
    public void setSourceObject(String sourceObject) {
        this.sourceObject = sourceObject;
    }

    /**
     * Get the targetObject identifier.
     *
     * @return targetObject
     */
    public String getTargetObject() {
        return targetObject;
    }

    /**
     * Set the targetObject identifier.
     *
     * @param targetObject
     */
    public void setTargetObject(String targetObject) {
        this.targetObject = targetObject;
    }

    /**
     * Get the name of the policy that applies for this configuration.
     *
     * @return policyName
     */
    public String getPolicyName() {
        return policyName;
    }

    /**
     * Set the policyName that applies for this configuration.
     *
     * @param policyName
     */
    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    /**
     * Get the reconciliation type that applies for this configuration.
     *
     * @return reconciliationType either full or incremental
     */
    public String getReconciliationType() {
        return reconciliationType;
    }

    /**
     * Get the reconciliation type that applies for this configuration.
     *
     * @param reconciliationType
     */
    public void setReconciliationType(String reconciliationType) {
        this.reconciliationType = reconciliationType;
    }

    /**
     * Is this configuration enabled or not, only enabled configurations can
     * be executed.
     *
     * @return enabled
     */
    public Boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable this configuration.
     *
     * @param enabled
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }


    /**
     * Debugging toString
     *
     * @return nested toString values
     */
    @Override
    public String toString() {
        return "ReconciliationConfigurationEntry{" +
                "name='" + name + '\'' +
                ", sourceObject='" + sourceObject + '\'' +
                ", targetObject='" + targetObject + '\'' +
                ", policyName='" + policyName + '\'' +
                ", reconciliationType='" + reconciliationType + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}