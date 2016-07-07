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

import static org.forgerock.guava.common.collect.FluentIterable.from;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.guava.common.base.Function;
import org.forgerock.guava.common.base.Predicate;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.messaging.jms.JmsMessageSubscriber;
import org.forgerock.script.ScriptRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MessagingService manages MessageSubscribers to allow IDM to subscribe to incoming message events.
 * <br/>
 * The 'messaging.json' file configures the list of subscribers.
 * <br/>
 * The enum factory pattern is used in lieu of using reflection to instantiate instances of subscribers and handlers
 * from config.
 * <br/>
 * The SubscriberFactory enum, defined below, represents the possible types of subscribers.  Any new Subscriber
 * implementations will need to be added to this enum.
 * <br/>
 * The HandlerFactory enum, also defined below, represents the possible types of handlers.  Any new Handler
 * implementations will need to be added to this enum.
 * <br/>
 * Each individual instance of a Subscriber is expected to manage a single connection between IDM and the Messaging
 * event channel.  Increase the instanceCount in the configuration to increase message consumption throughput.
 * <br/>
 * Each subscriber has an instance of a message handler.  The handler is expected to process the message and take all
 * actions that the message represents.
 * <br/>
 * Here is a sample config for the MessagingService:
 * <pre>
 * {
 *     "subscribers": [
 *         {
 *             "name": "IDM CREST Queue Subscriber",
 *             "instanceCount": 10,
 *             "enabled": true,
 *             "type": "JMS",
 *             "handler": {
 *                 "type": "SCRIPTED",
 *                 "properties": {
 *                     "script": {
 *                         "type": "text\/javascript",
 *                         "file": "crudpaqTextMessageHandler.js"
 *                     }
 *                 }
 *             },
 *             "properties": {
 *                 "sessionMode": "CLIENT",
 *                 "jndi": {
 *                     "contextProperties": {
 *                         "java.naming.factory.initial": "org.apache.activemq.jndi.ActiveMQInitialContextFactory",
 *                         "java.naming.provider.url": "tcp:\/\/127.0.0.1:61616?daemon=true",
 *                         "queue.idmQ": "idmQ"
 *                     },
 *                     "destinationName": "idmQ",
 *                     "connectionFactoryName": "ConnectionFactory"
 *                 }
 *             }
 *         }
 *     ]
 * }
 * </pre>
 */
@Component(name = MessagingService.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Service({MessagingService.class})
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Messaging Service")
})
public class MessagingService {
    private static final Logger logger = LoggerFactory.getLogger(MessagingService.class);

    public static final String PID = "org.forgerock.openidm.messaging";
    private static final String NAME = "name";
    private static final String HANDLER = "handler";
    private static final String TYPE = "type";
    private static final String PROPS = "properties";
    private static final String INSTANCE_COUNT = "instanceCount";
    private static final String SUBSCRIBERS = "subscribers";
    private static final String ENABLED = "enabled";
    private static final String SUBSCRIBER_TYPE_JMS = "JMS";
    private static final String HANDLER_TYPE_SCRIPTED = "SCRIPTED";

    private final List<MessageSubscriber<?>> subscribers = new ArrayList<>();
    private JsonValue config;

    /**
     * Enhanced configuration service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected volatile EnhancedConfig enhancedConfig;

    /**
     * Script Registry service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected volatile ScriptRegistry scriptRegistry;

    /**
     * A {@link Predicate} that returns whether the subscriber is enabled
     */
    private static final Predicate<JsonValue> enabledMessengers =
            new Predicate<JsonValue>() {
                @Override
                public boolean apply(JsonValue jsonValue) {
                    return jsonValue.get(ENABLED).defaultTo(true).asBoolean();
                }
            };

    /**
     * Function that constructs a MessageHandler from the provided json configuration.
     */
    private final Function<JsonValue, MessageHandler<?>> withMessageHandler
            = new Function<JsonValue, MessageHandler<?>>() {
        @Override
        public MessageHandler<?> apply(JsonValue handlerConfig) {
            String handlerType = handlerConfig.get(TYPE).asString();
            if (HANDLER_TYPE_SCRIPTED.equals(handlerType)) {
                return new ScriptedMessageHandler<>(handlerConfig.get(PROPS), scriptRegistry);
            } else {
                // as soon as more handler types are needed, a better factory pattern will be needed.
                throw new InvalidException("Unknown handler of type=" + handlerType);
            }
        }
    };

    /**
     * Provided the json configuration for the subscribers, this will construct the subscribers and start their
     * subscription using the configured handler.
     */
    private final Function<JsonValue, List<MessageSubscriber<?>>> toSubscribedMessageSubscribers =
            new Function<JsonValue, List<MessageSubscriber<?>>>() {
                @SuppressWarnings("unchecked")
                @Override
                public List<MessageSubscriber<?>> apply(final JsonValue subscribersJson) {
                    final List<MessageSubscriber<?>> subscribers = new ArrayList<>();
                    final String namePrefix =
                            subscribersJson.get(NAME).required().asString().replaceAll("[^0-9A-Za-z]", "_");
                    final int instanceCount = subscribersJson.get(INSTANCE_COUNT).defaultTo(1).asInteger();
                    final JsonValue subscriberProperties = subscribersJson.get(PROPS);
                    final JsonValue handlerConfig = subscribersJson.get(HANDLER).required();

                    String subscriberType = subscribersJson.get(TYPE).required().asString();
                    if (!SUBSCRIBER_TYPE_JMS.equals(subscriberType)) {
                        // as soon as more subscriber types are needed, a better factory pattern will be needed to
                        // create subscribers.
                        throw new InvalidException("Unknown subscriber of type="+subscriberType);
                    }

                    // Repeated for the iterationCount, create a MessageSubscriber and start the subscription.
                    for (int i = 0; i < instanceCount; i++) {
                        // Construct an instance of the subscriber to start subscribing.
                        String instanceName = namePrefix + "#" + i;
                        MessageSubscriber subscriber = new JmsMessageSubscriber(instanceName, subscriberProperties);
                        try {
                            subscriber.subscribe(withMessageHandler.apply(handlerConfig));
                            subscribers.add(subscriber);
                        } catch (ResourceException e) {
                            throw new InvalidException("Failure subscribing MessageSubscriber", e);
                        }
                    }
                    return subscribers;
                }
            };

    /**
     * Reads the messaging.json config file to activate all configured subscribers.
     *
     * @param context OSGi context.
     */
    @Activate
    public void activate(ComponentContext context) {
        config = enhancedConfig.getConfigurationAsJson(context);
        for (final List<MessageSubscriber<?>> messageSubscribers :
                from(config.get(SUBSCRIBERS).expect(List.class))
                        .filter(enabledMessengers)
                        .transform(toSubscribedMessageSubscribers)) {
            subscribers.addAll(messageSubscribers);
        }
        logger.debug("OpenIDM MessagingService activated with {} activated subscribers.", subscribers.size());
    }

    /**
     * Implemented to unsubscribe on all subscribers that were activated.
     *
     * @param context The ComponentContext.
     */
    @Deactivate
    public void deactivate(ComponentContext context) {
        logger.debug("OpenIDM MessagingService deactivating.");
        for (MessageSubscriber<?> subscriber : subscribers) {
            try {
                subscriber.unsubscribe();
                logger.debug("MessageSubscriber {} has unsubscribed.", subscriber.getName());
            } catch (Exception e) {
                logger.warn("Failure to unsubscribe MessageSubscriber {}.", subscriber.getName(), subscriber, e);
            }
        }
        subscribers.clear();
        config = null;
    }
}
