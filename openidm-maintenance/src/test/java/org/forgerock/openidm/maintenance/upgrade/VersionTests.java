/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
 * the specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file
 * and include the License file at legal/CDDLv1.0.txt. If applicable, add the following
 * below the CDDL Header, with the fields enclosed by brackets [] replaced by your
 * own identifying information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
 * Portions copyright 2013-2017 ForgeRock AS.
 */
package org.forgerock.openidm.maintenance.upgrade;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class VersionTests {

    @Test
    public void testParse() {
        assertEquals(new Version(1), Version.parse("1"));
        assertEquals(new Version(1), Version.parse("1-alpha"));
        assertEquals(new Version(1, 2), Version.parse("1.2"));
        assertEquals(new Version(1, 2), Version.parse("1.2-alpha"));
        assertEquals(new Version(1, 2, 3), Version.parse("1.2.3"));
        assertEquals(new Version(1, 2, 3), Version.parse("1.2.3-alpha"));
        assertEquals(new Version(1, 2, 3, 4), Version.parse("1.2.3.4"));
        assertEquals(new Version(1, 2, 3, 4), Version.parse("1.2.3.4-alpha"));
    }

    @Test
    public void testCompare() {
        assertCompEq(new Version(1), Version.parse("1"));
        assertCompEq(new Version(1, 2), Version.parse("1.2"));
        assertCompEq(new Version(1, 2, 3), Version.parse("1.2.3"));
        assertCompEq(new Version(1, 2, 3, 4), Version.parse("1.2.3.4"));

        assertCompEq(new Version(1), Version.parse("1.0"));
        assertCompEq(new Version(1), Version.parse("1.0.0"));
        assertCompEq(new Version(1), Version.parse("1.0.0.0"));
        assertCompEq(new Version(1, 2), Version.parse("1.2.0"));
        assertCompEq(new Version(1, 2), Version.parse("1.2.0.0"));
        assertCompEq(new Version(1, 2, 3), Version.parse("1.2.3.0"));

        assertCompLt(new Version(1, 2), Version.parse("1.3"));
        assertCompLt(new Version(1, 2, 3), Version.parse("1.2.4"));
        assertCompLt(new Version(1, 2, 3, 4), Version.parse("1.2.3.5"));

        assertCompLt(new Version(2), Version.parse("3"));
        assertCompLt(new Version(2), Version.parse("2.1"));
        assertCompLt(new Version(2, 3), Version.parse("2.3.4"));
        assertCompLt(new Version(2, 3, 4), Version.parse("2.3.4.5"));

        assertCompGt(new Version(1, 2), Version.parse("1.1"));
        assertCompGt(new Version(1, 2, 3), Version.parse("1.2.2"));
        assertCompGt(new Version(1, 2, 3, 4), Version.parse("1.2.3.3"));

        assertCompGt(new Version(2), Version.parse("1"));
        assertCompGt(new Version(2), Version.parse("1.0"));
        assertCompGt(new Version(2, 3), Version.parse("2.2.0"));
        assertCompGt(new Version(2, 3, 4), Version.parse("2.3.3.0"));
    }

    @Test
    public void testQualifiedIgnored() {
        assertEquals(Version.parse("1.2.3"), Version.parse("1.2.3-alpha"));
    }

    @Test
    public void testComponents() {
        Version v = Version.parse("1.2.3.4");
        assertEquals(v.getMajor(), Integer.valueOf(1));
        assertEquals(v.getMinor(), Integer.valueOf(2));
        assertEquals(v.getMicro(), Integer.valueOf(3));
        assertEquals(v.getRevision(), Integer.valueOf(4));
    }

    @Test
    public void testCornerCases() {
        Version v = Version.parse("1.0");
        assertEquals(v.getMajor(), Integer.valueOf(1));
        assertEquals(v.getMinor(), Integer.valueOf(0));
        assertNull(v.getMicro());
        assertNull(v.getRevision());

        try {
            new Version();
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // OK.
        }

        try {
            Version.parse(" ");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // OK.
        }

        try {
            Version.parse("foo");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // OK.
        }
    }

    @Test
    public void testEqualsHashCode() {
        Version v1 = Version.parse("1.0");
        Version v2 = Version.parse("1.0.0.0");
        assertEquals(v1, v2);
        assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals(Version.parse("1.2.3").toString(), "Version[1.2.3]");
    }

    private void assertCompEq(Version v1, Version v2) {
        assertTrue(v1.compareTo(v2) == 0);
    }

    private void assertCompLt(Version v1, Version v2) {
        assertTrue(v1.compareTo(v2) < 0);
    }

    private void assertCompGt(Version v1, Version v2) {
        assertTrue(v1.compareTo(v2) > 0);
    }
}
