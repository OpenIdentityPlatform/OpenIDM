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
 * Portions copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.util;

import org.forgerock.json.JsonValue;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;

/**
 * A class that provides static methods for instantiating {@link Condition} objects.
 */
public class Conditions {
    
    /**
     * Creates a new {@link Condition} object based on the supplied configuration.  Currently a condition configuration
     * can represent a filter string or a script configuration.
     * 
     * @param config An Object representing a condition configuration.
     * @return
     */
    public static Condition newCondition(Object config) {
        return new Condition(new JsonValue(config));
    };
}
