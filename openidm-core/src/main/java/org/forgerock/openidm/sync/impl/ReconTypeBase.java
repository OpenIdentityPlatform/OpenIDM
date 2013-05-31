/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.sync.SynchronizationException;

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
    boolean runTargetPhase;

    public ReconTypeBase(ReconciliationContext reconContext, boolean defaultRunTargetPhase) {
        this.reconContext = reconContext;
        
        JsonValue runTargetPhaseCfg = calcEffectiveConfig("runTargetPhase");
        if (runTargetPhaseCfg.isNull()) {
            runTargetPhase = defaultRunTargetPhase;
        } else {
            runTargetPhase = runTargetPhaseCfg.asBoolean().booleanValue();
        }
        logger.debug("runTargetPhase: {}", runTargetPhase);
    }
    
    public boolean isRunTargetPhase() {
        return runTargetPhase;
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
        // Precedence to config supplied in the request body
        JsonValue effectiveConfig = reconContext.getReconParams().get("_entity").get(configPropertyName);
        if (effectiveConfig.isNull()) {
            // Use regular configuration when not overriden in request body
            JsonValue mappingCfg = reconContext.getObjectMapping().getConfig();
            effectiveConfig = mappingCfg.get(configPropertyName);
            logger.debug("Using settings from mapping configuration for {} : {}", configPropertyName, effectiveConfig);
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
            queryCfg.put(QueryConstants.QUERY_ID, QueryConstants.QUERY_ALL_IDS);
            logger.debug("Default {} query to {}", queryConfigPropertyName, QueryConstants.QUERY_ALL_IDS);
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
                queryCfg.isDefined(QueryConstants.QUERY_ID) 
                || queryCfg.isDefined(QueryConstants.QUERY_EXPRESSION)
                || queryCfg.isDefined(QueryConstants.QUERY_FILTER)
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
    protected Collection<String> query(final String objectSet, JsonValue query, ReconciliationContext reconContext, 
            Collection<String> collectionToPopulate, boolean caseSensitive) throws SynchronizationException {
        Collection<String> ids = collectionToPopulate;

        try {
            JsonValue objList = new JsonValue(reconContext.getService().getRouter().query(objectSet, query.asMap()))
                    .get(QueryConstants.QUERY_RESULT).required().expect(List.class);
            for (JsonValue obj : objList) {
                String value = obj.get("_id").asString();
                if (!caseSensitive) {
                    value = (value == null ? null : reconContext.getObjectMapping().getLinkType().normalizeId(value));
                }
                ids.add(value);
            }
        } catch (JsonValueException jve) {
            throw new SynchronizationException(jve);
        } catch (ObjectSetException ose) {
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
