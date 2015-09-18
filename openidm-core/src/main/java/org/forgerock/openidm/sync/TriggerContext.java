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
 * Portions copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openidm.sync;

import static org.forgerock.util.Reject.checkNotNull;

import org.forgerock.services.context.Context;
import org.forgerock.services.context.AbstractContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;


/**
 * A Context that stores the source of a trigger during a sync operation.
 *
 */
public class TriggerContext extends AbstractContext {

    private static final String CONTEXT_NAME = "trigger";

    // persisted attribute name
    private static final String ATTR_TRIGGER_SOURCE = "triggerSource";

    /**
     * Create a new trigger context from an existing (parent) context and the source of the trigger.
     *
     * @param parent the parent server context
     * @param trigger the trigger source
     */
    public TriggerContext(final Context parent, final String trigger) {
        super(checkNotNull(parent, "Cannot instantiate TriggerContext with null parent Context"), CONTEXT_NAME);
        data.put(ATTR_TRIGGER_SOURCE, trigger);
    }

    /**
     * Restore from JSON representation.
     *
     * @param savedContext
     *            The JSON representation from which this context's attributes
     *            should be parsed.
     * @param classLoader
     *            The ClassLoader.
     * @throws ResourceException
     *             If the JSON representation could not be parsed.
     */
    public TriggerContext(final JsonValue savedContext, final ClassLoader classLoader)
            throws ResourceException {
        super(savedContext, classLoader);
    }

    /**
     * Retrieves the trigger source.
     * @return the trigger
     */
    public String getTrigger() {
        return data.get(ATTR_TRIGGER_SOURCE).asString();
    }

}
