/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.config.manage;

import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.QueryFilter;
import org.junit.Test;
import org.testng.Assert;

/**
 * Test class for {@link ConfigObjectService}
 */
public class ConfigObjectServiceTest {

    @Test
    public void testVisitor() throws Exception {
        // Config fields
        String configField = "/field1";
        String nonConfigField = "/service__pid";
        
        // QueryFilter Strings
        String queryString1 = configField + " eq \"value1\"";
        String queryString2 = "" + queryString1 + " and " + nonConfigField + " eq \"value2\"";
        String queryString3 = configField + " pr";
        String queryString4 = configField + " lt 1";
        String queryString5 = "true";
        
        // QueryFilters
        QueryFilter filter1 = QueryFilter.valueOf(queryString1);
        QueryFilter filter2 = QueryFilter.valueOf(queryString2);
        QueryFilter filter3 = QueryFilter.valueOf(queryString3);
        QueryFilter filter4 = QueryFilter.valueOf(queryString4);
        QueryFilter filter5 = QueryFilter.valueOf(queryString5);
        
        // Assertions
        Assert.assertEquals(ConfigObjectService.asConfigQueryFilter(filter1).toString(), "/jsonconfig/field1 eq \"value1\"");
        Assert.assertEquals(ConfigObjectService.asConfigQueryFilter(filter2).toString(), "(/jsonconfig/field1 eq \"value1\" and /service__pid eq \"value2\")");
        Assert.assertEquals(ConfigObjectService.asConfigQueryFilter(filter3).toString(), "/jsonconfig/field1 pr");
        Assert.assertEquals(ConfigObjectService.asConfigQueryFilter(filter4).toString(), "/jsonconfig/field1 lt 1");
        Assert.assertEquals(ConfigObjectService.asConfigQueryFilter(filter5).toString(), "true");
    }
    
    @Test
    public void testParsedResourceName() throws Exception {
        
        ConfigObjectService service = new ConfigObjectService();
        
        try {
            service.getParsedId("");
            Assert.fail("Invalid id: ''");
        } catch (BadRequestException e) {
        }
        try {
            service.getParsedId("//");
            Assert.fail("Invalid id: '//'");
        } catch (IllegalArgumentException e) {
        }
        try {
            service.getParsedId("a/b/c");
            Assert.fail("Invalid id: 'a/b/c'");
        } catch (BadRequestException e) {
        }

        Assert.assertEquals(service.getParsedId("/a"), "a");
        Assert.assertFalse(service.isFactoryConfig("/a"));

        Assert.assertEquals(service.getParsedId("a"), "a");
        Assert.assertFalse(service.isFactoryConfig("a"));

        Assert.assertEquals(service.getParsedId("b/"), "b");
        Assert.assertFalse(service.isFactoryConfig("b/"));

        Assert.assertEquals(service.getParsedId("c/d"), "c-d");
        Assert.assertTrue(service.isFactoryConfig("c/d"));

        Assert.assertEquals(service.getParsedId("e/d/"), "e-d");
        Assert.assertTrue(service.isFactoryConfig("e/d/"));

        Assert.assertEquals(service.getParsedId(" f "), "_f_");
        Assert.assertFalse(service.isFactoryConfig(" f "));

    }
}
