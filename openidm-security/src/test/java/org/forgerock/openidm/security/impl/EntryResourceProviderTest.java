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
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.PatchOperation.add;
import static org.forgerock.json.resource.Requests.*;
import static org.forgerock.openidm.core.IdentityServer.*;
import static org.forgerock.openidm.core.IdentityServer.TRUSTSTORE_PASSWORD;
import static org.forgerock.openidm.core.ServerConstants.LAUNCHER_INSTALL_LOCATION;
import static org.forgerock.openidm.core.ServerConstants.LAUNCHER_PROJECT_LOCATION;
import static org.forgerock.openidm.util.CertUtil.generateCertificate;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.KeyRepresentation;
import org.forgerock.openidm.crypto.factory.CryptoServiceFactory;
import org.forgerock.openidm.crypto.impl.CryptoServiceImpl;
import org.forgerock.openidm.keystore.KeyStoreManagementService;
import org.forgerock.openidm.keystore.KeyStoreService;
import org.forgerock.openidm.keystore.factory.KeyStoreServiceFactory;
import org.forgerock.openidm.keystore.impl.KeyStoreServiceImpl;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.test.assertj.AssertJPromiseAssert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class EntryResourceProviderTest {

    private static final String KEY_STORE_PASSWORD = "password";
    private static final String ENTRY_ID = "entry";
    private static final String RESOURCE_CONTAINER = "test";
    private static final String[] EXPECTED_KEYS = {"_id", "privateKey"};
    private static final JsonValue EMPTY_JSON_OBJECT = json(object());
    private static final String CLASS_NAME = EntryResourceProvider.class.getCanonicalName();

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
    public void testCreatingEntry() throws Exception {
        // given
        final EntryResourceProvider entryResourceProvider = createEntryResourceProvider();

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                entryResourceProvider.createInstance(
                        new RootContext(), newCreateRequest(RESOURCE_CONTAINER, ENTRY_ID, EMPTY_JSON_OBJECT));

        // then
        final ResourceResponse resourceResponse = promise.get();
        assertThat(resourceResponse.getId()).isEqualTo(ENTRY_ID);
        assertThat(resourceResponse.getRevision()).isEqualTo(null);
        assertThat(resourceResponse.getContent().asMap()).isEqualTo(EMPTY_JSON_OBJECT.asMap());
    }

    @Test
    public void testReadEntry() throws Exception {
        // given
        final EntryResourceProvider entryResourceProvider = createEntryResourceProvider();

        Promise<ResourceResponse, ResourceException> promise =
                entryResourceProvider.createInstance(
                        new RootContext(), newCreateRequest(RESOURCE_CONTAINER, ENTRY_ID, EMPTY_JSON_OBJECT));
        promise.getOrThrow();

        // when
        promise = entryResourceProvider.readInstance(
                new RootContext(), ENTRY_ID, newReadRequest(RESOURCE_CONTAINER, ENTRY_ID));

        // then
        final ResourceResponse resourceResponse = promise.get();
        assertThat(resourceResponse.getId()).isEqualTo(ENTRY_ID);
        assertThat(resourceResponse.getRevision()).isEqualTo(null);
        assertThat(resourceResponse.getContent().asMap()).containsKeys(EXPECTED_KEYS);
    }

    @Test
    public void testDeleteEntry() throws Exception {
        // given
        final EntryResourceProvider entryResourceProvider = createEntryResourceProvider();

        Promise<ResourceResponse, ResourceException> promise =
                entryResourceProvider.createInstance(
                        new RootContext(), newCreateRequest(RESOURCE_CONTAINER, ENTRY_ID, EMPTY_JSON_OBJECT));
        promise.getOrThrow();

        // when
        promise = entryResourceProvider.deleteInstance(
                new RootContext(), ENTRY_ID, newDeleteRequest(RESOURCE_CONTAINER, ENTRY_ID));

        // then
        final ResourceResponse resourceResponse = promise.get();
        assertThat(resourceResponse.getId()).isEqualTo(ENTRY_ID);
        assertThat(resourceResponse.getRevision()).isEqualTo(null);
        assertThat(resourceResponse.getContent().asMap()).isEqualTo(EMPTY_JSON_OBJECT.asMap());
    }

    @Test
    public void testUpdateEntry() throws Exception {
        // given
        final EntryResourceProvider entryResourceProvider = createEntryResourceProvider();

        Promise<ResourceResponse, ResourceException> promise =
                entryResourceProvider.createInstance(
                        new RootContext(), newCreateRequest(RESOURCE_CONTAINER, ENTRY_ID, EMPTY_JSON_OBJECT));
        promise.getOrThrow();

        // when
        promise = entryResourceProvider.updateInstance(
                new RootContext(), ENTRY_ID, newUpdateRequest(RESOURCE_CONTAINER, ENTRY_ID, EMPTY_JSON_OBJECT));

        // then
        final ResourceResponse resourceResponse = promise.get();
        assertThat(resourceResponse.getId()).isEqualTo(ENTRY_ID);
        assertThat(resourceResponse.getRevision()).isEqualTo(null);
        assertThat(resourceResponse.getContent().asMap()).isEqualTo(EMPTY_JSON_OBJECT.asMap());
    }

    @Test
    public void testPatchEntry() throws Exception {
        // given
        final EntryResourceProvider entryResourceProvider = createEntryResourceProvider();

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                entryResourceProvider.patchInstance(
                        new RootContext(), ENTRY_ID, newPatchRequest(RESOURCE_CONTAINER, add("/test", "test")));

        // then
        AssertJPromiseAssert.assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testActionCollection() throws Exception {
        // given
        final EntryResourceProvider entryResourceProvider = createEntryResourceProvider();

        // when
        final Promise<ActionResponse, ResourceException> promise =
                entryResourceProvider.actionCollection(
                        new RootContext(), newActionRequest(RESOURCE_CONTAINER, "actionId"));

        // then
        AssertJPromiseAssert.assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testActionInstance() throws Exception {
        // given
        final KeyStoreService keyStoreService = KeyStoreServiceFactory.getInstance();
        final KeyStoreManagementService keyStoreManagementService = mock(KeyStoreManagementService.class);
        final CryptoService cryptoService = CryptoServiceFactory.getInstance();
        final EntryResourceProvider entryResourceProvider =
                new TestEntryResourceProvider(RESOURCE_CONTAINER, keyStoreService, keyStoreManagementService,
                        new TestRepositoryService(), cryptoService);

        // when
        final Promise<ActionResponse, ResourceException> promise =
                entryResourceProvider.actionInstance(
                        new RootContext(), ENTRY_ID, newActionRequest(RESOURCE_CONTAINER, "actionId"));

        // then
        AssertJPromiseAssert.assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testQueryCollection() throws Exception {
        // given
        final EntryResourceProvider entryResourceProvider = createEntryResourceProvider();

        // when
        final Promise<QueryResponse, ResourceException> promise =
                entryResourceProvider.queryCollection(
                        new RootContext(),
                        Requests.newQueryRequest(RESOURCE_CONTAINER),
                        new NullQueryResourceHandler());

        // then
        AssertJPromiseAssert.assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void attemptToCreateAliasThatAlreadyExists() throws Exception {
        // given
        final EntryResourceProvider entryResourceProvider = createEntryResourceProvider();

        // when
        Promise<ResourceResponse, ResourceException> promise =
                entryResourceProvider.createInstance(
                        new RootContext(), newCreateRequest(RESOURCE_CONTAINER, ENTRY_ID, EMPTY_JSON_OBJECT));

        final ResourceResponse resourceResponse = promise.get();
        assertThat(resourceResponse.getId()).isEqualTo(ENTRY_ID);
        assertThat(resourceResponse.getRevision()).isEqualTo(null);
        assertThat(resourceResponse.getContent().asMap()).isEqualTo(EMPTY_JSON_OBJECT.asMap());

        promise =
                entryResourceProvider.createInstance(
                        new RootContext(), newCreateRequest(RESOURCE_CONTAINER, ENTRY_ID, EMPTY_JSON_OBJECT));

        // then
        AssertJPromiseAssert.assertThat(promise).failedWithException().isInstanceOf(ConflictException.class);
    }

    @Test
    public void attemptToCreateWithNoResourceId() throws Exception {
        // given
        final EntryResourceProvider entryResourceProvider = createEntryResourceProvider();

        // when
        Promise<ResourceResponse, ResourceException> promise =
                entryResourceProvider.createInstance(
                        new RootContext(), newCreateRequest(RESOURCE_CONTAINER, EMPTY_JSON_OBJECT));

        // then
        AssertJPromiseAssert.assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void attemptToReadEntryThatDoesNotExist() throws Exception {
        // given
        final EntryResourceProvider entryResourceProvider = createEntryResourceProvider();

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                entryResourceProvider.readInstance(
                        new RootContext(), ENTRY_ID, newReadRequest(RESOURCE_CONTAINER, ENTRY_ID));

        // then
        AssertJPromiseAssert.assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
    }

    @Test
    public void attemptToDeleteEntryThatDoesNotExist() throws Exception {
        // given
        final EntryResourceProvider entryResourceProvider = createEntryResourceProvider();

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                entryResourceProvider.deleteInstance(
                        new RootContext(), ENTRY_ID, newDeleteRequest(RESOURCE_CONTAINER, ENTRY_ID));

        // then
        AssertJPromiseAssert.assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
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
        assertThat(keystoreFile.createNewFile()).isTrue().as("Unable to create keystore file");
        try (final OutputStream outputStream = new FileOutputStream(keystoreFile)) {
            final KeyStore keyStore =
                    KeyStore.getInstance(IdentityServer.getInstance().getProperty(KEYSTORE_TYPE));
            keyStore.load(null, IdentityServer.getInstance().getProperty(KEYSTORE_PASSWORD).toCharArray());
            keyStore.store(
                    outputStream,
                    IdentityServer.getInstance().getProperty(KEYSTORE_PASSWORD).toCharArray()
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
        assertThat(keystoreFile.createNewFile()).isTrue().as("Unable to create keystore file");
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

    private EntryResourceProvider createEntryResourceProvider() throws Exception {
        createKeyStores();
        final KeyStoreServiceImpl keyStoreService = new KeyStoreServiceImpl();
        keyStoreService.activate(null);
        final KeyStoreManagementService keyStoreManagementService = mock(KeyStoreManagementService.class);
        final CryptoServiceImpl cryptoService = new CryptoServiceImpl();
        cryptoService.bindKeyStoreService(keyStoreService);
        cryptoService.activate(null);
        return new TestEntryResourceProvider(RESOURCE_CONTAINER, keyStoreService, keyStoreManagementService,
                        new TestRepositoryService(), cryptoService);
    }

    private final class NullQueryResourceHandler implements QueryResourceHandler {

        @Override
        public boolean handleResource(ResourceResponse resource) {
            return true;
        }
    }

    private final class TestEntryResourceProvider extends EntryResourceProvider {

        public TestEntryResourceProvider(
                String resourceName, KeyStoreService keyStoreService, KeyStoreManagementService manager,
                RepositoryService repoService, CryptoService cryptoService) {
            super(resourceName, keyStoreService.getKeyStore(), keyStoreService, repoService, cryptoService, manager);
        }

        @Override
        protected void storeEntry(JsonValue value, String alias) throws Exception {
            Pair<X509Certificate, PrivateKey> pair = generateCertificate("localhost",
                    "OpenIDM Self-Signed Certificate", "None", "None", "None", "None",
                    DEFAULT_ALGORITHM, DEFAULT_KEY_SIZE, DEFAULT_SIGNATURE_ALGORITHM, null, null);
            Certificate cert = pair.getKey();
            PrivateKey key = pair.getValue();
            keyStoreService.getKeyStore().setEntry(alias, new KeyStore.PrivateKeyEntry(key, new Certificate[]{cert}),
                    new KeyStore.PasswordProtection(keyStoreService.getKeyStoreDetails().getPassword().toCharArray()));
            keyStoreService.store();
        }

        @Override
        protected JsonValue readEntry(String alias) throws Exception {
            Key key = keyStoreService.getKeyStore().getKey(alias,
                    keyStoreService.getKeyStoreDetails().getPassword().toCharArray());
            if (key == null) {
                throw new NotFoundException("Alias does not correspond to a key entry in " + resourceName);
            } else {
                return KeyRepresentation.toJsonValue(alias, key);
            }
        }
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
