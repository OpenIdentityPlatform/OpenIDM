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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openidm.sync.impl;

import static org.forgerock.json.JsonValue.json;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.util.RequestUtil;
import org.forgerock.openidm.util.Script;
import org.forgerock.openidm.util.Scripts;
import org.forgerock.script.exception.ScriptThrownException;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class used to store and execute correlation queries and scripts.
 */
class Correlation {

    private static final Logger LOGGER = LoggerFactory.getLogger(Correlation.class);

    private enum CorrelationType {
        correlationQuery,
        correlationScript,
        none
    }

    /**
     * A Map of correlation queries where the keys are {@link String} instances representing link qualifiers and the
     * values are the correlation query {@link Script} instances.
     */
    private Map<String, Script> correlationQueries;

    /**
     * A correlation script which will return a Map object where the keys are {@link String} instances representing link qualifiers and the
     * values are the correlation query results.
     */
    private Script correlationScript;

    /**
     * The type of the correlation
     */
    private final CorrelationType type;

    private final ObjectMapping objectMapping;

    /**
     * Constructor.
     *
     * @param objectMapping the mapping
     * @throws JsonValueException
     */
    public Correlation(final ObjectMapping objectMapping) throws JsonValueException {
        this.objectMapping = Reject.checkNotNull(objectMapping);

        JsonValue config = objectMapping.getConfig();
        JsonValue correlationQueryValue = config.get("correlationQuery");
        JsonValue correlationScriptValue = config.get("correlationScript");
        if (!correlationQueryValue.isNull() && !correlationScriptValue.isNull()) {
            throw new JsonValueException(config, "Cannot configure both correlationQuery and correlationScript in a single mapping");
        } else if (!correlationQueryValue.isNull()) {
            correlationQueries = new HashMap<>();
            type = CorrelationType.correlationQuery;
            if (correlationQueryValue.isList()) {
                for (JsonValue correlationQuery : correlationQueryValue) {
                    correlationQueries.put(correlationQuery.get("linkQualifier").defaultTo(Link.DEFAULT_LINK_QUALIFIER).asString(),
                            Scripts.newScript(correlationQuery));
                }
            } else if (correlationQueryValue.isMap()) {
                correlationQueries.put(correlationQueryValue.get("linkQualifier").defaultTo(Link.DEFAULT_LINK_QUALIFIER).asString(),
                        Scripts.newScript(correlationQueryValue));
            }
        } else if (!correlationScriptValue.isNull()) {
            type = CorrelationType.correlationScript;
            correlationScript = Scripts.newScript(correlationScriptValue);
        } else {
            type = CorrelationType.none;
        }
    }

    /**
     * Returns true if there is a correlation query or script configured for the given link qualifier, false otherwise.
     *
     * @param linkQualifier the link qualifier for the current sync operation.
     * @return true if correlation is configured, fals otherwise.
     */
    public boolean hasCorrelation(String linkQualifier) {
        switch (type) {
        case correlationQuery:
            return correlationQueries.get(linkQualifier) != null;
        case correlationScript:
            return true;
        default:
            return false;
        }
    }

    /**
     * Performs the correlation.
     *
     * @param scope the scope to use for the correlation script
     * @param linkQualifier the link qualifier
     * @param context Context
     * @param reconContext Recon context or {@code null}
     * @return a list of results if no correlation is configured
     * @throws SynchronizationException if there was an error during correlation
     */
    public JsonValue correlate(Map<String, Object> scope, String linkQualifier, Context context,
            ReconciliationContext reconContext) throws SynchronizationException {
        // Set the link qualifier in the script's scope
        scope.put("linkQualifier", linkQualifier);
        final long startNanoTime = ObjectMapping.startNanoTime(reconContext, type != CorrelationType.none);
        try {
            switch (type) {
            case correlationQuery:
                // Execute the correlationQuery and return the results
                return json(queryTargetObjectSet(execScript(type.toString(),  correlationQueries.get(linkQualifier),
                        scope, context).asMap())).get(QueryResponse.FIELD_RESULT).required();
            case correlationScript:
                // Execute the correlationScript and return the results corresponding to the given linkQualifier
                return execScript(type.toString(), correlationScript, scope, context);
            default:
                return null;
            }
        } catch (ScriptThrownException ste) {
            String errorMessage = objectMapping.getName() + " " + type + " script encountered exception";
            LOGGER.debug(errorMessage, ste);
            throw new SynchronizationException(ste.toResourceException(ResourceException.INTERNAL_ERROR, errorMessage));
        } catch (ScriptException se) {
            LOGGER.debug("{} {} script encountered exception", objectMapping.getName(), type.toString(), se);
            throw new SynchronizationException(se);
        } finally {
            ObjectMapping.addDuration(reconContext, ReconciliationStatistic.DurationMetric.valueOf(type.toString()), startNanoTime);
        }
    }

    /**
     * Executes a script of a given type with the given scope.
     *
     * @param type the type of script (correlationQuery or correlationScript)
     * @param script the {@link Script} object representing the script
     * @param scope the script's scope
     * @return A {@link Map} representing the results
     * @throws ScriptException if there was an error during execution
     */
    private JsonValue execScript(String type, Script script, Map<String, Object> scope, Context context)
            throws ScriptException {
        Object results = script.exec(scope, context);
        return json(results);
    }

    private Map<String, Object> queryTargetObjectSet(Map<String, Object> queryParameters)
            throws SynchronizationException {
        try {
            Map<String, Object> result = new HashMap<>(1);
            final Collection<Object> list = new ArrayList<>();
            result.put(QueryResponse.FIELD_RESULT, list);

            QueryRequest request = RequestUtil.buildQueryRequestFromParameterMap(objectMapping.getTargetObjectSet(),
                    queryParameters);
            objectMapping.getConnectionFactory().getConnection().query(ObjectSetContext.get(), request,
                    new QueryResourceHandler() {
                        @Override
                        public boolean handleResource(ResourceResponse resource) {
                            list.add(resource.getContent().asMap());
                            return true;
                        }
                    });
            return result;
        } catch (ResourceException ose) {
            throw new SynchronizationException(ose);
        }
    }
}
