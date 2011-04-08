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
import org.forgerock.commons.json.schema.validator.exceptions.SchemaException;
import org.forgerock.commons.json.schema.validator.exceptions.ValidationException;
import org.json.simple.parser.ParseException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class ArrayTypeValidatorTest extends ValidatorTestBase {

    @DataProvider(name = "invalid-schema-objects")
    public Iterator<Object[]> invalidSchemaObject() throws IOException, ParseException {
        List<Object[]> tests = getTestJSON("invalid", "/arrayTests.json");
        return tests.iterator();
    }

    @DataProvider(name = "valid-schema-objects")
    public Iterator<Object[]> validSchemaObject() throws IOException, ParseException {
        List<Object[]> tests = getTestJSON("valid", "/arrayTests.json");
        return tests.iterator();
    }


    @Test(dataProvider = "valid-schema-objects")
    public void validateValidObjects(Validator validator, Object instance) throws SchemaException {
        Assert.assertNotNull(validator);
        validator.validate(instance, null, new ErrorHandler() {
            @Override
            public void error(ValidationException exception) throws SchemaException {
                throw new ValidationException("Failed");
            }

            @Override
            public void assembleException() throws ValidationException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
    }

    @Test(dataProvider = "invalid-schema-objects", expectedExceptions = ValidationException.class, expectedExceptionsMessageRegExp = "validateInvalidObjects")
    public void validateInvalidObjects(Validator validator, Object instance) throws SchemaException {
        Assert.assertNotNull(validator);
        validator.validate(instance, null, new ErrorHandler() {
            @Override
            public void error(ValidationException exception) throws SchemaException {
                throw new ValidationException("validateInvalidObjects");
            }

            @Override
            public void assembleException() throws ValidationException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
    }
}
