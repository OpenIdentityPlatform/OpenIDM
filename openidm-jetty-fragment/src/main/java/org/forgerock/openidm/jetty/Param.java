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
package org.forgerock.openidm.jetty;

import org.forgerock.openidm.crypto.util.JettyPropertyUtil;

/**
 * Provides the Jetty bundle (and in turn the jetty.xml)
 * access to configuration supplied by OpenIDM,
 * without having to resort to system properties
 *
 */
public class Param {

    /**
     * @return Requested OpenIDM configuration property
     */
    public static String getProperty(String propertyName) {
        return JettyPropertyUtil.getProperty(propertyName, false);
    }

    /**
     * @return OpenIDM default certAlias
     */
    public static String getCertAlias() {
        return JettyPropertyUtil.getProperty("openidm.https.keystore.cert.alias", false);
    }
    
    /**
     * @return OpenIDM keystore type
     */
    public static String getKeystoreType() {
        return JettyPropertyUtil.getProperty("openidm.keystore.type", false);
    }

    /**
     * @return OpenIDM keystore provider
     */
    public static String getKeystoreProvider() {
        return JettyPropertyUtil.getProperty("openidm.keystore.provider", false);
    }

    /**
     * @return OpenIDM keystore location, as absolute path.
     */
    public static String getKeystoreLocation() {
        String loc = JettyPropertyUtil.getPathProperty("openidm.keystore.location");
        return loc;
    }

    /**
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
        String obfPwd = JettyPropertyUtil.getProperty("openidm.keystore.key.password", true);
        if (obfPwd == null) {
            obfPwd = getKeystorePassword();
        }
        return obfPwd;
    }

    /**
     * @return the truststore location, as absolute path.
     * If no truststore setting is set, the keystore setting (if present) is used.
     */
    public static String getTruststoreLocation() {
        String trustLoc = JettyPropertyUtil.getPathProperty("openidm.truststore.location");
        if (trustLoc == null) {
            trustLoc = getKeystoreLocation();
        }
        return trustLoc;
    }

    /**
     * @return the truststore location, as absolute path.
     * If no truststore setting is set, the keystore setting (if present) is used.
     */
    public static String getTruststoreType() {
        String trustType = JettyPropertyUtil.getProperty("openidm.truststore.type", false);
        if (trustType == null) {
            trustType = getKeystoreType();
        }
        return trustType;
    }

    /**
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
        String pwd = JettyPropertyUtil.getProperty("openidm.truststore.password", obfuscated);
        if (pwd == null) {
            pwd = getKeystorePassword(obfuscated);
        }
        return pwd;
    }

    /**
     * Returns the keystore password in either clear text or obfuscated.
     *
     * @param   obfuscated if the password should be obfuscated.
     * @return  the keystore password.
     */
    public static String getKeystorePassword(boolean obfuscated) {
        return JettyPropertyUtil.getProperty("openidm.keystore.password", obfuscated);
    }
}

