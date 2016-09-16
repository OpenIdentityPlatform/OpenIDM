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

package org.forgerock.openidm.sync.impl;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.forgerock.guava.common.base.Optional;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.sync.impl.cluster.ClusteredReconJobDispatch;
import org.forgerock.openidm.sync.impl.cluster.ClusteredSourcePhaseTargetIdRegistry;
import org.forgerock.openidm.sync.impl.cluster.ReconciliationStatisticsPersistence;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.openidm.util.Scripts;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.services.TransactionId;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.TransactionIdContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class ClusteredReconTest {
    class TestReconciliationContext extends ReconciliationContext {
        TestReconciliationContext(
                ReconciliationService.ReconAction reconAction,
                ObjectMapping mapping,
                Context callingContext,
                JsonValue reconParams,
                JsonValue overridingConfig,
                ReconciliationService service) throws BadRequestException {
            super(reconAction, mapping, callingContext, reconParams, overridingConfig, service);
        }
        //The ReconciliationContext calls this method from the ctor, so it cannot leverage state set in the TestReconciliationContext ctor.
        //Yet the pagingCookie returned from the ReconTypeHandler determines whether the source recon is done, and thus must be configured
        //for various tests. The same applies for whether the target phase is run. Thus I need to reference a static thread-locals to obtain
        //this state - a static Provider would not work on multi-threaded tests.
        @Override
        protected ReconTypeHandler createReconTypeHandler(ReconciliationService.ReconAction reconAction) throws BadRequestException {
            try {
                return newReconTypeHandler(newResultIterable(), pagingCookieThreadLocal.get(), clusteredReconEnabledThreadLocal.get());
            } catch (SynchronizationException e) {
                throw new BadRequestException(e.getMessage(), e);
            }
        }

        @Override
        ResultIterable queryTarget() throws SynchronizationException {
            return newResultIterable();
        }
    }
    private static final String RECON_ID = "1234";
    private static final String PAGING_COOKIE = "asdf";
    private static final String RESULT_ID = "da_id";
    private static final String MAPPING_NAME = "managedUser_systemLdapAccounts";
    private static final boolean TARGET_PHASE_ENABLED = true;
    private static ThreadLocal<String> pagingCookieThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<Boolean> clusteredReconEnabledThreadLocal = new ThreadLocal<>();
    private Script mockScript;
    private Bindings mockBindings;
    private ScriptEntry mockScriptEntry;
    private ScriptRegistry mockScriptRegistry;

    private ClusteredRecon newClusteredRecon(ObjectMapping objectMapping,
                                             ReconciliationContext reconContext,
                                             ClusteredSourcePhaseTargetIdRegistry clusteredSourcePhaseTargetIdRegistry,
                                             ClusteredReconJobDispatch clusteredReconJobDispatch,
                                             ReconciliationStatisticsPersistence reconciliationStatisticsPersistence) {
        return new ClusteredRecon(objectMapping, reconContext, clusteredSourcePhaseTargetIdRegistry,
                clusteredReconJobDispatch, reconciliationStatisticsPersistence);
    }

    private ObjectMapping newObjectMapping(ConnectionFactory connectionFactory, JsonValue syncConfig) {
        final ObjectMapping mapping =  new ObjectMapping(connectionFactory, syncConfig);
        mapping.initRelationships(Collections.singletonList(mapping));
        return mapping;
    }

    private JsonValue newSyncConfig() {
        return json(object(
                field("name", MAPPING_NAME),
                field("source", "managed/user"),
                field("target", "system/ldap/account"),
                field("clusteredSourceReconEnabled", true),
                field("reconSourceQueryPaging", true),
                field("enableLinking", false)));
    }

    private ConnectionFactory newConnectionFactory() throws ResourceException {
        final Connection mockConnection = mock(Connection.class);
        final ResourceResponse cannedResponse = Responses.newResourceResponse("id1", "0", userResult());
        when(mockConnection.read(any(Context.class), any(ReadRequest.class))).thenReturn(cannedResponse);
        when(mockConnection.create(any(Context.class), any(CreateRequest.class))).thenReturn(cannedResponse);
        final ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        return mockConnectionFactory;
    }

    private ReconciliationContext newReconContext(ObjectMapping mapping, JsonValue reconParams) throws BadRequestException {
        return new TestReconciliationContext(ReconciliationService.ReconAction.recon, mapping, ObjectSetContext.get(),
                reconParams, json(object()), mock(ReconciliationService.class));
    }

    private JsonValue getReconInitiatedParams() {
        return json(object());
    }

    private JsonValue getReconSourcePageParams() {
        return json(object(
                field(ClusteredReconJobDispatch.RECON_ID_KEY, RECON_ID),
                field(ClusteredReconJobDispatch.PAGING_COOKIE_KEY, pagingCookieThreadLocal.get()),
                field(ClusteredReconJobDispatch.CLUSTERED_SUB_ACTION_KEY,
                        ClusteredReconJobDispatch.CLUSTERED_NEXT_PAGE_ACTION_PARAM)));
    }

    private JsonValue getReconSourceCompletionParams() {
        return json(object(
                field(ClusteredReconJobDispatch.RECON_ID_KEY, RECON_ID),
                field(ClusteredReconJobDispatch.PAGING_COOKIE_KEY, pagingCookieThreadLocal.get()),
                field(ClusteredReconJobDispatch.CLUSTERED_SUB_ACTION_KEY,
                        ClusteredReconJobDispatch.CLUSTERED_SOURCE_COMPLETION_PARAM)));
    }

    private JsonValue getReconTargetPhaseParams() {
        return json(object(
                field(ClusteredReconJobDispatch.RECON_ID_KEY, RECON_ID),
                field(ClusteredReconJobDispatch.PAGING_COOKIE_KEY, pagingCookieThreadLocal.get()),
                field(ClusteredReconJobDispatch.CLUSTERED_SUB_ACTION_KEY,
                        ClusteredReconJobDispatch.CLUSTERED_TARGET_PHASE_PARAM)));
    }

    private ReconTypeHandler newReconTypeHandler(final ResultIterable resultIterable, String pagingCookie, boolean runTargetPhase) throws SynchronizationException {
        final ReconTypeHandler mockHandler = mock(ReconTypeHandler.class);
        when(mockHandler.getReconParameters()).thenReturn(getReconSourcePageParams());
        when(mockHandler.querySource(anyInt(), anyString())).thenReturn(new ReconQueryResult(resultIterable, pagingCookie));
        when(mockHandler.isRunTargetPhase()).thenReturn(runTargetPhase);
        return mockHandler;
    }

    private ResultIterable newResultIterable() {
        return new ResultIterable(getAllResultIds(), getAllResultValues());
    }

    private Collection<String> getAllResultIds() {
        return Collections.singletonList(RESULT_ID);
    }

    private Collection<JsonValue> getAllResultValues() {
        return Collections.singletonList(userResult());
    }

    private JsonValue userResult() {
        return json(object(
                field("_id", RESULT_ID),
                field("_rev", "1"),
                field("displayName", "Fred"),
                field("description", "a canned user"),
                field("givenName", "Freddy"),
                field("mail", "fred@company.com"),
                field("sn", "222"),
                field("userName", "fredman"),
                field("accountStatus", "active")));
    }

    private ReconciliationStatisticsPersistence newMockReconStatsPersistence() throws SynchronizationException {
        final ReconciliationStatisticsPersistence persistence = mock(ReconciliationStatisticsPersistence.class);
        when(persistence.getAggregatedInstance(any(String.class))).thenReturn(Optional.<ReconciliationStatistic>absent());
        return persistence;
    }

    @BeforeTest
    public void initCommonDependencies() throws ScriptException {
        mockScript = mock(Script.class);
        mockBindings = mock(Bindings.class);
        when(mockScript.eval(any(Bindings.class))).thenReturn(true);
        when(mockScript.createBindings()).thenReturn(mockBindings);
        mockScriptEntry = mock(ScriptEntry.class);
        when(mockScriptEntry.getScript(any(Context.class))).thenReturn(mockScript);
        mockScriptRegistry = mock(ScriptRegistry.class);
        when(mockScriptRegistry.takeScript(any(JsonValue.class))).thenReturn(mockScriptEntry);
        Scripts.init(mockScriptRegistry);
    }

    @BeforeMethod
    public void pushObjectSetContext() {
        //recon entry points push/pop the Context onto the ObjectSetContext ThreadLocal. Do the same before/after each test
        ObjectSetContext.push(new TransactionIdContext(ContextUtil.createInternalContext(), new TransactionId()));
    }

    @AfterMethod
    public void popObjectSetContext() {
        //recon entry points push/pop the Context onto the ObjectSetContext ThreadLocal. Do the same before/after each test
        ObjectSetContext.pop();
    }

    @Test
    public void testClusteredReconInitiationNoNextPage() throws ResourceException {
        //given
        final ObjectMapping objectMapping = newObjectMapping(newConnectionFactory(), newSyncConfig());
        pagingCookieThreadLocal.set(null);
        clusteredReconEnabledThreadLocal.set(TARGET_PHASE_ENABLED);
        final ReconciliationContext reconContext = newReconContext(objectMapping, getReconInitiatedParams());
        final ClusteredSourcePhaseTargetIdRegistry registry = mock(ClusteredSourcePhaseTargetIdRegistry.class);
        final ClusteredReconJobDispatch dispatch = mock(ClusteredReconJobDispatch.class);
        final ReconciliationStatisticsPersistence reconStatsPersist = newMockReconStatsPersistence();
        //when
        newClusteredRecon(objectMapping, reconContext, registry, dispatch, reconStatsPersist).dispatchClusteredRecon();
        //then
        verify(dispatch, never()).dispatchSourcePageRecon(any(String.class), any(String.class), any(String.class));
        verify(dispatch).dispatchSourcePhaseCompletionTask(any(String.class), eq(MAPPING_NAME));
        //can't test for equality on RECON_ID, as this will be generated the first time from the context value
        verify(registry).persistTargetIds(any(String.class));
        verify(reconStatsPersist).persistInstance(any(String.class), any(ReconciliationStatistic.class));
        verify(reconStatsPersist, never()).getAggregatedInstance(any(String.class));
        verify(reconStatsPersist, never()).deletePersistedInstances(any(String.class));
    }

    @Test
    public void testOngoingClusteredRecon() throws ResourceException {
        //given
        final ObjectMapping objectMapping = newObjectMapping(newConnectionFactory(), newSyncConfig());
        pagingCookieThreadLocal.set(PAGING_COOKIE);
        clusteredReconEnabledThreadLocal.set(TARGET_PHASE_ENABLED);
        final ReconciliationContext reconContext = newReconContext(objectMapping, getReconSourcePageParams());
        final ClusteredSourcePhaseTargetIdRegistry registry = mock(ClusteredSourcePhaseTargetIdRegistry.class);
        final ClusteredReconJobDispatch dispatch = mock(ClusteredReconJobDispatch.class);
        final ReconciliationStatisticsPersistence reconStatsPersist = newMockReconStatsPersistence();
        //when
        newClusteredRecon(objectMapping, reconContext, registry, dispatch, reconStatsPersist).dispatchClusteredRecon();
        //then
        verify(registry).persistTargetIds(eq(RECON_ID));
        verify(dispatch).dispatchSourcePageRecon(eq(RECON_ID), eq(MAPPING_NAME), eq(PAGING_COOKIE));
        verify(dispatch, never()).dispatchSourcePhaseCompletionTask(any(String.class), eq(MAPPING_NAME));
        verify(reconStatsPersist).persistInstance(any(String.class), any(ReconciliationStatistic.class));
        verify(reconStatsPersist, never()).getAggregatedInstance(any(String.class));
        verify(reconStatsPersist, never()).deletePersistedInstances(any(String.class));
    }

    @Test(expectedExceptions = SynchronizationException.class)
    public void testOngoingClusteredReconNoTargetPhase() throws ResourceException {
        //given
        final ObjectMapping objectMapping = newObjectMapping(newConnectionFactory(), newSyncConfig());
        // there is no paging cookie, but the reconContext is created with params that indicate that another source
        // page should be run - this should generate an exception
        pagingCookieThreadLocal.set(null);
        clusteredReconEnabledThreadLocal.set(!TARGET_PHASE_ENABLED);
        final ReconciliationContext reconContext = newReconContext(objectMapping, getReconSourcePageParams());
        final ClusteredSourcePhaseTargetIdRegistry registry = mock(ClusteredSourcePhaseTargetIdRegistry.class);
        final ClusteredReconJobDispatch dispatch = mock(ClusteredReconJobDispatch.class);
        //when
        newClusteredRecon(objectMapping, reconContext, registry, dispatch, newMockReconStatsPersistence()).dispatchClusteredRecon();
    }

    @Test
    public void testSourceCompletionWithTargetPhase() throws ResourceException {
        //given
        final ObjectMapping objectMapping = newObjectMapping(newConnectionFactory(), newSyncConfig());
        pagingCookieThreadLocal.set(PAGING_COOKIE);
        clusteredReconEnabledThreadLocal.set(TARGET_PHASE_ENABLED);
        final ReconciliationContext reconContext = newReconContext(objectMapping, getReconSourceCompletionParams());
        final ClusteredSourcePhaseTargetIdRegistry registry = mock(ClusteredSourcePhaseTargetIdRegistry.class);
        final ClusteredReconJobDispatch dispatch = mock(ClusteredReconJobDispatch.class);
        final ReconciliationStatisticsPersistence reconStatsPersist = newMockReconStatsPersistence();
        //when
        newClusteredRecon(objectMapping, reconContext, registry, dispatch, reconStatsPersist).dispatchClusteredRecon();
        //then
        verify(registry, never()).persistTargetIds(any(String.class));
        verify(dispatch, never()).dispatchSourcePageRecon(eq(RECON_ID), eq(MAPPING_NAME), eq(PAGING_COOKIE));
        verify(dispatch, never()).dispatchSourcePhaseCompletionTask(eq(RECON_ID), eq(MAPPING_NAME));
        verify(dispatch).dispatchTargetPhase(eq(RECON_ID), eq(MAPPING_NAME));
        verify(reconStatsPersist, never()).persistInstance(any(String.class), any(ReconciliationStatistic.class));
        verify(reconStatsPersist, never()).getAggregatedInstance(any(String.class));
        verify(reconStatsPersist, never()).deletePersistedInstances(any(String.class));
    }

    @Test
    public void testSourceCompletionNoTargetPhase() throws ResourceException {
        //given
        final ObjectMapping objectMapping = newObjectMapping(newConnectionFactory(), newSyncConfig());
        pagingCookieThreadLocal.set(PAGING_COOKIE);
        clusteredReconEnabledThreadLocal.set(!TARGET_PHASE_ENABLED);
        final ReconciliationContext reconContext = newReconContext(objectMapping, getReconSourceCompletionParams());
        final ClusteredSourcePhaseTargetIdRegistry registry = mock(ClusteredSourcePhaseTargetIdRegistry.class);
        final ClusteredReconJobDispatch dispatch = mock(ClusteredReconJobDispatch.class);
        final ReconciliationStatisticsPersistence reconStatsPersist = newMockReconStatsPersistence();
        //when
        newClusteredRecon(objectMapping, reconContext, registry, dispatch, reconStatsPersist).dispatchClusteredRecon();
        //then
        verify(registry, never()).persistTargetIds(any(String.class));
        verify(dispatch, never()).dispatchSourcePageRecon(eq(RECON_ID), eq(MAPPING_NAME), eq(PAGING_COOKIE));
        verify(dispatch, never()).dispatchSourcePhaseCompletionTask(eq(RECON_ID), eq(MAPPING_NAME));
        verify(dispatch, never()).dispatchTargetPhase(eq(RECON_ID), eq(MAPPING_NAME));
        verify(reconStatsPersist).persistInstance(any(String.class), any(ReconciliationStatistic.class));
        verify(reconStatsPersist).getAggregatedInstance(any(String.class));
        verify(reconStatsPersist).deletePersistedInstances(any(String.class));
    }

    @Test
    public void testTargetPhase() throws ResourceException {
        //given
        final ObjectMapping objectMapping = newObjectMapping(newConnectionFactory(), newSyncConfig());
        pagingCookieThreadLocal.set(PAGING_COOKIE);
        clusteredReconEnabledThreadLocal.set(TARGET_PHASE_ENABLED);
        final ReconciliationContext reconContext = newReconContext(objectMapping, getReconTargetPhaseParams());
        final ClusteredSourcePhaseTargetIdRegistry registry = mock(ClusteredSourcePhaseTargetIdRegistry.class);
        final ClusteredReconJobDispatch dispatch = mock(ClusteredReconJobDispatch.class);
        final ReconciliationStatisticsPersistence reconStatsPersist = newMockReconStatsPersistence();
        //when
        newClusteredRecon(objectMapping, reconContext, registry, dispatch, reconStatsPersist).dispatchClusteredRecon();
        //then
        verify(registry).getTargetPhaseIds(eq(RECON_ID), anyCollection());
        verify(registry).deletePersistedTargetIds(eq(RECON_ID));
        verify(dispatch, never()).dispatchSourcePageRecon(eq(RECON_ID), eq(MAPPING_NAME), eq(PAGING_COOKIE));
        verify(dispatch, never()).dispatchSourcePhaseCompletionTask(eq(RECON_ID), eq(MAPPING_NAME));
        verify(dispatch, never()).dispatchTargetPhase(eq(RECON_ID), eq(MAPPING_NAME));
        verify(reconStatsPersist).persistInstance(any(String.class), any(ReconciliationStatistic.class));
        verify(reconStatsPersist).getAggregatedInstance(any(String.class));
        verify(reconStatsPersist).deletePersistedInstances(any(String.class));
    }
}
