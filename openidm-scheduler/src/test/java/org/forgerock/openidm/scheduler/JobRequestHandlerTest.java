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
package org.forgerock.openidm.scheduler;

import static org.forgerock.json.resource.Resources.newInternalConnectionFactory;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.quartz.impl.StdSchedulerFactory.PROP_SCHED_INSTANCE_ID;
import static org.quartz.impl.StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME;
import static org.quartz.impl.StdSchedulerFactory.PROP_THREAD_POOL_CLASS;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Router;
import org.forgerock.openidm.cluster.ClusterManagementService;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.test.assertj.AssertJPromiseAssert;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.SimpleThreadPool;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JobRequestHandlerTest {

    private static final String INSTANCE_ID = "instanceId";

    @DataProvider(name = "scheduleFiles")
    public Object[][] createCompositeData() throws Exception {
        return new Object[][] {
                { getConfig("/schedule-test1.json")},
                { getConfig("/schedule-persisted.json")}
        };
    }

    /**
     * Returns a {@link JsonValue} object representing a JSON configuration.
     *
     * @param configName the name of the file containing the configuration
     * @return a {@link JsonValue} object representing a JSON configuration
     * @throws Exception
     */
    private JsonValue getConfig(final String configName) throws Exception {
        InputStream configStream = getClass().getResourceAsStream(configName);
        return new JsonValue(new ObjectMapper().readValue(configStream, LinkedHashMap.class));
    }

    private ConnectionFactory createConnectionFactory() {
        final Router router = new Router();
        return newInternalConnectionFactory(router);
    }

    @Test(dataProvider = "scheduleFiles")
    public void testCreateJob(final JsonValue scheduleConfig) throws Exception {
        // given
        final JobRequestHandler jobRequestHandler = createJobRequestHandler();

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                jobRequestHandler.handleCreate(new RootContext(), Requests.newCreateRequest("", scheduleConfig));

        // then
        assertThat(promise).succeeded().isNotNull();
        Assertions.assertThat(promise.get().getId()).isNotNull().isNotEqualTo("");
    }

    @Test(dataProvider = "scheduleFiles")
    public void testReadJob(final JsonValue scheduleConfig) throws Exception {
        final JobRequestHandler jobRequestHandler = createJobRequestHandler();

        // when
        // create job
        final Promise<ResourceResponse, ResourceException> job =
                jobRequestHandler.handleCreate(new RootContext(), Requests.newCreateRequest("", scheduleConfig));

        final Promise<ResourceResponse, ResourceException> promise =
                jobRequestHandler.handleRead(new RootContext(), Requests.newReadRequest("", job.get().getId()));

        // then
        assertThat(promise).succeeded().isNotNull();
        Assertions.assertThat(promise.get().getId()).isNotNull().isEqualTo(job.get().getId());
    }

    @Test(dataProvider = "scheduleFiles")
    public void testUpdateJob(final JsonValue scheduleConfig) throws Exception {
        final JobRequestHandler jobRequestHandler = createJobRequestHandler();

        // when
        // create job
        final Promise<ResourceResponse, ResourceException> job =
                jobRequestHandler.handleCreate(new RootContext(), Requests.newCreateRequest("", scheduleConfig));

        scheduleConfig.put("enabled", true);
        final Promise<ResourceResponse, ResourceException> promise =
                jobRequestHandler.handleUpdate(
                        new RootContext(), Requests.newUpdateRequest("", job.get().getId(), scheduleConfig));

        // then
        assertThat(promise).succeeded().isNotNull();
        Assertions.assertThat(promise.get().getContent().get("enabled").asBoolean()).isNotNull().isEqualTo(true);
    }

    @Test(dataProvider = "scheduleFiles")
    public void testDeleteJob(final JsonValue scheduleConfig) throws Exception {
        final JobRequestHandler jobRequestHandler = createJobRequestHandler();

        // when
        // create job
        final Promise<ResourceResponse, ResourceException> job =
                jobRequestHandler.handleCreate(new RootContext(), Requests.newCreateRequest("", scheduleConfig));

        final Promise<ResourceResponse, ResourceException> promise =
                jobRequestHandler.handleDelete(new RootContext(), Requests.newDeleteRequest("", job.get().getId()));

        // then
        assertThat(promise).succeeded().isNotNull();
        Assertions.assertThat(promise.get().getId()).isNotNull().isEqualTo(job.get().getId());
    }

    @Test
    public void testPatchJob() throws Exception {
        final JobRequestHandler jobRequestHandler = createJobRequestHandler();

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                jobRequestHandler.handlePatch(new RootContext(), Requests.newPatchRequest("", ""));

        // then
        assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testPauseJobsAction() throws Exception {
        //given
        final ActionRequest actionRequest = Requests.newActionRequest("", JobRequestHandler.JobAction.pauseJobs.name());
        final JobRequestHandler jobRequestHandler = createJobRequestHandler();

        //when
        final Promise<ActionResponse, ResourceException> promise =
                jobRequestHandler.handleAction(new RootContext(), actionRequest);

        //then
        AssertJPromiseAssert.assertThat(promise).isNotNull().succeeded();
        final ActionResponse resourceResponse = promise.getOrThrow();
        Assertions.assertThat(resourceResponse.getJsonContent().get("success").asBoolean()).isEqualTo(true);
    }

    @Test
    public void testResumeJobsAction() throws Exception {
        //given
        final ActionRequest actionRequest =
                Requests.newActionRequest("", JobRequestHandler.JobAction.resumeJobs.name());
        final JobRequestHandler jobRequestHandler = createJobRequestHandler();

        //when
        final Promise<ActionResponse, ResourceException> promise =
                jobRequestHandler.handleAction(new RootContext(), actionRequest);

        //then
        AssertJPromiseAssert.assertThat(promise).isNotNull().succeeded();
        final ActionResponse resourceResponse = promise.getOrThrow();
        Assertions.assertThat(resourceResponse.getJsonContent().get("success").asBoolean()).isEqualTo(true);
    }

    @Test
    public void testListCurrentlyExecutingJobsAction() throws Exception {
        //given
        final ActionRequest actionRequest =
                Requests.newActionRequest("", JobRequestHandler.JobAction.listCurrentlyExecutingJobs.name());
        final JobRequestHandler jobRequestHandler = createJobRequestHandler();

        //when
        final Promise<ActionResponse, ResourceException> promise =
                jobRequestHandler.handleAction(new RootContext(), actionRequest);

        //then
        AssertJPromiseAssert.assertThat(promise).isNotNull().succeeded();
        final ActionResponse resourceResponse = promise.getOrThrow();
        Assertions.assertThat(resourceResponse.getJsonContent().asList().size()).isEqualTo(0);
    }

    @Test
    public void testQueryJobs() throws Exception {
        // given
        final JobRequestHandler jobRequestHandler = createJobRequestHandler();
        for (int i = 0; i < 12; i++) {
            JsonValue persisted = getConfig("/schedule-persisted.json");
            Promise<ResourceResponse, ResourceException> createPromise =
                    jobRequestHandler.handleCreate(new RootContext(), Requests.newCreateRequest("", persisted));
            assertThat(createPromise).isNotNull().succeeded();
        }
        // should have 12 jobs.
        validateQueryCount(
                Requests.newQueryRequest("").setQueryId(QueryConstants.QUERY_ALL_IDS), 12, jobRequestHandler);

        // then validate first page with a single offset.
        QueryRequest queryRequest = Requests.newQueryRequest("");
        queryRequest.setPagedResultsOffset(1); // offset shifts the results by 1 record.
        queryRequest.setQueryFilter(QueryFilter.equalTo(new JsonPointer("persisted"), true));
        queryRequest.setPageSize(5);
        QueryResponse queryResponse = validateQueryCount(queryRequest, 5, jobRequestHandler);
        Assertions.assertThat(queryResponse.getTotalPagedResults()).isEqualTo(12);
        Assertions.assertThat(queryResponse.getPagedResultsCookie()).isEqualTo("6");

        // then validate second page of 5 using cookie from last result.
        queryRequest = Requests.newQueryRequest("");
        queryRequest.setQueryFilter(QueryFilter.equalTo(new JsonPointer("persisted"), true));
        queryRequest.setPageSize(5);
        queryRequest.setPagedResultsCookie(queryResponse.getPagedResultsCookie());
        queryResponse = validateQueryCount(queryRequest, 5, jobRequestHandler);
        Assertions.assertThat(queryResponse.getTotalPagedResults()).isEqualTo(12);
        Assertions.assertThat(queryResponse.getPagedResultsCookie()).isEqualTo("11");

        // then validate last page, only 1 should be left
        queryRequest = Requests.newQueryRequest("");
        queryRequest.setQueryFilter(QueryFilter.equalTo(new JsonPointer("persisted"), true));
        queryRequest.setPageSize(5);
        queryRequest.setPagedResultsCookie(queryResponse.getPagedResultsCookie());
        queryResponse = validateQueryCount(queryRequest, 1, jobRequestHandler);
        Assertions.assertThat(queryResponse.getTotalPagedResults()).isEqualTo(12);
        Assertions.assertThat(queryResponse.getPagedResultsCookie()).isNull();
    }

    /**
     * runs the queryRequest and validates that the count of returned records matches the expectedCount
     * @param request query to run.
     * @param expectedCount expected results to find.
     * @param requestHandler the {@link RequestHandler} to send the {@link QueryRequest} to.
     * @return the response from the query
     * @throws ResourceException when terrible things happen.
     */
    private QueryResponse validateQueryCount(final QueryRequest request, final int expectedCount,
            final RequestHandler requestHandler) throws ResourceException {
        final AtomicInteger count = new AtomicInteger();
        final Promise<QueryResponse, ResourceException> queryPromise = requestHandler.handleQuery(
                new RootContext(), request, new QueryResourceHandler() {
                    @Override
                    public boolean handleResource(ResourceResponse resource) {
                        count.getAndIncrement();
                        return true;
                    }
                });
        assertThat(queryPromise).isNotNull().succeeded();
        Assertions.assertThat(count.get()).isEqualTo(expectedCount);
        return queryPromise.getOrThrowUninterruptibly();
    }

    private Scheduler createScheduler(final String name) throws SchedulerException {
        final StdSchedulerFactory sf = new StdSchedulerFactory();
        final Properties properties = new Properties();
        properties.put(PROP_SCHED_INSTANCE_NAME, name);
        properties.put(PROP_SCHED_INSTANCE_ID, name);
        properties.put(PROP_THREAD_POOL_CLASS, SimpleThreadPool.class.getCanonicalName());
        properties.put("org.quartz.threadPool.threadCount", "10");
        properties.put("org.quartz.threadPool.threadPriority", "5");
        properties.put("org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", true);
        sf.initialize(properties);
        return sf.getScheduler();
    }

    private JobRequestHandler createJobRequestHandler() throws SchedulerException {
        final ClusterManagementService clusterManager = mock(ClusterManagementService.class);
        when(clusterManager.getInstanceId()).thenReturn(INSTANCE_ID);
        return new JobRequestHandler(
                new PersistedScheduler(createScheduler(UUID.randomUUID().toString()), createConnectionFactory()),
                new MemoryScheduler(createScheduler(UUID.randomUUID().toString())),
                INSTANCE_ID);
    }
}
