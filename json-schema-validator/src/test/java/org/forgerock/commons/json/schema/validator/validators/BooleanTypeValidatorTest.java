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
package org.forgerock.commons.json.schema.validator.validators;

import org.forgerock.commons.json.schema.validator.validators.Validator;
import org.forgerock.commons.json.schema.validator.ErrorHandler;
import org.forgerock.commons.json.schema.validator.ObjectValidatorFactory;
import org.forgerock.commons.json.schema.validator.exceptions.SchemaException;
import org.forgerock.commons.json.schema.validator.exceptions.ValidationException;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.annotations.Test;

import java.util.Map;

public class BooleanTypeValidatorTest {

    private String schema1 = "{"
            + "\"type\": \"boolean\","
            + "\"required\": true"
            + "}";

    @Test(expectedExceptions = ValidationException.class, expectedExceptionsMessageRegExp = "WrongType")
    public void RequiredValueNotBoolean() throws SchemaException {
        try {
            JSONParser parser = new JSONParser();
            Map<String, Object> schema = (Map<String, Object>) parser.parse(schema1);
            Validator v = ObjectValidatorFactory.getTypeValidator(schema);
            v.validate("test", Validator.AT_ROOT, new ErrorHandler() {

                @Override
                public void error(ValidationException exception) throws ValidationException {
                    throw new ValidationException("WrongType");
                }

                @Override
                public void assembleException() throws ValidationException {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            });
        } catch (ParseException ex) {
        }
    }

    @Test(expectedExceptions = ValidationException.class)
    public void RequiredValueNull() throws SchemaException {
        try {
            JSONParser parser = new JSONParser();
            Map<String, Object> schema = (Map<String, Object>) parser.parse(schema1);
            Validator v = ObjectValidatorFactory.getTypeValidator(schema);
            v.validate(null, Validator.AT_ROOT, new ErrorHandler() {

                @Override
                public void error(ValidationException exception) throws ValidationException {
                    throw new ValidationException("RequiredValueNull");
                }

                @Override
                public void assembleException() throws ValidationException {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            });
        } catch (ParseException ex) {
        }
    }

    @Test
    public void RequiredValueNotNull() throws SchemaException {
        try {
            JSONParser parser = new JSONParser();
            Map<String, Object> schema = (Map<String, Object>) parser.parse(schema1);
            Validator v = ObjectValidatorFactory.getTypeValidator(schema);
            v.validate(false, Validator.AT_ROOT, new ErrorHandler() {

                @Override
                public void error(ValidationException exception) throws SchemaException {
                    throw new ValidationException("RequiredValueNull");
                }

                @Override
                public void assembleException() throws ValidationException {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            });
        } catch (ParseException ex) {
        }
    }

//    @Test(dataProvider = "dp", threadPoolSize = 10)
//    public void f(Map<String, Object> schema, boolean b) {
//        Validator v = ObjectValidatorFactory.getTypeValidator(schema);
//        Assert.assertNotNull(v, "Schema helpers can not be null");
//        v.validate(b, v.AT_ROOT, new ErrorHandler() {
//
//            @Override
//            public void error(SchemaException exception) throws SchemaException {
//                throw new ValidationException("TestException");
//            }
//
//            @Override
//            public void assembleException() throws ValidationException {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//        });
//    }
//
//    @DataProvider
//    public Object[][] dp() throws ParseException {
//        Map<String, Object> schemaObject1 = (Map<String, Object>) parser.parse(schema1);
//
//        Object[][] stressTest = new Object[1000][2];
//        for (int i = 0; i < stressTest.length; i++) {
//            stressTest[i] = new Object[]{schemaObject1, true};
//        }
//        return stressTest;
//    }
}
