/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2015 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.query.QueryFilter;

public interface TableHandler {

    /**
     * Gets an object from the repository by identifier. The returned object is not validated 
     * against the current schema and may need processing to conform to an updated schema.
     * <p>
     * The object will contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} to enable optimistic concurrency
     *
     * @param fullId the qualified identifier of the object to retrieve from the object set.
     * @param type is the qualifier of the object to retrieve
     * @param localId the identifier without the qualifier of the object to retrieve
     * @param connection
     * @throws NotFoundException if the specified object could not be found.
     * @throws SQLException if a DB failure was reported
     * @throws IOException if a failure to convert the JSON model was reported
     * @throws InternalServerErrorException if the operation failed because of a (possibly transient) failure
     * @return the requested object.
     */
    public abstract ResourceResponse read(String fullId, String type,
            String localId, Connection connection) 
            throws SQLException, IOException, ResourceException;

    /**
     * Creates a new object in the object set.
     * <p>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version (For optimistic concurrency)
     *
     * @param fullId the client-generated identifier to use, or {@code null} if server-generated identifier is requested.
     * @param type
     * @param localId
     * @param obj the contents of the object to create in the object set.
     * @param connection
     * @throws NotFoundException if the specified id could not be resolved.
     * @throws ForbiddenException if access to the object or object set is forbidden.
     * @throws PreconditionFailedException if an object with the same ID already exists.
     * @throws InternalServerErrorException if the operation failed because of a (possibly transient) failure
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     */
    public abstract void create(String fullId, String type, String localId,
            Map<String, Object> obj, Connection connection)
            throws SQLException, IOException, ResourceException;

    /**
     * Updates the specified object in the object set. 
     * <p>
     * This implementation requires MVCC and hence enforces that clients state what revision they expect 
     * to be updating
     * 
     * If successful, this method updates metadata properties within the passed object,
     * including: a new {@code _rev} value for the revised object's version
     *
     * @param fullId the identifier of the object to be put, or {@code null} to request a generated identifier.
     * @param type
     * @param localId
     * @param rev the version of the object to update; or {@code null} if not provided.
     * @param obj the contents of the object to put in the object set.
     * @param connection
     * @throws ConflictException if version is required but is {@code null}.
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     * @throws BadRequestException if the passed identifier is invalid
     * @throws InternalServerErrorException if the operation failed because of a (possibly transient) failure
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     */
    public abstract void update(String fullId, String type, String localId,
            String rev, Map<String, Object> obj, Connection connection)
            throws SQLException, IOException, ResourceException;

    /**
     * Deletes the specified object from the object set.
     *
     * @param fullId the identifier of the object to be deleted.
     * @param type
     * @param localId
     * @param rev the version of the object to delete or {@code null} if not provided.
     * @param connection
     * @throws NotFoundException if the specified object could not be found.
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws ConflictException if version is required but is {@code null}.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     * @throws InternalServerErrorException if the operation failed because of a (possibly transient) failure
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     */
    public abstract void delete(String fullId, String type, String localId,
            String rev, Connection connection)
            throws SQLException, IOException, ResourceException;

    /**
     * Performs the query on the specified object and returns the associated results.
     * <p>
     * Queries are parametric; a set of named parameters is provided as the query criteria.
     * The query result is a JSON object structure composed of basic Java types. 
     * 
     * The returned map is structured as follow: 
     * - The top level map contains meta-data about the query, plus an entry with the actual result records.
     * - The <code>QueryConstants</code> defines the map keys, including the result records (QUERY_RESULT)
     *
     * @param type identifies the object to query.
     * @param params the parameters of the query to perform.
     * @param connection
     * @return the query results, which includes meta-data and the result records in JSON object structure format.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws BadRequestException if the specified params contain invalid arguments, e.g. a query id that is not
     * configured, a query expression that is invalid, or missing query substitution tokens.
     * @throws ForbiddenException if access to the object or specified query is forbidden.
     * @throws InternalServerErrorException if the operation failed because of a (possibly transient) failure
     * @throws java.sql.SQLException
     */
    public List<Map<String, Object>> query(String type, Map<String, Object> params, Connection connection)
                throws SQLException, ResourceException;

    /**
     * Performs the command on the specified target and returns the number of affected objects
     * <p>
     * Commands are parametric; a set of named parameters is provided as the query criteria.
     * The command returns the number of records altered/updated/deleted.
     *
     * @param type identifies the object to query.
     * @param params the parameters of the query to perform.
     * @param connection
     * @return the number of records affected.
     * @throws BadRequestException if the specified params contain invalid arguments, e.g. a query id that is not
     * configured, a query expression that is invalid, or missing query substitution tokens.
     * @throws ForbiddenException if access to the object or specified query is forbidden.
     * @throws InternalServerErrorException if the operation failed because of a (possibly transient) failure
     * @throws java.sql.SQLException
     */

    public Integer command(String type, Map<String, Object> params, Connection connection)
            throws SQLException, ResourceException;

    /**
     * Check if a given queryId exists in our set of known queries
     *
     * @param queryId Identifier for the query
     *
     * @return true if queryId is available
     */
    public boolean queryIdExists(final String queryId);
    
    /**
     * Builds a raw query from the supplied filter.
     * 
     * @param filter the query filter
     * @param replacementTokens a map to store any replacement tokens
     * @param params a map containing query parameters
     * @return the raw query String
     */
    public String renderQueryFilter(QueryFilter<JsonPointer> filter, Map<String, Object> replacementTokens, Map<String, Object> params);
    
    /**
     * Query if a given exception signifies a well known error type
     * 
     * Allows table handlers to abstract database specific differences in reporting errors.
     * 
     * @param ex The exception thrown by the database
     * @param errorType the error type to test against
     * @return true if the exception matches the error type passed
     */
    public boolean isErrorType(SQLException ex, ErrorType errorType);
    
    /**
     * As whether a given exception should be retried
     * @param ex the exception thrown by the database
     * @param connection where the failure occured, used for additional context
     * @return
     */
    public boolean isRetryable(SQLException ex, Connection connection);
}