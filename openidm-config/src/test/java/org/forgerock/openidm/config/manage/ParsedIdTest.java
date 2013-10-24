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

import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ResourceName;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class ParsedIdTest {

    @Test
    public void testParsedResourceName() throws Exception {
        try {
            ParsedId id = new ParsedId(ResourceName.valueOf(""));
            Assert.fail("Invalid id: ''");
        } catch (BadRequestException e) {
        }
        try {
            ParsedId id = new ParsedId(ResourceName.valueOf("//"));
            Assert.fail("Invalid id: '//'");
        } catch (IllegalArgumentException e) {
        }
        try {
            ParsedId id = new ParsedId(ResourceName.valueOf("a/b/c"));
            Assert.fail("Invalid id: 'a/b/c'");
        } catch (BadRequestException e) {
        }

        ParsedId id = new ParsedId(ResourceName.valueOf("/a"));
        Assert.assertEquals(id.toString(), "a");
        Assert.assertFalse(id.isFactoryConfig());

        ParsedId a = new ParsedId(ResourceName.valueOf("a"));
        Assert.assertEquals(a.toString(), "a");
        Assert.assertFalse(a.isFactoryConfig());

        ParsedId b = new ParsedId(ResourceName.valueOf("b/"));
        Assert.assertEquals(b.toString(), "b");
        Assert.assertFalse(b.isFactoryConfig());

        ParsedId c = new ParsedId(ResourceName.valueOf("c/d"));
        Assert.assertEquals(c.toString(), "c-d");
        Assert.assertTrue(c.isFactoryConfig());

        ParsedId e = new ParsedId(ResourceName.valueOf("e/d/"));
        Assert.assertEquals(e.toString(), "e-d");
        Assert.assertTrue(e.isFactoryConfig());

        ParsedId f = new ParsedId(ResourceName.valueOf(" f "));
        Assert.assertEquals(f.toString(), "_f_");
        Assert.assertFalse(f.isFactoryConfig());

    }
}
