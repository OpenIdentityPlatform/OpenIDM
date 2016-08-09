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

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;

import org.forgerock.openidm.security.KeyStoreHandler;
import org.forgerock.security.keystore.KeyStoreBuilder;
import org.forgerock.security.keystore.KeyStoreType;
import org.forgerock.util.Reject;

/**
 * Handles file based key stores.
 */
public class FileBasedKeyStoreHandler implements KeyStoreHandler {
    private final String location;
    private final String password;
    private final KeyStoreType type;
    private KeyStore store;

    /**
     * Constructs a file based {@link KeyStoreHandler}.
     * @param type the {@link KeyStoreType}.
     * @param location the keystore location.
     * @param password the keystore password.
     * @throws Exception if unable to create the {@link KeyStoreHandler}.
     */
    public FileBasedKeyStoreHandler(final KeyStoreType type, final String location, final String password)
            throws Exception {
        this.location = location;
        this.password = password;
        this.type = type;
        this.store = new KeyStoreBuilder()
                .withKeyStoreType(type)
                .withPassword(password)
                .withKeyStoreFile(location)
                .build();
    }

    @Override
    public KeyStore getStore() {
        return store;
    }

    @Override
    public void setStore(KeyStore keystore) throws Exception {
        store = Reject.checkNotNull(keystore);
        store();
    }

    @Override
    public void store() throws Exception {
        try (final OutputStream out = new FileOutputStream(location)) {
            store.store(out, password.toCharArray());
        }
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public KeyStoreType getType() {
        return type;
    }
}
