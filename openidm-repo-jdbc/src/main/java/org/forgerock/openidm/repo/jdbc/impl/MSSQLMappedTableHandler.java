/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.repo.jdbc.impl;

import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.repo.jdbc.SQLExceptionHandler;

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
