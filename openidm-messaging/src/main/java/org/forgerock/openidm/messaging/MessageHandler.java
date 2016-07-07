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
 * Each MessageSubscriber has a single MessageHandler for it to call to handle any received message.
 *
 * @param <T> The type of message object to expect to handle.
 */
public interface MessageHandler<T> {

    /**
     * The implementor is expected to parse the message and execute logic that the message is requesting or represents.
     * Any problem dealing with the message should result in a ResourceException so that the Subscriber can determine
     * what to do with the message.
     *
     * @param message the message received by the subscriber.
     * @throws ResourceException
     * @see MessageSubscriber
     */
    void handleMessage(T message) throws ResourceException;
}
