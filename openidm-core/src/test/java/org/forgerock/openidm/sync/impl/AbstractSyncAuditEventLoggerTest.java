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
package org.forgerock.openidm.sync.impl;

import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;

import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.sync.ReconAction;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AbstractSyncAuditEventLoggerTest {

    public static final String AUTHENTICATION_ID = "principal";
    public static final String TEST_MAPPING = "testMapping";
    private Context context;
    private ObjectMapping.SourceSyncOperation syncOperation;

    @BeforeClass
    public void setup() throws Exception {
        SecurityContext securityContext = new SecurityContext(new RootContext("test_id"), AUTHENTICATION_ID, null);
        context = securityContext;

        syncOperation = mock(ObjectMapping.SourceSyncOperation.class);
        syncOperation.action = ReconAction.CREATE;
        syncOperation.situation = Situation.FOUND;
    }

    @Test
    public void testReconLog() throws Exception {
        // Create event
        ReconAuditEventLogger reconAuditEvent = new ReconAuditEventLogger(syncOperation, TEST_MAPPING, context);
        setCommonFields(reconAuditEvent);
        reconAuditEvent.setEntryType("entryType");
        reconAuditEvent.setReconciliationServiceReconAction(ReconciliationService.ReconAction.reconById);
        reconAuditEvent.setAmbiguousTargetIds(Arrays.asList("a", "b", "c"));
        reconAuditEvent.setReconciling("reconciling");
        reconAuditEvent.setReconId("reconId");

        // Do the common setup and testing.
        JsonValue content = doCommonSetupAndValidation(reconAuditEvent);

        // Now validate the RECON specific stuff.
        String ambiguousIds = content.get(ReconAuditEventBuilder.AMBIGUOUS_TARGET_IDS).asString();
        assertThat(ambiguousIds).isEqualTo("a, b, c");

        String entryType = content.get(ReconAuditEventBuilder.ENTRY_TYPE).asString();
        assertThat(entryType).isEqualTo("entryType");

        ReconciliationService.ReconAction reconAction = content.get(ReconAuditEventBuilder.RECON_ACTION).asEnum(
                ReconciliationService.ReconAction.class);
        assertThat(reconAction).isEqualTo(ReconciliationService.ReconAction.reconById);

        String reconciling = content.get(ReconAuditEventBuilder.RECONCILING).asString();
        assertThat(reconciling).isEqualTo("reconciling");

        String reconId = content.get(ReconAuditEventBuilder.RECON_ID).asString();
        assertThat(reconId).isEqualTo("reconId");

    }

    @Test
    public void testSyncLog() throws Exception {

        SyncAuditEventLogger syncAuditEvent = new SyncAuditEventLogger(syncOperation, TEST_MAPPING, context);
        setCommonFields(syncAuditEvent);

        doCommonSetupAndValidation(syncAuditEvent);
    }

    private void setCommonFields(AbstractSyncAuditEventLogger auditEvent) {
        auditEvent.setStatus(Status.SUCCESS);

        auditEvent.setException(new InternalServerErrorException("test"));
        auditEvent.setLinkQualifier("testLink");
        auditEvent.setMessage("testMessage");
        auditEvent.setMessageDetail(json(object()));
        auditEvent.setSourceObjectId(UUID.randomUUID().toString());
        auditEvent.setTargetObjectId(UUID.randomUUID().toString());
    }

    private JsonValue doCommonSetupAndValidation(AbstractSyncAuditEventLogger auditEvent) throws Exception {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.create(any(Context.class), any(CreateRequest.class))).thenReturn(newResourceResponse("bl", "1", null));

        ArgumentCaptor<CreateRequest> createRequestArgumentCaptor = ArgumentCaptor.forClass(CreateRequest.class);

        auditEvent.log(connectionFactory);
        verify(connection).create(any(Context.class), createRequestArgumentCaptor.capture());

        JsonValue content = createRequestArgumentCaptor.getValue().getContent();

        String mapping = content.get(AbstractSyncAuditEventBuilder.MAPPING).asString();
        assertThat(mapping).isEqualTo(TEST_MAPPING);

        Status status = content.get(AbstractSyncAuditEventBuilder.STATUS).asEnum(Status.class);
        assertThat(status).isEqualTo(Status.SUCCESS);

        ReconAction reconAction = content.get(AbstractSyncAuditEventBuilder.ACTION).asEnum(
                ReconAction.class);
        assertThat(reconAction).isEqualTo(ReconAction.CREATE);

        Situation situation = content.get(AbstractSyncAuditEventBuilder.SITUATION).asEnum(Situation.class);
        assertThat(situation).isEqualTo(Situation.FOUND);

        Object exception = content.get(SyncAuditEventBuilder.EXCEPTION).getObject();
        assertThat(exception).isInstanceOf(InternalServerErrorException.class);

        return content;

    }
}