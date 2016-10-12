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
package org.forgerock.openidm.keystore.impl;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import org.forgerock.openidm.keystore.KeyStoreDetails;

/**
 * Interface for loading and initializing the keystore and truststore.
 */
public interface KeyStoreInitializer {

    /**
     * Loads and initializes the keystore.
     * @param keyStoreDetails the {@link KeyStoreDetails} for the keystore to load and initialize.
     * @return the loaded and initialized {@link KeyStore}.
     * @throws GeneralSecurityException if unable to load or initialize the {@link KeyStore}.
     */
    KeyStore initializeKeyStore(final KeyStoreDetails keyStoreDetails) throws GeneralSecurityException;

    /**
     * Loads and initializes the truststore.
     * @param keyStore the keystore to store the ssl cert to.
     * @param keyStoreDetails the truststore {@link KeyStoreDetails truststore details}.
     * @return the loaded and initialized {@link KeyStore truststore}.
     * @throws GeneralSecurityException if unable to load or initialize the {@link KeyStore truststore}.
     */
    KeyStore initializeTrustStore(final KeyStore keyStore, final KeyStoreDetails keyStoreDetails)
            throws GeneralSecurityException;
}
