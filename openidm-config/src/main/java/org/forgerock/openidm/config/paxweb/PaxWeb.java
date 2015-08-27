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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openidm.config.paxweb;

import org.forgerock.openidm.crypto.util.JettyPropertyUtil;

/**
 * Configures pax web by setting the pax web properties.
 */
public final class PaxWeb {

    private PaxWeb() {
    }

    /**
     * Sets the pax web properties.
     */
    public static void configurePaxWebProperties() {
        System.setProperty(
                "org.osgi.service.http.port", JettyPropertyUtil.getProperty("openidm.port.http", false));
        System.setProperty(
                "org.osgi.service.http.port.secure", JettyPropertyUtil.getProperty("openidm.port.https", false));
        System.setProperty(
                "org.ops4j.pax.web.ssl.keystore", JettyPropertyUtil.getProperty("openidm.keystore.location", false));
        System.setProperty(
                "org.ops4j.pax.web.ssl.keystore.type", JettyPropertyUtil.getProperty("openidm.keystore.type", false));
        System.setProperty(
                "org.ops4j.pax.web.ssl.password", JettyPropertyUtil.getProperty("openidm.keystore.password", true));
        System.setProperty(
                "org.ops4j.pax.web.ssl.keypassword", JettyPropertyUtil.getProperty("openidm.keystore.password", true));
    }
}
