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
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidm.audit.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.promise.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A builder for AuditLogFilters.
 *
 * @author brmiller
 */
class AuditLogFilterBuilder {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(AuditLogFilterBuilder.class);

    /**
     * Map of config element JsonPointers to a builder function that creates the appropriate AuditLogFilter
     * from that config.
     */
    private final Map<JsonPointer, Function<JsonValue, AuditLogFilter, Exception>> auditLogFilterBuilder =
            new HashMap<JsonPointer, Function<JsonValue, AuditLogFilter, Exception>>();

    /**
     * Add a mapping from a config path to the factory function used to build an audit log filter for this
     * config fragment.
     *
     * @param configPath a JsonPointer to a config subset
     * @param filterFactory the function to create an AuditLogFilter from that config subset
     * @return this builder object
     */
    AuditLogFilterBuilder add(JsonPointer configPath, Function<JsonValue, AuditLogFilter, Exception> filterFactory) {
        auditLogFilterBuilder.put(configPath, filterFactory);
        return this;
    }

    /**
     * Remove the mapping for a given config path
     * @param configPath a JsonPointer to a config subset
     * @return this builder object
     */
    AuditLogFilterBuilder remove(JsonPointer configPath) {
        auditLogFilterBuilder.remove(configPath);
        return this;
    }

    /**
     * Create the AuditLogFilter from the live configuration.
     *
     * @param config the config describe audit log filters
     * @return an AuditLogFilter
     */
    AuditLogFilter build(JsonValue config) {
        List<AuditLogFilter> filters = new ArrayList<AuditLogFilter>();
        for (Map.Entry<JsonPointer, Function<JsonValue, AuditLogFilter, Exception>> entry :
                auditLogFilterBuilder.entrySet()) {
            final JsonValue filterConfig = config.get(entry.getKey());
            final Function<JsonValue, AuditLogFilter, Exception> builder = entry.getValue();
            // if filterConfig is null, then we do not have this config
            if (filterConfig != null) {
                try {
                    filters.add(builder.apply(filterConfig));
                } catch (Exception e) {
                    logger.error("Audit Log Filter builder threw exception {} while processing {}",
                            e.getClass().getName(), filterConfig.toString(), e);
                }
            }
        }
        return AuditLogFilters.newCompositeActionFilter(filters);
    }
}
