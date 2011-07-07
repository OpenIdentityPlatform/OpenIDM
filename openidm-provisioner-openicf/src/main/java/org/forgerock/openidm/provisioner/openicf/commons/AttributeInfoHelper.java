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
package org.forgerock.openidm.provisioner.openicf.commons;

import org.forgerock.json.schema.validator.Constants;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.*;

import java.io.IOException;
import java.util.*;


public class AttributeInfoHelper {
    private final Class<?> type;
    private final String name;
    private final Set<AttributeFlag> flags;
    private final AttributeInfo attributeInfo;
    private final Object defaultValue;



    public AttributeInfoHelper(String name, boolean isOperationalOption, Map<String, Object> schema) throws SchemaException {
        this.name = name;

        //type
        Object typeString = schema.get(Constants.TYPE);
        if (typeString instanceof String) {
            //TODO fix the multivalue support
//            if (Constants.TYPE_ARRAY.equals(typeString)) {
//                Object items = schema.get(Constants.ITEMS);
//                if (items instanceof Map) {
//                    typeString = ((Map) items).get(Constants.TYPE);
//                }
//            }
            type = ConnectorUtil.findClassForName((String) typeString);
        } else {
            throw new SchemaException("type MUST be non empty String or List<String> value");
        }

        //nativeType
        Object nativeTypeString = schema.get(ConnectorUtil.OPENICF_NATIVE_TYPE);
        Class<?> nativeType = null;
        if (nativeTypeString instanceof String) {
            nativeType = ConnectorUtil.findClassForName((String) nativeTypeString);
        } else {
            nativeType = type;
        }

        //nativeName
        Object nativeNameString = schema.get(ConnectorUtil.OPENICF_NATIVE_NAME);
        String nativeName = null;
        if (nativeNameString instanceof String) {
            nativeName = (String) nativeNameString;
        } else {
            nativeName = name;
        }

        //default
        Object def = schema.get(Constants.DEFAULT);
        if (null == def) {
            defaultValue = null;
        } else {
            defaultValue = def;
        }

        if (!isOperationalOption) {

            AttributeInfoBuilder builder = new AttributeInfoBuilder(nativeName, nativeType);
            builder.setMultiValued(Collection.class.isAssignableFrom(type));

            //flags
            Object flagsObject = schema.get(ConnectorUtil.OPENICF_FLAGS);
            if (flagsObject instanceof List) {
                flags = new HashSet<AttributeFlag>(((List) flagsObject).size());
                for (String flagString : (List<String>) flagsObject) {
                    AttributeFlag flag = AttributeFlag.findByKey(flagString);
                    if (null != flag) {
                        if (AttributeFlag.NOT_CREATABLE.equals(flag)) {
                            builder.setCreateable(false);
                        } else if (AttributeFlag.NOT_UPDATEABLE.equals(flag)) {
                            builder.setUpdateable(false);
                        } else if (AttributeFlag.NOT_READABLE.equals(flag)) {
                            builder.setReadable(false);
                        } else if (AttributeFlag.NOT_RETURNED_BY_DEFAULT.equals(flag)) {
                            builder.setReturnedByDefault(false);
                        } else {
                            flags.add(flag);
                        }
                    }
                }
            } else {
                flags = Collections.emptySet();
            }

            //required
            builder.setRequired((null != schema.get(Constants.REQUIRED)) ? (Boolean) schema.get(Constants.REQUIRED) : false);
            attributeInfo = builder.build();
        } else {
            flags = null;
            attributeInfo = null;
        }
    }


    /**
     * Get the Java Class for the {@code type} value in the schema.
     * <p/>
     * The Default mapping:
     * <table>
     * <tr><td>JSON Type</td><td>Java Type</td></tr>
     * <tr><td>any</td><td>{@link Object}</td></tr>
     * <tr><td>boolean</td><td>{@link Boolean}</td></tr>
     * <tr><td>integer</td><td>{@link Integer}</td></tr>
     * <tr><td>array</td><td>{@link List}</td></tr>
     * <tr><td>null</td><td>{@code null}</td></tr>
     * <tr><td>number</td><td>{@link Number}</td></tr>
     * <tr><td>object</td><td>{@link Map}</td></tr>
     * <tr><td>string</td><td>{@link String}</td></tr>
     * </tr>
     * </table>
     *
     * @return
     */
    public Class<?> getType() {
        return type;
    }


    public String getName() {
        return name;
    }

    public AttributeInfo getAttributeInfo() {
        return attributeInfo;
    }

    public Attribute build(Object source) throws Exception {
        return AttributeInfoHelper.build(attributeInfo, source);
    }

    public static Attribute build(AttributeInfo attributeInfo, Object source) throws Exception {
        Attribute attribute = null;
        if (null == source) {
            attribute = AttributeBuilder.build(attributeInfo.getName());
        } else {
            if (attributeInfo.isMultiValued()) {
                attribute = AttributeBuilder.build(attributeInfo.getName(), getMultiValue(source, attributeInfo.getType()));
            } else {
                attribute = AttributeBuilder.build(attributeInfo.getName(), getSingleValue(source, attributeInfo.getType()));
            }
        }
        return attribute;
    }

    public Object build(Attribute source) throws IOException {
        if (attributeInfo.isMultiValued()) {
            List<Object> value = new ArrayList<Object>(source.getValue().size());
            for (Object o : source.getValue()) {
                value.add(ConnectorUtil.coercedTypeCasting(o, Object.class));
            }
            return value;
        } else {
            return ConnectorUtil.coercedTypeCasting(AttributeUtil.getSingleValue(source), type);
        }
    }

    /**
     * @param builder
     * @param value
     * @throws IOException
     * @throws IllegalArgumentException if the type is not on the supported list.
     * @see {@link org.identityconnectors.framework.common.FrameworkUtil#checkOperationOptionType(Class)}
     */
    public void build(OperationOptionsBuilder builder, Object value) throws IOException {
        if (OperationOptions.OP_ATTRIBUTES_TO_GET.equals(name)) {
            builder.setAttributesToGet(getMultiValue(value, String.class));
        } else if (OperationOptions.OP_CONTAINER.equals(name)) {
            builder.setContainer(getSingleValue(value, QualifiedUid.class));
        } else if (OperationOptions.OP_RUN_AS_USER.equals(name)) {
            builder.setRunAsUser(getSingleValue(value, String.class));
        } else if (OperationOptions.OP_RUN_WITH_PASSWORD.equals(name)) {
            builder.setRunWithPassword(getSingleValue(value, GuardedString.class));
        } else if (OperationOptions.OP_SCOPE.equals(name)) {
            builder.setScope(getSingleValue(value, String.class));
        } else {
            builder.setOption(name, getNewValue(value == null ? defaultValue : value, attributeInfo.isMultiValued(), attributeInfo.getType()));
        }
    }

    private static Object getNewValue(Object source, boolean isMultiValued, Class type) throws IOException {
        if (isMultiValued) {
            return getMultiValue(source, type);
        } else {
            return getSingleValue(source, type);
        }
    }


    private static <T> T getSingleValue(Object source, Class<T> clazz) throws IOException {
        if (null == source) {
            return null;
        } else if ((source instanceof List)) {
            List c = (List) source;
            if (c.size() < 2) {
                if (c.isEmpty()) {
                    return null;
                } else {
                    return ConnectorUtil.coercedTypeCasting(c.get(0), clazz);
                }
            }
            throw new IllegalArgumentException("Non multivalued argument has collection value");
        } else if (source.getClass().isArray()) {
            throw new IllegalArgumentException("Non multivalued argument has collection value");
        } else {
            return ConnectorUtil.coercedTypeCasting(source, clazz);
        }
    }

    private static <T> Collection<T> getMultiValue(Object source, Class<T> clazz) throws IOException {
        if (null == source) {
            return null;
        }
        List<T> newValues = null;
        if (source instanceof Collection) {
            newValues = new ArrayList<T>(((Collection) source).size());
            for (Object o : (Collection) source) {
                newValues.add(ConnectorUtil.coercedTypeCasting(o, clazz));
            }
        } else if (source.getClass().isArray()) {
            newValues = new ArrayList<T>(((Object[]) source).length);
            for (Object o : (Object[]) source) {
                newValues.add(ConnectorUtil.coercedTypeCasting(o, clazz));
            }
        } else {
            newValues = new ArrayList<T>(1);
            newValues.add(ConnectorUtil.coercedTypeCasting(source, clazz));
        }

        return newValues;
    }

}
