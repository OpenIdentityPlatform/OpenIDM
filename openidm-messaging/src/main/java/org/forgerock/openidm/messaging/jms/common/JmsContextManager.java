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
package org.forgerock.openidm.messaging.jms.common;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.forgerock.json.resource.InternalServerErrorException;

/**
 * This defines the methods needed to work with a JMS Broker context.
 */
public interface JmsContextManager {

    /**
     * Gets a JMS {@link Destination}.
     *
     * @return a JMS {@link Destination}.
     * @throws InternalServerErrorException if an error occurs getting the {@link Destination JMS topic}.
     */
    Destination getDestination() throws InternalServerErrorException;

    /**
     * Gets a JMS {@link ConnectionFactory}.
     *
     * @return a JMS {@link ConnectionFactory}.
     * @throws InternalServerErrorException if an error occurs getting the JMS {@link ConnectionFactory}.
     */
    ConnectionFactory getConnectionFactory() throws InternalServerErrorException;

}
