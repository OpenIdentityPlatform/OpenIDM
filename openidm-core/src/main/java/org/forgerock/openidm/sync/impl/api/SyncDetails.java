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

package org.forgerock.openidm.sync.impl.api;

import javax.validation.constraints.NotNull;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * A class for notify actions sync details in results.
 */
@Title("Sync Details")
public class SyncDetails {
    private String result;
    private String oldTargetValue;
    private String cause;
    private String reconId;
    private String action;
    private String targetId;
    private String mapping;
    private String targetObjectSet;
    private String situation;
    private String sourceId;

    /**
     * Gets the returned result.
     *
     * @return The returned result
     */
    @NotNull
    @Description("The returned result")
    public String getResult() {
        return this.result;
    }

    /**
     * Sets the returned result.
     *
     * @param result The returned result
     */
    public void setResult(String result) {
        this.result = result;
    }

    /**
     * Gets the old target value.
     *
     * @return The old target value
     */
    @NotNull
    @Description("The old target value")
    public String getOldTargetValue() {
        return this.oldTargetValue;
    }

    /**
     * Sets the old target value.
     *
     * @param oldTargetValue The old target value
     */
    public void setOldTargetValue(String oldTargetValue) {
        this.oldTargetValue = oldTargetValue;
    }

    /**
     * Gets the cause if error happens.
     *
     * @return The cause if error happens
     */
    @Description("The cause")
    public String getCause() {
        return this.cause;
    }

    /**
     * Sets the cause if error happens.
     *
     * @param cause The cause if error happens.
     */
    public void setCause(String cause) {
        this.cause = cause;
    }

    /**
     * Gets the recon ID.
     *
     * @return The recon ID
     */
    @NotNull
    @Description("The recon ID")
    public String getReconId() {
        return this.reconId;
    }

    /**
     * Sets the recon ID.
     *
     * @param reconId The recon ID
     */
    public void setReconId(String reconId) {
        this.reconId = reconId;
    }

    /**
     * Gets the target ID.
     *
     * @return The target ID
     */
    @Description("The target ID")
    public String getTargetId() {
        return this.targetId;
    }

    /**
     * Sets the target ID.
     *
     * @param targetId The target ID
     */
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    /**
     * Gets the mapping.
     *
     * @return The mapping
     */
    @NotNull
    @Description("The mapping")
    public String getMapping() {
        return this.mapping;
    }

    /**
     * Sets the mapping.
     *
     * @param mapping The mapping
     */
    public void setMapping(String mapping) {
        this.mapping = mapping;
    }

    /**
     * Gets the target object set.
     *
     * @return The target object set
     */
    @NotNull
    @Description("The target object set")
    public String getTargetObjectSet() {
        return this.targetObjectSet;
    }

    /**
     * Sets the target object set.
     *
     * @param targetObjectSet The target object set
     */
    public void setTargetObjectSet(String targetObjectSet) {
        this.targetObjectSet = targetObjectSet;
    }

    /**
     * Gets action.
     *
     * @return The action
     */
    @NotNull
    @Description("The action")
    public String getAction() {
        return this.action;
    }

    /**
     * Sets the action.
     *
     * @param action The action
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Gets the situation.
     *
     * @return The situation
     */
    @NotNull
    @Description("The situation")
    public String getSituation() {
        return this.situation;
    }

    /**
     * Sets the situation.
     *
     * @param situation The situation
     */
    public void setSituation(String situation) {
        this.situation = situation;
    }

    /**
     * Gets the source ID.
     *
     * @return The source ID
     */
    @NotNull
    @Description("The source ID")
    public String getSourceId() {
        return this.sourceId;
    }

    /**
     * Sets the source ID.
     *
     * @param sourceId The source ID
     */
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }
}
