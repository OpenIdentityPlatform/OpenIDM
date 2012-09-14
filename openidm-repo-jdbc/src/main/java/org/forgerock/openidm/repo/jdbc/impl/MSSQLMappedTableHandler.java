package org.forgerock.openidm.repo.jdbc.impl;

import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.repo.jdbc.SQLExceptionHandler;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class MSSQLMappedTableHandler extends MappedTableHandler {

    public MSSQLMappedTableHandler(String tableName, Map mapping, String dbSchemaName,
            JsonValue queriesConfig, SQLExceptionHandler sqlExceptionHandler) {
        super(tableName, mapping, dbSchemaName, queriesConfig, sqlExceptionHandler);
        String mainTable = dbSchemaName == null ? tableName : dbSchemaName + "." + tableName;
        /*
         * SQLServer does not support the FOR UPDATE clause         
         */
        readForUpdateQueryStr = "SELECT * FROM " + mainTable + " WHERE objectid = ?";
    }
}
