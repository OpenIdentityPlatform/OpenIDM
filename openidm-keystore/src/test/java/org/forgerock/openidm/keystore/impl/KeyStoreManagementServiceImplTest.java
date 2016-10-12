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

import static org.forgerock.openidm.core.ServerConstants.LAUNCHER_INSTALL_LOCATION;
import static org.forgerock.openidm.core.ServerConstants.LAUNCHER_PROJECT_LOCATION;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.keystore.KeyStoreDetails;
import org.forgerock.openidm.keystore.KeyStoreService;
import org.forgerock.security.keystore.KeyStoreType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class KeyStoreManagementServiceImplTest {

    @BeforeClass
    public void setUp() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        System.setProperty(LAUNCHER_PROJECT_LOCATION,
                Paths.get(getClass().getResource("/").toURI()).toFile().getAbsolutePath());
        System.setProperty(LAUNCHER_INSTALL_LOCATION,
                Paths.get(getClass().getResource("/").toURI()).toFile().getAbsolutePath());
        try {
            IdentityServer.initInstance(null);
        } catch (final IllegalStateException e) {
            // tried to reinitialize ignore
        }
    }

    @Test
    public void reloadSslContext() throws Exception {
        // given
        final KeyStoreService keyStoreService = mock(KeyStoreService.class);
        final KeyStoreService trustStoreService = mock(KeyStoreService.class);
        when(keyStoreService.getKeyStore()).thenReturn(createKeyStore());
        when(keyStoreService.getKeyStoreDetails())
                .thenReturn(new KeyStoreDetails(KeyStoreType.JCEKS, "SunJCE", "none", "changeit"));
        when(trustStoreService.getKeyStore()).thenReturn(createKeyStore());
        when(trustStoreService.getKeyStoreDetails())
                .thenReturn(new KeyStoreDetails(KeyStoreType.JKS, "SUN", "none", "changeit"));
        final KeyStoreManagementServiceImpl keyStoreManagementService = new KeyStoreManagementServiceImpl();
        keyStoreManagementService.bindKeyStore(keyStoreService);
        keyStoreManagementService.bindTrustStore(trustStoreService);

        // when
        keyStoreManagementService.reloadSslContext();

        // then
        // do nothing if it reached here test was a success.
    }

    private KeyStore createKeyStore() throws Exception {
        final KeyStore keyStore = KeyStore.getInstance("JCEKS");
        keyStore.load(null, "password".toCharArray());
        return keyStore;
    }
}
