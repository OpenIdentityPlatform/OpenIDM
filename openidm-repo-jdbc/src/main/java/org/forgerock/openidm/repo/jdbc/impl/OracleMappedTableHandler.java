package org.forgerock.openidm.repo.jdbc.impl;

import static org.forgerock.openidm.repo.QueryConstants.PAGED_RESULTS_OFFSET;
import static org.forgerock.openidm.repo.QueryConstants.PAGE_SIZE;
import static org.forgerock.openidm.repo.QueryConstants.SORT_KEYS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.SortKey;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.repo.jdbc.SQLExceptionHandler;
import org.forgerock.openidm.util.Accessor;

public class OracleMappedTableHandler extends MappedTableHandler {

    public OracleMappedTableHandler(String tableName, Map<String, Object> mapping, String dbSchemaName,
            JsonValue queriesConfig, JsonValue commandsConfig, SQLExceptionHandler sqlExceptionHandler,
            Accessor<CryptoService> cryptoServiceAccessor) throws InternalServerErrorException {
        super(tableName, mapping, dbSchemaName, queriesConfig, commandsConfig, sqlExceptionHandler, cryptoServiceAccessor);
    }    
    
    @Override
    public String buildRawQuery(QueryFilter filter, Map<String, Object> replacementTokens, Map<String, Object> params) {
        final int offsetParam = Integer.parseInt((String)params.get(PAGED_RESULTS_OFFSET));
        final int pageSizeParam = Integer.parseInt((String)params.get(PAGE_SIZE));
        String filterString = getFilterString(filter, replacementTokens);
        final String keysClause;
        
        // Check for sort keys and build up order-by syntax
        final List<SortKey> sortKeys = (List<SortKey>)params.get(SORT_KEYS);
        if (sortKeys != null && sortKeys.size() > 0) {
            List<String> keys = new ArrayList<String>();
            prepareSortKeyStatements(sortKeys, keys, replacementTokens);
            keysClause = StringUtils.join(keys, ", ");
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
