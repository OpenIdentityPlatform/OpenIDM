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
package org.forgerock.openidm.crypto.util;

import java.security.GeneralSecurityException;

import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.crypto.impl.Main;

/**
 * Utility for handling Jetty configuration properties, including properties
 * that may need to be encrypted/obfuscated, and/or may be encrypted/obfuscated
 *
 * @author aegloff
 */
public class JettyPropertyUtil {
    
    /**
     * Gets an OpenIDM configuration property in an obfuscated format, 
     * compatible with Jetty obfuscation
     */
    public static String getPropertyJettyObfuscated(String name) {
        String prop = IdentityServer.getInstance().getProperty(name);
        if (prop != null) {
            try {
                String clear = new String(Main.unfold(prop));
                prop = Main.obfuscate(prop);
            } catch (GeneralSecurityException ex) {
                throw new RuntimeException("Failed ot obtain property " + name + " in Jetty obfuscated format.", ex);
            }
        }
        return prop;
    }
    
    /**
     * Get a Jetty configuration property
     */
    public static String getProperty(String propName) {
        return IdentityServer.getInstance().getProperty(propName);
    }
    
    /**
     * Get a Jetty configuration property as qualified file path
     */
    public static String getPathProperty(String propName) {
        String loc = getProperty(propName);
        if (loc != null) {
            loc = IdentityServer.getFileForInstallPath(loc).getAbsolutePath();
        }
        return loc;
    }
}
