/*
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
 */

import org.apache.olingo.client.api.uri.URIFilter
import org.apache.olingo.client.api.uri.v3.FilterFactory
import org.identityconnectors.framework.common.objects.AttributeUtil
import org.identityconnectors.framework.common.objects.filter.AndFilter
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter
import org.identityconnectors.framework.common.objects.filter.ContainsFilter
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter
import org.identityconnectors.framework.common.objects.filter.EqualsFilter
import org.identityconnectors.framework.common.objects.filter.Filter
import org.identityconnectors.framework.common.objects.filter.FilterVisitor
import org.identityconnectors.framework.common.objects.filter.GreaterThanFilter
import org.identityconnectors.framework.common.objects.filter.GreaterThanOrEqualFilter
import org.identityconnectors.framework.common.objects.filter.LessThanFilter
import org.identityconnectors.framework.common.objects.filter.LessThanOrEqualFilter
import org.identityconnectors.framework.common.objects.filter.NotFilter
import org.identityconnectors.framework.common.objects.filter.OrFilter
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter

/**
 * An ODataFilterVisitor transform the OpenICF Filter to OData filter string
 *
 * @author Laszlo Hordos
 */
public class ODataFilterVisitor implements FilterVisitor<URIFilter, FilterFactory> {

    public static final ODataFilterVisitor VISITOR = new ODataFilterVisitor();

    URIFilter visitAndFilter(FilterFactory filterFactory, AndFilter filter) {
        return filterFactory.and(filter.left.accept(this, filterFactory),
                filter.right.accept(this, filterFactory),)
    }

    URIFilter visitContainsFilter(FilterFactory filterFactory, ContainsFilter filter) {
        return filterFactory.match()
    }

    URIFilter visitContainsAllValuesFilter(FilterFactory filterFactory, ContainsAllValuesFilter filter) {
        throw new UnsupportedOperationException("ContainsAllValuesFilter is not supported")
    }

    URIFilter visitEqualsFilter(FilterFactory filterFactory, EqualsFilter filter) {
        return filterFactory.eq(filter.name, AttributeUtil.getSingleValue(filter.attribute))
    }

    URIFilter visitExtendedFilter(FilterFactory filterFactory, Filter filter) {
        throw new UnsupportedOperationException("ExtendedFilter is not supported")
    }

    URIFilter visitGreaterThanFilter(FilterFactory filterFactory, GreaterThanFilter filter) {
        return filterFactory.gt(filter.name, AttributeUtil.getSingleValue(filter.attribute))
    }

    URIFilter visitGreaterThanOrEqualFilter(FilterFactory filterFactory, GreaterThanOrEqualFilter filter) {
        return filterFactory.ge(filter.name, AttributeUtil.getSingleValue(filter.attribute))
    }

    URIFilter visitLessThanFilter(FilterFactory filterFactory, LessThanFilter filter) {
        return filterFactory.lt(filter.name, AttributeUtil.getSingleValue(filter.attribute))
    }

    URIFilter visitLessThanOrEqualFilter(FilterFactory filterFactory, LessThanOrEqualFilter filter) {
        return filterFactory.le(filter.name, AttributeUtil.getSingleValue(filter.attribute))
    }

    URIFilter visitNotFilter(FilterFactory filterFactory, NotFilter filter) {
        return filterFactory.not(filter.filter.accept(this, filterFactory))
    }

    URIFilter visitOrFilter(FilterFactory filterFactory, OrFilter filter) {
        return filterFactory.or(filter.left.accept(this, filterFactory),
                filter.right.accept(this, filterFactory),)
    }

    URIFilter visitStartsWithFilter(FilterFactory filterFactory, StartsWithFilter filter) {
        return filterFactory.match(
                filterFactory.argFactory.startswith(
                        filterFactory.argFactory.property(filter.name),
                        filterFactory.argFactory.literal(AttributeUtil.getSingleValue(filter.attribute))));
    }

    URIFilter visitEndsWithFilter(FilterFactory filterFactory, EndsWithFilter filter) {
        return filterFactory.match(
                filterFactory.argFactory.endswith(
                        filterFactory.argFactory.property(filter.name),
                        filterFactory.argFactory.literal(AttributeUtil.getSingleValue(filter.attribute))));
    }
}
