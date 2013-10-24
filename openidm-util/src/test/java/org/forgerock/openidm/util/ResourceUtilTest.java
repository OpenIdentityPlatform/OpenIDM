/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.util;

import static org.fest.assertions.api.Assertions.assertThat;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class ResourceUtilTest {

    /*
    @Test
    public void testURLParser() throws Exception {
        ResourceUtil.URLParser p0 = ResourceUtil.URLParser.parse("/resourceName/resourceId");
        Assert.assertEquals(p0.index(), 0);
        Assert.assertEquals(p0.value(), "resourceName");
        Assert.assertEquals(p0.resourceCollection(), "/");
        Assert.assertEquals(p0.resourceName(), "/resourceName");
        Assert.assertEquals(p0.first(), p0);

        ResourceUtil.URLParser p1 = p0.next();
        Assert.assertEquals(p1.index(), 1);
        Assert.assertEquals(p1.value(), "resourceId");
        Assert.assertEquals(p1.resourceCollection(), "/resourceName");
        Assert.assertEquals(p1.resourceName(), "/resourceName/resourceId");
        Assert.assertEquals(p1.first(), p0);
        Assert.assertEquals(p0.last(), p1);
        Assert.assertEquals(p1.last(), p1);

        p0 = ResourceUtil.URLParser.parse("/resourceName/");
        Assert.assertEquals(p0.index(), 0);
        Assert.assertEquals(p0.value(), "resourceName");
        Assert.assertEquals(p0.resourceCollection(), "/");
        Assert.assertEquals(p0.resourceName(), "/resourceName");
        Assert.assertEquals(p0.first(), p0);

        p1 = p0.next();
        Assert.assertEquals(p1, p0);

        p0 = ResourceUtil.URLParser.parse("/");
        Assert.assertEquals(p0.index(), 0);
        Assert.assertEquals(p0.value(), "");
        Assert.assertEquals(p0.resourceCollection(), "/");
        Assert.assertEquals(p0.resourceName(), "/");
        Assert.assertEquals(p0.first(), p0);

        p1 = p0.next();
        Assert.assertEquals(p1, p0);
    }
    */
}
