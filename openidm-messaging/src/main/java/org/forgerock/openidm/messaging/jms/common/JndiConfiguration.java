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
import javax.naming.InitialContext;
import java.util.Collections;
import java.util.Map;

import org.forgerock.json.JsonValue;

/**
 * Stores the JNDI context properties and lookup names.
 */
public class JndiConfiguration {

    private final String destinationName;
    private final String connectionFactoryName;
    private final Map<String, String> contextProperties;

    public JndiConfiguration(final JsonValue jndiConfig) {
        connectionFactoryName = jndiConfig.get("connectionFactoryName").required().asString();
        destinationName = jndiConfig.get("destinationName").required().asString();
        contextProperties =
                Collections.unmodifiableMap(jndiConfig.get("contextProperties").required().asMap(String.class));
    }

    /**
     * Gets the Jndi {@link InitialContext} properties.
     *
     * @return The {@link InitialContext} properties.
     */
    public Map<String, String> getContextProperties() {
        return contextProperties;
    }

    /**
     * Returns the jndi lookup name for the JMS {@link Destination} to which messages will be delivered to/from.
     * Do not confuse this with Audit Topics.
     *
     * @return The jndi lookup name for the JMS {@link Destination} to which messages will be delivered to/from.
     * @see InitialContext#lookup(String)
     */
    public String getDestinationName() {
        return destinationName;
    }

    /**
     * Returns the jndi lookup name for the JMS {@link ConnectionFactory} to which messages will be published.
     * Do not confuse this with Audit Topics.
     *
     * @return The jndi lookup name for the JMS {@link ConnectionFactory} to which messages will be published.
     * @see InitialContext#lookup(String)
     */
    public String getConnectionFactoryName() {
        return connectionFactoryName;
    }
}
