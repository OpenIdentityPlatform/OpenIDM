/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 * $Id$
 */
package org.forgerock.commons.json.schema.validator.helpers;

import org.forgerock.commons.json.schema.validator.helpers.MinimumHelper;
import org.forgerock.commons.json.schema.validator.TestErrorHandler;
import org.forgerock.commons.json.schema.validator.exceptions.ValidationException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MinimumHelperTest {
    @Test
    public void testValidateFloat() throws Exception {

        MinimumHelper instance = new MinimumHelper(new Float("99.03"), true);
        ValidationException exception = null;
        TestErrorHandler handler = new TestErrorHandler();
        instance.validate(new Float("100.01"), "$", handler);
        Assert.assertTrue(handler.getExceptions().isEmpty());
        instance.validate(new Double("100.02"), "$", handler);
        Assert.assertTrue(handler.getExceptions().isEmpty());
        instance.validate(100, "$", handler);
        Assert.assertTrue(handler.getExceptions().isEmpty());
        //Exceptions
        instance.validate(new Float("95.01"), "$", handler);
        Assert.assertTrue(handler.getExceptions().size() == 1);
        instance.validate(new Double("95.02"), "$", handler);
        Assert.assertTrue(handler.getExceptions().size() == 2);
        instance.validate(95, "$", handler);
        Assert.assertTrue(handler.getExceptions().size() == 3);
        instance.validate(new Float("99.03"), "$", handler);
        Assert.assertTrue(handler.getExceptions().size() == 4);
        instance.validate(new Double("99.03"), "$", handler);
        Assert.assertTrue(handler.getExceptions().size() == 5);
    }

    @Test
    public void testValidateDouble() throws Exception {

        MinimumHelper instance = new MinimumHelper(new Double("99.03"), false);
        ValidationException exception = null;
        TestErrorHandler handler = new TestErrorHandler();
        instance.validate(new Float("100.01"), "$", handler);
        Assert.assertTrue(handler.getExceptions().isEmpty());
        instance.validate(new Double("100.02"), "$", handler);
        Assert.assertTrue(handler.getExceptions().isEmpty());
        instance.validate(100, "$", handler);
        Assert.assertTrue(handler.getExceptions().isEmpty());
        //Exceptions
        instance.validate(new Float("95.01"), "$", handler);
        Assert.assertTrue(handler.getExceptions().size() == 1);
        instance.validate(new Double("95.02"), "$", handler);
        Assert.assertTrue(handler.getExceptions().size() == 2);
        instance.validate(95, "$", handler);
        Assert.assertTrue(handler.getExceptions().size() == 3);
//        1 == new Double("99.03").compareTo((new Float("99.03")).doubleValue());
//        instance.validate(new Float("99.03"), "$", handler);
//        Assert.assertTrue(handler.getExceptions().size() == 3);
        instance.validate(new Double("99.03"), "$", handler);
        Assert.assertTrue(handler.getExceptions().size() == 3);
    }
}
