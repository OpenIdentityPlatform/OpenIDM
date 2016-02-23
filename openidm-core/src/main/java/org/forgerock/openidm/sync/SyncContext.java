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
package org.forgerock.openidm.sync;

import static org.forgerock.util.Reject.checkNotNull;

import org.forgerock.json.JsonValue;
import org.forgerock.services.context.AbstractContext;
import org.forgerock.services.context.Context;

/**
 *  A context to store sync data on the request context chain.
 */
public class SyncContext extends AbstractContext {

    /** The name of the Context */
    private static final String CONTEXT_NAME = "sync";

    /** The name of the mapping associated with the synchronization */
    private static final String ATTR_MAPPING = "mapping";

    public SyncContext(final Context parent, String mapping) {
        super(checkNotNull(parent, "Cannot instantiate SyncContext with null parent Context"), CONTEXT_NAME);
        data.put(ATTR_MAPPING, mapping);
    }

    public SyncContext(final JsonValue savedContext, final ClassLoader classLoader) {
        super(savedContext, classLoader);
    }

    /**
     * Return boolean to indicate if Sync is enabled
     *
     * @return true if sync is enabled; false otherwise
     */
    public boolean isSyncEnabled() {
        return data.get("enabled").defaultTo(Boolean.TRUE).asBoolean();
    }

    /**
     * Enable Sync.
     */
    public void enableSync() {
        data.put("enabled", Boolean.TRUE);
    }

    /**
     * Disable Sync
     */
    public void disableSync() {
        data.put("enabled", Boolean.FALSE);
    }
}
