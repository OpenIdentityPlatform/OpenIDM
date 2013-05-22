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

import java.util.Map;

import org.osgi.framework.BundleContext;

/**
 * Abstract audit logger to implement common attributes.
 *
 * @author brmiller
 */
public abstract class AbstractAuditLogger implements AuditLogger {

    /** config token to determine whether to use this logger for queries */
    public final static String CONFIG_LOG_USE_FOR_QUERIES = "useForQueries";

    /** whether to use this logger for reads/queries */
    boolean useForQueries;

    /**
     * Set base configuration for all loggers from config.
     *
     * @param config configuration
     * @param ctx context
     */
    public void setConfig(Map config, BundleContext ctx) {
        useForQueries = config.containsKey(CONFIG_LOG_USE_FOR_QUERIES)
            && (Boolean) config.get(CONFIG_LOG_USE_FOR_QUERIES);
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

}
