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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        
        /* Initialize the SecurityManager using the local on disk keystore as
         * soon as the bundle is activated.  This is required in order to ensure
         * that the local OpenIDM keystore and truststore files are picked up
         * by the Repository Bootstrap Service. The SecurityManager will be
         * reloaded once the SecurityManager service is activated.
         */
        SecurityManager sm = new SecurityManager();
        sm.reload();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        //do nothing
    }
}
