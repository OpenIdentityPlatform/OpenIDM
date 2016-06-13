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

package org.forgerock.openidm.provisioner.openicf.impl;

import static org.identityconnectors.framework.common.objects.filter.FilterBuilder.*;

import java.util.Iterator;
import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.PresenceFilter;

/**
 * Converts CREST {@link QueryFilter}s to ICF {@link Filter}s. Callers should catch, but not log,
 * {@link EmptyResultSetException}s and do what is necessary to return an empty-result representation.
 * This class is thread-safe.
 */
class OpenICFFilterAdapter implements QueryFilterVisitor<Filter, ObjectClassInfoHelper, JsonPointer> {

    private static final EmptyResultSetException EMPTY_RESULT_SET_EXCEPTION =
            new EmptyResultSetException("Empty query result from " + OpenICFFilterAdapter.class.getCanonicalName());

    static {
        // preallocate stack trace, for best performance, because this exception is used for flow-control
        EMPTY_RESULT_SET_EXCEPTION.setStackTrace(new StackTraceElement[0]);
    }

    @Override
    public Filter visitAndFilter(final ObjectClassInfoHelper helper,
            List<QueryFilter<JsonPointer>> subFilters) {
        final Iterator<QueryFilter<JsonPointer>> iterator = subFilters.iterator();
        if (iterator.hasNext()) {
            return buildAnd(helper, iterator.next(), iterator);
        } else {
            throw new IllegalArgumentException("cannot parse 'and' QueryFilter with zero operands");
        }
    }

    private Filter buildAnd(final ObjectClassInfoHelper helper, final QueryFilter<JsonPointer> left,
            final Iterator<QueryFilter<JsonPointer>> iterator) {
        if (iterator.hasNext()) {
            final QueryFilter<JsonPointer> right = iterator.next();
            return and(left.accept(this, helper), buildAnd(helper, right, iterator));
        } else {
            return left.accept(this, helper);
        }
    }

    @Override
    public Filter visitOrFilter(ObjectClassInfoHelper helper,
            List<QueryFilter<JsonPointer>> subFilters) {
        final Iterator<QueryFilter<JsonPointer>> iterator = subFilters.iterator();
        if (iterator.hasNext()) {
            return buildOr(helper, iterator.next(), iterator);
        } else {
            throw new IllegalArgumentException("cannot parse 'or' QueryFilter with zero operands");
        }
    }

    private Filter buildOr(final ObjectClassInfoHelper helper, final QueryFilter<JsonPointer> left,
            final Iterator<QueryFilter<JsonPointer>> iterator) {
        if (iterator.hasNext()) {
            final QueryFilter<JsonPointer> right = iterator.next();
            return or(left.accept(this, helper), buildOr(helper, right, iterator));
        } else {
            return left.accept(this, helper);
        }
    }

    /**
     * Visits a boolean literal filter.
     *
     * @param helper Helper for attribute name/value lookup on the data model
     * @param value The boolean literal value.
     * @return Returns {@code null} when {@code value} is {@code true}, which matches all possible results
     * @throws EmptyResultSetException Indicates that {@code value} is {@code false}, which matches an empty-result
     */
    @Override
    public Filter visitBooleanLiteralFilter(final ObjectClassInfoHelper helper, final boolean value) {
        if (value) {
            return null;
        }
        throw EMPTY_RESULT_SET_EXCEPTION;
    }

    @Override
    public Filter visitContainsFilter(ObjectClassInfoHelper helper, JsonPointer field,
            Object valueAssertion) {
        return contains(helper.filterAttribute(field, valueAssertion));
    }

    @Override
    public Filter visitEqualsFilter(ObjectClassInfoHelper helper, JsonPointer field,
            Object valueAssertion) {
        return equalTo(helper.filterAttribute(field, valueAssertion));
    }

    /**
     * EndsWith filter
     */
    protected static final String EW = "ew";

    /**
     * ContainsAll filter
     */
    protected static final String CA = "ca";

    @Override
    public Filter visitExtendedMatchFilter(ObjectClassInfoHelper helper,
            JsonPointer field, String matchingRuleId, Object valueAssertion) {
        if (EW.equals(matchingRuleId)) {
            return endsWith(helper.filterAttribute(field, valueAssertion));
        } else if (CA.equals(matchingRuleId)) {
            return containsAllValues(helper.filterAttribute(field, valueAssertion));
        }
        throw new IllegalArgumentException("ExtendedMatchFilter is not supported");
    }

    @Override
    public Filter visitGreaterThanFilter(ObjectClassInfoHelper helper,
            JsonPointer field, Object valueAssertion) {
        return greaterThan(helper.filterAttribute(field, valueAssertion));
    }

    @Override
    public Filter visitGreaterThanOrEqualToFilter(ObjectClassInfoHelper helper,
            JsonPointer field, Object valueAssertion) {
        return greaterThanOrEqualTo(helper.filterAttribute(field, valueAssertion));
    }

    @Override
    public Filter visitLessThanFilter(ObjectClassInfoHelper helper, JsonPointer field,
            Object valueAssertion) {
        return lessThan(helper.filterAttribute(field, valueAssertion));
    }

    @Override
    public Filter visitLessThanOrEqualToFilter(ObjectClassInfoHelper helper,
            JsonPointer field, Object valueAssertion) {
        return lessThanOrEqualTo(helper.filterAttribute(field, valueAssertion));
    }

    @Override
    public Filter visitNotFilter(ObjectClassInfoHelper helper, QueryFilter<JsonPointer> subFilter) {
        return not(subFilter.accept(this, helper));
    }

    /**
     * Visits a {@code present} filter.
     *
     * @param helper Helper for attribute name/value lookup on the data model
     * @param field Single-level {@link JsonPointer} which is the CREST attribute-name
     * @return Returns a new {@link PresenceFilter} instance <i>only</i> when attribute is present
     * @throws EmptyResultSetException Indicates that an attribute is <i>not</i> present, which matches an empty-result
     */
    @Override
    public Filter visitPresentFilter(ObjectClassInfoHelper helper, JsonPointer field) {
        String attributeName = helper.getAttributeName(field);
        if (attributeName != null) {
            return present(attributeName);
        }
        throw EMPTY_RESULT_SET_EXCEPTION;
    }

    @Override
    public Filter visitStartsWithFilter(ObjectClassInfoHelper helper,
            JsonPointer field, Object valueAssertion) {
        return startsWith(helper.filterAttribute(field, valueAssertion));
    }
}
