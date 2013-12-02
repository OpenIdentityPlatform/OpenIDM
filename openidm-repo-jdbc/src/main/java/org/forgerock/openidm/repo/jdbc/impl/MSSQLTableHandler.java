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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.repo.jdbc.SQLExceptionHandler;
import static org.forgerock.openidm.repo.jdbc.impl.GenericTableHandler.logger;

public class MSSQLTableHandler extends GenericTableHandler {

    public MSSQLTableHandler(JsonValue tableConfig, String dbSchemaName, JsonValue queriesConfig,
            int maxBatchSize, SQLExceptionHandler sqlExceptionHandler) {
        super(tableConfig, dbSchemaName, queriesConfig, maxBatchSize, sqlExceptionHandler);
    }

    protected Map<QueryDefinition, String> initializeQueryMap() {
        Map<QueryDefinition, String> result = super.initializeQueryMap();
        String typeTable = dbSchemaName == null ? "objecttypes" : dbSchemaName + ".objecttypes";
        String mainTable =
                dbSchemaName == null ? mainTableName : dbSchemaName + "." + mainTableName;
        String propertyTable =
                dbSchemaName == null ? propTableName : dbSchemaName + "." + propTableName;

        result.put(
                QueryDefinition.READFORUPDATEQUERYSTR,
                "SELECT obj.* FROM "
                        + mainTable
                        + " obj INNER JOIN "
                        + typeTable
                        + " objtype ON obj.objecttypes_id = objtype.id AND objtype.objecttype = ? WHERE obj.objectid = ?");
        result.put(QueryDefinition.UPDATEQUERYSTR,
                "UPDATE obj SET obj.objectid = ?, obj.rev = ?, obj.fullobject = ? FROM "
                        + mainTable + " obj WHERE obj.id = ? AND obj.rev = ?");

        return result;

    }
    
    /* (non-Javadoc)
    * @see org.forgerock.openidm.repo.jdbc.impl.TableHandler#update(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.Map, java.sql.Connection)
    */
    @Override
    public void update(String fullId, String type, String localId, String rev, Map<String, Object> obj, Connection connection)
            throws SQLException, IOException, org.forgerock.json.resource.PreconditionFailedException, org.forgerock.json.resource.NotFoundException, org.forgerock.json.resource.InternalServerErrorException {
        logger.debug("Update with fullid {}", fullId);

        int revInt = Integer.parseInt(rev);
        ++revInt;
        String newRev = Integer.toString(revInt);
        obj.put("_rev", newRev); // Save the rev in the object, and return the changed rev from the create.

        ResultSet rs = null;
        PreparedStatement updateStatement = null;
        PreparedStatement deletePropStatement = null;
        try {
            rs = readForUpdate(fullId, type, localId, connection);
            String existingRev = rs.getString("rev");
            long dbId = rs.getLong("id");
            long objectTypeDbId = rs.getLong("objecttypes_id");
            logger.debug("Update existing object {} rev: {} db id: {}, object type db id: {}", new Object[]{fullId, existingRev, dbId, objectTypeDbId});

            if (!existingRev.equals(rev)) {
                throw new org.forgerock.json.resource.PreconditionFailedException("Update rejected as current Object revision " + existingRev + " is different than expected by caller (" + rev + "), the object has changed since retrieval.");
            }
            updateStatement = getPreparedStatement(connection, QueryDefinition.UPDATEQUERYSTR);
            deletePropStatement = getPreparedStatement(connection, QueryDefinition.PROPDELETEQUERYSTR);

            // Support changing object identifier
            String newLocalId = (String) obj.get("_id");
            if (newLocalId != null && !localId.equals(newLocalId)) {
                logger.debug("Object identifier is changing from " + localId + " to " + newLocalId);
            } else {
                newLocalId = localId; // If it hasn't changed, use the existing ID
                obj.put("_id", newLocalId); // Ensure the ID is saved in the object
            }
            String objString = mapper.writeValueAsString(obj);

            logger.trace("Populating prepared statement {} for {} {} {} {} {} {}", new Object[]{updateStatement, fullId, newLocalId, newRev, objString, dbId, existingRev});
            updateStatement.setString(1, newLocalId);
            updateStatement.setString(2, newRev);
            updateStatement.setString(3, objString);
            updateStatement.setLong(4, dbId);
            updateStatement.setString(5, existingRev);
            logger.debug("Update statement: {}", updateStatement);
            int updateCount = updateStatement.executeUpdate();
            logger.trace("Updated rows: {} for {}", updateCount, fullId);
            if (updateCount == 0) {
                throw new org.forgerock.json.resource.PreconditionFailedException("Update rejected as current Object revision " + existingRev + ", has changed since retrieval.");
            } else if (updateCount > 1) {
                throw new org.forgerock.json.resource.InternalServerErrorException("Update execution did not result in updating 1 row as expected. Updated rows: " + updateCount);
            }

            JsonValue jv = new JsonValue(obj);
            // TODO: only update what changed?
            logger.trace("Populating prepared statement {} for {} {} {}", new Object[]{deletePropStatement, fullId, type, localId});
            deletePropStatement.setString(1, type);
            deletePropStatement.setString(2, localId);
            logger.debug("Update properties del statement: {}", deletePropStatement);
            int deleteCount = deletePropStatement.executeUpdate();
            logger.trace("Deleted child rows: {} for: {}", deleteCount, fullId);
            writeValueProperties(fullId, dbId, localId, jv, connection);
        } finally {
            if (rs != null) {
                // Ensure associated statement also is closed
                Statement rsStatement = rs.getStatement();
                CleanupHelper.loggedClose(rs);
                CleanupHelper.loggedClose(rsStatement);
            }
            CleanupHelper.loggedClose(updateStatement);
            CleanupHelper.loggedClose(deletePropStatement);
        }
    }    
}
