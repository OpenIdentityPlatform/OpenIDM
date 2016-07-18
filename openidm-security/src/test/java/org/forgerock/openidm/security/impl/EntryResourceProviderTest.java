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
import static org.assertj.core.util.Files.temporaryFolder;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.PatchOperation.add;
import static org.forgerock.json.resource.Requests.newActionRequest;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.Requests.newDeleteRequest;
import static org.forgerock.json.resource.Requests.newPatchRequest;
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.json.resource.Requests.newUpdateRequest;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyRep;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
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
import org.forgerock.openidm.crypto.KeyRepresentation;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.security.KeyStoreHandler;
import org.forgerock.openidm.security.KeyStoreManager;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class EntryResourceProviderTest {

    private static final String KEY_STORE_TYPE = "JCEKS";
    private static final String KEY_STORE_PASSWORD = "password";
    private static final String ENTRY_ID = "entry";
    private static final String RESOURCE_CONTAINER = "test";
    private static final String[] EXPECTED_KEYS = {"_id", "privateKey"};
    private static final JsonValue EMPTY_JSON_OBJECT = json(object());
    private static final String CLASS_NAME = EntryResourceProvider.class.getCanonicalName();

    @BeforeClass
    public void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testCreatingEntry() throws Exception {
        // given
        final File keystoreFile = createTemporaryKeyStore();
        final EntryResourceProvider entryResourceProvider =
                new TestEntryResourceProvider(
                        RESOURCE_CONTAINER,
                        new JcaKeyStoreHandler(KEY_STORE_TYPE, keystoreFile.getAbsolutePath(), KEY_STORE_PASSWORD),
                        new TestKeyStoreManager(),
                        new TestRepositoryService());

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
        final File keystoreFile = createTemporaryKeyStore();
        final EntryResourceProvider entryResourceProvider =
                new TestEntryResourceProvider(
                        RESOURCE_CONTAINER,
                        new JcaKeyStoreHandler(KEY_STORE_TYPE, keystoreFile.getAbsolutePath(), KEY_STORE_PASSWORD),
                        new TestKeyStoreManager(),
                        new TestRepositoryService());

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
        final File keystoreFile = createTemporaryKeyStore();
        final EntryResourceProvider entryResourceProvider =
                new TestEntryResourceProvider(
                        RESOURCE_CONTAINER,
                        new JcaKeyStoreHandler(KEY_STORE_TYPE, keystoreFile.getAbsolutePath(), KEY_STORE_PASSWORD),
                        new TestKeyStoreManager(),
                        new TestRepositoryService());

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
        final File keystoreFile = createTemporaryKeyStore();
        final EntryResourceProvider entryResourceProvider =
                new TestEntryResourceProvider(
                        RESOURCE_CONTAINER,
                        new JcaKeyStoreHandler(KEY_STORE_TYPE, keystoreFile.getAbsolutePath(), KEY_STORE_PASSWORD),
                        new TestKeyStoreManager(),
                        new TestRepositoryService());

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
        final File keystoreFile = createTemporaryKeyStore();
        final EntryResourceProvider entryResourceProvider =
                new TestEntryResourceProvider(
                        RESOURCE_CONTAINER,
                        new JcaKeyStoreHandler(KEY_STORE_TYPE, keystoreFile.getAbsolutePath(), KEY_STORE_PASSWORD),
                        new TestKeyStoreManager(),
                        new TestRepositoryService());

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                entryResourceProvider.patchInstance(
                        new RootContext(), ENTRY_ID, newPatchRequest(RESOURCE_CONTAINER, add("/test", "test")));

        // then
        assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testActionCollection() throws Exception {
        // given
        final File keystoreFile = createTemporaryKeyStore();
        final EntryResourceProvider entryResourceProvider =
                new TestEntryResourceProvider(
                        RESOURCE_CONTAINER,
                        new JcaKeyStoreHandler(KEY_STORE_TYPE, keystoreFile.getAbsolutePath(), KEY_STORE_PASSWORD),
                        new TestKeyStoreManager(),
                        new TestRepositoryService());

        // when
        final Promise<ActionResponse, ResourceException> promise =
                entryResourceProvider.actionCollection(
                        new RootContext(), newActionRequest(RESOURCE_CONTAINER, "actionId"));

        // then
        assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testActionInstance() throws Exception {
        // given
        final File keystoreFile = createTemporaryKeyStore();
        final EntryResourceProvider entryResourceProvider =
                new TestEntryResourceProvider(
                        RESOURCE_CONTAINER,
                        new JcaKeyStoreHandler(KEY_STORE_TYPE, keystoreFile.getAbsolutePath(), KEY_STORE_PASSWORD),
                        new TestKeyStoreManager(),
                        new TestRepositoryService());

        // when
        final Promise<ActionResponse, ResourceException> promise =
                entryResourceProvider.actionInstance(
                        new RootContext(), ENTRY_ID, newActionRequest(RESOURCE_CONTAINER, "actionId"));

        // then
        assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testQueryCollection() throws Exception {
        // given
        final File keystoreFile = createTemporaryKeyStore();
        final EntryResourceProvider entryResourceProvider =
                new TestEntryResourceProvider(
                        RESOURCE_CONTAINER,
                        new JcaKeyStoreHandler(KEY_STORE_TYPE, keystoreFile.getAbsolutePath(), KEY_STORE_PASSWORD),
                        new TestKeyStoreManager(),
                        new TestRepositoryService());

        // when
        final Promise<QueryResponse, ResourceException> promise =
                entryResourceProvider.queryCollection(
                        new RootContext(),
                        Requests.newQueryRequest(RESOURCE_CONTAINER),
                        new NullQueryResourceHandler());

        // then
        assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void attemptToCreateAliasThatAlreadyExists() throws Exception {
        // given
        final File keystoreFile = createTemporaryKeyStore();
        final EntryResourceProvider entryResourceProvider =
                new TestEntryResourceProvider(
                        RESOURCE_CONTAINER,
                        new JcaKeyStoreHandler(KEY_STORE_TYPE, keystoreFile.getAbsolutePath(), KEY_STORE_PASSWORD),
                        new TestKeyStoreManager(),
                        new TestRepositoryService());

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
        assertThat(promise).failedWithException().isInstanceOf(ConflictException.class);
    }

    @Test
    public void attemptToCreateWithNoResourceId() throws Exception {
        // given
        final File keystoreFile = createTemporaryKeyStore();
        final EntryResourceProvider entryResourceProvider =
                new TestEntryResourceProvider(
                        RESOURCE_CONTAINER,
                        new JcaKeyStoreHandler(KEY_STORE_TYPE, keystoreFile.getAbsolutePath(), KEY_STORE_PASSWORD),
                        new TestKeyStoreManager(),
                        new TestRepositoryService());

        // when
        Promise<ResourceResponse, ResourceException> promise =
                entryResourceProvider.createInstance(
                        new RootContext(), newCreateRequest(RESOURCE_CONTAINER, EMPTY_JSON_OBJECT));

        // then
        assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void attemptToReadEntryThatDoesNotExist() throws Exception {
        // given
        final File keystoreFile = createTemporaryKeyStore();
        final EntryResourceProvider entryResourceProvider =
                new TestEntryResourceProvider(
                        RESOURCE_CONTAINER,
                        new JcaKeyStoreHandler(KEY_STORE_TYPE, keystoreFile.getAbsolutePath(), KEY_STORE_PASSWORD),
                        new TestKeyStoreManager(),
                        new TestRepositoryService());

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                entryResourceProvider.readInstance(
                        new RootContext(), ENTRY_ID, newReadRequest(RESOURCE_CONTAINER, ENTRY_ID));

        // then
        assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
    }

    @Test
    public void attemptToDeleteEntryThatDoesNotExist() throws Exception {
        // given
        final File keystoreFile = createTemporaryKeyStore();
        final EntryResourceProvider entryResourceProvider =
                new TestEntryResourceProvider(
                        RESOURCE_CONTAINER,
                        new JcaKeyStoreHandler(KEY_STORE_TYPE, keystoreFile.getAbsolutePath(), KEY_STORE_PASSWORD),
                        new TestKeyStoreManager(),
                        new TestRepositoryService());

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                entryResourceProvider.deleteInstance(
                        new RootContext(), ENTRY_ID, newDeleteRequest(RESOURCE_CONTAINER, ENTRY_ID));

        // then
        assertThat(promise).failedWithException().isInstanceOf(NotFoundException.class);
    }


    private File createTemporaryKeyStore()
            throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        final File keystoreFile = File.createTempFile(CLASS_NAME, null, temporaryFolder());
        keystoreFile.deleteOnExit();
        KeyStore ks = KeyStore.getInstance(KEY_STORE_TYPE);

        char[] password = KEY_STORE_PASSWORD.toCharArray();
        ks.load(null, password);

        FileOutputStream fos = new FileOutputStream(keystoreFile);
        ks.store(fos, password);
        fos.close();

        return keystoreFile;
    }


    private final class NullQueryResourceHandler implements QueryResourceHandler {

        @Override
        public boolean handleResource(ResourceResponse resource) {
            return true;
        }
    }

    private final class TestEntryResourceProvider extends EntryResourceProvider {

        public TestEntryResourceProvider(
                String resourceName, KeyStoreHandler store, KeyStoreManager manager, RepositoryService repoService) {
            super(resourceName, store, manager, repoService);
        }

        @Override
        public void createDefaultEntry(String alias) throws Exception {
            Pair<X509Certificate, PrivateKey> pair = generateCertificate("localhost",
                    "OpenIDM Self-Signed Certificate", "None", "None", "None", "None",
                    DEFAULT_ALGORITHM, DEFAULT_KEY_SIZE, DEFAULT_SIGNATURE_ALGORITHM, null, null);
            Certificate cert = pair.getKey();
            PrivateKey key = pair.getValue();
            store.getStore().setEntry(alias, new KeyStore.PrivateKeyEntry(key, new Certificate[]{cert}),
                    new KeyStore.PasswordProtection(store.getPassword().toCharArray()));
            store.store();
        }

        @Override
        protected void storeEntry(JsonValue value, String alias) throws Exception {
            Pair<X509Certificate, PrivateKey> pair = generateCertificate("localhost",
                    "OpenIDM Self-Signed Certificate", "None", "None", "None", "None",
                    DEFAULT_ALGORITHM, DEFAULT_KEY_SIZE, DEFAULT_SIGNATURE_ALGORITHM, null, null);
            Certificate cert = pair.getKey();
            PrivateKey key = pair.getValue();
            store.getStore().setEntry(alias, new KeyStore.PrivateKeyEntry(key, new Certificate[]{cert}),
                    new KeyStore.PasswordProtection(store.getPassword().toCharArray()));
            store.store();
        }

        @Override
        protected JsonValue readEntry(String alias) throws Exception {
            Key key = store.getStore().getKey(alias, store.getPassword().toCharArray());
            if (key == null) {
                throw new NotFoundException("Alias does not correspond to a key entry in " + resourceName);
            } else {
                return KeyRepresentation.toJsonValue(alias, key);
            }
        }
    }

    private final class TestKeyStoreManager implements KeyStoreManager {

        @Override
        public void reload() throws Exception {
            // Do Nothing
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
