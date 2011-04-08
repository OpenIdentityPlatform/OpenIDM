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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

public class NullTypeValidatorTest {

    private String schema1 = "{"
            + "\"type\": \"null\","
            + "\"pattern\": \".*\","
            + "\"required\": true"
            + "\"minLength\": 0"
            + "\"maxLength\": 11"
            + "\"enum\": [\"number1\",\"number2\"]"
            + "\"format\": \"date-time\""
            + "}";
    private String schema2 = "{"
            + "\"type\": \"null\","
            + "\"required\": true"
            + "}";
    private String schema3 = "{"
            + "\"type\": \"null\","
            + "\"required\": \"true\""
            + "}";

    @Test
    public void ValueIsNull() throws SchemaException  {
        try {
            JSONParser parser = new JSONParser();
            Map<String, Object> schema = (Map<String, Object>) parser.parse(schema2);
            Validator v = ObjectValidatorFactory.getTypeValidator(schema);
            Assert.assertTrue(v.isRequired(), "Required MUST be true");
            v.validate(null, Validator.AT_ROOT, new ErrorHandler() {

                @Override
                public void error(ValidationException exception) throws SchemaException {
                    throw new ValidationException("ValueIsNull");
                }

                @Override
                public void assembleException() throws ValidationException {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            });
        } catch (ParseException ex) {
        }
    }

    @Test(expectedExceptions = ValidationException.class, expectedExceptionsMessageRegExp = "ValueIsNotNull")
    public void ValueIsNotNull()  throws SchemaException {
        try {
            JSONParser parser = new JSONParser();
            Map<String, Object> schema = (Map<String, Object>) parser.parse(schema3);
            Validator v = ObjectValidatorFactory.getTypeValidator(schema);
            Assert.assertTrue(v.isRequired(), "Required MUST be true");
            v.validate(Boolean.TRUE, Validator.AT_ROOT, new ErrorHandler() {

                @Override
                public void error(ValidationException exception) throws SchemaException {
                    throw new ValidationException("ValueIsNotNull");
                }

                @Override
                public void assembleException() throws ValidationException {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            });
        } catch (ParseException ex) {
        }
    }
}
