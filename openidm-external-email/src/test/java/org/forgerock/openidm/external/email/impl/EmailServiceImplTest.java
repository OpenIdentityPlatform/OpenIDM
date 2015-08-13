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
package org.forgerock.openidm.external.email.impl;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.Test;

public class EmailServiceImplTest {

    public static final String RESOURCE_PATH = "resourcePath";
    public static final String STATUS = "status";
    public static final String OK = "OK";

    @Test
    public void testSuccessfulActionInstance() throws Exception {
        // given
        final EmailClient emailClient = mock(EmailClient.class);
        final EmailServiceImpl emailService = new EmailServiceImpl();
        final ActionRequest actionRequest = mock(ActionRequest.class);

        emailService.emailClient = emailClient;
        doNothing().when(emailClient).send(any(JsonValue.class));
        when(actionRequest.getResourcePath()).thenReturn(RESOURCE_PATH);
        when(actionRequest.getContent()).thenReturn(json(object()));

        // when
        Promise<ActionResponse, ResourceException> promise =
                emailService.actionInstance(mock(Context.class), actionRequest);

        // then
        ActionResponse expectedResponse = Responses.newActionResponse(JsonValue.json(object(
                field(STATUS, OK)
        )));
        assertThat(promise).succeeded().isInstanceOf(ActionResponse.class).isEqualTo(expectedResponse);
    }

    @Test
    public void testFailedActionInstance() throws Exception {
        // given
        final EmailClient emailClient = mock(EmailClient.class);
        final EmailServiceImpl emailService = new EmailServiceImpl();
        final ActionRequest actionRequest = mock(ActionRequest.class);

        emailService.emailClient = emailClient;
        doThrow(new BadRequestException()).when(emailClient).send(any(JsonValue.class));
        when(actionRequest.getResourcePath()).thenReturn(RESOURCE_PATH);
        when(actionRequest.getContent()).thenReturn(json(object()));

        // when
        Promise<ActionResponse, ResourceException> promise =
                emailService.actionInstance(mock(Context.class), actionRequest);

        // then
        assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void testPatchInstanceForbidden() throws Exception {
        // given
        final EmailServiceImpl emailService = new EmailServiceImpl();

        // when
        Promise<ResourceResponse, ResourceException> promise =
                emailService.patchInstance(mock(Context.class), mock(PatchRequest.class));

        // then
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void testReadInstanceForbidden() throws Exception {
        // given
        final EmailServiceImpl emailService = new EmailServiceImpl();

        // when
        Promise<ResourceResponse, ResourceException> promise =
                emailService.readInstance(mock(Context.class), mock(ReadRequest.class));

        // then
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void testUpdateInstanceForbidden() throws Exception {
        // given
        final EmailServiceImpl emailService = new EmailServiceImpl();

        // when
        Promise<ResourceResponse, ResourceException> promise =
                emailService.updateInstance(mock(Context.class), mock(UpdateRequest.class));

        // then
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
    }
}
