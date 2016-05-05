/**
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
 *
 */
package org.forgerock.openidm.config.crypto;

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.openidm.config.enhanced.InternalErrorException;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.config.installer.JSONConfigInstaller;
import org.forgerock.openidm.config.installer.JSONPrettyPrint;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.NotConfiguration;
import org.forgerock.openidm.metadata.WaitForMetaData;
import org.forgerock.openidm.metadata.impl.ProviderListener;
import org.forgerock.openidm.metadata.impl.ProviderTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Configuration encryption support
 *
 *
 */
public class ConfigCrypto {
    final static Logger logger = LoggerFactory.getLogger(ConfigCrypto.class);

    static ServiceTracker cryptoTracker;
    static ConfigCrypto instance;

    BundleContext context;
    ObjectMapper mapper = new ObjectMapper();

    String alias = "openidm-config-default";

    JSONPrettyPrint prettyPrint = new JSONPrettyPrint();

    ProviderTracker providerTracker;
    ProviderListener delayedHandler;

    private ConfigCrypto(BundleContext context, ProviderListener delayedHandler) {
        this.context = context;
        this.delayedHandler = delayedHandler;
        this.delayedHandler.init(this);
        alias = IdentityServer.getInstance().getProperty("openidm.config.crypto.alias", "openidm-config-default");
        logger.info("Using keystore alias {} to handle config encryption", alias);

        providerTracker = new ProviderTracker(context, delayedHandler, false);

        // TODO: add bundle listeners to track new installs and remove uninstalls
    }

    public synchronized static ConfigCrypto getInstance(BundleContext context, ProviderListener providerListener) {
        if (instance == null) {
            instance = new ConfigCrypto(context, providerListener);
        }
        return instance;
    }

    /**
     * Check each provider for meta-data for a given pid until the first match is found
     * Requested each time configuration is changed so that meta data providers can handle additional plug-ins
     *
     * @param pidOrFactory the pid or factory pid
     * @param factoryAlias the alias of the factory configuration instance
     * @return the list of properties to encrypt
     */
    public List<JsonPointer> getPropertiesToEncrypt(String pidOrFactory, String factoryAlias, JsonValue parsed)
            throws WaitForMetaData {
        Collection<MetaDataProvider> providers = providerTracker.getProviders();
        WaitForMetaData lastWaitException = null;
        for (MetaDataProvider provider : providers) {
            try {
                List<JsonPointer> result = provider.getPropertiesToEncrypt(pidOrFactory, factoryAlias, parsed);
                if (result != null) {
                    return result;
                }
            } catch (WaitForMetaData ex) {
                // Continue to check if another meta data provider can resolve the meta data
                lastWaitException = ex;
            } catch (NotConfiguration e) {
                logger.error("Error getting additional properties to encrypt", e);
            }
        }
        if (lastWaitException != null) {
            throw lastWaitException;
        }

        return null;
    }

    /**
     * Encrypt properties in the configuration if necessary
     * Also results in pretty print formatting of the JSON configuration.
     *
     * @param pidOrFactory the PID of either the managed service; or for factory configuration the PID of the Managed Service Factory
     * @param instanceAlias null for plain managed service, or the subname (alias) for the managed factory configuration instance
     * @param config The OSGi configuration
     * @return The configuration with any properties encrypted that a component's meta data marks as encrypted
     * @throws InvalidException if the configuration was not valid JSON and could not be parsed
     * @throws InternalErrorException if parsing or encryption failed for technical, possibly transient reasons
     */
    public Dictionary encrypt(String pidOrFactory, String instanceAlias, Dictionary config)
            throws InvalidException, InternalErrorException, WaitForMetaData {

        JsonValue parsed = parse(config, pidOrFactory);
        return encrypt(pidOrFactory, instanceAlias, config, parsed);
    }

    public Dictionary encrypt(String pidOrFactory, String instanceAlias, Dictionary existingConfig, JsonValue newConfig)
            throws WaitForMetaData {

        JsonValue parsed = newConfig;
        Dictionary encrypted = (existingConfig == null ? new Hashtable() : existingConfig); // Default to existing

        List<JsonPointer> props = getPropertiesToEncrypt(pidOrFactory, instanceAlias, parsed);
        if (logger.isTraceEnabled()) {
            logger.trace("Properties to encrypt for {} {}: {}", new Object[] {pidOrFactory, instanceAlias, props});
        }
        if (props != null && !props.isEmpty()) {
            boolean modified = false;
            CryptoService crypto = getCryptoService(context);
            for (JsonPointer pointer : props) {
                logger.trace("Handling property to encrypt {}", pointer);

                JsonValue valueToEncrypt = parsed.get(pointer);
                if (null != valueToEncrypt && !valueToEncrypt.isNull() && !crypto.isEncrypted(valueToEncrypt)) {

                    if (logger.isTraceEnabled()) {
                        logger.trace("Encrypting {} with cipher {} and alias {}", new Object[] {pointer,
                                ServerConstants.SECURITY_CRYPTOGRAPHY_DEFAULT_CIPHER, alias});
                    }

                    // Encrypt and replace value
                    try {
                        JsonValue encryptedValue = crypto.encrypt(valueToEncrypt,
                                ServerConstants.SECURITY_CRYPTOGRAPHY_DEFAULT_CIPHER, alias);
                        parsed.put(pointer, encryptedValue.getObject());
                        modified = true;
                    } catch (JsonCryptoException ex) {
                        throw new InternalErrorException("Failure during encryption of configuration "
                                + pidOrFactory + "-" + instanceAlias + " for property " + pointer.toString()
                                + " : " + ex.getMessage(), ex);
                    }
                }
            }
        }
        String value = null;
        try {
            ObjectWriter writer = prettyPrint.getWriter();
            value = writer.writeValueAsString(parsed.asMap());
        } catch (Exception ex) {
            throw new InternalErrorException("Failure in writing formatted and encrypted configuration "
                    + pidOrFactory + "-" + instanceAlias + " : " + ex.getMessage(), ex);
        }

        encrypted.put(JSONConfigInstaller.JSON_CONFIG_PROPERTY, value);
        
        if (logger.isDebugEnabled()) {
            logger.debug("Config with senstiive data encrypted {} {} : {}", 
                    new Object[] {pidOrFactory, instanceAlias, encrypted});
        }
        
        return encrypted;
    }

    /**
     * Parse the OSGi configuration in JSON format
     * 
     * @param dict the OSGi configuration
     * @param serviceName a name for the configuration getting parsed for logging purposes
     * @return The parsed JSON structure
     * @throws InvalidException if the configuration was not valid JSON and could not be parsed
     * @throws InternalErrorException if parsing failed for technical, possibly transient reasons
     */
    public JsonValue parse(Dictionary<String, Object> dict, String serviceName)
            throws InvalidException, InternalErrorException {
        JsonValue jv = new JsonValue(new HashMap<String, Object>());
        
        if (dict != null) {
            Map<String, Object> parsedConfig = null;
            String jsonConfig = (String) dict.get(JSONConfigInstaller.JSON_CONFIG_PROPERTY);

            try {
                if (jsonConfig != null && jsonConfig.trim().length() > 0) {
                    parsedConfig = mapper.readValue(jsonConfig, Map.class);
                }
            } catch (Exception ex) {
                throw new InvalidException("Configuration for " + serviceName
                                + " could not be parsed and may not be valid JSON : " + ex.getMessage(), ex);
            }

            try {
                jv = new JsonValue(parsedConfig);
            } catch (JsonValueException ex) {
                throw new InvalidException("Component configuration for " + serviceName
                                + " is invalid: " + ex.getMessage(), ex);
            }
        }
        logger.debug("Parsed configuration for {}", serviceName);

        return jv;
    }

    private CryptoService getCryptoService(BundleContext context)
            throws InternalErrorException {
        CryptoService crypto = null;

        try {
            synchronized (JSONEnhancedConfig.class) {
                if (cryptoTracker == null) {
                    Filter cryptoFilter = context.createFilter("("
                            + Constants.OBJECTCLASS + "="
                            + CryptoService.class.getName() + ")");
                    cryptoTracker = new ServiceTracker(context, cryptoFilter,
                            null);
                    cryptoTracker.open();
                }
            }

            crypto = (CryptoService) cryptoTracker.waitForService(5000);
            if (crypto != null) {
                logger.trace("Obtained crypto service");
            } else {
                logger.warn("Failed to get crypto service to handle configuration encryption");
                if (logger.isTraceEnabled()) {
                    logger.trace("List of available service {}", 
                            Arrays.asList(context.getAllServiceReferences(null, null)));
                }
                throw new InternalErrorException(
                        "Configuration handling could not locate cryptography service to encrypt configuration." 
                        + " Cryptography service is not registered..");
            }
        } catch (Exception ex) {
            logger.warn("Exception in getting crypto service to handle configuration encryption", ex);
            throw new InternalErrorException("Exception in getting cryptography service to encrypt configuration "
                            + ex.getMessage(), ex);
        }
        return crypto;
    }
}
