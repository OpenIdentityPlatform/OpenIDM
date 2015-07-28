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

package org.forgerock.openidm.script.impl;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ServerContext;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

public class ScriptRegistryServiceTest {

    @Test
    public void testAuditScheduledService() throws Exception {
        //given
        final ScriptRegistryService scriptRegistryService = new ScriptRegistryService();
        final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        final Connection connection = mock(Connection.class);
        final ServerContext serverContext = mock(ServerContext.class);
        final AuditEvent auditEvent = mock(AuditEvent.class);
        final ArgumentCaptor<CreateRequest> argumentCaptor = ArgumentCaptor.forClass(CreateRequest.class);

        scriptRegistryService.setConnectionFactory(connectionFactory);

        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.create(any(Context.class), argumentCaptor.capture()))
                .thenReturn(new Resource("id", "rev", null));
        when(auditEvent.getValue()).thenReturn(json(object()));

        //when
        scriptRegistryService.auditScheduledService(serverContext, auditEvent);

        //then
        verify(connection).create(any(Context.class), any(CreateRequest.class));
        assertThat(argumentCaptor.getValue().getContent()).isEqualTo(auditEvent.getValue());
        assertThat(argumentCaptor.getValue().getResourceName()).isEqualTo("audit/access");
    }
}
