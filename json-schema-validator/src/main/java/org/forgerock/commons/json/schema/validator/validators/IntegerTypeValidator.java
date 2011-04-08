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

import org.forgerock.commons.json.schema.validator.ErrorHandler;
import org.forgerock.commons.json.schema.validator.exceptions.SchemaException;
import org.forgerock.commons.json.schema.validator.exceptions.ValidationException;
import org.forgerock.commons.json.schema.validator.helpers.EnumHelper;
import org.forgerock.commons.json.schema.validator.helpers.MaximumHelper;
import org.forgerock.commons.json.schema.validator.helpers.MinimumHelper;

import java.util.List;
import java.util.Map;

import static org.forgerock.commons.json.schema.validator.Constants.*;

/**
 * IntegerTypeValidator applies all the constraints of a <code>integer</code> type.
 * <p/>
 * Sample JSON Schema:
 * </code>
 * {
 * "type"             : "number",
 * "required"         : false,
 * "minimum"          : -500,
 * "maximum"          : 753,
 * "exclusiveMinimum" : false,
 * "exclusiveMaximum" : true,
 * "divisibleBy"      : 3,
 * "enum" : [
 * 12,
 * 333,
 * 492
 * ]
 * }
 * </code>
 *
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1">type</a>
 */
public class IntegerTypeValidator extends Validator {

    /**
     * This attribute defines what value the number instance must be
     * divisible by with no remainder (the result of the division must be an
     * integer.)  The value of this attribute SHOULD NOT be 0.
     */
    private int divisibleBy = 0;
    /**
     * This provides an enumeration of all possible values that are valid
     * for the instance property.  This MUST be an array, and each item in
     * the array represents a possible value for the instance value.  If
     * this attribute is defined, the instance value MUST be one of the
     * values in the array in order for the schema to be valid.
     */
    private EnumHelper enumHelper = null;

    private SimpleValidator minimumValidator = null;
    private SimpleValidator maximumValidator = null;

    private final long LONG_HIGH_BITS = 0xFFFFFFFF80000000L;

    public IntegerTypeValidator(Map<String, Object> schema) {
        super(schema);
        int minimum = Integer.MIN_VALUE;
        int maximum = Integer.MAX_VALUE;
        boolean exclusiveMinimum = false;
        boolean exclusiveMaximum = false;

        for (Map.Entry<String, Object> e : schema.entrySet()) {

            if (MINIMUM.equals(e.getKey())) {
                if (e.getValue() instanceof Number) {
                    minimum = Math.max(((Number) e.getValue()).intValue(), minimum);
                }
            } else if (MAXIMUM.equals(e.getKey())) {
                if (e.getValue() instanceof Number) {
                    maximum = Math.max(minimum, ((Number) e.getValue()).intValue());
                }
            } else if (EXCLUSIVEMINIMUM.equals(e.getKey())) {
                if (e.getValue() instanceof Boolean) {
                    exclusiveMinimum = ((Boolean) e.getValue());
                } else if (e.getValue() instanceof String) {
                    exclusiveMinimum = Boolean.parseBoolean((String) e.getValue());
                }
            } else if (EXCLUSIVEMAXIMUM.equals(e.getKey())) {
                if (e.getValue() instanceof Boolean) {
                    exclusiveMaximum = ((Boolean) e.getValue());
                } else if (e.getValue() instanceof String) {
                    exclusiveMaximum = Boolean.parseBoolean((String) e.getValue());
                }
            } else if (DIVISIBLEBY.equals(e.getKey())) {
                if (e.getValue() instanceof Number) {
                    divisibleBy = ((Number) e.getValue()).intValue() != 0 ? ((Number) e.getValue()).intValue() : 0;
                }
            } else if (ENUM.equals(e.getKey())) {
                if (e.getValue() instanceof List) {
                    enumHelper = new EnumHelper((List<Object>) e.getValue());
                }
            }
        }
        if (Integer.MIN_VALUE != minimum) {
            minimumValidator = new MinimumHelper(minimum, exclusiveMinimum);
        }
        if (Integer.MIN_VALUE != maximum) {
            maximumValidator = new MaximumHelper(maximum, exclusiveMaximum);
        }

    }

    /**
     * {@inheritDoc}
     */
    public void validate(Object node, String at, ErrorHandler handler) throws SchemaException {
        if (node instanceof Number) {
            int nodeValue = truncate((Long) node, at, handler);

            if (null != minimumValidator) {
                minimumValidator.validate(node, getPath(at, null), handler);
            }
            if (null != maximumValidator) {
                maximumValidator.validate(node, getPath(at, null), handler);
            }

            if (0 != divisibleBy && nodeValue % divisibleBy != 0) {
                handler.error(new ValidationException("", getPath(at, null)));
            }
            if (null != enumHelper) {
                enumHelper.validate(node, at, handler);
            }
        } else if (null != node) {
            handler.error(new ValidationException(ERROR_MSG_TYPE_MISMATCH, getPath(at, null)));
        } else if (required) {
            handler.error(new ValidationException(ERROR_MSG_REQUIRED_PROPERTY, getPath(at, null)));
        }
    }


    private int truncate(Long nodeValue, String at, ErrorHandler handler) throws SchemaException {
        if ((nodeValue & LONG_HIGH_BITS) == 0 || (nodeValue & LONG_HIGH_BITS) == LONG_HIGH_BITS) {
            return nodeValue.intValue();
        } else {
            //TODO: Should it throw a type cast exception?
            handler.error(new ValidationException(ERROR_MSG_TYPE_MISMATCH, getPath(at, null)));
            return nodeValue.intValue();
        }
    }
}
