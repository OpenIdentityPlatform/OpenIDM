/*
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
 */

package org.forgerock.openidm.provisioner.openicf.internal;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryFilterVisitor;
import org.forgerock.json.resource.QueryRequest;
import org.identityconnectors.framework.common.objects.filter.Filter;

import java.util.List;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class ConnectorQueryFilterVisitor implements QueryFilterVisitor<Filter,QueryRequest> {
    @Override
    public Filter visitAndFilter(QueryRequest queryRequest, List<QueryFilter> subFilters) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Filter visitBooleanLiteralFilter(QueryRequest queryRequest, boolean value) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Filter visitContainsFilter(QueryRequest queryRequest, JsonPointer field, Object valueAssertion) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Filter visitEqualsFilter(QueryRequest queryRequest, JsonPointer field, Object valueAssertion) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Filter visitExtendedMatchFilter(QueryRequest queryRequest, JsonPointer field, String matchingRuleId, Object valueAssertion) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Filter visitGreaterThanFilter(QueryRequest queryRequest, JsonPointer field, Object valueAssertion) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Filter visitGreaterThanOrEqualToFilter(QueryRequest queryRequest, JsonPointer field, Object valueAssertion) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Filter visitLessThanFilter(QueryRequest queryRequest, JsonPointer field, Object valueAssertion) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Filter visitLessThanOrEqualToFilter(QueryRequest queryRequest, JsonPointer field, Object valueAssertion) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Filter visitNotFilter(QueryRequest queryRequest, QueryFilter subFilter) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Filter visitOrFilter(QueryRequest queryRequest, List<QueryFilter> subFilters) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Filter visitPresentFilter(QueryRequest queryRequest, JsonPointer field) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Filter visitStartsWithFilter(QueryRequest queryRequest, JsonPointer field, Object valueAssertion) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
