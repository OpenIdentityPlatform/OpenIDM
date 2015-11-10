/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.provisioner.openicf.commons;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.schema.validator.Constants;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.QualifiedUid;
import org.identityconnectors.framework.common.objects.SortKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributeInfoHelper {

    /**
     * Setup logging for the {@link AttributeInfoHelper}.
     */
    private final static Logger logger = LoggerFactory.getLogger(AttributeInfoHelper.class);

    // Name of OpenIDM attribute
    private final String name;
    // Type of OpenIDM attribute
    private final Class<?> type;

    // OpenIDM sensitive attribute definition
    private final String cipher;
    private final String key;

    // OpenICF attribute info
    private final Object defaultValue;
    private final AttributeInfo attributeInfo;

    // TODO Revisit function of this property
    private final Set<AttributeFlag> flags;

    public AttributeInfoHelper(String name, boolean isOperationalOption, JsonValue schema)
            throws SchemaException {
        this.name = name;

        // type
        String typeString = schema.get(Constants.TYPE).required().asString();
        // TODO fix the multivalue support
        // if (Constants.TYPE_ARRAY.equals(typeString)) {
        // Object items = schema.get(Constants.ITEMS);
        // if (items instanceof Map) {
        // typeString = ((Map) items).get(Constants.TYPE);
        // }
        // }
        type = ConnectorUtil.findClassForName(typeString);

        // nativeType
        JsonValue nativeTypeString = schema.get(ConnectorUtil.OPENICF_NATIVE_TYPE);
        Class<?> nativeType = null;
        if (nativeTypeString.isNull()) {
            nativeType = type;
        } else {
            nativeType = ConnectorUtil.findClassForName(nativeTypeString.asString());
        }

        // nativeName
        JsonValue nativeNameString = schema.get(ConnectorUtil.OPENICF_NATIVE_NAME);
        String nativeName = null;
        if (nativeNameString.isNull()) {
            nativeName = name;
        } else {
            nativeName = nativeNameString.asString();
        }

        // defaultValue
        JsonValue def = schema.get(Constants.DEFAULT);
        if (def.isNull()) {
            defaultValue = null;
        } else {
            defaultValue = def.getObject();
        }

        if (!isOperationalOption) {

            // Encrypted attribute
            JsonValue k = schema.get("key");
            if (k.isString()) {
                key = k.asString();
            } else {
                key = null;
            }
            JsonValue c = schema.get("cipher");
            if (c.isString()) {
                cipher = c.asString();
            } else {
                cipher = ServerConstants.SECURITY_CRYPTOGRAPHY_DEFAULT_CIPHER;
            }

            AttributeInfoBuilder builder = new AttributeInfoBuilder(nativeName, nativeType);
            builder.setMultiValued(Collection.class.isAssignableFrom(type));

            // flags
            JsonValue flagsObject = schema.get(ConnectorUtil.OPENICF_FLAGS);
            if (flagsObject.isList()) {
                Set<AttributeFlag> flags0 = new HashSet<AttributeFlag>(flagsObject.size());
                for (JsonValue flagString : flagsObject) {
                    AttributeFlag flag = AttributeFlag.findByKey(flagString.asString());
                    if (null != flag) {
                        if (AttributeFlag.NOT_CREATABLE.equals(flag)) {
                            builder.setCreateable(false);
                        } else if (AttributeFlag.NOT_UPDATEABLE.equals(flag)) {
                            builder.setUpdateable(false);
                        } else if (AttributeFlag.NOT_READABLE.equals(flag)) {
                            builder.setReadable(false);
                        } else if (AttributeFlag.NOT_RETURNED_BY_DEFAULT.equals(flag)) {
                            builder.setReturnedByDefault(false);
                        } else if (AttributeFlag.MULTIVALUED.equals(flag)) {
                            builder.setMultiValued(true);
                        } else {
                            flags0.add(flag);
                        }
                    }
                }
                flags = Collections.unmodifiableSet(flags0);
            } else {
                flags = Collections.emptySet();
            }

            // required
            if (schema.isDefined(Constants.REQUIRED)) {
                builder.setRequired(schema.get(Constants.REQUIRED).asBoolean());
            }
            attributeInfo = builder.build();
        } else {
            key = null;
            cipher = null;
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

    public boolean isConfidential() {
        return null != key;
    }

    public Attribute build(Object source, final CryptoService cryptoService)
            throws ResourceException {
        try {
            if (null != cryptoService
                    && (GuardedString.class.isAssignableFrom(getAttributeInfo().getType()) || GuardedByteArray.class
                            .isAssignableFrom(getAttributeInfo().getType()))) {
                JsonValue decryptedValue =
                        new JsonValue(source, new JsonPointer(),
                                null != cryptoService ? cryptoService.getDecryptionTransformers()
                                        : null);
                return build(attributeInfo, decryptedValue.getObject());
            } else {
                return build(attributeInfo, source);
            }
        } catch (Exception e) {
            logger.error("Failed to build {} attribute out of {}", name, source);
            throw new InternalServerErrorException("Failed build " + name + " attribute.", e);
        }
    }

    /**
     * The {@link org.forgerock.json.resource.QueryFilterVisitor} use this
     * method to convert the string value to {@link Attribute} use in
     * {@link org.identityconnectors.framework.common.objects.filter.Filter}
     * 
     * @param source
     * @return
     * @throws Exception
     */
    public Attribute build(Object source) {
        return build(attributeInfo, source);
    }

    public Attribute build(AttributeInfo attributeInfo, Object source) {
        Attribute attribute;

        if (OperationalAttributes.PASSWORD_NAME.equals(attributeInfo.getName())
                || OperationalAttributes.CURRENT_PASSWORD_NAME.equals(attributeInfo.getName())) {
            // check the value..
            if (source == null /*|| value.size() != 1*/) {
                final String MSG = "Must be a single value.";
                //throw new IllegalArgumentException(MSG);
                return null;
            }
            if (!(source instanceof GuardedString)) {
                final String MSG = "Password value must be an instance of GuardedString";
                //throw new IllegalArgumentException(MSG);
            }
        }

        if (null == source) {
            attribute = AttributeBuilder.build(attributeInfo.getName());
        } else {
            if (attributeInfo.isMultiValued()) {
                attribute =
                        AttributeBuilder.build(attributeInfo.getName(), getMultiValue(source,
                                attributeInfo.getType()));
            } else {
                attribute =
                        AttributeBuilder.build(attributeInfo.getName(), getSingleValue(source,
                                attributeInfo.getType()));
            }
        }
        return attribute;
    }

    public Object build(Attribute source, CryptoService cryptoService) throws JsonCryptoException {
        Object resultValue = null;
        if (attributeInfo.isMultiValued()) {
            if (null != source.getValue()) {
                List<Object> value = new ArrayList<Object>(source.getValue().size());
                for (Object o : source.getValue()) {
                    value.add(ConnectorUtil.coercedTypeCasting(o, Object.class));
                }
                resultValue = value;
            }
        } else {
            try {
                resultValue =
                        ConnectorUtil
                                .coercedTypeCasting(AttributeUtil.getSingleValue(source), type);
            } catch (IllegalArgumentException e) {
                logger.warn(
                        "Incorrect schema configuration. Expecting {} attribute to be single but it has multi value.",
                        attributeInfo.getName());
                throw e;
            }
        }
        if (isConfidential()) {
            if (null == cryptoService) {
                throw new JsonCryptoException(
                        "Confidential attribute can not be encrypted. Reason: CryptoService is null");
            } else {
                return cryptoService.encrypt(new JsonValue(resultValue), cipher, key).getObject();
            }
        } else {
            return resultValue;
        }
    }

    /**
     * @param builder
     * @param value
     * @throws IOException
     * @throws IllegalArgumentException
     *             if the type is not on the supported list.
     * @see {@link org.identityconnectors.framework.common.FrameworkUtil#checkOperationOptionType(Class)}
     */
    public void build(OperationOptionsBuilder builder, Object value) throws IOException {
        if (value == null || (value instanceof JsonValue && !((JsonValue) value).isNull())) {
            return;
        }
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
        } else if (OperationOptions.OP_PAGED_RESULTS_COOKIE.equals(name)) {
            builder.setPagedResultsCookie(getSingleValue(value, String.class));
        } else if (OperationOptions.OP_PAGED_RESULTS_OFFSET.equals(name)) {
            builder.setPagedResultsOffset(getSingleValue(value, Integer.class));
        } else if (OperationOptions.OP_PAGE_SIZE.equals(name)) {
            builder.setPageSize(getSingleValue(value, Integer.class));
        } else if (OperationOptions.OP_FAIL_ON_ERROR.equals(name)) {
            builder.setAttributesToGet(getSingleValue(value, Boolean.class).toString());
        } else if (OperationOptions.OP_REQUIRE_SERIAL.equals(name)) {
            builder.setAttributesToGet(getSingleValue(value, String.class));
        } else if (OperationOptions.OP_SORT_KEYS.equals(name)) {
            builder.setSortKeys((List<SortKey>) getMultiValue(value, SortKey.class));
        } else {
            builder.setOption(name, getNewValue(value == null ? defaultValue : value, attributeInfo
                    .isMultiValued(), attributeInfo.getType()));
        }
    }

    private Object getNewValue(Object source, boolean isMultiValued, Class type) {
        if (isMultiValued) {
            return getMultiValue(source, type);
        } else {
            return getSingleValue(source, type);
        }
    }

    private <T> T getSingleValue(Object source, Class<T> clazz) {
        if (null == source) {
            return null;
        } 
        
        if (source instanceof JsonValue) {
            source = ((JsonValue) source).getObject();
        }
        
        if (source instanceof List) {
            List c = (List) source;
            if (c.size() < 2) {
                if (c.isEmpty()) {
                    return null;
                } else {
                    return ConnectorUtil.coercedTypeCasting(c.get(0), clazz);
                }
            }
            logger.error("Non multivalued [{}] argument has collection value", name);
            throw new IllegalArgumentException("Non multivalued argument [" + name
                    + "] has collection value");
        } else if (source.getClass().isArray()) {
            logger.error("Non multivalued [{}] argument has array value", name);
            throw new IllegalArgumentException("Non multivalued argument [" + name
                    + "] has array value");
        } else {
            return ConnectorUtil.coercedTypeCasting(source, clazz);
        }
    }

    private <T> Collection<T> getMultiValue(Object source, Class<T> clazz) {
        if (null == source) {
            return null;
        }

        if (source instanceof JsonValue) {
            source = ((JsonValue) source).getObject();
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
