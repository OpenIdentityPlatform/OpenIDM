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

public class StringTypeValidatorTest extends ValidatorTestBase {

    @DataProvider(name = "invalid-schema-objects")
    public Iterator<Object[]> invalidSchemaObject() throws IOException, ParseException {
        List<Object[]> tests = getTestJSON("invalid", "/stringTests.json");
        return tests.iterator();
    }

    @DataProvider(name = "valid-schema-objects")
    public Iterator<Object[]> validSchemaObject() throws IOException, ParseException {
        List<Object[]> tests = getTestJSON("valid", "/stringTests.json");
        return tests.iterator();
    }

    @Test(dataProvider = "valid-schema-objects")
    public void validateValidObjects(Validator validator, Object instance)  throws SchemaException {
        Assert.assertNotNull(validator);
        validator.validate(instance, null, new ErrorHandler() {
            @Override
            public void error(ValidationException exception) throws SchemaException {
                System.out.println(exception.getMessage());
                throw new ValidationException("Failed");
            }

            @Override
            public void assembleException() throws ValidationException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
    }

    @Test(dataProvider = "invalid-schema-objects", expectedExceptions = ValidationException.class, expectedExceptionsMessageRegExp = "validateInvalidObjects")
    public void validateInvalidObjects(Validator validator, Object instance) throws SchemaException  {
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
