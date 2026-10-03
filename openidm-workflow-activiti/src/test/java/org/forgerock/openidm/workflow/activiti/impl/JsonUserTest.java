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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openidm.workflow.activiti.impl;

import static org.forgerock.json.JsonValue.json;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.crypto.CryptoService;
import org.testng.annotations.Test;

/**
 * Tests for {@link JsonUser#clone()}.
 */
public class JsonUserTest {

    @Test
    public void cloneKeepsTypeStateAndCryptoService() throws Exception {
        CryptoService cryptoService = mock(CryptoService.class);
        when(cryptoService.decrypt(any(JsonValue.class))).thenReturn(json("secret"));
        JsonUser user = new JsonUser(cryptoService, "bjensen");
        user.setFirstName("Barbara");
        user.setPassword("encrypted");

        JsonUser copy = user.clone();

        assertNotSame(copy, user);
        assertEquals(copy.getId(), "bjensen");
        assertEquals(copy.getFirstName(), "Barbara");
        assertEquals(copy.getPassword(), "secret", "the crypto service must be carried over");
    }

    @Test
    public void cloneIsIndependentAtTheTopLevel() {
        JsonUser user = new JsonUser(mock(CryptoService.class), "bjensen");

        JsonUser copy = user.clone();
        copy.setId("other");
        copy.remove(SharedIdentityService.SCIM_USERNAME);

        assertEquals(user.getId(), "bjensen");
        assertNull(copy.get(SharedIdentityService.SCIM_USERNAME).getObject());
    }
}
