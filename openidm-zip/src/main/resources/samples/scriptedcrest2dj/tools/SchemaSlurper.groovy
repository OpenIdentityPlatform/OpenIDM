/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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
 */

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.forgerock.json.fluent.JsonPointer
import org.forgerock.openicf.misc.crest.VisitorParameter
import org.identityconnectors.common.CollectionUtil
import org.identityconnectors.common.Pair
import org.identityconnectors.common.security.GuardedByteArray
import org.identityconnectors.common.security.GuardedString
import org.identityconnectors.framework.common.exceptions.ConfigurationException
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributeInfo
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder
import org.identityconnectors.framework.common.objects.AttributeUtil
import org.identityconnectors.framework.common.objects.Name
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.QualifiedUid
import org.identityconnectors.framework.common.objects.Uid

/**
 * A CRESTSchema parses the OpenIDM configuration and builds the schema from.
 *
 * @author Laszlo Hordos
 */
class SchemaSlurper {

    public static final String TYPE_STRING = "string";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_NULL = "null";
    public static final String JAVA_TYPE_BIGDECIMAL = "JAVA_TYPE_BIGDECIMAL";
    public static final String JAVA_TYPE_BIGINTEGER = "JAVA_TYPE_BIGINTEGER";
    public static final String JAVA_TYPE_PRIMITIVE_BOOLEAN = "JAVA_TYPE_PRIMITIVE_BOOLEAN";
    public static final String JAVA_TYPE_BYTE_ARRAY = "JAVA_TYPE_BYTE_ARRAY";
    public static final String JAVA_TYPE_CHAR = "JAVA_TYPE_CHAR";
    public static final String JAVA_TYPE_CHARACTER = "JAVA_TYPE_CHARACTER";
    public static final String JAVA_TYPE_DATE = "JAVA_TYPE_DATE";
    public static final String JAVA_TYPE_PRIMITIVE_DOUBLE = "JAVA_TYPE_PRIMITIVE_DOUBLE";
    public static final String JAVA_TYPE_DOUBLE = "JAVA_TYPE_DOUBLE";
    public static final String JAVA_TYPE_FILE = "JAVA_TYPE_FILE";
    public static final String JAVA_TYPE_PRIMITIVE_FLOAT = "JAVA_TYPE_PRIMITIVE_FLOAT";
    public static final String JAVA_TYPE_FLOAT = "JAVA_TYPE_FLOAT";
    public static final String JAVA_TYPE_GUARDEDBYTEARRAY = "JAVA_TYPE_GUARDEDBYTEARRAY";
    public static final String JAVA_TYPE_GUARDEDSTRING = "JAVA_TYPE_GUARDEDSTRING";
    public static final String JAVA_TYPE_INT = "JAVA_TYPE_INT";
    public static final String JAVA_TYPE_PRIMITIVE_LONG = "JAVA_TYPE_PRIMITIVE_LONG";
    public static final String JAVA_TYPE_LONG = "JAVA_TYPE_LONG";
    public static final String JAVA_TYPE_NAME = "JAVA_TYPE_NAME";
    public static final String JAVA_TYPE_OBJECTCLASS = "JAVA_TYPE_OBJECTCLASS";
    public static final String JAVA_TYPE_QUALIFIEDUID = "JAVA_TYPE_QUALIFIEDUID";
    public static final String JAVA_TYPE_SCRIPT = "JAVA_TYPE_SCRIPT";
    public static final String JAVA_TYPE_UID = "JAVA_TYPE_UID";
    public static final String JAVA_TYPE_URI = "JAVA_TYPE_URI";
    public static final String JAVA_TYPE_BYTE = "JAVA_TYPE_BYTE";
    public static final String JAVA_TYPE_PRIMITIVE_BYTE = "JAVA_TYPE_PRIMITIVE_BYTE";


    static Map<String, Object> parse(URL jsonConfig) {
        assert null != jsonConfig;
        new SchemaSlurper(jsonConfig).validate()
    }

    private def schema = [:]

    Map<String, Object> validate() {
        schema.each { key, Map value ->
            if (!value.attributes.containsKey(Name.NAME)) {
                throw new ConfigurationException("Schema of ${key} does not contain '__NAME__'")
            }
        }
    }


    private SchemaSlurper(URL url) {
        Map<String, Class> typeMap = new HashMap<String, Class>(43);
        typeMap.put(TYPE_ARRAY, List.class);
        typeMap.put(TYPE_BOOLEAN, Boolean.class);
        typeMap.put(TYPE_INTEGER, Integer.class);
        typeMap.put(TYPE_NUMBER, Number.class);
        typeMap.put(TYPE_OBJECT, Map.class);
        typeMap.put(TYPE_STRING, String.class);
        typeMap.put(JAVA_TYPE_BIGDECIMAL, BigDecimal.class);
        typeMap.put(JAVA_TYPE_BIGINTEGER, BigInteger.class);
        typeMap.put(JAVA_TYPE_PRIMITIVE_BOOLEAN, boolean.class);
        typeMap.put(JAVA_TYPE_BYTE_ARRAY, byte[].class);
        typeMap.put(JAVA_TYPE_CHAR, char.class);
        typeMap.put(JAVA_TYPE_CHARACTER, Character.class);
        typeMap.put(JAVA_TYPE_DATE, Date.class);
        typeMap.put(JAVA_TYPE_PRIMITIVE_DOUBLE, double.class);
        typeMap.put(JAVA_TYPE_DOUBLE, Double.class);
        typeMap.put(JAVA_TYPE_FILE, File.class);
        typeMap.put(JAVA_TYPE_PRIMITIVE_FLOAT, float.class);
        typeMap.put(JAVA_TYPE_FLOAT, Float.class);
        typeMap.put(JAVA_TYPE_GUARDEDBYTEARRAY, GuardedByteArray.class);
        typeMap.put(JAVA_TYPE_GUARDEDSTRING, GuardedString.class);
        typeMap.put(JAVA_TYPE_INT, int.class);
        typeMap.put(JAVA_TYPE_PRIMITIVE_LONG, long.class);
        typeMap.put(JAVA_TYPE_LONG, Long.class);
        typeMap.put(JAVA_TYPE_NAME, Name.class);
        typeMap.put(JAVA_TYPE_OBJECTCLASS, ObjectClass.class);
        typeMap.put(JAVA_TYPE_QUALIFIEDUID, QualifiedUid.class);
        typeMap.put(JAVA_TYPE_SCRIPT, org.identityconnectors.common.script.Script.class);
        typeMap.put(JAVA_TYPE_UID, Uid.class);
        typeMap.put(JAVA_TYPE_URI, URI.class);
        typeMap.put(JAVA_TYPE_BYTE, Byte.class);
        typeMap.put(JAVA_TYPE_PRIMITIVE_BYTE, Byte.TYPE);


        def slurper = new JsonSlurper()
        //def url = getClass().getClassLoader().getResource("schema.json")

        def result = slurper.parse(url)
        result.objectTypes.collectEntries(schema) { objectName, objectDefinition ->
            [objectDefinition.nativeType,
             [resourceContainer: objectName,
              attributes       :
                      objectDefinition.properties.collectEntries {String propertyName, Map propertyDefinition ->
                          if (AttributeUtil.namesEqual(Uid.NAME, propertyDefinition.nativeName)) {
                              //Ignore it
                              return null
                          } else {
                              def jsonType = propertyDefinition.type

                              AttributeInfoBuilder builder = new AttributeInfoBuilder(propertyDefinition.nativeName);
                              if (propertyDefinition?.required) {
                                  builder.setRequired(true)
                              }
                              propertyDefinition?.flags?.each {
                                  if ("NOT_CREATABLE".equalsIgnoreCase(it)) {
                                      builder.setCreateable(false)
                                  } else if ("NOT_UPDATEABLE".equalsIgnoreCase(it)) {
                                      builder.setUpdateable(false)
                                  } else if ("NOT_READABLE".equalsIgnoreCase(it)) {
                                      builder.setReadable(false)
                                  } else if ("NOT_RETURNED_BY_DEFAULT".equalsIgnoreCase(it)) {
                                      builder.setReturnedByDefault(false)
                                  }
                              }
                              if (TYPE_ARRAY.equals(jsonType)) {
                                  builder.setMultiValued(true)
                                  jsonType = propertyDefinition.items.type
                              }
                              if (null != propertyDefinition.nativeType) {
                                  builder.setType(typeMap[propertyDefinition.nativeType])
                              }
                              return [propertyDefinition.nativeName,
                                      [attributeInfo: builder.build(),
                                       jsonType     : jsonType,
                                       jsonName     : new JsonPointer(propertyName),
                                      ]]
                          }
                      }]]
        }
    }

    String getResourceContainer(ObjectClass objectClass) {
        for (Pair<String, String> pair : cache.keySet()) {
            if (objectClass.is(pair.second)) {
                return pair.first
            }
        }
        throw new UnsupportedOperationException("Unsupported ObjectClass: " + objectClass.objectClassValue)
    }


    Map<String, Set<AttributeInfo>> getAttributeInfo() {
        return cache.collectEntries { key, value ->
            [key.second, value*.value.attributeInfo]
        }
    }

    private Map<String, VisitorParameter> visitorCache;

    VisitorParameter getVisitorParameter(ObjectClass objectClass) {
        if (visitorCache == null) {
            visitorCache = CollectionUtil.newCaseInsensitiveMap();
            cache.each { key, value ->
                final
                def parameter = visitorCache[key.second] = new org.forgerock.openicf.misc.crest.VisitorParameter() {

                    def params = value

                    String translateName(String filter) {
                        def a = params[filter]
                        if (null != a) {
                            return a.attributeInfo.name
                        } else {
                            filter
                        }
                    }

                    String convertValue(Attribute filter) {
                        def a = params[filter]
                        if (null != a) {
                            if (TYPE_ARRAY.equals(a.jsonType)) {
                                JsonOutput.toJson(filter.value.each {
                                    CRESTHelper.setAttributeValue(a.attributeInfo, it)
                                })
                            } else if (TYPE_BOOLEAN.equals(a.jsonType)) {
                                AttributeUtil.getBooleanValue(filter)
                            } else {
                                CRESTHelper.setAttributeValue(a.attributeInfo, AttributeUtil.getSingleValue(filter))
                            }
                        } else {
                            AttributeUtil.getAsStringValue(filter)
                        }
                    }
                }
                parameter
            }

        }
        VisitorParameter p = visitorCache[objectClass.objectClassValue]
        if (null != p) {
            return p
        } else {
            throw new UnsupportedOperationException("Unsupported ObjectClass: " + objectClass.objectClassValue)
        }
    }
}
