/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.sync;

import static org.forgerock.util.Reject.checkNotNull;

import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.PersistenceConfig;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;


/**
 * A ServerContext that stores the pending link information during a sync operation.
 *
 * @author ckienle
 */
public class PendingLinkContext extends ServerContext {

    public static final String CONTEXT_NAME = "pendingLink";

    private static final String ATTR_PENDING_LINK = "pendingLink";
    private static final String ATTR_PENDING = "pending";

    /**
     * Create a new PendingLinkContext from an existing (parent) context.
     *
     * @param parent the parent server context
     */
    public PendingLinkContext(final Context parent, Map<String, Object> pendingLink) {
        super(checkNotNull(parent, "Cannot instantiate PendingLinkContext with null parent Context"));
        data.put(ATTR_PENDING_LINK, pendingLink);
        data.put(ATTR_PENDING, true);
    }

    /**
     * Restore from JSON representation.
     *
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed.
     * @param config
     *            The persistence configuration.
     * @throws ResourceException
     *             If the JSON representation could not be parsed.
     */
    public PendingLinkContext(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        super(savedContext, config);
    }

    /**
     * Get this Context's name.
     *
     * @return this object's name
     */
    public String getContextName() {
        return CONTEXT_NAME;
    }
    
    /**
     * Returns true if the pending link is still pending, false otherwise.
     *
     * @return this object's name
     */
    public boolean isPending() {
        return data.get(ATTR_PENDING).asBoolean();
    }
    
    /**
     * Clears pending link data, sets pending to false.
     *
     * @return this object's name
     */
    public void clear() {
        data.remove(ATTR_PENDING_LINK);
        data.put(ATTR_PENDING, false);
    }

    /**
     * Retrieves the pending link data.
     * 
     * @return the pending link
     */
    public Map<String, Object> getPendingLink() {
        return data.get(ATTR_PENDING_LINK).asMap();
    }

}

