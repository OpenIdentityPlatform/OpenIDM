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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ssl.SslConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the Jetty bundle (and in turn the jetty.xml)
 * a way to mark which connector to not apply the OpenIDM authentication mechanism on.
 * Allows for just using SSL mutual auth on a given port.
 *
 */
public class DisableOpenIDMAuth {

    final static Logger logger = LoggerFactory.getLogger(DisableOpenIDMAuth.class);
    
    private static Set<Integer> clientAuthOnly = new HashSet<Integer>();
    
    /**
     * @return Requested OpenIDM configuration property
     */
    public static void add(Object connector) {
        int port = -1;
        if (connector instanceof SslConnector) {
            SslConnector sslConnector = (SslConnector) connector;
            port = sslConnector.getPort();
            boolean needClientAuth = sslConnector.getNeedClientAuth();
            if (needClientAuth == false) {
                logger.warn("OpenIDM authentication disabled on port {} without the port requiring SSL mutual authentication.", port);
            } else {
                logger.info("Port {} set up to require SSL mutual authentication only, no additional OpenIDM authentication.", port);
            }
        } else if (connector instanceof Connector) {
            Connector plainConnector = (Connector) connector;
            port = plainConnector.getPort();
            logger.warn("OpenIDM authentication disabled on port {} without SSL.", port);
        } else {
            logger.warn("Connector type not recognized and can not disable authentication on it. {}", connector);
            return;
        }
        clientAuthOnly.add(Integer.valueOf(port));
        setProperty();
     }
    
    // TODO: Replace properties mechanism
    private static void setProperty() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Integer port : clientAuthOnly) {
            if (!first) {
                sb.append(",");
            }
            sb.append(port);
            first = false;
        }
        System.setProperty("openidm.auth.clientauthonlyports", sb.toString());
    }
    
}

