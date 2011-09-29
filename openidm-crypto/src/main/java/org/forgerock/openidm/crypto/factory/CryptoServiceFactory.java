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

package org.forgerock.openidm.crypto.factory;

import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.impl.CryptoServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cryptography Service Factory
 * The cryptography service can be obtained either though this factory, or the OSGi registry.
 * This avoids bootstrap order issues with decrypting configuration.
 * @author aegloff
 */
public class CryptoServiceFactory {

    private final static Logger LOGGER = LoggerFactory.getLogger(CryptoServiceFactory.class);

    /**
     * A cryptography service instance
     */
    private static CryptoServiceImpl instance;
    
    /**
     * Get a cryptography service instance
     * The cryptography service can be obtained either through this factory, 
     * or preferrably the OSGi service registry. 
     * This avoids bootstrap order issues with decrypting configuration.
     * @return a cryptography service instance
     */
    public static synchronized CryptoService getInstance() {
        if (instance == null) {
            instance = new CryptoServiceImpl();
            instance.activate(null);
        }
        return instance;
    }
}
