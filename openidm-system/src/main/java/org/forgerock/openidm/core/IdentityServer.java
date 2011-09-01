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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This class defines the core of the Identity Server.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class IdentityServer {

    /**
     * The singleton Identity Server instance.
     */
    private static IdentityServer identityServer = new IdentityServer();

    // The set of properties for the environment config.
    private final HashMap<String, String> configProperties;


    /**
     * Creates a new identity environment configuration initialized
     * from the system properties defined in the JVM.
     */
    public IdentityServer() {
        this(System.getProperties());
    }


    /**
     * Creates a new identity environment configuration initialized
     * with a copy of the provided set of properties.
     *
     * @param properties The properties to use when initializing this
     *                   environment configuration, or {@code null}
     *                   to use an empty set of properties.
     */
    public IdentityServer(Properties properties) {
        configProperties = new HashMap<String, String>();
        if (properties != null) {
            Enumeration propertyNames = properties.propertyNames();
            while (propertyNames.hasMoreElements()) {
                Object o = propertyNames.nextElement();
                configProperties.put(String.valueOf(o),
                        String.valueOf(properties.get(o)));
            }
        }
    }


    /**
     * Creates a new identity environment configuration initialized
     * with a copy of the provided set of properties.
     *
     * @param properties The properties to use when initializing this
     *                   environment configuration, or {@code null}
     *                   to use an empty set of properties.
     */
    public IdentityServer(Map<String, String> properties) {
        if (properties == null) {
            configProperties = new HashMap<String, String>();
        } else {
            configProperties = new HashMap<String, String>(properties);
        }
    }


    /**
     * Retrieves the property with the specified name.  The check will
     * first be made in the local config properties, but if no value is
     * found then the JVM system properties will be checked.
     *
     * @param name The name of the property to retrieve.
     * @return The property with the specified name, or {@code null} if
     *         no such property is defined.
     */
    public String getProperty(String name) {
        String value = configProperties.get(name);
        if (value == null) {
            value = System.getProperty(name);
        }
        return value;
    }

    /**
     * Retrieves the path to the root directory for this instance of the Identity
     * Server.
     *
     * @return The path to the root directory for this instance of the Identity
     *         Server.
     */
    public static String getServerRoot() {
        String root = identityServer.getProperty(ServerConstants.PROPERTY_SERVER_ROOT);
        if (null != root) {
            try {
                URI r = new URI(root);
                if (r.isAbsolute()) {
                    return root;
                } else {
                    return new File(r).getAbsolutePath();
                }
            } catch (URISyntaxException e) {
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
    public static URI getServerRootURI() {
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
        File f = new File(path);

        if (f.isAbsolute()) {
            return f;
        } else {
            return new File(getServerRoot(), path).getAbsoluteFile();
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
}
