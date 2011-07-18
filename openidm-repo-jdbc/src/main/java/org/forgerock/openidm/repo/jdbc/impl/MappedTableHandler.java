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
import org.forgerock.openidm.config.InvalidException;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handling of tables in a generic (not object specific) layout
 * @author aegloff
 */
public class MappedTableHandler implements TableHandler {
    final static Logger logger = LoggerFactory.getLogger(MappedTableHandler.class);

    String tableName;
    String dbSchemaName;

    LinkedHashMap<String, String> mapping;
    // The names of the properties to replace the ? tokens in the prepared statement, 
    // in the order they need populating
    List<String> tokenReplacementPropNames = new ArrayList<String>();
    
    ObjectMapper mapper = new ObjectMapper();
    GenericTableQueries queries;

    String readQueryStr;
    String createQueryStr;
    String updateQueryStr;
    String deleteQueryStr;
    
    public MappedTableHandler(String tableName, Map mapping, String dbSchemaName, JsonNode queriesConfig) {
        this.tableName = tableName;
        this.dbSchemaName = dbSchemaName;
        // Maintain a stable ordering
        this.mapping = new LinkedHashMap<String, String>();
        this.mapping.putAll(mapping);
        
        queries = new GenericTableQueries();
        //TODO: replace with explicit table specific handling.
        queries.setConfiguredQueries(tableName, tableName, dbSchemaName, queriesConfig);
        tokenReplacementPropNames = new ArrayList<String>();
        
        readQueryStr = "SELECT * FROM " + dbSchemaName + "." + tableName + " WHERE objectid  = ?";
  // TODO: populate fields according to mapping      
        
        StringBuffer colNames = new StringBuffer();
        StringBuffer tokenNames = new StringBuffer();
        StringBuffer prepTokens = new StringBuffer();
        StringBuffer updateAssign = new StringBuffer();
        boolean isFirst = true;
        for (Map.Entry<String, String> entry : this.mapping.entrySet()) {
            Object value = entry.getValue();
            String colName = null;
            String colType = "STRING";
            if (value instanceof List) {
                List<String> colInfo = (List<String>) value;
                if (colInfo.size() != 2) {
                    throw new InvalidException("Explicit table mapping has invalid entry for " + entry.getKey() + ", expecting column name and type but contains " + value);
                }
                colName = colInfo.get(0);
                colType = colInfo.get(1);
            } else if (value instanceof String) {
                colName = (String) value;
            }
            if (!isFirst) {
                colNames.append(", ");
                tokenNames.append(",");
                prepTokens.append(",");
            }
            colNames.append(colName);
            tokenNames.append("${" + entry.getKey() + "}");
            prepTokens.append("?");
            tokenReplacementPropNames.add(entry.getKey());
            updateAssign.append(colName + " = ${" + entry.getKey() + "}");
            isFirst = false;
        }
        createQueryStr = "INSERT INTO " + dbSchemaName + "." + tableName + " (" + colNames + ") VALUES ( " + prepTokens + ")";
        updateQueryStr = "UPDATE " + dbSchemaName + "." + tableName + " SET " + updateAssign + " WHERE objectid = ?";
        deleteQueryStr = "DELETE FROM " + dbSchemaName + "." + tableName + " WHERE objectid = ? AND rev = ?";
        
        logger.debug("Unprepared query strings {} {} {} {} {}", new Object[] {readQueryStr, createQueryStr, updateQueryStr, deleteQueryStr});
        
    }
    
    /* (non-Javadoc)
     * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#read(java.lang.String, java.lang.String, java.lang.String, java.sql.Connection)
     */
    @Override
    public Map<String, Object> read(String fullId, String type, String localId, Connection connection) 
                    throws NotFoundException, SQLException, IOException {
        Map<String, Object> result = null;
        PreparedStatement readStatement = queries.getPreparedStatement(connection, readQueryStr);

        logger.debug("Populating prepared statement {} for {}", readStatement, fullId);
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
            logger.debug(" full id: {}, rev: {}, obj {}", new Object[] {fullId, rev, result});  
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
                throws SQLException, IOException {
        connection.setAutoCommit(false);

        PreparedStatement createStatement = queries.getPreparedStatement(connection, createQueryStr);

        logger.debug("Create with fullid {}", fullId);
        String rev = "0";
        obj.put("_id", localId); // Save the id in the object
        obj.put("_rev", rev); // Save the rev in the object, and return the changed rev from the create.
        String objString = mapper.writeValueAsString(obj);

        logger.debug("Preparing statement {} with {}, {}, {}, {}", 
                new Object[]{ createStatement, type, localId, rev, objString });
        int colPos = 1;
        for (String propName : tokenReplacementPropNames) {
         //   String propName = entry; // TODO: handle case where type info is added
         // TODO: JSON path/pointer instead
            Object rawValue = obj.get(propName);
            String propValue = null;
            if (rawValue instanceof String || rawValue == null) {
                propValue = (String) rawValue;
            } else {
                logger.warn("Value for col " + colPos + "  is not a STRING!!! " + rawValue.getClass() + " : " + rawValue);
                propValue = rawValue.toString(); // TODO: temp work-around
            }

            createStatement.setString(colPos, (String) propValue); 
            colPos++;
        }
        
        //createStatement.setString(1, type);
        //createStatement.setString(2, localId);
        //createStatement.setString(3, rev);
        //createStatement.setString(4, objString);
        logger.debug("Executing: {}", createStatement);
        int val = createStatement.executeUpdate();
        
        logger.debug("Created object for id {} with rev {}", fullId, rev);
        JsonNode node = new JsonNode(obj);
        //writeNodeProperties(fullId, type, localId, node, connection);
    }

   
    /* (non-Javadoc)
     * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#update(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Map, java.sql.Connection)
     */
    @Override
    public void update(String fullId, String type, String localId, String rev, Map<String, Object> obj, Connection connection) 
                throws SQLException, IOException {
        logger.info("Update with fullid {}", fullId);
        
        int revInt = Integer.parseInt(rev);
        ++revInt;
        String newRev = Integer.toString(revInt);
        obj.put("_rev", rev); // Save the rev in the object, and return the changed rev from the create.
     
        // TODO: should we support rename/updating id?
        obj.put("_id", localId); // Save the id in the object
        String objString = mapper.writeValueAsString(obj);
        
        // TODO: MVCC rev checking/handling
        
        PreparedStatement updateStatement = queries.getPreparedStatement(connection, updateQueryStr);
        //PreparedStatement deletePropStatement = queries.getPreparedStatement(connection, propDeleteQueryStr);
        updateStatement.setString(1, newRev);
        updateStatement.setString(2, objString);
        updateStatement.setString(3, type);
        updateStatement.setString(4, localId);
        logger.debug("Update statement: {}", updateStatement);
        int updateCount = updateStatement.executeUpdate();
        logger.info("Updated object id: {} rev: {} type: {} obj: {}", new Object[] {fullId, newRev, type,  objString});
        // TODO: do in transaction
        JsonNode node = new JsonNode(obj);
        // TODO: only update what changed?
        //deletePropStatement.setString(1, type);
        //deletePropStatement.setString(2, localId);
        //logger.debug("Update del statement: {}", deletePropStatement);
        //int deleteCount = deletePropStatement.executeUpdate();
        //writeNodeProperties(fullId, type, localId, node, connection);
        
        /* NVCC handling to resolve
        ODatabaseDocumentTx db = pool.acquire(dbURL, user, password);
        try{
            db.begin();
            ODocument existingDoc = predefinedQueries.getByID(localId, type, db);
            if (existingDoc == null) {
                throw new NotFoundException("Update on object " + fullId + " could not find existing object.");
            }
            ODocument updatedDoc = DocumentUtil.toDocument(obj, existingDoc, db, type);
            logger.trace("Updated doc for id {} to save {}", fullId, updatedDoc);
            updatedDoc.save();
            db.commit();

            obj.put(DocumentUtil.TAG_REV, Integer.toString(updatedDoc.getVersion()));
            logger.debug("update for id: {} saved doc: {}", fullId, updatedDoc);
        } catch (OConcurrentModificationException ex) {
            db.rollback();
            throw new PreconditionFailedException("Update rejected as current Object revision is different than expected by caller, the object has changed since retrieval: " + ex.getMessage(), ex);
        } catch (RuntimeException e){
            db.rollback();
            throw e;
        } finally {
            if (db != null) {
                db.close();
                pool.release(db);
            } 
        }
*/        
    }

    /* (non-Javadoc)
     * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#delete(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.sql.Connection)
     */ 
    @Override
    public void delete(String fullId, String type, String localId, String rev, Connection connection) 
                throws PreconditionFailedException, InternalServerErrorException, NotFoundException, SQLException, IOException {
        logger.info("Delete with fullid {}", fullId);

        PreparedStatement deleteStatement = queries.getPreparedStatement(connection, deleteQueryStr);
        
        // Rely on ON DELETE CASCADE for connected object properties to be deleted
        deleteStatement.setString(1, type);
        deleteStatement.setString(2, localId);
        deleteStatement.setString(3, rev);
        logger.debug("Delete statement: {}", deleteStatement);
               
        int deletedRows = deleteStatement.executeUpdate();
        if (deletedRows < 1) {
            Map<String, Object> existing = null;
            try {
                // TODO: do read, deletes in one tx.
                existing = read(fullId, type, localId, connection);
            } catch (NotFoundException ex) {
                throw new NotFoundException("Object does not exist for delete on: " + fullId);
            }
            String existingRev = (String) existing.get("_rev");
            if (!rev.equals(existingRev)) {
                throw new PreconditionFailedException("Delete rejected as current Object revision " + existingRev + " is different than " 
                        + "expected by caller " + rev + ", the object has changed since retrieval.");
            } else {
                // Without a transaction, this could happen if a concurrent insert/update created an object that originally was missing or had wrong rev
                throw new InternalServerErrorException("Deleting object failed, object for " + fullId + " revision " + rev + " still exists");
            }
        } else {
            logger.info("delete for id succeeded: {} revision: {}", localId, rev);
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
        return "Generic handler mapped to " + tableName + " and mapping " + mapping; 
    }
}