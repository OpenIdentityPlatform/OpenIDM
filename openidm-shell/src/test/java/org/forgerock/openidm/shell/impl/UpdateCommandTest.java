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
 * Portions Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openidm.shell.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openidm.shell.impl.UpdateCommand.*;
import static org.forgerock.openidm.shell.impl.UpdateCommand.UpdateStep.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.apache.felix.service.command.CommandSession;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.mockito.ArgumentMatcher;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests the UpdateCommand and its various exit points.
 *
 * @see UpdateCommand
 */
public class UpdateCommandTest {
    private CommandSession session;

    @BeforeClass
    public void setup() {
        session = mock(CommandSession.class);
        when(session.getConsole()).thenReturn(System.out);
    }

    @BeforeMethod
    public void beforeMethod() {
        System.out.println("-------- Update Command test START ---------");
    }

    @AfterMethod
    public void afterMethod() {
        System.out.println("-------- Update Command test END   ---------");
    }

    @Test
    public void testCantFindArchive() throws Exception {
        HttpRemoteJsonResource resource = mockResource(
                mc(UPDATE_ROUTE, UPDATE_ACTION_AVAIL,
                        json(object(
                                field("updates", array(object(field("archive", "xyz.zip")))),
                                field("rejects", array())
                        ))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_RESUME_JOBS, json(object(field("success", true)))),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE, json(object(field("maintenanceEnabled", false))))
        );

        UpdateCommandConfig config = new UpdateCommandConfig()
                .setUpdateArchive("test.zip")
                .setLogFilePath(null)
                .setQuietMode(false)
                .setAcceptedLicense(true)
                .setSkipRepoUpdatePreview(true)
                .setMaxJobsFinishWaitTimeMs(1000L)
                .setMaxUpdateWaitTimeMs(1000L);
        UpdateCommand updateCommand = new UpdateCommand(session, resource, config);
        UpdateExecutionState executionState = updateCommand.execute(new RootContext());

        assertThat(executionState.getLastAttemptedStep()).isEqualTo(PREVIEW_ARCHIVE);
        assertThat(executionState.getCompletedInstallStatus()).isNull();
        assertThat(executionState.getLastRecoveryStep()).isEqualTo(ENABLE_SCHEDULER);
    }

    @Test
    public void testCantFindValidArchive() throws Exception {
        HttpRemoteJsonResource resource = mockResource(
                mc(UPDATE_ROUTE, UPDATE_ACTION_AVAIL,
                        json(object(
                                field("updates", array()),
                                field("rejects",
                                        array(object(
                                                field("archive", "test.zip"),
                                                field("reason", "Fake failed message.")
                                        )))
                        ))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_RESUME_JOBS, json(object(field("success", true)))),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE, json(object(field("maintenanceEnabled", false))))
        );

        UpdateCommandConfig config = new UpdateCommandConfig()
                .setUpdateArchive("test.zip")
                .setLogFilePath(null)
                .setQuietMode(false)
                .setAcceptedLicense(true)
                .setSkipRepoUpdatePreview(true)
                .setMaxJobsFinishWaitTimeMs(1000L)
                .setMaxUpdateWaitTimeMs(1000L);
        UpdateCommand updateCommand = new UpdateCommand(session, resource, config);
        UpdateExecutionState executionState = updateCommand.execute(new RootContext());

        assertThat(executionState.getLastAttemptedStep()).isEqualTo(PREVIEW_ARCHIVE);
        assertThat(executionState.getCompletedInstallStatus()).isNull();
        assertThat(executionState.getLastRecoveryStep()).isEqualTo(ENABLE_SCHEDULER);
    }

    @Test
    public void testCantPauseScheduler() throws Exception {
        HttpRemoteJsonResource resource = mockResource(
                mc(UPDATE_ROUTE, UPDATE_ACTION_AVAIL,
                        json(object(
                                field("updates", array(object(field("archive", "test.zip")))),
                                field("rejects", array())
                        ))),
                mc(UPDATE_ROUTE, UPDATE_ACTION_GET_LICENSE, json(object(field("license", "This is the license")))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_PAUSE, json(object(field("success", false)))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_RESUME_JOBS, json(object(field("success", true)))),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE, json(object(field("maintenanceEnabled", false))))
        );

        UpdateCommandConfig config = new UpdateCommandConfig()
                .setUpdateArchive("test.zip")
                .setLogFilePath(null)
                .setQuietMode(false)
                .setAcceptedLicense(true)
                .setSkipRepoUpdatePreview(true)
                .setMaxJobsFinishWaitTimeMs(1000L)
                .setMaxUpdateWaitTimeMs(1000L);
        UpdateCommand updateCommand = new UpdateCommand(session, resource, config);
        UpdateExecutionState executionState = updateCommand.execute(new RootContext());

        assertThat(executionState.getLastAttemptedStep()).isEqualTo(PAUSING_SCHEDULER);
        assertThat(executionState.getCompletedInstallStatus()).isNull();
        assertThat(executionState.getLastRecoveryStep()).isEqualTo(ENABLE_SCHEDULER);
    }

    @Test
    public void testWaitForRunningJobsToFinish() throws Exception {
        HttpRemoteJsonResource resource = mockResource(
                mc(UPDATE_ROUTE, UPDATE_ACTION_AVAIL,
                        json(object(
                                field("updates", array(object(field("archive", "test.zip")))),
                                field("rejects", array())
                        ))),
                mc(UPDATE_ROUTE, UPDATE_ACTION_GET_LICENSE, json(object(field("license", "This is the license")))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_PAUSE, json(object(field("success", true)))),
                // mock our running jobs listing responses, with jobs running.
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_LIST_JOBS, json(array(object()))),
                // mock the next step to fail.
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_ENABLE, json(object(field("maintenanceEnabled", false)))),
                // mock the calls on recovery.
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_RESUME_JOBS, json(object(field("success", true)))),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE, json(object(field("maintenanceEnabled", false))))
        );

        // with the timeout set to 10 and the retry set to 20, it should timeout waiting for jobs to complete.
        UpdateCommandConfig config = new UpdateCommandConfig()
                .setUpdateArchive("test.zip")
                .setLogFilePath(null)
                .setQuietMode(false)
                .setAcceptedLicense(true)
                .setSkipRepoUpdatePreview(true)
                .setMaxJobsFinishWaitTimeMs(10L)
                .setCheckJobsRunningFrequency(20L)
                .setMaxUpdateWaitTimeMs(1000L);
        UpdateCommand updateCommand = new UpdateCommand(session, resource, config);
        UpdateExecutionState executionState = updateCommand.execute(new RootContext());

        assertThat(executionState.getLastAttemptedStep()).isEqualTo(WAIT_FOR_JOBS_TO_COMPLETE);
        assertThat(executionState.getCompletedInstallStatus()).isNull();
        assertThat(executionState.getLastRecoveryStep()).isEqualTo(ENABLE_SCHEDULER);

        //now that timeout testing worked, test that is can pass waiting for jobs to complete
        resource = mockResource(
                mc(UPDATE_ROUTE, UPDATE_ACTION_AVAIL,
                        json(object(
                                field("updates", array(object(field("archive", "test.zip")))),
                                field("rejects", array())
                        ))),
                mc(UPDATE_ROUTE, UPDATE_ACTION_GET_LICENSE, json(object(field("license", "This is the license")))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_PAUSE, json(object(field("success", true)))),
                // mock our running jobs listing responses, with 3 iterations of mocked responses.
                // this will help simulate waiting for a job to finish.
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_LIST_JOBS,
                        json(array(object())), json(array(object())), json(array(object())), json(array())),
                // mock the next step to fail.
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_ENABLE, json(object(field("maintenanceEnabled", false)))),
                // mock the calls on recovery.
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_RESUME_JOBS, json(object(field("success", true)))),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE, json(object(field("maintenanceEnabled", false))))
        );

        config = new UpdateCommandConfig()
                .setUpdateArchive("test.zip")
                .setLogFilePath(null)
                .setQuietMode(false)
                .setAcceptedLicense(true)
                .setSkipRepoUpdatePreview(true)
                .setMaxJobsFinishWaitTimeMs(1000L)
                .setCheckJobsRunningFrequency(10L)
                .setMaxUpdateWaitTimeMs(2000L);
        updateCommand = new UpdateCommand(session, resource, config);
        executionState = updateCommand.execute(new RootContext());

        assertThat(executionState.getLastAttemptedStep()).isEqualTo(ENTER_MAINTENANCE_MODE);
        assertThat(executionState.getCompletedInstallStatus()).isNull();
        assertThat(executionState.getLastRecoveryStep()).isEqualTo(ENABLE_SCHEDULER);
    }

    @Test
    public void testEnterMaintenanceMode() throws Exception {
        HttpRemoteJsonResource resource = mockResource(
                mc(UPDATE_ROUTE, UPDATE_ACTION_AVAIL,
                        json(object(
                                field("updates",
                                        array(object(
                                                field("archive", "test.zip"),
                                                field("restartRequired", false)
                                        ))
                                ),
                                field("rejects", array())
                        ))
                ),
                mc(UPDATE_ROUTE, UPDATE_ACTION_GET_LICENSE, json(object(field("license", "This is the license")))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_PAUSE, json(object(field("success", true)))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_LIST_JOBS, json(array(object())), json(array())),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_ENABLE, json(object(field("maintenanceEnabled", true)))),

                // mock with empty response to simulate early error
                mc(UPDATE_ROUTE, UPDATE_ACTION_UPDATE, json(object())),

                // mock the calls on recovery.
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_RESUME_JOBS, json(object(field("success", true)))),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE, json(object(field("maintenanceEnabled", false))))
        );

        UpdateCommandConfig config = new UpdateCommandConfig()
                .setUpdateArchive("test.zip")
                .setLogFilePath(null)
                .setQuietMode(false)
                .setAcceptedLicense(true)
                .setSkipRepoUpdatePreview(true)
                .setMaxJobsFinishWaitTimeMs(100L)
                .setCheckJobsRunningFrequency(10L)
                .setMaxUpdateWaitTimeMs(5000L);
        // a fake clock keeps the 100ms jobs budget from being crossed by a stalled CI runner (issue #219).
        UpdateCommand updateCommand = new UpdateCommand(session, resource, config, new FakeWaitClock(0L));
        UpdateExecutionState executionState = updateCommand.execute(new RootContext());

        assertThat(executionState.getLastAttemptedStep()).isEqualTo(INSTALL_ARCHIVE);
        assertThat(executionState.getCompletedInstallStatus()).isNull();
        assertThat(executionState.getLastRecoveryStep()).isEqualTo(ENABLE_SCHEDULER);
    }

    /**
     * Regression test for issue #219: the jobs budget expires during a sleep, but the next poll finds no running
     * jobs. The wait step must report what the last poll saw, not fail on the stale state from before the sleep.
     */
    @Test
    public void testWaitForJobsPollsAgainWhenTimeoutExpiresDuringSleep() throws Exception {
        HttpRemoteJsonResource resource = mockResource(
                mc(UPDATE_ROUTE, UPDATE_ACTION_AVAIL,
                        json(object(
                                field("updates", array(object(field("archive", "test.zip")))),
                                field("rejects", array())
                        ))),
                mc(UPDATE_ROUTE, UPDATE_ACTION_GET_LICENSE, json(object(field("license", "This is the license")))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_PAUSE, json(object(field("success", true)))),
                // one job running at the first poll, none at the second.
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_LIST_JOBS, json(array(object())), json(array())),
                // mock the next step to fail.
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_ENABLE, json(object(field("maintenanceEnabled", false)))),
                // mock the calls on recovery.
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_RESUME_JOBS, json(object(field("success", true)))),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE, json(object(field("maintenanceEnabled", false))))
        );

        UpdateCommandConfig config = new UpdateCommandConfig()
                .setUpdateArchive("test.zip")
                .setLogFilePath(null)
                .setQuietMode(false)
                .setAcceptedLicense(true)
                .setSkipRepoUpdatePreview(true)
                .setMaxJobsFinishWaitTimeMs(100L)
                .setCheckJobsRunningFrequency(10L)
                .setMaxUpdateWaitTimeMs(1000L);
        // every 10ms sleep stalls for another 190ms, so the 100ms budget is crossed during the first sleep.
        UpdateCommand updateCommand = new UpdateCommand(session, resource, config, new FakeWaitClock(190L));
        UpdateExecutionState executionState = updateCommand.execute(new RootContext());

        assertThat(executionState.getLastAttemptedStep()).isEqualTo(ENTER_MAINTENANCE_MODE);
        assertThat(executionState.getCompletedInstallStatus()).isNull();
        assertThat(executionState.getLastRecoveryStep()).isEqualTo(ENABLE_SCHEDULER);
    }

    /**
     * Issue #222: when the update wait budget is exceeded, the command stops waiting and detaches from the update,
     * which may still be running on the server. The recovery steps must not leave maintenance mode or resume the
     * scheduler mid-install.
     */
    @Test
    public void testTimeoutInstallUpdateArchive() throws Exception {
        HttpRemoteJsonResource resource = mockResource(
                mc(UPDATE_ROUTE, UPDATE_ACTION_AVAIL,
                        json(object(
                                field("updates",
                                        array(object(
                                                field("archive", "test.zip"),
                                                field("restartRequired", false)
                                        ))
                                ),
                                field("rejects", array())
                        ))
                ),
                mc(UPDATE_ROUTE, UPDATE_ACTION_GET_LICENSE, json(object(field("license", "This is the license")))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_PAUSE, json(object(field("success", true)))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_LIST_JOBS, json(array())),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_ENABLE, json(object(field("maintenanceEnabled", true)))),

                mc(UPDATE_ROUTE, UPDATE_ACTION_UPDATE,
                        json(object(field("status", "IN_PROGRESS"), field(ResourceResponse.FIELD_CONTENT_ID, "1234")))),

                mc(UPDATE_LOG_ROUTE, null,
                        json(object(
                                field(ResourceResponse.FIELD_CONTENT_ID, "1234"),
                                field(ResourceResponse.FIELD_CONTENT_REVISION, "1"),
                                field("status", "IN_PROGRESS")
                        ))
                ),

                // mock the calls on recovery.
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_RESUME_JOBS, json(object(field("success", true)))),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE, json(object(field("maintenanceEnabled", false))))
        );

        UpdateCommandConfig config = new UpdateCommandConfig()
                .setUpdateArchive("test.zip")
                .setLogFilePath(null)
                .setQuietMode(false)
                .setAcceptedLicense(true)
                .setSkipRepoUpdatePreview(true)
                .setMaxJobsFinishWaitTimeMs(1000L)
                .setCheckJobsRunningFrequency(10L)
                .setMaxUpdateWaitTimeMs(10L)
                .setCheckCompleteFrequency(20L);
        // every 20ms poll sees IN_PROGRESS, so the 10ms budget is exceeded before the second sleep.
        UpdateCommand updateCommand = new UpdateCommand(session, resource, config, new FakeWaitClock(0L));
        UpdateExecutionState executionState = updateCommand.execute(new RootContext());

        assertThat(executionState.getLastAttemptedStep()).isEqualTo(WAIT_FOR_INSTALL_DONE);
        assertThat(executionState.getCompletedInstallStatus()).isNull();
        assertThat(executionState.isDetached()).isTrue();
        assertThat(executionState.getLastRecoveryStep()).isNull();
        verifyNoRecoveryCalls(resource);
    }

    @Test
    public void testFailedInstallUpdateArchive() throws Exception {
        HttpRemoteJsonResource resource = mockResource(
                mc(UPDATE_ROUTE, UPDATE_ACTION_AVAIL,
                        json(object(
                                field("updates",
                                        array(object(
                                                field("archive", "test.zip"),
                                                field("restartRequired", false)
                                        ))
                                ),
                                field("rejects", array())
                        ))
                ),
                mc(UPDATE_ROUTE, UPDATE_ACTION_GET_LICENSE, json(object(field("license", "This is the license")))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_PAUSE, json(object(field("success", true)))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_LIST_JOBS, json(array(object())), json(array())),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_ENABLE, json(object(field("maintenanceEnabled", true)))),

                mc(UPDATE_ROUTE, UPDATE_ACTION_UPDATE,
                        json(object(field("status", "IN_PROGRESS"), field(ResourceResponse.FIELD_CONTENT_ID, "1234")))),

                mc(UPDATE_LOG_ROUTE, null,
                        json(object(
                                field(ResourceResponse.FIELD_CONTENT_ID, "1234"),
                                field(ResourceResponse.FIELD_CONTENT_REVISION, "1"),
                                field("status", "IN_PROGRESS")
                        )),
                        json(object(
                                field(ResourceResponse.FIELD_CONTENT_ID, "1234"),
                                field(ResourceResponse.FIELD_CONTENT_REVISION, "1"),
                                field("status", UPDATE_STATUS_FAILED)
                        ))),

                // mock the calls on recovery.
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_RESUME_JOBS, json(object(field("success", true)))),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE, json(object(field("maintenanceEnabled", false))))
        );

        UpdateCommandConfig config = new UpdateCommandConfig()
                .setUpdateArchive("test.zip")
                .setLogFilePath(null)
                .setQuietMode(false)
                .setAcceptedLicense(true)
                .setSkipRepoUpdatePreview(true)
                .setMaxJobsFinishWaitTimeMs(1000L)
                .setCheckJobsRunningFrequency(10L)
                .setMaxUpdateWaitTimeMs(5000L)
                .setCheckCompleteFrequency(10L);
        // a fake clock keeps the wait budgets from being crossed by a stalled CI runner (issue #222).
        UpdateCommand updateCommand = new UpdateCommand(session, resource, config, new FakeWaitClock(0L));
        UpdateExecutionState executionState = updateCommand.execute(new RootContext());

        assertThat(executionState.getLastAttemptedStep()).isEqualTo(MARK_REPO_UPDATES_COMPLETE);
        assertThat(executionState.getCompletedInstallStatus()).isEqualTo(UPDATE_STATUS_FAILED);
        assertThat(executionState.getLastRecoveryStep()).isEqualTo(ENABLE_SCHEDULER);
    }

    @Test
    public void testSuccessfulInstall() throws Exception {

        HttpRemoteJsonResource resource = mockResource(
                mc(UPDATE_ROUTE, UPDATE_ACTION_AVAIL,
                        json(object(
                                field("updates",
                                        array(object(
                                                field("archive", "test.zip"),
                                                field("restartRequired", false)
                                        ))
                                ),
                                field("rejects", array())
                        ))
                ),
                mc(UPDATE_ROUTE, UPDATE_ACTION_GET_LICENSE, json(object(field("license", "This is the license")))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_PAUSE, json(object(field("success", true)))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_LIST_JOBS, json(array(object())), json(array())),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_ENABLE, json(object(field("maintenanceEnabled", true)))),
                mc(UPDATE_ROUTE, UPDATE_ACTION_UPDATE,
                        json(object(field("status", "IN_PROGRESS"), field(ResourceResponse.FIELD_CONTENT_ID, "1234")))),
                mc(UPDATE_LOG_ROUTE, null,
                        json(object(
                                field(ResourceResponse.FIELD_CONTENT_ID, "1234"),
                                field(ResourceResponse.FIELD_CONTENT_REVISION, "1"),
                                field("status", "IN_PROGRESS")
                        )),
                        json(object(
                                field(ResourceResponse.FIELD_CONTENT_ID, "1234"),
                                field(ResourceResponse.FIELD_CONTENT_REVISION, "1"),
                                field("status", "IN_PROGRESS")
                        )),
                        json(object(
                                field(ResourceResponse.FIELD_CONTENT_ID, "1234"),
                                field(ResourceResponse.FIELD_CONTENT_REVISION, "1"),
                                field("status", UPDATE_STATUS_COMPLETE)
                        ))),

                // mock the calls on recovery.
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_RESUME_JOBS, json(object(field("success", true)))),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE, json(object(field("maintenanceEnabled", false))))
        );

        UpdateCommandConfig config = new UpdateCommandConfig()
                .setUpdateArchive("test.zip")
                .setLogFilePath(null)
                .setQuietMode(false)
                .setAcceptedLicense(true)
                .setSkipRepoUpdatePreview(true)
                .setMaxJobsFinishWaitTimeMs(500L)
                .setCheckJobsRunningFrequency(10L)
                .setMaxUpdateWaitTimeMs(2000L)
                .setCheckCompleteFrequency(10L);
        // a fake clock keeps the wait budgets from being crossed by a stalled CI runner (issue #222).
        UpdateCommand updateCommand = new UpdateCommand(session, resource, config, new FakeWaitClock(0L));
        UpdateExecutionState executionState = updateCommand.execute(new RootContext());

        assertThat(executionState.getLastAttemptedStep()).isEqualTo(MARK_REPO_UPDATES_COMPLETE);
        assertThat(executionState.getCompletedInstallStatus()).isEqualTo(UPDATE_STATUS_COMPLETE);
        assertThat(executionState.getLastRecoveryStep()).isEqualTo(ENABLE_SCHEDULER);
    }

    @Test
    public void testSuccessfulInstallWithRestart() throws Exception {

        HttpRemoteJsonResource resource = mockResource(
                mc(UPDATE_ROUTE, UPDATE_ACTION_AVAIL,
                        json(object(
                                field("updates",
                                        array(object(
                                                field("archive", "test.zip"),
                                                field("restartRequired", true)
                                        ))
                                ),
                                field("rejects", array())
                        ))
                ),
                mc(UPDATE_ROUTE, UPDATE_ACTION_GET_LICENSE, json(object(field("license", "This is the license")))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_PAUSE, json(object(field("success", true)))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_LIST_JOBS, json(array(object())), json(array())),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_ENABLE, json(object(field("maintenanceEnabled", true)))),
                mc(UPDATE_ROUTE, UPDATE_ACTION_UPDATE,
                        json(object(field("status", "IN_PROGRESS"), field(ResourceResponse.FIELD_CONTENT_ID, "1234")))),
                mc(UPDATE_LOG_ROUTE, null,
                        json(object(
                                field(ResourceResponse.FIELD_CONTENT_ID, "1234"),
                                field(ResourceResponse.FIELD_CONTENT_REVISION, "1"),
                                field("status", "IN_PROGRESS")
                        )),
                        json(object(
                                field(ResourceResponse.FIELD_CONTENT_ID, "1234"),
                                field(ResourceResponse.FIELD_CONTENT_REVISION, "1"),
                                field("status", "IN_PROGRESS")
                        )),
                        json(object(
                                field(ResourceResponse.FIELD_CONTENT_ID, "1234"),
                                field(ResourceResponse.FIELD_CONTENT_REVISION, "1"),
                                field("status", UPDATE_STATUS_COMPLETE)
                        ))),

                // mock the calls on recovery.
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_RESUME_JOBS, json(object(field("success", true)))),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE, json(object(field("maintenanceEnabled", false))))
        );

        UpdateCommandConfig config = new UpdateCommandConfig()
                .setUpdateArchive("test.zip")
                .setLogFilePath(null)
                .setQuietMode(false)
                .setAcceptedLicense(true)
                .setSkipRepoUpdatePreview(true)
                .setMaxJobsFinishWaitTimeMs(500L)
                .setCheckJobsRunningFrequency(10L)
                .setMaxUpdateWaitTimeMs(2000L)
                .setCheckCompleteFrequency(10L);
        // a fake clock keeps the wait budgets from being crossed by a stalled CI runner (issue #222).
        UpdateCommand updateCommand = new UpdateCommand(session, resource, config, new FakeWaitClock(0L));
        UpdateExecutionState executionState = updateCommand.execute(new RootContext());

        assertThat(executionState.getLastAttemptedStep()).isEqualTo(MARK_REPO_UPDATES_COMPLETE);
        assertThat(executionState.getCompletedInstallStatus()).isEqualTo(UPDATE_STATUS_COMPLETE);
        assertThat(executionState.getLastRecoveryStep()).isEqualTo(FORCE_RESTART);
    }

    /**
     * Issue #222: an error while polling the update status leaves the update possibly running on the server, so the
     * command detaches from it exactly as on a timeout instead of running the recovery steps.
     */
    @Test
    public void testPollingErrorDetachesFromInstall() throws Exception {
        HttpRemoteJsonResource resource = mockResource(
                mc(UPDATE_ROUTE, UPDATE_ACTION_AVAIL,
                        json(object(
                                field("updates",
                                        array(object(
                                                field("archive", "test.zip"),
                                                field("restartRequired", true)
                                        ))
                                ),
                                field("rejects", array())
                        ))
                ),
                mc(UPDATE_ROUTE, UPDATE_ACTION_GET_LICENSE, json(object(field("license", "This is the license")))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_PAUSE, json(object(field("success", true)))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_LIST_JOBS, json(array())),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_ENABLE, json(object(field("maintenanceEnabled", true)))),
                mc(UPDATE_ROUTE, UPDATE_ACTION_UPDATE,
                        json(object(field("status", "IN_PROGRESS"), field(ResourceResponse.FIELD_CONTENT_ID, "1234")))),
                // mock the calls on recovery, which must not be made.
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_RESUME_JOBS, json(object(field("success", true)))),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE, json(object(field("maintenanceEnabled", false)))),
                mc(UPDATE_ROUTE, UPDATE_ACTION_RESTART, json(object()))
        );
        when(resource.read(any(Context.class), argThat(new IsRouteMatcher(UPDATE_LOG_ROUTE))))
                .thenThrow(ResourceException.newResourceException(ResourceException.UNAVAILABLE, "Connection lost"));

        UpdateCommandConfig config = new UpdateCommandConfig()
                .setUpdateArchive("test.zip")
                .setLogFilePath(null)
                .setQuietMode(false)
                .setAcceptedLicense(true)
                .setSkipRepoUpdatePreview(true)
                .setMaxJobsFinishWaitTimeMs(1000L)
                .setCheckJobsRunningFrequency(10L)
                .setCheckCompleteFrequency(10L);
        UpdateCommand updateCommand = new UpdateCommand(session, resource, config, new FakeWaitClock(0L));
        UpdateExecutionState executionState = updateCommand.execute(new RootContext());

        assertThat(executionState.getLastAttemptedStep()).isEqualTo(WAIT_FOR_INSTALL_DONE);
        assertThat(executionState.getCompletedInstallStatus()).isNull();
        assertThat(executionState.isDetached()).isTrue();
        assertThat(executionState.getLastRecoveryStep()).isNull();
        verifyNoRecoveryCalls(resource);
        verify(resource, never()).action(any(Context.class),
                argThat(new IsActionMatcher(UPDATE_ROUTE, UPDATE_ACTION_RESTART)));
    }

    /**
     * Issue #222: the default wait budget is unlimited, so an install whose polls stall for longer than the former
     * 30s default still runs to completion and to the regular recovery steps.
     */
    @Test
    public void testDefaultWaitsForInstallWithoutLimit() throws Exception {
        JsonValue inProgress = json(object(
                field(ResourceResponse.FIELD_CONTENT_ID, "1234"),
                field(ResourceResponse.FIELD_CONTENT_REVISION, "1"),
                field("status", "IN_PROGRESS")
        ));
        HttpRemoteJsonResource resource = mockResource(
                mc(UPDATE_ROUTE, UPDATE_ACTION_AVAIL,
                        json(object(
                                field("updates",
                                        array(object(
                                                field("archive", "test.zip"),
                                                field("restartRequired", false)
                                        ))
                                ),
                                field("rejects", array())
                        ))
                ),
                mc(UPDATE_ROUTE, UPDATE_ACTION_GET_LICENSE, json(object(field("license", "This is the license")))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_PAUSE, json(object(field("success", true)))),
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_LIST_JOBS, json(array())),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_ENABLE, json(object(field("maintenanceEnabled", true)))),
                mc(UPDATE_ROUTE, UPDATE_ACTION_UPDATE,
                        json(object(field("status", "IN_PROGRESS"), field(ResourceResponse.FIELD_CONTENT_ID, "1234")))),
                mc(UPDATE_LOG_ROUTE, null,
                        inProgress, inProgress, inProgress,
                        json(object(
                                field(ResourceResponse.FIELD_CONTENT_ID, "1234"),
                                field(ResourceResponse.FIELD_CONTENT_REVISION, "1"),
                                field("status", UPDATE_STATUS_COMPLETE)
                        ))),
                // mock the calls on recovery.
                mc(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_RESUME_JOBS, json(object(field("success", true)))),
                mc(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE, json(object(field("maintenanceEnabled", false))))
        );

        // maxUpdateWaitTimeMs is left at its default.
        UpdateCommandConfig config = new UpdateCommandConfig()
                .setUpdateArchive("test.zip")
                .setLogFilePath(null)
                .setQuietMode(false)
                .setAcceptedLicense(true)
                .setSkipRepoUpdatePreview(true)
                .setMaxJobsFinishWaitTimeMs(1000L)
                .setCheckJobsRunningFrequency(10L)
                .setCheckCompleteFrequency(10L);
        // every poll stalls for 40s, so four polls take well over the former 30s default.
        UpdateCommand updateCommand = new UpdateCommand(session, resource, config, new FakeWaitClock(40000L));
        UpdateExecutionState executionState = updateCommand.execute(new RootContext());

        assertThat(executionState.getLastAttemptedStep()).isEqualTo(MARK_REPO_UPDATES_COMPLETE);
        assertThat(executionState.getCompletedInstallStatus()).isEqualTo(UPDATE_STATUS_COMPLETE);
        assertThat(executionState.isDetached()).isFalse();
        assertThat(executionState.getLastRecoveryStep()).isEqualTo(ENABLE_SCHEDULER);
    }

    private void verifyNoRecoveryCalls(HttpRemoteJsonResource resource) throws ResourceException {
        verify(resource, never()).action(any(Context.class),
                argThat(new IsActionMatcher(MAINTENANCE_ROUTE, MAINTENANCE_ACTION_DISABLE)));
        verify(resource, never()).action(any(Context.class),
                argThat(new IsActionMatcher(SCHEDULER_JOB_ROUTE, SCHEDULER_ACTION_RESUME_JOBS)));
    }

    /**
     * A {@link UpdateCommand.WaitClock} whose time only advances when the command sleeps: each sleep advances it by
     * the requested duration plus a fixed stall, simulating a runner that is descheduled while sleeping.
     */
    private static class FakeWaitClock implements UpdateCommand.WaitClock {
        private final long stallMs;
        private long nowMs;

        FakeWaitClock(long stallMs) {
            this.stallMs = stallMs;
        }

        @Override
        public long nanoTime() {
            return TimeUnit.MILLISECONDS.toNanos(nowMs);
        }

        @Override
        public void sleep(long millis) {
            nowMs += millis + stallMs;
        }
    }

    private HttpRemoteJsonResource mockResource(MockCriteria... mockCriterion) throws ResourceException {
        HttpRemoteJsonResource resource = mock(HttpRemoteJsonResource.class);
        for (MockCriteria mockCriteria : mockCriterion) {
            mockCriteria.mockResponse(resource);
        }
        return resource;
    }

    private static MockCriteria mc(String route, String action, JsonValue... responseContent) {
        if (null == action) {
            return new MockReadCriteria(route, responseContent);
        } else {
            return new MockActionCriteria(route, action, responseContent);
        }
    }

    private static class MockReadCriteria extends MockCriteria {
        public MockReadCriteria(String route, JsonValue... responseContent) {
            super(route, null, responseContent);
        }

        @Override
        public void mockResponse(HttpRemoteJsonResource resource) throws ResourceException {
            JsonValue[] responseContent = this.getResponseContent();
            ResourceResponse firstResponse =
                    Responses.newResourceResponse(
                            responseContent[0].get(ResourceResponse.FIELD_CONTENT_ID).asString(),
                            responseContent[0].get(ResourceResponse.FIELD_CONTENT_REVISION).asString(),
                            responseContent[0]);
            if (responseContent.length > 1) {
                ResourceResponse[] secondOnwardResponses = new ResourceResponse[responseContent.length - 1];
                for (int i = 1; i < responseContent.length; i++) {
                    secondOnwardResponses[i - 1] = Responses.newResourceResponse(
                            responseContent[i].get(ResourceResponse.FIELD_CONTENT_ID).asString(),
                            responseContent[i].get(ResourceResponse.FIELD_CONTENT_REVISION).asString(),
                            responseContent[i]);
                }
                when(resource.read(
                        any(Context.class),
                        argThat(new IsRouteMatcher(this.getRoute()))))
                        .thenReturn(firstResponse, secondOnwardResponses);

            } else {
                when(resource.read(
                        any(Context.class),
                        argThat(new IsRouteMatcher(this.getRoute()))))
                        .thenReturn(firstResponse);
            }

        }
    }

    private static class MockActionCriteria extends MockCriteria {
        public MockActionCriteria(String route, String action, JsonValue... responseContent) {
            super(route, action, responseContent);
        }

        @Override
        public void mockResponse(HttpRemoteJsonResource resource) throws ResourceException {
            JsonValue[] responseContent = this.getResponseContent();
            if (responseContent.length > 1) {
                ActionResponse firstResponse = Responses.newActionResponse(responseContent[0]);
                ActionResponse[] secondOnwardResponses = new ActionResponse[responseContent.length - 1];
                for (int i = 1; i < responseContent.length; i++) {
                    secondOnwardResponses[i - 1] = Responses.newActionResponse(responseContent[i]);
                }
                when(resource.action(
                        any(Context.class),
                        argThat(new IsActionMatcher(this.getRoute(), this.getAction()))))
                        .thenReturn(firstResponse, secondOnwardResponses);
            } else {
                ActionResponse response = Responses.newActionResponse(responseContent[0]);
                when(resource.action(
                        any(Context.class),
                        argThat(new IsActionMatcher(this.getRoute(), this.getAction()))))
                        .thenReturn(response);
            }
        }
    }

    private abstract static class MockCriteria {
        private final String route;
        private final String action;
        private final JsonValue[] responseContent;

        public MockCriteria(String route, String action, JsonValue... responseContent) {
            this.route = route;
            this.action = action;
            this.responseContent = responseContent;
        }

        public String getRoute() {
            return route;
        }

        public String getAction() {
            return action;
        }

        public JsonValue[] getResponseContent() {
            return responseContent;
        }

        public abstract void mockResponse(HttpRemoteJsonResource resource) throws ResourceException;

    }

    private static class IsActionMatcher extends ArgumentMatcher<ActionRequest> {

        private final String route;
        private final String action;

        public IsActionMatcher(String route, String action) {
            this.route = route;
            this.action = action;
        }

        @Override
        public boolean matches(Object actionToMatch) {
            if (null == actionToMatch) {
                return false;
            }
            ActionRequest actionRequest = (ActionRequest) actionToMatch;
            return actionRequest.getResourcePath().equals(route) && actionRequest.getAction().equals(this.action);
        }
    }

    private static class IsRouteMatcher extends ArgumentMatcher<ReadRequest> {
        private final String route;

        public IsRouteMatcher(String route) {
            this.route = route;
        }

        @Override
        public boolean matches(Object requestToMatch) {
            return (null != requestToMatch && ((ReadRequest) requestToMatch).getResourcePath().startsWith(route));
        }
    }
}
