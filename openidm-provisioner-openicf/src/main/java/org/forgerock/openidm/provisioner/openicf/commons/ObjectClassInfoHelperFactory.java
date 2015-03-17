/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidm.provisioner.openicf.commons;

import org.forgerock.guava.common.base.Function;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.schema.validator.Constants;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ObjectClassInfoHelperFactory {

    /**
     * Setup logging for the {@link ObjectClassInfoHelperFactory}.
     */
    private static final Logger logger = LoggerFactory.getLogger(ObjectClassInfoHelperFactory.class);

    private static final Map<ObjectClass, Function<JsonValue, ObjectClassInfoHelper>> factory =
            new LinkedHashMap<ObjectClass, Function<JsonValue, ObjectClassInfoHelper>>();

    private static final Function<JsonValue, ObjectClassInfoHelper> DEFAULT_SCHEMA_PARSER =
            new Function<JsonValue, ObjectClassInfoHelper>() {
                @Override
                public ObjectClassInfoHelper apply(JsonValue schema) {
                    try {
                        final ObjectClass objectClass = new ObjectClass(schema.get(ConnectorUtil.OPENICF_OBJECT_CLASS).required().asString());
                        JsonValue properties = schema.get(Constants.PROPERTIES).required().expect(Map.class);
                        Set<String> propertyNames = properties.keys();

                        Set<AttributeInfoHelper> attributes0 = new HashSet<AttributeInfoHelper>(propertyNames.size());
                        Set<String> defaultAttributes = new HashSet<String>(propertyNames.size());
                        String __NAME__ = null;

                        for (String propertyName : propertyNames) {
                            AttributeInfoHelper helper = new AttributeInfoHelper(propertyName, false, properties.get(propertyName));
                            if (helper.getAttributeInfo().getName().equals(Name.NAME)) {
                                __NAME__ = propertyName;
                            }
                            if (helper.getAttributeInfo().isReturnedByDefault()) {
                                defaultAttributes.add(helper.getAttributeInfo().getName());
                            }
                            attributes0.add(helper);
                        }
                        return new ObjectClassInfoHelper(objectClass, __NAME__, attributes0, defaultAttributes, properties);
                    } catch (JsonValueException jsonValueException) {
                        throw new SchemaException(schema, jsonValueException);
                    }
                }
            };

    private static final Function<JsonValue, ObjectClassInfoHelper> ALL_SCHEMA_PARSER =
            new Function<JsonValue, ObjectClassInfoHelper>() {
                @Override
                public ObjectClassInfoHelper apply(JsonValue schema) {
                    final ObjectClass objectClass = new ObjectClass(schema.get(ConnectorUtil.OPENICF_OBJECT_CLASS).required().asString());
                    if (schema.isDefined(Constants.PROPERTIES)) {
                        throw new SchemaException(schema, "Properties is not allowed for the __ALL__ object class");
                    }
                    return new ObjectClassInfoHelper(
                            objectClass,
                            null,
                            new HashSet<AttributeInfoHelper>(),
                            new HashSet<String>(),
                            new JsonValue(new HashMap<String, Object>()));
                }
            };

    static {
        factory.put(ObjectClass.ACCOUNT, DEFAULT_SCHEMA_PARSER);
        factory.put(ObjectClass.GROUP, DEFAULT_SCHEMA_PARSER);
        factory.put(ObjectClass.ALL, ALL_SCHEMA_PARSER);
        factory.put(new ObjectClass("__TEST__"), DEFAULT_SCHEMA_PARSER);
    }


    /**
     * Create a custom {@link ObjectClassInfoHelper}.
     * @param schema of the {@link ObjectClass} to create the {@link ObjectClassInfoHelper} for.
     * @return a new {@link ObjectClassInfoHelper}.
     * @throws SchemaException when schema is invalid.
     */
    static ObjectClassInfoHelper createObjectClassInfoHelper(JsonValue schema) throws SchemaException {
        ObjectClass objectClass = new ObjectClass(schema.get(ConnectorUtil.OPENICF_OBJECT_CLASS).required().asString());
        return factory.containsKey(objectClass)
                ? factory.get(objectClass).apply(schema)
                : DEFAULT_SCHEMA_PARSER.apply(schema);
    }
}
