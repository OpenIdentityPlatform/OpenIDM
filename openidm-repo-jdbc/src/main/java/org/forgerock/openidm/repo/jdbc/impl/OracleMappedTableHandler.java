/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All Rights Reserved
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

import static org.forgerock.openidm.repo.QueryConstants.PAGED_RESULTS_OFFSET;
import static org.forgerock.openidm.repo.QueryConstants.PAGE_SIZE;
import static org.forgerock.openidm.repo.QueryConstants.SORT_KEYS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.SortKey;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.repo.jdbc.SQLExceptionHandler;
import org.forgerock.openidm.util.Accessor;
import org.forgerock.util.query.QueryFilter;

public class OracleMappedTableHandler extends MappedTableHandler {

    public OracleMappedTableHandler(String tableName, Map<String, Object> mapping, String dbSchemaName,
            JsonValue queriesConfig, JsonValue commandsConfig, SQLExceptionHandler sqlExceptionHandler,
            Accessor<CryptoService> cryptoServiceAccessor) throws InternalServerErrorException {
        super(tableName, mapping, dbSchemaName, queriesConfig, commandsConfig, sqlExceptionHandler, cryptoServiceAccessor);
    }    
    
    @Override
    public String renderQueryFilter(QueryFilter<JsonPointer> filter, Map<String, Object> replacementTokens, Map<String, Object> params) {
        final int offsetParam = Integer.parseInt((String)params.get(PAGED_RESULTS_OFFSET));
        final int pageSizeParam = Integer.parseInt((String)params.get(PAGE_SIZE));
        String filterString = getFilterString(filter, replacementTokens);
        final String keysClause;

        // JsonValue-cheat to avoid an unchecked cast
        final List<SortKey> sortKeys = new JsonValue(params).get(SORT_KEYS).asList(SortKey.class);
        // Check for sort keys and build up order-by syntax
        if (sortKeys != null && sortKeys.size() > 0) {
            keysClause = StringUtils.join(prepareSortKeyStatements(sortKeys), ", ");
        } else {
            keysClause = "objectid DESC";
        }
        
        return "SELECT * FROM ( SELECT ${_dbSchema}.${_mainTable}.*, row_number() OVER ( ORDER BY "
                + keysClause
                + " ) AS rn FROM ${_dbSchema}.${_mainTable} "
                + filterString 
                + " ) WHERE rn BETWEEN " 
                + (offsetParam+1)
                + " AND " 
                + (offsetParam + pageSizeParam)
                + " ORDER BY rn";
    }

}
