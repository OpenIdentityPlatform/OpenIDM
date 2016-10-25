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

import static org.forgerock.openidm.core.IdentityServer.KEYSTORE_LOCATION;
import static org.forgerock.openidm.core.IdentityServer.KEYSTORE_TYPE;
import static org.forgerock.openidm.core.IdentityServer.TRUSTSTORE_LOCATION;
import static org.forgerock.openidm.core.IdentityServer.TRUSTSTORE_PASSWORD;
import static org.forgerock.openidm.core.IdentityServer.TRUSTSTORE_TYPE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;

import org.assertj.core.api.Assertions;
import org.forgerock.openidm.core.IdentityServer;

class SecurityTestUtils {

    private SecurityTestUtils() {
        // prevent initialization
    }

    /**
     * Creates a keystore and truststore given the configuration from the {@link IdentityServer}.
     * @throws Exception If unable to create the keystore or truststore.
     */
    static void createKeyStores() throws Exception {
        createKeyStore(IdentityServer.getFileForPath(IdentityServer.getInstance().getProperty(KEYSTORE_LOCATION)));
        createTrustStore(IdentityServer.getFileForPath(IdentityServer.getInstance().getProperty(TRUSTSTORE_LOCATION)));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void createKeyStore(final File keystoreFile) throws Exception {
        keystoreFile.deleteOnExit();
        if (keystoreFile.exists()) {
            keystoreFile.delete();
        }

        keystoreFile.getParentFile().mkdirs();
        Assertions.assertThat(keystoreFile.createNewFile()).isTrue().as("Unable to create keystore file");
        try (final OutputStream outputStream = new FileOutputStream(keystoreFile)) {
            final KeyStore keyStore =
                    KeyStore.getInstance(IdentityServer.getInstance().getProperty(KEYSTORE_TYPE));
            keyStore.load(null, IdentityServer.getInstance().getProperty(IdentityServer.KEYSTORE_PASSWORD).toCharArray());
            keyStore.store(
                    outputStream,
                    IdentityServer.getInstance().getProperty(IdentityServer.KEYSTORE_PASSWORD).toCharArray()
            );
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void createTrustStore(final File keystoreFile) throws Exception {
        keystoreFile.deleteOnExit();
        if (keystoreFile.exists()) {
            keystoreFile.delete();
        }

        keystoreFile.getParentFile().mkdirs();
        Assertions.assertThat(keystoreFile.createNewFile()).isTrue().as("Unable to create keystore file");
        try (final OutputStream outputStream = new FileOutputStream(keystoreFile)) {
            final KeyStore keyStore =
                    KeyStore.getInstance(IdentityServer.getInstance().getProperty(TRUSTSTORE_TYPE));
            keyStore.load(null, IdentityServer.getInstance().getProperty(TRUSTSTORE_PASSWORD).toCharArray());
            keyStore.store(
                    outputStream,
                    IdentityServer.getInstance().getProperty(TRUSTSTORE_PASSWORD).toCharArray()
            );
        }
    }
}
