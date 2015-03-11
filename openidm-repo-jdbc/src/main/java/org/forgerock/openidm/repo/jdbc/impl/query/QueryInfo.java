/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.jdbc.impl.query;

import java.util.List;

/**
 * Information about a JDBC query
 *
 */
final class QueryInfo {
    private String queryString;
    private List<String> tokenNames;

    public QueryInfo(String queryString, List<String> tokenNames) {
        this.queryString = queryString;
        this.tokenNames = tokenNames;
    }

    /**
     * @return the prepared query in string form
     * may contain ? for token replacement
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * @return the token names in the order they need to replaced
     * in the queryString
     */
    public List<String> getTokenNames() {
        return tokenNames;
    }
}