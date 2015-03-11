/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2014 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.orientdb.impl.query;

/**
 * Information about a query, and state of the query
 * 
 */
final class QueryInfo<Q> {
    private boolean usePrepared;
    private Q preparedQuery;
    private String queryString;

    /**
     * Constructor
     * 
     * @param usePrepared whether queries should attempt to use the prepared query representation.
     * @param preparedQuery an optional prepared query representation
     * @param queryString the query in string form with optional OpenIDM tokens
     */
    public QueryInfo(boolean usePrepared, Q preparedQuery, String queryString) {
        this.usePrepared = usePrepared;
        this.preparedQuery = preparedQuery;
        this.queryString = queryString;
    }
    
    /**
     * @return whether queries should attempt to use the prepared query representation.
     */
    public boolean isUsePrepared() {
        return usePrepared;
    }

    /**
     * @param prep whether queries should attempt to use the prepared query representation.
     */
    public void setUsePrepared(boolean prep) {
        this.usePrepared = prep;
    }
    
    /**
     * @return an optional prepared query representation
     */
    public Q getPreparedQuery() {
        return preparedQuery;
    }
    
    /**
     * @return the query in string form with optional OpenIDM tokens
     */
    public String getQueryString() {
        return queryString;
    }
}