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
 * A bean like wrapper for reconciliation {@policies} defined in json configuration.
 * </p>
 * The following default values are applied if values are missing from configuration.
 * </p>
 * {@code batchRecord = 10}
 * {@code errorsBeforeFailing = 30}
 * {@code detectNativeChangesToAccounts = false}
 */
public class ReconciliationPolicyEntry {

    private static final long serialVersionUID = 1L;

    private String name;
    private Integer batchRecords;
    private Integer errorsBeforeFailing;
    private String serviceAccount;
    private Boolean detectNativeChangesToAccounts;
    private String preReconciliationScript;
    private String perObjectScript;
    private String postReconciliationScript;
    private String correlationScript;
    private String correlationQuery;
    private String filter;
    private SituationMap situationMap;
    private ActionMap actionMap;

    /**
     * Construct the reconciliation policy from json configuration data.
     *
     * @param policyConfiguration json configuration
     * @throws ReconciliationException if there is an error validating the configuration
     */
    public ReconciliationPolicyEntry(Map<String, Object> policyConfiguration) throws JsonNodeException {
        mapProperties(policyConfiguration);
    }

    /**
     * Helper to pull out and map json data to the policy structure
     *
     * @param policyConfiguration json data
     * @throws ReconciliationException if there is a validation error
     */
    private void mapProperties(Map<String, Object> policyConfiguration) throws JsonNodeException {
        JsonNode node = new JsonNode(policyConfiguration);
        setName(node.get("name").required().asString());
        setBatchRecords(node.get("batchRecords").defaultTo(10).asInteger());
        setErrorsBeforeFailing(node.get("errorsBeforeFailing").defaultTo(30).asInteger());
        setServiceAccount(node.get("serviceAccount").required().asString());
        setDetectNativeChangesToAccounts(node.get("detectNativeChanges").defaultTo(false).asBoolean());
        setPreReconciliationScript(node.get("preReconciliationScript").asString());
        setPerObjectScript(node.get("perObjectScript").asString());
        setPostReconciliationScript(node.get("postReconciliationScript").asString());
        setCorrelationScript(node.get("correlationScript").required().asString());
        setCorrelationQuery(node.get("correlationQuery").asString());
        setFilter(node.get("filterScript").asString());
        setSituationMap(new SituationMap(node.get("situations").required().asMap()));
        setActionMap(new ActionMap(node.get("actionMap").required().asMap()));
    }

    /**
     * Get the name of this policy, as it was read from configuration.
     *
     * @return name of this policy
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this policy, as it will read in configuration.
     * <p/>
     * Must not be null.
     *
     * @param name of this policy
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Get the number of batch records to process, the reconciliation engine will process
     * this number of system records at a time.
     * <p/>
     * There is a default set to 10
     *
     * @return batchRecords to process
     */
    public Integer getBatchRecords() {
        return batchRecords;
    }

    /**
     * Set the number of batch record for the reconciliation engine to process at
     * a time per node.
     * <p/>
     * Must be a positive Integer.
     *
     * @param batchRecords
     */
    public void setBatchRecords(Integer batchRecords) {
        this.batchRecords = batchRecords;
    }

    /**
     * Get the number of reconciliation errors that should be allowed before the reconciliation
     * process has been considered to fail.
     * <p/>
     * There is a default of 30
     *
     * @return errorsBeforeFailing
     */
    public Integer getErrorsBeforeFailing() {
        return errorsBeforeFailing;
    }

    /**
     * Set the number of reconciliation errors that should be allowed before the reconciliation
     * process should be considered a failure.
     * <p/>
     * Must be a positive Integer.
     *
     * @param errorsBeforeFailing reconciliation
     */
    public void setErrorsBeforeFailing(Integer errorsBeforeFailing) {
        this.errorsBeforeFailing = errorsBeforeFailing;
    }

    /**
     * Get the Service Account id of the user that will be running the reconciliation process.
     *
     * @return serviceAccount is the user id of the reconciliation user
     */
    public String getServiceAccount() {
        return serviceAccount;
    }

    /**
     * Set the Service Account id of the user that will be running the reconciliation process.
     * <p/>
     * Must not be null, and must be a valid user id.
     *
     * @param serviceAccount
     */
    public void setServiceAccount(String serviceAccount) {
        this.serviceAccount = serviceAccount;
    }

    /**
     * Get whether or not the reconciliation process should detect native changes
     * to account attributes from the System Object.
     * <p/>
     * The default is false
     *
     * @return detectNativeChangesToAccounts
     */
    public Boolean getDetectNativeChangesToAccounts() {
        return detectNativeChangesToAccounts;
    }

    /**
     * Set whether or not the reconciliation process hould detect native changes
     * to account attributes from the  System Object.
     *
     * @param detectNativeChangesToAccounts
     */
    public void setDetectNativeChangesToAccounts(Boolean detectNativeChangesToAccounts) {
        this.detectNativeChangesToAccounts = detectNativeChangesToAccounts;
    }

    /**
     * Get the script that should run before reconciliation.
     *
     * @return preReconciliationScript
     */
    public String getPreReconciliationScript() {
        return preReconciliationScript;
    }

    /**
     * Set the script that should run before reconciliation.
     *
     * @param preReconciliationScript
     */
    public void setPreReconciliationScript(String preReconciliationScript) {
        this.preReconciliationScript = preReconciliationScript;
    }

    /**
     * Get the script that should run after every reconciled object.
     *
     * @return
     */
    public String getPerObjectScript() {
        return perObjectScript;
    }

    /**
     * Set the script that should run after every reconciled object.
     *
     * @param perObjectScript
     */
    public void setPerObjectScript(String perObjectScript) {
        this.perObjectScript = perObjectScript;
    }

    /**
     * Get the script that should run after every reconciliation run.
     *
     * @return postReconciliationScript
     */
    public String getPostReconciliationScript() {
        return postReconciliationScript;
    }

    /**
     * Set the script that should run after every reconciliation run.
     *
     * @param postReconciliationScript
     */
    public void setPostReconciliationScript(String postReconciliationScript) {
        this.postReconciliationScript = postReconciliationScript;
    }

    /**
     * Get the script that should be run for correlation.
     *
     * @return correlationScript
     */
    public String getCorrelationScript() {
        return correlationScript;
    }

    /**
     * Set the script that should be used for correlation.
     *
     * @param correlationScript
     */
    public void setCorrelationScript(String correlationScript) {
        this.correlationScript = correlationScript;
    }

    /**
     * Get the correlation query to narrow down reconciliation correlation queries
     * to only those attributes required to correlate. This needs to be a {@code named
     * query}, and must not be a native inline query.
     *
     * @return
     */
    public String getCorrelationQuery() {
        return correlationQuery;
    }

    /**
     * Set the correlation query.
     *
     * @param correlationQuery must be a {@code named query}
     */
    public void setCorrelationQuery(String correlationQuery) {
        this.correlationQuery = correlationQuery;
    }

    /**
     * Get the filter script to be applied to returned reconciliation records
     *
     * @return filter script
     */
    public String getFilter() {
        return filter;
    }

    /**
     * Set the filter script to be applied to returned reconciliation records
     *
     * @param filter
     */
    public void setFilter(String filter) {
        this.filter = filter;
    }

    /**
     * Get the mapping of situations to script that need to be applied for this
     * policy.
     *
     * @return situationMap
     */
    public SituationMap getSituationMap() {
        return situationMap;
    }

    /**
     * Set the mapping of situations to scripts that need to be applied for this
     * policy.
     *
     * @param situationMap
     */
    public void setSituationMap(SituationMap situationMap) {
        this.situationMap = situationMap;
    }

    /**
     * Get the configured action map for this policy configuration.
     *
     * @return actionMap that maps script string results to {@Action}s
     */
    public ActionMap getActionMap() {
        return actionMap;
    }

    /**
     * @param actionMap
     */
    public void setActionMap(ActionMap actionMap) {
        this.actionMap = actionMap;
    }

    /**
     * Debugging toString
     *
     * @return nested toString values
     */
    @Override
    public String toString() {
        return "ReconciliationPolicyEntry{" +
                "name='" + name + '\'' +
                ", batchRecords=" + batchRecords +
                ", errorsBeforeFailing=" + errorsBeforeFailing +
                ", serviceAccount='" + serviceAccount + '\'' +
                ", detectNativeChangesToAccounts=" + detectNativeChangesToAccounts +
                ", preReconciliationScript='" + preReconciliationScript + '\'' +
                ", perObjectScript='" + perObjectScript + '\'' +
                ", postReconciliationScript='" + postReconciliationScript + '\'' +
                ", correlationScript='" + correlationScript + '\'' +
                ", correlationQuery='" + correlationQuery + '\'' +
                ", filter='" + filter + '\'' +
                ", situationMap=" + situationMap +
                ", actionMap=" + actionMap +
                '}';
    }
}