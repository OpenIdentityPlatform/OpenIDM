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

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;

import org.codehaus.jackson.map.ObjectMapper;

import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.repo.RepositoryService; 
import org.forgerock.openidm.repo.jdbc.TableHandler;
import org.forgerock.openidm.repo.jdbc.impl.query.GenericTableQueries;

/**
 * Handling of tables in a generic (not object specific) layout
 * @author aegloff
 */
public class GenericTableHandler implements TableHandler {
    final static Logger logger = LoggerFactory.getLogger(GenericTableHandler.class);

    String mainTableName;
    String propTableName;
    String dbSchemaName;
    
    ObjectMapper mapper = new ObjectMapper();
    GenericTableQueries queries;

    String readQueryStr;
    String createQueryStr;
    String updateQueryStr;
    String deleteQueryStr;
    String propCreateQueryStr;
    String propDeleteQueryStr;
    
    public GenericTableHandler(String mainTableName, String propTableName, String dbSchemaName, JsonNode queriesConfig) {
        this.mainTableName = mainTableName;
        this.propTableName = propTableName;
        this.dbSchemaName = dbSchemaName;
        
        queries = new GenericTableQueries();
        queries.setConfiguredQueries(mainTableName, propTableName, dbSchemaName, queriesConfig);

        readQueryStr = "SELECT rev, fullobject FROM " + dbSchemaName + "." + mainTableName + " WHERE type = ? AND openidmid  = ?";
        createQueryStr = "INSERT INTO " + dbSchemaName + "." + mainTableName + " (type, openidmid, rev, fullobject) VALUES (?,?,?,?)";
        updateQueryStr = "UPDATE " + dbSchemaName + "." + mainTableName + " SET rev = ? , fullobject = ? WHERE type = ? AND openidmid = ?";
        deleteQueryStr = "DELETE FROM " + dbSchemaName + "." + mainTableName + " WHERE type = ? AND openidmid = ? AND rev = ?";

        propCreateQueryStr = "INSERT INTO " + dbSchemaName + "." + propTableName + " ( " + mainTableName + "_type, " + mainTableName + "_openidmid, propkey, proptype, propvalue) VALUES (?,?,?,?,?)";
        propDeleteQueryStr = "DELETE FROM " + dbSchemaName + "." + propTableName + " WHERE " + mainTableName + "_type = ? AND " + mainTableName + "_openidmid = ?";
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
        if (rs.first()) {
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

        logger.info("Create with fullid {}", fullId);
        String rev = "0";
        obj.put("_id", localId); // Save the id in the object
        obj.put("_rev", rev); // Save the rev in the object, and return the changed rev from the create.
        String objString = mapper.writeValueAsString(obj);

        logger.info("Prepared statement {} with params {}, {}, {}, {}", 
                new Object[]{ createStatement, type, localId, rev, objString });
        createStatement.setString(1, type);
        createStatement.setString(2, localId);
        createStatement.setString(3, rev);
        createStatement.setString(4, objString);
        logger.info("Executing: {}", createStatement);
        int val = createStatement.executeUpdate();
        
        logger.info("Created object for id {} with rev {}", fullId, rev);
        JsonNode node = new JsonNode(obj);
        writeNodeProperties(fullId, type, localId, node, connection);
    }

    void writeNodeProperties(String fullId, String type, String localId, JsonNode node, Connection connection) throws SQLException {
        
        PreparedStatement propCreateStatement = queries.getPreparedStatement(connection, propCreateQueryStr);
        
        for (JsonNode entry : node) {
            String propkey = entry.getPath().toString();
            if (entry.isMap() || entry.isList()) {
                writeNodeProperties(fullId, type, localId, entry, connection);
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
                logger.debug("Prepared statement {} with params {}, {}, {}, {}, {}", 
                        new Object[]{propCreateStatement, type, localId, propkey, proptype, propvalue});
                propCreateStatement.setString(1, type);
                propCreateStatement.setString(2, localId);
                propCreateStatement.setString(3, propkey);
                propCreateStatement.setString(4, proptype);
                propCreateStatement.setString(5, propvalue);
                logger.debug("Executing: {}", propCreateStatement);
                int val2 = propCreateStatement.executeUpdate();
                logger.debug("Created objectproperty id: {} propkey: {} proptype: {}, propvalue: {}", new Object[] {fullId, propkey, proptype, propvalue});
            }
        }
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
        PreparedStatement deletePropStatement = queries.getPreparedStatement(connection, propDeleteQueryStr);
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
        deletePropStatement.setString(1, type);
        deletePropStatement.setString(2, localId);
        logger.debug("Update del statement: {}", deletePropStatement);
        int deleteCount = deletePropStatement.executeUpdate();
        writeNodeProperties(fullId, type, localId, node, connection);
        
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
        return "Generic handler mapped to [" + mainTableName + ", " + propTableName + "]"; 
    }
}