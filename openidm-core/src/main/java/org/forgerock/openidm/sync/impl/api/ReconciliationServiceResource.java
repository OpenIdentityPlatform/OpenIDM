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

import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;

import javax.validation.constraints.NotNull;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Format;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.sync.impl.ReconStage;
import org.forgerock.openidm.sync.impl.ReconState;

/**
 * Resource for {@link org.forgerock.openidm.sync.impl.ReconciliationService}.
 */
public class ReconciliationServiceResource {

    @JsonProperty(FIELD_CONTENT_ID)
    private String id;

    private String mapping;

    private ReconState state;

    private ReconStage stage;

    private String stageDescription;

    private ReconProgress progress;

    private Map<String, Integer> situationSummary;

    private Map<Status, Integer> statusSummary;

    private Map<String, DurationStats> durationSummary;

    private Map<String, Object> parameters;

    private String started;

    private String ended;

    private long duration;

    /**
     * Gets recon ID.
     *
     * @return Recon ID
     */
    @NotNull
    @Description("Recon ID")
    public String getId() {
        return id;
    }

    /**
     * Sets recon ID.
     *
     * @param id Recon ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets recon duration, in millisconds.
     *
     * @return Recon duration, in millisconds
     */
    @NotNull
    @Description("Recon duration, in millisconds")
    public long getDuration() {
        return duration;
    }

    /**
     * Sets recon duration, in millisconds.
     *
     * @param duration Recon duration, in millisconds
     */
    public void setDuration(long duration) {
        this.duration = duration;
    }

    /**
     * Gets duration stats for discrete recon operations.
     *
     * @return Duration stats for discrete recon operations
     */
    @NotNull
    @Description("Duration stats for discrete recon operations")
    public Map<String, DurationStats> getDurationSummary() {
        return durationSummary;
    }

    /**
     * Sets duration stats for discrete recon operations.
     *
     * @param durationSummary Duration stats for discrete recon operations
     */
    public void setDurationSummary(
            Map<String, DurationStats> durationSummary) {
        this.durationSummary = durationSummary;
    }

    /**
     * Gets ISO 8601 date-time when recon ended or empty-string if not yet completed.
     *
     * @return ISO 8601 date-time when recon ended or empty-string
     */
    @NotNull
    @Description("ISO 8601 date-time when recon ended or empty-string")
    @Format("date-time")
    public String getEnded() {
        return ended;
    }

    /**
     * Sets ISO 8601 date-time when recon ended or empty-string if not yet completed.
     *
     * @param ended ISO 8601 date-time when recon ended or empty-string
     */
    public void setEnded(String ended) {
        this.ended = ended;
    }

    /**
     * Gets Mapping name.
     *
     * @return Mapping name (e.g., systemXmlfileAccounts_managedUser)
     */
    @NotNull
    @Description("Mapping name (e.g., systemXmlfileAccounts_managedUser)")
    public String getMapping() {
        return mapping;
    }

    /**
     * Sets Mapping name.
     *
     * @param mapping Mapping name (e.g., systemXmlfileAccounts_managedUser)
     */
    public void setMapping(String mapping) {
        this.mapping = mapping;
    }

    /**
     * Gets parameters for source and target selection.
     *
     * @return Parameters for source and target selection
     */
    @NotNull
    @Description("Parameters for source and target selection")
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Sets parameters for source and target selection.
     *
     * @param parameters Parameters for source and target selection
     */
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    /**
     * Gets recon progress stats.
     *
     * @return Recon progress stats
     */
    @NotNull
    @Description("Recon progress stats")
    public ReconProgress getProgress() {
        return progress;
    }

    /**
     * Sets recon progress stats.
     *
     * @param progress Recon progress stats
     */
    public void setProgress(ReconProgress progress) {
        this.progress = progress;
    }

    /**
     * Gets stats related to success and failure.
     *
     * @return Stats related to success and failure
     */
    @NotNull
    @Description("Stats related to success and failure")
    public Map<String, Integer> getSituationSummary() {
        return situationSummary;
    }

    /**
     * Sets stats related to success and failure.
     *
     * @param situationSummary Stats related to success and failure
     */
    public void setSituationSummary(Map<String, Integer> situationSummary) {
        this.situationSummary = situationSummary;
    }

    /**
     * Gets fine-grained stage of recon process.
     *
     * @return Fine-grained stage of recon process
     */
    @NotNull
    @Description("Fine-grained stage of recon process")
    public ReconStage getStage() {
        return stage;
    }

    /**
     * Sets fine-grained stage of recon process.
     *
     * @param stage Fine-grained stage of recon process
     */
    public void setStage(ReconStage stage) {
        this.stage = stage;
    }

    /**
     * Gets description of current stage.
     *
     * @return Description of current stage
     */
    @NotNull
    @Description("Description of current stage")
    public String getStageDescription() {
        return stageDescription;
    }

    /**
     * Sets description of current stage.
     *
     * @param stageDescription Description of current stage
     */
    public void setStageDescription(String stageDescription) {
        this.stageDescription = stageDescription;
    }

    /**
     * Gets ISO 8601 date-time when recon started.
     *
     * @return ISO 8601 date-time when recon started
     */
    @NotNull
    @Description("ISO 8601 date-time when recon started")
    @Format("date-time")
    public String getStarted() {
        return started;
    }

    /**
     * Sets ISO 8601 date-time when recon started.
     *
     * @param started ISO 8601 date-time when recon started
     */
    public void setStarted(String started) {
        this.started = started;
    }

    /**
     * Gets coarse-grained state of recon process.
     *
     * @return Coarse-grained state of recon process
     */
    @NotNull
    @Description("Coarse-grained state of recon process")
    public ReconState getState() {
        return state;
    }

    /**
     * Sets coarse-grained state of recon process.
     *
     * @param state Coarse-grained state of recon process
     */
    public void setState(ReconState state) {
        this.state = state;
    }

    /**
     * Gets success and failure counts.
     *
     * @return Success and failure counts
     */
    @NotNull
    @Description("Success and failure counts")
    public Map<Status, Integer> getStatusSummary() {
        return statusSummary;
    }

    /**
     * Sets success and failure counts.
     *
     * @param statusSummary Success and failure counts
     */
    public void setStatusSummary(Map<Status, Integer> statusSummary) {
        this.statusSummary = statusSummary;
    }

}
