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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.filter;

import static org.forgerock.openidm.util.JsonUtil.compareJsonObjectValues;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

/**
 * Filters JsonValue objects and then returns Boolean for the test result.
 */
public class JsonValueFilterVisitor implements QueryFilterVisitor<Boolean, JsonValue, JsonPointer> {

    @Override
    public Boolean visitAndFilter(final JsonValue p, final List<QueryFilter<JsonPointer>> subFilters) {
        for (final QueryFilter<JsonPointer> subFilter : subFilters) {
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
                    if (compareJsonObjectValues(valueAssertion, value) == 0) {
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
            if (isCompatible(value, valueAssertion) && compareJsonObjectValues(value, valueAssertion) == 0) {
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
            if (isCompatible(value, valueAssertion) && compareJsonObjectValues(value, valueAssertion) > 0) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean visitGreaterThanOrEqualToFilter(final JsonValue p, final JsonPointer field,
            final Object valueAssertion) {
        for (final Object value : getValues(p, field)) {
            if (isCompatible(value, valueAssertion) && compareJsonObjectValues(value, valueAssertion) >= 0) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean visitLessThanFilter(final JsonValue p, final JsonPointer field,
            final Object valueAssertion) {
        for (final Object value : getValues(p, field)) {
            if (isCompatible(value, valueAssertion) && compareJsonObjectValues(value, valueAssertion) < 0) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean visitLessThanOrEqualToFilter(final JsonValue p, final JsonPointer field,
            final Object valueAssertion) {
        for (final Object value : getValues(p, field)) {
            if (isCompatible(value, valueAssertion) && compareJsonObjectValues(value, valueAssertion) <= 0) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean visitNotFilter(final JsonValue p, final QueryFilter<JsonPointer> subFilter) {
        return !subFilter.accept(this, p);
    }

    @Override
    public Boolean visitOrFilter(final JsonValue p, final List<QueryFilter<JsonPointer>> subFilters) {
        for (final QueryFilter<JsonPointer> subFilter : subFilters) {
            if (subFilter.accept(this, p)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean visitPresentFilter(final JsonValue p, final JsonPointer field) {
        final JsonValue value = p.get(field);
        return value != null && !value.isNull();
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
                    if (compareJsonObjectValues(valueAssertion, value) == 0) {
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

    private boolean isCompatible(final Object v1, final Object v2) {
        return (v1 instanceof String && v2 instanceof String)
                || (v1 instanceof Number && v2 instanceof Number)
                || (v1 instanceof Boolean && v2 instanceof Boolean);
    }

}
