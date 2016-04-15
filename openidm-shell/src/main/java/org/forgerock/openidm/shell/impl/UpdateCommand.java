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

import static org.forgerock.openidm.shell.impl.UpdateCommand.UpdateStep.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import static java.util.Arrays.asList;

import org.apache.felix.service.command.CommandSession;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.services.context.Context;

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
    static final String UPDATE_ACTION_LIST_REPO_UPDATES = "listRepoUpdates";
    static final String UPDATE_ACTION_MARK_COMPLETE = "markComplete";
    static final String UPDATE_PARAM_ARCHIVE = "archive";
    static final String UPDATE_PARAM_ARCHIVES = "archives";
    static final String UPDATE_PARAM_UPDATE_ID = "updateId";
    static final String UPDATE_ACTION_UPDATE = "update";
    static final String UPDATE_ACTION_RESTART = "restart";
    static final String UPDATE_STATUS_FAILED = "FAILED";
    static final String UPDATE_STATUS_COMPLETE = "COMPLETE";
    static final String UPDATE_STATUS_PENDING_REPO_UPDATES = "PENDING_REPO_UPDATES";
    static final String UPDATE_STATUS_IN_PROGRESS = "IN_PROGRESS";
    static final List<String> TERMINAL_STATE = asList(UPDATE_STATUS_COMPLETE, UPDATE_STATUS_FAILED,
            UPDATE_STATUS_PENDING_REPO_UPDATES);

    private final CommandSession session;
    private final HttpRemoteJsonResource resource;
    private final UpdateCommandConfig config;
    private final Map<UpdateStep, StepExecutor> executorRegistry = new HashMap<>();
    private PrintWriter logger;
    private UpdateStep[] executeSequence;
    private UpdateStep[] recoverySequence;

    /**
     * All steps associated with the update installation process.
     */
    enum UpdateStep {
        PREVIEW_ARCHIVE,
        ACCEPT_LICENSE,
        LIST_REPO_UPDATES,
        PAUSING_SCHEDULER,
        WAIT_FOR_JOBS_TO_COMPLETE,
        ENTER_MAINTENANCE_MODE,
        INSTALL_ARCHIVE,
        WAIT_FOR_INSTALL_DONE,
        MARK_REPO_UPDATES_COMPLETE,
        EXIT_MAINTENANCE_MODE,
        ENABLE_SCHEDULER,
        FORCE_RESTART
    }

    /**
     * Status execute() will return.
     */
    enum ExecutorStatus {
        /**
         * Completion
         */
        SUCCESS,
        /**
         * An error in the executor
         */
        FAIL,
        /**
         * A non-error state that is an early abortion of the normal update process
         */
        ABORT
    }

    /**
     * Constructs the UpdateCommand with the default StepExecutors for the Update OpenIDM process.
     * <br/>
     * Default Steps include:
     * <ol>
     * <li>Lookup the archive data</li>
     * <li>Accept License</li>
     * <li>Pause Scheduler</li>
     * <li>waiting for jobs to complete</li>
     * <li>Enter maintenance mode.</li>
     * <li>Install Archive.</li>
     * <li>Wait for the install to complete.</li>
     * </ol>
     * Recovery Steps include:
     * <ol>
     * <li>Exit Maintenance mode if needed.</li>
     * <li>Restart Scheduler if needed.</li>
     * <li>Force Restart if needed.</li>
     * </ol>
     *
     * Engineering note: To add additional steps:
     * <ol>
     *     <li>Add the step enum to UpdateStep</li>
     *     <li>Implement a new StepExecutor</li>
     *     <li>register the StepExecutor</li>
     *     <li>insert into the execute sequence or recovery sequence.</li>
     * </ol>
     *
     * @param session the command line session to possibly log output to, or to get keyboard input.
     * @param resource the resource provider to execute REST calls to OpenIDM.
     * @param config the configuration provided by the command line parameters.
     */
    public UpdateCommand(CommandSession session, HttpRemoteJsonResource resource, UpdateCommandConfig config) {
        this.session = session;
        this.resource = resource;
        this.config = config;

        // Register the update steps.
        registerStepExecutor(new GetArchiveDataStepExecutor());
        registerStepExecutor(new AcceptLicenseStepExecutor());
        registerStepExecutor(new ListRepoUpdatesStepExecutor());
        registerStepExecutor(new PauseSchedulerStepExecutor());
        registerStepExecutor(new WaitForJobsStepExecutor());
        registerStepExecutor(new EnterMaintenanceModeStepExecutor());
        registerStepExecutor(new InstallArchiveStepExecutor());
        registerStepExecutor(new WaitForInstallDoneStepExecutor());
        registerStepExecutor(new MarkRepoUpdatesCompleteExecutor());
        registerStepExecutor(new ExitMaintenanceModeStepExecutor());
        registerStepExecutor(new EnableSchedulerStepExecutor());
        registerStepExecutor(new ForceRestartStepExecutor());

        // Set the sequence of execution for the steps.
        setExecuteSequence(PREVIEW_ARCHIVE,
                           ACCEPT_LICENSE,
                           LIST_REPO_UPDATES,
                           PAUSING_SCHEDULER,
                           WAIT_FOR_JOBS_TO_COMPLETE,
                           ENTER_MAINTENANCE_MODE,
                           INSTALL_ARCHIVE,
                           WAIT_FOR_INSTALL_DONE,
                           MARK_REPO_UPDATES_COMPLETE);
        // Set the sequence of execution of the recovery steps.
        setRecoverySequence(EXIT_MAINTENANCE_MODE,
                            ENABLE_SCHEDULER,
                            FORCE_RESTART);
    }

    /**
     * Registers a executor for the step it satisfies.
     *
     * @param stepExecutor the executor to register.
     */
    public void registerStepExecutor(StepExecutor stepExecutor) {
        executorRegistry.put(stepExecutor.getStep(), stepExecutor);
    }

    /**
     * Saves the sequence of steps to execute.
     *
     * @param executeSequence the array of steps to complete in the order provided.
     */
    public void setExecuteSequence(UpdateStep... executeSequence) {
        this.executeSequence = executeSequence;
    }

    /**
     * Saves the sequence of steps to execute once the command has entered recovery mode.
     *
     * @param recoverySequence the array of steps to complete in the order provided.
     */
    public void setRecoverySequence(UpdateStep... recoverySequence) {
        this.recoverySequence = recoverySequence;
    }

    /**
     * For each step in the executeSequence, this executes each registered executor as long as the condition of
     * the executor is ok to run.  When all are complete, or one fails, or one aborts, this will then similarly
     * loop through each step in the recoverySequence and execute its registered executor.
     *
     * @param context The context to pass to the REST calls.
     * @return the value bean holding the results of the execution run.
     */
    public UpdateExecutionState execute(Context context) {
        UpdateExecutionState executionResults = new UpdateExecutionState();
        openLogger();

        try {
            for (UpdateStep nextStep : executeSequence) {
                StepExecutor executor = executorRegistry.get(nextStep);
                if (null != executor && executor.onCondition(executionResults)) {
                    executionResults.setLastAttemptedStep(nextStep);
                    ExecutorStatus status = executor.execute(context, executionResults);
                    if (status.equals(ExecutorStatus.ABORT)) {
                        return executionResults;
                    } else if (status.equals(ExecutorStatus.FAIL)) {
                        log("ERROR: Error during execution. The state of OpenIDM is now unknown. " +
                                "Last Attempted step was " + executionResults.getLastAttemptedStep() +
                                ". Now attempting recovery steps.");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log("ERROR: Error during execution. Last Attempted step was " + executionResults.getLastAttemptedStep() +
                    ". Will now attempt recovery steps.", e);
        } finally {
            for (UpdateStep nextStep : recoverySequence) {
                try {
                    StepExecutor executor = executorRegistry.get(nextStep);
                    if (null != executor && executor.onCondition(executionResults)) {
                        executionResults.setLastRecoveryStep(nextStep);
                        ExecutorStatus status = executor.execute(context, executionResults);
                        if (status.equals(ExecutorStatus.ABORT)) {
                            return executionResults;
                        } else if (status.equals(ExecutorStatus.FAIL)) {
                            log("WARN: Failed a recovery step " + nextStep + ", continuing on with recovery.");
                        }
                    }
                } catch (Exception e) {
                    log("ERROR: Error in recovery step " + nextStep + ", continuing on with recovery.", e);
                }
            }
        }

        closeLogger();

        return executionResults;
    }

    private void openLogger() {
        String logFilePath = config.getLogFilePath();
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

    private void closeLogger() {
        if (null != logger) {
            logger.close();
        }
    }

    private void log(String message) {
        if (!config.isQuietMode()) {
            session.getConsole().println(message);
        }
        if (null != logger) {
            logger.println(SimpleDateFormat.getDateTimeInstance().format(new Date()) + ": " + message);
            logger.flush();
        }
    }

    private void log(String message, Throwable throwable) {
        if (!config.isQuietMode()) {
            throwable.printStackTrace(session.getConsole());
            log(message);
        }
        if (null != logger) {
            throwable.printStackTrace(logger);
            logger.flush();
        }
    }

    /**
     * Evaluates if the archive requires a restart.  As a default, if the archive data is null or if the setting
     * "restartRequired" is missing from the archive data, then this will return false. The origin of the
     * restartRequired field is a read from a properties file, therefore the value is treated as a String.
     *
     * @param state current state of the execution.
     * @return true if the "restartRequired" is set to "true"
     */
    private static boolean isRestartRequired(UpdateExecutionState state) {
        JsonValue archiveData = state.getArchiveData();
        return (null != archiveData && archiveData.get("restartRequired").defaultTo(false).asBoolean());
    }

    /**
     * Defines the interface that each executor must implement.
     */
    private interface StepExecutor {
        /**
         * Implementors should return the step that this executor handles.
         *
         * @return the step that this executor handles.
         */
        UpdateStep getStep();

        /**
         * Handle the step based on the current state of the command execution.
         *
         * @param context context to be utilized for REST calls.
         * @param state the current state of the command execution.
         * @return ExecutorStatus.SUCCESS if the execution ran to a successful outcome.
         */
        ExecutorStatus execute(Context context, UpdateExecutionState state);

        /**
         * Implementors should return true if the conditions are appropriate for the executor to execute.
         *
         * @param state the current state of the command execution.
         * @return true if the conditions are appropriate for the executor to execute.
         */
        boolean onCondition(UpdateExecutionState state);
    }

    /**
     * Retrieves the Archive meta-data and stores it in the UpdateExecutionState bean.
     */
    private class GetArchiveDataStepExecutor implements StepExecutor {
        /**
         * {@inheritDoc}
         */
        @Override
        public UpdateStep getStep() {
            return PREVIEW_ARCHIVE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExecutorStatus execute(Context context, UpdateExecutionState state) {
            try {
                ActionResponse response = resource.action(context,
                        Requests.newActionRequest(UPDATE_ROUTE, UPDATE_ACTION_AVAIL));
                String updateArchive = config.getUpdateArchive();
                JsonValue responseData = response.getJsonContent();
                for (JsonValue archiveData : responseData.get("updates")) {
                    if (updateArchive.equals(archiveData.get("archive").asString())) {
                        state.setArchiveData(archiveData);
                        return ExecutorStatus.SUCCESS;
                    }
                }
                log("A valid archive was not found in the bin/update directory. Requested filename was = " +
                        updateArchive);
                JsonValue rejects = responseData.get("rejects");
                if (!rejects.asList().isEmpty()) {
                    log("Invalid archive(s) were found:");
                    for (JsonValue reject : rejects) {
                        log(reject.get("archive").asString() + ": " + reject.get("reason").asString() + " " +
                                (reject.get("error").isNull()
                                        ? "" :
                                        " error=" + reject.get("error").asString()));
                    }
                }
                return ExecutorStatus.FAIL;
            } catch (ResourceException e) {
                log("The attempt to lookup the archive meta-data failed.", e);
                return ExecutorStatus.FAIL;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onCondition(UpdateExecutionState state) {
            return true;
        }
    }

    /**
     * If the licence wasn't already accepted via the command line, then the execute will retrieve the license
     * from the archive.  If found, then the user will be prompted to accept the license terms.
     *
     * @see UpdateCommandConfig#isAcceptedLicense()
     */
    private class AcceptLicenseStepExecutor implements StepExecutor {
        /**
         * {@inheritDoc}
         */
        @Override
        public UpdateStep getStep() {
            return ACCEPT_LICENSE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExecutorStatus execute(Context context, UpdateExecutionState state) {
            try {
                // Request the license content
                ActionResponse response = resource.action(context,
                        Requests.newActionRequest(UPDATE_ROUTE, UPDATE_ACTION_GET_LICENSE)
                                .setAdditionalParameter(UPDATE_PARAM_ARCHIVE, config.getUpdateArchive()));
                // If the content is found ask to accept.
                String license = response.getJsonContent().get("license").asString();
                if (null == license) {
                    log("No license found to accept in this update archive.");
                    return ExecutorStatus.SUCCESS;
                }
                log("-------------BEGIN LICENSE----------------------------------------------------");
                log(license);
                log("-------------END LICENSE------------------------------------------------------");
                log("Do you accept these license terms? (y|n) ");
                Scanner input = new Scanner(session.getKeyboard());
                while (input.hasNext()) {
                    String next = input.next();
                    if ("y".equals(next)) {
                        return ExecutorStatus.SUCCESS;
                    } else if ("n".equals(next)) {
                        break;
                    }
                }
                log("License was NOT accepted.");
                return ExecutorStatus.FAIL;
            } catch (ResourceException e) {
                log("There was trouble retrieving the license agreement.", e);
                return ExecutorStatus.FAIL;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onCondition(UpdateExecutionState state) {
            if (config.isAcceptedLicense()) {
                log("License was accepted via command line argument.");
                return false;
            }
            return true;
        }
    }

    /**
     * This pauses the scheduler.
     */
    private class PauseSchedulerStepExecutor implements StepExecutor {
        /**
         * {@inheritDoc}
         */
        @Override
        public UpdateStep getStep() {
            return PAUSING_SCHEDULER;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExecutorStatus execute(Context context, UpdateExecutionState state) {
            try {
                log("Pausing the Scheduler");
                ActionResponse response = resource.action(context,
                        Requests.newActionRequest(SCHEDULER_ROUTE, SCHEDULER_ACTION_PAUSE));
                // Test if the pause request was successful.
                if (response.getJsonContent().get("success").defaultTo(false).asBoolean()) {
                    log("Scheduler has been paused.");
                    return ExecutorStatus.SUCCESS;
                } else {
                    log("Scheduler could not be paused. Exiting update process.");
                    return ExecutorStatus.FAIL;
                }
            } catch (ResourceException e) {
                log("Error encountered while pausing the job scheduler.", e);
                return ExecutorStatus.FAIL;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onCondition(UpdateExecutionState state) {
            return true;
        }
    }

    /**
     * This repeatably calls to get the list of running jobs until all jobs have finished.
     *
     * @see UpdateCommandConfig#getMaxJobsFinishWaitTimeMs()
     * @see UpdateCommandConfig#getCheckJobsRunningFrequency()
     */
    private class WaitForJobsStepExecutor implements StepExecutor {
        /**
         * {@inheritDoc}
         */
        @Override
        public UpdateStep getStep() {
            return WAIT_FOR_JOBS_TO_COMPLETE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExecutorStatus execute(Context context, UpdateExecutionState state) {
            long start = System.currentTimeMillis();
            long maxWaitTime = config.getMaxJobsFinishWaitTimeMs();

            boolean jobRunning;
            boolean timeout = false;
            log("Waiting for running jobs to finish.");
            do {
                try {
                    jobRunning = isJobRunning(context);
                    if (jobRunning) {
                        if (maxWaitTime < 0) {
                            log("Jobs are still running, exiting update process.");
                            return ExecutorStatus.FAIL;
                        }
                        try {
                            log("Waiting for jobs to finish...");
                            Thread.sleep(config.getCheckJobsRunningFrequency());
                        } catch (InterruptedException e) {
                            log("WARNING: Got interrupted while waiting for jobs to finish, exiting update process.");
                            return ExecutorStatus.FAIL;
                        }
                        timeout = (System.currentTimeMillis() - start > maxWaitTime);
                    }
                } catch (ResourceException e) {
                    log("Error encountered while waiting for jobs to finish", e);
                    return ExecutorStatus.FAIL;
                }
            } while (jobRunning && !timeout);

            if (jobRunning) {
                log("Running jobs did not finish within the allotted wait time of " + maxWaitTime + "ms.");
                return ExecutorStatus.FAIL;
            } else {
                log("All running jobs have finished.");
                return ExecutorStatus.SUCCESS;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onCondition(UpdateExecutionState state) {
            return true;
        }

        /**
         * Gets the list of running jobs and returns true if the count of jobs is > 0.
         *
         * @param context Context of the update.
         * @return true if no jobs are running.
         * @throws ResourceException if the call to the router fails.
         */
        private boolean isJobRunning(Context context) throws ResourceException {
            ActionResponse response = resource.action(context,
                    Requests.newActionRequest(SCHEDULER_ROUTE, SCHEDULER_ACTION_LIST_JOBS));
            //return true if more than 1 job is running.
            return response.getJsonContent().asList().size() > 0;
        }
    }

    /**
     * Puts OpenIDM into maintenance mode.
     */
    private class EnterMaintenanceModeStepExecutor implements StepExecutor {
        /**
         * {@inheritDoc}
         */
        @Override
        public UpdateStep getStep() {
            return ENTER_MAINTENANCE_MODE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExecutorStatus execute(Context context, UpdateExecutionState state) {
            try {
                log("Entering into maintenance mode...");
                // Make the call to enter maintenance mode.
                ActionResponse response = resource.action(context,
                        Requests.newActionRequest(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_ENABLE));
                // Test that we are now in maintenance mode.
                if (response.getJsonContent().get("maintenanceEnabled").defaultTo(false).asBoolean()) {
                    log("Now in maintenance mode.");
                    return ExecutorStatus.SUCCESS;
                } else {
                    log("Failed to enter maintenance mode. Exiting update process.");
                    return ExecutorStatus.FAIL;
                }
            } catch (ResourceException e) {
                log("Error occurred while attempting to enter maintenance mode.", e);
                return ExecutorStatus.FAIL;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onCondition(UpdateExecutionState state) {
            return true;
        }
    }

    /**
     * Installs the update archive into OpenIDM.
     */
    private class InstallArchiveStepExecutor implements StepExecutor {
        /**
         * {@inheritDoc}
         */
        @Override
        public UpdateStep getStep() {
            return INSTALL_ARCHIVE;
        }

        /**
         * Invokes the update process with the archive that is expected to be in the update folder.
         *
         * @param context Context of the update.
         * @param state The current state of the execution sequence.
         */
        @Override
        public ExecutorStatus execute(Context context, UpdateExecutionState state) {
            try {
                log("Installing the update archive " + config.getUpdateArchive());
                // Invoke the installation process.
                ActionResponse response = resource.action(context,
                        Requests.newActionRequest(UPDATE_ROUTE, UPDATE_ACTION_UPDATE)
                                .setAdditionalParameter(UPDATE_PARAM_ARCHIVE, config.getUpdateArchive()));
                // Read response from install call.
                JsonValue installResponse = response.getJsonContent();
                if (installResponse.get("status").isNull()) {
                    return ExecutorStatus.FAIL;
                }
                state.setInstallResponse(installResponse);
                state.setStartInstallTime(System.currentTimeMillis());
                return ExecutorStatus.SUCCESS;
            } catch (ResourceException e) {
                log("Error encountered while installing the update archive.", e);
                return ExecutorStatus.FAIL;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onCondition(UpdateExecutionState state) {
            return true;
        }

    }

    /**
     * This will repeatably check the update installation status until it times out or returns a TERMINAL_STATE.
     *
     * @see UpdateCommandConfig#getMaxUpdateWaitTimeMs()
     * @see UpdateCommandConfig#getCheckCompleteFrequency()
     */
    private class WaitForInstallDoneStepExecutor implements StepExecutor {
        /**
         * {@inheritDoc}
         */
        @Override
        public UpdateStep getStep() {
            return WAIT_FOR_INSTALL_DONE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExecutorStatus execute(Context context, UpdateExecutionState state) {
            JsonValue installResponse = state.getInstallResponse();
            long startTime = state.getStartInstallTime();
            if (null == installResponse || startTime <= 0 || installResponse.get("status").isNull()) {
                throw new IllegalStateException(
                        "Install start time or Initial install status from install step is missing. Ensure the step " +
                                INSTALL_ARCHIVE + " was completed");
            }
            String status = installResponse.get("status").defaultTo(UPDATE_STATUS_IN_PROGRESS).asString().toUpperCase();
            String updateId = installResponse.get(ResourceResponse.FIELD_CONTENT_ID).asString();
            try {
                while (!TERMINAL_STATE.contains(status)) {
                    log("Update procedure is still processing...");
                    // Wait for the installation process to make some progress.
                    try {
                        Thread.sleep(config.getCheckCompleteFrequency());
                    } catch (InterruptedException e) {
                        //ignore interruption and just check status.
                    }
                    // Query the status of the installation process.
                    ResourceResponse response = resource.read(context,
                            Requests.newReadRequest(UPDATE_LOG_ROUTE, updateId));
                    status = response.getContent().get("status").defaultTo(UPDATE_STATUS_IN_PROGRESS)
                            .asString().toUpperCase();
                }
                if (TERMINAL_STATE.contains(status)) {
                    state.setCompletedInstallStatus(status);
                    log("The update process is complete with a status of " + status);
                    return ExecutorStatus.SUCCESS;
                } else {
                    log("The update process failed to complete within the allotted time.  " +
                            "Please verify the state of OpenIDM.");
                    return ExecutorStatus.FAIL;
                }
            } catch (ResourceException e) {
                log("Error encountered while checking status of install.  The update might still be in process", e);
                return ExecutorStatus.FAIL;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onCondition(UpdateExecutionState state) {
            return true;
        }
    }

    /**
     * If status is PENDING, then the user will be prompted to execute the repo updates on the DB.
     */
    private class MarkRepoUpdatesCompleteExecutor implements StepExecutor {
        /**
         * {@inheritDoc}
         */
        @Override
        public UpdateStep getStep() {
            return MARK_REPO_UPDATES_COMPLETE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExecutorStatus execute(Context context, UpdateExecutionState state) {
            try {
                // Skip if status is not PENDING.
                if (!state.getCompletedInstallStatus().equals(UPDATE_STATUS_PENDING_REPO_UPDATES)) {
                    return ExecutorStatus.SUCCESS;
                }

                // Ask user to perform repo updates.
                log("Run repository update scripts now, and then enter 'yes' to complete the OpenIDM update process.");
                // Forcing user to hit "yes".
                Scanner input = new Scanner(session.getKeyboard());
                while (input.hasNext()) {
                    String next = input.next();
                    if (next.equalsIgnoreCase("yes")) {
                        // Mark repo updates as complete for a PENDING_REPO_UPDATES update.
                        ActionResponse response = resource.action(context,
                                Requests.newActionRequest(UPDATE_ROUTE, UPDATE_ACTION_MARK_COMPLETE)
                                .setAdditionalParameter(UPDATE_PARAM_UPDATE_ID,
                                        state.getInstallResponse().get(ResourceResponse.FIELD_CONTENT_ID).asString()));
                        String status = response.getJsonContent().get("status").asString().toUpperCase();
                        if (status.equals(UPDATE_STATUS_COMPLETE)) {
                            log("Repo Updates status: " + UPDATE_STATUS_COMPLETE);
                            return ExecutorStatus.SUCCESS;
                        } else {
                            log("Unable to mark repository updates as complete. Status: " + status);
                            return ExecutorStatus.FAIL;
                        }
                    } else {
                        log("Run repository update scripts now, and then enter 'yes' to complete the OpenIDM update process.");
                    }
                }
                return ExecutorStatus.FAIL;
            } catch (ResourceException e) {
                log("Unable to mark repository updates as complete.", e);
                return ExecutorStatus.FAIL;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onCondition(UpdateExecutionState state) {
            return true;
        }
    }

    /**
     * If no restart required, the StepExecutor will attempt to exit maintenance mode.
     */
    private class ExitMaintenanceModeStepExecutor implements StepExecutor {
        /**
         * {@inheritDoc}
         */
        @Override
        public UpdateStep getStep() {
            return EXIT_MAINTENANCE_MODE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExecutorStatus execute(Context context, UpdateExecutionState state) {
            try {
                log("Exiting maintenance mode...");
                // Make the call to exit maintenance mode.
                ActionResponse response = resource.action(context,
                        Requests.newActionRequest(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE));
                // Test that we are no longer in maintenance mode.
                if (response.getJsonContent().get("maintenanceEnabled").defaultTo(false).asBoolean()) {
                    log("Failed to exit maintenance mode. Exiting update process.");
                    return ExecutorStatus.FAIL;
                } else {
                    log("No longer in maintenance mode.");
                    return ExecutorStatus.SUCCESS;
                }
            } catch (ResourceException e) {
                log("Error encountered while exiting maintenance mode.", e);
                return ExecutorStatus.FAIL;
            }
        }

        /**
         * {@inheritDoc}
         *
         * @return implemented to return true if the archive data is null or doesn't need to restart and therefore we
         * should exit maintenance mode and if the archive data is null.
         */
        public boolean onCondition(UpdateExecutionState state) {
            return !isRestartRequired(state);
        }
    }

    /**
     * Only if the archive didn't need a restart, then this will request OpenIDM to resume the scheduler.
     */
    private class EnableSchedulerStepExecutor implements StepExecutor {
        /**
         * {@inheritDoc}
         */
        @Override
        public UpdateStep getStep() {
            return ENABLE_SCHEDULER;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExecutorStatus execute(Context context, UpdateExecutionState state) {
            try {
                log("Resuming the job scheduler.");
                ActionResponse response = resource.action(context,
                        Requests.newActionRequest(SCHEDULER_ROUTE, SCHEDULER_ACTION_RESUME_JOBS));
                // Pull the success value from the response.
                if (response.getJsonContent().get("success").defaultTo(false).asBoolean()) {
                    log("Scheduler has been resumed.");
                    return ExecutorStatus.SUCCESS;
                } else {
                    log("WARN: A successful request was made to resume the scheduler, " +
                            "but it appears to not have restarted.");
                    return ExecutorStatus.FAIL;
                }
            } catch (ResourceException e) {
                log("Trouble attempting to resume scheduled jobs.  Please check that the scheduler is resumed.", e);
                return ExecutorStatus.FAIL;
            }
        }

        /**
         * {@inheritDoc}
         *
         * @return implemented to return true if the archive data is null or doesn't need to restart and therefore we
         * should exit maintenance mode and if the archive data is null.
         */
        public boolean onCondition(UpdateExecutionState state) {
            return !isRestartRequired(state);
        }
    }

    /**
     * Only if the archive DID need a restart do we request a forced immediate restart of OpenIDM.
     * Without this step, OpenIDM would wait 30 seconds before restarting on its own.
     */
    private class ForceRestartStepExecutor implements StepExecutor {
        /**
         * {@inheritDoc}
         */
        @Override
        public UpdateStep getStep() {
            return FORCE_RESTART;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExecutorStatus execute(Context context, UpdateExecutionState state) {
            try {
                log("Restarting OpenIDM.");
                // Invoke the restart.
                resource.action(context, Requests.newActionRequest(UPDATE_ROUTE, UPDATE_ACTION_RESTART));
                log("Restart request completed.");
                return ExecutorStatus.SUCCESS; //
            } catch (ResourceException e) {
                log("Error encountered while requesting the restart of OpenIDM.", e);
                return ExecutorStatus.FAIL;
            }
        }

        /**
         * {@inheritDoc}
         * If the archive data is null, then it means that the archive file wasn't found to install. No need to restart.
         *
         * @return implemented to return true if the archive data is null or does need a restart.
         */
        @Override
        public boolean onCondition(UpdateExecutionState state) {
            return isRestartRequired(state);
        }
    }

    /**
     * If the database repo update files are found, then the user will be prompted to specify a path to save
     * repo update files.
     * If the path does not exist, create one. Then save the repo update files in the path specified.
     *
     * @see UpdateCommandConfig#isSkipRepoUpdatePreview()
     */
    private class ListRepoUpdatesStepExecutor implements StepExecutor {
        /**
         * {@inheritDoc}
         */
        @Override
        public UpdateStep getStep() {
            return LIST_REPO_UPDATES;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExecutorStatus execute(Context context, UpdateExecutionState state) {
            try {
                // List repo update files present in archive.
                JsonValue responseData = fetchRepoUpdates(context);
                if (!responseData.asList().isEmpty()) {
                    log("Database repo update files present in archive were found:");
                    for (JsonValue repoUpdate: responseData) {
                        log(repoUpdate.get("file").asString());
                    }

                    // Prompt for path to save repo updates files.
                    // Check if that directory specified exists, if not create it.
                    log("Please enter the directory to save repository update files: ");
                    boolean isDirCreated = false;
                    Scanner input = new Scanner(session.getKeyboard());
                    String targetPath;
                    do {
                        targetPath = input.next();
                        File dir = new File(targetPath);
                        if (!dir.isDirectory()) {
                            if (dir.mkdirs()) {
                                isDirCreated = true;
                            }
                            else { // failed to create dir
                                log("Invalid directory. Please enter a valid directory: ");
                            }
                        } else { // dir already exists
                            isDirCreated = true;
                        }
                    } while (!isDirCreated);

                    // Fetch each repo update file and place it as a file in that dir.
                    for (JsonValue repoUpdate : responseData) {
                        // get file name and path
                        String file = repoUpdate.get("file").asString();
                        String path = repoUpdate.get("path").asString();
                        // get file content
                        ResourceResponse responseValue = resource.read(context,
                                Requests.newReadRequest(UPDATE_ROUTE + "/" + UPDATE_PARAM_ARCHIVES
                                        + "/" +  config.getUpdateArchive() + "/" +  path));
                        JsonValue fileContent = responseValue.getContent().get("contents");
                        // create a file in dir and write content in it
                        if (Files.exists(Paths.get(targetPath + "/" + file))) {
                            // ask if they want to overwrite it
                            log(targetPath + "/" + file + " already exists. Do you want to overwrite it? (y|n) ");
                            boolean isYOrN = false;
                            do {
                                String nextInput = input.next();
                                if ("y".equals(nextInput)) {
                                    try {
                                        writeToFile(Paths.get(targetPath + "/" + file), fileContent.asString());
                                    } catch (IOException ex) {
                                        log("There was trouble storing repo update file " + file +
                                                " to the path " + targetPath + ".", ex);
                                        return ExecutorStatus.FAIL;
                                    }
                                    log(file + " was overwritten.");
                                    isYOrN = true;
                                } else if ("n".equals(nextInput)) {
                                    log("Skipped overwriting " + file + ".");
                                    isYOrN = true;
                                } else {
                                    log("Invalid entry. Please reenter (y|n): ");
                                }
                            } while (!isYOrN);
                        } else {
                            try {
                                writeToFile(Paths.get(targetPath + "/" + file), fileContent.asString());
                            } catch (IOException e) {
                                log("There was trouble storing repo update file " + file +
                                        " to the path " + targetPath + ".", e);
                                return ExecutorStatus.FAIL;
                            }
                        }
                    }
                    log("Repository scripts are in the following directory: " + targetPath + ".\n" +
                            "Please come back and run the update again with --skipRepoUpdatePreview " +
                            "after updates have been reviewed.");
                    return ExecutorStatus.ABORT;
                }
                return ExecutorStatus.SUCCESS;
            } catch (ResourceException e) {
                log("Unable to retrieve repository scripts.", e);
                return ExecutorStatus.FAIL;
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onCondition(UpdateExecutionState state) {
            if (config.isSkipRepoUpdatePreview()) {
                log("Repository update preview was skipped.");
                return false;
            }
            return true;
        }
    }

    /**
     * Write content to file with the default charset of this Java virtual machine for encoding.
     * Overwrite content if the file already exists.
     *
     * @param path the path where the file is located
     * @param content the content to be written into the file
     * @throws IOException
     */
    private void writeToFile(Path path, String content) throws IOException {
        try (final BufferedWriter writer = Files.newBufferedWriter(path, Charset.defaultCharset())) {
            writer.write(content);
        }
    }

    protected JsonValue fetchRepoUpdates(Context context) throws ResourceException {
        // List repo update files present in archive.
        ActionResponse response = resource.action(context,
                Requests.newActionRequest(UPDATE_ROUTE, UPDATE_ACTION_LIST_REPO_UPDATES)
                        .setAdditionalParameter(UPDATE_PARAM_ARCHIVE, config.getUpdateArchive()));
        return response.getJsonContent();
    }
}