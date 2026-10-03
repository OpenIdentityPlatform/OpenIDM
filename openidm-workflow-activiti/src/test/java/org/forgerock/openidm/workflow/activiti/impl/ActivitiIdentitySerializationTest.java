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

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.forgerock.openidm.crypto.CryptoService;
import org.testng.annotations.Test;

/**
 * {@link JsonUser} and {@link JsonGroup} are {@code Serializable} through the Activiti
 * {@code User}/{@code Group} interfaces; make sure a Java serialization round trip actually works.
 */
public class ActivitiIdentitySerializationTest {

    @Test
    public void jsonUserSurvivesSerializationRoundTrip() throws Exception {
        JsonUser user = new JsonUser(mock(CryptoService.class), "bjensen");
        user.setFirstName("Barbara");
        user.setLastName("Jensen");

        JsonUser copy = roundTrip(user);

        assertNotSame(copy, user);
        assertEquals(copy.getId(), "bjensen");
        assertEquals(copy.getFirstName(), "Barbara");
        assertEquals(copy.getLastName(), "Jensen");
    }

    @Test
    public void jsonGroupSurvivesSerializationRoundTrip() throws Exception {
        JsonGroup group = new JsonGroup("openidm-admin");
        group.setName("Administrators");
        group.setType("security-role");

        JsonGroup copy = roundTrip(group);

        assertNotSame(copy, group);
        assertEquals(copy.getId(), "openidm-admin");
        assertEquals(copy.getName(), "Administrators");
        assertEquals(copy.getType(), "security-role");
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
