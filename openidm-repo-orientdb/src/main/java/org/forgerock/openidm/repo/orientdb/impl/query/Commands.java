/*
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
 * Copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.openidm.repo.orientdb.impl.query;

import java.util.HashMap;
import java.util.Map;

import com.orientechnologies.common.exception.OException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.repo.orientdb.impl.OrientDBRepoService;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OQueryParsingException;
import com.orientechnologies.orient.core.sql.OCommandSQL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Configured and add-hoc command support on OrientDB
 * 
 * Commands can contain tokens of the format ${token-name}
 * 
 */
public class Commands extends ConfiguredQueries<OCommandSQL, ActionRequest, Integer> {

    final static Logger logger = LoggerFactory.getLogger(Commands.class);

    private static final String COMMAND_ID = "commandId";
    private static final String COMMAND_EXPRESSION = "commandExpression";

    public Commands() {
        super(new HashMap<String, QueryInfo<OCommandSQL>>());
    }

    /**
     * Create an SQL command to execute the command query.
     *
     * @param queryString the query expression, including tokens to replace
     * @return the prepared query object
     */
    protected OCommandSQL createQueryObject(String queryString) {
        return new OCommandSQL(queryString);
    }

    /**
     * Execute a command, either a pre-configured command by using the command ID, or a command expression passed as
     * part of the params.
     *
     * The keys for the input parameters as well as the return map entries are in QueryConstants.
     *
     * @param type the relative/local resource name, which needs to be converted to match the OrientDB document class name
     * @param request the query request, including parameters which include the query id, or the query expression, as well as the
     *        token key/value pairs to replace in the query
     * @param database a handle to a database connection instance for exclusive use by the query method whilst it is executing.
     * @return The query result, which includes meta-data about the query, and the result set itself.
     * @throws org.forgerock.json.resource.BadRequestException if the passed request parameters are invalid, e.g. missing query id or query expression or tokens.
     */
    public Integer query(final String type, final ActionRequest request, final ODatabaseDocumentTx database)
            throws BadRequestException {

        final Map<String, String> params = new HashMap<String, String>(request.getAdditionalParameters());
        params.put(QueryConstants.RESOURCE_NAME, OrientDBRepoService.typeToOrientClassName(type));

        if (params.get(COMMAND_ID) == null && params.get(COMMAND_EXPRESSION) == null) {
            throw new BadRequestException("Either " + COMMAND_ID + " or " + COMMAND_EXPRESSION
                    + " to identify/define a command must be passed in the parameters. " + params);
        }

        final QueryInfo<OCommandSQL> queryInfo;
        try {
            queryInfo = findQueryInfo(type, params.get(COMMAND_ID), params.get(COMMAND_EXPRESSION));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("The passed command identifier " + params.get(COMMAND_ID)
                    + " does not match any configured commands on the OrientDB repository service.");
        }

        Integer result = null;

        logger.debug("Evaluate command {}", queryInfo.getQueryString());
        Name eventName = getEventName(params.get(COMMAND_ID), params.get(COMMAND_EXPRESSION));
        EventEntry measure = Publisher.start(eventName, queryInfo, null);

        try {
            result = doTokenSubsitutionQuery(queryInfo, params, database);
            measure.setResult(result);
            return result;
        } catch (OQueryParsingException firstTryEx) {
            // TODO: consider differentiating between bad configuration and bad request
            throw new BadRequestException("Failed to resolve and parse the command "
                    + queryInfo.getQueryString() + " with params: " + params, firstTryEx);
        } catch (IllegalArgumentException ex) {
            // TODO: consider differentiating between bad configuration and bad request
            logger.debug("Command is invalid: {} {}", queryInfo.getQueryString(), ex.getMessage(), ex);
            throw new BadRequestException("Command is invalid.");
        } catch (OException ex) {
            logger.debug("Error executing DB command {} {}", queryInfo.getQueryString(), ex.getMessage(), ex);
            throw new BadRequestException("Error executing DB command.");
        } catch (RuntimeException ex) {
            logger.warn("Unexpected failure during DB command: {}", ex.getMessage());
            throw ex;
        } finally {
            measure.end();
        }
    }

}
