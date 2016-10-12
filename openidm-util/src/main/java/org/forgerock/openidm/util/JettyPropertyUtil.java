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

package org.forgerock.openidm.util;

import java.security.GeneralSecurityException;

import org.forgerock.openidm.core.IdentityServer;

/**
 * Utility for handling Jetty configuration properties, including properties
 * that may need to be encrypted/obfuscated, and/or may be encrypted/obfuscated
 *
 */
public class JettyPropertyUtil {

    /**
     * Gets a Jetty configuration property. If obfuscated is true, it will
     * return a value obfuscated in Jetty format.
     *
     * @param propName
     *            name of the property.
     * @param obfuscated
     *            if value should be obfuscated.
     * @return the property value.
     */
    public static String getProperty(String propName, boolean obfuscated) {
        final String clear = decryptOrDeobfuscate(IdentityServer.getInstance().getProperty(propName));
        if (obfuscated) {
            return CryptoUtil.obfuscate(clear);
        } else {
            return clear;
        }
    }

    /**
     * Decrypts or de-obfuscates a string. Will decrypt if string is appended with 'CRYPT:' and
     * will de-obfuscate if string is appended with 'OBF:'. If neither prefix is present, will
     * return the same value. Will return null if encrypted is null or encountered an error.
     *
     * @see CryptoUtil for further details
     *
     * @param encrypted
     *            string value that will be decrypted/de-obfuscated.
     * @return decrypted/de-obfuscated value.
     * @throws java.lang.RuntimeException
     */
    public static String decryptOrDeobfuscate(String encrypted) {
        if (encrypted == null) {
            return null;
        }
        final String decrypted;
        try {
            decrypted = new String(CryptoUtil.unfold(encrypted));
        } catch (GeneralSecurityException ex) {
            throw new RuntimeException("Failed to decrypt/de-obfuscate value of string", ex);
        }
        return decrypted;
    }

    /**
     * Get a Jetty configuration property as qualified file path
     *
     * @param propName
     *            name of the property.
     * @return the property value.
     */
    public static String getPathProperty(String propName) {
        String loc = getProperty(propName, false);
        if (loc != null) {
            loc = IdentityServer.getFileForInstallPath(loc).getAbsolutePath();
        }
        return loc;
    }
}
