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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openidm.security.impl;

import static org.forgerock.json.resource.test.assertj.AssertJActionResponseAssert.assertThat;
import static org.forgerock.openidm.core.IdentityServer.*;
import static org.forgerock.openidm.core.ServerConstants.LAUNCHER_INSTALL_LOCATION;
import static org.forgerock.openidm.core.ServerConstants.LAUNCHER_PROJECT_LOCATION;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.test.assertj.AssertJActionResponseAssert;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.crypto.impl.CryptoServiceImpl;
import org.forgerock.openidm.keystore.KeyStoreManagementService;
import org.forgerock.openidm.keystore.KeyStoreService;
import org.forgerock.openidm.keystore.impl.KeyStoreServiceImpl;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class KeystoreResourceProviderTest {

    private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    private static final String END_CERT = "-----END CERTIFICATE-----";

    private final String KEYSTORE_PASSWORD = "changeit";
    private final String TEST_CERT_ALIAS = "testCert";

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
    public void testActionGenerateCertReturningPrivateKey() throws Exception {
        //given
        createKeyStores();
        final KeyStoreServiceImpl keyStoreService = new KeyStoreServiceImpl();
        keyStoreService.activate(null);
        final KeystoreResourceProvider keystoreResourceProvider = createKeyStoreResurceProvider(keyStoreService);
        ActionRequest actionRequest =
                Requests.newActionRequest("", KeystoreResourceProvider.ACTION_GENERATE_CERT);
        actionRequest.setContent(createGenerateCertActionContent(true));

        //when
        final Promise<ActionResponse, ResourceException> result =
                keystoreResourceProvider.actionInstance(new RootContext(), actionRequest);

        //then
        final JsonValue content = result.get().getJsonContent();
        AssertJActionResponseAssert.assertThat(result).succeeded();
        AssertJJsonValueAssert.assertThat(content).hasObject("privateKey");
        checkResultForRequiredFields(content);
        checkKeyStoreEntry(content, keyStoreService);
    }

    @Test
    public void testActionGenerateCertNotReturningPrivateKey() throws Exception {
        //given
        createKeyStores();
        final KeyStoreServiceImpl keyStoreService = new KeyStoreServiceImpl();
        keyStoreService.activate(null);
        final KeystoreResourceProvider keystoreResourceProvider = createKeyStoreResurceProvider(keyStoreService);
        ActionRequest actionRequest =
                Requests.newActionRequest("", KeystoreResourceProvider.ACTION_GENERATE_CERT);
        actionRequest.setContent(createGenerateCertActionContent(true));

        //when
        final Promise<ActionResponse, ResourceException> result =
                keystoreResourceProvider.actionInstance(new RootContext(), actionRequest);

        //then
        final JsonValue content = result.get().getJsonContent();
        AssertJActionResponseAssert.assertThat(result).succeeded();
        AssertJJsonValueAssert.assertThat(content).hasObject("privateKey");
        checkResultForRequiredFields(content);
        checkKeyStoreEntry(content, keyStoreService);
    }

    @Test
    public void testActionGenerateCertWithoutAlias() throws Exception {
        //given
        createKeyStores();
        final KeyStoreServiceImpl keyStoreService = new KeyStoreServiceImpl();
        keyStoreService.activate(null);
        final KeystoreResourceProvider keystoreResourceProvider = createKeyStoreResurceProvider(keyStoreService);
        ActionRequest actionRequest =
                Requests.newActionRequest("", KeystoreResourceProvider.ACTION_GENERATE_CERT);
        final JsonValue content = createGenerateCertActionContent(true);
        content.remove("alias");
        actionRequest.setContent(content);

        //when
        final Promise<ActionResponse, ResourceException> result =
                keystoreResourceProvider.actionInstance(new RootContext(), actionRequest);

        assertThat(result).failedWithException().isInstanceOf(ResourceException.class);
    }

    @Test
    public void testActionGenerateCertWithAliasAlreadyInUse() throws Exception {
        //given
        createKeyStores();
        final KeyStoreServiceImpl keyStoreService = new KeyStoreServiceImpl();
        keyStoreService.activate(null);
        final KeystoreResourceProvider keystoreResourceProvider = createKeyStoreResurceProvider(keyStoreService);
        ActionRequest actionRequest =
                Requests.newActionRequest("", KeystoreResourceProvider.ACTION_GENERATE_CERT);
        actionRequest.setContent(createGenerateCertActionContent(true));

        //when
        Promise<ActionResponse, ResourceException> result =
                keystoreResourceProvider.actionInstance(new RootContext(), actionRequest);
        assertThat(result).succeeded();

        result = keystoreResourceProvider.actionInstance(new RootContext(), actionRequest);
        assertThat(result).failedWithException().isInstanceOf(ResourceException.class);
    }

    private KeystoreResourceProvider createKeyStoreResurceProvider(final KeyStoreService keyStoreService)
            throws Exception {
        final KeyStoreManagementService keyStoreManagementService = mock(KeyStoreManagementService.class);
        final CryptoServiceImpl cryptoService = new CryptoServiceImpl();
        cryptoService.bindKeyStoreService(keyStoreService);
        cryptoService.activate(null);
        return new KeystoreResourceProvider("keystore", keyStoreService, new TestRepositoryService(), cryptoService,
                keyStoreManagementService);
    }

    private void createKeyStores() throws Exception {
        createKeyStore(IdentityServer.getFileForPath(IdentityServer.getInstance().getProperty(KEYSTORE_LOCATION)));
        createTrustStore(IdentityServer.getFileForPath(IdentityServer.getInstance().getProperty(TRUSTSTORE_LOCATION)));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createKeyStore(final File keystoreFile) throws Exception {
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
    private void createTrustStore(final File keystoreFile) throws Exception {
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

    private JsonValue createGenerateCertActionContent(final boolean returnPrivateKey) {
        final DateUtil dateUtil = DateUtil.getDateUtil();
        final Map<String,Object> content = new HashMap<>();
        content.put("alias", TEST_CERT_ALIAS);
        content.put("algorithm", KeystoreResourceProvider.DEFAULT_ALGORITHM);
        content.put("signatureAlgorithm", KeystoreResourceProvider.DEFAULT_SIGNATURE_ALGORITHM);
        content.put("keySize", KeystoreResourceProvider.DEFAULT_KEY_SIZE);
        content.put("domainName","domainName");
        content.put("validFrom", dateUtil.now());
        content.put("validTo", dateUtil.parseIfDate(dateUtil.currentDateTime().plusDays(1).toDate().toString()));
        content.put("returnPrivateKey", returnPrivateKey);
        return new JsonValue(content);
    }

    private void checkResultForRequiredFields(final JsonValue result) {
        AssertJJsonValueAssert.assertThat(result).isNotNull();
        AssertJJsonValueAssert.assertThat(result).hasString("_id");
        AssertJJsonValueAssert.assertThat(result).hasString("type");
        AssertJJsonValueAssert.assertThat(result).hasString("cert");
        AssertJJsonValueAssert.assertThat(result).hasObject("publicKey");
    }

    private void checkKeyStoreEntry(final JsonValue result, final KeyStoreService keyStoreService) throws Exception {
        final KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStoreService
                        .getKeyStore()
                        .getEntry(TEST_CERT_ALIAS, new KeyStore.PasswordProtection(KEYSTORE_PASSWORD.toCharArray()));
        Assertions.assertThat(privateKeyEntry).isNotNull();

        Certificate certificate = privateKeyEntry.getCertificate();
        Assertions.assertThat(certificate).isNotNull();

        final String certAsPEM = convertCertToPEM(certificate.getEncoded());
        Assertions.assertThat(certAsPEM)
                .isNotNull()
                .isNotEmpty()
                .isEqualToIgnoringWhitespace(replaceNewLines(result.get("cert").asString()));

        PrivateKey privateKey = privateKeyEntry.getPrivateKey();
        Assertions.assertThat(privateKey).isNotNull();
    }

    private String replaceNewLines(final String string) {
        return string.replaceAll("\n", "");
    }

    private String convertCertToPEM(final byte[] encodedCert) {
        final StringBuilder certAsPEM = new StringBuilder();
        certAsPEM.append(BEGIN_CERT)
                .append(Base64.encode(encodedCert))
                .append(END_CERT);
        return  certAsPEM.toString();
    }

    private final class TestRepositoryService implements RepositoryService {

        private final MemoryBackend repo = new MemoryBackend();

        @Override
        public ResourceResponse create(CreateRequest request) throws ResourceException {
            try {
                final Promise<ResourceResponse, ResourceException> promise =
                        repo.createInstance(new RootContext(), request);
                return promise.getOrThrow();
            } catch (InterruptedException e) {
                throw new InternalServerErrorException("Unable to create object in repo", e);
            }
        }

        @Override
        public ResourceResponse read(ReadRequest request) throws ResourceException {
            try {
                final Promise<ResourceResponse, ResourceException> promise =
                        repo.readInstance(new RootContext(), request.getResourcePath(), request);
                return promise.getOrThrow();
            } catch (InterruptedException e) {
                throw new InternalServerErrorException("Unable to read object in repo", e);
            }
        }

        @Override
        public ResourceResponse update(UpdateRequest request) throws ResourceException {
            try {
                final Promise<ResourceResponse, ResourceException> promise =
                        repo.updateInstance(new RootContext(), request.getResourcePath(), request);
                return promise.getOrThrow();
            } catch (InterruptedException e) {
                throw new InternalServerErrorException("Unable to update object in repo", e);
            }
        }

        @Override
        public ResourceResponse delete(DeleteRequest request) throws ResourceException {
            try {
                final Promise<ResourceResponse, ResourceException> promise =
                        repo.deleteInstance(new RootContext(), request.getResourcePath(), request);
                return promise.getOrThrow();
            } catch (InterruptedException e) {
                throw new InternalServerErrorException("Unable to delete object in repo", e);
            }
        }

        @Override
        public List<ResourceResponse> query(QueryRequest request) throws ResourceException {
            try {
                final List<ResourceResponse> resources = new LinkedList<>();
                final Promise<QueryResponse, ResourceException> promise =
                        repo.queryCollection(new RootContext(), request, new QueryResourceHandler() {
                            @Override
                            public boolean handleResource(ResourceResponse resource) {
                                resources.add(resource);
                                return true;
                            }
                        });
                promise.getOrThrow();
                return resources;
            } catch (InterruptedException e) {
                throw new InternalServerErrorException("Unable to query objects in repo", e);
            }
        }
    }
}
