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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.config;

import org.forgerock.auth.common.DebugLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jaspi Debug Logger implementation which delegate log calls to SLF4J.
 */
public class JaspiDebugLogger implements DebugLogger {

    private final Logger logger = LoggerFactory.getLogger(JaspiDebugLogger.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(String message) {
        logger.trace(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(String message, Throwable t) {
        logger.trace(message, t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(String message) {
        logger.debug(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(String message, Throwable t) {
        logger.debug(message, t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(String message) {
        logger.error(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(String message, Throwable t) {
        logger.error(message, t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warn(String message, Throwable t) {
        logger.warn(message, t);
    }
}
