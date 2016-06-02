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
package org.forgerock.openidm.condition;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.openidm.util.Scripts;


/**
 * A class that provides static methods for instantiating {@link Condition} objects.
 */
public class Conditions {

    private static final TrueCondition TRUE_CONDITION = new TrueCondition();

    /**
     * Creates a new {@link Condition} object based on the supplied configuration.  Currently a condition configuration
     * can represent a filter string or a script configuration.
     *
     * @param config An Object representing a condition configuration.
     * @return a Condition object
     */
    public static Condition newCondition(Object config) {
        JsonValue jsonConfig = new JsonValue(config);
        if (jsonConfig.isNull()) {
            return TRUE_CONDITION;
        } else if (jsonConfig.isString()) {
            return new QueryFilterCondition(QueryFilters.parse(jsonConfig.asString()));
        } else {
            return new ScriptedCondition(Scripts.newScript(jsonConfig));
        }
    }

}
