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
package org.forgerock.commons.json.schema.validator;

import org.forgerock.commons.json.schema.validator.ObjectValidatorFactory;
import org.forgerock.commons.json.schema.validator.Constants;
import junit.framework.Assert;
import org.forgerock.commons.json.schema.validator.validators.*;
import org.testng.annotations.Test;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectValidatorFactoryTest {
    @Test
    public void testGetTypeValidatorBySchema() throws Exception {
        Map<String, Object> schema = new HashMap<String, Object>();
        Assert.assertTrue(ObjectValidatorFactory.getTypeValidator(schema) instanceof AnyTypeValidator);
        schema.put(Constants.TYPE,Constants.TYPE_STRING);
        Assert.assertTrue(ObjectValidatorFactory.getTypeValidator(schema) instanceof StringTypeValidator);
        schema.put(Constants.TYPE,Constants.TYPE_NUMBER);
        Assert.assertTrue(ObjectValidatorFactory.getTypeValidator(schema) instanceof NumberTypeValidator);
        schema.put(Constants.TYPE,Constants.TYPE_INTEGER);
        Assert.assertTrue(ObjectValidatorFactory.getTypeValidator(schema) instanceof IntegerTypeValidator);
        schema.put(Constants.TYPE,Constants.TYPE_BOOLEAN);
        Assert.assertTrue(ObjectValidatorFactory.getTypeValidator(schema) instanceof BooleanTypeValidator);
        schema.put(Constants.TYPE,Constants.TYPE_OBJECT);
        Assert.assertTrue(ObjectValidatorFactory.getTypeValidator(schema) instanceof ObjectTypeValidator);
        schema.put(Constants.TYPE,Constants.TYPE_ARRAY);
        Assert.assertTrue(ObjectValidatorFactory.getTypeValidator(schema) instanceof ArrayTypeValidator);
        schema.put(Constants.TYPE,Constants.TYPE_NULL);
        Assert.assertTrue(ObjectValidatorFactory.getTypeValidator(schema) instanceof NullTypeValidator);
        schema.put(Constants.TYPE,Constants.TYPE_ANY);
        Assert.assertTrue(ObjectValidatorFactory.getTypeValidator(schema) instanceof AnyTypeValidator);
        schema.put(Constants.TYPE, Arrays.asList(Constants.TYPE_ANY,Constants.TYPE_NULL));
        Assert.assertTrue(ObjectValidatorFactory.getTypeValidator(schema) instanceof UnionTypeValidator);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testGetUnsupportedTypeValidator() throws Exception {
        Map<String, Object> schema = new HashMap<String, Object>();
        schema.put(Constants.TYPE,"FAKE");
        ObjectValidatorFactory.getTypeValidator(schema);

    }

     @Test(expectedExceptions = RuntimeException.class)
    public void testInvalidSchema() throws Exception {
        Map<String, Object> schema = new HashMap<String, Object>();
        schema.put(Constants.TYPE,1);
        ObjectValidatorFactory.getTypeValidator(schema);

    }

    @Test
    public void enginesTest() {
        ScriptEngineManager manager = new ScriptEngineManager();
        List<ScriptEngineFactory> factoryList = manager.getEngineFactories();
        for (ScriptEngineFactory factory : factoryList) {
          System.out.println(factory.getEngineName());
          System.out.println(factory.getLanguageName());
        }
    }
}
