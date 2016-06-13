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

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;

/**
 * Generic table configuration.
 */
class GenericTableConfig {
    public String mainTableName;
    public String propertiesTableName;
    public boolean searchableDefault;
    public GenericPropertiesConfig properties;

    public boolean isSearchable(JsonPointer propPointer) {

        // More specific configuration takes precedence
        Boolean explicit = null;
        while (!propPointer.isEmpty() && explicit == null) {
            explicit = properties.explicitlySearchable.get(propPointer);
            propPointer = propPointer.parent();
        }

        if (explicit != null) {
            return explicit.booleanValue();
        } else {
            return searchableDefault;
        }
    }

    /**
     * @return Approximation on whether this may have searchable properties
     * It is only an approximation as we do not have an exhaustive list of possible properties
     * to consider against a default setting of searchable.
     */
    public boolean hasPossibleSearchableProperties() {
        return ((searchableDefault) ? true : properties.explicitSearchableProperties);
    }

    public static GenericTableConfig parse(JsonValue tableConfig) {
        GenericTableConfig cfg = new GenericTableConfig();
        tableConfig.required();
        cfg.mainTableName = tableConfig.get("mainTable").required().asString();
        cfg.propertiesTableName = tableConfig.get("propertiesTable").required().asString();
        cfg.searchableDefault = tableConfig.get("searchableDefault").defaultTo(Boolean.TRUE).asBoolean();
        cfg.properties = GenericPropertiesConfig.parse(tableConfig.get("properties"));

        return cfg;
    }
}
