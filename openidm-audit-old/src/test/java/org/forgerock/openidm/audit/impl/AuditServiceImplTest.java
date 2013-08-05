/*
 * Copyright 2013 ForgeRock, AS.
 *
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
 */
package org.forgerock.openidm.audit.impl;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author brmiller
 */
public class AuditServiceImplTest {

    // some tests for the splitFirstLevel method

    @Test
    public void testLeadingSlash() throws Exception {
        String[] split = AuditServiceImpl.splitFirstLevel("/access/joe");
        Assert.assertEquals("access", split[0]);
        Assert.assertEquals("joe", split[1]);
    }

    @Test
    public void testNoLeadingSlash() throws Exception {
        String[] split = AuditServiceImpl.splitFirstLevel("access/joe");
        Assert.assertEquals("access", split[0]);
        Assert.assertEquals("joe", split[1]);
    }

    @Test
    void testLeadingSlashMissingId() throws Exception {
        String[] split = AuditServiceImpl.splitFirstLevel("/access");
        Assert.assertEquals("access", split[0]);
        Assert.assertEquals(null, split[1]);
    }

    @Test
    void testNoLeadingSlashMissingId() throws Exception {
        String[] split = AuditServiceImpl.splitFirstLevel("access");
        Assert.assertEquals("access", split[0]);
        Assert.assertEquals(null, split[1]);
    }
}
