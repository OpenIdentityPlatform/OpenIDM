/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 * $Id$
 */

package org.forgerock.openidm.web.osgi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import javax.servlet.ServletContext;
import java.util.Properties;
import java.util.ServiceLoader;
import org.apache.felix.main.AutoProcessor;
import org.apache.felix.main.Main;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.apache.felix.framework.util.Util;
import org.osgi.framework.BundleContext;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class FrameworkService {

    private final ServletContext context;
    private Framework framework;

    public FrameworkService(ServletContext context) {
        this.context = context;
    }

    public void start() {
        try {
            doStart(this.context.getRealPath("/WEB-INF/bundle/"), this.context.getRealPath("/WEB-INF/felix-cache/"));
        } catch (Exception e) {
            log("Failed to start framework", e);
        }
    }

    public void stop() {
        try {
            doStop();
        } catch (Exception e) {
            log("Error stopping framework", e);
        }
    }

    private void doStart(String bundleDir, String cacheDir) throws Exception {


        // Look for bundle directory and/or cache directory.
        // We support at most one argument, which is the bundle
        // cache directory.

        // Load system properties.
        loadSystemProperties();
        System.setProperty("user.dir", this.context.getRealPath("/WEB-INF"));
        // Read configuration properties.
        Properties configProps = loadConfigProperties();
        // If no configuration properties were found, then create
        // an empty properties object.
        if (configProps == null) {
            System.err.println("No " + Main.CONFIG_PROPERTIES_FILE_VALUE + " found.");
            configProps = new Properties();
        }

        // Copy framework properties from the system properties.
        Main.copySystemProperties(configProps);

        // If there is a passed in bundle auto-deploy directory, then
        // that overwrites anything in the config file.
        if (bundleDir != null) {
            configProps.setProperty(AutoProcessor.AUTO_DEPLOY_DIR_PROPERY, bundleDir);
        }

        // If there is a passed in bundle cache directory, then
        // that overwrites anything in the config file.
        if (cacheDir != null) {
            configProps.setProperty(Constants.FRAMEWORK_STORAGE, cacheDir);
        }

        // If enabled, register a shutdown hook to make sure the framework is
        // cleanly shutdown when the VM exits.
        String enableHook = configProps.getProperty(Main.SHUTDOWN_HOOK_PROP);
        if ((enableHook == null) || !enableHook.equalsIgnoreCase("false")) {
            Runtime.getRuntime().addShutdownHook(new Thread("Felix Shutdown Hook") {

                public void run() {
                    try {
                        if (framework != null) {
                            framework.stop();
                            framework.waitForStop(0);
                        }
                    } catch (Exception ex) {
                        log("Error stopping framework", ex);
                    }
                }
            });
        }


        // Create an instance of the framework.
        FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
        framework = factory.newFramework(configProps);
        // Initialize the framework, but don't start it yet.
        framework.init();
        // Use the system bundle context to process the auto-deploy
        // and auto-install/auto-start properties.
        AutoProcessor.process(configProps, framework.getBundleContext());
        this.context.setAttribute(BundleContext.class.getName(), framework.getBundleContext());
        // Start the framework.
        framework.start();

        log("OSGi framework started");
    }

    private void doStop() throws Exception {
        if (null != framework) {
            try {
                // Stop the framework
                framework.stop();
                // Wait for the framework to stop completely
                framework.waitForStop(0);
            } finally {
                // HEHE :) System.exit(0);
            }
        }

        log("OSGi framework stopped");
    }

//    private Map<String, Object> createConfig()
//            throws Exception {
//        Properties props = new Properties();
//        //props.load(this.context.getResourceAsStream("/WEB-INF/framework.properties"));
//        props.load(this.context.getResourceAsStream("/WEB-INF/config.properties"));
//
//        HashMap<String, Object> map = new HashMap<String, Object>();
//        for (Object key : props.keySet()) {
//            map.put(key.toString(), props.get(key));
//        }
//
//        //WAS 7 and 6.1 issue
//        map.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK);
//        //System.setProperty("user.dir",this.context.getRealPath("/WEB-INF/"));
//        map.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, Arrays.asList(new ProvisionActivator(this.context)));
//        return map;
//    }

    private void log(String message, Throwable cause) {
        this.context.log(message, cause);
    }

    private void log(String message) {
        this.context.log(message);
    }

    /**
     * <p>
     * Loads the properties in the system property file associated with the
     * framework installation into <tt>System.setProperty()</tt>. These properties
     * are not directly used by the framework in anyway. By default, the system
     * property file is located in the <tt>conf/</tt> directory of the Felix
     * installation directory and is called "<tt>system.properties</tt>". The
     * installation directory of Felix is assumed to be the parent directory of
     * the <tt>felix.jar</tt> file as found on the system class path property.
     * The precise file from which to load system properties can be set by
     * initializing the "<tt>felix.system.properties</tt>" system property to an
     * arbitrary URL.
     * </p>
     **/
    private void loadSystemProperties() {
        // The system properties file is either specified by a system
        // property or it is in the same directory as the Felix JAR file.
        // Try to load it from one of these places.

        // See if the property URL was specified as a property.
        URL propURL = null;
        String custom = System.getProperty(Main.SYSTEM_PROPERTIES_PROP);
        if (custom != null) {
            try {
                propURL = new URL(custom);
            } catch (MalformedURLException ex) {
                System.err.print("Main: " + ex);
                return;
            }
        }

        // Read the properties file.
        Properties props = new Properties();
        InputStream is = null;
        try {
            if (null == propURL) {
                is = this.context.getResourceAsStream("/WEB-INF/" + Main.CONFIG_DIRECTORY + "/" + Main.SYSTEM_PROPERTIES_FILE_VALUE);
            } else {
                is = propURL.openConnection().getInputStream();
            }
            props.load(is);
            is.close();
        } catch (FileNotFoundException ex) {
            // Ignore file not found.
        } catch (Exception ex) {
            System.err.println(
                    "Main: Error loading system properties from " + propURL);
            System.err.println("Main: " + ex);
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex2) {
                // Nothing we can do.
            }
            return;
        }

        // Perform variable substitution on specified properties.
        for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            System.setProperty(name,
                    Util.substVars(props.getProperty(name), name, null, null));
        }
    }

    /**
     * <p>
     * Loads the configuration properties in the configuration property file
     * associated with the framework installation; these properties
     * are accessible to the framework and to bundles and are intended
     * for configuration purposes. By default, the configuration property
     * file is located in the <tt>conf/</tt> directory of the Felix
     * installation directory and is called "<tt>config.properties</tt>".
     * The installation directory of Felix is assumed to be the parent
     * directory of the <tt>felix.jar</tt> file as found on the system class
     * path property. The precise file from which to load configuration
     * properties can be set by initializing the "<tt>felix.config.properties</tt>"
     * system property to an arbitrary URL.
     * </p>
     * @return A <tt>Properties</tt> instance or <tt>null</tt> if there was an error.
     **/
    private Properties loadConfigProperties() {
        // The config properties file is either specified by a system
        // property or it is in the conf/ directory of the Felix
        // installation directory.  Try to load it from one of these
        // places.

        // See if the property URL was specified as a property.
        URL propURL = null;
        String custom = System.getProperty(Main.CONFIG_PROPERTIES_PROP);
        if (custom != null) {
            try {
                propURL = new URL(custom);
            } catch (MalformedURLException ex) {
                System.err.print("Main: " + ex);
                return null;
            }
        }

        // Read the properties file.
        Properties props = new Properties();
        InputStream is = null;
        try {
            // Try to load config.properties.
            if (null == propURL) {
                is = this.context.getResourceAsStream("/WEB-INF/" + Main.CONFIG_DIRECTORY + "/" + Main.CONFIG_PROPERTIES_FILE_VALUE);
            } else {
                is = propURL.openConnection().getInputStream();

            }
            props.load(is);
            is.close();
        } catch (Exception ex) {
            // Try to close input stream if we have one.
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex2) {
                // Nothing we can do.
            }

            return null;
        }

        // Perform variable substitution for system properties.
        for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            props.setProperty(name,
                    Util.substVars(props.getProperty(name), name, null, props));
        }

        return props;
    }
}
