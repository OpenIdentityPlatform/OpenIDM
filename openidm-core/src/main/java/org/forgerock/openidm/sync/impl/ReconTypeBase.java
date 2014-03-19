/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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
*
*/
package org.forgerock.openidm.sync.impl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;


import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.core.ServerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base class for reconciliation type handling
 *
 * @author aegloff
 */
public abstract class ReconTypeBase implements ReconTypeHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReconTypeBase.class);

    ReconciliationContext reconContext;
    final boolean runTargetPhase;
    final boolean allowEmptySourceSet;

    public ReconTypeBase(ReconciliationContext reconContext, boolean defaultRunTargetPhase) {
        this.reconContext = reconContext;
        this.allowEmptySourceSet = calcEffectiveConfig("allowEmptySourceSet").defaultTo(false).asBoolean();
        logger.debug("allowEmptySourceSet: {}", allowEmptySourceSet);
        this.runTargetPhase = calcEffectiveConfig("runTargetPhase").defaultTo(defaultRunTargetPhase).asBoolean();
        logger.debug("runTargetPhase: {}", runTargetPhase);
    }

    public boolean isRunTargetPhase() {
        return runTargetPhase;
    }
    
    public boolean allowEmptySourceSet() {
        return allowEmptySourceSet;
    }

    /**
     * Calculate the effective configuration for the given configuration property
     * Properties passed with the request body are given precedence, they override the default configuration
     * If not overridden by request body, the static configuration is used
     * @param configPropertyName The name of the configuration property
     * @param reconContext The reconciliation context
     * @return the effective configuration; may be null if neither an override or static configuration is provided
     */
    protected JsonValue calcEffectiveConfig(String configPropertyName) {
        JsonValue overridingConfig = reconContext.getOverridingConfig();
        // Precedence to config supplied in the request body
        JsonValue effectiveConfig = overridingConfig == null ? new JsonValue(null) : overridingConfig.get(configPropertyName);
        
        if (effectiveConfig.isNull()) {
            // Use regular configuration when not overridden in request body
            JsonValue cfg = reconContext.getObjectMapping().getConfig().get(configPropertyName);
            logger.debug("Using settings from mapping configuration for {} : {}", configPropertyName, cfg);
            return cfg;
        } else {
            logger.debug("Using settings supplied in call for {} : {}", configPropertyName, effectiveConfig);
        }
        return effectiveConfig;
    }

    /**
     * Calculate the effective query, taking into account config overrides in the request, as well
     * as defaults
     * @param queryConfigPropertyName The property name in the configuration for this query
     * @param mappingResource the resource name in the mapping that this query relates to.
     * Used to default the resource the query will be sent to
     * @return the effecive query
     */
    protected JsonValue calcEffectiveQuery(String queryConfigPropertyName, String mappingResource) {
        JsonValue queryCfg = calcEffectiveConfig(queryConfigPropertyName);

        if (queryCfg.isNull()) {
            queryCfg = new JsonValue(new LinkedHashMap());
        }
        // If not defined in the query config itself, default the query resource to the mapping source
        if (!queryCfg.isDefined("resourceName")) {
            queryCfg.put("resourceName", mappingResource);
            logger.debug("Default {} resource to query to {}", queryConfigPropertyName, mappingResource);
        }

        // If config doesn't explicitly specify the query, default to query all ids
        if (!specifiesQuery(queryCfg)) {
            queryCfg.put(QueryRequest.FIELD_QUERY_ID, ServerConstants.QUERY_ALL_IDS);
            logger.debug("Default {} query to {}", queryConfigPropertyName, ServerConstants.QUERY_ALL_IDS);
        }
        logger.debug("Effective query for {}: {}", queryConfigPropertyName, queryCfg);

        return queryCfg;
    }

    /**
     * @param queryCfg The effective query configuration
     * @return true if the effective query configuration explicitly defines the query to execute,
     * false if not
     */
    protected boolean specifiesQuery(JsonValue queryCfg) {
        // Check if there is a property that defines what query to execute
        boolean specifiesQuery =
                queryCfg.isDefined(QueryRequest.FIELD_QUERY_ID)
                || queryCfg.isDefined(QueryRequest.FIELD_QUERY_EXPRESSION)
                || queryCfg.isDefined(QueryRequest.FIELD_QUERY_FILTER)
                || queryCfg.isDefined("query");
        // OpenICF provisioner uses an inconsistent "query" param - to be deprecated

        if (logger.isDebugEnabled()) {
            if (specifiesQuery) {
                logger.debug("Explicit query was specified");
            } else {
                logger.debug("No explicit query specified");
            }
        }

        return specifiesQuery;
    }

    /**
     * Execute the specified query
     *
     * @param objectSet the object set to query
     * @param query the query parameters
     * @param collectionToPopulate the collection to populate with results
     * @param caseSensitive whether the collection should be populated in case
     * sensitive fashion, or if false it populates as lower case only
     * @return the collection of (unqualified) ids
     * @throws SynchronizationException if retrieving or processing the ids failed
     */
    protected Collection<String> query(final String objectSet, JsonValue query, final ReconciliationContext reconContext,
            Collection<String> collectionToPopulate, final boolean caseSensitive) throws SynchronizationException {
        final Collection<String> ids = collectionToPopulate;

        try {
            QueryRequest r = Requests.newQueryRequest(objectSet);
            r.setQueryId(query.get(QueryRequest.FIELD_QUERY_ID).asString());
            r.setQueryExpression(query.get(QueryRequest.FIELD_QUERY_EXPRESSION).asString());
            JsonValue queryFilter = query.get(QueryRequest.FIELD_QUERY_FILTER);
            if (!queryFilter.isNull()) {
                r.setQueryFilter(QueryFilter.valueOf(queryFilter.asString()));
            }
            for (Map.Entry<String, Object> e: query.asMap().entrySet()) {
                r.setAdditionalParameter(e.getKey(), String.valueOf(e.getValue()));
            }
            reconContext.getService().getConnectionFactory().getConnection().query(reconContext.getService().getRouter(), r,
                    new QueryResultHandler() {
                        @Override
                        public void handleError(ResourceException error) {
                            // ignore
                        }

                        @Override
                        public boolean handleResource(Resource resource) {
                            if (resource.getId() == null) {
                                // do not add null values to collection
                                logger.warn("Resource {0} id is null!", resource.toString());
                            }
                            else {
                                ids.add(
                                    caseSensitive
                                    ? resource.getId()
                                    : reconContext.getObjectMapping().getLinkType().normalizeId(resource.getId()));
                            }
                            return true;
                        }

                        @Override
                        public void handleResult(QueryResult result) {
                            //ignore
                        }
                    });
        } catch (JsonValueException jve) {
            throw new SynchronizationException(jve);
        } catch (ResourceException ose) {
            throw new SynchronizationException(ose);
        }

        reconContext.checkCanceled(); // Throws an exception if reconciliation was canceled
        return ids;
    }

    /**
     * @inheritDoc
     */
    public abstract List<String> querySourceIds() throws SynchronizationException;

    /**
     * @inheritDoc
     */
    public abstract List<String> queryTargetIds() throws SynchronizationException;

}
