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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.schema.validator.Constants.*;

import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.schema.validator.Constants;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.provisioner.openicf.ConnectorReference;
import org.identityconnectors.common.Base64;
import org.identityconnectors.common.pooling.ObjectPoolConfiguration;
import org.identityconnectors.common.script.Script;
import org.identityconnectors.common.script.ScriptBuilder;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.*;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.common.objects.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


/**
 * Contains openicf connector utilities for the OpenICF provisioner.
 */
public class ConnectorUtil {

    /**
     * Setup logging for the {@link ConnectorUtil}.
     */
    private final static Logger logger = LoggerFactory.getLogger(ConnectorUtil.class);

    private static final String OPENICF_BUNDLENAME = "bundleName";
    private static final String OPENICF_BUNDLEVERSION = "bundleVersion";
    private static final String OPENICF_CONNECTOR_NAME = "connectorName";
    public static final String OPENICF_CONNECTOR_HOST_REF = "connectorHostRef";
    private static final String OPENICF_HOST = "host";
    private static final String OPENICF_PORT = "port";
    public static final String OPENICF_KEY = "key";
    private static final String OPENICF_USE_SSL = "useSSL";
    private static final String OPENICF_TRUST_MANAGERS = "trustManagers";
    private static final String OPENICF_TIMEOUT = "timeout";
    private static final String OPENICF_MAX_OBJECTS = "maxObjects";
    private static final String OPENICF_MAX_IDLE = "maxIdle";
    private static final String OPENICF_MAX_WAIT = "maxWait";
    private static final String OPENICF_MIN_EVICTABLE_IDLE_TIME_MILLIS = "minEvictableIdleTimeMillis";
    private static final String OPENICF_MIN_IDLE = "minIdle";
    private static final String OPENICF_POOL_CONFIG_OPTION = "poolConfigOption";
    private static final String OPENICF_RESULTSHANDLER_CONFIG_OPTION = "resultsHandlerConfig";
    private static final String OPENICF_RESULTSHANDLER_ENABLENORMALIZINGRESULTSHANDLER = "enableNormalizingResultsHandler";
    private static final String OPENICF_RESULTSHANDLER_ENABLEFILTEREDRESULTSHANDLER = "enableFilteredResultsHandler";
    private static final String OPENICF_RESULTSHANDLER_ENABLECASEINSENSITIVEFILTER = "enableCaseInsensitiveFilter";
    private static final String OPENICF_RESULTSHANDLER_ENABLEATTRIBUTESTOGETSEARCHRESULTSHANDLER = "enableAttributesToGetSearchResultsHandler";
    private static final String OPENICF_OPERATION_TIMEOUT = "operationTimeout";
    public static final String OPENICF_CONFIGURATION_PROPERTIES = "configurationProperties";
    public static final String OPENICF_FLAGS = "flags";
    public static final String OPENICF_OBJECT_CLASS = "nativeType";
    //public static final String PROPERTY_IS_CONTAINER = "isContainer";
    public static final String OPENICF_NATIVE_NAME = "nativeName";
    public static final String OPENICF_NATIVE_TYPE = "nativeType";

    public static final String OPENICF_REMOTE_CONNECTOR_SERVERS = "remoteConnectorServers";

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


    private static final Map<String, Class> typeMap = new HashMap<String, Class>(43);
    public static final String OPENICF_CONNECTOR_REF = "connectorRef";
    public static final String OPENICF_OBJECT_TYPES = "objectTypes";
    public static final String OPENICF_OPERATION_OPTIONS = "operationOptions";
    public static final String OPENICF_SYNC_TOKEN = "syncToken";
    public static final String OPENICF_OBJECT_FEATURES = "objectFeatures";

    static {

        typeMap.put(Constants.TYPE_ANY, Object.class);
        //typeMap.put(Constants.TYPE_NULL, null);
        typeMap.put(Constants.TYPE_ARRAY, List.class);
        typeMap.put(Constants.TYPE_BOOLEAN, Boolean.class);
        typeMap.put(Constants.TYPE_INTEGER, Integer.class);
        typeMap.put(Constants.TYPE_NUMBER, Number.class);
        typeMap.put(Constants.TYPE_OBJECT, Map.class);
        typeMap.put(Constants.TYPE_STRING, String.class);
        typeMap.put(JAVA_TYPE_BIGDECIMAL, BigDecimal.class);
        typeMap.put(JAVA_TYPE_BIGINTEGER, BigInteger.class);
        typeMap.put(JAVA_TYPE_PRIMITIVE_BOOLEAN, boolean.class);
        typeMap.put(JAVA_TYPE_BYTE_ARRAY, byte[].class);
        typeMap.put(JAVA_TYPE_CHAR, char.class);
        typeMap.put(JAVA_TYPE_CHARACTER, Character.class);
        typeMap.put(JAVA_TYPE_DATE, String.class);
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
        typeMap.put(JAVA_TYPE_SCRIPT, Script.class);
        typeMap.put(JAVA_TYPE_UID, Uid.class);
        typeMap.put(JAVA_TYPE_URI, URI.class);
        typeMap.put(JAVA_TYPE_BYTE, Byte.class);
        typeMap.put(JAVA_TYPE_PRIMITIVE_BYTE, Byte.TYPE);
    }

    private static final String BUFFER_SIZE = "bufferSize";


    //Util Methods

    /**
     * Get the Java Class for the {@code type} value in the schema.
     * <p/>
     * The Default mapping:
     * <table>
     * <tr><td>any</td><td>{@link Object}</td></tr>
     * <tr><td>JAVA_TYPE_BIGDECIMAL</td><td>{@link BigDecimal}</td></tr>
     * <tr><td>JAVA_TYPE_BIGINTEGER</td><td>{@link BigInteger}</td></tr>
     * <tr><td>JAVA_TYPE_PRIMITIVE_BOOLEAN</td><td>{@link boolean}</td></tr>
     * <tr><td>boolean</td><td>{@link Boolean}</td></tr>
     * <tr><td>JAVA_TYPE_BYTE_ARRAY</td><td>{@link byte[]}</td></tr>
     * <tr><td>JAVA_TYPE_CHAR</td><td>{@link char}</td></tr>
     * <tr><td>JAVA_TYPE_CHARACTER</td><td>{@link Character}</td></tr>
     * <tr><td>JAVA_TYPE_DATE</td><td>{@link String}</td></tr>
     * <tr><td>JAVA_TYPE_PRIMITIVE_DOUBLE</td><td>{@link double}</td></tr>
     * <tr><td>JAVA_TYPE_DOUBLE</td><td>{@link Double}</td></tr>
     * <tr><td>JAVA_TYPE_FILE</td><td>{@link File}</td></tr>
     * <tr><td>JAVA_TYPE_PRIMITIVE_FLOAT</td><td>{@link float}</td></tr>
     * <tr><td>JAVA_TYPE_FLOAT</td><td>{@link Float}</td></tr>
     * <tr><td>JAVA_TYPE_GUARDEDBYTEARRAY</td><td>{@link GuardedByteArray}</td></tr>
     * <tr><td>JAVA_TYPE_GUARDEDSTRING</td><td>{@link GuardedString}</td></tr>
     * <tr><td>JAVA_TYPE_INT</td><td>{@link int}</td></tr>
     * <tr><td>integer</td><td>{@link Integer}</td></tr>
     * <tr><td>array</td><td>{@link List}</td></tr>
     * <tr><td>JAVA_TYPE_PRIMITIVE_LONG</td><td>{@link long}</td></tr>
     * <tr><td>JAVA_TYPE_LONG</td><td>{@link Long}</td></tr>
     * <tr><td>JAVA_TYPE_NAME</td><td>{@link Name}</td></tr>
     * <tr><td>null</td><td>{@code null}</td></tr>
     * <tr><td>number</td><td>{@link Number}</td></tr>
     * <tr><td>object</td><td>{@link Map}</td></tr>
     * <tr><td>JAVA_TYPE_OBJECTCLASS</td><td>{@link ObjectClass}</td></tr>
     * <tr><td>JAVA_TYPE_QUALIFIEDUID</td><td>{@link QualifiedUid}</td></tr>
     * <tr><td>JAVA_TYPE_SCRIPT</td><td>{@link Script}</td></tr>
     * <tr><td>string</td><td>{@link String}</td></tr>
     * <tr><td>JAVA_TYPE_UID</td><td>{@link Uid }</td></tr>
     * <tr><td>JAVA_TYPE_URI</td><td>{@link URI}</td></tr>
     * </table>
     *
     * @param name
     * @return class if it has mapped to a type or null if not.
     */
    public static Class findClassForName(String name) {
        return typeMap.get(name);
    }

    /**
     * Find the string code of the given {@code clazz}.
     * <p/>
     * Encodes the {@link Class} to String code
     *
     * @param clazz
     * @return
     * @see #findClassForName(String)
     */
    public static String findNameForClass(Class clazz) {
        for (Map.Entry<String, Class> entry : typeMap.entrySet()) {
            if (entry.getValue().equals(clazz)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Find the proper target simple class type for the source type.
     * <p/>
     * JSON schema has predefined types abd this method maps the {@code clazz} Class to proper representation
     *
     * @param clazz
     * @return
     */
    public static String findJSONTypeForClass(Class clazz) {
        if ((Integer.class.isAssignableFrom(clazz)) || (int.class == clazz)) {
            return Constants.TYPE_INTEGER;
        } else if ((Number.class.isAssignableFrom(clazz)) || (double.class == clazz) || (float.class == clazz) || (long.class == clazz)) {
            return Constants.TYPE_NUMBER;
        } else if ((Boolean.class.isAssignableFrom(clazz)) || (boolean.class == clazz)) {
            return Constants.TYPE_BOOLEAN;
        } else if (List.class.isAssignableFrom(clazz)) {
            return Constants.TYPE_ARRAY;
        } else if (Map.class.isAssignableFrom(clazz)) {
            return Constants.TYPE_OBJECT;
        } else {
            return Constants.TYPE_STRING;
        }
    }


    /**
     * Convert the {@link ObjectPoolConfiguration} to simple Map.
     * <p/>
     *
     * @param info
     * @return
     */
    public static Map<String, Object> getObjectPoolConfiguration(ObjectPoolConfiguration info) {
        Map<String, Object> poolConfigOption = new LinkedHashMap<String, Object>(5);
        poolConfigOption.put(OPENICF_MAX_OBJECTS, info.getMaxObjects());
        poolConfigOption.put(OPENICF_MAX_IDLE, info.getMaxIdle());
        poolConfigOption.put(OPENICF_MAX_WAIT, info.getMaxWait());
        poolConfigOption.put(OPENICF_MIN_EVICTABLE_IDLE_TIME_MILLIS, info.getMinEvictableIdleTimeMillis());
        poolConfigOption.put(OPENICF_MIN_IDLE, info.getMinIdle());
        return poolConfigOption;
    }

    /**
     * @param source
     * @param target
     * @throws UnsupportedOperationException when the property value can not be converted to String.
     */
    public static void configureResultsHandlerConfiguration(JsonValue source, ResultsHandlerConfiguration target) throws JsonValueException {
        if (!source.get(OPENICF_RESULTSHANDLER_ENABLENORMALIZINGRESULTSHANDLER).isNull()) {
            target.setEnableNormalizingResultsHandler(source.get(OPENICF_RESULTSHANDLER_ENABLENORMALIZINGRESULTSHANDLER).asBoolean());
        }
        if (!source.get(OPENICF_RESULTSHANDLER_ENABLEFILTEREDRESULTSHANDLER).isNull()) {
            target.setEnableFilteredResultsHandler(source.get(OPENICF_RESULTSHANDLER_ENABLEFILTEREDRESULTSHANDLER).asBoolean());
        }
        if (!source.get(OPENICF_RESULTSHANDLER_ENABLECASEINSENSITIVEFILTER).isNull()) {
            target.setEnableCaseInsensitiveFilter(source.get(OPENICF_RESULTSHANDLER_ENABLECASEINSENSITIVEFILTER).asBoolean());
        }
        if (!source.get(OPENICF_RESULTSHANDLER_ENABLEATTRIBUTESTOGETSEARCHRESULTSHANDLER).isNull()) {
            target.setEnableAttributesToGetSearchResultsHandler(source.get(OPENICF_RESULTSHANDLER_ENABLEATTRIBUTESTOGETSEARCHRESULTSHANDLER).asBoolean());
        }
    }

    /**
     * Convert the {@link ObjectPoolConfiguration} to simple Map.
     * <p/>
     *
     * @param info
     * @return
     */
    public static Map<String, Object> getResultsHandlerConfiguration(ResultsHandlerConfiguration info) {
        Map<String, Object> config = new LinkedHashMap<String, Object>(5);
        config.put(OPENICF_RESULTSHANDLER_ENABLENORMALIZINGRESULTSHANDLER, info.isEnableNormalizingResultsHandler());
        config.put(OPENICF_RESULTSHANDLER_ENABLEFILTEREDRESULTSHANDLER, info.isEnableFilteredResultsHandler());
        config.put(OPENICF_RESULTSHANDLER_ENABLECASEINSENSITIVEFILTER, info.isEnableCaseInsensitiveFilter());
        config.put(OPENICF_RESULTSHANDLER_ENABLEATTRIBUTESTOGETSEARCHRESULTSHANDLER, info.isEnableAttributesToGetSearchResultsHandler());
        return config;
    }

    /**
     * @param source
     * @param target
     * @throws UnsupportedOperationException when the property value can not be converted to String.
     */
    public static void configureObjectPoolConfiguration(JsonValue source, ObjectPoolConfiguration target) throws JsonValueException {
        Map<String, Object> poolConfiguration = source.asMap();
        if (null != poolConfiguration.get(OPENICF_MAX_OBJECTS)) {
            target.setMaxObjects(coercedTypeCasting(poolConfiguration.get(OPENICF_MAX_OBJECTS), int.class));
        }
        if (null != poolConfiguration.get(OPENICF_MAX_IDLE)) {
            target.setMaxIdle(coercedTypeCasting(poolConfiguration.get(OPENICF_MAX_IDLE), int.class));
        }
        if (null != poolConfiguration.get(OPENICF_MAX_WAIT)) {
            target.setMaxWait(coercedTypeCasting(poolConfiguration.get(OPENICF_MAX_WAIT), long.class));
        }
        if (null != poolConfiguration.get(OPENICF_MIN_EVICTABLE_IDLE_TIME_MILLIS)) {
            target.setMinEvictableIdleTimeMillis(coercedTypeCasting(poolConfiguration.get(OPENICF_MIN_EVICTABLE_IDLE_TIME_MILLIS), long.class));
        }
        if (null != poolConfiguration.get(OPENICF_MIN_IDLE)) {
            target.setMinIdle(coercedTypeCasting(poolConfiguration.get(OPENICF_MIN_IDLE), int.class));
        }
    }


    public static Map<String, Object> getTimeout(APIConfiguration configuration) {
        Map<String, Object> result = new LinkedHashMap<String, Object>(12);
        for (OperationType e : OperationType.values()) {
            result.put(e.name(), configuration.getTimeout(e.getValue()));
        }
        return result;
    }

    public static void configureTimeout(JsonValue source, APIConfiguration target) throws JsonValueException {
        for (OperationType e : OperationType.values()) {
            JsonValue value = source.get(e.name());
            try {
                if (!value.isNull()) {
                    target.setTimeout(e.getValue(), coercedTypeCasting(value.asNumber(), int.class));
                }
            } catch (IllegalArgumentException e1) {
                logger.error("Type casting exception of {} from {} to int", new Object[]{value.getObject(), value.getObject().getClass().getCanonicalName()}, e);
            }
        }
    }


    /**
     * @param source
     * @param target
     * @throws IllegalArgumentException
     */
    public static void setConfigurationProperties(ConfigurationProperties source, Map<String, Object> target, CryptoService cryptoService) throws JsonCryptoException{
        for (String propertyName : source.getPropertyNames()) {
            ConfigurationProperty configurationProperty = source.getProperty(propertyName);
            target.put(propertyName, convertFromConfigurationProperty(configurationProperty, cryptoService));
        }
    }

    private static Object convertFromConfigurationProperty(ConfigurationProperty configurationProperty, CryptoService cryptoService) throws JsonCryptoException{
        Object sourceValue = configurationProperty.getValue();
        if (sourceValue == null) {
            return null;
        }
        boolean isArray = sourceValue.getClass().isArray();
        Class sourceType = isArray ? sourceValue.getClass().getComponentType() : sourceValue.getClass();
        Object result = null;
        if (isArray) {
            if (sourceType == byte.class) {
                result = new String((byte[]) sourceValue);
            } else if (sourceType == char.class) {
                result = new String((char[]) sourceValue);
            } else if (sourceType == Character.class) {
                Character[] characterArray = (Character[]) sourceValue;
                char[] charArray = new char[characterArray.length];
                for (int i = 0; i < characterArray.length; i++) {
                    charArray[i] = characterArray[i];
                }
                result = new String(charArray);
            } else {
                int length = Array.getLength(sourceValue);
                List values = new ArrayList(length);
                for (int i = 0; i < length; i++) {
                    Object item = Array.get(sourceValue, i);
                    Object newValue = coercedTypeCasting(item, Object.class);
                    if (sourceType.isAssignableFrom(GuardedString.class)
                            || sourceType.isAssignableFrom(GuardedByteArray.class)) {
                        newValue = cryptoService.encrypt(json(newValue),
                                ServerConstants.SECURITY_CRYPTOGRAPHY_DEFAULT_CIPHER,
                                IdentityServer.getInstance().getProperty("openidm.config.crypto.alias", "openidm-config-default"))
                        .getObject();
                    }
                    values.add(newValue);
                }
                result = values;
            }
        } else if (configurationProperty.getType().equals(GuardedString.class)
                || configurationProperty.getType().equals(GuardedByteArray.class)) {
            result = cryptoService.encrypt(json(coercedTypeCasting(configurationProperty.getValue(), String.class)),
                        ServerConstants.SECURITY_CRYPTOGRAPHY_DEFAULT_CIPHER,
                        IdentityServer.getInstance().getProperty("openidm.config.crypto.alias", "openidm-config-default"))
                    .getObject();
        } else {
            result = coercedTypeCasting(sourceValue, Object.class);
        }
        return result;
    }


    public static void  configureConfigurationProperties(JsonValue source, ConfigurationProperties target,
            CryptoService cryptoService) throws JsonValueException {
        source.required();
        if (null != target) {
            List<String> configPropNames = target.getPropertyNames();
            for (Map.Entry<String, Object> e : source.asMap().entrySet()) {
                if (!configPropNames.contains(e.getKey())) {
                    /*
                    * The connector's Configuration does not define this property.
                    */
                    continue;
                }
                ConfigurationProperty property = target.getProperty(e.getKey());
                Class targetType = property.getType();
                Object propertyValue = null;
                if (targetType.isArray()) {
                    Class targetBaseType = targetType.getComponentType();
                    if (targetBaseType == byte.class || targetBaseType == char.class) {
                        propertyValue = coercedTypeCasting(e.getValue(), targetType);
                    } else if (e.getValue() instanceof List) {
                        List v = (List) e.getValue();
                        propertyValue = Array.newInstance(targetBaseType, v.size());
                        for (int i = 0; i < v.size(); i++) {
                            Array.set(propertyValue, i, coercedTypeCasting(v.get(i), targetBaseType));
                        }
                    } else {
                        propertyValue = Array.newInstance(targetBaseType, 1);
                        Array.set(propertyValue, 0, coercedTypeCasting(e.getValue(), targetBaseType));
                    }
                } else if (property.isConfidential()
                        || property.getType().equals(GuardedString.class)
                        || property.getType().equals(GuardedByteArray.class)) {
                    propertyValue = coercedTypeCasting(cryptoService.decrypt(source.get(e.getKey())), targetType);

                } else {
                    propertyValue = coercedTypeCasting(e.getValue(), targetType);
                }
                property.setValue(propertyValue);
            }
        }
    }


    public static void configureDefaultAPIConfiguration(
                JsonValue source, APIConfiguration target, CryptoService cryptoService)
            throws JsonValueException {
        JsonValue poolConfigOption = source.get(OPENICF_POOL_CONFIG_OPTION);
        if (poolConfigOption.isMap()) {
            configureObjectPoolConfiguration(poolConfigOption, target.getConnectorPoolConfiguration());
        }
        JsonValue resultsHandlerConfigOption = source.get(OPENICF_RESULTSHANDLER_CONFIG_OPTION);
        if (resultsHandlerConfigOption.isMap()) {
            configureResultsHandlerConfiguration(resultsHandlerConfigOption, target.getResultsHandlerConfiguration());
        }
        JsonValue operationTimeout = source.get(OPENICF_OPERATION_TIMEOUT);
        if (operationTimeout.isMap()) {
            configureTimeout(operationTimeout, target);
        }
        JsonValue configurationProperties = source.get(OPENICF_CONFIGURATION_PROPERTIES);
        configureConfigurationProperties(configurationProperties, target.getConfigurationProperties(), cryptoService);
        if (source.isDefined(BUFFER_SIZE)) {
            target.setProducerBufferSize(source.get(BUFFER_SIZE).required().asInteger());
        }
    }

    public static JsonValue createSystemConfigurationFromAPIConfiguration(
                APIConfiguration source, JsonValue target, CryptoService cryptoService)
            throws JsonCryptoException {
        target.put(OPENICF_POOL_CONFIG_OPTION, getObjectPoolConfiguration(source.getConnectorPoolConfiguration()));
        target.put(OPENICF_RESULTSHANDLER_CONFIG_OPTION, getResultsHandlerConfiguration(source.getResultsHandlerConfiguration()));
        target.put(OPENICF_OPERATION_TIMEOUT, getTimeout(source));
        Map<String, Object> configurationProperties = new LinkedHashMap<String, Object>();
        target.put(OPENICF_CONFIGURATION_PROPERTIES, configurationProperties);
        setConfigurationProperties(source.getConfigurationProperties(), configurationProperties, cryptoService);
        return target;
    }


    /**
     * Convert the {@link ConnectorKey} into a Map.
     * <p/>
     * The connector key is saved in a JSON object and this method converts it to simple Map.
     *
     * @param info
     * @return
     */
    public static Map<String, Object> getConnectorKey(ConnectorKey info) {
        Map<String, Object> result = new HashMap<String, Object>(6);
        result.put(OPENICF_BUNDLENAME, info.getBundleName());
        result.put(OPENICF_BUNDLEVERSION, info.getBundleVersion());
        result.put(OPENICF_CONNECTOR_NAME, info.getConnectorName());
        return result;
    }

    /**
     * Create a new {@link ConnectorKey} newBuilder form the {@code configuration} object.
     * <p/>
     * The Configuration object MUST contain the three required String properties.
     * <ul>
     * <li>bundleName</li>
     * <li>bundleVersion</li>
     * <li>connectorName</li>
     * </ul>
     *
     * @param configuration
     * @return new newBuilder of {@link ConnectorKey}
     * @throws IllegalArgumentException when one of the three required parameter is null.
     * @throws IOException              when the property value can not be converted to String.
     */
    public static ConnectorKey getConnectorKey(JsonValue configuration) throws JsonValueException {
        String bundleName = configuration.get(OPENICF_BUNDLENAME).asString();
        String bundleVersion = configuration.get(OPENICF_BUNDLEVERSION).asString();
        String connectorName = configuration.get(OPENICF_CONNECTOR_NAME).asString();
        return new ConnectorKey(bundleName, bundleVersion, connectorName);
    }

    /**
     * @param info
     * @return
     * @throws IllegalArgumentException if the configuration can not be read from {@code info}
     */
    public static RemoteFrameworkConnectionInfo getRemoteFrameworkConnectionInfo(JsonValue info) {
        String _host = info.get(OPENICF_HOST).required().asString();

        JsonValue port = info.get(OPENICF_PORT).defaultTo(8759);
        int _port = ConnectorUtil.coercedTypeCasting(port, int.class);

        GuardedString _key = ConnectorUtil
                .coercedTypeCasting(info.get(OPENICF_KEY).required(), GuardedString.class);
        JsonValue useSSL = info.get(OPENICF_USE_SSL).defaultTo(Boolean.FALSE);
        boolean _useSSL = ConnectorUtil.coercedTypeCasting(useSSL, boolean.class);

        //List<TrustManager> _trustManagers;
        JsonValue timeout = info.get(OPENICF_TIMEOUT).defaultTo(0);
        int _timeout = ConnectorUtil.coercedTypeCasting(timeout, int.class);

        return new RemoteFrameworkConnectionInfo(_host, _port, _key, _useSSL, null, _timeout);
    }


    public static Map<String, Object> getRemoteFrameworkConnectionMap(RemoteFrameworkConnectionInfo info) {
        Map<String, Object> result = new HashMap<String, Object>(6);
        result.put(OPENICF_HOST, info.getHost());
        result.put(OPENICF_PORT, info.getPort());
        result.put(OPENICF_KEY, info.getKey().toString());
        result.put(OPENICF_USE_SSL, info.getUseSSL());
        result.put(OPENICF_TRUST_MANAGERS, info.getTrustManagers());
        result.put(OPENICF_TIMEOUT, info.getTimeout());
        return result;
    }


    public static ConnectorReference getConnectorReference(JsonValue configuration) throws JsonValueException {
        JsonValue connectorRef = configuration.get(OPENICF_CONNECTOR_REF).required().expect(Map.class);
        ConnectorKey key = getConnectorKey(connectorRef);
        String connectorHost = connectorRef.get(OPENICF_CONNECTOR_HOST_REF).defaultTo(ConnectorReference.SINGLE_LOCAL_CONNECTOR_MANAGER).asString();
        return new ConnectorReference(key, connectorHost);
    }

    public static void setConnectorReference(ConnectorReference source, JsonValue target) {
        Map<String, Object> connectorReference = getConnectorKey(source.getConnectorKey());
        connectorReference.put(OPENICF_CONNECTOR_HOST_REF, source.getConnectorHost());
        target.put(OPENICF_CONNECTOR_REF, connectorReference);
    }


    public static Map<String, ObjectClassInfoHelper> getObjectTypes(JsonValue configuration) throws JsonValueException {
        JsonValue objectTypes = configuration.get(OPENICF_OBJECT_TYPES).defaultTo(json(object()));
        Map<String, ObjectClassInfoHelper> result = new HashMap<String, ObjectClassInfoHelper>(objectTypes.expect(Map.class).asMap().size());
        boolean allObjectClassFound = false;
        for (String objectType : objectTypes.keys()) {
            final ObjectClassInfoHelper objectClassInfoHelper =
                    ObjectClassInfoHelperFactory.createObjectClassInfoHelper(objectTypes.get(objectType));
            result.put(objectType, objectClassInfoHelper);
            if (allObjectClassFound == false && ObjectClass.ALL.equals(objectClassInfoHelper.getObjectClass())){
                allObjectClassFound = true;
            }
        }
        if (!allObjectClassFound) {
            //check if an objectclass is using the default object class name
            if (result.containsKey(ObjectClass.ALL_NAME)) {
                throw new SchemaException(configuration,
                        "Unable to add the __ALL__ object class because some other object type is using the __ALL__ name");
            }

            //add default __ALL__ object class
            final JsonValue allObjectClassSchema = new JsonValue(new HashMap<String, Object>());
            allObjectClassSchema.put(ConnectorUtil.OPENICF_OBJECT_CLASS, ObjectClass.ALL_NAME);
            result.put(ObjectClass.ALL_NAME, ObjectClassInfoHelperFactory.createObjectClassInfoHelper(allObjectClassSchema));
        }
        return result;
    }


    public static void setObjectAndOperationConfiguration(Schema source, JsonValue target) {
        Map<String, Object> objectTypes = new LinkedHashMap<String, Object>(source.getObjectClassInfo().size());
        for (ObjectClassInfo objectClassInfo : source.getObjectClassInfo()) {
            objectTypes.put(objectClassInfo.getType(), getObjectClassInfoMap(objectClassInfo));
        }
        target.put(OPENICF_OBJECT_TYPES, objectTypes);

        Map<String, Object> operationOptions = new LinkedHashMap<String, Object>(12);
        for (OperationType e : OperationType.values()) {
            Map<String, Object> operationOptionInfo = new LinkedHashMap<String, Object>();
            Map<String, Object> objectFeatures = new LinkedHashMap<String, Object>();
            operationOptionInfo.put(OPENICF_OBJECT_FEATURES, objectFeatures);
            for (ObjectClassInfo o : source.getSupportedObjectClassesByOperation(e.getValue())) {
                objectFeatures.put(o.getType(), OperationOptionInfoHelper.build(source.getSupportedOptionsByOperation(e.getValue())));
            }
            operationOptions.put(e.name(), operationOptionInfo);
        }
        target.put(OPENICF_OPERATION_OPTIONS, operationOptions);

    }

    public static Map<String, Map<Class<? extends APIOperation>, OperationOptionInfoHelper>> getOperationOptionConfiguration(JsonValue configuration) throws JsonValueException, SchemaException {
        Set<String> objectTypes = ConnectorUtil.getObjectTypes(configuration).keySet();
        Map<String, Map<Class<? extends APIOperation>, OperationOptionInfoHelper>> operationOptionConfigurationMap =
                new HashMap<String, Map<Class<? extends APIOperation>, OperationOptionInfoHelper>>(objectTypes.size());

        JsonValue operationOptions = configuration.get(OPENICF_OPERATION_OPTIONS);

        if (operationOptions.expect(Map.class).isNull()) {
            for (String type : objectTypes) {
                Map<Class<? extends APIOperation>, OperationOptionInfoHelper> config = new HashMap<Class<? extends APIOperation>, OperationOptionInfoHelper>(12);
                operationOptionConfigurationMap.put(type, config);
                for (OperationType entry : OperationType.values()) {
                    config.put(entry.getValue(), new OperationOptionInfoHelper());
                }
            }
        } else {
            for (OperationType entry : OperationType.values()) {
                OperationOptionInfoHelper defaultOperationOptionInfoHelper = null;

                JsonValue operation = operationOptions.get(entry.name()).expect(Map.class);
                if (operation.isNull()) {
                    defaultOperationOptionInfoHelper = new OperationOptionInfoHelper();
                } else {
                    defaultOperationOptionInfoHelper = new OperationOptionInfoHelper(operation);
                }

                JsonValue objectFeatures = operation.get(OPENICF_OBJECT_FEATURES).expect(Map.class);
                for (String type : objectTypes) {
                    JsonValue objectFeature = objectFeatures.get(type);

                    Map<Class<? extends APIOperation>, OperationOptionInfoHelper> config = operationOptionConfigurationMap.get(type);
                    if (null == config) {
                        config = new HashMap<Class<? extends APIOperation>, OperationOptionInfoHelper>(12);
                        operationOptionConfigurationMap.put(type, config);
                    }
                    config.put(entry.getValue(), new OperationOptionInfoHelper(objectFeature, defaultOperationOptionInfoHelper));
                }
            }
        }

        return operationOptionConfigurationMap;
    }


    public static Map<String, Object> getObjectClassInfoMap(ObjectClassInfo info) {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put(SCHEMA, JSON_SCHEMA_DRAFT03);
        schema.put(ID, info.getType());
        schema.put(TYPE, TYPE_OBJECT);
        schema.put(OPENICF_OBJECT_CLASS, info.getType());
        //schema.put(ConnectorUtil.PROPERTY_IS_CONTAINER, info.isContainer());
        Map<String, Object> properties = new LinkedHashMap<String, Object>(info.getAttributeInfo().size());
        schema.put(PROPERTIES, properties);
        for (AttributeInfo attributeInfo : info.getAttributeInfo()) {
            properties.put(attributeInfo.getName(), getAttributeInfoMap(attributeInfo));
        }
        return schema;
    }

    /**
     * Build a {@link Map} from the given {@link org.identityconnectors.framework.common.objects.AttributeInfo}
     * <p/>
     * The result will look like this:
     * {
     * "type"    : "number",
     * "mapName" : "lastLogin",
     * "mapType" : "JAVA_TYPE_DOUBLE",
     * "flags" : [
     * "REQUIRED",
     * "MULTIVALUED",
     * "NOT_CREATABLE",
     * "NOT_UPDATEABLE",
     * "NOT_READABLE",
     * "NOT_RETURNED_BY_DEFAULT"
     * ]
     * }
     *
     * @param info
     * @return
     */
    public static Map<String, Object> getAttributeInfoMap(AttributeInfo info) {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        if (info.isMultiValued()) {
            schema.put(Constants.TYPE, Constants.TYPE_ARRAY);
            Map<String, Object> itemSchema = new LinkedHashMap<String, Object>(2);
            itemSchema.put(Constants.TYPE, findJSONTypeForClass(info.getType()));
            itemSchema.put(ConnectorUtil.OPENICF_NATIVE_TYPE, findNameForClass(info.getType()));
            schema.put(Constants.ITEMS, itemSchema);
        } else {
            schema.put(Constants.TYPE, findJSONTypeForClass(info.getType()));
        }
        if (info.isRequired()) {
            schema.put(Constants.REQUIRED, true);
        }
        schema.put(ConnectorUtil.OPENICF_NATIVE_NAME, info.getName());
        schema.put(ConnectorUtil.OPENICF_NATIVE_TYPE, findNameForClass(info.getType()));
        if (!info.getFlags().isEmpty()) {
            List<String> flags = null;
            for (AttributeInfo.Flags flag : info.getFlags()) {
                if (AttributeInfo.Flags.MULTIVALUED.equals(flag) || AttributeInfo.Flags.REQUIRED.equals(flag)) {
                    continue;
                }
                if (null == flags) {
                    flags = new ArrayList<String>(4);
                }
                flags.add(flag.name());
            }
            if (null != flags) {
                schema.put(ConnectorUtil.OPENICF_FLAGS, Collections.unmodifiableList(flags));
            }
        }
        return schema;
    }

    public static Map<String, Object> getOperationOptionInfoMap(OperationOptionInfo info) {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        Class clazz = info.getType().isArray() ? info.getType().getComponentType() : info.getType();
        if (info.getType().isArray()) {
            schema.put(Constants.TYPE, Constants.TYPE_ARRAY);
            Map<String, Object> itemSchema = new LinkedHashMap<String, Object>(2);
            itemSchema.put(Constants.TYPE, findJSONTypeForClass(clazz));
            itemSchema.put(ConnectorUtil.OPENICF_NATIVE_TYPE, findNameForClass(clazz));
            schema.put(Constants.ITEMS, itemSchema);
        } else {
            schema.put(Constants.TYPE, findJSONTypeForClass(clazz));
        }
        schema.put(ConnectorUtil.OPENICF_NATIVE_TYPE, findNameForClass(clazz));
        return schema;
    }


    /**
     * Create a new {@link SyncToken} from the input.
     * <p/>
     * The source object:
     * {@code
     * {
     * "syncToken" : "1305555929000",
     * "nativeType" : "JAVA_TYPE_LONG"
     * }}
     *
     * @param token
     * @return
     * @throws JsonValueException        if {@code syncToken} is null or {@code nativeType} is not String
     * @throws IllegalArgumentException if the value of {@code syncToken} can not be converted to expected type.
     */
    public static SyncToken convertToSyncToken(JsonValue token) {
        JsonValue nativeType = token.get(OPENICF_NATIVE_TYPE);
        JsonValue tokenValue = token.get(OPENICF_SYNC_TOKEN).required();
        SyncToken result = null;
        if (null == nativeType) {
            result = new SyncToken(tokenValue.getObject());
        } else {
            result = new SyncToken(coercedTypeCasting(tokenValue.getObject(), findClassForName(nativeType.asString())));
        }
        return result;
    }

    /**
     * Create a new Map from the given {@link SyncToken}.
     * <p/>
     * The target object:
     * {@code
     * {
     * "syncToken" : "1305555929000",
     * "nativeType" : "JAVA_TYPE_LONG"
     * }}
     *
     * @param token
     * @return
     * @throws IllegalArgumentException if the value of {@code token} can not be converted to simple Java type.
     */
    public static Map<String, Object> convertFromSyncToken(SyncToken token) {
        Map<String, Object> result = new HashMap<String, Object>(2);
        result.put(OPENICF_NATIVE_TYPE, findNameForClass(token.getValue().getClass()));
        result.put(OPENICF_SYNC_TOKEN, coercedTypeCasting(token.getValue(), Object.class));
        return result;
    }

    public static String normalizeConnectorName(String connectorName) {
        String name = null;
        if (connectorName != null) {
            int lastDot = connectorName.lastIndexOf(46);
            if (lastDot != -1) {
                name = connectorName.substring(lastDot + 1);
            }
        }

        return name;
    }

    /**
     * Coerce the {@code source} object to an object of {@code clazz} type.
     * <p/>
     *
     * @param <T>
     * @param source
     * @param clazz
     * @return
     * @throws NumberFormatException
     * @throws URISyntaxException
     * @throws UnsupportedOperationException
     */
    @SuppressWarnings("unchecked")
    public static <T> T coercedTypeCasting(Object source, Class<T> clazz) throws IllegalArgumentException {
        if (null == clazz) {
            throw new IllegalArgumentException("Target Class can not be null");
        }
        if (source instanceof JsonValue) {
            source = ((JsonValue)source).getObject();
        }

        Class<T> targetClazz = clazz;
        Class sourceClass = (source == null ? null : source.getClass());
        boolean coerced = false;
        T result = null;
        try {
            if (source == null) {
                return null;
            }

            //Default JSON Type conversion
            if (targetClazz.equals(Object.class)) {
                if ((Number.class.isAssignableFrom(sourceClass)) || (int.class == clazz) || (double.class == clazz) || (float.class == clazz) || (long.class == clazz)) {
                    return (T) source;
                } else if ((Boolean.class.isAssignableFrom(sourceClass)) || (boolean.class == clazz)) {
                    return (T) source;
                } else if (String.class.isAssignableFrom(sourceClass)) {
                    return (T) source;
                } else if (Map.class.isAssignableFrom(sourceClass)) {
                    return (T) source;
                } else if (List.class.isAssignableFrom(sourceClass)) {
                    return (T) source;
                } else if (byte[].class.isAssignableFrom(sourceClass) || Byte[].class.isAssignableFrom(sourceClass) || byte[].class == clazz) {
                    return (T) source;
                } else if (sourceClass.isArray()) {
                    return (T) Arrays.asList(source); //Items in array may need to be converted too.
                } else if (sourceClass == QualifiedUid.class) {
                    //@TODO: Not null safe!!!
                    Map<String, Object> v = new HashMap<String, Object>(2);
                    v.put("_id", ((QualifiedUid) source).getUid().getUidValue());
                    v.put("_type", ((QualifiedUid) source).getObjectClass().getObjectClassValue());
                    return (T) v;
                } else if (sourceClass == Script.class) {
                    Map<String, Object> v = new HashMap<String, Object>(2);
                    v.put("scriptLanguage", ((Script) source).getScriptLanguage());
                    v.put("scriptText", ((Script) source).getScriptText());
                    return (T) v;
                } else {
                    targetClazz = (Class<T>) String.class;
                }
            }

            if (targetClazz.isAssignableFrom(sourceClass)) {
                return (T) source;
            } else if (targetClazz == sourceClass) {
                return (T) source;
            } else if (targetClazz.equals(java.math.BigDecimal.class)) {
                if (Double.class.isAssignableFrom(sourceClass) || sourceClass == double.class) {
                    result = (T) BigDecimal.valueOf((Double) source);
                    coerced = true;
                } else if (Integer.class.isAssignableFrom(sourceClass) || sourceClass == int.class) {
                    result = (T) BigDecimal.valueOf((Integer) source);
                    coerced = true;
                } else if (Long.class.isAssignableFrom(sourceClass) || sourceClass == long.class) {
                    result = (T) BigDecimal.valueOf((Long) source);
                    coerced = true;
                } else if (sourceClass == String.class) {
                    java.math.BigDecimal v = new java.math.BigDecimal((String) source);
                    result = targetClazz.cast(v);
                    coerced = true;
                }
            } else if (targetClazz.equals(java.math.BigInteger.class)) {
                if (Long.class.isAssignableFrom(sourceClass) || sourceClass == long.class) {
                    result = (T) BigInteger.valueOf((Long) source);
                    coerced = true;
                } else if (sourceClass == String.class) {
                    java.math.BigInteger v = new java.math.BigInteger((String) source);
                    result = targetClazz.cast(v);
                    coerced = true;
                } else {
                    result = (T) BigInteger.valueOf(coercedTypeCasting(source, Long.class));
                    coerced = true;
                }
            } else if (targetClazz.equals(boolean.class) || targetClazz.equals(Boolean.class)) {
                if (sourceClass == Boolean.class) {
                    result = (T) source;
                    coerced = true;
                } else if (sourceClass == Integer.class) {
                    int val = ((Integer) source).intValue();
                    if (val == 0) {
                        result = targetClazz.cast(Boolean.FALSE);
                        coerced = true;
                    } else if (val == 1) {
                        result = targetClazz.cast(Boolean.TRUE);
                        coerced = true;
                    }
                } else if (sourceClass == String.class) {
                    String s = (String) source;
                    if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) {
                        result = (T) Boolean.valueOf((String) source);
                        coerced = true;
                    }
                }
            } else if (targetClazz.equals(Byte.TYPE) || targetClazz.equals(Byte.class)) {
                if (sourceClass == Byte.TYPE || sourceClass == Byte.class) {
                    result = (T) source;
                    coerced = true;
                } else if (sourceClass == String.class) {
                    if (targetClazz.equals(Byte.class)) {
                        result = (T) new Byte((String) source);
                        coerced = true;
                    } else {
                        result = (T) Byte.valueOf((String) source);
                        coerced = true;
                    }
                } else if (source instanceof Number) {
                    int sourceInt = ((Number) source).intValue();
                    if (targetClazz.equals(Byte.TYPE)) {
                        result = (T) Byte.valueOf((byte) sourceInt);
                        coerced = true;
                    } else if (sourceInt > Byte.MIN_VALUE || sourceInt < Byte.MAX_VALUE) {
                        result = (T) Byte.valueOf((byte) sourceInt);
                        coerced = true;
                    }
                }
            } else if (targetClazz.equals(byte[].class)) {
                if (sourceClass == String.class) {
                    result = targetClazz.cast(Base64.decode((String) source));
                    coerced = true;
                } else if (sourceClass == GuardedByteArray.class) {
                    GuardedByteArray gba = (GuardedByteArray) source;
                    byte[] byteArray = decrypt(gba);
                    result = targetClazz.cast(byteArray);
                    coerced = true;
                }
            } else if ((targetClazz.equals(Character.class)) || (targetClazz.equals(char.class))) {
                if (sourceClass == String.class) {
                    Character v = ((String) source).charAt(0);
                    result = (T) v;
                    coerced = true;
                }
            } else if (targetClazz.equals(Character[].class)) {
                if (sourceClass == String.class) {
                    char[] charArray = ((String) source).toCharArray();
                    Character[] characterArray = new Character[charArray.length];
                    for (int i = 0; i < charArray.length; i++) {
                        characterArray[i] = new Character(charArray[i]);
                    }
                    result = targetClazz.cast(characterArray);
                    coerced = true;
                }
            } else if (targetClazz.equals(char[].class)) {
                if (sourceClass == String.class) {
                    char[] charArray = ((String) source).toCharArray();
                    result = targetClazz.cast(charArray);
                    coerced = true;
                }
            } else if (targetClazz.equals(Date.class)) {
                if (sourceClass == String.class) {
                    //TODO AttributeUtil.getDateValue()
                }
            } else if (targetClazz.equals(double.class)) {
                if (sourceClass == Double.class) {
                    result = (T) source;
                    coerced = true;
                } else if (sourceClass == int.class) {
                    //noinspection BoxingBoxedValue
                    result = (T) Double.valueOf((((Integer) source).doubleValue()));
                    coerced = true;
                } else if (sourceClass == Integer.class) {
                    result = (T) Double.valueOf(((Integer) source).doubleValue());
                    coerced = true;
                } else if (sourceClass == String.class) {
                    result = targetClazz.cast(Double.valueOf((String) source));
                    coerced = true;
                }
            } else if (targetClazz.equals(Double.class)) {
                if (sourceClass == double.class) {
                    result = (T) source;
                    coerced = true;
                } else if (sourceClass == int.class) {
                    result = (T) Double.valueOf((((Integer) source).doubleValue()));
                    coerced = true;
                } else if (sourceClass == Integer.class) {
                    result = (T) Double.valueOf(((Integer) source).doubleValue());
                    coerced = true;
                } else if (sourceClass == String.class) {
                    result = targetClazz.cast(Double.valueOf((String) source));
                    coerced = true;
                }
            } else if (targetClazz.equals(java.io.File.class)) {
                if (sourceClass == String.class) {
                    result = (T) new File((String) source);
                    coerced = true;
                }
            } else if (targetClazz.equals(float.class) || targetClazz.equals(Float.class)) {
                if (sourceClass == Float.class || sourceClass == float.class) {
                    result = (T) source;
                    coerced = true;
                } else if (sourceClass == Double.class || sourceClass == double.class) {
                    result = (T) new Float((Double) source);
                    coerced = true;
                } else if (sourceClass == int.class) {
                    result = (T) Float.valueOf((((Integer) source).floatValue()));
                    coerced = true;
                } else if (sourceClass == Integer.class) {
                    result = (T) Float.valueOf(((Integer) source).floatValue());
                    coerced = true;
                } else if (sourceClass == String.class) {
                    result = (T) Float.valueOf((String) source);
                    coerced = true;
                }
            } else if (targetClazz.equals(GuardedByteArray.class)) {
                if (sourceClass == String.class) {
                    byte[] byteArray = ((String) source).getBytes();
                    GuardedByteArray v = new GuardedByteArray(byteArray);
                    result = targetClazz.cast(v);
                    coerced = true;
                }
            } else if (targetClazz.equals(GuardedString.class)) {
                if (sourceClass == String.class) {
                    char[] charArray = ((String) source).toCharArray();
                    GuardedString v = new GuardedString(charArray);
                    result = targetClazz.cast(v);
                    coerced = true;
                }
            } else if (targetClazz.equals(int.class) || targetClazz.equals(Integer.class)) {
                if (sourceClass == Integer.class || sourceClass == int.class) {
                    result = (T) source;
                    coerced = true;
                } else if (sourceClass == String.class) {
                    result = (T) Integer.valueOf((String) source);
                    coerced = true;
                } else if (sourceClass == Float.class) {
                    result = targetClazz.cast(((Float) source).intValue());
                    coerced = true;
                } else if (sourceClass == Long.class) {
                    Long l = (Long) source;
                    if (l.longValue() <= Integer.MAX_VALUE) {
                        result = targetClazz.cast(l.intValue());
                        coerced = true;
                    }
                } else if (sourceClass == Boolean.class) {
                    boolean val = ((Boolean) source).booleanValue();
                    if (val) {
                        result = targetClazz.cast(1);
                    } else {
                        result = targetClazz.cast(new Integer(0));
                    }
                    coerced = true;
                }
            } else if (targetClazz.equals(long.class) || targetClazz.equals(Long.class)) {
                if (sourceClass == int.class) {
                    result = (T) Long.valueOf((((Integer) source).longValue()));
                    coerced = true;
                } else if (sourceClass == Integer.class) {
                    result = (T) Long.valueOf(((Integer) source).longValue());
                    coerced = true;
                } else if (sourceClass == Long.class || sourceClass == long.class) {
                    result = (T) source;
                    coerced = true;
                } else if (sourceClass == String.class) {
                    result = (T) Long.valueOf((String) source);
                    coerced = true;
                }
            } else if (targetClazz.equals(Name.class)) {
                if (sourceClass == String.class) {
                    result = targetClazz.cast(new Name((String) source));
                    coerced = true;
                }
            } else if (targetClazz.equals(ObjectClass.class)) {
                if (sourceClass == String.class) {
                    ScriptBuilder sb = new ScriptBuilder();
                    sb.setScriptLanguage("");
                    sb.setScriptText("");
                    result = targetClazz.cast(sb.build());
                    coerced = true;
                }
            } else if (targetClazz.equals(QualifiedUid.class)) {
                if (sourceClass == String.class) {
                    ScriptBuilder sb = new ScriptBuilder();
                    sb.setScriptLanguage("");
                    sb.setScriptText("");
                    result = targetClazz.cast(sb.build());
                    coerced = true;
                }
            } else if (targetClazz.equals(Script.class)) {
                if (sourceClass == String.class) {
                    ScriptBuilder sb = new ScriptBuilder();
                    sb.setScriptLanguage("");
                    sb.setScriptText("");
                    result = targetClazz.cast(sb.build());
                    coerced = true;
                } else if (Map.class.isAssignableFrom(sourceClass)) {
                    ScriptBuilder sb = new ScriptBuilder();
                    sb.setScriptLanguage((String) ((Map) source).get("scriptLanguage"));
                    sb.setScriptText((String) ((Map) source).get("scriptText"));
                    result = targetClazz.cast(sb.build());
                    coerced = true;
                }
            } else if (targetClazz.equals(String.class)) {
                if (sourceClass == byte[].class) {
                    result = (T) Base64.encode((byte[])source);
                    coerced = true;
                } else if (sourceClass == Byte.class) {
                    result = (T) Base64.encode(new byte[] {((Byte)source).byteValue()});
                    coerced = true;
                } else if (sourceClass == char.class) {
                    result = (T) new String((char[]) source);
                    coerced = true;
                } else if (sourceClass == Character[].class) {
                    Character[] characterArray = (Character[]) source;
                    char[] charArray = new char[characterArray.length];
                    for (int i = 0; i < characterArray.length; i++) {
                        charArray[i] = characterArray[i];
                    }
                    result = (T) new String(charArray);
                    coerced = true;
                } else if (sourceClass == Double.class) {
                    String s = ((Double) source).toString();
                    result = targetClazz.cast(s);
                    coerced = true;
                } else if (sourceClass == Float.class) {
                    String s = ((Float) source).toString();
                    result = targetClazz.cast(s);
                    coerced = true;
                } else if (sourceClass == Boolean.class) {
                    Boolean b = (Boolean) source;
                    result = targetClazz.cast(Boolean.toString(b.booleanValue()));
                    coerced = true;
                } else if (sourceClass == Long.class) {
                    String s = ((Long) source).toString();
                    result = targetClazz.cast(s);
                    coerced = true;
                } else if (sourceClass == Integer.class) {
                    String s = ((Integer) source).toString();
                    result = targetClazz.cast(s);
                    coerced = true;
                } else if (sourceClass == java.math.BigInteger.class) {
                    String s = source.toString();
                    result = targetClazz.cast(s);
                    coerced = true;
                } else if (sourceClass == java.math.BigDecimal.class) {
                    String s = ((java.math.BigDecimal) source).toString();
                    result = targetClazz.cast(s);
                    coerced = true;
                } else if (sourceClass == java.io.File.class) {
                    File file = (File) source;
                    String s = file.getPath();
                    result = targetClazz.cast(s);
                    coerced = true;
                } else if (sourceClass == java.net.URI.class) {
                    java.net.URI uri = (java.net.URI) source;
                    String s = uri.toString();
                    result = targetClazz.cast(s);
                    coerced = true;
                } else if (sourceClass == Character.class) {
                    Character c = (Character) source;
                    char[] charArray = new char[1];
                    charArray[0] = c.charValue();
                    String s = new String(charArray);
                    result = targetClazz.cast(s);
                    coerced = true;
                } else if (sourceClass == GuardedString.class) {
                    String s = decrypt((GuardedString) source);
                    result = targetClazz.cast(s);
                    coerced = true;
                } else if (sourceClass == GuardedByteArray.class) {
                    byte[] s = decrypt((GuardedByteArray) source);
                    result = targetClazz.cast(new String(s));
                    coerced = true;
                }
            } else if (targetClazz.equals(Uid.class)) {
                if (sourceClass == String.class) {
                    Uid v = new Uid((String) source);
                    result = targetClazz.cast(v);
                    coerced = true;
                }
            } else if (targetClazz.equals(java.net.URI.class)) {
                if (sourceClass == String.class) {
                    try {
                        java.net.URI v = new java.net.URI((String) source);
                        result = targetClazz.cast(v);
                        coerced = true;
                    } catch (URISyntaxException e) {
                        throw new IOException(e);
                    }
                }
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error("Failed to coerce {} from {} to {} ",
                        source,
                        sourceClass != null ? sourceClass.getCanonicalName() : "??",
                        targetClazz.getCanonicalName(),
                        e);
            } else {
                logger.error("Failed to coerce from {} to {} ",
                        sourceClass != null ? sourceClass.getCanonicalName() : "??",
                        targetClazz.getCanonicalName(),
                        e);
            }
            throw new IllegalArgumentException(source.getClass().getCanonicalName() + " to " + targetClazz.getCanonicalName(), e);
        }

        if (!coerced) {
            logger.error("Can not coerce {} to {}", sourceClass.getCanonicalName(), targetClazz.getCanonicalName());
            throw new IllegalArgumentException(source.getClass().getCanonicalName() + " to " + targetClazz.getCanonicalName());
        }
        return result;
    }

    private static byte[] decrypt(GuardedByteArray guardedByteArray) {
        final ByteArrayOutputStream clearStream = new ByteArrayOutputStream();
        GuardedByteArray.Accessor accessor = new GuardedByteArray.Accessor() {

            public void access(byte[] clearBytes) {
                clearStream.write(clearBytes, 0, clearBytes.length);
            }
        };
        guardedByteArray.access(accessor);
        return clearStream.toByteArray();
    }

    private static String decrypt(GuardedString guardedString) {
        final String[] clearText = new String[1];
        GuardedString.Accessor accessor = new GuardedString.Accessor() {

            public void access(char[] clearChars) {
                clearText[0] = new String(clearChars);
            }
        };

        guardedString.access(accessor);
        return clearText[0];
    }
}
