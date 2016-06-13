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
import org.forgerock.openidm.config.enhanced.InvalidException;

/**
 * Parsed Config handling
 */
class ColumnMapping {
    public static final String DB_COLUMN_NAME = "column";
    public static final String DB_COLUMN_TYPE = "type";

    public static final String TYPE_STRING = "STRING";
    public static final String TYPE_JSON_MAP = "JSON_MAP";
    public static final String TYPE_JSON_LIST = "JSON_LIST";


    public JsonPointer objectColPointer;
    public String objectColName; // String representation of the column
                                 // name/path
    public String dbColName;
    public String dbColType;

    public ColumnMapping(String objectColName, JsonValue dbColMappingConfig) {
        this.objectColName = objectColName;
        this.objectColPointer = new JsonPointer(objectColName);
        if (dbColMappingConfig.required().isList()) {
            if (dbColMappingConfig.asList().size() != 2) {
                throw new InvalidException("Explicit table mapping has invalid entry for "
                        + objectColName + ", expecting column name and type but contains "
                        + dbColMappingConfig.asList());
            }
            dbColName = dbColMappingConfig.get(0).required().asString();
            dbColType = dbColMappingConfig.get(1).required().asString();
        } else if (dbColMappingConfig.isMap()) {
            dbColName = dbColMappingConfig.asMap().get(DB_COLUMN_NAME).toString();
            dbColType = dbColMappingConfig.asMap().get(DB_COLUMN_TYPE).toString();
        } else {
            dbColName = dbColMappingConfig.asString();
            dbColType = TYPE_STRING;
        }
    }

    public boolean isJsonPointer(JsonPointer fieldPointer) {
        return objectColPointer.equals(fieldPointer);
    }

    public String toString() {
        return "object column : " + objectColName + " -> " + dbColName + ":" + dbColType + "\n";
    }
}
