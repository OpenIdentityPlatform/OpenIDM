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
import static org.identityconnectors.framework.common.objects.filter.FilterBuilder.startsWith;

import java.util.Iterator;
import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.identityconnectors.framework.common.objects.filter.Filter;

/**
 * Converts CREST {@link QueryFilter}s to ICF {@link Filter}s. This class is thread-safe.
 */
class OpenICFFilterAdapter implements QueryFilterVisitor<Filter, ObjectClassInfoHelper, JsonPointer> {
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

    @Override
    public Filter visitBooleanLiteralFilter(final ObjectClassInfoHelper helper, final boolean value) {
        if (value) {
            return null;
        }
        throw new UnsupportedOperationException(
                "visitBooleanLiteralFilter only supported for literal true, not false");
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

    @Override
    public Filter visitPresentFilter(ObjectClassInfoHelper helper, JsonPointer field) {
        throw new IllegalArgumentException("PresentFilter is not supported");
    }

    @Override
    public Filter visitStartsWithFilter(ObjectClassInfoHelper helper,
            JsonPointer field, Object valueAssertion) {
        return startsWith(helper.filterAttribute(field, valueAssertion));
    }
}
