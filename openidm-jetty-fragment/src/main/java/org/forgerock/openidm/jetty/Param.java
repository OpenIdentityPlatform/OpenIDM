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
 * Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.openidm.jetty;

import static org.forgerock.openidm.core.IdentityServer.*;
import static org.forgerock.openidm.crypto.util.JettyPropertyUtil.getPathProperty;

import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.crypto.util.JettyPropertyUtil;

/**
 * Provides the Jetty bundle (and in turn the jetty.xml) access to configuration supplied by OpenIDM,
 * without having to resort to system properties.
 */
public class Param {

    private static final String NONE = "NONE";
    private static final String JETTY_CONF_LOCATION = "conf/jetty.xml";

    /**
     * Gets an unobfuscated {@link IdentityServer} property.
     * @param propertyName the property
     * @return Requested OpenIDM configuration property
     */
    public static String getProperty(String propertyName) {
        return JettyPropertyUtil.getProperty(propertyName, false);
    }

    /**
     * Gets the https cert alias.
     * @return OpenIDM default certAlias
     */
    public static String getCertAlias() {
        return JettyPropertyUtil.getProperty(HTTPS_KEYSTORE_CERT_ALIAS, false);
    }
    
    /**
     * Gets the keystore type.
     * @return OpenIDM keystore type
     */
    public static String getKeystoreType() {
        return JettyPropertyUtil.getProperty(KEYSTORE_TYPE, false);
    }

    /**
     * Gets the keystore provider.
     * @return OpenIDM keystore provider
     */
    public static String getKeystoreProvider() {
        return JettyPropertyUtil.getProperty(KEYSTORE_PROVIDER, false);
    }

    /**
     * Gets the keystore location.
     * @return OpenIDM keystore location, as absolute pathh, or if the path is NONE, return the jetty.xml file location.
     */
    public static String getKeystoreLocation() {
        final String path = getPathProperty(KEYSTORE_LOCATION);
        if (path != null && NONE.equalsIgnoreCase(path)) {
            return IdentityServer.getFileForInstallPath(JETTY_CONF_LOCATION).getAbsolutePath();
        }
        return path;
    }

    /**
     * Gets the keystore password.
     * @return OpenIDM keystore password, obfuscated in Jetty format.
     */
    public static String getKeystorePassword() {
        return getKeystorePassword(true);
    }

    /**
     * @return OpenIDM keystore key password, obfuscated in Jetty format.
     * If no specific key password is set, the keystore password (if present) is used.
     */
    public static String getKeystoreKeyPassword() {
        String obfPwd = JettyPropertyUtil.getProperty(KEYSTORE_KEY_PASSWORD, true);
        if (obfPwd == null) {
            obfPwd = getKeystorePassword();
        }
        return obfPwd;
    }

    /**
     * Gets the truststore location.
     * @return the truststore location, as absolute path.
     * If no truststore setting is set, the keystore setting (if present) is used.
     */
    public static String getTruststoreLocation() {
        String path = getPathProperty(TRUSTSTORE_LOCATION);
        if (path == null) {
            path = getKeystoreLocation();
        } else if (NONE.equalsIgnoreCase(path)) {
            return IdentityServer.getFileForInstallPath(JETTY_CONF_LOCATION).getAbsolutePath();
        }
        return path;
    }

    /**
     * Gets the truststore type.
     * @return the truststore location, as absolute path.
     * If no truststore setting is set, the keystore setting (if present) is used.
     */
    public static String getTruststoreType() {
        String trustType = JettyPropertyUtil.getProperty(TRUSTSTORE_TYPE, false);
        if (trustType == null) {
            trustType = getKeystoreType();
        }
        return trustType;
    }

    /**
     * Gets the truststore password.
     * @return  the truststore password, obfuscated in Jetty format.
     * If no truststore setting is set, the keystore setting (if present) is used.
     */
    public static String getTruststorePassword() {
        String obfPwd = getTruststorePassword(true);
        if (obfPwd == null) {
            obfPwd = getKeystorePassword();
        }
        return obfPwd;
    }

    /**
     * Returns the truststore password in either clear text or obfuscated.
     * If no truststore setting is set, the keystore setting (if present) is used.
     *
     * @param   obfuscated if the password should be obfuscated.
     * @return  the truststore password.
     */
    public static String getTruststorePassword(boolean obfuscated) {
        String pwd = JettyPropertyUtil.getProperty(TRUSTSTORE_PASSWORD, obfuscated);
        if (pwd == null) {
            pwd = getKeystorePassword(obfuscated);
        }
        return pwd;
    }

    /**
     * Gets the truststore provider.
     * @return OpenIDM keystore provider.
     */
    public static String getTruststoreProvider() {
        return JettyPropertyUtil.getProperty(TRUSTSTORE_PROVIDER, false);
    }

    /**
     * Gets the keystore password in either clear text or obfuscated.
     *
     * @param   obfuscated if the password should be obfuscated.
     * @return  the keystore password.
     */
    public static String getKeystorePassword(boolean obfuscated) {
        return JettyPropertyUtil.getProperty(KEYSTORE_PASSWORD, obfuscated);
    }
}

