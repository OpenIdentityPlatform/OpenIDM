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
package org.forgerock.openidm.messaging;

import static org.forgerock.json.JsonValue.json;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.matches;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.util.Hashtable;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.services.context.Context;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests that the MessagingService can be activated with a its config file and that a mocked JMS connection
 * can get loaded via JNDI and a successful message can be sent.
 */
public class MessagingServiceTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private JsonValue testConfig;

    private static ConnectionFactory connectionFactory;
    private static Destination destination;

    @BeforeClass
    public void setUp() throws Exception {
        testConfig = json(
                OBJECT_MAPPER.readValue(getClass().getResource("/config/messaging.json"), Map.class));
        connectionFactory = mock(ConnectionFactory.class);
        destination = mock(Destination.class);
    }

    @Test
    public void testActivateWithSentMessage() throws Exception {
        final Connection connection = mock(Connection.class);
        Session session = mock(Session.class);
        final MessageConsumer messageConsumer = new MockedMessageConsumer();
        when(session.createConsumer(any(Destination.class), anyString())).thenReturn(messageConsumer);
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
        when(connectionFactory.createConnection()).thenReturn(connection);

        final JSONEnhancedConfig jsonEnhancedConfig = mock(JSONEnhancedConfig.class);
        final ScriptRegistry scriptRegistry = mock(ScriptRegistry.class);
        final ScriptEntry scriptEntry = mock(ScriptEntry.class);
        doReturn(Boolean.TRUE).when(scriptEntry).isActive();
        when(scriptEntry.getScript(any(Context.class))).thenReturn(mock(Script.class));

        MessagingService messagingService = new MessagingService();
        messagingService.bindEnhancedConfig(jsonEnhancedConfig);
        messagingService.bindScriptRegistry(scriptRegistry);

        when(jsonEnhancedConfig.getConfigurationAsJson(any(ComponentContext.class))).thenReturn(testConfig);
        when(scriptRegistry.takeScript(any(JsonValue.class))).thenReturn(scriptEntry);

        TextMessage message = mock(TextMessage.class);
        when(message.getText()).thenReturn("This is a test");
        when(message.getJMSMessageID()).thenReturn("testMessage");

        messagingService.activate(mock(ComponentContext.class));

        // Now that the service is activated, lets send a test message and make sure it gets acknowledged.
        MessageListener messageListener = messageConsumer.getMessageListener();
        messageListener.onMessage(message);
        verify(message, times(1)).acknowledge();
    }

    /**
     * A context factory for testing the Messaging service.  Static class so that it can be instantiated via the JNDI
     * context loader.
     */
    public static class TestContextFactory implements InitialContextFactory {

        @Override
        public javax.naming.Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {

            try {
                javax.naming.Context context = mock(javax.naming.Context.class);
                when(context.lookup(matches("TestFactory"))).thenReturn(connectionFactory);
                when(context.lookup(matches("testQ"))).thenReturn(destination);
                return context;
            } catch (Exception e) {
                throw new IllegalStateException("trouble setting up testing initial context", e);
            }
        }
    }

    /**
     * Mocked MessageConsumer to allow mocking the setMessageListener() method which returns void.
     */
    public class MockedMessageConsumer implements MessageConsumer {
        private MessageListener listener;

        @Override
        public String getMessageSelector() throws JMSException {
            return null;
        }

        @Override
        public MessageListener getMessageListener() throws JMSException {
            return listener;
        }

        @Override
        public void setMessageListener(MessageListener listener) throws JMSException {
            this.listener = listener;
        }

        @Override
        public Message receive() throws JMSException {
            return null;
        }

        @Override
        public Message receive(long timeout) throws JMSException {
            return null;
        }

        @Override
        public Message receiveNoWait() throws JMSException {
            return null;
        }

        @Override
        public void close() throws JMSException {

        }
    }

}
