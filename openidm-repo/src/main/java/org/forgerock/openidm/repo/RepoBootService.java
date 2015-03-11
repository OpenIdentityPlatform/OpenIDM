/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2013 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo;

import java.util.List;

import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;

/**
 * Common OpenIDM bootstrap repository interface
 * 
 * Implementing services provide basic connectivity and access 
 * to bootstrapping configuration
 * 
 * Every repository boot service must provide CRUD access to configuration,
 * And provide built in query support for queryid query-all-ids
 * 
 */
public interface RepoBootService {
    
    /**
     * Creates a new resource in the repository.
     *
     * @param request
     *            the create request
     * @return the created resource
     * @throws ResourceException
     *             if an error was encountered during creation
     */
    public Resource create(CreateRequest request) throws ResourceException;
    
    /**
     * Reads a resource from the repository based on the supplied read request.
     *
     * @param request
     *            the read request.
     * @return the requested resource.
     * @throws ResourceException
     *             if an error was encountered during the read.
     */
    public Resource read(ReadRequest request) throws ResourceException;
    
    /**
     * Updates a resource in the repository
     * <p/>
     * This implementation requires MVCC and hence enforces that clients state
     * what revision they expect to be updating
     * <p/>
     * If successful, this method updates metadata properties within the passed
     * object, including: a new {@code _rev} value for the revised object's
     * version
     *
     * @param request
     *            the update request
     * @return the updated resource
     * @throws ResourceException
     *             if an error was encountered while updating
     */
    public Resource update(UpdateRequest request) throws ResourceException;
    
    /**
     * Deletes a new resource in the repository.
     *
     * @param request
     *            the delete request
     * @return an empty resource
     * @throws ResourceException
     *             if an error was encountered during the delete
     */
    public Resource delete(DeleteRequest request) throws ResourceException;
    
    /**
     * Queries resources in the repository.  Must provide built in query support 
     * for queryid query-all-ids.
     *
     * @param request
     *            the query request
     * @return a list containing the query results
     * @throws ResourceException
     *             if an error was encountered during query
     */
    public List<Resource> query(QueryRequest request) throws ResourceException;
    
}
