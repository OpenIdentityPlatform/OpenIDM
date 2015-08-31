/*
 * Copyright 2013-2015 ForgeRock, AS.
 *
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
 */
package org.forgerock.openidm.provisioner.openicf.syncfailure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.script.ScriptRegistry;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Config tests for SyncFailureHandlerFactoryImpl factor method.
 *
 */
public class SyncFailureHandlerFactoryImplTest {

    private ObjectMapper mapper;

    private SyncFailureHandlerFactory factory;

    @BeforeTest
    public void setUp() throws Exception {
        mapper = new ObjectMapper();
        factory = createInitialFactory();
        Field bind = SyncFailureHandlerFactoryImpl.class.getDeclaredField("scriptRegistry");
        if (null != bind) {
            bind.setAccessible(true);
            bind.set(factory, mock(ScriptRegistry.class));
        }
    }

    private SyncFailureHandlerFactory createInitialFactory() {
        return new SyncFailureHandlerFactoryImpl();
    }

    private JsonValue parseJsonString(String json) throws Exception {
        return new JsonValue((Map) mapper.readValue(json, Map.class));
    }

    private int getRetries(SyncFailureHandler handler) throws Exception {
        Field bind = SimpleRetrySyncFailureHandler.class.getDeclaredField("syncFailureRetries");
        if (null != bind) {
            bind.setAccessible(true);
            return bind.getInt(handler);
        }
        return -1;
    }

    private Object getPostRetryHandler(SyncFailureHandler handler) throws Exception {
        Field bind = SimpleRetrySyncFailureHandler.class.getDeclaredField("postRetryHandler");
        if (null != bind) {
            bind.setAccessible(true);
            return bind.get(handler);
        }
        return null;
    }

    @Test
    public void testCreateNoConfig() throws Exception {
        SyncFailureHandler handler = factory.create(null);
        assertThat(handler).isInstanceOf(InfiniteRetrySyncFailureHandler.class);
    }

    @Test
    public void testCreateNullConfig() throws Exception {
        SyncFailureHandler handler = factory.create(new JsonValue(null));
        assertThat(handler).isInstanceOf(InfiniteRetrySyncFailureHandler.class);
    }

    @Test
    public void testCreateEmptyConfig() throws Exception {
        SyncFailureHandler handler = factory.create(new JsonValue(new HashMap<String,Object>()));
        assertThat(handler).isInstanceOf(InfiniteRetrySyncFailureHandler.class);
    }

    @Test
    public void testCreateInfiniteRetry() throws Exception {
        JsonValue config = parseJsonString("{\"maxRetries\":-1}");
        SyncFailureHandler handler = factory.create(config);
        assertThat(handler).isInstanceOf(InfiniteRetrySyncFailureHandler.class);
    }

    @Test
    public void testCreateLoggedIgnore() throws Exception {
        JsonValue config = parseJsonString("{\"maxRetries\":0,\"postRetryAction\":\"logged-ignore\"}");
        SyncFailureHandler handler = factory.create(config);
        assertThat(handler).isInstanceOf(LoggedIgnoreHandler.class);
    }

    @Test
    public void testCreateDeadLetterQueue() throws Exception {
        JsonValue config = parseJsonString("{\"maxRetries\":0,\"postRetryAction\":\"dead-letter-queue\"}");
        SyncFailureHandler handler = factory.create(config);
        assertThat(handler).isInstanceOf(DeadLetterQueueHandler.class);
    }

    @Test
    public void testCreateScript() throws Exception {
        JsonValue config = parseJsonString("{\"maxRetries\":0,\"postRetryAction\":{\"script\":{\"type\":\"text/javascript\",\"file\":\"script/onSyncFailure.js\"}}}");
        SyncFailureHandler handler = factory.create(config);
        assertThat(handler).isInstanceOf(ScriptedSyncFailureHandler.class);
    }

    @Test
    public void testCreateRetryOnly() throws Exception {
        JsonValue config = parseJsonString("{\"maxRetries\":3}");
        SyncFailureHandler handler = factory.create(config);
        assertThat(handler).isInstanceOf(SimpleRetrySyncFailureHandler.class);
        assertThat(3).isEqualTo(getRetries(handler));
        assertThat(getPostRetryHandler(handler)).isInstanceOf(NullSyncFailureHandler.class);
    }

    @Test
    public void testCreateRetryWithDeadLetterQueue() throws Exception {
        JsonValue config = parseJsonString("{\"maxRetries\":4,\"postRetryAction\":\"dead-letter-queue\"}");
        SyncFailureHandler handler = factory.create(config);
        assertThat(handler).isInstanceOf(SimpleRetrySyncFailureHandler.class);
        assertThat(4).isEqualTo(getRetries(handler));
        assertThat(getPostRetryHandler(handler)).isInstanceOf(DeadLetterQueueHandler.class);
    }

    @Test
    public void testCreateRetryWithScript() throws Exception {
        JsonValue config = parseJsonString("{\"maxRetries\":5,\"postRetryAction\":{\"script\":{\"type\":\"text/javascript\",\"file\":\"script/onSyncFailure.js\"}}}");
        SyncFailureHandler handler = factory.create(config);
        assertThat(handler).isInstanceOf(SimpleRetrySyncFailureHandler.class);
        assertThat(5).isEqualTo(getRetries(handler));
        assertThat(getPostRetryHandler(handler)).isInstanceOf(ScriptedSyncFailureHandler.class);
    }
}
