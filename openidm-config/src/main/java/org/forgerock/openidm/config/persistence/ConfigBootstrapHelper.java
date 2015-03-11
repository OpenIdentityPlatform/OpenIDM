/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.config.persistence;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.config.installer.JSONConfigInstaller;
import org.forgerock.openidm.core.IdentityServer;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for bootstrapping the configuration, and in turn the system
 * 
 * The boostrapping mechanism works in the following order:
 * 1. Repository bundle activators register a bootstrap repository that knows how to access configuration.
 *    The basic info to bootstrap comes from system properties or configuration files
 * 2. A repo persistence plug-in for the configuration admin is registered for configuration to get loaded/stored in the repository
 * 3. When the OSGi configuration administration service comes up, proceed with handling configuration in files (if enabled)
 *    via the felix file install mechanism
 */
public class ConfigBootstrapHelper {

    // Properties to set bootstrapping behavior
    public static final String PREFIX_OPENIDM_REPO = "openidm.repo.";
    public static final String OPENIDM_REPO_TYPE = "openidm.repo.type";
    
    // Properties to set configuration file handling behavior
    public static final String OPENIDM_FILEINSTALL_BUNDLES_NEW_START = "openidm.fileinstall.bundles.new.start";
    public static final String OPENIDM_FILEINSTALL_FILTER = "openidm.fileinstall.filter";
    public static final String OPENIDM_FILEINSTALL_DIR = "openidm.fileinstall.dir";
    public static final String OPENIDM_FILEINSTALL_POLL = "openidm.fileinstall.poll";
    public static final String OPENIDM_FILEINSTALL_ENABLED = "openidm.fileinstall.enabled";
    
    public static final String OPENIDM_UI_FILEINSTALL_ENABLED = "openidm.ui.fileinstall.enabled";
    public static final String OPENIDM_UI_FILEINSTALL_DIR = "openidm.ui.fileinstall.dir";
    public static final String OPENIDM_UI_FILEINSTALL_POLL = "openidm.ui.fileinstall.poll";

    public static final String FELIX_FILEINSTALL_PID = "org.apache.felix.fileinstall";
    
    public static final String CONFIG_ALIAS = "config__factory-pid";
    public static final String SERVICE_PID = "service__pid";
    public static final String SERVICE_FACTORY_PID = "service__factoryPid";

    // Filename prefix for repository configuration
    public static final String REPO_FILE_PREFIX = "repo.";
    public static final String JSON_CONFIG_FILE_EXT = ".json";
    
    // Default prefix for OpenIDM OSGi services
    public static final String DEFAULT_SERVICE_RDN_PREFIX = "org.forgerock.openidm.";
    
    final static Logger logger = LoggerFactory.getLogger(ConfigBootstrapHelper.class);

    static boolean warnMissingConfig = true;
    
    /**
     * Get the configured bootstrap information for a repository
     * 
     * Currently only one bootstrap repository is selected.
     * 
     * Bootstrap information in system properties takes precedence over configuration files
     * 
     * Configuration keys returned are lower case, whether they originate from system properties or
     * configuration files.
     * 
     * @param repoType the type of the bootstrap repository, 
     * equivalent to the last part of its PID 
     * @return The relevant bootstrap configuration if this repository should be bootstraped, null if not
     */
    public static JsonValue getRepoBootConfig(String repoType, BundleContext bundleContext) {
        JsonValue result = new JsonValue(new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER));
        result.put(OPENIDM_REPO_TYPE, repoType);
        
        // System properties take precedence over configuration files
        String sysPropType = System.getProperty(OPENIDM_REPO_TYPE);
        if (sysPropType != null && sysPropType.toLowerCase().equals(repoType)) {
            for (Object entry : System.getProperties().keySet()) {
                String key = (String) entry;
                if (key.startsWith(PREFIX_OPENIDM_REPO)) {
                    result.put(key.substring(PREFIX_OPENIDM_REPO.length()).toLowerCase(), System.getProperty(key));
                }
            }
            logger.info("Bootstrapping with settings from system properties {}", result);
            return result;
        }

        // If bootstrap info not found in system properties, check for configuration files
        String confDir = getConfigFileInstallDir();
        File unqualified = new File(confDir, REPO_FILE_PREFIX + repoType.toLowerCase() + JSON_CONFIG_FILE_EXT);
        File qualified = new File(confDir, ConfigBootstrapHelper.DEFAULT_SERVICE_RDN_PREFIX + REPO_FILE_PREFIX 
                + repoType.toLowerCase() + JSON_CONFIG_FILE_EXT);
        
        File loadedFile = null;
        try {
            Dictionary rawConfig = null;
            if (unqualified.exists()) {
                rawConfig = JSONConfigInstaller.loadConfigFile(unqualified);
                loadedFile = unqualified;
            } else if (qualified.exists()) {
                rawConfig = JSONConfigInstaller.loadConfigFile(qualified);
                loadedFile = qualified;
            } else {
                logger.debug("No configuration to bootstrap {}", repoType);
                
                // Check at least one configuration exists
                String[] repoConfigFiles = getRepoConfigFiles(confDir);
                if (warnMissingConfig && (repoConfigFiles == null || repoConfigFiles.length == 0)) {
                    logger.error("No configuration to bootstrap the repository found.");
                    warnMissingConfig = false;
                }
                
                return null;
            }
            JsonValue jsonCfg = new JSONEnhancedConfig().getConfiguration(rawConfig, bundleContext, repoType);
            Map<String, Object> cfg = jsonCfg.asMap();
            for (Entry<String, Object> entry : cfg.entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
        } catch (Exception ex) {
            logger.warn("Failed to load configuration file to bootstrap repository " + ex.getMessage(), ex);
            throw new RuntimeException("Failed to load configuration file to bootstrap repository " 
                    + ex.getMessage(), ex);
        }
        logger.info("Bootstrapping with settings from configuration file {}", loadedFile);
        
        return result;
    }
    
    // A list of repository configuration files 
    static String[] getRepoConfigFiles(final String confDir) {
        FilenameFilter repoConfFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(REPO_FILE_PREFIX);
            }
        };

        return IdentityServer.getFileForProjectPath(confDir).list(repoConfFilter);
    }
    
    /**
     * Get the directory for configuration file view
     * 
     * @return config dir
     */
    public static String getConfigFileInstallDir() {
        // Default the configuration directory if not declared
        String dir = System.getProperty(OPENIDM_FILEINSTALL_DIR, "conf");
        dir =  IdentityServer.getFileForProjectPath(dir).getAbsolutePath();
        logger.debug("Configuration file directory {}", dir);
        return dir;
    }
    
    /**
     * Configure to process all JSON configuration files (if enabled)
     * 
     * @param configAdmin the OSGi configuration admin service
     * @throws java.io.IOException
     */
    public static void installAllConfig(ConfigurationAdmin configAdmin) throws IOException {
        IdentityServer identityServer = IdentityServer.getInstance();
        
        String enabled = System.getProperty(OPENIDM_FILEINSTALL_ENABLED, "true");
        String poll = System.getProperty(OPENIDM_FILEINSTALL_POLL, "2000");
        String dir = getConfigFileInstallDir();
        String filter = System.getProperty(OPENIDM_FILEINSTALL_FILTER, ".*\\.cfg|.*\\.json");
        String start = System.getProperty(OPENIDM_FILEINSTALL_BUNDLES_NEW_START, "false");

        String uiConfigEnabled = identityServer.getProperty(OPENIDM_UI_FILEINSTALL_ENABLED, "true");
        String uiPoll = identityServer.getProperty(OPENIDM_UI_FILEINSTALL_POLL, "2000");
        String uiDir = identityServer.getProperty(OPENIDM_UI_FILEINSTALL_DIR, "ui/default");

        Configuration config = configAdmin.createFactoryConfiguration(FELIX_FILEINSTALL_PID, null);
        
        Configuration uiConfig = configAdmin.createFactoryConfiguration(FELIX_FILEINSTALL_PID, null);
    
        Dictionary props = config.getProperties();
        if (props == null) {
            props = new Hashtable();
        }
        
        Dictionary uiProps = uiConfig.getProperties();
        if (uiProps == null) {
            uiProps = new Hashtable();
        }
        
        if ("true".equals(enabled)) {
            // Apply the latest configuration changes from file
            props.put("felix.fileinstall.poll", poll);
            props.put("felix.fileinstall.noInitialDelay", "true");
            props.put("felix.fileinstall.dir", dir);
            props.put("felix.fileinstall.filter", filter);
            props.put("felix.fileinstall.bundles.new.start", start);
            props.put("config.factory-pid","openidm");
            config.update(props);
            logger.info("Configuration from file enabled");
        } else {
            logger.info("Configuration from file disabled");
        }
        
        if ("true".equals(uiConfigEnabled)) {
            uiProps.put("felix.fileinstall.poll", uiPoll);
            uiProps.put("felix.fileinstall.dir", uiDir);
            uiProps.put("config.factory-pid", "ui");
            uiConfig.update(uiProps);
            logger.info("UI file installer enabled");
        } else {
            logger.info("UI file installer disabled");
        }
        
    }
    
    /**
     * Prefixes unqualified PIDs with the default RDN qualifier
     * I.e. file names can be unqualified and will be prefixed
     * with the default. 
     * Configuring services with PIDs that are not qualified 
     * by org. or com. is currently not supported.
     * @param fileNamePid
     * @return
     */
    public static String qualifyPid(String fileNamePid) {
        String qualifiedPid = fileNamePid;
        // Prefix unqualified pid names with the default.
        if (fileNamePid != null && !(fileNamePid.startsWith("org.") || fileNamePid.startsWith("com."))) {
            qualifiedPid = DEFAULT_SERVICE_RDN_PREFIX + fileNamePid;
        }
        return qualifiedPid;
    }
    
    /**
     * Removes the default RDN prefix if this is a qualified name 
     * int the default namespace. IF not, it STAYS qualified.
     * 
     * Configuring services with PIDs that are not qualified 
     * by org. or com. is currently not supported.
     * @param qualifiedPid
     * @return
     */
    public static String unqualifyPid(String qualifiedPid) {
        if (qualifiedPid != null && qualifiedPid.startsWith(DEFAULT_SERVICE_RDN_PREFIX)) {
            return qualifiedPid.substring(DEFAULT_SERVICE_RDN_PREFIX.length());
        } else {
            return qualifiedPid;
        }
    }
    


    /**
     * Construct the configurations's resource ID.
     * 
     * @param alias the config factory pid alias
     * @param pid the service pid
     * @param factoryPid the service factory pid
     * @return the configuration's resource ID
     */
    public static String getId(String alias, String pid, String factoryPid) {
        String unqualifiedPid = ConfigBootstrapHelper.unqualifyPid(pid);
        String unqualifiedFactoryPid = ConfigBootstrapHelper.unqualifyPid(factoryPid);
        // If there is an alias for factory config is available, make a nicer ID then the internal PID
        return unqualifiedFactoryPid != null && alias != null
                ? unqualifiedFactoryPid + "/" + alias
                : unqualifiedPid;
    }
    
}
