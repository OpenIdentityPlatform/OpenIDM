/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.config.internal;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.fileinstall.internal.DirectoryWatcher;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.openidm.config.ConfigurationManager;
import org.forgerock.openidm.config.InternalErrorException;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.config.installer.JSONConfigInstaller;
import org.forgerock.openidm.config.installer.JSONPrettyPrint;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.MetaDataProviderCallback;
import org.forgerock.openidm.metadata.NotConfiguration;
import org.forgerock.openidm.metadata.WaitForMetaData;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class ConfigurationManagerImpl
        implements
        ConfigurationManager,
        ServiceTrackerCustomizer<MetaDataProvider, ConfigurationManagerImpl.MetaDataProviderHelper>,
        BundleTrackerCustomizer<ConfigurationManagerImpl.MetaDataProviderHelper> {

    /**
     * Setup logging for the {@link ConfigurationManagerImpl}.
     */
    final static Logger logger = LoggerFactory.getLogger(ConfigurationManagerImpl.class);

    private final BundleContext bundleContext;

    private final ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> configurationAdminTracker;

    private final ServiceTracker<CryptoService, CryptoService> cryptoServiceTracker;

    private final ServiceTracker<MetaDataProvider, MetaDataProviderHelper> serviceProviderTracker;

    private final BundleTracker<MetaDataProviderHelper> bundleProviderTracker;

    private ServiceRegistration selfServiceRegistration = null;

    private ObjectMapper mapper = new ObjectMapper();

    private JSONPrettyPrint prettyPrint = new JSONPrettyPrint();

    String alias = "openidm-config-default";

    // Three phase storage of DelayedConfigs

    /**
     * Store DelayedConfig before MetaDataProvider processes it.
     */
    private final ConcurrentMap<String, DelayedConfig> delayedConfigs =
            new ConcurrentHashMap<String, DelayedConfig>();

    private final ConcurrentMap<String, DelayedConfig> delayedConfigsToEncrypt =
            new ConcurrentHashMap<String, DelayedConfig>();

    private final ConcurrentMap<String, DelayedConfig> delayedConfigsToInstall =
            new ConcurrentHashMap<String, DelayedConfig>();

    ServiceRegistration jsonFileInstallerService = null;

    Configuration jsonFileInstaller = null;

    public ConfigurationManagerImpl(BundleContext context) {
        this.bundleContext = context;
        alias =
                IdentityServer.getInstance().getProperty("openidm.config.crypto.alias",
                        "openidm-config-default");
        logger.info("Using keystore alias {} to handle config encryption", alias);

        configurationAdminTracker =
                new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(bundleContext,
                        ConfigurationAdmin.class, new ConfigurationAdminCustomizer());

        cryptoServiceTracker =
                new ServiceTracker<CryptoService, CryptoService>(bundleContext,
                        CryptoService.class, new CryptoServiceCustomizer());

        serviceProviderTracker =
                new ServiceTracker<MetaDataProvider, MetaDataProviderHelper>(bundleContext,
                        MetaDataProvider.class, this);

        bundleProviderTracker =
                new BundleTracker<MetaDataProviderHelper>(context, Bundle.STOPPING | Bundle.ACTIVE,
                        this);

    }

    void start() {
        // Start monitoring Configurations and Artifact and cache then in the
        // ConfigurationManager. The manager can install these configurations
        // when it's ready.
        JSONConfigInstaller installer = new JSONConfigInstaller(bundleContext, this);
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(Constants.SERVICE_DESCRIPTION, "Config installer for JSON files");
        properties.put(Constants.SERVICE_VENDOR, ServerConstants.SERVER_VENDOR_NAME);

        String[] clazzes = null;
        // If we don't want to save configurations back to JSON files then
        // disable the ConfigurationListener service from registration
        Object obj = bundleContext.getProperty(DirectoryWatcher.DISABLE_CONFIG_SAVE);
        if (obj instanceof String && Boolean.valueOf((String) obj)) {
            clazzes =
                    new String[] { ArtifactInstaller.class.getName(),
                        ConfigurationListener.class.getName() };
        } else {
            clazzes = new String[] { ArtifactInstaller.class.getName() };
        }
        jsonFileInstallerService = bundleContext.registerService(clazzes, installer, properties);
        logger.debug("JSON configuration installer service registered");

        configurationAdminTracker.open();
        cryptoServiceTracker.open();
        serviceProviderTracker.open();
        bundleProviderTracker.open();
        properties = new Hashtable<String, String>();
        properties.put(Constants.SERVICE_DESCRIPTION, "OpenIDM ConfigurationManager");
        properties.put(Constants.SERVICE_VENDOR, ServerConstants.SERVER_VENDOR_NAME);
        selfServiceRegistration =
                bundleContext.registerService(ConfigurationManager.class, this, properties);

    }

    void stop() {
        if (null != jsonFileInstaller) {
            try {
                jsonFileInstaller.delete();
            } catch (Exception e) {
                logger.error("Failed to disable and delete the JSON config polling", e);
            }
        }
        if (null != jsonFileInstallerService) {
            jsonFileInstallerService.unregister();
            jsonFileInstallerService = null;
        }
        if (null != selfServiceRegistration) {
            // Ready to stop
            selfServiceRegistration.unregister();
            selfServiceRegistration = null;
        }
        bundleProviderTracker.close();
        serviceProviderTracker.close();
        cryptoServiceTracker.close();
        configurationAdminTracker.close();
    }

    // ----- Handle the lifecycle of the MetaDataProviders

    @Override
    public MetaDataProviderHelper addingBundle(Bundle bundle, BundleEvent event) {
        if (logger.isTraceEnabled()) {
            logger.trace("Scanning bundle {} for metadata file", bundle.getBundleId());
        }

        // If tracker process the initial bundles then the event is null so we
        // check the BundleContext to make sure it's started
        if ((null == event && null != bundle.getBundleContext())
                || BundleEvent.STARTED == event.getType()) {
            Enumeration<URL> entries =
                    bundle.findEntries("org/forgerock/metadata", "bundle.json", true);
            try {
                if (entries != null && entries.hasMoreElements()) {
                    URL entryUrl = entries.nextElement();
                    logger.trace("Found metadata file, load and parse {}", entryUrl);
                    Map metaConfig = mapper.readValue(entryUrl, Map.class);

                    String providerClazzName = (String) metaConfig.get("metaDataProvider");
                    logger.trace("Loading declared MetaDataProvider {}", providerClazzName);
                    if (providerClazzName == null) {
                        logger.trace(
                                "No MetaDataProvider class declared in meta data file {} for {}",
                                entryUrl, bundle.getSymbolicName());
                    } else {
                        logger.trace("Loading declared MetaDataProvider {}", providerClazzName);
                        Class providerClazz = bundle.loadClass(providerClazzName);
                        MetaDataProvider provider = null;

                        try {
                            Constructor constructor = providerClazz.getConstructor(Bundle.class);
                            provider = (MetaDataProvider) constructor.newInstance(bundle);
                        } catch (NoSuchMethodException e) {
                            // Do nothing try the default constructor
                            provider = (MetaDataProvider) providerClazz.newInstance();
                        } /* catch (SecurityException e) {} */

                        if (null != provider) {
                            return new MetaDataProviderHelper(
                                    new SoftReference<ConfigurationManagerImpl>(this), provider);
                        }
                        logger.debug("Registered MetaDataProvider {} for {}", providerClazzName,
                                bundle.getSymbolicName());
                    }
                }
            } catch (Exception ex) {
                logger.warn("Failed to obtain meta-data on handling configuration for {}", bundle
                        .getSymbolicName(), ex);
            }
        }
        return null;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, MetaDataProviderHelper object) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, MetaDataProviderHelper object) {
        if (null != object) {
            // Release resources
            object.destroy();
        }
    }

    @Override
    public MetaDataProviderHelper addingService(ServiceReference<MetaDataProvider> reference) {
        return new MetaDataProviderHelper(new SoftReference<ConfigurationManagerImpl>(this),
                bundleContext.getService(reference));
    }

    @Override
    public void modifiedService(ServiceReference<MetaDataProvider> reference,
            MetaDataProviderHelper service) {
        // TODO Should we fire a refresh event?
    }

    @Override
    public void removedService(ServiceReference<MetaDataProvider> reference,
            MetaDataProviderHelper service) {
        if (null != service) {
            service.destroy();
        }
        // TODO unget the service or the tracker does it?
        bundleContext.ungetService(reference);
    }

    // ----- Implementation of ConfigurationManager interface

    @Override
    public Resource installConfiguration(final String pid, final String factoryPid,
            final JsonValue configuration) throws ResourceException {

        Resource result = null;

        synchronized (this) {
            try {
                String key = getFullCanonicalName(pid, factoryPid);
                // Remove the previous versions from the delayed caches
                delayedConfigs.remove(key);
                delayedConfigsToInstall.remove(key);
                delayedConfigsToEncrypt.remove(key);

                JsonValue content = new JsonValue(new LinkedHashMap<String, Object>());
                DelayedConfig sourceConfig = new DelayedConfig(pid, factoryPid, configuration);
                DelayedConfig targetConfig = null;

                try {
                    // Process the config with the providers
                    for (MetaDataProviderHelper helper : bundleProviderTracker.getTracked()
                            .values()) {
                        try {
                            targetConfig = helper.process(sourceConfig);
                            if (null != targetConfig) {
                                // The provider could process the config
                                break;
                            }
                        } catch (WaitForMetaData e) {
                            // TODO should we stop and delay the config or
                            // continue
                            // the processing
                        }
                    }
                    if (targetConfig == null) {
                        Object[] service = serviceProviderTracker.getServices();
                        if (null != service && service.length > 0) {
                            for (Object helper : service) {
                                try {
                                    if (helper instanceof MetaDataProviderHelper) {
                                        targetConfig =
                                                ((MetaDataProviderHelper) helper)
                                                        .process(sourceConfig);
                                        if (null != targetConfig) {
                                            // The provider could process the
                                            // config
                                            break;
                                        }
                                    }
                                } catch (WaitForMetaData e) {
                                    // TODO should we stop and delay the config
                                    // or
                                    // continue the processing
                                }
                            }
                        }
                    }
                } catch (NotConfiguration e) {
                    return null;
                }

                // Encrypt, Install the config or put it into the cache
                if (null != targetConfig) {
                    if (targetConfig.sensitiveAttributes.isEmpty()) {
                        ConfigurationAdmin configurationAdmin = getConfigurationAdmin();
                        if (null != configurationAdmin) {
                            // May throw InternalServerErrorException then
                            // config is rejected
                            installConfiguration(configurationAdmin, targetConfig);
                        } else {
                            delayedConfigsToInstall.put(key, targetConfig);
                            content.put("location", "delayedInstall");
                        }
                    } else {
                        CryptoService cryptoService = getCryptoService();
                        if (null != cryptoService) {
                            // May throw InternalServerErrorException
                            encryptConfiguration(cryptoService, targetConfig);
                            ConfigurationAdmin configurationAdmin = getConfigurationAdmin();
                            if (null != configurationAdmin) {
                                // May throw InternalServerErrorException
                                // then
                                // config is rejected
                                installConfiguration(configurationAdmin, targetConfig);
                            } else {
                                delayedConfigsToInstall.put(key, targetConfig);
                                content.put("location", "delayedInstall");
                            }
                        } else {
                            delayedConfigsToEncrypt.put(key, targetConfig);
                            content.put("location", "delayedEncryption");
                        }
                    }
                } else {
                    delayedConfigs.put(key, sourceConfig);
                    content.put("location", "delayed");
                }
                result = new Resource(getCanonicalName(pid, factoryPid), null, content);
            } catch (ResourceException e) {
                throw e;
            } catch (ExecutionException e) {
                throw new InternalServerErrorException(e);
            }
        }
        return result;
    }

    @Override
    public Configuration getConfiguration(String pid, String factoryPid) {
        return null; // To change body of implemented methods use File |
                     // Settings | File Templates.
    }

    @Override
    public void deleteConfiguration(String pid, String factoryPid) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    @Override
    public void listConfigurations(ResultHandler<Resource> handler) {
        for (Map.Entry<String, DelayedConfig> entry : delayedConfigs.entrySet()) {
            JsonValue content = new JsonValue(new LinkedHashMap<String, Object>());
            content.put("location", "delayed");
            handler.handleResult(new Resource(entry.getKey(), null, content));
        }
        for (Map.Entry<String, DelayedConfig> entry : delayedConfigsToEncrypt.entrySet()) {
            JsonValue content = new JsonValue(new LinkedHashMap<String, Object>());
            content.put("location", "delayedEncryption");
            handler.handleResult(new Resource(entry.getKey(), null, content));
        }
        for (Map.Entry<String, DelayedConfig> entry : delayedConfigsToInstall.entrySet()) {
            JsonValue content = new JsonValue(new LinkedHashMap<String, Object>());
            content.put("location", "delayedInstall");
            handler.handleResult(new Resource(entry.getKey(), null, content));
        }
        // Local reference to ConfigurationAdmin
        final ConfigurationAdmin service = getConfigurationAdmin();
        if (null != service) {
            try {
                for (Configuration configuration : service.listConfigurations(null)) {
                    JsonValue content = new JsonValue(new LinkedHashMap<String, Object>());
                    content.put("location", "installed");
                    handler.handleResult(new Resource(getCanonicalName(configuration.getPid(),
                            configuration.getFactoryPid()), null, content));
                }
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e));
            }
        }
    }

    // ----- Non ThreadSafe methods called from CompletionService

    /**
     * Update the DelayedConfig in the input cache if the {@code helper} can get
     * the {@code PropertiesToEncrypt} and moves these configs to the buildNext
     * encryption cache.
     * 
     * @param helper
     */
    private void updateDelayedConfigs(MetaDataProviderHelper helper) {
        for (String key : delayedConfigs.keySet()) {
            DelayedConfig config = delayedConfigs.get(key);
            try {
                DelayedConfig result = helper.process(config);
                if (result != null) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Properties to encrypt for {} {}: {}", new Object[] {
                            config.pid, config.factoryPid, result });
                    }
                    // Handle this config
                    delayedConfigs.remove(key);
                    if (result.sensitiveAttributes.isEmpty()) {
                        delayedConfigsToInstall.put(key, config);
                    } else {
                        delayedConfigsToEncrypt.put(key, result);
                    }
                }
            } catch (WaitForMetaData ex) {
                // provider is not yet ready to handle this configuration
            } catch (NotConfiguration e) {
                delayedConfigs.remove(key);
                break;
            }
        }
    }

    /**
     * Update the DelayedConfig in the encryption cache and moves these configs
     * to the buildNext install cache.
     * 
     * @param service
     * @throws InternalServerErrorException
     */
    private void encryptDelayedConfigs(CryptoService service) throws InternalServerErrorException {
        for (String key : delayedConfigsToEncrypt.keySet()) {
            DelayedConfig config = delayedConfigsToEncrypt.get(key);
            if (config.sensitiveAttributes.isEmpty()) {
                delayedConfigsToEncrypt.remove(key);
                delayedConfigsToInstall.put(key, config);
            }
            if (null != service) {
                encryptConfiguration(service, config);
                delayedConfigsToEncrypt.remove(key);
                delayedConfigsToInstall.put(key, config);
            }
        }
    }

    private void installDelayedConfigs(ConfigurationAdmin service)
            throws InternalServerErrorException {
        for (String key : delayedConfigsToInstall.keySet()) {
            DelayedConfig config = delayedConfigsToInstall.get(key);

            if (null != installConfiguration(service, config)) {
                delayedConfigsToInstall.remove(key);
            }
        }
    }

    // ----- END Non ThreadSafe methods called from CompletionService

    // ----- Util Methods

    private ConfigurationAdmin getConfigurationAdmin() {
        if (configurationAdminTracker.size() > 0) {
            return configurationAdminTracker.getService();
        }
        return null;
    }

    private CryptoService getCryptoService() {
        if (cryptoServiceTracker.size() > 0) {
            return cryptoServiceTracker.getService();
        }
        return null;
    }

    private String getCanonicalName(String pid, String factoryPid) {
        if (pid.startsWith(DEFAULT_SERVICE_RDN_PREFIX)) {
            if (StringUtils.isNotBlank(factoryPid)) {
                return (pid.substring(DEFAULT_SERVICE_RDN_PREFIX.length()) + "/" + factoryPid)
                        .toLowerCase();
            } else {
                return pid.substring(DEFAULT_SERVICE_RDN_PREFIX.length()).toLowerCase();
            }
        } else {
            if (StringUtils.isNotBlank(factoryPid)) {
                return (DEFAULT_SERVICE_RDN_PREFIX + pid + "/" + factoryPid).toLowerCase();
            } else {
                return (DEFAULT_SERVICE_RDN_PREFIX + pid).toLowerCase();
            }
        }
    }

    private String getFullCanonicalName(String pid, String factoryPid) {
        if (pid.startsWith(DEFAULT_SERVICE_RDN_PREFIX)) {
            if (StringUtils.isNotBlank(factoryPid)) {
                return (pid + "/" + factoryPid).toLowerCase();
            } else {
                return pid.toLowerCase();
            }
        } else {
            if (StringUtils.isNotBlank(factoryPid)) {
                return (DEFAULT_SERVICE_RDN_PREFIX + pid + "/" + factoryPid).toLowerCase();
            } else {
                return (DEFAULT_SERVICE_RDN_PREFIX + pid).toLowerCase();
            }
        }
    }

    private void encryptConfiguration(CryptoService service, DelayedConfig config)
            throws InternalServerErrorException {
        for (JsonPointer pointer : config.sensitiveAttributes) {
            logger.trace("Handling property to encrypt {}", pointer);

            JsonValue valueToEncrypt = config.configuration.get(pointer);
            if (null != valueToEncrypt && !valueToEncrypt.isNull()
                    && !service.isEncrypted(valueToEncrypt)) {

                if (logger.isTraceEnabled()) {
                    logger.trace("Encrypting {} with cipher {} and alias {}", new Object[] {
                        pointer, ServerConstants.SECURITY_CRYPTOGRAPHY_DEFAULT_CIPHER, alias });
                }

                // Encrypt and replace value
                try {
                    JsonValue encryptedValue =
                            service.encrypt(valueToEncrypt,
                                    ServerConstants.SECURITY_CRYPTOGRAPHY_DEFAULT_CIPHER, alias);
                    config.configuration.put(pointer, encryptedValue.getObject());
                } catch (JsonCryptoException ex) {
                    throw new InternalServerErrorException(
                            "Failure during encryption of configuration " + config.pid + "-"
                                    + config.factoryPid + " for property " + pointer.toString()
                                    + " : " + ex.getMessage(), ex);
                }
            }
        }
    }

    private Configuration installConfiguration(ConfigurationAdmin service, DelayedConfig config)
            throws InternalServerErrorException {
        Configuration configuration = null;
                StringBuilder sb = new StringBuilder(config.pid);
        if (null != config.factoryPid) {
            sb.append('-').append(config.factoryPid);
        }
        String configKey = sb.toString();

        try {
            configuration = getConfiguration(service, config.pid, config.factoryPid);
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to get the configuration " + configKey,
                    e);
        }

        Dictionary props = configuration.getProperties();
        Hashtable existingConfig = props != null ? new Hashtable(new DictionaryAsMap(props)) : null;
        if (existingConfig != null) {
            existingConfig.remove( "felix.fileinstall.filename" );
            existingConfig.remove( Constants.SERVICE_PID );
            existingConfig.remove( ConfigurationAdmin.SERVICE_FACTORYPID );
        }
        Dictionary encrypted =  existingConfig == null ? new Hashtable() : existingConfig;

        try {
            ObjectWriter writer = prettyPrint.getWriter();
            String value = writer.writeValueAsString(config.configuration.asMap());

            encrypted.put(JSONConfigInstaller.JSON_CONFIG_PROPERTY, value);
            if (config.factoryPid instanceof String) {
                encrypted.put(ServerConstants.CONFIG_FACTORY_PID, config.factoryPid);
            }
            if (configuration.getBundleLocation() != null)
            {
                configuration.setBundleLocation(null);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Config with sensitive data encrypted {} {} : {}", new Object[] {
                    config.pid, config.factoryPid, encrypted });
            }
            configuration.update(encrypted);
            return configuration;
        } catch (Exception e) {
            logger.error("Failure to update formatted and encrypted configuration: {}-{}",
                    config.pid, config.factoryPid, e);
            throw new InternalServerErrorException(
                    "Failure to update formatted and encrypted configuration: " + config.pid + "-"
                            + config.factoryPid + " : " + e.getMessage(), e);
        }
    }


    protected Configuration getConfiguration(ConfigurationAdmin service, String pid, String factoryPid) throws Exception {
        Configuration oldConfiguration = findExistingConfiguration(service, pid, factoryPid);
        if (oldConfiguration != null) {
            if (factoryPid == null) {
                logger.info("Updating configuration from {}", pid);
            } else {
                logger.info("Updating configuration from {}-{}", pid, factoryPid);
            }
            return oldConfiguration;
        } else {
            Configuration newConfiguration;
            if (factoryPid != null) {
                /*if ("org.forgerock.openidm.router".equalsIgnoreCase(pid)) {
                    throw new ConfigurationException(factoryPid,
                            "router config can not be factory config");
                }*/
                newConfiguration = getConfigurationAdmin().createFactoryConfiguration(pid, null);
            } else {
                newConfiguration = getConfigurationAdmin().getConfiguration(pid, null);
            }
            return newConfiguration;
        }
    }

    Configuration findExistingConfiguration(ConfigurationAdmin service, String pid,
                                              String factoryPid) throws IOException {
        String filter = null;
        if (null == factoryPid) {
            filter = "(" + Constants.SERVICE_PID + "=" + pid + ")";
        } else {
            filter =
                    "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + pid
                            + ")(config.factory-pid=" + factoryPid + "))";
        }

        try {
            Configuration[] configurations = service.listConfigurations(filter);
            if (configurations != null && configurations.length > 0) {
                return configurations[0];
            }
        } catch (InvalidSyntaxException e) {
            logger.error("Failed to find existing configuration {}-{}", pid, factoryPid, e);
        }
        return null;
    }

    /**
     * Configure to process all JSON configuration files (if enabled)
     * 
     * @param configAdmin
     *            the OSGi configuration admin service
     * @throws java.io.IOException
     */
    public Configuration startPollingJSONConfigurations(ConfigurationAdmin configAdmin)
            throws IOException {

        IdentityServer system = IdentityServer.getInstance();
        String enabled = system.getProperty(OPENIDM_FILEINSTALL_ENABLED, "true");
        if ("true".equalsIgnoreCase(enabled)) {

            // Setup the config directory
            // TODO Share this method with JsonConfigInstaller
            String dir = system.getProperty(OPENIDM_FILEINSTALL_DIR, "conf");
            dir = IdentityServer.getFileForProjectPath(dir).getAbsolutePath();
            logger.debug("Configuration file directory {}", dir);

            // Get the other configurations from IdentityServer
            String poll = system.getProperty(OPENIDM_FILEINSTALL_POLL, "2000");
            String filter = system.getProperty(OPENIDM_FILEINSTALL_FILTER, ".*\\.cfg|.*\\.json");
            String start = system.getProperty(OPENIDM_FILEINSTALL_BUNDLES_NEW_START, "false");

            Configuration config =
                    findExistingConfiguration(configAdmin, FELIX_FILEINSTALL_PID, "openidm");

            if (null == config) {
                config = configAdmin.createFactoryConfiguration(FELIX_FILEINSTALL_PID, null);
            }

            Dictionary props = config.getProperties();
            if (props == null) {
                props = new Hashtable();
            }

            // Apply the latest configuration changes
            props.put("felix.fileinstall.poll", poll);
            props.put("felix.fileinstall.noInitialDelay", "true");
            props.put("felix.fileinstall.dir", dir);
            props.put("felix.fileinstall.filter", filter);
            props.put("felix.fileinstall.bundles.new.start", start);
            props.put(ServerConstants.CONFIG_FACTORY_PID, "openidm");
            config.update(props);
            logger.info("Configuration from file enabled");
            return config;
        } else {
            logger.info("Configuration from file disabled");
        }
        return null;
    }

    /**
     * Check each provider for meta-data for a given pid until the first match
     * is found Requested each time configuration is changed so that meta data
     * providers can handle additional plug-ins
     * 
     * @param pidOrFactory
     *            the pid or factory pid
     * @param factoryAlias
     *            the alias of the factory configuration newBuilder
     * @return the list of properties to encrypt
     */
    // public List<JsonPointer> getPropertiesToEncrypt(String pidOrFactory,
    // String factoryAlias,
    // JsonValue parsed) throws WaitForMetaData {
    // Collection<MetaDataProvider> providers = providerTracker.getProviders();
    // WaitForMetaData lastWaitException = null;
    // for (MetaDataProvider provider : providers) {
    // try {
    // List<JsonPointer> result =
    // provider.getPropertiesToEncrypt(pidOrFactory, factoryAlias, parsed);
    // if (result != null) {
    // return result;
    // }
    // } catch (WaitForMetaData ex) {
    // // Continue to check if another meta data provider can resolve
    // // the meta data
    // lastWaitException = ex;
    // }
    // }
    // if (lastWaitException != null) {
    // throw lastWaitException;
    // }
    //
    // return null;
    // }

    /**
     * Encrypt properties in the configuration if necessary Also results in
     * pretty print formatting of the JSON configuration.
     * 
     * @param pidOrFactory
     *            the PID of either the managed service; or for factory
     *            configuration the PID of the Managed Service Factory
     * @param instanceAlias
     *            null for plain managed service, or the subname (alias) for the
     *            managed factory configuration newBuilder
     * @param config
     *            The OSGi configuration
     * @return The configuration with any properties encrypted that a
     *         component's meta data marks as encrypted
     * @throws org.forgerock.openidm.config.InvalidException
     *             if the configuration was not valid JSON and could not be
     *             parsed
     * @throws org.forgerock.openidm.config.InternalErrorException
     *             if parsing or encryption failed for technical, possibly
     *             transient reasons
     */
    // public Dictionary encrypt(String pidOrFactory, String instanceAlias,
    // Dictionary config)
    // throws InvalidException, InternalErrorException, WaitForMetaData {
    // // TODO Fix when cryptoService is null
    // JsonValue parsed = parse(config, pidOrFactory);
    // return encrypt(cryptoService, pidOrFactory, instanceAlias, config,
    // parsed);
    // }
    //
    // private Dictionary encrypt(CryptoService crypto, String pidOrFactory,
    // String instanceAlias,
    // Dictionary existingConfig, JsonValue newConfig) throws WaitForMetaData {
    //
    // JsonValue parsed = newConfig;
    // Dictionary encrypted = (existingConfig == null ? new Hashtable() :
    // existingConfig); // Default
    // // to
    // // existing
    //
    // List<JsonPointer> props = getPropertiesToEncrypt(pidOrFactory,
    // instanceAlias, parsed);
    // if (logger.isTraceEnabled()) {
    // logger.trace("Properties to encrypt for {} {}: {}", new Object[] {
    // pidOrFactory,
    // instanceAlias, props });
    // }
    // if (props != null && !props.isEmpty()) {
    // for (JsonPointer pointer : props) {
    // logger.trace("Handling property to encrypt {}", pointer);
    //
    // JsonValue valueToEncrypt = parsed.get(pointer);
    // if (null != valueToEncrypt && !valueToEncrypt.isNull()
    // && !crypto.isEncrypted(valueToEncrypt)) {
    //
    // if (logger.isTraceEnabled()) {
    // logger.trace("Encrypting {} with cipher {} and alias {}", new Object[] {
    // pointer, ServerConstants.SECURITY_CRYPTOGRAPHY_DEFAULT_CIPHER, alias });
    // }
    //
    // // Encrypt and replace value
    // try {
    // JsonValue encryptedValue =
    // crypto.encrypt(valueToEncrypt,
    // ServerConstants.SECURITY_CRYPTOGRAPHY_DEFAULT_CIPHER, alias);
    // parsed.put(pointer, encryptedValue.getObject());
    // } catch (JsonCryptoException ex) {
    // throw new InternalErrorException(
    // "Failure during encryption of configuration " + pidOrFactory + "-"
    // + instanceAlias + " for property " + pointer.toString()
    // + " : " + ex.getMessage(), ex);
    // }
    // }
    // }
    // }
    // String value = null;
    // try {
    // ObjectWriter writer = prettyPrint.getWriter();
    // value = writer.writeValueAsString(parsed.asMap());
    // } catch (Exception ex) {
    // throw new InternalErrorException(
    // "Failure in writing formatted and encrypted configuration " +
    // pidOrFactory
    // + "-" + instanceAlias + " : " + ex.getMessage(), ex);
    // }
    //
    // encrypted.put(JSONConfigInstaller.JSON_CONFIG_PROPERTY, value);
    //
    // if (logger.isDebugEnabled()) {
    // logger.debug("Config with senstiive data encrypted {} {} : {}", new
    // Object[] {
    // pidOrFactory, instanceAlias, encrypted });
    // }
    //
    // return encrypted;
    // }

    /**
     * Parse the OSGi configuration in JSON format
     * 
     * @param dict
     *            the OSGi configuration
     * @param serviceName
     *            a name for the configuration getting parsed for logging
     *            purposes
     * @return The parsed JSON structure
     * @throws InvalidException
     *             if the configuration was not valid JSON and could not be
     *             parsed
     * @throws InternalErrorException
     *             if parsing failed for technical, possibly transient reasons
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

    // ----- Inner Classes

    private class ConfigurationAdminCustomizer implements
            ServiceTrackerCustomizer<ConfigurationAdmin, ConfigurationAdmin> {

        public ConfigurationAdmin addingService(ServiceReference<ConfigurationAdmin> reference) {
            final ConfigurationAdmin service = bundleContext.getService(reference);
            try {
                synchronized (ConfigurationManagerImpl.this) {
                    installDelayedConfigs(service);
                }
            } catch (Exception e) {
                logger.error("Failed processing delayed configuration", e);
            }
            try {
                Configuration installer = startPollingJSONConfigurations(service);
                if (null != installer) {
                    jsonFileInstaller = installer;
                }
            } catch (IOException e) {
                logger.error("Failed to start Json file monitoring.", e);
            }
            return service;
        }

        public void modifiedService(ServiceReference<ConfigurationAdmin> reference,
                ConfigurationAdmin service) {
            // We don't need to do anything
        }

        public void removedService(ServiceReference<ConfigurationAdmin> reference,
                ConfigurationAdmin service) {
            if (null != jsonFileInstaller) {
                try {
                    jsonFileInstaller.delete();
                } catch (IOException e) {
                    logger.debug("Failed to stop Json file monitoring.", e);
                }
            }
            bundleContext.ungetService(reference);
        }
    }

    private class CryptoServiceCustomizer implements
            ServiceTrackerCustomizer<CryptoService, CryptoService> {
        public CryptoService addingService(ServiceReference<CryptoService> reference) {
            final CryptoService service = bundleContext.getService(reference);
            try {
                synchronized (ConfigurationManagerImpl.this) {
                    encryptDelayedConfigs(service);
                    if (!delayedConfigsToInstall.isEmpty()) {
                        ConfigurationAdmin configurationAdmin = getConfigurationAdmin();
                        if (null != configurationAdmin) {
                            installDelayedConfigs(configurationAdmin);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Failed processing delayed configuration", e);
            }
            return service;
        }

        public void modifiedService(ServiceReference<CryptoService> reference, CryptoService service) {
            // We don't need to do anything
        }

        public void removedService(ServiceReference<CryptoService> reference, CryptoService service) {
            bundleContext.ungetService(reference);
        }
    }

    /**
     * Represent a configuration object before it saved to
     * {@link ConfigurationAdmin}
     */
    private static class DelayedConfig {
        final String pid;
        final String factoryPid;
        final JsonValue configuration;
        final List<JsonPointer> sensitiveAttributes;

        private DelayedConfig(String pid, String factoryPid, JsonValue configuration) {
            this.pid = pid;
            this.factoryPid = factoryPid;
            this.configuration = configuration;
            this.sensitiveAttributes = null;
        }

        /**
         * Copy constructor
         */
        private DelayedConfig(DelayedConfig delayedConfig, List<JsonPointer> sensitiveAttributes) {
            this.pid = delayedConfig.pid;
            this.factoryPid = delayedConfig.factoryPid;
            this.configuration = delayedConfig.configuration;
            List<JsonPointer> attribute =
                    null != sensitiveAttributes ? sensitiveAttributes
                            : delayedConfig.sensitiveAttributes;
            this.sensitiveAttributes =
                    null != attribute ? attribute : Collections.<JsonPointer> emptyList();
        }
    }

    /**
     * Wraps a {@link MetaDataProvider} newBuilder and handles its lifecycle
     * inside {@link ConfigurationManager}.
     */
    static class MetaDataProviderHelper {

        private final MetaDataProvider provider;

        private MetaDataProviderHelper(
                final SoftReference<ConfigurationManagerImpl> configurationManager,
                MetaDataProvider provider) {
            this.provider = provider;
            refresh(configurationManager.get());
            provider.setCallback(new MetaDataProviderCallback() {
                @Override
                public void refresh() {
                    MetaDataProviderHelper.this.refresh(configurationManager.get());
                }
            });
        }

        private void refresh(final ConfigurationManagerImpl manager) {
            if (null != manager) {
                try {
                    synchronized (manager) {
                        // Update the MetaDataProviders
                        manager.updateDelayedConfigs(MetaDataProviderHelper.this);
                        if (!manager.delayedConfigsToEncrypt.isEmpty()) {
                            CryptoService cryptoService = manager.getCryptoService();
                            if (null != cryptoService) {
                                manager.encryptDelayedConfigs(cryptoService);
                            }
                        }
                        if (!manager.delayedConfigsToInstall.isEmpty()) {
                            ConfigurationAdmin configurationAdmin = manager.getConfigurationAdmin();
                            if (null != configurationAdmin) {
                                manager.installDelayedConfigs(configurationAdmin);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed processing delayed configuration", e);
                }
                // We don't care about the result
            }
            // ConfigurationManager has been GC-ed and this callback
            // does not belong to any
        }

        public void destroy() {
            // TODO How to release?
            // provider.release();
        }

        /**
         * Get the properties to encrypt from the wrapped
         * {@code MetaDataProvider} and create a new newBuilder of
         * {@link DelayedConfig} if it can handle it with the updated
         * {@link DelayedConfig#sensitiveAttributes}
         * 
         * @param config
         * @return null if this {@code MetaDataProvider} can not handle the
         *         given {@code config}
         * @throws WaitForMetaData
         *             when this {@code MetaDataProvider} can handle this config
         *             later.
         */
        public DelayedConfig process(DelayedConfig config) throws WaitForMetaData, NotConfiguration {
            List<JsonPointer> result =
                    provider.getPropertiesToEncrypt(config.pid, config.factoryPid,
                            config.configuration);
            if (result != null && logger.isTraceEnabled()) {
                logger.trace("Properties to encrypt for {} {}: {}", new Object[] { config.pid,
                    config.factoryPid, result });
            }
            return null == result ? null : new DelayedConfig(config, result);
        }

        // ----- Override

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            MetaDataProviderHelper that = (MetaDataProviderHelper) o;

            // Compare the pointer
            return provider == that.provider;
            // Instead of the Object
            // return provider.equals(that.provider);
        }

        @Override
        public int hashCode() {
            return provider.hashCode();
        }
    }

    /**
     * A wrapper around a dictionary access it as a Map
     */
    static class DictionaryAsMap<U, V> extends AbstractMap<U, V> {

        private Dictionary<U, V> dict;

        public DictionaryAsMap(Dictionary<U, V> dict) {
            this.dict = dict;
        }

        @Override
        public Set<Entry<U, V>> entrySet() {

            return new AbstractSet<Entry<U, V>>() {

                @Override
                public Iterator<Entry<U, V>> iterator() {

                    final Enumeration<U> e = dict.keys();

                    return new Iterator<Entry<U, V>>() {

                        private U key;

                        public boolean hasNext() {
                            return e.hasMoreElements();
                        }

                        public Entry<U, V> next() {

                            key = e.nextElement();
                            return new KeyEntry(key);
                        }

                        public void remove() {

                            if (key == null) {

                                throw new IllegalStateException();
                            }
                            dict.remove(key);
                        }
                    };
                }

                @Override
                public int size() {
                    return dict.size();
                }
            };
        }

        @Override
        public V put(U key, V value) {
            return dict.put(key, value);
        }

        class KeyEntry implements Map.Entry<U,V> {

            private final U key;

            KeyEntry(U key) {
                this.key = key;
            }

            public U getKey() {
                return key;
            }

            public V getValue() {
                return dict.get(key);
            }

            public V setValue(V value) {
                return DictionaryAsMap.this.put(key, value);
            }
        }

    }
}
