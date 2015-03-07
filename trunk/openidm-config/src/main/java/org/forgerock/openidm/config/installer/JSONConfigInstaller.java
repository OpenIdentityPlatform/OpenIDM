/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.forgerock.openidm.config.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.fileinstall.internal.DirectoryWatcher;
import org.forgerock.openidm.config.crypto.ConfigCrypto;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.config.persistence.ConfigBootstrapHelper;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.metadata.WaitForMetaData;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ArtifactInstaller for JSON configurations. Based on ConfigInstaller.
 * TODO: This service lifecycle should be bound to the ConfigurationAdmin service lifecycle.
 */
public class JSONConfigInstaller implements ArtifactInstaller, ConfigurationListener {
    final static Logger logger = LoggerFactory.getLogger(JSONConfigInstaller.class);

    // The key in the OSGi configuration dictionary holding the complete JSON configuration string
    public final static String JSON_CONFIG_PROPERTY = JSONEnhancedConfig.JSON_CONFIG_PROPERTY;

    public final static String SERVICE_FACTORY_PID_ALIAS = "config.factory-pid";

    private static String fileEncoding = IdentityServer.getInstance()
            .getProperty("openidm.config.file.encoding", "UTF-8");

    private final Map<String, String> pidToFile = Collections.synchronizedMap(new HashMap<String, String>());

    private BundleContext context;
    private ConfigurationAdmin configAdmin;
    private ConfigCrypto configCrypto;

    private DelayedConfigHandler delayedConfigHandler = new DelayedConfigHandler();

    public void start(BundleContext ctx) {
        configCrypto = ConfigCrypto.getInstance(ctx, delayedConfigHandler);

        this.context = ctx;
        this.configAdmin = lookupConfigAdmin(context);
        if (this.configAdmin != null) {
            logger.debug("Starting JSON configuration listener");
        } else {
            logger.debug("ConfigAdmin is not yet available for JSON configuration listener, " 
                    + " will not handle JSON configuration files until it is available.");
        }
    }

    public void stop(BundleContext ctx) {
        this.context = null;
        this.configAdmin = null;
        logger.debug("Stopped JSON configuration listener");        
    }

    public boolean canHandle(File artifact) {
        if (this.configAdmin == null) {
            // See if the configuration admin service is available now
            this.configAdmin = lookupConfigAdmin(this.context);
            if (this.configAdmin != null) {
                logger.info("Detected ConfigAdmin service, starting JSON configuration listener");
            }
        }

        if (this.configAdmin != null) {
            logger.debug("Checking if can handle artifact: {}", artifact);
            return artifact.getName().endsWith(ConfigBootstrapHelper.JSON_CONFIG_FILE_EXT);
        } else {
            return false;
        }
    }

    public void install(File artifact) throws Exception {
        logger.debug("Artifact install {}", artifact);
        setConfig(artifact);
    }

    public void update(File artifact) throws Exception {
        logger.debug("Artifact update {}", artifact);
        setConfig(artifact);
    }

    public void uninstall(File artifact) throws Exception {
        logger.debug("Artifact uninstall {}", artifact);
        deleteConfig(artifact);
    }

    public void configurationEvent(ConfigurationEvent configurationEvent) {    
        logger.debug("ConfigurationEvent {}, pid: {}, factoryPid: {}, type: {}", 
                new Object[] {configurationEvent, configurationEvent.getPid(), 
                configurationEvent.getFactoryPid(), configurationEvent.getType()});
        // Check if writing back configurations has been disabled.
        Object obj = this.context.getProperty( DirectoryWatcher.DISABLE_CONFIG_SAVE );
        if (obj instanceof String) {
            obj = Boolean.valueOf((String) obj);
        }
        if (Boolean.FALSE.equals(obj)) {
            return;
        }

        String factoryPid = configurationEvent.getFactoryPid();
        if ("org.forgerock.openidm.router".equalsIgnoreCase(factoryPid)) {
            logger.warn("Factory router config is detected. OpenIDM prevents further processing of this config!");
            return;
        }
        String pid = configurationEvent.getPid();
        if (configurationEvent.getType() == ConfigurationEvent.CM_UPDATED) {
            try {
                Configuration config = getConfigurationAdmin().getConfiguration(pid, factoryPid);
                Dictionary dict = config.getProperties();
                if (dict == null) {
                    dict = new Properties();
                }
                String fileName = (String) dict.get( DirectoryWatcher.FILENAME );
                String confDir = ConfigBootstrapHelper.getConfigFileInstallDir();

                // Externalize OpenIDM configurations into the file "view"
                if (fileName == null && pid.startsWith(ConfigBootstrapHelper.DEFAULT_SERVICE_RDN_PREFIX)) {
                    String unqualified = pid.substring(ConfigBootstrapHelper.DEFAULT_SERVICE_RDN_PREFIX.length());
                    if (factoryPid != null) {
                        String unqualifiedFactoryPid = factoryPid;
                        if (factoryPid.startsWith(ConfigBootstrapHelper.DEFAULT_SERVICE_RDN_PREFIX)) {
                            unqualifiedFactoryPid = factoryPid.substring(ConfigBootstrapHelper.DEFAULT_SERVICE_RDN_PREFIX.length());
                        }
                        String alias = (String) dict.get(SERVICE_FACTORY_PID_ALIAS);
                        if (alias == null) {
                            logger.warn("Could not write out factory configuration file, as no friendly alias is set in the configuration."
                                    + " factory pid: {} assigned pid {}", factoryPid, pid);
                            return;
                        }
                        fileName = toConfigKey(new File(confDir, unqualifiedFactoryPid + "-" + alias + 
                                ConfigBootstrapHelper.JSON_CONFIG_FILE_EXT));
                    } else {
                        fileName = toConfigKey(new File(confDir, unqualified + ConfigBootstrapHelper.JSON_CONFIG_FILE_EXT));
                    }
                    logger.debug("Store config view filename in configuration {}", fileName);
                    dict.put(DirectoryWatcher.FILENAME, fileName);
                    config.update(dict);
                }

                File file = fileName != null ? fromConfigKey(fileName) : null;
                // IF file exists, update it, if does not exist create it
                if ( file != null) {
                    if (fileName.endsWith(ConfigBootstrapHelper.JSON_CONFIG_FILE_EXT)) {
                        synchronized(this) { // With rapid changes prevent conflicting writes to a file
                            boolean isUpToDate = false;
                            if (file.exists()) {
                                Hashtable existingCfg = loadConfigFile(file);
                                isUpToDate = isConfigSame(dict, existingCfg);
                            }
                            if (isUpToDate) {
                                logger.debug("Config file is up-to-date: {}", fileName);
                            } else {
                                logger.info("Updating configuration file: {}", fileName);
                                // Note: currently only stores JSON config property, not other properties in Dictionary.
                                String jsonConfig = "";
                                Object entry = dict.get(JSON_CONFIG_PROPERTY);
                                if (entry != null) {
                                    jsonConfig = entry.toString(); 
                                }
                                Writer writer = new OutputStreamWriter(new FileOutputStream(file), fileEncoding);
                                try {
                                    writer.write(jsonConfig);
                                } finally {
                                    writer.close();
                                }
                                logger.debug("Completed update of configuration file {}", fileName);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.info("Unable to save configuration", e);
            }
        } else if (configurationEvent.getType() == ConfigurationEvent.CM_DELETED) {
            String fileName = pidToFile.get(pid);
            if (fileName != null) {
                File fileToDel = fromConfigKey(fileName);
                synchronized(this) {
                    logger.trace("Try to delete {} exists: {}", fileToDel, fileToDel.exists());
                    boolean deleted = fileToDel.delete();
                    if (deleted) {
                        logger.debug("Deleted configuration file from view {}", fileName);
                    } else {
                        logger.info("No configuration deleted from view corresponding to {} {}", pid, fileName);
                    }
                    pidToFile.remove(pid);
                }
            }
        }
    }

    public static ConfigurationAdmin lookupConfigAdmin(BundleContext context) {
        ConfigurationAdmin confAdmin = null;
        if (context != null) {
            ServiceReference configurationAdminReference = 
                    context.getServiceReference(ConfigurationAdmin.class.getName()); 
            if (configurationAdminReference != null) {
                confAdmin = (org.osgi.service.cm.ConfigurationAdmin) context.getService(configurationAdminReference);
            }
        }
        return confAdmin;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        // TOOD: better guarding against this service not (yet) being there.
        if (configAdmin == null) {
            this.configAdmin = lookupConfigAdmin(context);
            if (this.configAdmin != null) {
                logger.info("ConfigAdmin service detected by JSON configuration listener");
            } else {
                logger.warn("JSON Configuration listener could not find ConfigAdmin service");
            }
        }

        return configAdmin;
    }

    /**
     * Load the specified configuration file as hashtable. 
     * 
     * May also be called by other clients that want direct access to a given configuration file
     * without going through the configuration admin service.
     * 
     * @param f
     * @return
     * @throws java.io.IOException
     */
    @SuppressWarnings("unchecked")
    public static Hashtable loadConfigFile(final File f) throws IOException {
        logger.debug("Loading configuration from {}", f);
        final Hashtable ht = new Hashtable();

        if (f.getName().endsWith(ConfigBootstrapHelper.JSON_CONFIG_FILE_EXT)) {

            StringBuilder fileBuf = new StringBuilder(1024);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(f), fileEncoding));
            try {
                char[] buf = new char[1024];
                int numRead = 0;
                while((numRead = reader.read(buf)) != -1){
                    fileBuf.append(buf, 0, numRead);
                }
            } finally {
                reader.close();
            }

            ht.put(JSON_CONFIG_PROPERTY, fileBuf.toString());
        }

        return ht;
    }

    /**
     * Set the configuration based on the config file.
     *
     * @param f
     *            Configuration file
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    synchronized boolean setConfig(final File f) throws Exception {
        boolean updated = false;
        try {
            Dictionary ht = loadConfigFile(f);
            String pid[] = parsePid(f.getName());
            updated = setConfig(ht, pid, f);
        } catch (Exception ex) {
            logger.warn("Loading configuration file {} failed ", f, ex);
        }
        return updated;
    }

    synchronized boolean setConfig(Dictionary ht, final String[] pid, final File f) throws Exception {
        boolean updated = false;
        Configuration config = getConfiguration(toConfigKey(f), pid[0], pid[1], true);

        Dictionary props = config.getProperties();
        if (!isConfigSame(ht, props)) {
            try {
                ht = configCrypto.encrypt(pid[0], pid[1], ht);
                ht.put(DirectoryWatcher.FILENAME, toConfigKey(f));
                if (pid != null && pid[1] != null) {
                    ht.put(SERVICE_FACTORY_PID_ALIAS, pid[1]);
                }
                if (config.getBundleLocation() != null) {
                    config.setBundleLocation(null);
                }
                if (pid[1] == null) {
                    logger.info("Loaded changed configuration for {} from {}", pid[0], f.getName());
                } else {
                    logger.info("Loaded changed configuration for {} {} from {}", 
                            new Object[] {pid[0], pid[1], f.getName()});
                }
                config.update(ht);
            } catch (WaitForMetaData ex) {
                logger.debug("Wait for meta data for config {}-{}", pid[0], pid[1]);
                DelayedConfig delayed = new DelayedConfig();
                delayed.pidOrFactory = pid[0];
                delayed.factoryAlias = pid[1];
                delayed.file = f;
                delayed.oldConfig = props;
                delayed.newConfig = ht;
                delayed.parsedConfig = configCrypto.parse(ht, pid[0] + "-" + pid[1]);
                delayed.configInstaller = this;
                delayed.configCrypto = configCrypto;
                delayedConfigHandler.addConfig(delayed);
            }
            updated = true;
        } else {
            logger.debug("File contents of configuration for {} from {} has not changed.", pid[1], f);
            updated = false;
        }
        return updated;
    }

    /**
     * Remove the configuration.
     *
     * @param f
     *            File where the configuration in whas defined.
     * @return
     * @throws Exception
     */
    boolean deleteConfig(File f) throws Exception {
        String pid[] = parsePid(f.getName());
        Configuration config = getConfiguration(toConfigKey(f), pid[0], pid[1], false);
        config.delete();
        return true;
    }

    String toConfigKey(File f) {
        return f.getAbsoluteFile().toURI().toString();
    }

    File fromConfigKey(String key) {
        return new File(URI.create(key));
    }

    String[] parsePid(String path) {
        String pid = path.substring(0, path.lastIndexOf('.'));
        int n = pid.indexOf('-');
        if (n > 0) {
            String factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
            pid = ConfigBootstrapHelper.qualifyPid(pid);
            logger.info("Configuring service PID {} factory PID {}", pid, factoryPid);
            return new String[] { pid, factoryPid };
        }
        else {
            pid = ConfigBootstrapHelper.qualifyPid(pid);
            return new String[] { pid, null };
        }
    }

    /**
     * Whether the JSON configuration is the same (Including formatting)
     * Ignores meta-data such as whether factory pid has been assigned yet
     * 
     * @param newCfg
     * @param oldCfg
     * @return true if the JSON config is the same
     */
    boolean isConfigSame(Dictionary newCfg, Dictionary oldCfg) {
        if (newCfg == null || oldCfg == null) {
            return oldCfg == newCfg;
        }
        Dictionary newCompare = new Hashtable(new DictionaryAsMap(newCfg));
        newCompare.remove( DirectoryWatcher.FILENAME );
        newCompare.remove( Constants.SERVICE_PID );
        newCompare.remove( ConfigurationAdmin.SERVICE_FACTORYPID );
        newCompare.remove( SERVICE_FACTORY_PID_ALIAS );

        Dictionary oldCompare = new Hashtable(new DictionaryAsMap(oldCfg));
        oldCompare.remove( DirectoryWatcher.FILENAME );
        oldCompare.remove( Constants.SERVICE_PID );
        oldCompare.remove( ConfigurationAdmin.SERVICE_FACTORYPID );
        oldCompare.remove( SERVICE_FACTORY_PID_ALIAS );

        return newCompare.equals(oldCompare);
    }

    Configuration getConfiguration(String fileName, String pid, String factoryPid, boolean addIfNew) throws Exception {

        Configuration oldConfiguration = findExistingConfiguration(fileName, pid, factoryPid);
        if (oldConfiguration != null) {
            logger.debug("Updating configuration from {}", fileName);
            return oldConfiguration;
        }
        else {
            Configuration newConfiguration;
            if (factoryPid != null) {

                if ("org.forgerock.openidm.router".equalsIgnoreCase(pid)) {
                    throw new ConfigurationException(factoryPid, "router config can not be factory config");
                }
                newConfiguration = getConfigurationAdmin().createFactoryConfiguration(pid, null);
            }
            else {
                newConfiguration = getConfigurationAdmin().getConfiguration(pid, null);
            }
            if (addIfNew) {
                pidToFile.put(newConfiguration.getPid(), fileName);
            }
            return newConfiguration;
        }
    }

    Configuration findExistingConfiguration(String fileName, String pid, String factoryPid) throws Exception {

        String filter = null;
        if (null == factoryPid) {
            filter = "(" + Constants.SERVICE_PID + "=" + pid + ")";
        }  else {
            filter = "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + pid + ")(config.factory-pid=" + factoryPid + "))";
        }

        Configuration[] configurations = getConfigurationAdmin().listConfigurations(filter);
        if (configurations != null && configurations.length > 0) {
            pidToFile.put(configurations[0].getPid(), fileName);
            return configurations[0];
        }
        else {
            return null;
        }
    }
}

/**
 * A wrapper around a dictionary access it as a Map
 */
class DictionaryAsMap<U, V> extends AbstractMap<U, V> {

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
