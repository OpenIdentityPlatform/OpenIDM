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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.Provider;

import org.forgerock.security.keystore.KeyStoreType;
import org.forgerock.util.Reject;

/**
 * A class that holds the details for loading and accessing a {@link KeyStore}.
 */
public class KeyStoreDetails {

    private final KeyStoreType type;
    private final String provider;
    private final String filename;
    private final String password;

    /**
     * Constructs a {@link KeyStoreDetails} object.
     * @param type the {@link KeyStoreType}.
     * @param provider the keystore {@link Provider} string identifier.
     * @param filename the filename for where the keystore is located. "NONE" if the keystore type is PKCS11.
     * @param password the password to access the keystore.
     */
    public KeyStoreDetails(final KeyStoreType type, final String provider, final String filename,
            final String password) {
        this.type = Reject.checkNotNull(type);
        this.provider = provider;
        this.filename = filename;
        this.password = Reject.checkNotNull(password);
    }

    /**
     * Gets the {@link KeyStoreType}.
     * @return the {@link KeyStoreType}.
     */
    public KeyStoreType getType() {
        return type;
    }

    /**
     * Gets the keystore provider identifier.
     * @return the keystore provider identifier.
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Gets the keystore filename.
     * @return the keystore filename.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Gets the keystore password.
     * @return the keystore password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets an {@link OutputStream}to the location of the keystore. Null if the {@link KeyStoreType} is PKCS11.
     * The caller of this method is responsible for closing the {@link OutputStream}.
     * @return the {@link OutputStream} to the location of the keystore.
     * @throws FileNotFoundException
     */
    public OutputStream getOutputStream() throws FileNotFoundException {
        if (KeyStoreType.PKCS11.equals(type)) {
            return null;
        } else {
            return new FileOutputStream(filename);
        }
    }
}
