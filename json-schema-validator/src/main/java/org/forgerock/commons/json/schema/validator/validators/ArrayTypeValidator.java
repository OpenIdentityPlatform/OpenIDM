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
import org.forgerock.commons.json.schema.validator.ObjectValidatorFactory;
import org.forgerock.commons.json.schema.validator.exceptions.SchemaException;
import org.forgerock.commons.json.schema.validator.exceptions.ValidationException;

import java.util.*;

import static org.forgerock.commons.json.schema.validator.Constants.*;

/**
 * ArrayTypeValidator applies all the constraints of a <code>array</code> type.
 * <p/>
 * Sample JSON Schema:
 * </code>
 * {
 * "type"            : "array",
 * "required"        : true,
 * "uniqueItems"     : "true",
 * "minItems"        : 2,
 * "maxItems"        : 3,
 * "additionalItems" : {
 * "type" : "string",
 * "enum" : [
 * "test1",
 * "test2",
 * "test3"
 * ]
 * },
 * "items" : {
 * "type" : "string"
 * }
 * }
 * </code>
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1">type</a>
 */
public class ArrayTypeValidator extends Validator {

    /**
     * This attribute defines the minimum number of values in an array when
     * the array is the instance value.
     */
    int minItems = -1;
    /**
     * This attribute defines the maximum number of values in an array when
     * the array is the instance value.
     */
    int maxItems = -1;
    /**
     * This attribute indicates that all items in an array instance MUST be
     * unique (contains no two identical values).
     * <p/>
     * Two instance are consider equal if they are both of the same validators
     * and: are null; or are booleans/numbers/strings and have the same value; or
     * are arrays, contains the same number of items, and each item in
     * the array is equal to the corresponding item in the other array;
     * or are objects, contains the same property names, and each property
     * in the object is equal to the corresponding property in the other
     * object
     */
    boolean uniqueItems = false;
    /**
     * This provides a definition for additional items in an array instance
     * when tuple definitions of the items is provided.  This can be false
     * to indicate additional items in the array are not allowed, or it can
     * be a schema that defines the schema of the additional items.
     */
    boolean additionalItems = true;
    List<Validator> tupleValidators = null;
    Validator singleValidator = null;
    Validator additionalItemsValidator = null;

    public ArrayTypeValidator(Map<String, Object> schema) {
        super(schema);
        for (Map.Entry<String, Object> e : schema.entrySet()) {
            if (UNIQUEITEMS.equals(e.getKey())) {
                if (e.getValue() instanceof Boolean) {
                    uniqueItems = ((Boolean) e.getValue());
                } else if (e.getValue() instanceof String) {
                    uniqueItems = Boolean.parseBoolean((String) e.getValue());
                }
            } else if (MINITEMS.equals(e.getKey())) {
                if (e.getValue() instanceof Number) {
                    minItems = Math.max(((Number) e.getValue()).intValue(), -1);
                }
            } else if (MAXITEMS.equals(e.getKey())) {
                if (e.getValue() instanceof Number) {
                    maxItems = Math.max(((Number) e.getValue()).intValue(), minItems);
                }
            } else if (ADDITIONALITEMS.equals(e.getKey())) {
                if (e.getValue() instanceof Boolean) {
                    additionalItems = ((Boolean) e.getValue());
                } else if (e.getValue() instanceof String) {
                    additionalItems = Boolean.parseBoolean((String) e.getValue());
                } else if (e.getValue() instanceof Map) {
                    additionalItemsValidator = ObjectValidatorFactory.getTypeValidator((Map<String, Object>) e.getValue());
                }
            } else if (ITEMS.equals(e.getKey())) {
                if (e.getValue() instanceof Map) {
                    singleValidator = ObjectValidatorFactory.getTypeValidator((Map<String, Object>) e.getValue());
                } else if (e.getValue() instanceof List) {
                    tupleValidators = new ArrayList<Validator>(2);
                    for (Object o : (List<Object>) e.getValue()) {
                        if (o instanceof Map) {
                            tupleValidators.add(ObjectValidatorFactory.getTypeValidator((Map<String, Object>) o));
                        }
                    }
                }
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    public void validate(Object node, String at, ErrorHandler handler) throws SchemaException {
        if (node instanceof List) {
            List<Object> nodeValue = (List<Object>) node;
            if (minItems > -1 && nodeValue.size() < minItems) {
                handler.error(new ValidationException(("minItems error")));
            }
            if (maxItems > -1 && nodeValue.size() > maxItems) {
                handler.error(new ValidationException(("maxItems error")));
            }

            checkUniqueItems(nodeValue, at, handler);

            if (null != singleValidator) {
                for (int i = 0; i < nodeValue.size(); i++) {
                    singleValidator.validate(nodeValue.get(i), String.format("%s[%s]", at, i), handler);
                }
            } else if (null != tupleValidators) {
                if (tupleValidators.size() > nodeValue.size()) {
                    handler.error(new ValidationException("Array has less item then expected", getPath(at, null)));
                } else if (!additionalItems && tupleValidators.size() < nodeValue.size()) {
                    handler.error(new ValidationException("Array can not have additional item(s)", getPath(at, null)));
                } else {
                    for (int i = 0; i < nodeValue.size(); i++) {
                        Validator v = i < tupleValidators.size() ? tupleValidators.get(i) : additionalItemsValidator;
                        if (null != v) {
                            v.validate(nodeValue.get(i), String.format("%s[%s]", at, i), handler);
                        }
                    }
                }
            }
        } else if (null != node) {
            handler.error(new ValidationException(ERROR_MSG_TYPE_MISMATCH, getPath(at, null)));
        } else if (required) {
            handler.error(new ValidationException(ERROR_MSG_REQUIRED_PROPERTY, getPath(at, null)));
        }


    }

    private void checkUniqueItems(List<Object> nodeValue, String at, ErrorHandler handler) throws SchemaException {
        if (uniqueItems && nodeValue.size() > 1) {
            Set<Object> set = new HashSet<Object>();
            for (Object n : nodeValue) {
                set.add(n);
            }

            if (set.size() < nodeValue.size()) {
                handler.error(new ValidationException("The items in the array must be unique", getPath(at, null)));
            }
        }
    }
}
