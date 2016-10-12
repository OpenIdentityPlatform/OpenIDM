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

package org.forgerock.openidm.crypto.factory;

import java.security.GeneralSecurityException;

import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.impl.CryptoServiceImpl;
import org.forgerock.openidm.keystore.factory.KeyStoreServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cryptography Service Factory The cryptography service can be obtained either
 * though this factory, or the OSGi registry. This avoids bootstrap order issues
 * with decrypting configuration.
 */
public class CryptoServiceFactory {
    private static final Logger logger = LoggerFactory.getLogger(CryptoService.class);

    /**
     * A {@link CryptoServiceImpl} singleton instance.
     */
    private static CryptoServiceImpl instance;

    /**
     * Get a cryptography service singleton instance. The cryptography service can be
     * obtained either through this factory, or preferrably the OSGi service
     * registry. This avoids bootstrap order issues with decrypting
     * configuration.
     *
     * @return a cryptography service singleton instance.
     */
    public static synchronized CryptoService getInstance() throws GeneralSecurityException {
        if (instance == null) {
            try {
                instance = new CryptoServiceImpl();
                instance.bindKeyStoreService(KeyStoreServiceFactory.getInstance());
                instance.activate(null);
            } catch (GeneralSecurityException e) {
                logger.error("Unable to create crypto service instance", e);
                throw e;
            }
        }
        return instance;
    }
}
