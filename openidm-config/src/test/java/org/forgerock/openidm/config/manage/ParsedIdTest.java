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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.config.manage;

import org.forgerock.openidm.objset.BadRequestException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class ParsedIdTest {
    @Test
    public void testParsedId() throws Exception {
        try {
            ParsedId id = new ParsedId("");
            Assert.fail("Invalid id: ''");
        } catch (BadRequestException e) {
        }
        try {
            ParsedId id = new ParsedId("//");
            Assert.fail("Invalid id: '//'");
        } catch (BadRequestException e) {
        }
        try {
            ParsedId id = new ParsedId("a/b/c");
            Assert.fail("Invalid id: 'a/b/c'");
        } catch (BadRequestException e) {
        }
        try {
            ParsedId id = new ParsedId("/a");
            Assert.fail("Invalid id: ''");
        } catch (BadRequestException e) {
        }
        ParsedId a = new ParsedId("a");
        Assert.assertEquals(a.toString(), "a");
        Assert.assertFalse(a.isFactoryConfig());

        ParsedId b = new ParsedId("b/");
        Assert.assertEquals(b.toString(), "b");
        Assert.assertFalse(b.isFactoryConfig());

        ParsedId c = new ParsedId("c/d");
        Assert.assertEquals(c.toString(), "c-d");
        Assert.assertTrue(c.isFactoryConfig());

        ParsedId e = new ParsedId("e/d/");
        Assert.assertEquals(e.toString(), "e-d");
        Assert.assertTrue(e.isFactoryConfig());

        ParsedId f = new ParsedId(" f ");
        Assert.assertEquals(f.toString(), "f");
        Assert.assertFalse(f.isFactoryConfig());

    }
}
