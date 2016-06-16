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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.openidm.launcher;

import static org.kohsuke.args4j.ExampleMode.ALL;
import static org.kohsuke.args4j.ExampleMode.REQUIRED;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.DirectoryScanner;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonTransformer;
import org.forgerock.json.JsonValue;
import org.json.simple.parser.JSONParser;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * An OSGiDaemonBean starts the Embedded OSGi
 * {@link org.osgi.framework.launch.Framework}.
 * 
 * @author Laszlo Hordos
 */
public class OSGiFrameworkService extends AbstractOSGiFrameworkService {

    /**
     * The property name used to specify whether the launcher should install a
     * shutdown hook.
     */
    public static final String SHUTDOWN_HOOK_PROP = "shutdown.hook";
    /**
     * The property name used to specify an URL to the system property file.
     */
    public static final String SYSTEM_PROPERTIES_PROP = "system.properties";
    /**
     * The default name used for the system properties file.
     */
    public static final String SYSTEM_PROPERTIES_FILE_VALUE = "conf/system.properties";
    /**
     * The property name used to specify an URL to the configuration property
     * file to be used for the created the framework instance.
     */
    public static final String CONFIG_PROPERTIES_PROP = "config.properties";
    /**
     * The default name used for the configuration properties file.
     */
    public static final String CONFIG_PROPERTIES_FILE_VALUE = "conf/config.properties";

    /**
     * The property name used to specify an URL to the boot property file to be
     * used for the created the Boot Map Service.
     */
    public static final String BOOT_PROPERTIES_PROP = "boot.properties";
    /**
     * The default name used for the boot properties file.
     */
    public static final String BOOT_PROPERTIES_FILE_VALUE = "conf/boot/boot.properties";

    private String installDir;

    private String projectDir;

    private String workingDir;

    private String storageDir;

    private boolean verbose = false;

    private boolean newThread = false;

    private String configFile;

    @Option(name = "-P", usage = "custom parameters to configure the container", metaVar = "attribute=value")
    private Map bootParameters;

    // receives other command line parameters than options
    // Disallow other arguments https://bugster.forgerock.org/jira/browse/OPENIDM-1021
    //@Argument
    private List<String> arguments = new ArrayList<String>();

    /**
     * Properties to initiate the OSGi Framework
     */
    private Map configurationProperties = new HashMap<String, Object>();

    /**
     * Configuration of this {@link OSGiFrameworkService#init()}.
     */
    private JsonValue launcherConfiguration = null;

    private final JsonTransformer transformer;

    private final PropertyAccessor propertyAccessor;

    public OSGiFrameworkService() {

        propertyAccessor = new PropertyAccessor() {
            public <T> T get(String name) {
                Object value = null;
                if (null != bootParameters) {
                    value = bootParameters.get(name);
                }
                if (null == value && null != configurationProperties) {
                    value = configurationProperties.get(name);
                }
                if (null == value && null != launcherConfiguration) {
                    value = launcherConfiguration.get(name).getObject();
                }
                if (null == value) {
                    value = System.getProperty(name);
                }
                if (null == value) {
                    value = System.getenv(name);
                }

                T result = null;
                try {
                    result = (T) value;
                } catch (ClassCastException e) {
                    /* ignore */
                }
                return result;
            }
        };

        transformer = new JsonTransformer() {
            public void transform(JsonValue value) throws JsonException {
                if (null != value && value.isString()) {
                    value.setObject(ConfigurationUtil.substVars(value.asString(), propertyAccessor));
                }
            }
        };
    }

    /**
     *  Extends the passed in PropertyAccessor to check for the property in the extendedMap prior to deferring to
     *  the passed in PropertyAccessor.
     */
    private static class ExtendedPropertyAccessor implements PropertyAccessor {
        private final Map<String, Object> extendedMap;
        private PropertyAccessor propertyAccessor;

        /**
         * Creates the extended accessor.
         * @param extendedMap the map to look for the property first.
         * @param propertyAccessor the original accessor to refer to if not found in the extendedMap.
         */
        public ExtendedPropertyAccessor(Map<String, Object> extendedMap, PropertyAccessor propertyAccessor) {
            this.extendedMap = extendedMap;
            this.propertyAccessor = propertyAccessor;
        }

        @Override
        public <T> T get(String name) {
            Object value = extendedMap.get(name);
            if (null == value) {
                value = propertyAccessor.get(name);
            }
            T result = null;
            try {
                result = (T) value;
            } catch (ClassCastException e) {
                /* ignore */
            }
            return result;
        }
    }

    public String getInstallDir() {
        return installDir;
    }

    @Option(name = "-i", aliases = { "--install-location" },
            usage = "install folder (default value is the 'user.dir')")
    public void setInstallDir(String value) {
        this.installDir = value;
    }

    public String getProjectDir() {
        return projectDir;
    }

    @Option(name = "-p", aliases = { "--project-location" },
            usage = "project folder (default value is the 'user.dir')")
    public void setProjectDir(String value) {
        this.projectDir = value;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    @Option(name = "-w", aliases = { "--working-location" },
            usage = "working folder (default value is the 'user.dir')")
    public void setWorkingDir(String value) {
        this.workingDir = value;
    }

    public String getConfigFile() {
        return configFile;
    }

    @Option(name = "-c", aliases = { "--config" }, required = false,
            usage = "osgi configuration file", metaVar = "launcher.json")
    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    protected boolean isVerbose() {
        return verbose;
    }

    @Option(name = "-v", aliases = { "--verbose" }, usage = "enable verbose output")
    public void setVerbose(boolean value) {
        verbose = value;
    }

    public String getStorageDir() {
        return storageDir;
    }

    @Option(name = "-s", aliases = { "--storage" },
            usage = "OSGi storage (org.osgi.framework.storage) location")
    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }

    public Map getBootParameters() {
        return bootParameters;
    }

    public void setBootParameters(Map bootParameters) {
        this.bootParameters = bootParameters;
    }

    public boolean isNewThread() {
        return newThread;
    }

    @Option(name = "-t", aliases = { "--thread" }, usage = "start new thread in the background")
    public void setNewThread(boolean value) {
        newThread = value;
    }

    protected long getStopTimeout() {
        return 0;
    }

    @SuppressWarnings({ "unchecked" })
    public void init(String[] arguments) throws Exception {
        CmdLineParser parser = new CmdLineParser(this);

        // if you have a wider console, you could increase the value;
        // here 80 is also the default
        parser.setUsageWidth(80);

        try {
            // parse the arguments.
            parser.parseArgument(arguments);
        } catch (CmdLineException e) {
            // if there's a problem in the command line,
            // you'll get this exception. this will report
            // an error message.
            System.err.println(e.getMessage());
            System.err.println("java Main [options...] arguments...");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();

            // print option sample. This is useful some time
            System.err.println("  Example: java Main" + parser.printExample(REQUIRED));
            System.err.println("  Example: java Main" + parser.printExample(ALL));

            throw e;
        }
        init();
    }

    public void destroy() {

    }

    public Bundle getSystemBundle() {
        return getFramework();
    }

    public void init() throws Exception {
        if (null == bootParameters) {
            bootParameters = new HashMap<String, Object>();
        }

        URI installLocation = getInstallURI();
        bootParameters.put(LAUNCHER_INSTALL_LOCATION, new File(installLocation).getAbsolutePath());
        bootParameters.put(LAUNCHER_INSTALL_URL, installLocation.toURL().toString());

        URI projectLocation = getProjectURI();
        bootParameters.put(LAUNCHER_PROJECT_LOCATION, new File(projectLocation).getAbsolutePath());
        bootParameters.put(LAUNCHER_PROJECT_URL, projectLocation.toURL().toString());

        URI workingLocation = getWorkingURI();
        bootParameters.put(LAUNCHER_WORKING_LOCATION, new File(workingLocation).getAbsolutePath());
        bootParameters.put(LAUNCHER_WORKING_URL, workingLocation.toURL().toString());

        BufferedReader input = null;
        try {
            if (null != configFile) {
                File _configFile = getFileForPath(configFile, projectLocation);
                if (!_configFile.isFile()) {
                    _configFile = getFileForPath(configFile, installLocation);
                }
                if (_configFile.isFile()) {
                    input = new BufferedReader(new FileReader(_configFile));
                } else {
                    throw new IllegalArgumentException(
                            "Boot OSGi configuration file does not exists: "
                                    + _configFile.getAbsolutePath());
                }
            } else {
                input =
                        new BufferedReader(new InputStreamReader(Main.class
                                .getResourceAsStream("/launcher.json")));
                if (null == input) {
                    throw new IllegalArgumentException(
                            "Boot OSGi configuration file does not exists on CLASSPATH: "
                                    + Main.class.getResource("/").toString() + "/launcher.json");
                }
            }
            launcherConfiguration = new JsonValue((new JSONParser()).parse(input));
            launcherConfiguration.getTransformers().add(transformer);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    /* ignore */
                }
            }
        }

        // Load system properties.
        loadSystemProperties(launcherConfiguration, projectLocation);

        // Read configuration properties.
        loadConfigProperties(configurationProperties, launcherConfiguration, projectLocation);
        configurationProperties.putAll(bootParameters);

        // Copy framework properties from the system properties.
        copySystemProperties(configurationProperties);

        configurationProperties.put(Constants.FRAMEWORK_STORAGE,
                URLDecoder.decode(getFileForPath(determineStorageDir(), getWorkingURI()).getAbsolutePath(), "UTF-8"));

        // Append the custom bootProperties
        for (Map.Entry<String, Object> entry : loadBootProperties(launcherConfiguration,
                projectLocation).entrySet()) {
            if (null != entry.getValue() && !bootParameters.containsKey(entry.getKey())) {
                bootParameters.put(entry.getKey(), entry.getValue());
            }
        }

        // If enabled, register a shutdown hook to make sure the framework is
        // cleanly shutdown when the VM exits.
        Object enableHook = getConfigurationProperties().get(SHUTDOWN_HOOK_PROP);
        if ((enableHook == null)
                || ((enableHook instanceof String) && !((String) enableHook)
                        .equalsIgnoreCase("false"))) {
            Runtime.getRuntime().addShutdownHook(new Thread("Felix Shutdown Hook") {
                public void run() {
                    try {
                        OSGiFrameworkService.this.stop();
                    } catch (Exception ex) {
                        if (isVerbose()) {
                            System.err.println("Error stopping framework: " + ex);
                        }
                    }
                }
            });
        }

    }

    /**
     * If there is a passed in bundle cache directory (-s), then
     * that overwrites anything in the config file, then if
     * the config.properties file doesn't provide the setting, default to
     * "felix-cache".
     * <p/>
     * Refer to config.properties: org.osgi.framework.storage
     *
     * @return The directory to store the "felix-cache".
     */
    private String determineStorageDir() {
        // If there is a passed in bundle cache directory, then
        // that overwrites anything in the config file.
        String cmdLineStorageDir = getStorageDir();
        Object configPropsStorageDir = configurationProperties.get(Constants.FRAMEWORK_STORAGE);
        String determinedStorageDir;
        if (null != cmdLineStorageDir) {
            determinedStorageDir = cmdLineStorageDir;
        } else if (configPropsStorageDir instanceof String) {
            determinedStorageDir = (String) configPropsStorageDir;
        } else {
            determinedStorageDir = "felix-cache";
        }
        return determinedStorageDir;
    }

    protected void registerServices(BundleContext bundleContext) throws Exception {
        Dictionary<String, String> properties2 = new Hashtable<String, String>(4);
        properties2.put(Constants.SERVICE_VENDOR, "ForgeRock AS.");
        properties2.put(Constants.SERVICE_DESCRIPTION, "OSGi Framework Service");
        properties2.put(Constants.SERVICE_PID, OSGiFramework.class.getName());
        bundleContext.registerService(OSGiFramework.class, this, properties2);

        Dictionary<String, String> properties = new Hashtable<String, String>(4);
        properties.put(Constants.SERVICE_VENDOR, "ForgeRock AS.");
        properties.put(Constants.SERVICE_DESCRIPTION, "Boot Configuration");
        properties.put(Constants.SERVICE_PID, BootConfiguration.class.getName());
        bundleContext.registerService(Map.class, Collections.unmodifiableMap(bootParameters), properties);
    }

    protected Map<String, String> getConfigurationProperties() {
        if (null == configurationProperties) {
            configurationProperties = new HashMap<String, Object>();
        }
        return configurationProperties;
    }

    public void setConfigurationProperties(Map configurationProperties) {
        this.configurationProperties = configurationProperties;
    }

    public JsonValue getLauncherConfiguration() {
        if (null == launcherConfiguration) {
            launcherConfiguration = new JsonValue(new HashMap<String, Object>());
            launcherConfiguration.getTransformers().add(transformer);
        }
        return launcherConfiguration;
    }

    public void setLauncherConfiguration(JsonValue launcherConfiguration) {
        this.launcherConfiguration = launcherConfiguration;
    }

    protected List<BundleHandler> listBundleHandlers(BundleContext context)
            throws MalformedURLException {
        JsonValue bundle = getLauncherConfiguration().get("bundle");
        BundleHandlerBuilder defaultBuilder =
                BundleHandlerBuilder.newBuilder(bundle.get("default"));

        List<BundleHandler> result = new ArrayList<BundleHandler>();

        URI installDirectory = getInstallURI();

        for (JsonValue container : bundle.get("containers")) {
            BundleHandlerBuilder innerBuilder =
                    BundleHandlerBuilder.newBuilder(container, defaultBuilder);

            String location = container.get("location").required().asString();
            if (location.toLowerCase().endsWith(".zip")) {
                File inputFile = getFileForPath(location, installDirectory);
                for (URL url : ConfigurationUtil.getZipFileListing(inputFile.toURI().toURL(),
                        container.get("includes").asList(String.class), container.get("excludes")
                                .asList(String.class))) {
                    result.add(innerBuilder.build(url));
                }

            } else if (location.toLowerCase().endsWith(".jar")) {
                File inputFile = getFileForPath(location, installDirectory);
                result.add(innerBuilder.build(inputFile.toURI().toURL()));
            } else {
                DirectoryScanner scanner = new DirectoryScanner();
                scanner.setBasedir(getFileForPath(location, installDirectory));
                if (container.isDefined("includes")) {
                    List<String> includes = container.get("includes").asList(String.class);
                    scanner.setIncludes(includes.toArray(new String[includes.size()]));
                }
                if (container.isDefined("excludes")) {
                    List<String> includes = container.get("excludes").asList(String.class);
                    scanner.setExcludes(includes.toArray(new String[includes.size()]));
                }
                scanner.scan();

                for (String bundleLocation : scanner.getIncludedFiles()) {
                    BundleHandler newHandler =
                            innerBuilder.build(scanner.getBasedir().toURI().resolve(
                                    bundleLocation.replaceAll("\\\\", "/")).toURL());
                    for (BundleHandler handler : result) {
                        if (newHandler.getBundleUrl().equals(handler.getBundleUrl())) {
                            if (newHandler.getActions().equals(handler.getActions())
                                    && newHandler.getStartLevel() == newHandler.getStartLevel()) {
                                // Do not duplicate
                                newHandler = null;
                                break;
                            } else {
                                StringBuilder sb =
                                        new StringBuilder("Controversial provisioning between ");
                                sb.append(handler).append(" and ").append(newHandler);
                                throw new IllegalArgumentException(sb.toString());
                            }
                        }
                    }
                    if (null != newHandler) {
                        result.add(newHandler);
                    }
                }
            }
        }
        return result;
    }

    /**
     * <p>
     * Loads the properties in the system property file associated with the
     * framework installation into <tt>System.setProperty()</tt>. These
     * properties are not directly used by the framework in anyway. By default,
     * the system property file is located in the <tt>conf/</tt> directory and
     * is called "<tt>system.properties</tt>".
     * </p>
     */
    protected void loadSystemProperties(JsonValue configuration, URI projectDirectory) {
        JsonValue systemProperties = configuration.get(SYSTEM_PROPERTIES_PROP);
        if (systemProperties.isMap()) {
            for (Map.Entry<String, Object> entry : systemProperties.copy().asMap().entrySet()) {
                // The user.dir MUST not be overwritten!!!
                if (entry.getValue() instanceof String && !"user.dir".equals(entry.getKey())) {
                    System.setProperty(entry.getKey(), (String) entry.getValue());
                }
            }
        } else {
            Properties props =
                    loadPropertyFile(projectDirectory, systemProperties.expect(String.class)
                            .defaultTo(SYSTEM_PROPERTIES_FILE_VALUE).asString());
            if (props == null)
                return;
            // Perform variable substitution on specified properties.
            for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
                String name = (String) e.nextElement();
                if (!"user.dir".equals(name)) {
                    Object newValue =
                            ConfigurationUtil.substVars(props.getProperty(name), propertyAccessor);
                    if (newValue instanceof String) {
                        System.setProperty(name, (String) newValue);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Loads the configuration properties in the configuration property file
     * associated with the framework installation; these properties are
     * accessible to the framework and to bundles and are intended for
     * configuration purposes. By default, the configuration property file is
     * located in the <tt>conf/</tt> directory and is called "
     * <tt>config.properties</tt>".
     * </p>
     *
     * @param configurationProperties Current evaluated properties.
     * @param configuration Configuration of where the config properties file is located or the already loaded Map of
     * config settings.
     * @param projectDirectory Working directory to start looking for the properties file.
     */
    protected void loadConfigProperties(
            Map<String, Object> configurationProperties, JsonValue configuration, URI projectDirectory) {
        JsonValue systemProperties = configuration.get(CONFIG_PROPERTIES_PROP);
        if (systemProperties.isMap()) {
            // Substitute all variables
            systemProperties = systemProperties.copy();
        } else {
            Properties props =
                    loadPropertyFile(projectDirectory, systemProperties.expect(String.class)
                            .defaultTo(CONFIG_PROPERTIES_FILE_VALUE).asString());
            if (props == null) {
                configurationProperties.clear();
            }
            // Perform variable substitution on properties loaded from other sources.
            systemProperties = (new JsonValue(props, null, Arrays.asList(transformer))).copy();
        }

        // Perform variable substitution on all properties that have been loaded thus far, including those just loaded,
        // by extending the existing propertyAccessor to include properties that have just been read.
        Map<String, Object> propertiesMap = systemProperties.asMap();
        PropertyAccessor extendedPropertyAccessor = new ExtendedPropertyAccessor(propertiesMap, propertyAccessor);
        for (Map.Entry<String, Object> entry : propertiesMap.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                // Exclude the null and non String values
                Object newValue =
                        ConfigurationUtil.substVars((String) value, extendedPropertyAccessor);
                configurationProperties.put(entry.getKey(), newValue);
            }
        }
    }

    /**
     * <p>
     * Loads the boot properties in the configuration property file associated
     * with the framework installation; these properties are accessible to the
     * framework and to bundles and are intended for configuration purposes. By
     * default, the configuration property file is located in the <tt>conf/</tt>
     * directory and is called " <tt>config.properties</tt>".
     * </p>
     * 
     * @return A <tt>Map<String, Object></tt> instance or <tt>null</tt> if there
     *         was an error.
     */
    protected Map<String, Object> loadBootProperties(JsonValue configuration, URI projectDirectory) {
        JsonValue bootProperties = configuration.get(BOOT_PROPERTIES_PROP);
        if (bootProperties.isMap()) {
            // Substitute all variables
            return bootProperties.copy().asMap();
        } else {
            Properties props =
                    loadPropertyFile(projectDirectory, bootProperties.expect(String.class)
                            .defaultTo(BOOT_PROPERTIES_FILE_VALUE).asString());
            if (props == null)
                return new HashMap<String, Object>(0);
            // Perform variable substitution on specified properties.
            return (new JsonValue(props, null, Arrays.asList(transformer))).expect(Map.class)
                    .copy().asMap();
        }
    }

    protected Properties loadPropertyFile(URI projectDirectory, String propertyFile) {
        // Read the properties file.
        Properties props = new Properties();
        InputStream is = null;
        try {
            File pFile = new File(propertyFile);
            if (!pFile.isAbsolute()) {
                is =
                        projectDirectory.resolve(propertyFile.toString()).toURL().openConnection()
                                .getInputStream();
            } else {
                is =  pFile.toURI().toURL().openConnection().getInputStream();
            }
            props.load(is);
            is.close();
        } catch (MalformedURLException ex) {
            System.err.print("Main: " + ex);
            return null;
        } catch (FileNotFoundException ex) {
            // Ignore file not found.
        } catch (Exception ex) {
            System.err.append("Main: Error loading properties from ").println(propertyFile);
            System.err.println("Main: " + ex);
            try {
                if (is != null)
                    is.close();
            } catch (IOException ex2) {
                // Nothing we can do.
            }
            return null;
        }
        return props;
    }

    protected void copySystemProperties(Map<String, String> configProps) {
        for (Enumeration e = System.getProperties().propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            if (key.startsWith("org.osgi.framework.")) {
                configProps.put(key, System.getProperty(key));
            }
        }
    }

    protected Map<String, String> loadCliParameters() {
        /*
         * Match any name=value name="value value" name="value%20value"
         */
        Pattern argumentPattern = Pattern.compile("^(\\w+)=[\"]?([^\"]+)[\"]?$");
        Map<String, String> params = new HashMap<String, String>(arguments.size());
        for (String arg : arguments) {
            Matcher matcher = argumentPattern.matcher(arg);
            if (matcher.matches()) {
                params.put(matcher.group(0), matcher.group(1));
            }
        }
        return params;
    }

    /**
     * Retrieves a <CODE>File</CODE> object corresponding to the specified path.
     * If the given path is an absolute path, then it will be used. If the path
     * is relative, then it will be interpreted as if it were relative to the
     * Identity Server root.
     * 
     * @param path
     *            The path string to be retrieved as a <CODE>File</CODE>
     * @param rootDir
     *            the server root to resolve against
     * @return A <CODE>File</CODE> object that corresponds to the specified
     *         path.
     */
    public File getFileForPath(String path, URI rootDir) {
        File f = new File(path);
        if (f.isAbsolute()) {
            return f;
        } else {
            return new File(rootDir.resolve(path)).getAbsoluteFile();
        }
    }

    /**
     * Retrieves the path to the install directory for this instance of the
     * Identity Server.
     * 
     * @return The path to the root directory for this instance of the Identity
     *         Server.
     */
    public URI getInstallURI() {
        return getAbsoluteURI(getInstallDir());
    }

    /**
     * Retrieves the path to the project directory for this instance of the
     * Identity Server.
     * 
     * @return The path to the project directory for this instance of the
     *         Identity Server.
     */
    public URI getProjectURI() {
        return getAbsoluteURI(getProjectDir());
    }

    /**
     * Retrieves the path to the working directory for this instance of the
     * Identity Server.
     * 
     * @return The path to the working directory for this instance of the
     *         Identity Server.
     */
    public URI getWorkingURI() {
        return getAbsoluteURI(getWorkingDir());
    }

    private URI getAbsoluteURI(String directory) {

        if (null != directory) {
            File r = new File(directory);
            if (r.isAbsolute()) {
                return r.toURI();
            } else {
                return r.getAbsoluteFile().toURI();
            }
        }
        // We don't know where the server root is, so we'll have to assume it's
        // the current working directory.
        return new File(System.getProperty("user.dir")).toURI();
    }

}
