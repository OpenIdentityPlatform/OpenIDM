/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.salesforce.internal.streaming;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.util.ResourceUtil;
import org.restlet.data.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A StreamingClient receives the Messages from Force.com Streaming API.
 *
 * TODO: Synchronize the deactivation and the operation to disconnect the
 * BayeuxClient
 *
 * @author Laszlo Hordos
 */
public class StreamingResourceProvider implements SingletonResourceProvider {

    /**
     * Setup logging for the {@link StreamingResourceProvider}.
     */
    final static Logger logger = LoggerFactory.getLogger(StreamingResourceProvider.class);

    // Supported character set: A-Z, a-z, 0-9, '.', '-' and '_'. First character
    // must be alpha numeric.
    private static final Pattern TOPIC_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9.-_]+$");

    // The long poll duration
    private static final int TIMEOUT = 120000;

    private final SalesforceConnection connection;

    private final MessageListener MESSAGE_LISTENER = new MessageListener() {
        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            if (message.isSuccessful()) {
                logger.debug("OK >> Received message on channel '{}': {}", channel.getId(), message
                        .getJSON());
            } else {
                logger.debug("ERR>> Received message on channel '{}': {}", channel.getId(), message
                        .getJSON());
            }

        }
    };

    private BayeuxClient client = null;

    public StreamingResourceProvider(final SalesforceConnection c) {
        connection = c;
    }

    public void deactivate() throws Exception {
        synchronized (this) {
            if (null != client) {
                // Disconnecting
                client.disconnect();
                client.waitFor(1000, BayeuxClient.State.DISCONNECTED);
                client = null;
            }
        }
    }

    enum Action {
        SUBSCRIBE, UNSUBSCRIBE;
    }

    @Override
    public void actionInstance(ServerContext serverContext, ActionRequest actionRequest,
            ResultHandler<JsonValue> resultHandler) {
        try {
            JsonValue params = new JsonValue(actionRequest.getAdditionalParameters());
            String topic = params.get("topic").required().asString();
            if (TOPIC_PATTERN.matcher(topic).matches()) {
                JsonValue result = new JsonValue(new HashMap<String, Object>());
                if (Action.SUBSCRIBE.name().equalsIgnoreCase(actionRequest.getAction())) {

                    subscribe(topic);
                    resultHandler.handleResult(result);
                } else if (Action.UNSUBSCRIBE.name().equalsIgnoreCase(actionRequest.getAction())) {

                    getClient().getChannel("/topic/" + topic).unsubscribe();
                    resultHandler.handleResult(result);
                } else {
                    resultHandler.handleError(new BadRequestException("Unsupported actionId"));
                }
            } else {
                resultHandler.handleError(new BadRequestException("Value of topic is not valid"));
            }
        } catch (Throwable t) {
            resultHandler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void patchInstance(ServerContext serverContext, PatchRequest patchRequest,
            ResultHandler<Resource> resourceResultHandler) {
        ResourceUtil.notSupportedOnInstance(patchRequest);
    }

    @Override
    public void readInstance(ServerContext serverContext, ReadRequest readRequest,
            ResultHandler<Resource> resourceResultHandler) {
        ResourceUtil.notSupportedOnInstance(readRequest);
    }

    @Override
    public void updateInstance(ServerContext serverContext, UpdateRequest updateRequest,
            ResultHandler<Resource> resourceResultHandler) {
        ResourceUtil.notSupportedOnInstance(updateRequest);
    }

    public void subscribe(String topic) throws Exception {
        BayeuxClient client = getClient();

        logger.debug("Waiting for handshake: {}", topic);
        waitForHandshake(client, 60 * 1000, 1000);

        logger.info("Subscribing to topic: {}", topic);
        client.getChannel("/topic/" + topic).subscribe(MESSAGE_LISTENER);

        logger.info("Waiting for streamed data from Force.com...");

    }

    private BayeuxClient getClient() throws Exception {
        synchronized (this) {
            if (null == client) {
                // Set up a Jetty HTTP client to use with CometD
                HttpClient httpClient = new HttpClient();
                httpClient.setConnectTimeout(TIMEOUT);
                httpClient.setTimeout(TIMEOUT);
                // httpClient.setIdleTimeout(5000);
                // httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
                httpClient.setMaxConnectionsPerAddress(32768);
                httpClient.start();

                Map<String, Object> options = new HashMap<String, Object>();
                options.put(ClientTransport.TIMEOUT_OPTION, TIMEOUT);
                // options.put(ClientTransport.INTERVAL_OPTION, ?);
                // options.put(ClientTransport.MAX_NETWORK_DELAY_OPTION,
                // TIMEOUT);

                // Adds the OAuth header in LongPollingTransport
                LongPollingTransport transport = new LongPollingTransport(options, httpClient) {
                    @Override
                    protected void customize(ContentExchange exchange) {
                        super.customize(exchange);
                        try {
                            exchange.addRequestHeader("Au" + "thorization", connection
                                    .getOAuthUser().getAuthorization());
                        } catch (ResourceException e) {
                            throw new IllegalArgumentException("Failed to Authenticate", e);
                        }
                    }
                };
                transport.setDebugEnabled(true);

                // Now set up the Bayeux client itself
                client =
                        new BayeuxClient(
                                new Reference(connection.getOAuthUser().getBaseReference(),
                                        "./cometd/" + "29.0"/*
                                                             * connection.
                                                             * getAPIVersion()
                                                             */).toUri().toString(), transport);
                client.setDebugEnabled(true);
                client.getChannel(Channel.META_CONNECT).addListener(MESSAGE_LISTENER);
                client.getChannel(Channel.META_HANDSHAKE).addListener(MESSAGE_LISTENER);
                client.handshake();
            }
        }
        return client;
    }

    protected void waitForHandshake(BayeuxClient client, long timeoutInMilliseconds,
            long intervalInMilliseconds) {
        if (client.waitFor(timeoutInMilliseconds, BayeuxClient.State.CONNECTING,
                BayeuxClient.State.CONNECTED, BayeuxClient.State.UNCONNECTED)) {
            return;
        }

        // long start = System.currentTimeMillis();
        // long end = start + timeoutInMilliseconds;
        // while (System.currentTimeMillis() < end) {
        // if (client.isHandshook())
        // return;
        // try {
        // Thread.sleep(intervalInMilliseconds);
        // } catch (InterruptedException e) {
        // throw new RuntimeException(e);
        // }
        // }
        throw new IllegalStateException("Client did not handshake with server");
    }

}
