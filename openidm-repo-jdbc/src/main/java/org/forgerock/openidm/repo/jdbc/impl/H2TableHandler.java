/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2013-2014 ForgeRock AS. All rights reserved.
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
 * $Id$
 */
package org.forgerock.openidm.repo.jdbc.impl;

import static org.forgerock.openidm.repo.QueryConstants.PAGED_RESULTS_OFFSET;
import static org.forgerock.openidm.repo.QueryConstants.PAGE_SIZE;
import static org.forgerock.openidm.repo.QueryConstants.SORT_KEYS;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.SortKey;
import org.forgerock.openidm.repo.jdbc.SQLExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Jake Feasel
 */
public class H2TableHandler extends GenericTableHandler {

    public H2TableHandler(JsonValue tableConfig, String dbSchemaName, JsonValue queriesConfig, JsonValue commandsConfig,
            int maxBatchSize, SQLExceptionHandler sqlExceptionHandler) {
        super(tableConfig, dbSchemaName, queriesConfig, commandsConfig, maxBatchSize, sqlExceptionHandler);
    }

    protected Map<QueryDefinition, String> initializeQueryMap() {
        Map<QueryDefinition, String> result = super.initializeQueryMap();
        String typeTable = dbSchemaName == null ? "objecttypes" : dbSchemaName + ".objecttypes";
        String mainTable = dbSchemaName == null ? mainTableName : dbSchemaName + "." + mainTableName;
        String propertyTable = dbSchemaName == null ? propTableName : dbSchemaName + "." + propTableName;

        result.put(QueryDefinition.PROPDELETEQUERYSTR, "DELETE FROM " + propertyTable + " WHERE " + mainTableName + "_id IN (SELECT obj.id FROM " + mainTable + " obj INNER JOIN " + typeTable + " objtype ON obj.objecttypes_id = objtype.id WHERE objtype.objecttype = ? AND obj.objectid = ?)");
        return result;
    }
    
    @Override
    public String buildRawQuery(QueryFilter filter, Map<String, Object> replacementTokens, Map<String, Object> params) {
        final String offsetParam = (String) params.get(PAGED_RESULTS_OFFSET);
        final String pageSizeParam = (String) params.get(PAGE_SIZE);
        String filterString = getFilterString(filter, replacementTokens);
        
        // Check for sort keys and build up order-by syntax
        final List<SortKey> sortKeys = (List<SortKey>)params.get(SORT_KEYS);
        if (sortKeys != null && sortKeys.size() > 0) {
            List<String> innerJoins = new ArrayList<String>();
            List<String> keys = new ArrayList<String>();
            prepareSortKeyStatements(sortKeys, innerJoins, keys, replacementTokens);
            filterString = StringUtils.join(innerJoins, " ") + " " + filterString + " ORDER BY " + StringUtils.join(keys, ", ");
        }

        return "SELECT obj.fullobject FROM ${_dbSchema}.${_mainTable} obj "
                + filterString + " LIMIT " + pageSizeParam + " OFFSET " + offsetParam;
    }
}
