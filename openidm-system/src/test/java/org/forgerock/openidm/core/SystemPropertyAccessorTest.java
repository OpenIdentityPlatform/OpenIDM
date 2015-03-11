/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openidm.core;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

/**
 * A NAME does ...
 *
 */
public class SystemPropertyAccessorTest {
    @Test
    public void testGetProperty() throws Exception {

        PropertyAccessor delegate = mock(PropertyAccessor.class);
        PropertyAccessor testable = new SystemPropertyAccessor(delegate);
        System.setProperty("testable", "testvalue");
        assertEquals(testable.getProperty("testable", null, String.class), "testvalue");
        assertEquals(testable.getProperty("testable", "defaultValue", String.class), "testvalue");
        assertEquals(testable.getProperty("pirospaprika", "voroshagyma", null), "voroshagyma");
        assertEquals(testable.getProperty("pirospaprika", "voroshagyma", Object.class),
                "voroshagyma");
        verifyZeroInteractions(delegate);
        assertNull(testable.getProperty(null, null, null));
        verify(delegate, times(1)).getProperty(null, null, null);
        assertNull(testable.getProperty("testable", 1, null));
        verify(delegate, times(1)).getProperty("testable", 1, null);
        assertNull(testable.getProperty("testable", null, Integer.class));
        verify(delegate, times(1)).getProperty("testable", null, Integer.class);
    }
}
