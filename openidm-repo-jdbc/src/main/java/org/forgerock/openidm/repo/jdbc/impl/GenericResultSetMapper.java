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
 * Copyright 2011-2016 ForgeRock AS.
 */
package org.forgerock.openidm.repo.jdbc.impl;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ResultSet Mapper for Generic Mappings
 */
public class GenericResultSetMapper implements ResultSetMapper {
    static final Logger logger = LoggerFactory.getLogger(GenericResultSetMapper.class);

    /** Jackson parser */
    private final ObjectMapper mapper = new ObjectMapper();

    /** Type information for the Jackson parser */
    TypeReference<LinkedHashMap<String, Object>> typeRef = new TypeReference<LinkedHashMap<String, Object>>() {
    };
    
    @Override
    public List<Map<String, Object>> mapToRawObject(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();
        List<Map<String, Object>> result = new ArrayList<>();
        while (rs.next()){
            Map<String, Object> obj = new HashMap<>(columns);
            for(int i = 1; i <= columns; ++i){
                obj.put(md.getColumnName(i), rs.getObject(i));
            }
            result.add(obj);
        }
        rs.beforeFirst();
        return result;
    }
    
    @Override
    public List<Map<String, Object>> mapToObject(ResultSet rs, String queryId, String type, Map<String, Object> params) throws SQLException, IOException {
        List<Map<String, Object>> result = new ArrayList<>();
        ResultSetMetaData rsMetaData = rs.getMetaData();
        boolean hasFullObject = hasColumn(rsMetaData, "fullobject");
        boolean hasId = false;
        boolean hasRev = false;
        boolean hasPropKey = false;
        boolean hasPropValue = false;
        boolean hasTotal = false;
        if (!hasFullObject) {
            hasId = hasColumn(rsMetaData, "objectid");
            hasRev = hasColumn(rsMetaData, "rev");
            hasPropKey = hasColumn(rsMetaData, "propkey");
            hasPropValue = hasColumn(rsMetaData, "propvalue");
            hasTotal = hasColumn(rsMetaData, "total");
        }
        while (rs.next()) {
            if (hasFullObject) {
                String objString = rs.getString("fullobject");
                Map<String, Object> obj = mapper.readValue(objString, typeRef);
                // TODO: remove data logging
                logger.trace("Query result for queryId: {} type: {} converted obj: {}", new Object[]{queryId, type, obj});
                result.add(obj);
            } else {
                Map<String, Object> obj = new HashMap<String, Object>();
                if (hasId) {
                    obj.put("_id", rs.getString("objectid"));
                }
                if (hasRev) {
                    obj.put("_rev", rs.getString("rev"));
                }
                if (hasTotal) {
                    obj.put("total", rs.getInt("total"));
                }
                // Results from query on individual searchable property
                if (hasPropKey && hasPropValue) {
                    String propKey = rs.getString("propkey");
                    Object propValue = rs.getObject("propvalue");
                    JsonPointer pointer = new JsonPointer(propKey);
                    JsonValue wrapped = new JsonValue(obj);
                    wrapped.put(pointer, propValue);
                }
                result.add(obj);
            }
        }
        rs.beforeFirst();
        return result;
    }
    
    /**
     * Whether a result set contains a given column
     *
     * @param rsMetaData
     *            result set meta data
     * @param columnName
     *            name of the column to look for
     * @return true if it is present
     * @throws SQLException
     *             if meta data inspection failed
     */
    private boolean hasColumn(ResultSetMetaData rsMetaData, String columnName) throws SQLException {
        for (int colPos = 1; colPos <= rsMetaData.getColumnCount(); colPos++) {
            if (columnName.equalsIgnoreCase(rsMetaData.getColumnName(colPos))) {
                return true;
            }
        }
        return false;
    }
}
