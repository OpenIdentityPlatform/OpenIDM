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
package org.forgerock.openidm.repo;

import java.util.List;

import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;

// JSON Resource
//import org.forgerock.json.resource.JsonResource;

/**
 * Common OpenIDM bootstrap repository interface
 * 
 * Implementing services provide basic connectivity and access 
 * to bootstrapping configuration
 * 
 * Every repository boot service must provide CRUD access to configuration,
 * And provide built in query support for queryid query-all-ids
 * 
 * @author aegloff
 */
public interface RepoBootService {
	
	public Resource create(CreateRequest request) throws ResourceException;
	
	public Resource read(ReadRequest request) throws ResourceException;
	
	public Resource update(UpdateRequest request) throws ResourceException;
	
	public Resource delete(DeleteRequest request) throws ResourceException;
	
	public List<Resource> query(QueryRequest request) throws ResourceException;
	
}
