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
package org.forgerock.openidm.managed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Requests.*;
import static org.forgerock.json.resource.ResourceResponse.*;
import static org.forgerock.json.resource.Resources.newInternalConnectionFactory;
import static org.forgerock.json.resource.Router.uriTemplate;
import static org.forgerock.openidm.managed.ManagedObjectSet.Action.triggerSyncCheck;
import static org.forgerock.openidm.managed.ManagedObjectSet.CRYPTO_KEY_PTR;
import static org.forgerock.util.Utils.closeSilently;
import static org.mockito.Mockito.mock;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.SecretKeyEntry;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonTransformer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.crypto.JsonCryptoTransformer;
import org.forgerock.json.crypto.simple.SimpleDecryptor;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.util.NullActivityLogger;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.impl.CryptoServiceImpl;
import org.forgerock.openidm.crypto.impl.UpdatableKeyStoreSelector;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.router.IDMConnectionFactoryWrapper;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;
import org.testng.annotations.Test;

/**
 * Tests for {@link ManagedObjectSet}
 */
public class ManagedObjectSetTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_PASSWORD = "password";
    private static final String FIELD_EMAIL = "email";
    private static final String MANAGED_USER_RESOURCE_PATH = "managed/user";
    private static final String REPO_MANAGED_USER_RESOURCE_PATH = "/repo/managed/user";
    private static final String ALIAS = "alias";
    private static final String ALIAS1 = "alias1";
    private static final String CONF_MANAGED_USER_USING_ALIAS = "/conf/managed-user-alias.json";
    private static final String CONF_MANAGED_USER_USING_ALIAS1 = "/conf/managed-user-alias1.json";
    private static final String CONF_MANAGED_USER_USING_NO_ENCRYPTION = "/conf/managed-user-no-encryption.json";
    private static final String RESOURCE_ID = "user1";
    private static final String KEYSTORE_PASSWORD = "Password1";
    private static final int NUMBER_OF_USERS = 5;

    @Test
    public void testTriggerSyncCheckOnActionCollection() throws Exception {
        // given
        final CryptoService cryptoService = createCryptoService();
        final ConnectionObjects connectionObjects = createConnectionObjects();
        final ManagedObjectSet managedObjectSet =
                createManagedObjectSet(CONF_MANAGED_USER_USING_ALIAS, cryptoService,
                        connectionObjects.getConnectionFactory());
        addRoutesToRouter(connectionObjects.getRouter(), managedObjectSet, new MemoryBackend());

        // create users
        createUsers(NUMBER_OF_USERS, managedObjectSet);

        // when triggerSync on all users.
        ActionRequest actionRequest = newActionRequest(MANAGED_USER_RESOURCE_PATH, triggerSyncCheck.name());
        ActionResponse actionResponse = managedObjectSet.actionCollection(new RootContext(), actionRequest)
                .getOrThrowUninterruptibly();

        // then
        assertThat(actionResponse.getJsonContent().get(ManagedObjectSet.COUNT_TRIGGERED).asInteger())
                .isEqualTo(NUMBER_OF_USERS);
    }

    @Test
    public void testTriggerSyncCheckOnActionCollectionWithQuery() throws Exception {
        // given
        final CryptoService cryptoService = createCryptoService();
        final ConnectionObjects connectionObjects = createConnectionObjects();
        final ManagedObjectSet managedObjectSet =
                createManagedObjectSet(CONF_MANAGED_USER_USING_ALIAS, cryptoService,
                        connectionObjects.getConnectionFactory());
        addRoutesToRouter(connectionObjects.getRouter(), managedObjectSet, new MemoryBackend());

        // create users
        createUsers(NUMBER_OF_USERS, managedObjectSet);

        // when triggerSync on queried users.
        ActionRequest actionRequest = newActionRequest(MANAGED_USER_RESOURCE_PATH, triggerSyncCheck.name())
                .setAdditionalParameter(QueryConstants.QUERY_FILTER, FIELD_USERNAME+" sw \"0_\"");
        ActionResponse actionResponse = managedObjectSet.actionCollection(new RootContext(), actionRequest)
                .getOrThrowUninterruptibly();

        // then
        JsonValue responseContent = actionResponse.getJsonContent();
        assertThat(responseContent.get(ManagedObjectSet.COUNT_TRIGGERED).asInteger()).isEqualTo(1);
        assertThat(responseContent.get(ManagedObjectSet.STATUS).asString()).isEqualTo("OK");
    }


    @Test
    public void testUpdateWithNoChanges() throws Exception {
        // given
        final CryptoService cryptoService = createCryptoService();
        final ConnectionObjects connectionObjects = createConnectionObjects();
        final ManagedObjectSet managedObjectSet =
                createManagedObjectSet(CONF_MANAGED_USER_USING_ALIAS, cryptoService,
                        connectionObjects.getConnectionFactory());
        addRoutesToRouter(connectionObjects.getRouter(), managedObjectSet, new MemoryBackend());

        // create user
        final JsonValue userContent = createUserObject(RESOURCE_ID, "password", "user@forgerock.com");
        final JsonValue createdUser = createUser(RESOURCE_ID, userContent, managedObjectSet);

        // when update user - no changes
        final UpdateRequest updateRequest = newUpdateRequest(MANAGED_USER_RESOURCE_PATH, RESOURCE_ID, userContent);
        updateRequest.setRevision(createdUser.get(FIELD_REVISION).asString());
        JsonValue updatedUser = managedObjectSet.updateInstance(new RootContext(), RESOURCE_ID, updateRequest)
                .getOrThrow().getContent();

        // then
        assertThat(updatedUser.isEqualTo(createdUser)).isTrue();
        assertThat(cryptoService.isEncrypted(updatedUser.get(FIELD_PASSWORD))).isTrue();
    }

    @Test
    public void testUpdateWithChanges() throws Exception {
        // given
        final CryptoService cryptoService = createCryptoService();
        final ConnectionObjects connectionObjects = createConnectionObjects();
        final ManagedObjectSet managedObjectSet =
                createManagedObjectSet(CONF_MANAGED_USER_USING_ALIAS, cryptoService,
                        connectionObjects.getConnectionFactory());
        addRoutesToRouter(connectionObjects.getRouter(), managedObjectSet, new MemoryBackend());

        // create user
        JsonValue userContent = createUserObject(RESOURCE_ID, "password1", "user@forgerock.com");
        final JsonValue createdUser = createUser(RESOURCE_ID, userContent, managedObjectSet);

        // when update user
        userContent = createUserObject(RESOURCE_ID, "password2", "user@forgerock.com");
        final UpdateRequest updateRequest = newUpdateRequest(MANAGED_USER_RESOURCE_PATH, RESOURCE_ID, userContent);
        JsonValue updatedUser = managedObjectSet.updateInstance(new RootContext(), RESOURCE_ID, updateRequest)
                .getOrThrow().getContent();

        // then
        assertThat(cryptoService.isEncrypted(updatedUser.get(FIELD_PASSWORD))).isTrue();
        assertThat(updatedUser.isEqualTo(createdUser)).isFalse();
    }

    @Test
    public void testUpdateWithPasswordAliasChanged() throws Exception {
        // given
        final CryptoService cryptoService = createCryptoService();
        final ConnectionObjects connectionObjects = createConnectionObjects();
        ManagedObjectSet managedObjectSet =
                createManagedObjectSet(CONF_MANAGED_USER_USING_ALIAS, cryptoService,
                        connectionObjects.getConnectionFactory());
        MemoryBackend memoryBackend = new MemoryBackend();
        addRoutesToRouter(connectionObjects.getRouter(), managedObjectSet, memoryBackend);

        // create user
        JsonValue userContent = createUserObject(RESOURCE_ID, "password1", "user@forgerock.com");
        final JsonValue createdUser = createUser(RESOURCE_ID, userContent, managedObjectSet);
        assertThat(getKeyAlias(createdUser)).isEqualTo(ALIAS);

        // reconfigure managed object set with a new config changing the password encryption key alias
        managedObjectSet = createManagedObjectSet(CONF_MANAGED_USER_USING_ALIAS1, cryptoService,
                connectionObjects.getConnectionFactory());
        addRoutesToRouter(connectionObjects.getRouter(), managedObjectSet, memoryBackend);

        // update user which will use password encryption key with alias alias1
        userContent = createUserObject(RESOURCE_ID, "password1", "user@forgerock.com");
        final UpdateRequest updateRequest = newUpdateRequest(MANAGED_USER_RESOURCE_PATH, RESOURCE_ID, userContent);
        JsonValue updatedUser = managedObjectSet.updateInstance(new RootContext(), RESOURCE_ID, updateRequest)
                .getOrThrow().getContent();

        // then
        assertThat(cryptoService.isEncrypted(updatedUser.get(FIELD_PASSWORD))).isTrue();
        assertThat(updatedUser.isEqualTo(createdUser)).isFalse();
    }

    @Test
    public void testUpdateWithPasswordBeingNewlyEncrypted() throws Exception {
        // given
        final CryptoService cryptoService = createCryptoService();
        final ConnectionObjects connectionObjects = createConnectionObjects();
        ManagedObjectSet managedObjectSet =
                createManagedObjectSet(CONF_MANAGED_USER_USING_NO_ENCRYPTION, cryptoService,
                        connectionObjects.getConnectionFactory());
        MemoryBackend backend = new MemoryBackend();
        addRoutesToRouter(connectionObjects.getRouter(), managedObjectSet, backend);

        // create user
        JsonValue userContent = createUserObject(RESOURCE_ID, "password1", "user@forgerock.com");
        final JsonValue createdUser = createUser(RESOURCE_ID, userContent, managedObjectSet);
        assertThat(getKeyAlias(createdUser)).isNull();
        assertThat(cryptoService.isEncrypted(createdUser.get(FIELD_PASSWORD))).isFalse();

        // reconfigure managed object set with a new config changing the password encryption key alias
        managedObjectSet = createManagedObjectSet(CONF_MANAGED_USER_USING_ALIAS, cryptoService,
                connectionObjects.getConnectionFactory());
        addRoutesToRouter(connectionObjects.getRouter(), managedObjectSet, backend);

        // update user which will use password encryption key with alias alias1
        userContent = createUserObject(RESOURCE_ID, "password1", "user@forgerock.com");
        final UpdateRequest updateRequest = newUpdateRequest(MANAGED_USER_RESOURCE_PATH, RESOURCE_ID, userContent);
        JsonValue updatedUser = managedObjectSet.updateInstance(new RootContext(), RESOURCE_ID, updateRequest)
                .getOrThrow().getContent();

        // then
        assertThat(cryptoService.isEncrypted(updatedUser.get(FIELD_PASSWORD))).isTrue();
        assertThat(getKeyAlias(updatedUser)).isEqualTo(ALIAS);
        assertThat(updatedUser.isEqualTo(createdUser)).isFalse();
    }

    @Test
    public void testUpdateWhenRemovingEncryptionFromPassword() throws Exception {
        // given
        final CryptoService cryptoService = createCryptoService();
        final ConnectionObjects connectionObjects = createConnectionObjects();
        ManagedObjectSet managedObjectSet =
                createManagedObjectSet(CONF_MANAGED_USER_USING_ALIAS, cryptoService,
                        connectionObjects.getConnectionFactory());
        MemoryBackend backend = new MemoryBackend();
        addRoutesToRouter(connectionObjects.getRouter(), managedObjectSet, backend);

        // create user
        JsonValue userContent = createUserObject(RESOURCE_ID, "password1", "user@forgerock.com");
        final JsonValue createdUser = createUser(RESOURCE_ID, userContent, managedObjectSet);
        assertThat(getKeyAlias(createdUser)).isEqualTo(ALIAS);
        assertThat(cryptoService.isEncrypted(createdUser.get(FIELD_PASSWORD))).isTrue();

        // reconfigure managed object set with a new config changing the password encryption key alias
        managedObjectSet = createManagedObjectSet(CONF_MANAGED_USER_USING_NO_ENCRYPTION, cryptoService,
                connectionObjects.getConnectionFactory());
        addRoutesToRouter(connectionObjects.getRouter(), managedObjectSet, backend);

        // update user which will use password encryption key with alias alias1
        userContent = createUserObject(RESOURCE_ID, "password1", "user@forgerock.com");
        final UpdateRequest updateRequest = newUpdateRequest(MANAGED_USER_RESOURCE_PATH, RESOURCE_ID, userContent);
        JsonValue updatedUser = managedObjectSet.updateInstance(new RootContext(), RESOURCE_ID, updateRequest)
                .getOrThrow().getContent();

        // then
        assertThat(getKeyAlias(updatedUser)).isNull();
        assertThat(cryptoService.isEncrypted(updatedUser.get(FIELD_PASSWORD))).isFalse();
        assertThat(updatedUser.isEqualTo(createdUser)).isFalse();
    }

    /**
     * Create a number of users with generated random content.
     *
     * @param numberOfUsers The number of users.
     * @param managedObjectSet The {@link ManagedObjectSet} to create the users for.
     * @return A list of users as {@link JsonValue}'s.
     * @throws ResourceException If unable to create the users.
     */
    private List<JsonValue> createUsers(final int numberOfUsers, final ManagedObjectSet managedObjectSet)
            throws ResourceException {
        final List<JsonValue> resources = new LinkedList<>();
        for (int i = 0; i < numberOfUsers; i++) {
            JsonValue user = createUserObject(i + "_" + UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                    UUID.randomUUID().toString());
            resources.add(createUser(user.get(FIELD_ID).asString(), user, managedObjectSet));
        }
        return resources;
    }

    private CryptoService createCryptoService() throws Exception {
        final KeyStore keyStore = createKeyStore();
        final UpdatableKeyStoreSelector keySelector = new UpdatableKeyStoreSelector(keyStore, KEYSTORE_PASSWORD);
        return new CryptoServiceImpl(keySelector,
                Collections.<JsonTransformer>singletonList(
                        new JsonCryptoTransformer(new SimpleDecryptor(keySelector))));
    }

    private KeyStore createKeyStore() throws Exception {
        final String KEYSTORE = "keystore";
        final String KEYSTORE_TYPE = "JCEKS";
        final File keystoreFile = File.createTempFile(KEYSTORE, KEYSTORE_TYPE);
        final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        keyStore.load(null, KEYSTORE_PASSWORD.toCharArray());
        FileOutputStream fileOutputStream = new FileOutputStream(keystoreFile);
        try {
            SecretKeyEntry keyStoreEntry = new SecretKeyEntry(createSecretKey());
            PasswordProtection keyPassword = new PasswordProtection(KEYSTORE_PASSWORD.toCharArray());
            keyStore.setEntry(ALIAS, keyStoreEntry, keyPassword);
            keyStoreEntry = new SecretKeyEntry(createSecretKey());
            keyStore.setEntry(ALIAS1, keyStoreEntry, keyPassword);
            keyStore.store(fileOutputStream, KEYSTORE_PASSWORD.toCharArray());
        } finally {
            closeSilently(fileOutputStream);
        }
        return keyStore;
    }

    //
    private SecretKey createSecretKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        return keyGen.generateKey();
    }

    private String getKeyAlias(final JsonValue resource) {
        JsonValue jsonValue = resource.get(FIELD_PASSWORD).get(CRYPTO_KEY_PTR);
        return null != jsonValue
                ? jsonValue.asString()
                : null;
    }

    private JsonValue getResource(final String resourceFile) throws IOException {
        final InputStream resource = getClass().getResourceAsStream(resourceFile);
        try {
            return json(mapper.readValue(resource, Map.class));
        } finally {
            if (resource != null) {
                closeSilently(resource);
            }
        }
    }

    private void addRoutesToRouter(final Router router, final ManagedObjectSet managedObjectSet,
            final MemoryBackend backend) {
        router.addRoute(uriTemplate(REPO_MANAGED_USER_RESOURCE_PATH), backend);
        router.addRoute(uriTemplate(MANAGED_USER_RESOURCE_PATH), managedObjectSet);
    }

    @SuppressWarnings("unchecked")
    private ManagedObjectSet createManagedObjectSet(final String configJson, final CryptoService cryptoService,
            final IDMConnectionFactory connectionFactory) throws Exception {
        // given
        final ScriptRegistry scriptRegistry = mock(ScriptRegistry.class);
        final AtomicReference<RouteService> routeService = (AtomicReference<RouteService>) mock(AtomicReference.class);
        final JsonValue config = getResource(configJson);
        return new ManagedObjectSet(scriptRegistry, cryptoService, routeService, connectionFactory, config,
                new NullActivityLogger());
    }

    private JsonValue createUser(final String resourceId, final JsonValue userContent,
            final ManagedObjectSet managedObjectSet) throws ResourceException {
        Promise<ResourceResponse, ResourceException> promise = managedObjectSet.createInstance(new RootContext(),
                newCreateRequest(MANAGED_USER_RESOURCE_PATH, resourceId, userContent));
        return promise.getOrThrowUninterruptibly().getContent();
    }

    private ConnectionObjects createConnectionObjects() {
        final Router router = new Router();
        return new ConnectionObjects(new IDMConnectionFactoryWrapper(newInternalConnectionFactory(router)), router);
    }

    private JsonValue createUserObject(final String username, final String password, final String email) {
        return json(object(
                field(FIELD_USERNAME, username),
                field(FIELD_PASSWORD, password),
                field(FIELD_EMAIL, email)
        ));
    }

    @SuppressWarnings({"unchecked", "unused"})
    private <T> TestResultHandler<T> newTestResultHandler(Class<T> type) {
        return new TestResultHandler<>();
    }

    @SuppressWarnings("unused")
    private static class TestResultHandler<T> implements ResultHandler<T> {

        final List<ResourceException> errors = new LinkedList<>();
        final List<T> results = new LinkedList<>();

        public void handleError(ResourceException error) {
            errors.add(error);
        }

        @Override
        public void handleResult(T result) {
            results.add(result);
        }

        List<T> getResults() {
            return Collections.unmodifiableList(results);
        }

        List<ResourceException> getErrors() {
            return Collections.unmodifiableList(errors);
        }
    }

    private static class ConnectionObjects {
        private IDMConnectionFactory connectionFactory;
        private Router router;

        ConnectionObjects(final IDMConnectionFactory connectionFactory, final Router router) {
            this.connectionFactory = connectionFactory;
            this.router = router;
        }

        IDMConnectionFactory getConnectionFactory() {
            return connectionFactory;
        }

        Router getRouter() {
            return router;
        }
    }

}
