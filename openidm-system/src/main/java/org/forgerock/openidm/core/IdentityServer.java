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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

/**
 * This class defines the core of the Identity Server.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class IdentityServer {

//    final static Logger logger = LoggerFactory.getLogger(IdentityServer.class);
    
    /**
     * The singleton Identity Server instance.
     */
    private static final IdentityServer identityServer = new IdentityServer();

    // The set of properties for the environment config. 
    // Keys are lower case for easier case insensitive searching
    // Precedences is 1. Explicit config properties, 2. Boot file properties, 3. System properties
    private final Map<String, String> configProperties;
    private final Map<String, String> bootFileProperties;

    /**
     * Creates a new identity environment configuration initialized
     * from the system properties defined in the JVM.
     */
    private IdentityServer() {
        this(null);
    }

    /**
     * Creates a new identity environment configuration initialized
     * with a copy of the provided set of properties.
     *
     * @param properties The properties to use when initializing this
     *                   environment configuration, or {@code null}
     *                   to use an empty set of properties.
     */
    private IdentityServer(Map<String, String> properties) {
        configProperties = new HashMap<String, String>();
        if (properties != null) {
            // Populate with lower case keys for easier case insensitive comparisons
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                configProperties.put(entry.getKey().toLowerCase(), entry.getValue());
            }
        }
        String bootFileName = getProperty(ServerConstants.PROPERTY_BOOT_FILE_LOCATION, 
                ServerConstants.DEFAULT_BOOT_FILE_LOCATION);
        bootFileProperties = loadProps(bootFileName);
    }
    
    public static IdentityServer getInstance() {
        return identityServer;
    }
    
    public static IdentityServer getInstance(Map<String, String> properties) {
        return new IdentityServer(properties);
    }
    
    /**
     * Retrieves the property with the specified name (case insensitvie).  
     * The check will first be made in the local config properties, 
     * the boot properties file, but if no value is
     * found then the JVM system properties will be checked.
     *
     * @param name The name of the property to retrieve.
     * @param defaultValue the default value to return if the property is not set
     * @return The property with the specified name, or {@code defaultValue} if
     *         no such property is defined.
     */
    public String getProperty(String name, String defaultValue) {
        String lowerCaseName = name.toLowerCase();
        if (configProperties != null && configProperties.containsKey(lowerCaseName)) {
            //logger.trace("Property " + name + " resolved from programmatic config properties: " + configProperties.get(lowerCaseName));
            return configProperties.get(lowerCaseName);
        } else if (bootFileProperties != null && bootFileProperties.containsKey(lowerCaseName)) {
            //logger.trace("Property " + name + " resolved from boot properties file: " + bootFileProperties.get(lowerCaseName));
            return bootFileProperties.get(lowerCaseName);
        } else {
            String value = getSystemPropertyIgnoreCase(lowerCaseName);
            if (value == null) {
                //logger.trace("Property " + name + " no setting found, defaulting to: " + defaultValue);
                return defaultValue;
            } else {
                //logger.trace("Property " + name + " resolved from system properties: " + value);
                return value;
            }
        }
    }
    
    /**
     * Retrieves the property with the specified name (case insensitvie).  
     * The check will first be made in the local config properties, 
     * the boot properties file, but if no value is
     * found then the JVM system properties will be checked.
     *
     * @param name The name of the property to retrieve.
     * @param defaultValue the default value to return if the property is not set
     * @return The property with the specified name, or {@code null} if
     *         no such property is defined.
     */
    public String getProperty(String name) {
        return getProperty(name, null);
    }

    // Case insensitive retrieval of system properties
    private String getSystemPropertyIgnoreCase(String name) {
        Properties allProps = System.getProperties();
        for (Object key : allProps.keySet()) {
            if (((String)key).equalsIgnoreCase(name)) {
                return (String) allProps.get(key);
            }
        }
        return null;
    }
    
    /**
     * Retrieves the path to the root directory for this instance of the Identity
     * Server.
     *
     * @return The path to the root directory for this instance of the Identity
     *         Server.
     */
    public String getServerRoot() {
        String root = getProperty(ServerConstants.PROPERTY_SERVER_ROOT);

        if (null != root) {
                File r = new File(root);
                if (r.isAbsolute()) {
                    return root;
                } else {
                    return r.getAbsolutePath();
                }
        }
        // We don't know where the server root is, so we'll have to assume it's
        // the current working directory.
        root = System.getProperty("user.dir");
        return root;
    }

    /**
     * Retrieves the path to the root directory for this instance of the Identity
     * Server.
     *
     * @return The path to the root directory for this instance of the Identity
     *         Server.
     */
    public URI getServerRootURI() {
        return URI.create(getServerRoot());
    }

    /**
     * Retrieves a <CODE>File</CODE> object corresponding to the specified path.
     * If the given path is an absolute path, then it will be used.  If the path
     * is relative, then it will be interpreted as if it were relative to the
     * Identity Server root.
     *
     * @param path The path string to be retrieved as a <CODE>File</CODE>
     * @return A <CODE>File</CODE> object that corresponds to the specified path.
     */
    public static File getFileForPath(String path) {
        return getFileForPath(path, identityServer.getServerRoot());
    }
    
    /**
     * Retrieves a <CODE>File</CODE> object corresponding to the specified path.
     * If the given path is an absolute path, then it will be used.  If the path
     * is relative, then it will be interpreted as if it were relative to the
     * Identity Server root.
     *
     * @param path The path string to be retrieved as a <CODE>File</CODE>
     * @param serverRoot the server root to resolve against
     * @return A <CODE>File</CODE> object that corresponds to the specified path.
     */
    public static File getFileForPath(String path, String serverRoot) {
        File f = new File(path);

        if (f.isAbsolute()) {
            //logger.trace("getFileForPath is absolute: {}", path);
            return f;
        } else {
            //logger.trace("getFileForPath is relative: {} resolving against {}", path, serverRoot);
            return new File(serverRoot, path).getAbsoluteFile();
        }
    }

    /**
     * Retrieves the current running mode of Identity Server.
     * <p/>
     * Default running mode is the {@code Production}, that prohibit access to some
     * insecure method.
     * Development mode allow access to all method. To enable development mode set the
     * {@link ServerConstants#PROPERTY_DEBUG_ENABLE} system property {@code true}.
     *
     * @return true if {@code Development} mode is on.
     */
    public static boolean isDevelopmentProfileEnabled() {
        String debug = identityServer.getProperty(ServerConstants.PROPERTY_DEBUG_ENABLE);
        return (null != debug) && Boolean.valueOf(debug);
    }
    
    /**
     * Loads boot properties file
     * @return properties in boot properties file, keys in lower case
     */
    private Map<String, String> loadProps(String bootFileLocation) { 
        File bootFile = IdentityServer.getFileForPath(bootFileLocation, getServerRoot());
        Map<String, String> entries = new HashMap<String, String>();
        
        if (!bootFile.exists()) {
// TODO: move this class out of system bundle so we can use logging
            //logger.info("No boot properties file detected at {}.", bootFile.getAbsolutePath());
            System.out.println("No boot properties file detected at " + bootFile.getAbsolutePath());
        } else {
            //logger.info("Using boot properties at {}.", bootFile.getAbsolutePath());
            System.out.println("Using boot properties at " + bootFile.getAbsolutePath());
            InputStream in = null;
            try {
                Properties prop = new Properties();
                in = new BufferedInputStream(new FileInputStream(bootFile));
                prop.load(in);
                for (Map.Entry<Object, Object> entry :  prop.entrySet()) {
                    entries.put(((String) entry.getKey()).toLowerCase(), (String) entry.getValue());
                }
            } catch (FileNotFoundException ex) {
            //    logger.info("Boot properties file {} not found", bootFile.getAbsolutePath(), ex);
            } catch (IOException ex) {
            //    logger.warn("Failed to load boot properties file {}", bootFile.getAbsolutePath(), ex);
                throw new RuntimeException("Failed to load boot properties file " 
                        + bootFile.getAbsolutePath() + " " + ex.getMessage(), ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
            //            logger.warn("Failure in closing boot properties file", ex);
                    }
                }
            }
        }

        return entries;
    }
}
