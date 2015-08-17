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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openidm.audit.impl;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openidm.audit.impl.AuditLogFilters.JsonValueObjectConverter;
import org.forgerock.util.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A builder for AuditLogFilters.
 *
 */
class AuditLogFilterBuilder {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(AuditLogFilterBuilder.class);

    /**
     * Map of config element config paths to a builder function that creates the appropriate AuditLogFilter
     * from that config.
     */
    private final Map<String, JsonValueObjectConverter<AuditLogFilter>> auditLogFilterBuilder =
            new HashMap<String, JsonValueObjectConverter<AuditLogFilter>>();

    /**
     * Add a mapping from a config path to the factory function used to build an audit log filter for this
     * config fragment.
     *
     * @param configPath a JsonPointer to a config subset
     * @param filterFactory the function to create an AuditLogFilter from that config subset
     * @return this builder object
     */
    AuditLogFilterBuilder add(String configPath, JsonValueObjectConverter<AuditLogFilter> filterFactory) {
        auditLogFilterBuilder.put(configPath, filterFactory);
        return this;
    }

    /**
     * Remove the mapping for a given config path
     * @param configPath a JsonPointer to a config subset
     * @return this builder object
     */
    AuditLogFilterBuilder remove(String configPath) {
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
        List<AuditLogFilter> filters = new ArrayList<>();
        for (Map.Entry<String, JsonValueObjectConverter<AuditLogFilter>> entry : auditLogFilterBuilder.entrySet()) {
            final String configPath = entry.getKey();
            final Function<JsonValue, AuditLogFilter, JsonValueException> builder = entry.getValue();

            final JsonValue filterConfig;
            if (configPath.contains("*")) {
                // config path contains a wildcard - use special glob-processing
                filterConfig = getByGlob(config, configPath);
            } else {
                // config path is normal, treat as JsonPointer
                filterConfig = config.get(new JsonPointer(configPath));
            }

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
        return AuditLogFilters.newOrCompositeFilter(filters);
    }

    /**
     * Returns a JsonValue whose keys replace the elements matched by wildcard path elements and values are the
     * values at those keys.  A simple example:
     * <pre>
     *     value = {
     *         "outside" : {
     *             "temp" : {
     *                 "value" : 5,
     *                 "unit" : "fahrenheit"
     *             },
     *             "barometer" : {
     *                 "value" : 700
     *                 "unit" : "mmHg"
     *             }
     *         },
     *         "inside" : {
     *             "temp" : {
     *                 "value" : 30,
     *                 "unit" : "celsius"
     *             },
     *             "barometer" : {
     *                 "value" : 760
     *                 "unit" : "mmHg"
     *             }
     *         }
     *     }
     * </pre>
     * then
     * <pre>
     *     getByGlob(value, "*&#47;temp")
     * </pre>
     * will return
     * <pre>
     *     {
     *         "outside" : {
     *             "value" : 5,
     *             "unit" : "fahrenheit"
     *         },
     *         "inside" : {
     *             "value" : 30,
     *             "unit" : "celsius"
     *         }
     *     }
     * </pre>
     * Note how this picks out just the temp subvalues.  If multiple wildcards occur in the path, the result will
     * have multiple levels of keys following the wildcard expansion.
     *
     * @param value the JsonValue to extract path elements
     * @param path the JsonPointer-like path containing wildcards
     * @return a JsonValue map of matching keys to values a those keys
     */
    JsonValue getByGlob(final JsonValue value, final String path) {
        return getByGlob(value, path.split("/"));
    }

    /**
     * Match the JsonValue according to the path array.
     *
     * @param value the JsonValue to extract path elements
     * @param paths an array of path elements to search on
     * @return a JsonValue map of matching keys to values a those keys
    */
    JsonValue getByGlob(final JsonValue value, final String[] paths) {

        if (value.isNull()) {
            return json(object());
        }

        if (paths.length == 0) {
            return value;
        }

        final String path = paths[0];
        final String[] remainingPath = Arrays.copyOfRange(paths, 1, paths.length);

        if ("*".equals(path)) {
            // this path matches all "nodes" under 'value' - add all children who continue to match the subsequent path
            final JsonValue result = json(object());
            for (String key : value.keys()) {
                JsonValue child = getByGlob(value.get(key), remainingPath);
                if (child.size() > 0) {
                    result.put(key, child);
                }
            }
            return result;
        } else {
            // regular path element - get it (recursion)
            return getByGlob(value.get(path), remainingPath);
        }
    }
}
