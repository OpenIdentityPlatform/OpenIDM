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
 * Copyright 2013-2016 ForgeRock AS.
 */
package org.forgerock.openidm.keystore.impl;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;
import javax.net.ssl.X509KeyManager;

/**
 * A X509KeyManager which selects client key aliases based upon a map of hostname to key alias values.
 * If a map entry does not exist for a given host the lookup falls through to the existing default
 * X509KeyManager.
 */
class MappedAliasKeyManager implements X509KeyManager {
    
    private final X509KeyManager keyManager;
    private final Map<String,String> hostAliases;

    /**
     * Constructs a {@link MappedAliasKeyManager}.
     * @param keyManager the {@link X509KeyManager}.
     * @param hostAliases the host aliases mapping.
     */
    MappedAliasKeyManager(final X509KeyManager keyManager, final Map<String, String> hostAliases) {
        this.keyManager = keyManager;
        this.hostAliases = hostAliases;
    }
    
    @Override
    public String[] getClientAliases(final String string, final Principal[] prncpls) {
        return keyManager.getClientAliases(string, prncpls);
    }

    /*
     * Choose an alias to authenticate the client side of a secure
     * socket based upon the configured hostname to alias mapping.
     * If no mapping is specified for the hostname fallthrough to the default
     * X509KeyManager.
     */
    @Override
    public String chooseClientAlias(final String[] keyType, final Principal[] issuers, final Socket socket) {
        InetSocketAddress socketAddress = (InetSocketAddress)socket.getRemoteSocketAddress();
        String hostName = socketAddress.getHostString().toUpperCase();
        return (hostAliases.containsKey(hostName)) ? 
                hostAliases.get(hostName) : keyManager.chooseClientAlias(keyType, issuers, socket);
    }

    @Override
    public String[] getServerAliases(final String string, final Principal[] prncpls) {
        return keyManager.getServerAliases(string, prncpls);
    }

    @Override
    public String chooseServerAlias(final String string, final Principal[] prncpls, final Socket socket) {
        return keyManager.chooseServerAlias(string, prncpls, socket);
    }

    @Override
    public X509Certificate[] getCertificateChain(final String string) {
        return keyManager.getCertificateChain(string);
    }

    @Override
    public PrivateKey getPrivateKey(final String string) {
        return keyManager.getPrivateKey(string);
    }
    
}
