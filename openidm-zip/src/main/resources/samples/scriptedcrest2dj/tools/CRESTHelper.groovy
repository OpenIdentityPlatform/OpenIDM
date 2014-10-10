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


import org.forgerock.json.fluent.JsonValue
import org.identityconnectors.common.Base64
import org.identityconnectors.common.security.GuardedByteArray
import org.identityconnectors.common.security.GuardedString
import org.identityconnectors.common.security.SecurityUtil
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributeInfo
import org.identityconnectors.framework.common.objects.AttributeUtil
import org.identityconnectors.framework.common.objects.Name

/**
 * CRESTHelper is a util class to make simpler the ConnectorObject and the JSON Map conversion.
 *
 * @author Laszlo Hordos
 */
class CRESTHelper {
    /**
     * Transform a <code>Collection</code> of {@link org.identityconnectors.framework.common.objects.Attribute} instances into a {@link Map}.
     * <p/>
     * The key to each element in the map is the <i>name</i> of an <code>Attribute</code>. The value of each element in
     * the map is the <code>Attribute</code> value checked with <code>ObjectClassInfo</code>.
     *
     * @param attributes
     *         set of attribute to transform to a map.
     * @param objectClassInfo
     *         object class info used to check/convert the attribute value
     * @return a map of string and attribute value.
     * @throws NullPointerException
     *         if the parameter <strong>attributes</strong> is <strong>null</strong>.
     */
    public static JsonValue toJsonValue(
            final String id, final Map<String, Attribute> attributes, Map<String, Object> objectClassInfo) {
        def ret = new JsonValue(new LinkedHashMap<String, Object>())
        objectClassInfo.attributes.each { key, value ->
            if (AttributeUtil.namesEqual(key, Name.NAME)) {
                if (null == id && value.attributeInfo.required) {
                    throw new InvalidAttributeValueException("Missing requires attribute:" + key);
                } else {
                    ret.addPermissive(value.jsonName, id)
                }
            } else if (value.attributeInfo.isCreateable()) {
                Attribute attribute = attributes[key];

                if (value.attributeInfo.required && (null == attribute || null == attribute.getValue() || attribute.getValue().isEmpty())) {
                    throw new InvalidAttributeValueException("Missing requires attribute:" + key);
                } else if (null != attribute) {
                    List<Object> attributeValue = attribute.getValue();
                    if (attributeValue == null) {
                        ret.addPermissive(value.jsonName, null)
                    } else {
                        if (value.attributeInfo.isMultiValued()) {
                            if (attributeValue.isEmpty()) {
                                ret.addPermissive(value.jsonName, attributeValue)
                            } else {
                                ret.addPermissive(value.jsonName, attributeValue.each {
                                    fromAttributeToJSON(value.attributeInfo, it)
                                })
                            }
                        } else if (attributeValue.size() > 1) {
                            final StringBuilder msg =
                                    new StringBuilder("The ").append(attribute.getName()).append(
                                            " attribute is not single value attribute.");
                            throw new InvalidAttributeValueException(msg.toString());
                        } else {
                            if (attributeValue.isEmpty()) {
                                ret.addPermissive(value.jsonName, null)
                            } else {
                                ret.addPermissive(value.jsonName, fromAttributeToJSON(value.attributeInfo, attributeValue.get(0)))
                            }
                        }
                    }
                }
            }
        }
        return ret
    }


    public static Object fromJSONToAttribute(AttributeInfo attributeInfo, Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (String.class.equals(attributeInfo.getType())) {
                return (String) value;
            } else if (Long.TYPE.equals(attributeInfo.getType()) || Long.class.equals(attributeInfo.getType())) {
                return (Number) value;
            } else if (Character.TYPE.equals(attributeInfo.getType()) || Character.class
                    .equals(attributeInfo.getType())) {
                return value;
            } else if (Double.TYPE.equals(attributeInfo.getType()) || Double.class.equals(attributeInfo.getType())) {
                return (Number) value;
            } else if (Float.TYPE.equals(attributeInfo.getType()) || Float.class.equals(attributeInfo.getType())) {
                return (Number) value;
            } else if (Integer.TYPE.equals(attributeInfo.getType()) || Integer.class.equals(attributeInfo.getType())) {
                return (Number) value;
            } else if (Boolean.TYPE.equals(attributeInfo.getType()) || Boolean.class.equals(attributeInfo.getType())) {
                return (Boolean) value;
            } else if (Byte.TYPE.equals(attributeInfo.getType()) || Byte.class.equals(attributeInfo.getType())) {
                return new Byte((String) value);
            } else if (byte[].class.equals(attributeInfo.getType())) {
                return Base64.decode((String) value);
            } else if (BigDecimal.class.equals(attributeInfo.getType())) {
                return (Number) value;
            } else if (BigInteger.class.equals(attributeInfo.getType())) {
                return (Number) value;
            } else if (GuardedByteArray.class.equals(attributeInfo.getType())) {
                return new GuardedByteArray(Base64.decode((String) value))
            } else if (GuardedString.class.equals(attributeInfo.getType())) {
                return new GuardedString(((String) value).toCharArray())
            } else if (Map.class.equals(attributeInfo.getType())) {
                return (Map) value;
            } else {
                //This could happen only if this implementation and the FrameworkUtil is out of sync.
                throw new IllegalArgumentException("");
            }
        } catch (final ClassCastException e) {
            final StringBuilder msg =
                    new StringBuilder("The ").append(attributeInfo.getName()).append(
                            " attribute has wrong type ").append(e.getMessage());
            throw new IllegalArgumentException(msg.toString(), e);
        }
    }


    public static Object fromAttributeToJSON(AttributeInfo attributeInfo, Object value) {
        if (value == null) {
            if (attributeInfo.getType().isPrimitive()) {
                final StringBuilder msg =
                        new StringBuilder("The ").append(attributeInfo.getName()).append(
                                " attribute is not null value attribute.");
                throw new InvalidAttributeValueException(msg.toString());
            }
            return null;
        }
        try {
            if (String.class.equals(attributeInfo.getType())) {
                return (String) value;
            } else if (Long.TYPE.equals(attributeInfo.getType()) || Long.class.equals(attributeInfo.getType())) {
                return (Long) value;
            } else if (Character.TYPE.equals(attributeInfo.getType()) || Character.class
                    .equals(attributeInfo.getType())) {
                return (Character) value;
            } else if (Double.TYPE.equals(attributeInfo.getType()) || Double.class.equals(attributeInfo.getType())) {
                return (Double) value;
            } else if (Float.TYPE.equals(attributeInfo.getType()) || Float.class.equals(attributeInfo.getType())) {
                return (Float) value;
            } else if (Integer.TYPE.equals(attributeInfo.getType()) || Integer.class.equals(attributeInfo.getType())) {
                return (Integer) value;
            } else if (Boolean.TYPE.equals(attributeInfo.getType()) || Boolean.class.equals(attributeInfo.getType())) {
                return (Boolean) value;
            } else if (Byte.TYPE.equals(attributeInfo.getType()) || Byte.class.equals(attributeInfo.getType())) {
                return ((Byte) value).toString();
            } else if (byte[].class.equals(attributeInfo.getType())) {
                return Base64.encode((byte[]) value);
            } else if (BigDecimal.class.equals(attributeInfo.getType())) {
                return (BigDecimal) value;
            } else if (BigInteger.class.equals(attributeInfo.getType())) {
                return (BigInteger) value;
            } else if (GuardedByteArray.class.equals(attributeInfo.getType())) {
                return Base64.encode(SecurityUtil.decrypt((GuardedByteArray) value));
            } else if (GuardedString.class.equals(attributeInfo.getType())) {
                return SecurityUtil.decrypt((GuardedString) value);
            } else if (Map.class.equals(attributeInfo.getType())) {
                return (Map) value;
            } else {
                //This could happen only if this implementation and the FrameworkUtil is out of sync.
                throw new IllegalArgumentException("");
            }
        } catch (final ClassCastException e) {
            final StringBuilder msg =
                    new StringBuilder("The ").append(attributeInfo.getName()).append(
                            " attribute has wrong type ").append(e.getMessage());
            throw new IllegalArgumentException(msg.toString(), e);
        }
    }
}
