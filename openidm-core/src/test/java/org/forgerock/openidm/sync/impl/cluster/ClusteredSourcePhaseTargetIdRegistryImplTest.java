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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.Context;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.testng.annotations.Test;

public class ClusteredSourcePhaseTargetIdRegistryImplTest {
    @Test
    public void testGetTargetPhaseIdsNoneReconciled() throws ResourceException {
        //given
        final ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
        final Connection mockConnection = mock(Connection.class);
        final List<String> targetIds = getTargetIdsList();
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.query(any(Context.class), any(QueryRequest.class), any(Collection.class))).thenAnswer(new Answer<QueryResponse>() {
            @Override
            public QueryResponse answer(InvocationOnMock invocation) throws Throwable {
                ((Collection)invocation.getArguments()[2]).clear();
                return newQueryResponse();
            }
        });
        //when
        final ClusteredSourcePhaseTargetIdRegistry registry =
                new ClusteredSourcePhaseTargetIdRegistryImpl(mockConnectionFactory, mock(Logger.class));
        final Collection<String> toBeReconciled = registry.getTargetPhaseIds("reconId", targetIds);
        assertThat(targetIds.containsAll(toBeReconciled)).isTrue();
    }

    @Test
    public void testGetTargetPhaseIdsAllReconciled() throws ResourceException {
        //given
        final ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
        final Connection mockConnection = mock(Connection.class);
        final List<String> targetIds = getTargetIdsList();
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.query(any(Context.class), any(QueryRequest.class), any(Collection.class))).thenAnswer(new Answer<QueryResponse>() {
            @Override
            public QueryResponse answer(InvocationOnMock invocation) throws Throwable {
                ((Collection)invocation.getArguments()[2]).addAll(targetIds);
                return newQueryResponse();
            }
        });
        //when
        final ClusteredSourcePhaseTargetIdRegistry registry =
                new ClusteredSourcePhaseTargetIdRegistryImpl(mockConnectionFactory, mock(Logger.class));
        final Collection<String> toBeReconciled = registry.getTargetPhaseIds("reconId", targetIds);
        assertThat(toBeReconciled.isEmpty()).isTrue();
    }

    private List<String> getTargetIdsList() {
        final List<String> targetIds = new ArrayList<>();
        targetIds.add("firstId");
        targetIds.add("secondId");
        targetIds.add("thirdId");
        return targetIds;
    }
}
