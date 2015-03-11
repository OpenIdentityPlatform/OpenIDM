/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openidm.audit.impl;

import org.forgerock.json.fluent.JsonValue;

/**
 * Abstract audit logger to implement common attributes.
 */
public abstract class AbstractAuditLogger implements AuditLogger {

    /** config token to determine whether to use this logger for queries */
    public final static String CONFIG_LOG_USE_FOR_QUERIES = "useForQueries";

    /** config token to determine whether a logging failure is fatal */
    public final static String CONFIG_LOG_IGNORE_LOGGING_FAILURES = "ignoreLoggingFailures";

    /** whether to use this logger for reads/queries (default: false) */
    boolean useForQueries;

    /** whether to ignore a logging failure exception (default: false) */
    boolean ignoreLoggingFailures;

    /**
     * Set base configuration for all loggers from config.
     *
     * @param config configuration
     */
    public void setConfig(JsonValue config) {
        useForQueries = config.get(CONFIG_LOG_USE_FOR_QUERIES).defaultTo(false).asBoolean();
        ignoreLoggingFailures = config.get(CONFIG_LOG_IGNORE_LOGGING_FAILURES).defaultTo(false).asBoolean();
    }
    
    /**
     * Cleanup called when auditlogger no longer needed.
     */
    public abstract void cleanup();

    /**
     * Returns whether this logger is used for read/query operations.
     *
     * @return whether this logger is used for reads/queries.
     */
    public boolean isUsedForQueries()
    {
        return useForQueries;
    }

    /**
     * Returns whether a logging failure should be ignored
     *
     * @return whether logging failures should be ignored
     */
    public boolean isIgnoreLoggingFailures() {
        return ignoreLoggingFailures;
    }

}
