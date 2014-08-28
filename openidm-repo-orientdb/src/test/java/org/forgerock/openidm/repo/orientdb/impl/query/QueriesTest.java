/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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

import org.forgerock.json.resource.BadRequestException;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class QueriesTest {

    @Test
    public void setConfiguredQueriesDefaultQueryAllIds() throws BadRequestException {
        Map<String,String> queryStrings = new HashMap<String, String>();
        Queries queries = new Queries();
        queries.setConfiguredQueries(queryStrings);
        assertTrue(queries.queryIdExists("query-all-ids"));
        assertEquals(queries.findQueryInfo("test", "query-all-ids", null).getQueryString(),
                "select _openidm_id from ${unquoted:_resource}");
    }
    
    @Test
    public void setConfiguredQueriesOverrideQueryAllIds() throws BadRequestException {
        Map<String,String> queryStrings = new HashMap<String, String>();
        queryStrings.put("query-all-ids", "select _id from ${unquoted:_resource}");
        Queries queries = new Queries();
        queries.setConfiguredQueries(queryStrings);
        assertTrue(queries.queryIdExists("query-all-ids"));
        assertEquals(queries.findQueryInfo("test", "query-all-ids", null).getQueryString(),
                "select _id from ${unquoted:_resource}");
    }
}