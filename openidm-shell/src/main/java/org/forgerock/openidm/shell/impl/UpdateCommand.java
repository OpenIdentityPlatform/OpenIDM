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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import org.apache.felix.service.command.CommandSession;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;

/**
 * Invoked by the command line interface, this command encapsulates all the logic and steps to perform an update to a
 * running OpenIDM system.
 */
public class UpdateCommand {
    static final String SCHEDULER_ROUTE = "scheduler";
    static final String SCHEDULER_ACTION_RESUME_JOBS = "resumeJobs";
    static final String SCHEDULER_ACTION_LIST_JOBS = "listCurrentlyExecutingJobs";
    static final String SCHEDULER_ACTION_PAUSE = "pauseJobs";
    static final String MAINTENANCE_ROUTE = "maintenance";
    static final String MAINTENANCE_ACTION_DISABLE = "disable";
    static final String MAINTENANCE_ACTION_ENABLE = "enable";
    static final String UPDATE_ROUTE = "maintenance/update";
    static final String UPDATE_LOG_ROUTE = UPDATE_ROUTE + "/log";
    static final String UPDATE_ACTION_AVAIL = "available";
    static final String UPDATE_ACTION_GET_LICENSE = "getLicense";
    static final String UPDATE_PARAM_ARCHIVE = "archive";
    static final String UPDATE_ACTION_UPDATE = "update";
    static final String UPDATE_STATUS_FAILED = "FAILED";
    static final String UPDATE_STATUS_COMPLETE = "COMPLETE";

    enum Status {
        READY,
        IN_PROGRESS,
        COMPLETE,
        FAILED,
        ERROR
    }

    private final long maxJobsFinishWaitTimeMs;
    private final long maxUpdateWaitTimeMs;
    private final HttpRemoteJsonResource resource;
    private final String updateArchive;
    private final boolean acceptedLicense;
    private long checkCompleteFrequency = 5000L;
    private long checkJobsRunningFrequency = 1000L;
    private String logFilePath;
    private boolean quietMode;
    private UpdateStep updateStep;
    private UpdateStep failedStep;
    private CommandSession session;
    private Status status;
    private PrintWriter logger;

    public enum UpdateStep {
        PREVIEW_ARCHIVE,
        ACCEPT_LICENSE,
        PAUSING_SCHEDULER,
        WAIT_FOR_JOBS_TO_COMPLETE,
        ENTER_MAINTENANCE_MODE,
        INSTALL_ARCHIVE,
        WAIT_FOR_INSTALL_DONE,
        EXIT_MAINTENANCE_MODE,
        ENABLE_SCHEDULER
    }

    /**
     * Constructor to create updateCommand, utilized to update an OpenIDM system.
     *  @param session command session.
     * @param resource utilized to invoke rest calls to openidm.
     * @param updateArchive the update filename.
     * @param maxJobsFinishWaitTimeMs max time to wait for jobs to complete running. -1 to exit immediately if jobs are
     * running.
     * @param maxUpdateWaitTimeMs max time to wait for update installation to complete.
     * @param logFilePath path to the log file to send output to.
     */
    public UpdateCommand(CommandSession session, HttpRemoteJsonResource resource, String updateArchive,
            long maxJobsFinishWaitTimeMs, long maxUpdateWaitTimeMs, boolean acceptedLicense, String logFilePath,
            boolean quietMode) {
        this.session = session;
        this.resource = resource;
        this.maxJobsFinishWaitTimeMs = maxJobsFinishWaitTimeMs;
        this.updateArchive = updateArchive;
        this.maxUpdateWaitTimeMs = maxUpdateWaitTimeMs;
        this.acceptedLicense = acceptedLicense;
        this.logFilePath = logFilePath;
        this.quietMode = quietMode;
        this.updateStep = null;
        this.failedStep = null;
        this.status = Status.READY;
    }

    /**
     * Executes an update to an OpenIDM installation.
     * <br/>
     * Steps include:
     * <ol>
     * <li>Lookup the archive data</li>
     * <li>Accept License</li>
     * <li>Pause Scheduler</li>
     * <li>waiting for jobs to complete</li>
     * <li>Enter maintenance mode.</li>
     * <li>Install Archive.</li>
     * <li>Exit Maintenance mode if needed.</li>
     * <li>Restart Scheduler if needed.</li>
     * </ol>
     */
    public void execute() {
        boolean archiveWillRestart = false;
        Context context = new RootContext();
        try {
            openLogger();

            log("Update process has begun.");
            status = Status.IN_PROGRESS;
            // Lookup the archive data - exit if the archive isn't found.
            JsonValue archiveData = getArchiveData(context);
            if (null == archiveData) {
                //this means the archive wasn't found.
                failedStep = updateStep;
                return;
            }
            // Display License for archive if not already accepted by command input.
            if (!acceptedLicense && !askToAcceptLicense(context)) {
                failedStep = updateStep;
                return;
            }
            // Pause the scheduler - exit if failed.
            if (!pauseScheduler(context)) {
                failedStep = updateStep;
                return;
            }
            // Wait for jobs to finish - exit if failed.
            if (!waitForRunningJobsToFinish(context)) {
                failedStep = updateStep;
                return;
            }
            // Enter maintenance mode - exit if failed.
            if (!enterMaintenanceMode(context)) {
                failedStep = updateStep;
                return;
            }
            // Discover if Archive needs a restart,
            // if it does then we don't need to exit maintenance mode or start jobs, as the install process will do it.
            archiveWillRestart = willArchiveRestart(archiveData);
            // Install Archive.
            installUpdateArchive(context);

        } catch (Exception e) {
            log("ERROR: There was an issue updating.  Last update state was " + updateStep, e);
            log("You should check the status of OpenIDM to ensure the system isn't corrupted.");
            failedStep = updateStep;
            status = Status.ERROR;
        } finally {
            try {
                if (null != failedStep) {
                    status = Status.FAILED;
                    if (UpdateStep.WAIT_FOR_INSTALL_DONE.equals(failedStep)) {
                        log("There was an error while checking the status of the update process while it was in " +
                                "progress. THE UPDATE PROCESS MIGHT STILL BE RUNNING.");
                    }
                }

                // The install process will restart the scheduler and exit maintenance mode if the archive requires
                // a restart.  If the archive does not require a restart, then we need to restart the scheduler
                // and exit maintenance mode here.
                if (!archiveWillRestart) {
                    recover(context);
                }
            } catch (Exception e) {
                log("There was an error while trying to recover the system.", e);
            } finally {
                if (null != logger) {
                    logger.flush();
                    logger.close();
                }
            }
        }
    }

    /**
     * Opens the logger for appending, if the file has been defined.
     */
    private void openLogger() {
        if (null != logFilePath) {
            File logFile = new File(logFilePath);
            try {
                logFile.getParentFile().mkdirs();
                logger = new PrintWriter(new FileWriter(logFile, true));
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to write to log file at " + logFile.getAbsolutePath(), e);
            }
        }
    }

    private boolean willArchiveRestart(JsonValue archiveData) {
        return "true".equals(archiveData.get("restartRequired").asString());
    }

    /**
     * Returns the meta-data associated with the found update archive.
     *
     * @param context context of the update.
     * @return the data describing the update archive, null if not found.
     * @throws ResourceException
     */
    private JsonValue getArchiveData(Context context) throws ResourceException {
        updateStep = UpdateStep.PREVIEW_ARCHIVE;
        ActionRequest request = Requests.newActionRequest(UPDATE_ROUTE, UPDATE_ACTION_AVAIL);
        ActionResponse response = resource.action(context, request);
        for (JsonValue archiveData : response.getJsonContent()) {
            if (updateArchive.equals(archiveData.get("archive").asString())) {
                return archiveData;
            }
        }
        log("Archive was not found in the bin/update directory. Requested name was = " + updateArchive);
        return null;
    }

    /**
     * This will request a y|n from the console input after retrieving and displaying the license for the archive.
     *
     * @param context context of the update execution.
     * @return true only of the license wasn't found, or the user accepted the license.
     * @throws ResourceException
     */
    private boolean askToAcceptLicense(Context context) throws ResourceException {
        updateStep = UpdateStep.ACCEPT_LICENSE;
        // Request the license content
        ActionRequest request = Requests.newActionRequest(UPDATE_ROUTE, UPDATE_ACTION_GET_LICENSE)
                .setAdditionalParameter(UPDATE_PARAM_ARCHIVE, updateArchive);
        ActionResponse response = resource.action(context, request);
        // If the content is found ask to accept.
        String license = response.getJsonContent().get("license").asString();
        if (null == license) {
            log("No license found to accept in this update archive.");
            return true;
        }
        log("-------------BEGIN LICENSE----------------------------------------------------");
        log(license);
        log("-------------END LICENSE------------------------------------------------------");
        log("Do you accept these license terms? (y|n) ");
        Scanner input = new Scanner(session.getKeyboard());
        while (input.hasNext()) {
            String next = input.next();
            if ("y".equals(next)) {
                return true;
            } else if ("n".equals(next)) {
                break;
            }
        }
        log("License was NOT accepted.");
        return false;

    }

    /**
     * This will attempt to exit maintenance mode and attempt to restart the scheduler.
     *
     * @param context Context of the update execution.
     */
    private void recover(Context context) {
        try {
            exitMaintenanceMode(context);
        } catch (ResourceException e) {
            log("Trouble attempting to exit maintenance mode.  Please check the state of OpenIDM.", e);
        } finally {
            //regardless of what has happened before, try to resume the scheduler.
            try {
                ActionRequest request = Requests.newActionRequest(SCHEDULER_ROUTE, SCHEDULER_ACTION_RESUME_JOBS);
                ActionResponse response = resource.action(context, request);
                if (response.getJsonContent().get("success").asBoolean()) {
                    log("Scheduler has been resumed.");
                }
            } catch (ResourceException e) {
                log("Trouble attempting to resume scheduled jobs.  Please check that the scheduler is resumed.", e);
            }
        }
    }

    /**
     * Invokes the update process with the archive that is expected to be in the update folder.
     *
     * @param context Context of the update.
     */
    private void installUpdateArchive(Context context) throws ResourceException {
        updateStep = UpdateStep.INSTALL_ARCHIVE;
        // Invoke the installation process.
        ActionRequest request = Requests.newActionRequest(UPDATE_ROUTE, UPDATE_ACTION_UPDATE)
                .setAdditionalParameter(UPDATE_PARAM_ARCHIVE, updateArchive);
        ActionResponse response = resource.action(context, request);
        // Read response from initial call.
        JsonValue jsonContent = response.getJsonContent();
        // Query for and Wait for completed status from the process.
        watchUpdateUntilComplete(
                jsonContent.get("status").asString().toUpperCase(),
                jsonContent.get(ResourceResponse.FIELD_CONTENT_ID).asString(),
                System.currentTimeMillis(),
                context);
    }

    /**
     * This recursively checks, until timeout, the status of the update for a finished state.
     *
     * @param status Status of the update process at last check.
     * @param updateId The id assigned to the invoked update status.
     * @param startUpdateTime The time the update process started.
     * @param context The context for this update.
     * @throws ResourceException
     */
    private void watchUpdateUntilComplete(String status, final String updateId, final long startUpdateTime,
            final Context context) throws ResourceException {
        updateStep = UpdateStep.WAIT_FOR_INSTALL_DONE;
        boolean timeout = false;
        while (!timeout && !UPDATE_STATUS_COMPLETE.equals(status) && !UPDATE_STATUS_FAILED.equals(status)) {
            log("Update procedure is still processing...");
            // Wait for the installation process to make some progress.
            try {
                Thread.sleep(checkCompleteFrequency);
            } catch (InterruptedException e) {
                //ignore interruption and just check status.
            }
            // Query the status of the installation process.
            ReadRequest request = Requests.newReadRequest(UPDATE_LOG_ROUTE, updateId);
            ResourceResponse response = resource.read(context, request);
            timeout = System.currentTimeMillis() - startUpdateTime > maxUpdateWaitTimeMs;
            status = response.getContent().get("status").asString().toUpperCase();
        }
        if (UPDATE_STATUS_COMPLETE.equals(status) || UPDATE_STATUS_FAILED.equals(status)) {
            log("The update process is complete with a status " + status);
            this.status = UPDATE_STATUS_COMPLETE.equals(status) ? Status.COMPLETE : Status.FAILED;
        } else {
            log("The update process failed to complete within the allotted time.  Please verify the state of OpenIDM.");
            failedStep = updateStep;
            this.status = Status.FAILED;
        }
    }

    private boolean exitMaintenanceMode(Context context) throws ResourceException {
        updateStep = UpdateStep.EXIT_MAINTENANCE_MODE;
        log("Exiting maintenance mode...");
        // Make the call to exit maintenance mode.
        ActionRequest request = Requests.newActionRequest(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE);
        ActionResponse response = resource.action(context, request);
        // Test that we are no longer in maintenance mode.
        boolean isEnabled = response.getJsonContent().get("maintenanceEnabled").asBoolean();
        if (isEnabled) {
            log("Failed to exit maintenance mode. Exiting update process.");
            return false;
        } else {
            log("No longer in maintenance mode.");
            return true;
        }
    }

    private boolean enterMaintenanceMode(Context context) throws ResourceException {
        updateStep = UpdateStep.ENTER_MAINTENANCE_MODE;
        log("Entering into maintenance mode...");
        // Make the call to enter maintenance mode.
        ActionRequest request = Requests.newActionRequest(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_ENABLE);
        ActionResponse response = resource.action(context, request);
        // Test that we are now in maintenance mode.
        boolean isEnabled = response.getJsonContent().get("maintenanceEnabled").asBoolean();
        if (isEnabled) {
            log("Now in maintenance mode.");
            return true;
        } else {
            log("Failed to enter maintenance mode. Exiting update process.");
            return false;
        }
    }

    /**
     * Repeatably calls isJobRunning, until timeout, checking or all jobs to complete.
     *
     * @param context The update context.
     * @return True if all jobs have stopped.
     * @throws ResourceException
     */
    private boolean waitForRunningJobsToFinish(Context context) throws ResourceException {
        updateStep = UpdateStep.WAIT_FOR_JOBS_TO_COMPLETE;
        long start = System.currentTimeMillis();
        boolean jobRunning;
        boolean timeout = false;
        log("Waiting for running jobs to finish.");
        do {
            jobRunning = isJobRunning(context);
            if (jobRunning) {
                if (maxJobsFinishWaitTimeMs < 0) {
                    log("Jobs are still running, exiting update process.");
                    return false;
                }
                try {
                    log("Waiting for jobs to finish...");
                    Thread.sleep(checkJobsRunningFrequency);
                } catch (InterruptedException e) {
                    log("WARNING: Got interrupted while waiting for jobs to finish, exiting update process.");
                    return false;
                }
                timeout = System.currentTimeMillis() - start > maxJobsFinishWaitTimeMs;
            }
        } while (jobRunning && !timeout);

        if (jobRunning) {
            log("Running jobs did not finish within the allotted wait time of " + maxJobsFinishWaitTimeMs + "ms.");
            return false;
        } else {
            log("All running jobs have finished.");
            return true;
        }
    }

    /**
     * Gets the list of running jobs and returns true if the count of jobs is > 0.
     *
     * @param context Context of the update.
     * @return true if no jobs are running.
     * @throws ResourceException
     */
    private boolean isJobRunning(Context context) throws ResourceException {
        ActionRequest request = Requests.newActionRequest(SCHEDULER_ROUTE, SCHEDULER_ACTION_LIST_JOBS);
        ActionResponse response = resource.action(context, request);
        //return true if more than 1 job is running.
        return response.getJsonContent().asList().size() > 0;
    }

    /**
     * Pauses the IDM job scheduler.
     *
     * @param context the context of this update.
     * @return True if the scheduler is reported as stopped.
     * @throws ResourceException
     */
    private boolean pauseScheduler(Context context) throws ResourceException {
        updateStep = UpdateStep.PAUSING_SCHEDULER;
        log("Pausing the Scheduler");
        ActionRequest request = Requests.newActionRequest(SCHEDULER_ROUTE, SCHEDULER_ACTION_PAUSE);
        ActionResponse response = resource.action(context, request);
        // Test if the pause request was successful.
        if (response.getJsonContent().get("success").asBoolean()) {
            log("Scheduler has been paused.");
            return true;
        } else {
            log("Scheduler could not be paused. Exiting update process.");
            return false;
        }
    }

    /**
     * Set the frequency for which the install step will check the status of the
     * install.
     *
     * @param checkCompleteFrequency time in milliseconds to repeat the status check.
     */
    public void setCheckCompleteFrequency(long checkCompleteFrequency) {
        this.checkCompleteFrequency = checkCompleteFrequency;
    }

    /**
     * Set the frequency for which to check if all jobs have finished running.
     *
     * @param checkJobsRunningFrequency time in milliseconds to repeat the listJobs call.
     */
    public void setCheckJobsRunningFrequency(long checkJobsRunningFrequency) {
        this.checkJobsRunningFrequency = checkJobsRunningFrequency;
    }

    /**
     * After execution is complete and had an error, call this to discover the step for which
     * the update process failed on.
     *
     * @return Last step the process was on before the error.
     */
    public UpdateStep getFailedStep() {
        return failedStep;
    }

    /**
     * Returns the status of the update process; used to determine if the update failed.
     *
     * @return the status of the update process.
     */
    public Status getStatus() {
        return status;
    }

    private void log(String message) {
        if (!quietMode) {
            session.getConsole().println(message);
        }
        if (null != logger) {
            logger.println(SimpleDateFormat.getDateTimeInstance().format(new Date()) + ": " + message);
        }
    }

    private void log(String message, Throwable throwable) {
        if (!quietMode) {
            throwable.printStackTrace(session.getConsole());
            log(message);
        }
        if (null != logger) {
            throwable.printStackTrace(logger);
        }
    }

}
