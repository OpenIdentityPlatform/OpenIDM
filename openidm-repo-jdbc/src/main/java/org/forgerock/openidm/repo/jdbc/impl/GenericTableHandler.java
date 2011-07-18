/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.repo.jdbc.TableHandler;
import org.forgerock.openidm.repo.jdbc.impl.query.GenericTableQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Handling of tables in a generic (not object specific) layout
 *
 * @author aegloff
 */
public class GenericTableHandler implements TableHandler {
    final static Logger logger = LoggerFactory.getLogger(GenericTableHandler.class);

    String mainTableName;
    String propTableName;
    String dbSchemaName;

    ObjectMapper mapper = new ObjectMapper();
    GenericTableQueries queries;

    Map<QueryDefinition, String> queryMap;

    public enum QueryDefinition {
        READTYPEQUERYSTR,
        CREATETYPEQUERYSTR,
        READFORUPDATEQUERYSTR,
        READQUERYSTR,
        CREATEQUERYSTR,
        UPDATEQUERYSTR,
        DELETEQUERYSTR,
        PROPCREATEQUERYSTR,
        PROPDELETEQUERYSTR;
    }

    public GenericTableHandler(String mainTableName, String propTableName, String dbSchemaName, JsonNode queriesConfig) {
        this.mainTableName = mainTableName;
        this.propTableName = propTableName;
        this.dbSchemaName = dbSchemaName;

        queries = new GenericTableQueries();
        queries.setConfiguredQueries(mainTableName, propTableName, dbSchemaName, queriesConfig);
        queryMap = Collections.unmodifiableMap(initializeQueryMap());
    }


    protected Map<QueryDefinition, String> initializeQueryMap() {
        Map<QueryDefinition, String> result = new EnumMap<QueryDefinition, String>(QueryDefinition.class);

        // objecttypes table
        result.put(QueryDefinition.CREATETYPEQUERYSTR, "INSERT INTO " + dbSchemaName + ".objecttypes (objecttype) VALUES (?)");
        result.put(QueryDefinition.READTYPEQUERYSTR, "SELECT id FROM " + dbSchemaName + ".objecttypes objtype WHERE objtype.objecttype = ?");

        // Main object table
        result.put(QueryDefinition.READFORUPDATEQUERYSTR, "SELECT obj.* FROM " + dbSchemaName + "." + mainTableName + " obj INNER JOIN " + dbSchemaName + ".objecttypes objtype ON obj.objecttypes_id = objtype.id AND objtype.objecttype = ? WHERE obj.objectid  = ? FOR UPDATE");
        result.put(QueryDefinition.READQUERYSTR, "SELECT obj.rev, obj.fullobject FROM " + dbSchemaName + ".objecttypes objtype, " + dbSchemaName + "." + mainTableName + " obj WHERE obj.objecttypes_id = objtype.id AND objtype.objecttype = ? AND obj.objectid  = ?");
        result.put(QueryDefinition.CREATEQUERYSTR, "INSERT INTO " + dbSchemaName + "." + mainTableName + " (objecttypes_id, objectid, rev, fullobject) VALUES (?,?,?,?)");
        result.put(QueryDefinition.UPDATEQUERYSTR, "UPDATE " + dbSchemaName + "." + mainTableName + " obj SET obj.objectid = ?, obj.rev = ?, obj.fullobject = ? WHERE obj.id = ?");
        result.put(QueryDefinition.DELETEQUERYSTR, "DELETE FROM " + dbSchemaName + "." + mainTableName + " obj INNER JOIN " + dbSchemaName + ".objecttypes objtype ON obj.objecttypes_id = objtype.id AND objtype.objecttype = ? WHERE obj.objectid = ? AND obj.rev = ?");

        /* DB2 Script
        deleteQueryStr = "DELETE FROM " + dbSchemaName + "." + mainTableName + " obj WHERE EXISTS (SELECT 1 FROM " + dbSchemaName + ".objecttypes objtype WHERE obj.objecttypes_id = objtype.id AND objtype.objecttype = ?) AND obj.objectid = ? AND obj.rev = ?";
        */

        // Object properties table
        result.put(QueryDefinition.PROPCREATEQUERYSTR, "INSERT INTO " + dbSchemaName + "." + propTableName + " ( " + mainTableName + "_id, propkey, proptype, propvalue) VALUES (?,?,?,?)");
        result.put(QueryDefinition.PROPDELETEQUERYSTR, "DELETE FROM " + dbSchemaName + "." + propTableName + " prop WHERE " + mainTableName + "_id = (SELECT obj.id FROM " + dbSchemaName + "." + mainTableName + " obj, " + dbSchemaName + ".objecttypes objtype WHERE obj.objecttypes_id = objtype.id AND objtype.objecttype = ? AND obj.objectid  = ?)");


        return result;
    }

    /* (non-Javadoc)
    * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#read(java.lang.String, java.lang.String, java.lang.String, java.sql.Connection)
    */
    @Override
    public Map<String, Object> read(String fullId, String type, String localId, Connection connection)
            throws NotFoundException, SQLException, IOException {

        Map<String, Object> result = null;
        PreparedStatement readStatement = getPreparedStatement(connection, QueryDefinition.READQUERYSTR);

        logger.trace("Populating prepared statement {} for {}", readStatement, fullId);
        readStatement.setString(1, type);
        readStatement.setString(2, localId);

        logger.debug("Executing: {}", readStatement);
        ResultSet rs = readStatement.executeQuery();
        if (rs.next()) {
            String rev = rs.getString("rev");
            String objString = rs.getString("fullobject");
            ObjectMapper mapper = new ObjectMapper();
            result = (Map<String, Object>) mapper.readValue(objString, Map.class);
            result.put("_rev", rev);
            logger.debug(" full id: {}, rev: {}, obj {}", new Object[]{fullId, rev, result});
        } else {
            throw new NotFoundException("Object " + fullId + " not found in " + type);
        }

        return result;
    }

    /* (non-Javadoc)
    * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#create(java.lang.String, java.lang.String, java.lang.String, java.util.Map, java.sql.Connection)
    */
    @Override
    public void create(String fullId, String type, String localId, Map<String, Object> obj, Connection connection)
            throws SQLException, IOException, InternalServerErrorException {
        // Do this outside of the main tx.
        connection.setAutoCommit(true);
        long typeId = getTypeId(type, connection);

        connection.setAutoCommit(false);

        PreparedStatement createStatement = queries.getPreparedStatement(connection, queryMap.get(QueryDefinition.CREATEQUERYSTR), true);

        logger.debug("Create with fullid {}", fullId);
        String rev = "0";
        obj.put("_id", localId); // Save the id in the object
        obj.put("_rev", rev); // Save the rev in the object, and return the changed rev from the create.
        String objString = mapper.writeValueAsString(obj);

        logger.trace("Populating statement {} with params {}, {}, {}, {}",
                new Object[]{createStatement, typeId, localId, rev, objString});
        createStatement.setLong(1, typeId);
        createStatement.setString(2, localId);
        createStatement.setString(3, rev);
        createStatement.setString(4, objString);
        logger.debug("Executing: {}", createStatement);
        int val = createStatement.executeUpdate();

        ResultSet keys = createStatement.getGeneratedKeys();
        boolean validKeyEntry = keys.next();
        if (!validKeyEntry) {
            throw new InternalServerErrorException("Object creation for " + fullId + " failed to retrieve an assigned ID from the DB.");
        }
        long dbId = keys.getLong(1);

        logger.debug("Created object for id {} with rev {}", fullId, rev);
        JsonNode node = new JsonNode(obj);
        writeNodeProperties(fullId, dbId, localId, node, connection);
    }

    void writeNodeProperties(String fullId, long dbId, String localId, JsonNode node, Connection connection) throws SQLException {

        PreparedStatement propCreateStatement = getPreparedStatement(connection, QueryDefinition.PROPCREATEQUERYSTR);

        for (JsonNode entry : node) {
            String propkey = entry.getPointer().toString();
            if (entry.isMap() || entry.isList()) {
                writeNodeProperties(fullId, dbId, localId, entry, connection);
            } else {
                String propvalue = null;
                Object val = entry.getValue();
                if (val != null) {
                    propvalue = val.toString(); // TODO: proper type conversions?
                }
                String proptype = null;
                if (propvalue != null) {
                    proptype = entry.getValue().getClass().getName(); // TODO: proper type info
                }
                logger.trace("Populating statement {} with params {}, {}, {}, {}, {}",
                        new Object[]{propCreateStatement, dbId, localId, propkey, proptype, propvalue});
                propCreateStatement.setLong(1, dbId);
                propCreateStatement.setString(2, propkey);
                propCreateStatement.setString(3, proptype);
                propCreateStatement.setString(4, propvalue);
                logger.debug("Executing: {}", propCreateStatement);
                int val2 = propCreateStatement.executeUpdate();
                logger.debug("Created objectproperty id: {} propkey: {} proptype: {}, propvalue: {}", new Object[]{fullId, propkey, proptype, propvalue});
            }
        }
    }

    // Ensure type is in objecttypes table and get its assigned id
    long getTypeId(String type, Connection connection) throws SQLException, InternalServerErrorException {
        Exception detectedEx = null;
        long typeId = readTypeId(type, connection);
        if (typeId < 0) {
            try {
                createTypeId(type, connection);
            } catch (SQLException ex) {
                // Rather than relying on DB specific ignore if exists functionality handle it here
                // Could extend this in the future to more explicitly check for duplicate key error codes, but these again can be DB specific
                detectedEx = ex;
            }
            typeId = readTypeId(type, connection);
            if (typeId < 0) {
                throw new InternalServerErrorException("Failed to populate and look up objecttypes table, no id could be retrieved for " + type, detectedEx);
            }
        }
        return typeId;
    }

    /**
     * @param type       the object type URI
     * @param connection the DB connection
     * @return the typeId for the given type if exists, or -1 if does not exist
     */
    long readTypeId(String type, Connection connection) throws SQLException {
        long typeId = -1;

        Map<String, Object> result = null;
        PreparedStatement readTypeStatement = getPreparedStatement(connection, QueryDefinition.READTYPEQUERYSTR);

        logger.trace("Populating prepared statement {} for {}", readTypeStatement, type);
        readTypeStatement.setString(1, type);

        logger.debug("Executing: {}", readTypeStatement);
        ResultSet rs = readTypeStatement.executeQuery();
        if (rs.next()) {
            typeId = rs.getLong("id");
            logger.debug("Type: {}, id: {}", new Object[]{type, typeId});
        }
        return typeId;
    }

    /**
     * @param type       the object type URI
     * @param connection the DB connection
     * @return true if a type was inserted
     * @throws SQLException if the insert failed (e.g. concurrent insert by another thread)
     */
    boolean createTypeId(String type, Connection connection) throws SQLException {
        PreparedStatement createTypeStatement = getPreparedStatement(connection, QueryDefinition.CREATETYPEQUERYSTR);

        logger.debug("Create objecttype {}", type);
        createTypeStatement.setString(1, type);
        logger.debug("Executing: {}", createTypeStatement);
        int val = createTypeStatement.executeUpdate();

        return (val == 1);
    }

    /**
     * @return the row for the requested object, selected FOR UPDATE
     * @throws NotFoundException if the requested object was not found in the DB
     */
    public ResultSet readForUpdate(String fullId, String type, String localId, Connection connection)
            throws NotFoundException, SQLException, IOException {

        PreparedStatement readForUpdateStatement = getPreparedStatement(connection, QueryDefinition.READFORUPDATEQUERYSTR);

        logger.trace("Populating prepared statement {} for {}", readForUpdateStatement, fullId);
        readForUpdateStatement.setString(1, type);
        readForUpdateStatement.setString(2, localId);

        logger.debug("Executing: {}", readForUpdateStatement);
        ResultSet rs = readForUpdateStatement.executeQuery();
        if (rs.next()) {
            logger.debug("Read for update full id: {}", fullId);
            return rs;
        } else {
            throw new NotFoundException("Object " + fullId + " not found in " + type);
        }
    }

    /* (non-Javadoc)
    * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#update(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Map, java.sql.Connection)
    */
    @Override
    public void update(String fullId, String type, String localId, String rev, Map<String, Object> obj, Connection connection)
            throws SQLException, IOException, PreconditionFailedException, NotFoundException, InternalServerErrorException {
        logger.debug("Update with fullid {}", fullId);

        int revInt = Integer.parseInt(rev);
        ++revInt;
        String newRev = Integer.toString(revInt);
        obj.put("_rev", newRev); // Save the rev in the object, and return the changed rev from the create.

        ResultSet rs = readForUpdate(fullId, type, localId, connection);
        String existingRev = rs.getString("rev");
        long dbId = rs.getLong("id");
        long objectTypeDbId = rs.getLong("objecttypes_id");
        logger.debug("Update existing object {} rev: {} db id: {}, object type db id: {}", new Object[]{fullId, existingRev, dbId, objectTypeDbId});

        if (!existingRev.equals(rev)) {
            throw new PreconditionFailedException("Update rejected as current Object revision " + existingRev + " is different than expected by caller (" + rev + "), the object has changed since retrieval.");
        }
        PreparedStatement updateStatement = getPreparedStatement(connection, QueryDefinition.UPDATEQUERYSTR);
        PreparedStatement deletePropStatement = getPreparedStatement(connection, QueryDefinition.PROPDELETEQUERYSTR);

        // Support changing object identifier
        String newLocalId = (String) obj.get("_id");
        if (newLocalId != null && !localId.equals(newLocalId)) {
            logger.debug("Object identifier is changing from " + localId + " to " + newLocalId);
        } else {
            newLocalId = localId; // If it hasn't changed, use the existing ID
            obj.put("_id", newLocalId); // Ensure the ID is saved in the object
        }
        String objString = mapper.writeValueAsString(obj);

        logger.trace("Populating prepared statement {} for {} {} {} {} {}", new Object[]{updateStatement, fullId, newLocalId, newRev, objString, dbId});
        updateStatement.setString(1, newLocalId);
        updateStatement.setString(2, newRev);
        updateStatement.setString(3, objString);
        updateStatement.setLong(4, dbId);
        logger.debug("Update statement: {}", updateStatement);
        int updateCount = updateStatement.executeUpdate();
        logger.trace("Updated rows: {} for {}", updateCount, fullId);
        if (updateCount != 1) {
            throw new InternalServerErrorException("Update execution did not result in updating 1 row as expected. Updated rows: " + updateCount);
        }

        JsonNode node = new JsonNode(obj);
        // TODO: only update what changed?
        logger.trace("Populating prepared statement {} for {} {} {}", new Object[]{deletePropStatement, fullId, type, localId});
        deletePropStatement.setString(1, type);
        deletePropStatement.setString(2, localId);
        logger.debug("Update properties del statement: {}", deletePropStatement);
        int deleteCount = deletePropStatement.executeUpdate();
        logger.trace("Deleted child rows: {} for: {}", deleteCount, fullId);
        writeNodeProperties(fullId, dbId, localId, node, connection);

    }

    /* (non-Javadoc)
     * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#delete(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.sql.Connection)
     */
    @Override
    public void delete(String fullId, String type, String localId, String rev, Connection connection)
            throws PreconditionFailedException, InternalServerErrorException, NotFoundException, SQLException, IOException {
        logger.debug("Delete with fullid {}", fullId);

        // First check if the revision matches and select it for UPDATE
        ResultSet existing = null;
        try {
            existing = readForUpdate(fullId, type, localId, connection);
        } catch (NotFoundException ex) {
            throw new NotFoundException("Object does not exist for delete on: " + fullId);
        }
        String existingRev = existing.getString("rev");
        if (!rev.equals(existingRev)) {
            throw new PreconditionFailedException("Delete rejected as current Object revision " + existingRev + " is different than "
                    + "expected by caller " + rev + ", the object has changed since retrieval.");
        }

        // Proceed with the valid delete
        PreparedStatement deleteStatement = getPreparedStatement(connection, QueryDefinition.DELETEQUERYSTR);
        logger.trace("Populating prepared statement {} for {} {} {} {}", new Object[]{deleteStatement, fullId, type, localId, rev});

        // Rely on ON DELETE CASCADE for connected object properties to be deleted
        deleteStatement.setString(1, type);
        deleteStatement.setString(2, localId);
        deleteStatement.setString(3, rev);
        logger.debug("Delete statement: {}", deleteStatement);

        int deletedRows = deleteStatement.executeUpdate();
        logger.trace("Deleted {} rows for id : {} {}", deletedRows, localId);
        if (deletedRows < 1) {
            throw new InternalServerErrorException("Deleting object for " + fullId + " failed, DB reported " + deletedRows + " rows deleted");
        } else {
            logger.debug("delete for id succeeded: {} revision: {}", localId, rev);
        }
    }

    /* (non-Javadoc)
     * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#delete(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.sql.Connection)
     */
    @Override
    public List<Map<String, Object>> query(String type, Map<String, Object> params, Connection connection)
            throws ObjectSetException {
        return queries.query(type, params, connection);
    }

    @Override
    public String toString() {
        return "Generic handler mapped to [" + mainTableName + ", " + propTableName + "]";
    }

    protected PreparedStatement getPreparedStatement(Connection connection, QueryDefinition queryDefinition) throws SQLException {
        return queries.getPreparedStatement(connection, queryMap.get(queryDefinition));
    }
}
