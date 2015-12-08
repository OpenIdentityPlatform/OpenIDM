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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openidm.sync;

import static org.forgerock.util.Reject.checkNotNull;

import org.forgerock.json.JsonValue;
import org.forgerock.services.context.AbstractContext;
import org.forgerock.services.context.Context;

/**
 * A context to store reconciliation data on the request context chain
 */
public class ReconContext extends AbstractContext {

    /** The name of the Context */
    private static final String CONTEXT_NAME = "recon";
    
    /** The name of the mapping associated with the reconciliation */
    private static final String ATTR_MAPPING = "mapping";

    /**
     * Create a new recon context from an existing (parent) context.
     *
     * @param parent the parent server context
     * @param trigger the trigger source
     */
    public ReconContext(final Context parent, String mapping) {
        super(checkNotNull(parent, "Cannot instantiate ReconContext with null parent Context"), CONTEXT_NAME);
        data.put(ATTR_MAPPING, mapping);
    }

    /**
     * Creates a new context from the JSON representation of a previously persisted context.
     *
     * @param savedContext The JSON representation from which this context's attributes should be parsed.
     * @param classLoader The ClassLoader which can properly resolve the persisted class-name.
     */
    public ReconContext(final JsonValue savedContext, final ClassLoader classLoader) {
        super(savedContext, classLoader);
    }
    
    /**
     * Puts a value in the data map
     */
    public void put(String key, Object value) {
        this.data.put(key, value);
    }

}
