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
package org.forgerock.openidm.repo.orientdb.impl.query;

import java.util.List;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.exception.OQueryParsingException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
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
     * @throws ResourceException if the passed identifier or type are invalid or error on execution
     */
    public ODocument getByID(final String id, final String type, ODatabaseDocumentTx database)
            throws ResourceException {

        String orientClassName = OrientDBRepoService.typeToOrientClassName(type);

        if (id == null) {
            throw new BadRequestException("Query by id the passed id was null.");
        } else if (type == null) {
            throw new BadRequestException("Query by id the passed type was null.");
        }

        // TODO: convert into a prepared statement
        OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(
                "select * from " + orientClassName + " where " + DocumentUtil.ORIENTDB_PRIMARY_KEY + " = '" + id + "'");

        ODocument first = null;
        try {
            List<ODocument> result = database.query(query);
            logger.trace("Query: {} Result: {}", query, result);
            if (result.size() > 0) {
                first = result.get(0); // ID is of type unique index, there must only be one at most
            }
        } catch (OQueryParsingException | OCommandExecutionException e) {
            logger.debug("The passed id: {} or type: {} is invalid.", id, type, e);
            throw new BadRequestException("Illegal query request.", e);
        } catch (Exception e) {
            logger.debug("Could not complete query for id: {} and type: {}.", id, type, e);
            throw new InternalServerErrorException("An error occurred processing the query request.", e);
        }
        return first;
    }
}