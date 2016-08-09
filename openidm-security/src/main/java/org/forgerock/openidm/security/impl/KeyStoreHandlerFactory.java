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
package org.forgerock.openidm.security.impl;

import org.forgerock.openidm.security.KeyStoreHandler;
import org.forgerock.security.keystore.KeyStoreType;

/**
 * Creates a {@link KeyStoreHandler} based on the {@link KeyStoreType}.
 */
public class KeyStoreHandlerFactory {

    /**
     * Gets a {@link KeyStoreHandler} given a {@link KeyStoreType}.
     * @param keyStoreType the {@link KeyStoreType}.
     * @param location the keystore location.
     * @param password the keystore password.
     * @return a {@link KeyStoreHandler}.
     * @throws Exception if unable to create a {@link KeyStoreHandler}.
     */
    public KeyStoreHandler getKeyStoreHandler(final KeyStoreType keyStoreType, final String location,
            final String password) throws Exception {
        switch (keyStoreType) {
        case PKCS11:
            return new PKCS11KeyStoreHandler(keyStoreType, password);
        case JKS:
        case JCEKS:
        case PKCS12:
        default:
            return new FileBasedKeyStoreHandler(keyStoreType, location, password);
        }
    }
}
