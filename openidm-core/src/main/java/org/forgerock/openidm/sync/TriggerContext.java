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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.PersistenceConfig;


/**
 * A ServerContext that stores the source of a trigger during a sync operation.
 *
 */
public class TriggerContext extends ServerContext {

    private static final String CONTEXT_NAME = "trigger";

    // persisted attribute namon
    private static final String ATTR_TRIGGER_SOURCE = "triggerSource";

    /**
     * Create a new trigger context from an existing (parent) context and the source of the trigger.
     *
     * @param parent the parent server context
     * @param trigger the trigger source
     */
    public TriggerContext(final Context parent, final String trigger) {
        super(checkNotNull(parent, "Cannot instantiate TriggerContext with null parent Context"));
        data.put(ATTR_TRIGGER_SOURCE, trigger);
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
    public TriggerContext(final JsonValue savedContext, final PersistenceConfig config)
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
     * Retrieves the trigger source.
     * @return the trigger
     */
    public String getTrigger() {
        return data.get(ATTR_TRIGGER_SOURCE).asString();
    }

}
