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
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openidm.sync.impl.cluster;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openidm.sync.impl.cluster.ClusteredReconJobDispatch.CLUSTERED_NEXT_PAGE_ACTION_PARAM;
import static org.forgerock.openidm.sync.impl.cluster.ClusteredReconJobDispatch.CLUSTERED_SOURCE_COMPLETION_PARAM;
import static org.forgerock.openidm.sync.impl.cluster.ClusteredReconJobDispatch.CLUSTERED_SUB_ACTION_KEY;
import static org.forgerock.openidm.sync.impl.cluster.ClusteredReconJobDispatch.CLUSTERED_TARGET_PHASE_PARAM;
import static org.forgerock.openidm.sync.impl.cluster.ClusteredReconJobDispatch.PAGING_COOKIE_KEY;
import static org.forgerock.openidm.sync.impl.cluster.ClusteredReconJobDispatch.RECON_ID_KEY;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.services.context.Context;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.testng.annotations.Test;

public class SchedulerClusteredReconJobDispatchTest {
    private static final String MAPPING_NAME = "ldap_managedobject";
    private static final String RECON_ID = "1234";
    private static final String PAGING_COOKIE = "offset";

    @Test
    public void testScheduledSourcePhaseState() throws ResourceException {
        final ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
        final Connection mockConnection = mock(Connection.class);
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.create(any(Context.class), any(CreateRequest.class))).thenAnswer(new Answer<ResourceResponse>() {
            @Override
            public ResourceResponse answer(InvocationOnMock invocation) throws Throwable {
                final CreateRequest request = ((CreateRequest) invocation.getArguments()[1]);
                final JsonValue requestContent = request.getContent();
                validateCommonFields(requestContent);
                org.assertj.core.api.Assertions.assertThat(
                        requestContent.get("invokeContext").asMap().get(CLUSTERED_SUB_ACTION_KEY)).isEqualTo(CLUSTERED_NEXT_PAGE_ACTION_PARAM);
                org.assertj.core.api.Assertions.assertThat(
                        requestContent.get("invokeContext").asMap().get(PAGING_COOKIE_KEY)).isEqualTo(PAGING_COOKIE);
                return newResourceResponse("id", "0", json(object()));
            }
        });
        new SchedulerClusteredReconJobDispatch(mockConnectionFactory, DateUtil.getDateUtil(), mock(Logger.class))
                .dispatchSourcePageRecon(RECON_ID, MAPPING_NAME, PAGING_COOKIE);
    }

    @Test
    public void testScheduledSourcePhaseCompletionCheckState() throws ResourceException {
        final ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
        final Connection mockConnection = mock(Connection.class);
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.create(any(Context.class), any(CreateRequest.class))).thenAnswer(new Answer<ResourceResponse>() {
            @Override
            public ResourceResponse answer(InvocationOnMock invocation) throws Throwable {
                final CreateRequest request = ((CreateRequest) invocation.getArguments()[1]);
                final JsonValue requestContent = request.getContent();
                validateCommonFields(requestContent);
                org.assertj.core.api.Assertions.assertThat(
                        requestContent.get("invokeContext").asMap().get(CLUSTERED_SUB_ACTION_KEY)).isEqualTo(CLUSTERED_SOURCE_COMPLETION_PARAM);
                return newResourceResponse("id", "0", json(object()));
            }
        });
        new SchedulerClusteredReconJobDispatch(mockConnectionFactory, DateUtil.getDateUtil(), mock(Logger.class))
                .dispatchSourcePhaseCompletionTask(RECON_ID, MAPPING_NAME);
    }

    @Test
    public void testScheduledTargetPhaseState() throws ResourceException {
        final ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
        final Connection mockConnection = mock(Connection.class);
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.create(any(Context.class), any(CreateRequest.class))).thenAnswer(new Answer<ResourceResponse>() {
            @Override
            public ResourceResponse answer(InvocationOnMock invocation) throws Throwable {
                final CreateRequest request = ((CreateRequest) invocation.getArguments()[1]);
                final JsonValue requestContent = request.getContent();
                validateCommonFields(requestContent);
                org.assertj.core.api.Assertions.assertThat(
                        requestContent.get("invokeContext").asMap().get(CLUSTERED_SUB_ACTION_KEY)).isEqualTo(CLUSTERED_TARGET_PHASE_PARAM);
                return newResourceResponse("id", "0", json(object()));
            }
        });
        new SchedulerClusteredReconJobDispatch(mockConnectionFactory, DateUtil.getDateUtil(), mock(Logger.class))
                .dispatchTargetPhase(RECON_ID, MAPPING_NAME);
    }

    @Test
    public void testAffirmativeIsSourcePageJobActive() throws ResourceException {
        final ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
        final Connection mockConnection = mock(Connection.class);
        final SchedulerClusteredReconJobDispatch dispatch =
                new SchedulerClusteredReconJobDispatch(mockConnectionFactory, DateUtil.getDateUtil(), mock(Logger.class));
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.query(any(Context.class), any(QueryRequest.class), any(Collection.class))).thenAnswer(new Answer<QueryResponse>() {
            @Override
            public QueryResponse answer(InvocationOnMock invocation) throws Throwable {
                final JsonValue responseContent = json(object(field("_id", dispatch.getClusteredReconJobPreface(RECON_ID))));
                final ResourceResponse resourceResponse = Responses.newResourceResponse("id", "rev", responseContent);
                //ClusteredReconJobDispatch.isSourcePageJobActive will return true only if two jobs corresponding to the
                //current recon job are returned, as one of the jobs will be the source page completion job itself. Thus
                //to ResourceResponse instances are added to the collection.
                ((Collection)invocation.getArguments()[2]).add(resourceResponse);
                ((Collection)invocation.getArguments()[2]).add(resourceResponse);
                return newQueryResponse();
            }
        });
        org.assertj.core.api.Assertions.assertThat(dispatch.isSourcePageJobActive(RECON_ID)).isTrue();
    }

    @Test
    public void testNegativeIsSourcePageJobActive() throws ResourceException {
        final ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
        final Connection mockConnection = mock(Connection.class);
        final SchedulerClusteredReconJobDispatch dispatch =
                new SchedulerClusteredReconJobDispatch(mockConnectionFactory, DateUtil.getDateUtil(), mock(Logger.class));
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.query(any(Context.class), any(QueryRequest.class), any(Collection.class))).thenAnswer(new Answer<QueryResponse>() {

            @Override
            public QueryResponse answer(InvocationOnMock invocation) throws Throwable {
                final JsonValue responseContent = json(object(field("_id", dispatch.getClusteredReconJobPreface(RECON_ID))));
                final ResourceResponse resourceResponse = Responses.newResourceResponse("id", "rev", responseContent);
                //Adding the single job corresponding to a clustered recon job for this recon corresponds to the running
                //source completion job.
                ((Collection)invocation.getArguments()[2]).add(resourceResponse);
                return newQueryResponse();
            }
        });
        org.assertj.core.api.Assertions.assertThat(dispatch.isSourcePageJobActive(RECON_ID)).isFalse();
    }

    private void validateCommonFields(JsonValue createContent) {
        assertThat(createContent).booleanAt("enabled").isEqualTo(true);
        assertThat(createContent).booleanAt("persisted").isEqualTo(true);
        assertThat(createContent).booleanAt("concurrentExecution").isEqualTo(false);
        assertThat(createContent).stringAt("type").isEqualTo("simple");
        assertThat(createContent).stringAt("invokeService").isEqualTo("sync");

        org.assertj.core.api.Assertions.assertThat(createContent.get("invokeContext").asMap().get("action")).isEqualTo("reconcile");
        org.assertj.core.api.Assertions.assertThat(createContent.get("invokeContext").asMap().get("mapping")).isEqualTo(MAPPING_NAME);
        org.assertj.core.api.Assertions.assertThat(createContent.get("invokeContext").asMap().get(RECON_ID_KEY)).isEqualTo(RECON_ID);
    }
}
