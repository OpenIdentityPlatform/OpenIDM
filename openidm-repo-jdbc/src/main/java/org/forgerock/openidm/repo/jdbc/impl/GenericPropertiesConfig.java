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
package org.forgerock.openidm.repo.jdbc.impl;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;

/**
 * Generic Properties configuration.
 */
class GenericPropertiesConfig {
    public Map<JsonPointer, Boolean> explicitlySearchable = new HashMap<JsonPointer, Boolean>();
    public String mainTableName;
    public String propertiesTableName;
    public boolean searchableDefault;
    public GenericPropertiesConfig properties;
    // Whether there are any properties explicitly set to searchable true
    public boolean explicitSearchableProperties;

    public static GenericPropertiesConfig parse(JsonValue propsConfig) {

        GenericPropertiesConfig cfg = new GenericPropertiesConfig();
        if (!propsConfig.isNull()) {
            for (String propName : propsConfig.keys()) {
                JsonValue detail = propsConfig.get(propName);
                boolean propSearchable = detail.get("searchable").asBoolean();
                cfg.explicitlySearchable.put(new JsonPointer(propName), propSearchable);
                if (propSearchable) {
                    cfg.explicitSearchableProperties = true;
                }
            }
        }

        return cfg;
    }
}
