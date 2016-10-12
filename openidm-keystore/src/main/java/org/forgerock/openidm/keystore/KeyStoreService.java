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
package org.forgerock.openidm.keystore;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * The interface tof a {@link KeyStoreService}.
 */
public interface KeyStoreService {

    /**
     * Gets the keystore this {@link KeyStoreService} manages.
     * @return the {@link KeyStore}.
     */
    KeyStore getKeyStore();

    /**
     * Gets the {@link KeyStoreDetails} for the managed {@link KeyStore} managed by the {@link KeyStoreService}.
     * @return the {@link KeyStoreDetails}.
     */
    KeyStoreDetails getKeyStoreDetails();

    /**
     * Stores the {@link KeyStore} object to disk or device.
     * @throws GeneralSecurityException if unable to store the {@link KeyStore}.
     */
    void store() throws GeneralSecurityException;
}
