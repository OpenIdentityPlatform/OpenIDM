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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.keystore.factory;

import java.security.GeneralSecurityException;

import org.forgerock.openidm.keystore.KeyStoreService;

import org.forgerock.openidm.keystore.impl.TrustStoreServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a {@link KeyStoreService} for the truststore.
 */
public class TrustStoreServiceFactory {
    private static final Logger logger = LoggerFactory.getLogger(TrustStoreServiceFactory.class);

    /**
     * The singleton instance of {@link TrustStoreServiceImpl}.
     */
    private static TrustStoreServiceImpl instance;

    /**
     * Gets a singleton instance of a {@link TrustStoreServiceImpl}.
     *
     * @return a {@link TrustStoreServiceImpl}.
     */
    public static synchronized KeyStoreService getInstance() throws GeneralSecurityException {
        if (instance == null) {
            try {
                instance = new TrustStoreServiceImpl();
                instance.bindKeyStore(KeyStoreServiceFactory.getInstance());
                instance.activate(null);
            } catch (final GeneralSecurityException e) {
                logger.error("Unable to create trust store service instance", e);
                throw e;
            }
        }
        return instance;
    }
}
