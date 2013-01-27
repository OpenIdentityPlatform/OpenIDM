///*
// * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
// *
// * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
// *
// * The contents of this file are subject to the terms
// * of the Common Development and Distribution License
// * (the License). You may not use this file except in
// * compliance with the License.
// *
// * You can obtain a copy of the License at
// * http://forgerock.org/license/CDDLv1.0.html
// * See the License for the specific language governing
// * permission and limitations under the License.
// *
// * When distributing Covered Code, include this CDDL
// * Header Notice in each file and include the License file
// * at http://forgerock.org/license/CDDLv1.0.html
// * If applicable, add the following below the CDDL Header,
// * with the fields enclosed by brackets [] replaced by
// * your own identifying information:
// * "Portions Copyrighted [year] [name of copyright owner]"
// */
//
//package org.forgerock.openidm.provisioner.openicf.internal;
//
//import org.forgerock.json.fluent.JsonPointer;
//import org.forgerock.json.resource.QueryFilter;
//import org.forgerock.json.resource.QueryFilterVisitor;
//import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
//import org.identityconnectors.framework.common.objects.ConnectorObject;
//import org.identityconnectors.framework.common.objects.filter.Filter;
//
//import java.util.Iterator;
//import java.util.List;
//
//import static org.identityconnectors.framework.common.objects.filter.FilterBuilder.*;
//
///**
// * A NAME does ...
// *
// * @author Laszlo Hordos
// */
//public class ConnectorQueryFilterVisitor implements QueryFilterVisitor<Filter,ObjectClassInfoHelper> {
//    @Override
//    public Filter visitAndFilter(final ObjectClassInfoHelper helper, List<QueryFilter> subFilters) {
//        final Iterator<QueryFilter> iterator = subFilters.iterator();
//        if (iterator.hasNext()) {
//            final QueryFilter left = iterator.next();
//            return buildAnd(helper, left, iterator);
//        } else {
//            return new Filter() {
//                @Override
//                public boolean accept(ConnectorObject obj) {
//                    return true;
//                }
//            };
//        }
//    }
//
//    private Filter buildAnd(final ObjectClassInfoHelper helper, final QueryFilter left,
//            final Iterator<QueryFilter> iterator) {
//        if (iterator.hasNext()) {
//            final QueryFilter right = iterator.next();
//            return and(left.accept(this, helper), buildAnd(helper, right,
//                    iterator));
//        } else {
//            return left.accept(this, helper);
//        }
//    }
//
//    @Override
//    public Filter visitOrFilter(ObjectClassInfoHelper helper, List<QueryFilter> subFilters) {
//        final Iterator<QueryFilter> iterator = subFilters.iterator();
//        if (iterator.hasNext()) {
//            final QueryFilter left = iterator.next();
//            return buildOr(helper, left, iterator);
//        } else {
//            return new Filter() {
//                @Override
//                public boolean accept(ConnectorObject obj) {
//                    return true;
//                }
//            };
//        }
//    }
//
//    private Filter buildOr(final ObjectClassInfoHelper helper, final QueryFilter left,
//                            final Iterator<QueryFilter> iterator) {
//        if (iterator.hasNext()) {
//
//            final QueryFilter right = iterator.next();
//            return or(left.accept(this, helper), buildAnd(helper, right,
//                    iterator));
//        } else {
//            return left.accept(this, helper);
//        }
//    }
//
//    @Override
//    public Filter visitBooleanLiteralFilter(final ObjectClassInfoHelper helper, final boolean value) {
//        return new Filter() {
//            @Override
//            public boolean accept(ConnectorObject obj) {
//                return value;
//            }
//        };
//    }
//
//    @Override
//    public Filter visitContainsFilter(ObjectClassInfoHelper helper, JsonPointer field, Object valueAssertion) {
//        return contains(helper.filterAttribute(field, valueAssertion));
//    }
//
//    @Override
//    public Filter visitEqualsFilter(ObjectClassInfoHelper helper, JsonPointer field, Object valueAssertion) {
//        return equalTo(helper.filterAttribute(field, valueAssertion));
//    }
//
//    /**
//     * EndsWith filter
//     */
//    private static final String EV = "ev";
//    /**
//     * ContainsAll filter
//     */
//    private static final String CA = "ca";
//
//
//    @Override
//    public Filter visitExtendedMatchFilter(ObjectClassInfoHelper helper, JsonPointer field, String matchingRuleId, Object valueAssertion) {
//        if (EV.equals(matchingRuleId)){
//           return endsWith(helper.filterAttribute(field, valueAssertion));
//        } else if (CA.equals(matchingRuleId)){
//            return containsAllValues(helper.filterAttribute(field, valueAssertion));
//        }
//        throw new IllegalArgumentException("ExtendedMatchFilter is not supported");
//    }
//
//    @Override
//    public Filter visitGreaterThanFilter(ObjectClassInfoHelper helper, JsonPointer field, Object valueAssertion) {
//        return greaterThan(helper.filterAttribute(field, valueAssertion));
//    }
//
//    @Override
//    public Filter visitGreaterThanOrEqualToFilter(ObjectClassInfoHelper helper, JsonPointer field, Object valueAssertion) {
//        return greaterThanOrEqualTo(helper.filterAttribute(field, valueAssertion));
//    }
//
//    @Override
//    public Filter visitLessThanFilter(ObjectClassInfoHelper helper, JsonPointer field, Object valueAssertion) {
//        return lessThan(helper.filterAttribute(field, valueAssertion));
//    }
//
//    @Override
//    public Filter visitLessThanOrEqualToFilter(ObjectClassInfoHelper helper, JsonPointer field, Object valueAssertion) {
//        return lessThanOrEqualTo(helper.filterAttribute(field, valueAssertion));
//    }
//
//    @Override
//    public Filter visitNotFilter(ObjectClassInfoHelper helper, QueryFilter subFilter) {
//        return not(subFilter.accept(this, helper));
//    }
//
//    @Override
//    public Filter visitPresentFilter(ObjectClassInfoHelper helper, JsonPointer field) {
//        throw new IllegalArgumentException("PresentFilter is not supported");
//    }
//
//    @Override
//    public Filter visitStartsWithFilter(ObjectClassInfoHelper helper, JsonPointer field, Object valueAssertion) {
//        return startsWith(helper.filterAttribute(field, valueAssertion));
//    }
//}
