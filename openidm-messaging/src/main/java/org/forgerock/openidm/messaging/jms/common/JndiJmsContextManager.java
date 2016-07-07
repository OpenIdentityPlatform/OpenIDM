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
import javax.naming.NamingException;
import java.util.Hashtable;

import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides utility methods to extract JMS objects from JNDI configured objects.
 */
public class JndiJmsContextManager implements JmsContextManager {

    private static final Logger logger = LoggerFactory.getLogger(JndiJmsContextManager.class);

    private Destination destination;
    private ConnectionFactory connectionFactory;
    private final InitialContext context;
    private final JndiConfiguration jndiConfiguration;

    /**
     * Given the configuration, this builds a JMS InitialContext. The classloader of this class will be used as
     * the context classloader {@link Thread#setContextClassLoader(ClassLoader)}.
     *
     * @param configuration The {@link JndiConfiguration JNDI configuration}.
     * @throws InternalServerErrorException If unable to create the {@link InitialContext JNDI context}
     */
    public JndiJmsContextManager(JndiConfiguration configuration) throws ResourceException {
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            jndiConfiguration = configuration;
            context = new InitialContext(new Hashtable<>(configuration.getContextProperties()));
        } catch (NamingException e) {
            throw new InternalServerErrorException("Encountered issue building initial JNDI context", e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    /**
     * Returns the {@link Destination JMS Destination} to use for JMS publish/subscribe functionality.
     *
     * @return The {@link Destination JMS Destination} to use for JMS publish/subscribe functionality.
     * @throws InternalServerErrorException If unable to retrieve the {@link Destination JMS topic}.
     */
    @Override
    public Destination getDestination() throws InternalServerErrorException {
        try {
            if (destination == null) {
                destination = getObject(jndiConfiguration.getDestinationName(), Destination.class);
            }
            return destination;
        } catch (NamingException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

    /**
     * Returns the {@link ConnectionFactory JMS connection factory} to use to connect to JMS services.
     *
     * @return the {@link ConnectionFactory JMS connection factory} to use to connect to JMS services.
     * @throws InternalServerErrorException If unable to retrieve the {@link ConnectionFactory JMS connection factory}.
     */
    @Override
    public ConnectionFactory getConnectionFactory() throws InternalServerErrorException {
        try {
            if (connectionFactory == null) {
                connectionFactory = getObject(jndiConfiguration.getConnectionFactoryName(), ConnectionFactory.class);
            }
            return connectionFactory;
        } catch (NamingException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }


    @SuppressWarnings("unchecked")
    private <T> T getObject(final String jndiName, final Class<T> clazz)
            throws NamingException, InternalServerErrorException {

        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            final Object object = context.lookup(jndiName);
            if (clazz.isInstance(object)) {
                return (T) object;
            }
            final String error;
            if (null == object) {
                error = String.format("No Object was found at JNDI name '%s'", jndiName);
            } else {
                error = String.format("JNDI lookup('%s') did not return a '%s'. It returned a '%s'='%s'",
                        jndiName, clazz.getCanonicalName(), object.getClass().getCanonicalName(), object.toString());
            }
            logger.error(error);
            throw new InternalServerErrorException(error);
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }
}