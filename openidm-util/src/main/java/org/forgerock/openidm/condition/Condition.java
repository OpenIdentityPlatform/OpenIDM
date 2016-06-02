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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openidm.condition;

import org.forgerock.services.context.Context;

/**
 * Represents a condition on which a property mapping may be applied, or a policy may be enforced.
 */
public interface Condition {

    /**
     * Evaluates the condition.  Returns true if the condition is met, false otherwise.
     * 
     * @param content the content to use during evaluation.
     * @param context the {@link Context} associated with this evaluation.
     * @return true if the condition is met, false otherwise.
     */
    boolean evaluate(Object content, Context context);
}
