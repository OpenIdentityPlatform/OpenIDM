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
package org.forgerock.openidm.managed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class CollectionRelationshipProviderTest {
    private ConnectionFactory connectionFactory;
    private RequestHandler handler;
    private CollectionRelationshipProvider provider;

//    @BeforeTest
//    public void setup() {
//        handler = mock(RequestHandler.class);
//        connectionFactory = Resources.newInternalConnectionFactory(handler);
//    }
//
//    @Test
//    public void testReplace() throws ResourceException {
//        assertThat(provider.getRelationshipValueForResource(null, "testId").getOrThrowUninterruptibly().size()).isZero();
//
//    }
//
//    @Test
//    public void testClearCollection() {
//
//    }
}
