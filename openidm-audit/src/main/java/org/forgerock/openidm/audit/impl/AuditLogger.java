/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2013 ForgeRock AS. All rights reserved.
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

import org.forgerock.openidm.objset.ObjectSet;

import org.osgi.framework.BundleContext;

/**
 * OpenIDM audit logger
 * @author aegloff
 * @author brmiller
 */
public interface AuditLogger extends ObjectSet {
    
    /**
     * Set the audit logger configuration which is a logger specific 
     * map 
     * @param config the configuration
     * @param ctx
     */
    void setConfig(Map config, BundleContext ctx);
    
    /**
     * Cleanup called when auditlogger no longer needed
     */
    void cleanup();

    /**
     * Whether this audit logger is used for reads/queries, when multiple
     * loggers are configured.
     *
     * @return whether to use this logger for reads/queries.
     */
    boolean isUsedForQueries();
}
