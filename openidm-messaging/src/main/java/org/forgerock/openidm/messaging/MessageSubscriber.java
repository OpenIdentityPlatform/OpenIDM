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

import org.forgerock.json.resource.ResourceException;

/**
 * A MessageSubscriber implements the logic needed to subscribe to an event channel.
 * It is expected to pass each received message to the handler.
 *
 * @param <T> The type of messages that this instance subscribes to.
 */
public abstract class MessageSubscriber<T> {
    private final String name;

    /**
     * Creates an instance with the provided name.
     *
     * @param name name of the instance, used to distinguish between
     */
    public MessageSubscriber(final String name) {
        this.name = name;
    }

    /**
     * Returns the name of the subscriber instance.
     *
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Implement this method to connect to the subscription resource.  Each message that is received is expected to
     * be passed to the handler for processing.
     *
     * @param withHandler the handler to use to process received messages.
     * @throws ResourceException
     */
    public abstract void subscribe(final MessageHandler<T> withHandler) throws ResourceException;

    /**
     * Implement this method to close and cleanup any resources opened by the subscribe call.
     */
    public abstract void unsubscribe();
}
