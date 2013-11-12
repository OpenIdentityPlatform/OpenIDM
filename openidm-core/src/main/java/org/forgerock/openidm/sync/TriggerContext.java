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
package org.forgerock.openidm.sync;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.PersistenceConfig;


/**
 * A ServerContext that stores the source of a trigger during a sync operation.
 *
 * @author brmiller
 */
public class TriggerContext extends ServerContext {

    private static final String ATTR_CLASS = "class";

    // persisted attribute name
    private static final String ATTR_TRIGGER_SOURCE = "trigger-source";

    /** the trigger source */
    private String trigger;

    /**
     * Create a new trigger context from an existing (parent) context and the source of the trigger.
     *
     * @param parent the parent server context
     * @param trigger the trigger source
     */
    public TriggerContext(final ServerContext parent, final String trigger) {
        super(/*checkNotNull(*/parent/*)*/); // !@#$%@#$% - org.forgerock.json.resource.Resources.checkNotNull is package privet...
        this.trigger = trigger;
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
        this.trigger = savedContext.get(ATTR_TRIGGER_SOURCE).asString();
    }

    /**
     * Retrieves the trigger source.
     * @return the trigger
     */
    public String getTrigger() {
        return trigger;
    }

    /**
     * {@inheritDoc}
     */
    protected void saveToJson(final JsonValue savedContext, final PersistenceConfig config)
            throws ResourceException {
        super.saveToJson(savedContext, config);
        savedContext.put(ATTR_TRIGGER_SOURCE, trigger);
    }

}
