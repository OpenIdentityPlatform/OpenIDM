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

package org.forgerock.openidm.auth;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.forgerock.json.resource.Responses.newResourceResponse;

import java.util.Collections;

import org.forgerock.audit.events.AuditEventBuilder;
import org.forgerock.audit.events.AuthenticationAuditEventBuilder;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.CreateRequest;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

public class IDMAuditApiTest {

    @Test
    public void testAudit() throws Exception {

        //given
        final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        final Connection connection = mock(Connection.class);
        final ArgumentCaptor<CreateRequest> createRequestArgumentCaptor = ArgumentCaptor.forClass(CreateRequest.class);
        final IDMAuditApi auditApi = new IDMAuditApi(connectionFactory);

        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.create(any(Context.class), createRequestArgumentCaptor.capture()))
                .thenReturn(newResourceResponse("id", "rev", null));

        final JsonValue jsonValue = JsonValue.json(object(
                field(AuthenticationAuditEventBuilder.PRINCIPAL, array("principal1", "principal2")),
                field(AuthenticationAuditEventBuilder.RESULT, AuthenticationAuditEventBuilder.Status.SUCCESSFUL.name()),
                field(AuthenticationAuditEventBuilder.CONTEXT, object(
                        field("ipAddress", "127.0.0.1"),
                        field("roles", array("openidm-admin"))
                )),
                field(AuthenticationAuditEventBuilder.ENTRIES, array(
                        object(
                                field("entryKey", "entryValue")
                        )
                )),
                field(IDMAuditApi.SESSION_ID, "sessionId")
        ));

        //when
        auditApi.audit(jsonValue);

        //then
        final JsonValue createRequestContent = createRequestArgumentCaptor.getValue().getContent();
        assertThat(createRequestContent.get(AuthenticationAuditEventBuilder.PRINCIPAL).asList())
                .isEqualTo(jsonValue.get(AuthenticationAuditEventBuilder.PRINCIPAL).asList());
        assertThat(createRequestContent.get(AuthenticationAuditEventBuilder.RESULT).asString())
                .isEqualTo(jsonValue.get(AuthenticationAuditEventBuilder.RESULT).asString());
        assertThat(createRequestContent.get(AuthenticationAuditEventBuilder.CONTEXT).asMap())
                .isEqualTo(jsonValue.get(AuthenticationAuditEventBuilder.CONTEXT).asMap());
        assertThat(createRequestContent.get(AuthenticationAuditEventBuilder.ENTRIES).asList())
                .isEqualTo(jsonValue.get(AuthenticationAuditEventBuilder.ENTRIES).asList());
        assertThat(createRequestContent.get(AuditEventBuilder.TRACKING_IDS).asList(String.class))
                .isEqualTo(Collections.singletonList(jsonValue.get(IDMAuditApi.SESSION_ID).asString()));
    }
}
