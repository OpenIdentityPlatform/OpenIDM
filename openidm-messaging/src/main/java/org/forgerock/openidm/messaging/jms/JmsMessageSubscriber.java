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
package org.forgerock.openidm.messaging.jms;

import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.messaging.MessageHandler;
import org.forgerock.openidm.messaging.MessageSubscriber;
import org.forgerock.openidm.messaging.jms.common.JndiConfiguration;
import org.forgerock.openidm.messaging.jms.common.JndiJmsContextManager;
import org.forgerock.openidm.messaging.jms.common.SessionModeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A MessageSubscriber that subscribes to JMS destinations.  JMS messages are acknowledged only if the handler doesn't
 * throw an exception.
 */
public class JmsMessageSubscriber extends MessageSubscriber<Message> {
    private static final Logger logger = LoggerFactory.getLogger(JmsMessageSubscriber.class);
    private final SessionModeConfig sessionMode;
    private final JndiConfiguration jndiConfiguration;
    private final String messageSelector;

    private Session session;
    private Connection connection;

    /**
     * Constructs a JMS Subscriber using the provided instance name and configuration.  The configuration is grabbed,
     * and isn't used to make connections until subscribe is called.
     *
     * @param name the name of this instance.
     * @param propertiesConfig the config to build and utilize for this instance.
     */
    public JmsMessageSubscriber(final String name, final JsonValue propertiesConfig) {
        super(name);
        sessionMode = SessionModeConfig.valueOf(propertiesConfig.get("sessionMode").required().asString());
        messageSelector = propertiesConfig.get("messageSelector").asString();
        jndiConfiguration = new JndiConfiguration(propertiesConfig.get("jndi").required());
    }

    /**
     * Implemented to subscribe on the JNDI configured JMS destination (queue or topic).  Implemented to use a single
     * connection and session.
     *
     * @param messageHandler an instance of a JMS message handler.
     */
    @Override
    public void subscribe(final MessageHandler<Message> messageHandler) throws ResourceException {
        JndiJmsContextManager contextManager;
        try {
            contextManager = new JndiJmsContextManager(jndiConfiguration);
        } catch (ResourceException e) {
            throw new InternalServerErrorException("Failure loading JNDI JMS Context configurations.", e);
        }



        try {
            if (null != connection || null != session) {
                // in case there exists an old connection or session, lets unsubscribe those before creating new ones.
                unsubscribe();
            }

            connection = contextManager.getConnectionFactory().createConnection();
            connection.setClientID(getName());
            connection.setExceptionListener(new SubscriptionExceptionListener(messageHandler));
            session = connection.createSession(false, sessionMode.getMode());
            session.createConsumer(contextManager.getDestination(), messageSelector)
                    .setMessageListener(new javax.jms.MessageListener() {
                        @Override
                        public void onMessage(Message message) {
                            String jmsMessageID = getMessageID(message);
                            try {
                                messageHandler.handleMessage(message);
                                try {
                                    logger.trace("JMS Message {} handled by {}", jmsMessageID, getName());
                                    message.acknowledge();
                                    logger.trace("JMS Message {} acknowledged by {}", jmsMessageID, getName());
                                } catch (JMSException e) {
                                    throw new InternalServerErrorException("Failure to acknowledge JMS message " +
                                            jmsMessageID, e);
                                }
                            } catch (Exception e) {
                                // if the handler throws an exception, the message won't be acknowledged.  This
                                // leaves the message available to pick up later, by this or another subscriber.
                                logger.error("Failure handling the JMS message {}.", jmsMessageID, e);
                            }
                        }
                    });
            connection.start();
            logger.debug("JMSMessageSubscriber {} is subscribed", getName());
        } catch (Exception e) {
            logger.error("Failure to create JMS subscription", e);
            unsubscribe();
            throw new InvalidException("Failure to create JMS subscription", e);
        }
    }

    private String getMessageID(Message message) {
        String jmsMessageID = "unknown";
        try {
            jmsMessageID = message.getJMSMessageID();
        } catch (JMSException e) {
            logger.error("Failure extracting the JMS Message ID", e);
        }
        return jmsMessageID;
    }

    /**
     * Implemented to close the JMS session and connection associated with this instance.
     */
    @Override
    public void unsubscribe() {
        if (null != session) {
            try {
                session.close();
                session = null;
            } catch (JMSException e) {
                logger.error("Failure to close JMS session", e);
            }
        }
        if (null != connection) {
            try {
                connection.close();
                connection = null;
            } catch (JMSException e) {
                logger.error("Failure to close JMS connection", e);
            }
        }
    }

    /**
     * Exception handler for the JMS connection.
     */
    private class SubscriptionExceptionListener implements ExceptionListener {
        private final MessageHandler<Message> messageHandler;

        /**
         * Constructs the exception listener that will use the messagehandler to re-subscribe/re-connect once the
         * failure is logged.
         *
         * @param messageHandler the handler to use to re-subscribe.
         */
        public SubscriptionExceptionListener(MessageHandler<Message> messageHandler) {
            this.messageHandler = messageHandler;
        }

        /**
         * Handles the JMSException that might get thrown while connected to the JMS broker.
         *
         * @param exception The exception that was thrown.
         */
        @Override
        public void onException(JMSException exception) {
            // This is called when a exception occurs with the long running connection.
            logger.warn("JMS Connection exception occurred, {} will unsubscribe, and attempt reconnect.", getName(),
                    exception);
            unsubscribe();
            try {
                subscribe(messageHandler);
            } catch (ResourceException e) {
                if (null != connection) {
                    try {
                        connection.close();
                    } catch (JMSException je) {
                        logger.error("Failure closing jms connection after reconnect attempt.", je);
                    }
                }
                logger.error("Failure to re-create JMS connection", e);
                throw new InvalidException("Failure to re-create JMS connection", e);
            }
        }
    }
}
