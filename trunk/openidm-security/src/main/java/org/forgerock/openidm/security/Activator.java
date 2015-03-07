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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidm.security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.forgerock.openidm.jetty.Param;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.security.Security;



public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext bundleContext) throws Exception {

        Security.addProvider(new BouncyCastleProvider());

        // Set System properties
        if (System.getProperty("javax.net.ssl.keyStore") == null) {
            System.setProperty("javax.net.ssl.keyStore", Param.getKeystoreLocation());
            System.setProperty("javax.net.ssl.keyStorePassword", Param.getKeystorePassword(false));
            System.setProperty("javax.net.ssl.keyStoreType", Param.getKeystoreType());
        }
        if (System.getProperty("javax.net.ssl.trustStore") == null) {
            System.setProperty("javax.net.ssl.trustStore", Param.getTruststoreLocation());
            System.setProperty("javax.net.ssl.trustStorePassword", Param.getTruststorePassword(false));
            System.setProperty("javax.net.ssl.trustStoreType", Param.getTruststoreType());
        }

    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        //do nothing
    }
}
