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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.shell.impl;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.shell.impl.UpdateCommand.UpdateStep;

/**
 * This class holds the data gathered during the UpdateCommand execution and recovery phases.
 */
class UpdateExecutionState {
    private JsonValue archiveData;
    private JsonValue installResponse;
    private long startInstallTime;
    private String completedInstallStatus;
    private UpdateStep lastAttemptedStep;
    private UpdateStep lastRecoveryStep;

    /**
     * Returns the archive metadata regarding the archive to be installed.
     *
     * @return the archive metadata regarding the archive to be installed.
     */
    public JsonValue getArchiveData() {
        return archiveData;
    }

    /**
     * Sets the archive metadata regarding the archive to be installed.
     *
     * @param archiveData the archive metadata regarding the archive to be installed.
     */
    public void setArchiveData(JsonValue archiveData) {
        this.archiveData = archiveData;
    }

    /**
     * Returns the response json provided when the initial install request was made.
     *
     * @return the response json provided when the initial install request was made.
     */
    public JsonValue getInstallResponse() {
        return installResponse;
    }

    /**
     * Sets the response json provided when the initial install request was made.
     *
     * @param installResponse The response json provided when the initial install request was made.
     */
    public void setInstallResponse(JsonValue installResponse) {
        this.installResponse = installResponse;
    }

    /**
     * Returns the start time in milliseconds for when the initial install request was made.
     *
     * @return the start time in milliseconds for when the initial install request was made.
     */
    public long getStartInstallTime() {
        return startInstallTime;
    }

    /**
     * Sets the start time in milliseconds for when the initial install request was made.
     *
     * @param startInstallTime the start time in milliseconds for when the initial install request was made.
     */
    public void setStartInstallTime(long startInstallTime) {
        this.startInstallTime = startInstallTime;
    }

    /**
     * Returns the final status returned by the finished install step, null if the install didn't complete.
     *
     * @return the final status returned by the finished install step, null if the install didn't complete.
     */
    public String getCompletedInstallStatus() {
        return completedInstallStatus;
    }

    /**
     * Sets the final status returned by the finished install step, null if the install didn't complete.
     *
     * @param completedInstallStatus the final status returned by the finished install step, null if the install
     * didn't complete.
     */
    public void setCompletedInstallStatus(String completedInstallStatus) {
        this.completedInstallStatus = completedInstallStatus;
    }

    /**
     * Returns the last attempted step of the execution phase of the update command.
     *
     * @return the last attempted step of the execution phase of the update command.
     */
    public UpdateStep getLastAttemptedStep() {
        return lastAttemptedStep;
    }

    /**
     * Sets the last attempted step of the execution phase of the update command.
     *
     * @param lastAttemptedStep the last attempted step of the execution phase of the update command.
     */
    public void setLastAttemptedStep(UpdateStep lastAttemptedStep) {
        this.lastAttemptedStep = lastAttemptedStep;
    }

    /**
     * Returns the last step executed in the recovery phase of the update command.
     *
     * @return the last step executed in the recovery phase of the update command.
     */
    public UpdateStep getLastRecoveryStep() {
        return lastRecoveryStep;
    }

    /**
     * Sets the last step executed in the recovery phase of the update command.
     *
     * @param lastRecoveryStep the last step executed in the recovery phase of the update command.
     */
    public void setLastRecoveryStep(UpdateStep lastRecoveryStep) {
        this.lastRecoveryStep = lastRecoveryStep;
    }
}
