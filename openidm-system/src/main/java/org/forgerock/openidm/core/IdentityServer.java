/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class defines the core of the Identity Server.
 *
 * @version $Revision$ $Date$
 */
public final class IdentityServer implements PropertyAccessor {

    /**
     * The singleton Identity Server instance.
     */
    private static final AtomicReference<IdentityServer> IDENTITY_SERVER =
            new AtomicReference<IdentityServer>(new IdentityServer(null, null));

    private static final AtomicBoolean INITIALISED = new AtomicBoolean(Boolean.FALSE);

    // The set of properties for the environment config.
    // Keys are lower case for easier case insensitive searching
    // Precedences is 1. Boot file properties, 2. Explicit config properties, 3.
    // System properties
    private final PropertyAccessor configProperties;
    private final Map<String, String> bootFileProperties;
    private File bootPropertyFile = null;

    /**
     * Creates a new identity environment configuration initialized with a copy
     * of the provided set of properties.
     *
     * @param properties
     *            The properties to use when initializing this environment
     *            configuration, or {@code null} to use an empty set of
     *            properties.
     */
    private IdentityServer(PropertyAccessor properties, IdentityServer identityServer) {
        if (null == properties) {
            configProperties = new SystemPropertyAccessor(null);
        } else {
            configProperties = properties;
        }
        String bootFileName =
                getProperty(ServerConstants.PROPERTY_BOOT_FILE_LOCATION,
                        ServerConstants.DEFAULT_BOOT_FILE_LOCATION);
        bootFileProperties = loadProps(bootFileName, identityServer);
    }

    public static IdentityServer getInstance() {
        IdentityServer server = IDENTITY_SERVER.get();
        if (null == server) {
            throw new IllegalStateException("IdentityServer has not been initialised");
        }
        return server;
    }

    /**
     * Initialise the singleton {@link IdentityServer} instance with the
     * provided {@link PropertyAccessor} instance.
     * <p/>
     * This or the {@link #initInstance(IdentityServer)} method can be called
     * only once and then it throws {@link IllegalStateException} if it's called
     * more then once.
     *
     * @param properties
     *            the parent {@code PropertyAccessor}
     * @return new instance of {@link IdentityServer}
     * @throws IllegalStateException
     *             when this method called more then once.
     */
    public static IdentityServer initInstance(PropertyAccessor properties) {
        if (INITIALISED.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            return IDENTITY_SERVER
                    .getAndSet(properties instanceof IdentityServer ? (IdentityServer) properties
                            : new IdentityServer(properties, IDENTITY_SERVER.get()));
        } else {
            throw new IllegalStateException("IdentityServer has been initialised already");
        }
    }

    /**
     * Initialise the singleton {@link IdentityServer} instance with the
     * provided {@link IdentityServer} instance.
     * <p/>
     * This or the {@link #initInstance(PropertyAccessor)} method can be called
     * only once and then it throws {@link IllegalStateException} if it's called
     * more then once.
     *
     * @param server
     *            new instance of {@link IdentityServer}
     * @return same instance as the {@code server} parameter if not {@code null}
     *         or the current {@link IdentityServer instance}
     * @throws IllegalStateException
     *             when this method called more then once.
     */
    public static IdentityServer initInstance(IdentityServer server) {
        if (null != server) {
            if (INITIALISED.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
                return IDENTITY_SERVER.getAndSet(server);
            } else {
                throw new IllegalStateException("IdentityServer has been initialised already");
            }
        }
        return IDENTITY_SERVER.get();
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue, Class<T> expected) {
        T value = null;
        if (null != bootFileProperties
                && null != key
                && ((null != expected && expected.isAssignableFrom(String.class)) || defaultValue instanceof String)) {
            value = (T) bootFileProperties.get(key);
        }
        return null != value ? value : (null != configProperties) ? configProperties.getProperty(
                key, defaultValue, expected) : null;

    }

    /**
     * Retrieves the property with the specified name (case insensitvie). The
     * check will first be made in the boot properties file, the local config
     * properties, but if no value is found then the JVM system properties will
     * be checked.
     *
     * @param name
     *            The name of the property to retrieve.
     * @param defaultValue
     *            the default value to return if the property is not set
     * @param withPropertySubstitution
     *            whether to use boot and system property substitution
     * @return The property with the specified name, or {@code defaultValue} if
     *         no such property is defined.
     */
    public String getProperty(String name, String defaultValue, boolean withPropertySubstitution) {
        String result = getProperty(name, defaultValue, String.class);
        if (withPropertySubstitution) {
            result = (String) PropertyUtil.substVars(result, IDENTITY_SERVER.get(), false);
        }
        return result;
    }

    /**
     * Retrieves the property with the specified name (case insensitvie). The
     * check will first be made in the boot properties file, the local config
     * properties, but if no value is found then the JVM system properties will
     * be checked.
     *
     * @param name
     *            The name of the property to retrieve.
     * @param defaultValue
     *            the default value to return if the property is not set
     * @return The property with the specified name, or {@code defaultValue} if
     *         no such property is defined.
     */
    public String getProperty(String name, String defaultValue) {
        return getProperty(name, defaultValue, String.class);
    }

    /**
     * Retrieves the property with the specified name (case insensitvie). The
     * check will first be made in the local config properties, the boot
     * properties file, but if no value is found then the JVM system properties
     * will be checked.
     *
     * @param name
     *            The name of the property to retrieve.
     * @return The property with the specified name, or {@code null} if no such
     *         property is defined.
     */
    public String getProperty(String name) {
        return getProperty(name, null, String.class);
    }

    // Case insensitive retrieval of system properties
    private String getSystemPropertyIgnoreCase(String name) {
        Properties allProps = System.getProperties();
        for (Object key : allProps.keySet()) {
            if (((String) key).equalsIgnoreCase(name)) {
                return (String) allProps.get(key);
            }
        }
        return null;
    }

    /**
     * Retrieves the path to the root directory for this instance of the
     * Identity Server.
     *
     * @return The path to the root directory for this instance of the Identity
     *         Server.
     */
    public String getServerRoot() {
        String root = getProperty(ServerConstants.LAUNCHER_PROJECT_LOCATION, null, String.class);
        // Keep the backward compatibility
        if (null == root) {
            root = getProperty(ServerConstants.PROPERTY_SERVER_ROOT, null, String.class);
        }

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
     * Retrieves the path to the root directory for this instance of the
     * Identity Server.
     *
     * @return The path to the root directory for this instance of the Identity
     *         Server.
     */
    public URI getServerRootURI() {
        return URI.create(getServerRoot());
    }

    public String getClusterName() {
        return getProperty("openidm.system.site.name", "default");
    }

    public String getNodeName() {
        try {
            return getProperty("openidm.system.node.name", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            return getProperty("openidm.system.node.name", "default");
        }
    }

    /**
     * Retrieves a <CODE>File</CODE> object corresponding to the specified path.
     * If the given path is an absolute path, then it will be used. If the path
     * is relative, then it will be interpreted as if it were relative to the
     * Identity Server root.
     *
     * @param path
     *            The path string to be retrieved as a <CODE>File</CODE>
     * @return A <CODE>File</CODE> object that corresponds to the specified
     *         path.
     */
    public static File getFileForPath(String path) {
        return getFileForPath(path, IDENTITY_SERVER.get().getServerRoot());
    }

    /**
     * Retrieves a {@code File} object corresponding to the <b>Install</b> path.
     * If the given path is an absolute path, then it will be used. If the path
     * is relative, then it will be interpreted as if it were relative to the
     * Install location.
     *
     * @param path
     *            The path string to be retrieved as a {@code File}
     * @return A {@code File} object that corresponds to the specified path.
     */
    public static File getFileForInstallPath(String path) {
        return getFileForPath(path, IDENTITY_SERVER.get().getInstallLocation());
    }

    /**
     * Retrieves a {@code File} object corresponding to the <b>Project</b> path.
     * If the given path is an absolute path, then it will be used. If the path
     * is relative, then it will be interpreted as if it were relative to the
     * Project location.
     *
     * @param path
     *            The path string to be retrieved as a {@code File}
     * @return A {@code File} object that corresponds to the specified path.
     */
    public static File getFileForProjectPath(String path) {
        return getFileForPath(path, IDENTITY_SERVER.get().getProjectLocation());
    }

    /**
     * Retrieves a {@code File} object corresponding to the <b>Working</b> path.
     * If the given path is an absolute path, then it will be used. If the path
     * is relative, then it will be interpreted as if it were relative to the
     * Working location.
     *
     * @param path
     *            The path string to be retrieved as a {@code File}
     * @return A {@code File} object that corresponds to the specified path.
     */
    public static File getFileForWorkingPath(String path) {
        return getFileForPath(path, IDENTITY_SERVER.get().getWorkingLocation());
    }

    /**
     * Retrieves a <CODE>File</CODE> object corresponding to the specified path.
     * If the given path is an absolute path, then it will be used. If the path
     * is relative, then it will be interpreted as if it were relative to the
     * Identity Server root.
     *
     * @param path
     *            The path string to be retrieved as a <CODE>File</CODE>
     * @param serverRoot
     *            the server root to resolve against
     * @return A <CODE>File</CODE> object that corresponds to the specified
     *         path.
     */
    public static File getFileForPath(String path, String serverRoot) {
        if (null == path) {
            return null;
        }
        File f = new File(path);

        if (f.isAbsolute()) {
            // logger.trace("getFileForPath is absolute: {}", path);
            return f;
        } else {
            // logger.trace("getFileForPath is relative: {} resolving against {}",
            // path, serverRoot);
            return new File(serverRoot, path).getAbsoluteFile();
        }
    }

    /**
     * Retrieves a <CODE>File</CODE> object corresponding to the specified path.
     * If the given path is an absolute path, then it will be used. If the path
     * is relative, then it will be interpreted as if it were relative to the
     * {@code rootLocation} parameter.
     *
     * @param path
     *            The path string to be retrieved as a {@code File}
     * @param rootLocation
     *            the server root to resolve against
     * @return A {@code File} object that corresponds to the specified path.
     */
    public static File getFileForPath(String path, File rootLocation) {
        File f = new File(path);

        if (f.isAbsolute()) {
            // logger.trace("getFileForPath is absolute: {}", path);
            return f;
        } else {
            // logger.trace("getFileForPath is relative: {} resolving against {}",
            // path, serverRoot);
            return new File(rootLocation, path).getAbsoluteFile();
        }
    }

    public File getInstallLocation() {
        return getLocation(ServerConstants.LAUNCHER_INSTALL_LOCATION);
    }

    public File getProjectLocation() {
        return getLocation(ServerConstants.LAUNCHER_PROJECT_LOCATION);
    }

    public File getWorkingLocation() {
        return getLocation(ServerConstants.LAUNCHER_WORKING_LOCATION);
    }

    protected File getLocation(String propertyName) {
        String location = getProperty(propertyName, null, String.class);
        if (null == location) {
            location = getProperty(ServerConstants.PROPERTY_SERVER_ROOT, null, String.class);
        }
        return new File(null != location ? location : "").getAbsoluteFile();
    }

    public URL getInstallLocationURL() {
        URL location = getLocationURL(ServerConstants.LAUNCHER_INSTALL_URL);
        if (null == location) {
            try {
                location =
                        getLocation(ServerConstants.LAUNCHER_INSTALL_LOCATION).getAbsoluteFile()
                                .toURI().toURL();
            } catch (MalformedURLException e) {
                /* ignore because the file is absolute */
            }
        }
        return location;
    }

    public URL getProjectLocationURL() {
        URL location = getLocationURL(ServerConstants.LAUNCHER_PROJECT_URL);
        if (null == location) {
            try {
                location =
                        getLocation(ServerConstants.LAUNCHER_PROJECT_LOCATION).getAbsoluteFile()
                                .toURI().toURL();
            } catch (MalformedURLException e) {
                /* ignore because the file is absolute */
            }
        }
        return location;
    }

    public URL getWorkingLocationURL() {
        URL location = getLocationURL(ServerConstants.LAUNCHER_WORKING_URL);
        if (null == location) {
            try {
                location =
                        getLocation(ServerConstants.LAUNCHER_WORKING_LOCATION).getAbsoluteFile()
                                .toURI().toURL();
            } catch (MalformedURLException e) {
                /* ignore because the file is absolute */
            }
        }
        return location;
    }

    protected URL getLocationURL(String propertyName) {
        String location = getProperty(ServerConstants.LAUNCHER_PROJECT_URL, null, String.class);
        if (null != location) {
            try {
                return new URL(location);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("The " + propertyName + " has no valid value!",
                        e);
            }
        }
        return null;
    }

    /**
     * Retrieves the current running mode of Identity Server.
     * <p/>
     * Default running mode is the {@code Production}, that prohibit access to
     * some insecure method. Development mode allow access to all method. To
     * enable development mode set the
     * {@link ServerConstants#PROPERTY_DEBUG_ENABLE} system property
     * {@code true}.
     *
     * @return true if {@code Development} mode is on.
     */
    public static boolean isDevelopmentProfileEnabled() {
        String debug =
                IDENTITY_SERVER.get().getProperty(ServerConstants.PROPERTY_DEBUG_ENABLE, null,
                        String.class);
        return (null != debug) && Boolean.valueOf(debug);
    }

    /**
     * Loads boot properties file
     *
     * @return properties in boot properties file, keys in lower case
     */
    private Map<String, String> loadProps(String bootFileLocation, IdentityServer identityServer) {
        File bootFile = IdentityServer.getFileForPath(bootFileLocation, getServerRoot());
        Map<String, String> entries = new HashMap<String, String>();

        if (null == bootFile) {
            System.out.println("No boot file properties: "
                    + ServerConstants.PROPERTY_BOOT_FILE_LOCATION);
        } else if (!bootFile.exists()) {
            // TODO: move this class out of system bundle so we can use logging
            // logger.info("No boot properties file detected at {}.",
            // bootFile.getAbsolutePath());
            System.out.println("No boot properties file detected at " + bootFile.getAbsolutePath());
        } else if (null != identityServer && bootFile.equals(identityServer.bootPropertyFile)) {
            return identityServer.bootFileProperties;
        } else {
            // logger.info("Using boot properties at {}.",
            // bootFile.getAbsolutePath());
            System.out.println("Using boot properties at " + bootFile.getAbsolutePath());
            bootPropertyFile = bootFile;
            InputStream in = null;
            try {
                Properties prop = new Properties();
                in = new BufferedInputStream(new FileInputStream(bootFile));
                prop.load(in);
                for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                    entries.put((String) entry.getKey(), (String) entry.getValue());
                }
            } catch (FileNotFoundException ex) {
                // logger.info("Boot properties file {} not found",
                // bootFile.getAbsolutePath(), ex);
            } catch (IOException ex) {
                // logger.warn("Failed to load boot properties file {}",
                // bootFile.getAbsolutePath(), ex);
                throw new RuntimeException("Failed to load boot properties file "
                        + bootFile.getAbsolutePath() + " " + ex.getMessage(), ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        // logger.warn("Failure in closing boot properties file",
                        // ex);
                    }
                }
            }
        }

        return entries;
    }
}
