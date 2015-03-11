/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2014 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.orientdb.impl.query;

import java.util.List;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import org.forgerock.json.resource.BadRequestException;
import org.forgerock.openidm.repo.orientdb.impl.DocumentUtil;
import org.forgerock.openidm.repo.orientdb.impl.OrientDBRepoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Queries pre-defined by the system
 * 
 */
public class PredefinedQueries {

    final static Logger logger = LoggerFactory.getLogger(PredefinedQueries.class);
    
    /**
     * Query by primary key, the OpenIDM identifier. This identifier is different from the OrientDB internal record id.
     * 
     * @param id the OpenIDM identifier for an object
     * @param type the OrientDB class
     * @param database a handle to the OrientDB database object. No other thread must operate on this concurrently.
     * @return The ODocument if found, null if not found.
     * @throws BadRequestException if the passed identifier or type are invalid
     */
    public ODocument getByID(final String id, final String type, ODatabaseDocumentTx database) throws BadRequestException {

        String orientClassName = OrientDBRepoService.typeToOrientClassName(type);
        
        if (id == null) {
            throw new BadRequestException("Query by id the passed id was null.");
        } else if (type == null) {
            throw new BadRequestException("Query by id the passed type was null.");
        }
        
        // TODO: convert into a prepared statement
        OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(
                "select * from " + orientClassName + " where " + DocumentUtil.ORIENTDB_PRIMARY_KEY + " = '" + id + "'");
        List<ODocument> result = database.query(query);
        logger.trace("Query: {} Result: {}", query, result);
        ODocument first = null;
        if (result.size() > 0) {
            first = result.get(0); // ID is of type unique index, there must only be one at most
        }
        return first;
    }
}
