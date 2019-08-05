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

import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.openidm.core.ServerConstants.LAUNCHER_INSTALL_LOCATION;
import static org.forgerock.openidm.core.ServerConstants.LAUNCHER_PROJECT_LOCATION;
import static org.forgerock.openidm.security.impl.SecurityTestUtils.createKeyStores;
import static org.mockito.Mockito.mock;

import java.nio.file.Paths;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.crypto.impl.CryptoServiceImpl;
import org.forgerock.openidm.keystore.KeyStoreManagementService;
import org.forgerock.openidm.keystore.KeyStoreService;
import org.forgerock.openidm.keystore.impl.KeyStoreServiceImpl;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.test.assertj.AssertJPromiseAssert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class PrivateKeyResourceProviderTest {

    private static final String TEST_KEY_ALIAS = "testCert";

    @BeforeClass
    public void runInitalSetup() throws Exception {
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
    public void testReadPrivateKey() throws Exception {
        //given
        createKeyStores();
        final KeyStoreServiceImpl keyStoreService = new KeyStoreServiceImpl();
        keyStoreService.activate(null);
        final PrivateKeyResourceProvider keystoreResourceProvider = createPrivateKeyResourceProvider(keyStoreService);

        //when
        final Promise<ResourceResponse, ResourceException> result =
                keystoreResourceProvider.readInstance(new RootContext(), TEST_KEY_ALIAS, newReadRequest(""));

        // then
        AssertJPromiseAssert.assertThat(result).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test(expectedExceptions = NotSupportedException.class)
    public void testReadEntry() throws Exception {
        //given
        createKeyStores();
        final KeyStoreServiceImpl keyStoreService = new KeyStoreServiceImpl();
        keyStoreService.activate(null);
        final PrivateKeyResourceProvider keystoreResourceProvider = createPrivateKeyResourceProvider(keyStoreService);

        //when
        keystoreResourceProvider.readEntry(TEST_KEY_ALIAS);
    }

    private PrivateKeyResourceProvider createPrivateKeyResourceProvider(final KeyStoreService keyStoreService) {
        final KeyStoreManagementService keyStoreManagementService = mock(KeyStoreManagementService.class);
        final CryptoServiceImpl cryptoService = new CryptoServiceImpl();
        cryptoService.bindKeyStoreService(keyStoreService);
        cryptoService.activate(null);
        return new PrivateKeyResourceProvider("keystore", keyStoreService, new TestRepositoryService(), cryptoService,
                keyStoreManagementService);
    }
}
