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

import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.ServerContext;

/**
 * A ServerContext that stores the source of a trigger during a sync operation.
 *
 * @author brmiller
 */
public class TriggerContext extends ServerContext {

    /** the trigger source */
    private String trigger;

    /**
     * Constructor
     *
     * @param parent the parent context
     * @param trigger the trigger
     */
    public TriggerContext(final Context parent, final String trigger) {
        super(parent);
        this.trigger = trigger;
    }

    /**
     * Retrieves the trigger source.
     * @return the trigger
     */
    public String getTrigger() {
        return trigger;
    }
}
