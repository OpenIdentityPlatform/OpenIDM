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
 * A ServerContext that stores a pending action information during a sync operation.
 *
 */
public class PendingActionContext extends ServerContext {

    public static final String CONTEXT_NAME = "pendingAction";

    private static final String ATTR_ACTION = "action";
    private static final String ATTR_ACTION_DATA = "actionData";
    private static final String ATTR_PENDING = "pending";

    /**
     * Create a new PendingActionContext from an existing (parent) context.
     *
     * @param parent the parent server context
     * @param pendingActionMap a Map containing the pending action data
     */
    public PendingActionContext(final Context parent, Map<String, Object> pendingActionData, String action) {
        super(checkNotNull(parent, "Cannot instantiate PendingActionContext with null parent Context"));
        data.put(ATTR_ACTION, action);
        data.put(ATTR_ACTION_DATA, pendingActionData);
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
    public PendingActionContext(final JsonValue savedContext, final PersistenceConfig config)
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
        data.remove(ATTR_ACTION_DATA);
        data.put(ATTR_PENDING, false);
    }

    /**
     * Retrieves the pending action data.
     * 
     * @return the pending action data
     */
    public Map<String, Object> getPendingActionData() {
        return data.get(ATTR_ACTION_DATA).asMap();
    }

    /**
     * Retrieves the pending action.
     * 
     * @return the pending action
     */
    public String getPendingAction() {
        return data.get(ATTR_ACTION).asString();
    }

}

