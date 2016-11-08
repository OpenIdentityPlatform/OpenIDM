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

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.util.Accessor;
import org.forgerock.openidm.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ResultSet Mapper for Explicit Mappings
 */
class ExplicitResultSetMapper implements ResultSetMapper {

    /**
     * ExplicitResultsSetMapper Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(ExplicitResultSetMapper.class);

    /**
     * JsonPointer to total number of results
     */
    private static final JsonPointer pathToTotal = new JsonPointer("/total");

    /**
     * Table name
     */
    private final String tableName;

    /**
     * Crypto Service Accessor
     */
    private final Accessor<CryptoService> cryptoServiceAccessor;

    /**
     * Mapping between Object column and table column names
     */
    private final List<ColumnMapping> columnMappings;

    /**
     * Jackson parser
     */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Quick access to mapping for MVCC revision
     */
    private ColumnMapping revMapping;

    public ExplicitResultSetMapper(String tableName, JsonValue mappingConfig, Accessor<CryptoService> cryptoServiceAccessor) {
        this.columnMappings = new ArrayList<>();
        this.cryptoServiceAccessor = cryptoServiceAccessor;
        this.tableName = tableName;
        for (Map.Entry<String, Object> entry : mappingConfig.asMap().entrySet()) {
            String key = entry.getKey();
            JsonValue value = mappingConfig.get(key);
            ColumnMapping colMapping = new ColumnMapping(key, value);
            columnMappings.add(colMapping);
            if ("_rev".equals(colMapping.objectColName)) {
                revMapping = colMapping;
            }
        }
    }

    public JsonValue mapToJsonValue(ResultSet rs, Set<String> columnNames) throws SQLException, InternalServerErrorException {
        JsonValue mappedResult = new JsonValue(new LinkedHashMap<String, Object>());
        for (ColumnMapping entry : getColumnMappings()) {
            Object value = null;
            if (columnNames.contains(entry.dbColName)) {
                if (ColumnMapping.TYPE_STRING.equals(entry.dbColType)) {
                    value = rs.getString(entry.dbColName);
                    if (cryptoServiceAccessor == null || cryptoServiceAccessor.access() == null) {
                        throw new InternalServerErrorException("CryptoService unavailable");
                    }
                    if (JsonUtil.isEncrypted((String) value)) {
                        value = convertToJson(entry.dbColName, "encrypted", (String) value, Map.class).asMap();
                    }
                } else if (ColumnMapping.TYPE_JSON_MAP.equals(entry.dbColType)) {
                    value = convertToJson(entry.dbColName, entry.dbColType, rs.getString(entry.dbColName), Map.class).asMap();
                } else if (ColumnMapping.TYPE_JSON_LIST.equals(entry.dbColType)) {
                    value = convertToJson(entry.dbColName, entry.dbColType, rs.getString(entry.dbColName), List.class).asList();
                } else {
                    throw new InternalServerErrorException("Unsupported DB column type " + entry.dbColType);
                }
                mappedResult.putPermissive(entry.objectColPointer, value);
            }
        }
        if (columnNames.contains("total") && !columnMappings.contains("total")) {
            mappedResult.putPermissive(pathToTotal, rs.getInt("total"));
        }
        logger.debug("Mapped rs {} to {}", rs, mappedResult);
        return mappedResult;
    }

    private <T> JsonValue convertToJson(String name, String nameType, String value, Class<T> valueType) throws InternalServerErrorException {
        if (value != null) {
            try {
                return new JsonValue(mapper.readValue(value, valueType));
            } catch (IOException e) {
                throw new InternalServerErrorException("Unable to map " + nameType + " value for " + name, e);
            }
        }
        return new JsonValue(null);
    }

    public String getRev(ResultSet rs) throws SQLException {
        return rs.getString(revMapping.dbColName);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Explicit table mapping for " + tableName + " :\n");
        for (ColumnMapping entry : getColumnMappings()) {
            sb.append(entry.toString());
        }
        return sb.toString();
    }

    public static Set<String> getColumnNames(ResultSet rs) throws SQLException {
        TreeSet<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            set.add(rs.getMetaData().getColumnName(i));
        }
        return set;
    }

    public String getDbColumnName(JsonPointer fieldName) {
        for (ColumnMapping column : getColumnMappings()) {
            if (column.isJsonPointer(fieldName)) {
                return column.dbColName;
            }
        }
        throw new IllegalArgumentException("Unknown object field: " + fieldName.toString());
    }

    /**
     * @return the columnMappings
     */
    public List<ColumnMapping> getColumnMappings() {
        return columnMappings;
    }

      /**
     * Maps the ResultSet to a List of mapped rows representing the OpenIDM object.
     * 
     * The implementation of this method traverses the ResultSet and moves the
     * cursor until it is positioned after the last row. This method should only
     * be called once per ResultSet unless the ResultSet is scrollable.
     * 
     * @return the ResultSet as a List of mapped rows
     */
    @Override
    public List<Map<String, Object>> mapToObject(ResultSet rs, String queryId, String type, Map<String, Object> params) throws SQLException, InternalServerErrorException {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> names = ExplicitResultSetMapper.getColumnNames(rs);
        while (rs.next()) {
            JsonValue obj = mapToJsonValue(rs, names);
            result.add(obj.asMap());
        }
        return result;
    }

    /**
     * Maps the ResultSet to a raw List of mapped rows.
     * 
     * The implementation of this method traverses the ResultSet and moves the
     * cursor until it is positioned after the last row. This method should only
     * be called once per ResultSet unless the ResultSet is scrollable.
     * 
     * @return the ResultSet as a List of mapped rows with column names converted
     * to lowercase
     */
    @Override
    public List<Map<String, Object>> mapToRawObject(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();
        List<Map<String, Object>> result = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> obj = new HashMap<>(columns);
            for (int i = 1; i <= columns; ++i) {
                obj.put(md.getColumnName(i).toLowerCase(), rs.getObject(i));
            }
            result.add(obj);
        }
        return result;
    }
}
