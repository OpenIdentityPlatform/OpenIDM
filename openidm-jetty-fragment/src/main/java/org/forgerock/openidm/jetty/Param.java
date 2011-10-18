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
package org.forgerock.openidm.jetty;

import org.forgerock.openidm.crypto.util.JettyPropertyUtil;

/**
 * Provides the Jetty bundle (and in turn the jetty.xml)
 * access to configuration supplied by OpenIDM, 
 * without having to resort to system properties
 *
 * @author aegloff
 */
public class Param {

    /**
     * @return Requested OpenIDM configuration property
     */
    public static String getProperty(String propertyName) {
        return JettyPropertyUtil.getProperty(propertyName);
    }
    
    /**
     * @return OpenIDM keystore type
     */
    public static String getKeystoreType() {
        return JettyPropertyUtil.getProperty("openidm.keystore.type");
    }

    /**
     * @return OpenIDM keystore provider
     */
    public static String getKeystoreProvider() {
        return JettyPropertyUtil.getProperty("openidm.keystore.provider");
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
        return JettyPropertyUtil.getPropertyJettyObfuscated("openidm.keystore.password");
    }
    
    /**
     * @return OpenIDM keystore key password, obfuscated in Jetty format.
     * If no specific key password is set, the keystore password (if present) is used.
     */
    public static String getKeystoreKeyPassword() {
        String obfPwd = JettyPropertyUtil.getPropertyJettyObfuscated("openidm.keystore.key.password");
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
     * @return  the truststore password.
     * If no truststore setting is set, the keystore setting (if present) is used.
     */
    public static String getTruststorePassword() {
        String obfPwd = JettyPropertyUtil.getPropertyJettyObfuscated("openidm.truststore.password");
        if (obfPwd == null) {
            obfPwd = getKeystorePassword();
        }
        return obfPwd;
    }
}

