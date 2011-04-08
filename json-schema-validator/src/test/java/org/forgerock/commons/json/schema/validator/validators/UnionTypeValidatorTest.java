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

public class UnionTypeValidatorTest {

    private String schema1 = "{"
            + "\"type\": [\"string\",\"boolean\"],"
            + "\"pattern\": \".*\","
            + "\"required\": true"
            + "\"minLength\": 0"
            + "\"maxLength\": 11"
            + "\"enum\": [\"number1\",\"number2\"]"
            + "\"format\": \"date-time\""
            + "}";
    private String schema2 = "{"
            + "\"type\": [\"string\",\"any\"],"
            + "\"required\": true"
            + "}";
    private String schema3 = "{"
            + "\"type\": [\"string\",\"null\"],"
            + "\"minLength\": 0"
            + "\"maxLength\": 10"
            + "}";

    @Test
    public void unionWithNullType() throws SchemaException  {
        try {
            JSONParser parser = new JSONParser();
            Map<String, Object> schema = (Map<String, Object>) parser.parse(schema3);
            Validator v = ObjectValidatorFactory.getTypeValidator(schema);
            v.validate(null, Validator.AT_ROOT, new ErrorHandler() {

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

    @Test(expectedExceptions = ValidationException.class, expectedExceptionsMessageRegExp = "BooleanNotAllowed")
    public void unionWithBooleanType()  throws SchemaException {
        try {
            JSONParser parser = new JSONParser();
            Map<String, Object> schema = (Map<String, Object>) parser.parse(schema3);
            Validator v = ObjectValidatorFactory.getTypeValidator(schema);
            v.validate(Boolean.TRUE, Validator.AT_ROOT, new ErrorHandler() {

                @Override
                public void error(ValidationException exception) throws SchemaException {
                    throw new ValidationException("BooleanNotAllowed");
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
