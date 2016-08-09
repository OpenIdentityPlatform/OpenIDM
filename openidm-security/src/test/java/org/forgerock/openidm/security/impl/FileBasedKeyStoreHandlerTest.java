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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;

import org.forgerock.openidm.security.KeyStoreHandler;
import org.forgerock.security.keystore.KeyStoreType;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class FileBasedKeyStoreHandlerTest {

    private static final String KEY_STORE_PASSWORD = "Passw0rd1";

    @DataProvider
    private Object[][] fileBasedKeyStores() {
        return new Object[][] {
                {KeyStoreType.JKS, "/keystore.jks"},
                {KeyStoreType.JCEKS, "/keystore.jceks"},
                {KeyStoreType.PKCS12, "/keystore.pfx"}
        };
    }

    @Test
    public void shouldInitializeKeyStoreHandler(final KeyStoreType keyStoreType, final String location)
            throws Exception {
        // given
        final String absoluteFileName =
                Paths.get(getClass().getResource(location).toURI()).toFile().getAbsolutePath();

        // when
        final KeyStoreHandler keyStoreHandler =
                new FileBasedKeyStoreHandler(keyStoreType, absoluteFileName, KEY_STORE_PASSWORD);

        // then
        assertThat(keyStoreHandler.getStore()).isNotNull();
        assertThat(keyStoreHandler.getType().name()).isEqualTo(keyStoreType.name());
        assertThat(keyStoreHandler.getLocation()).isEqualTo(location);
        assertThat(keyStoreHandler.getPassword()).isEqualTo(KEY_STORE_PASSWORD);
    }

    @Test
    public void shouldStoreTheKeyStore(final KeyStoreType keyStoreType, final String location) throws Exception {
        // given
        final String absoluteFileName =
                Paths.get(getClass().getResource(location).toURI()).toFile().getAbsolutePath();
        final KeyStoreHandler keyStoreHandler =
                new FileBasedKeyStoreHandler(keyStoreType, absoluteFileName, KEY_STORE_PASSWORD);

        // when
        keyStoreHandler.store();

        // then
        assertThat(keyStoreHandler.getStore()).isNotNull();
        assertThat(keyStoreHandler.getType().name()).isEqualTo(keyStoreType.name());
        assertThat(keyStoreHandler.getLocation()).isEqualTo(location);
        assertThat(keyStoreHandler.getPassword()).isEqualTo(KEY_STORE_PASSWORD);
    }

    @Test
    public void shouldSetTheKeyStore(final KeyStoreType keyStoreType, final String location) throws Exception {
        // given
        final String absoluteFileName =
                Paths.get(getClass().getResource(location).toURI()).toFile().getAbsolutePath();
        final KeyStoreHandler keyStoreHandler =
                new FileBasedKeyStoreHandler(keyStoreType, absoluteFileName, KEY_STORE_PASSWORD);

        // when
        assertThat(keyStoreHandler.getStore()).isNotNull();
        keyStoreHandler.setStore(null);

        // then
        assertThat(keyStoreHandler.getStore()).isNull();
        assertThat(keyStoreHandler.getType().name()).isEqualTo(keyStoreType.name());
        assertThat(keyStoreHandler.getLocation()).isEqualTo(location);
        assertThat(keyStoreHandler.getPassword()).isEqualTo(KEY_STORE_PASSWORD);
    }
}
