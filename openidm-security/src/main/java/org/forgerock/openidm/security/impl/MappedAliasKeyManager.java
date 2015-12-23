/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.security.impl;

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
public class MappedAliasKeyManager implements X509KeyManager {
    
    private final X509KeyManager keyManager;
    private final Map<String,String> hostAliases;

    public MappedAliasKeyManager(X509KeyManager keyManager, Map<String, String> hostAliases) {
        this.keyManager = keyManager;
        this.hostAliases = hostAliases;
    }
    
    @Override
    public String[] getClientAliases(String string, Principal[] prncpls) {
        return keyManager.getClientAliases(string, prncpls);
    }

    /*
     * Choose an alias to authenticate the client side of a secure
     * socket based upon the configured hostname to alias mapping.
     * If no mapping is specified for the hostname fallthrough to the default
     * X509KeyManager.
     */
    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        InetSocketAddress socketAddress = (InetSocketAddress)socket.getRemoteSocketAddress();
        String hostName = socketAddress.getHostString().toUpperCase();
        return (hostAliases.containsKey(hostName)) ? 
                hostAliases.get(hostName) : keyManager.chooseClientAlias(keyType, issuers, socket);
    }

    @Override
    public String[] getServerAliases(String string, Principal[] prncpls) {
        return keyManager.getServerAliases(string, prncpls);
    }

    @Override
    public String chooseServerAlias(String string, Principal[] prncpls, Socket socket) {
        return keyManager.chooseServerAlias(string, prncpls, socket);
    }

    @Override
    public X509Certificate[] getCertificateChain(String string) {
        return keyManager.getCertificateChain(string);
    }

    @Override
    public PrivateKey getPrivateKey(String string) {
        return keyManager.getPrivateKey(string);
    }
    
}
