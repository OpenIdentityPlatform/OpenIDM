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

import static org.forgerock.openidm.shell.impl.UpdateCommand.UpdateStep.ACCEPT_LICENSE;
import static org.forgerock.openidm.shell.impl.UpdateCommand.UpdateStep.ENABLE_SCHEDULER;
import static org.forgerock.openidm.shell.impl.UpdateCommand.UpdateStep.ENTER_MAINTENANCE_MODE;
import static org.forgerock.openidm.shell.impl.UpdateCommand.UpdateStep.EXIT_MAINTENANCE_MODE;
import static org.forgerock.openidm.shell.impl.UpdateCommand.UpdateStep.FORCE_RESTART;
import static org.forgerock.openidm.shell.impl.UpdateCommand.UpdateStep.INSTALL_ARCHIVE;
import static org.forgerock.openidm.shell.impl.UpdateCommand.UpdateStep.PAUSING_SCHEDULER;
import static org.forgerock.openidm.shell.impl.UpdateCommand.UpdateStep.PREVIEW_ARCHIVE;
import static org.forgerock.openidm.shell.impl.UpdateCommand.UpdateStep.WAIT_FOR_INSTALL_DONE;
import static org.forgerock.openidm.shell.impl.UpdateCommand.UpdateStep.WAIT_FOR_JOBS_TO_COMPLETE;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.felix.service.command.CommandSession;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
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
    static final String UPDATE_PARAM_ARCHIVE = "archive";
    static final String UPDATE_ACTION_UPDATE = "update";
    static final String UPDATE_ACTION_RESTART = "restart";
    static final String UPDATE_STATUS_FAILED = "FAILED";
    static final String UPDATE_STATUS_COMPLETE = "COMPLETE";
    static final String UPDATE_STATUS_IN_PROGRESS = "IN_PROGRESS";

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
        PAUSING_SCHEDULER,
        WAIT_FOR_JOBS_TO_COMPLETE,
        ENTER_MAINTENANCE_MODE,
        INSTALL_ARCHIVE,
        WAIT_FOR_INSTALL_DONE,
        EXIT_MAINTENANCE_MODE,
        ENABLE_SCHEDULER,
        FORCE_RESTART
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
        registerStepExecutor(new PauseSchedulerStepExecutor());
        registerStepExecutor(new WaitForJobsStepExecutor());
        registerStepExecutor(new EnterMaintenanceModeStepExecutor());
        registerStepExecutor(new InstallArchiveStepExecutor());
        registerStepExecutor(new WaitForInstallDoneStepExecutor());
        registerStepExecutor(new ExitMaintenanceModeStepExecutor());
        registerStepExecutor(new EnableSchedulerStepExecutor());
        registerStepExecutor(new ForceRestartStepExecutor());

        // Set the sequence of execution for the steps.
        setExecuteSequence(PREVIEW_ARCHIVE, ACCEPT_LICENSE, PAUSING_SCHEDULER, WAIT_FOR_JOBS_TO_COMPLETE,
                ENTER_MAINTENANCE_MODE, INSTALL_ARCHIVE, WAIT_FOR_INSTALL_DONE);
        // Set the sequence of execution of the recovery steps.
        setRecoverySequence(EXIT_MAINTENANCE_MODE, ENABLE_SCHEDULER, FORCE_RESTART);
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
     * the executor is ok to run.  When all are complete, or one fails, this will then similarly loop through each
     * step in the recoverySequence and execute its registered executor.
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
                    if (!executor.execute(context, executionResults)) {
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
                        if (!executor.execute(context, executionResults)) {
                            log("WARN: Failed a recovery step " + nextStep + ", continuing on with recovery.");
                        }
                    }
                } catch (Exception e) {
                    log("ERROR: Error in recovery step " + nextStep + ", continuing on with recovery.", e);
                }
            }
        }
        return executionResults;
    }

    /**
     * Opens the logger for appending, if the file has been defined in the config.
     */
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

    private void log(String message) {
        if (!config.isQuietMode()) {
            session.getConsole().println(message);
        }
        if (null != logger) {
            logger.println(SimpleDateFormat.getDateTimeInstance().format(new Date()) + ": " + message);
        }
    }

    private void log(String message, Throwable throwable) {
        if (!config.isQuietMode()) {
            throwable.printStackTrace(session.getConsole());
            log(message);
        }
        if (null != logger) {
            throwable.printStackTrace(logger);
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
        return (null != archiveData && "true".equals(archiveData.get("restartRequired").defaultTo("false").asString()));
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
         * @return true if the execution ran to a successful outcome.
         */
        boolean execute(Context context, UpdateExecutionState state);

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
        public boolean execute(Context context, UpdateExecutionState state) {
            try {
                ActionResponse response = resource.action(context,
                        Requests.newActionRequest(UPDATE_ROUTE, UPDATE_ACTION_AVAIL));
                String updateArchive = config.getUpdateArchive();
                for (JsonValue archiveData : response.getJsonContent()) {
                    if (updateArchive.equals(archiveData.get("archive").asString())) {
                        state.setArchiveData(archiveData);
                        return true;
                    }
                }
                log("Archive was not found in the bin/update directory. Requested filename was = " + updateArchive);
                return false;
            } catch (ResourceException e) {
                log("The attempt to lookup the archive meta-data failed.", e);
                return false;
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
        public boolean execute(Context context, UpdateExecutionState state) {
            try {
                // Request the license content
                ActionResponse response = resource.action(context,
                        Requests.newActionRequest(UPDATE_ROUTE, UPDATE_ACTION_GET_LICENSE)
                                .setAdditionalParameter(UPDATE_PARAM_ARCHIVE, config.getUpdateArchive()));
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
            } catch (ResourceException e) {
                log("There was trouble retrieving the license agreement.", e);
                return false;
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
        public boolean execute(Context context, UpdateExecutionState state) {
            try {
                log("Pausing the Scheduler");
                ActionResponse response = resource.action(context,
                        Requests.newActionRequest(SCHEDULER_ROUTE, SCHEDULER_ACTION_PAUSE));
                // Test if the pause request was successful.
                if (response.getJsonContent().get("success").defaultTo(false).asBoolean()) {
                    log("Scheduler has been paused.");
                    return true;
                } else {
                    log("Scheduler could not be paused. Exiting update process.");
                    return false;
                }
            } catch (ResourceException e) {
                log("Error encountered while pausing the job scheduler.", e);
                return false;
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
        public boolean execute(Context context, UpdateExecutionState state) {
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
                            return false;
                        }
                        try {
                            log("Waiting for jobs to finish...");
                            Thread.sleep(config.getCheckJobsRunningFrequency());
                        } catch (InterruptedException e) {
                            log("WARNING: Got interrupted while waiting for jobs to finish, exiting update process.");
                            return false;
                        }
                        timeout = (System.currentTimeMillis() - start > maxWaitTime);
                    }
                } catch (ResourceException e) {
                    log("Error encountered while waiting for jobs to finish", e);
                    return false;
                }
            } while (jobRunning && !timeout);

            if (jobRunning) {
                log("Running jobs did not finish within the allotted wait time of " + maxWaitTime + "ms.");
                return false;
            } else {
                log("All running jobs have finished.");
                return true;
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
        public boolean execute(Context context, UpdateExecutionState state) {
            try {
                log("Entering into maintenance mode...");
                // Make the call to enter maintenance mode.
                ActionResponse response = resource.action(context,
                        Requests.newActionRequest(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_ENABLE));
                // Test that we are now in maintenance mode.
                if (response.getJsonContent().get("maintenanceEnabled").defaultTo(false).asBoolean()) {
                    log("Now in maintenance mode.");
                    return true;
                } else {
                    log("Failed to enter maintenance mode. Exiting update process.");
                    return false;
                }
            } catch (ResourceException e) {
                log("Error occurred while attempting to enter maintenance mode.", e);
                return false;
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
        public boolean execute(Context context, UpdateExecutionState state) {
            try {
                log("Installing the update archive " + config.getUpdateArchive());
                // Invoke the installation process.
                ActionResponse response = resource.action(context,
                        Requests.newActionRequest(UPDATE_ROUTE, UPDATE_ACTION_UPDATE)
                                .setAdditionalParameter(UPDATE_PARAM_ARCHIVE, config.getUpdateArchive()));
                // Read response from install call.
                JsonValue installResponse = response.getJsonContent();
                if (installResponse.get("status").isNull()) {
                    return false;
                }
                state.setInstallResponse(installResponse);
                state.setStartInstallTime(System.currentTimeMillis());
                return true;
            } catch (ResourceException e) {
                log("Error encountered while installing the update archive.", e);
                return false;
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
     * This will repeatably check the update installation status until it times out or returns COMPLETE or FAILED.
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
        public boolean execute(Context context, UpdateExecutionState state) {
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
                boolean timeout = false;
                while (!timeout && !UPDATE_STATUS_COMPLETE.equals(status) && !UPDATE_STATUS_FAILED.equals(status)) {
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
                    timeout = (System.currentTimeMillis() - startTime > config.getMaxUpdateWaitTimeMs());
                    status = response.getContent().get("status").defaultTo(UPDATE_STATUS_IN_PROGRESS)
                            .asString().toUpperCase();
                }
                if (UPDATE_STATUS_COMPLETE.equals(status) || UPDATE_STATUS_FAILED.equals(status)) {
                    state.setCompletedInstallStatus(status);
                    log("The update process is complete with a status of " + status);
                    return true;
                } else {
                    log("The update process failed to complete within the allotted time.  " +
                            "Please verify the state of OpenIDM.");
                    return false;
                }
            } catch (ResourceException e) {
                log("Error encountered while checking status of install.  The update might still be in process", e);
                return false;
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
     * Only if the archive didn't need a restart, then this executor will request OpenIDM to exit maintenance mode.
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
        public boolean execute(Context context, UpdateExecutionState state) {
            try {
                log("Exiting maintenance mode...");
                // Make the call to exit maintenance mode.
                ActionResponse response = resource.action(context,
                        Requests.newActionRequest(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE));
                // Test that we are no longer in maintenance mode.
                if (response.getJsonContent().get("maintenanceEnabled").defaultTo(false).asBoolean()) {
                    log("Failed to exit maintenance mode. Exiting update process.");
                    return false;
                } else {
                    log("No longer in maintenance mode.");
                    return true;
                }
            } catch (ResourceException e) {
                log("Error encountered while exiting maintenance mode.", e);
                return false;
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
        public boolean execute(Context context, UpdateExecutionState state) {
            try {
                log("Resuming the job scheduler.");
                ActionResponse response = resource.action(context,
                        Requests.newActionRequest(SCHEDULER_ROUTE, SCHEDULER_ACTION_RESUME_JOBS));
                // Pull the success value from the response.
                if (response.getJsonContent().get("success").defaultTo(false).asBoolean()) {
                    log("Scheduler has been resumed.");
                    return true;
                } else {
                    log("WARN: A successful request was made to resume the scheduler, " +
                            "but it appears to not have restarted.");
                    return false;
                }
            } catch (ResourceException e) {
                log("Trouble attempting to resume scheduled jobs.  Please check that the scheduler is resumed.", e);
                return false;
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
        public boolean execute(Context context, UpdateExecutionState state) {
            try {
                log("Restarting OpenIDM.");
                // Invoke the restart.
                resource.action(context, Requests.newActionRequest(UPDATE_ROUTE, UPDATE_ACTION_RESTART));
                log("Restart request completed.");
                return true; //
            } catch (ResourceException e) {
                log("Error encountered while requesting the restart of OpenIDM.", e);
                return false;
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
}