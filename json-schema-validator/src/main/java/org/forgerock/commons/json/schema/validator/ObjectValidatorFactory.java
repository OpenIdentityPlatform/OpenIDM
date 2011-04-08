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

import org.forgerock.commons.json.schema.validator.validators.*;
import org.forgerock.commons.json.schema.validator.validators.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.forgerock.commons.json.schema.validator.Constants.*;

/**
 * ObjectValidatorFactory initialises the validator instances for given schemas.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class ObjectValidatorFactory {

    private static final Map<String, Class<? extends Validator>> validators;

    static {
        validators = new HashMap<String, Class<? extends Validator>>(8);
        validators.put(TYPE_STRING, StringTypeValidator.class);
        validators.put(TYPE_NUMBER, NumberTypeValidator.class);
        validators.put(TYPE_INTEGER, IntegerTypeValidator.class);
        validators.put(TYPE_BOOLEAN, BooleanTypeValidator.class);
        validators.put(TYPE_OBJECT, ObjectTypeValidator.class);
        validators.put(TYPE_ARRAY, ArrayTypeValidator.class);
        validators.put(TYPE_NULL, NullTypeValidator.class);
        validators.put(TYPE_ANY, AnyTypeValidator.class);
    }

    /**
     * @param schema JSON Schema Draft-03 object
     * @return Pre-configured {@link Validator} instance.
     * @throws NullPointerException when the <code>schema</code> is null.
     * @throws RuntimeException     when the validators in the <code>schema</code> is not supported.
     */
    public static Validator getTypeValidator(Map<String, Object> schema) {
        Object typeValue = schema.get(TYPE);
        if (null == typeValue) {
            return getTypeValidator(TYPE_ANY, schema);
        } else if (typeValue instanceof String) {
            return getTypeValidator((String) typeValue, schema);
        } else if (typeValue instanceof List) {
            return new UnionTypeValidator(schema);
        }
        throw new RuntimeException("Unsupported validators exception {}");
    }

    public static Validator getTypeValidator(String type, Map<String, Object> schema) {
        Class<? extends Validator> clazz = findClass(type);
        if (null != clazz) {
            try {
                return clazz.getConstructor(Map.class).newInstance(schema);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to initialization the new Validator instance");
            }
        }
        throw new RuntimeException("Unsupported validators exception {}");
    }

    private static Class<? extends Validator> findClass(String type) {
        return validators.get(type);
    }
}
