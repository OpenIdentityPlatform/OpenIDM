/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.sync.impl;

import javax.script.ScriptException;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryFilterVisitor;
import org.forgerock.openidm.sync.impl.Scripts.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a condition on which a property mapping may be applied, or a policy may be enforced.
 */
class Condition {
    
    /** 
     * Logger 
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(Condition.class);
    
    /**
     * The types of conditions.
     */
    private enum Type {
        /**
         * A condition evaluated by a script.
         */
        SCRIPTED,
        /**
         * A condition evaluated by a matching "queryFilter".
         */
        QUERY_FILTER,
        /**
         * A condition which always passes. This is used if a null configuration is passed in.
         */
        TRUE
    }

    /**
     * This condition's type
     */
    private Type type;
    
    /**
     * The query filter if configured
     */
    private QueryFilter queryFilter;
    
    /**
     * The condition script if configured
     */
    private Script script;
    
    /**
     * The constructor.
     * 
     * @param config the condition configuration
     */
    public Condition(JsonValue config) {
        if (config.isNull()) {
            init(Type.TRUE, null, null);
        } else if (config.isString()) {
            init(Type.QUERY_FILTER, QueryFilter.valueOf(config.asString()), null);
        } else {
            init(Type.SCRIPTED, null, Scripts.newInstance(config));
        }
    }
    
    /**
     * Initializes the condition fields.
     * 
     * @param type the conditions type.
     * @param queryFilter the query filter.
     * @param script the condition script.
     */
    private void init(Type type, QueryFilter queryFilter, Script script) {
        this.type = type;
        this.queryFilter = queryFilter;
        this.script = script;
    }
    
    /**
     * Evaluates the condition.  Returns true if the condition is met, false otherwise.
     * 
     * @param params parameters to use during evaluation.
     * @return true if the condition is met, false otherwise.
     * @throws SynchronizationException if errors are encountered.
     */
    public boolean evaluate(JsonValue params)
            throws SynchronizationException {
        switch (type) {
        case TRUE:
            return true;
        case QUERY_FILTER:
            return queryFilter == null ? false : queryFilter.accept(JSONVALUE_FILTER_VISITOR, params);
        case SCRIPTED:
            Map<String, Object> scope = new HashMap<String, Object>();
            try {
                if (params.isMap()) {
                    scope.putAll(params.asMap());
                }
                Object o = script.exec(scope);
                if (o == null || !(o instanceof Boolean) || Boolean.FALSE.equals(o)) {
                    return false; // property mapping is not applicable; do not apply
                }
                return true;
            } catch (JsonValueException jve) {
                LOGGER.warn("Unexpected JSON value exception while evaluating condition", jve);
                throw new SynchronizationException(jve);
            } catch (ScriptException se) {
                LOGGER.warn("Script encountered exception while evaluating condition", se);
                throw new SynchronizationException(se);
            }
        default:
            return false;
        }
    }

    /**
     * This is a relatively generic implementation for testing JsonValue objects though it
     * only returns Boolean for the test result.  This may be extractable to a more common
     * location for broader use.
     */
    private static final QueryFilterVisitor<Boolean, JsonValue> JSONVALUE_FILTER_VISITOR =
            new QueryFilterVisitor<Boolean, JsonValue>() {

                @Override
                public Boolean visitAndFilter(final JsonValue p, final List<QueryFilter> subFilters) {
                    for (final QueryFilter subFilter : subFilters) {
                        if (!subFilter.accept(this, p)) {
                            return Boolean.FALSE;
                        }
                    }
                    return Boolean.TRUE;
                }

                @Override
                public Boolean visitBooleanLiteralFilter(final JsonValue p, final boolean value) {
                    return value;
                }

                @Override
                public Boolean visitContainsFilter(final JsonValue p, final JsonPointer field,
                                                   final Object valueAssertion) {
                    for (final Object value : getValues(p, field)) {
                        if (isCompatible(valueAssertion, value)) {
                            if (valueAssertion instanceof String) {
                                final String s1 = ((String) valueAssertion).toLowerCase(Locale.ENGLISH);
                                final String s2 = ((String) value).toLowerCase(Locale.ENGLISH);
                                if (s2.contains(s1)) {
                                    return Boolean.TRUE;
                                }
                            } else {
                                // Use equality matching for numbers and booleans.
                                if (compareValues(valueAssertion, value) == 0) {
                                    return Boolean.TRUE;
                                }
                            }
                        }
                    }
                    return Boolean.FALSE;
                }

                @Override
                public Boolean visitEqualsFilter(final JsonValue p, final JsonPointer field,
                                                 final Object valueAssertion) {
                    for (final Object value : getValues(p, field)) {
                        if (isCompatible(value, valueAssertion) && compareValues(value, valueAssertion) == 0) {
                            return Boolean.TRUE;
                        }
                    }
                    return Boolean.FALSE;
                }

                @Override
                public Boolean visitExtendedMatchFilter(final JsonValue p, final JsonPointer field,
                                                        final String matchingRuleId, final Object valueAssertion) {
                    // Extended filters are not supported
                    return Boolean.FALSE;
                }

                @Override
                public Boolean visitGreaterThanFilter(final JsonValue p, final JsonPointer field,
                                                      final Object valueAssertion) {
                    for (final Object value : getValues(p, field)) {
                        if (isCompatible(value, valueAssertion) && compareValues(value, valueAssertion) > 0) {
                            return Boolean.TRUE;
                        }
                    }
                    return Boolean.FALSE;
                }

                @Override
                public Boolean visitGreaterThanOrEqualToFilter(final JsonValue p, final JsonPointer field,
                                                               final Object valueAssertion) {
                    for (final Object value : getValues(p, field)) {
                        if (isCompatible(value, valueAssertion) && compareValues(value, valueAssertion) >= 0) {
                            return Boolean.TRUE;
                        }
                    }
                    return Boolean.FALSE;
                }

                @Override
                public Boolean visitLessThanFilter(final JsonValue p, final JsonPointer field,
                                                   final Object valueAssertion) {
                    for (final Object value : getValues(p, field)) {
                        if (isCompatible(value, valueAssertion) && compareValues(value, valueAssertion) < 0) {
                            return Boolean.TRUE;
                        }
                    }
                    return Boolean.FALSE;
                }

                @Override
                public Boolean visitLessThanOrEqualToFilter(final JsonValue p, final JsonPointer field,
                                                            final Object valueAssertion) {
                    for (final Object value : getValues(p, field)) {
                        if (isCompatible(value, valueAssertion) && compareValues(value, valueAssertion) <= 0) {
                            return Boolean.TRUE;
                        }
                    }
                    return Boolean.FALSE;
                }

                @Override
                public Boolean visitNotFilter(final JsonValue p, final QueryFilter subFilter) {
                    return !subFilter.accept(this, p);
                }

                @Override
                public Boolean visitOrFilter(final JsonValue p, final List<QueryFilter> subFilters) {
                    for (final QueryFilter subFilter : subFilters) {
                        if (subFilter.accept(this, p)) {
                            return Boolean.TRUE;
                        }
                    }
                    return Boolean.FALSE;
                }

                @Override
                public Boolean visitPresentFilter(final JsonValue p, final JsonPointer field) {
                    final JsonValue value = p.get(field);
                    return value != null;
                }

                @Override
                public Boolean visitStartsWithFilter(final JsonValue p, final JsonPointer field,
                                                     final Object valueAssertion) {
                    for (final Object value : getValues(p, field)) {
                        if (isCompatible(valueAssertion, value)) {
                            if (valueAssertion instanceof String) {
                                final String s1 = ((String) valueAssertion).toLowerCase(Locale.ENGLISH);
                                final String s2 = ((String) value).toLowerCase(Locale.ENGLISH);
                                if (s2.startsWith(s1)) {
                                    return Boolean.TRUE;
                                }
                            } else {
                                // Use equality matching for numbers and booleans.
                                if (compareValues(valueAssertion, value) == 0) {
                                    return Boolean.TRUE;
                                }
                            }
                        }
                    }
                    return Boolean.FALSE;
                }

                private List<Object> getValues(final JsonValue resource, final JsonPointer field) {
                    final JsonValue value = resource.get(field);
                    if (value == null) {
                        return Collections.emptyList();
                    } else if (value.isList()) {
                        return value.asList();
                    } else {
                        return Collections.singletonList(value.getObject());
                    }
                }

            };

    private static int compareValues(final Object v1, final Object v2) {
        if (v1 instanceof String && v2 instanceof String) {
            final String s1 = (String) v1;
            final String s2 = (String) v2;
            return s1.compareToIgnoreCase(s2);
        } else if (v1 instanceof Number && v2 instanceof Number) {
            final Double n1 = ((Number) v1).doubleValue();
            final Double n2 = ((Number) v2).doubleValue();
            return n1.compareTo(n2);
        } else if (v1 instanceof Boolean && v2 instanceof Boolean) {
            final Boolean b1 = (Boolean) v1;
            final Boolean b2 = (Boolean) v2;
            return b1.compareTo(b2);
        } else {
            // Different types: we need to ensure predictable ordering,
            // so use class name as secondary key.
            return v1.getClass().getName().compareTo(v2.getClass().getName());
        }
    }

    private static boolean isCompatible(final Object v1, final Object v2) {
        return (v1 instanceof String && v2 instanceof String)
                || (v1 instanceof Number && v2 instanceof Number)
                || (v1 instanceof Boolean && v2 instanceof Boolean);
    }
}
