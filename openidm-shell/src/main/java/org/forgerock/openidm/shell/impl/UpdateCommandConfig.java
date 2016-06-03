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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openidm.shell.impl;

/**
 * Value bean to hold the provided command line input parameters, and config data, provided to the update command.
 */
public class UpdateCommandConfig {
    private String updateArchive;
    private long maxJobsFinishWaitTimeMs = -1;
    private long maxUpdateWaitTimeMs = 30000;
    private boolean acceptedLicense = false;
    private boolean skipRepoUpdatePreview = false;
    private String logFilePath = "logs/update.log";
    private boolean quietMode = false;
    private long checkCompleteFrequency = 5000L;
    private long checkJobsRunningFrequency = 1000L;

    /**
     * Returns the name of the update archive expected to be in the bin/update directory.
     *
     * @return the name of the update archive expected to be in the bin/update directory.
     */
    public String getUpdateArchive() {
        return updateArchive;
    }

    /**
     * Sets the name of the update archive expected to be in the bin/update directory.
     *
     * @param updateArchive the name of the update archive expected to be in the bin/update directory.
     * @return this config instance
     */
    public UpdateCommandConfig setUpdateArchive(String updateArchive) {
        this.updateArchive = updateArchive;
        return this;
    }

    /**
     * Returns the Maximum time the update command should wait for all jobs to complete in the scheduler.
     *
     * @return the Maximum time the update command should wait for all jobs to complete in the scheduler.
     */
    public long getMaxJobsFinishWaitTimeMs() {
        return maxJobsFinishWaitTimeMs;
    }

    /**
     * Sets the Maximum time the update command should wait for all jobs to complete in the scheduler.
     *
     * @param maxJobsFinishWaitTimeMs the Maximum time the update command should wait for all jobs to complete in the
     * scheduler.
     * @return this config instance
     */
    public UpdateCommandConfig setMaxJobsFinishWaitTimeMs(long maxJobsFinishWaitTimeMs) {
        this.maxJobsFinishWaitTimeMs = maxJobsFinishWaitTimeMs;
        return this;
    }

    /**
     * Returns the Maximum time the update command should wait for the installation of the archive to take.
     *
     * @return the Maximum time the update command should wait for the installation of the archive to take.
     */
    public long getMaxUpdateWaitTimeMs() {
        return maxUpdateWaitTimeMs;
    }

    /**
     * Sets the Maximum time the update command should wait for the installation of the archive to take.
     *
     * @param maxUpdateWaitTimeMs the Maximum time the update command should wait for the installation of the archive
     * to take.
     * @return this config instance
     */
    public UpdateCommandConfig setMaxUpdateWaitTimeMs(long maxUpdateWaitTimeMs) {
        this.maxUpdateWaitTimeMs = maxUpdateWaitTimeMs;
        return this;
    }

    /**
     * Returns true if the license was accepted via the input parameters of the command line.
     *
     * @return true if the license was accepted via the input parameters of the command line.
     */
    public boolean isAcceptedLicense() {
        return acceptedLicense;
    }

    /**
     * Sets if the license was accepted via the input parameters of the command line. If true the process will
     * skip requesting the license content and asking for keyboard input to accept the license.
     *
     * @param acceptedLicense true if the license is accepted via the input parameters of the command line.
     * @return this config instance
     */
    public UpdateCommandConfig setAcceptedLicense(boolean acceptedLicense) {
        this.acceptedLicense = acceptedLicense;
        return this;
    }

    /**
     * Returns true if a preview of repository updates was bypassed via the input parameters of the command line.
     *
     * @return true if a preview of repository updates was bypassed via the input parameters of the command line.
     */
    public boolean isSkipRepoUpdatePreview() {
        return skipRepoUpdatePreview;
    }

    /**
     * Sets if a preview of repository updates was bypassed via the input parameters of the command line.
     * Suitable if user has already downloaded and approved changes to his/her repository.
     * If true the process will skip a preview and storing of repository updates.
     *
     * @param skipRepoUpdatePreview true if a preview of repository updates was bypassed via the input parameters
     *                              of the command line.
     * @return this config instance
     */
    public UpdateCommandConfig setSkipRepoUpdatePreview(boolean skipRepoUpdatePreview) {
        this.skipRepoUpdatePreview = skipRepoUpdatePreview;
        return this;
    }

    /**
     * Returns the file path to the log file that will be appended to with the update log information.
     *
     * @return the file path to the log file that will be appended to with the update log information.
     */
    public String getLogFilePath() {
        return logFilePath;
    }

    /**
     * Sets the file path to the log file that will be appended to with the update log information.
     *
     * @param logFilePath the file path to the log file that will be appended to with the update log information.
     * @return this config instance
     */
    public UpdateCommandConfig setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
        return this;
    }

    /**
     * Returns if the command output is quite; ie the log output is only written to the file and not to the console.
     *
     * @return if the command output is quite.
     */
    public boolean isQuietMode() {
        return quietMode;
    }

    /**
     * Sets if the command output should be quite; ie the log output is only written to the file and not to the console.
     *
     * @param quietMode if the command output should be quite; ie the log output is only written to the file and not
     * to the console.
     * @return this config instance
     */
    public UpdateCommandConfig setQuietMode(boolean quietMode) {
        this.quietMode = quietMode;
        return this;
    }

    /**
     * Returns the time between checks for a finished status of an install call.
     *
     * @return the time between checks for a finished status of an install call.
     */
    public long getCheckCompleteFrequency() {
        return checkCompleteFrequency;
    }

    /**
     * Set the time between checks for a finished status of an install call.
     *
     * @param checkCompleteFrequency the time between checks for a finished status of an install call.
     * @return this config instance
     */
    public UpdateCommandConfig setCheckCompleteFrequency(long checkCompleteFrequency) {
        this.checkCompleteFrequency = checkCompleteFrequency;
        return this;
    }

    /**
     * Returns the time between calls to lookup the current list of running jobs.
     *
     * @return the time between calls to lookup the current list of running jobs.
     */
    public long getCheckJobsRunningFrequency() {
        return checkJobsRunningFrequency;
    }

    /**
     * Sets the time between calls to lookup the current list of running jobs.
     *
     * @param checkJobsRunningFrequency the time between calls to lookup the current list of running jobs.
     * @return this config instance
     */
    public UpdateCommandConfig setCheckJobsRunningFrequency(long checkJobsRunningFrequency) {
        this.checkJobsRunningFrequency = checkJobsRunningFrequency;
        return this;
    }
}
