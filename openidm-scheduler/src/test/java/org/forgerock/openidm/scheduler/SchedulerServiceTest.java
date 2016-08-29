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
 * Portions copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openidm.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.resource.Requests.newActionRequest;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.cluster.ClusterManagementService;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.scheduler.SchedulerService.SchedulerAction;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.test.assertj.AssertJPromiseAssert;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Basic unit tests for the scheduler service
 */
public class SchedulerServiceTest {
    
    /**
     * The Scheduler Service
     */
    private SchedulerService schedulerService;
    
    /**
     * The Scheduler configuration
     */
    private JsonValue testScheduleConfig;
    
    @BeforeClass
    void setUp() throws Exception {
        schedulerService = createSchedulerService("/scheduler.json");
        testScheduleConfig = getConfig("/schedule-test1.json");
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
    
    /**
     * Creates a {@link SchedulerService} from the passed in configuration file.
     * 
     * @param configFile the scheduler's configuration file name
     * @return a {@link SchedulerService} implementation
     * @throws Exception
     */
    private SchedulerService createSchedulerService(final String configFile) throws Exception {
        final JSONEnhancedConfig jsonEnhancedConfig = mock(JSONEnhancedConfig.class);
        final ClusterManagementService clusterService = mock(ClusterManagementService.class);
        final SchedulerService schedulerService = new SchedulerService();
        when(jsonEnhancedConfig.getConfigurationAsJson(any(ComponentContext.class))).thenReturn(getConfig(configFile));
        when(clusterService.getInstanceId()).thenReturn("test-node");
        // bind services
        schedulerService.bindEnhancedConfig(jsonEnhancedConfig);
        schedulerService.clusterManager = clusterService;
        // Activate the service
        schedulerService.activate(getMockedContext());
        return schedulerService;
    }

    /**
     * Returns a mocked {@link ComponentContext} instance.
     * @return a {@link ComponentContext} instance
     */
    private ComponentContext getMockedContext() {
        ComponentContext mockedContext = mock(ComponentContext.class);
        BundleContext mockedBundleContext = mock(BundleContext.class);
        Dictionary<String, Object> compContextProperties = new Hashtable<>();
        //compContextProperties.put("config.factory-pid", null);
        when(mockedContext.getProperties()).thenReturn(compContextProperties);
        when(mockedContext.getBundleContext()).thenReturn(mockedBundleContext);
        return mockedContext;
    }
    
    @Test
    public void testCreateSchedule() throws Exception {
        //given
        final CreateRequest createRequest = Requests.newCreateRequest("scheduler", "test1", testScheduleConfig);

        //when
        Promise<ResourceResponse, ResourceException> promise = schedulerService.handleCreate(new RootContext(), createRequest);

        //then
        AssertJPromiseAssert.assertThat(promise)
                .isNotNull()
                .succeeded();
        ResourceResponse resourceResponse =
                promise.getOrThrow(IdentityServer.getPromiseTimeout(), TimeUnit.MILLISECONDS);
        assertThat(resourceResponse.getContent().asMap()).isEqualTo(testScheduleConfig.asMap());
    }
    
    @Test
    public void testReadSchedule() throws Exception {
        //given
        final ReadRequest readRequest = Requests.newReadRequest("test1");

        //when
        Promise<ResourceResponse, ResourceException> promise = schedulerService.handleRead(new RootContext(), readRequest);

        //then
        AssertJPromiseAssert.assertThat(promise)
                .isNotNull()
                .succeeded();
        ResourceResponse resourceResponse =
                promise.getOrThrow(IdentityServer.getPromiseTimeout(), TimeUnit.MILLISECONDS);
        assertThat(resourceResponse.getContent().asMap()).isEqualTo(testScheduleConfig.asMap());
    }
    
    @Test
    public void testPauseJobsAction() throws Exception {
        //given
        final ActionRequest readRequest = Requests.newActionRequest("", SchedulerAction.pauseJobs.toString());

        //when
        Promise<ActionResponse, ResourceException> promise = schedulerService.handleAction(new RootContext(), readRequest);

        //then
        AssertJPromiseAssert.assertThat(promise)
                .isNotNull()
                .succeeded();
        ActionResponse resourceResponse = promise.getOrThrow(IdentityServer.getPromiseTimeout(), TimeUnit.MILLISECONDS);
        assertThat(resourceResponse.getJsonContent().get("success").getObject()).isEqualTo(new Boolean(true));
    }
    
    @Test
    public void testResumeJobsAction() throws Exception {
        //given
        final ActionRequest readRequest = Requests.newActionRequest("", SchedulerAction.resumeJobs.toString());

        //when
        Promise<ActionResponse, ResourceException> promise = schedulerService.handleAction(new RootContext(), readRequest);

        //then
        AssertJPromiseAssert.assertThat(promise)
                .isNotNull()
                .succeeded();
        ActionResponse resourceResponse = promise.getOrThrow(IdentityServer.getPromiseTimeout(), TimeUnit.MILLISECONDS);
        assertThat(resourceResponse.getJsonContent().get("success").getObject()).isEqualTo(new Boolean(true));
    }
    
    @Test
    public void testListCurrentlyExecutingJobsAction() throws Exception {
        //given
        final ActionRequest readRequest = Requests.newActionRequest("", SchedulerAction.listCurrentlyExecutingJobs.toString());

        //when
        Promise<ActionResponse, ResourceException> promise = schedulerService.handleAction(new RootContext(), readRequest);

        //then
        AssertJPromiseAssert.assertThat(promise)
                .isNotNull()
                .succeeded();
        ActionResponse resourceResponse = promise.getOrThrow(IdentityServer.getPromiseTimeout(), TimeUnit.MILLISECONDS);
        assertThat(resourceResponse.getJsonContent().asList().size()).isEqualTo(0);
    }

    @Test
    public void testQueryJobs() throws Exception {
        // given
        for (int i = 0; i < 12; i++) {
            JsonValue persisted = getConfig("/schedule-persisted.json");
            Promise<ResourceResponse, ResourceException> createPromise =
                    schedulerService.handleCreate(new RootContext(), Requests.newCreateRequest("", persisted));
            assertThat(createPromise).isNotNull().succeeded();
        }
        // should have 13 now, 12 of them persisted.
        validateQueryCount(Requests.newQueryRequest("").setQueryId(QueryConstants.QUERY_ALL_IDS), 13);

        // then validate first page with a single offset.
        QueryRequest queryRequest = Requests.newQueryRequest("");
        queryRequest.setPagedResultsOffset(1); // offset shifts the results by 1 record.
        queryRequest.setQueryFilter(QueryFilter.equalTo(new JsonPointer("persisted"), true));
        queryRequest.setPageSize(5);
        QueryResponse queryResponse = validateQueryCount(queryRequest, 5);
        assertThat(queryResponse.getTotalPagedResults()).isEqualTo(12);
        assertThat(queryResponse.getPagedResultsCookie()).isEqualTo("6");

        // then validate second page of 5 using cookie from last result.
        queryRequest = Requests.newQueryRequest("");
        queryRequest.setQueryFilter(QueryFilter.equalTo(new JsonPointer("persisted"), true));
        queryRequest.setPageSize(5);
        queryRequest.setPagedResultsCookie(queryResponse.getPagedResultsCookie());
        queryResponse = validateQueryCount(queryRequest, 5);
        assertThat(queryResponse.getTotalPagedResults()).isEqualTo(12);
        assertThat(queryResponse.getPagedResultsCookie()).isEqualTo("11");

        // then validate last page, only 1 should be left
        queryRequest = Requests.newQueryRequest("");
        queryRequest.setQueryFilter(QueryFilter.equalTo(new JsonPointer("persisted"), true));
        queryRequest.setPageSize(5);
        queryRequest.setPagedResultsCookie(queryResponse.getPagedResultsCookie());
        queryResponse = validateQueryCount(queryRequest, 1);
        assertThat(queryResponse.getTotalPagedResults()).isEqualTo(12);
        assertThat(queryResponse.getPagedResultsCookie()).isNull();
    }

    /**
     * runs the queryRequest and validates that the count of returned records matches the expectedCount
     * @param request query to run.
     * @param expectedCount expected results to find.
     * @return the response from the query
     * @throws ResourceException when terrible things happen.
     */
    private QueryResponse validateQueryCount(QueryRequest request, int expectedCount) throws ResourceException {
        final AtomicInteger count = new AtomicInteger();
        Promise<QueryResponse, ResourceException> queryPromise = schedulerService.handleQuery(
                new RootContext(), request, new QueryResourceHandler() {
                    @Override
                    public boolean handleResource(ResourceResponse resource) {
                        count.getAndIncrement();
                        return true;
                    }
                });
        assertThat(queryPromise).isNotNull().succeeded();
        assertThat(count.get()).isEqualTo(expectedCount);
        return queryPromise.getOrThrowUninterruptibly();
    }

}
