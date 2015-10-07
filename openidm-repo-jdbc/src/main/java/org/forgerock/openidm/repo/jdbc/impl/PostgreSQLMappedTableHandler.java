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
package org.forgerock.openidm.repo.jdbc.impl;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.repo.jdbc.SQLExceptionHandler;
import org.forgerock.openidm.util.Accessor;

import java.util.Map;

/**
 * Mapped table handler for PostgreSQL that expects {@code JSON}-type columns for
 * {@link ColumnMapping#TYPE_JSON_LIST} and {@link ColumnMapping#TYPE_JSON_MAP} property mappings.
 */
public class PostgreSQLMappedTableHandler extends MappedTableHandler {
    public PostgreSQLMappedTableHandler(String tableName, Map<String, Object> mapping, String dbSchemaName, JsonValue queriesConfig, JsonValue commandsConfig, SQLExceptionHandler sqlExceptionHandler, Accessor<CryptoService> cryptoServiceAccessor) throws InternalServerErrorException {
        super(tableName, mapping, dbSchemaName, queriesConfig, commandsConfig, sqlExceptionHandler, cryptoServiceAccessor);
    }

    @Override
    protected void initializeQueries() {
        final String mainTable = dbSchemaName == null ? tableName : dbSchemaName + "." + tableName;
        final StringBuffer colNames = new StringBuffer();
        final StringBuffer tokenNames = new StringBuffer();
        final StringBuffer prepTokens = new StringBuffer();
        final StringBuffer updateAssign = new StringBuffer();
        boolean isFirst = true;

        for (ColumnMapping colMapping : explicitMapping.columnMappings) {
            if (!isFirst) {
                colNames.append(", ");
                tokenNames.append(",");
                prepTokens.append(",");
                updateAssign.append(", ");
            }
            colNames.append(colMapping.dbColName);
            tokenNames.append("${").append(colMapping.objectColName).append("}");

            if (ColumnMapping.TYPE_JSON_LIST.equals(colMapping.dbColType) ||
                    ColumnMapping.TYPE_JSON_MAP.equals(colMapping.dbColType)) {
                prepTokens.append("?::json");
                updateAssign.append(colMapping.dbColName).append(" = ?::json");
            } else {
                prepTokens.append("?");
                updateAssign.append(colMapping.dbColName).append(" = ?");
            }

            tokenReplacementPropPointers.add(colMapping.objectColPointer);
            // updateAssign.append(colMapping.dbColName).append(" = ${").append(colMapping.objectColName).append("}");
            isFirst = false;
        }

        readQueryStr = "SELECT * FROM " + mainTable + " WHERE objectid = ?";
        readForUpdateQueryStr = "SELECT * FROM " + mainTable + " WHERE objectid = ? FOR UPDATE";
        createQueryStr =
                "INSERT INTO " + mainTable + " (" + colNames + ") VALUES ( " + prepTokens + ")";
        updateQueryStr = "UPDATE " + mainTable + " SET " + updateAssign + " WHERE objectid = ?";
        deleteQueryStr = "DELETE FROM " + mainTable + " WHERE objectid = ? AND rev = ?";

        logger.debug("Unprepared query strings {} {} {} {} {}",
                readQueryStr, createQueryStr, updateQueryStr, deleteQueryStr);
    }
}
