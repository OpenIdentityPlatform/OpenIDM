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
 * Copyright 2011-2015 ForgeRock AS.
 */
package org.forgerock.openidm.jetty;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the Jetty bundle (and in turn the jetty.xml)
 * a way to mark which connector to not apply the OpenIDM authentication mechanism on.
 * Allows for just using SSL mutual auth on a given port.
 *
 */
public final class DisableOpenIDMAuth {

    final static Logger logger = LoggerFactory.getLogger(DisableOpenIDMAuth.class);
    
    private static Set<Integer> clientAuthOnly = new HashSet<Integer>();

    private DisableOpenIDMAuth() {}

    /**
     * Sets openidm.auth.clientauthonlyports if client auth is required.
     *
     * @param serverConnector A instance of the ServerConnector
     */
    public static void add(ServerConnector serverConnector) {
        int port = -1;
        SslConnectionFactory sslConnectionFactory = (SslConnectionFactory) serverConnector.getConnectionFactory("SSL-http/1.1");
        port = serverConnector.getPort();
        if (sslConnectionFactory != null) {
            boolean needClientAuth = sslConnectionFactory.getSslContextFactory().getNeedClientAuth();
            if (needClientAuth == false) {
                logger.warn("OpenIDM authentication disabled on port {} without the port requiring SSL mutual authentication.", port);
            } else {
                logger.info("Port {} set up to require SSL mutual authentication only, no additional OpenIDM authentication.", port);
            }
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

