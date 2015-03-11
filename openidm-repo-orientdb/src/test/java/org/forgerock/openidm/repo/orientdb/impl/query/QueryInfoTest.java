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

import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import org.forgerock.json.resource.BadRequestException;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class QueryInfoTest {

    @Test
    public void fullQueryInfo() throws BadRequestException {
        boolean usePrepared = true;
        String queryString = "select * from managed/user";
        OSQLSynchQuery preparedQuery = new OSQLSynchQuery(queryString);
        QueryInfo<OSQLSynchQuery> queryInfo = new QueryInfo<OSQLSynchQuery>(usePrepared, preparedQuery, queryString);
        assertTrue(queryInfo.isUsePrepared());
        assertEquals(queryInfo.getPreparedQuery(), preparedQuery);
        assertEquals(queryInfo.getQueryString(), queryString);
    }
    
    @Test
    public void partialQueryInfo() throws BadRequestException {
        boolean usePrepared = false;
        String queryString = "select ${_fields} from managed/user";
        OSQLSynchQuery preparedQuery = null;
        QueryInfo<OSQLSynchQuery> queryInfo = new QueryInfo<OSQLSynchQuery>(usePrepared, preparedQuery, queryString);
        assertFalse(queryInfo.isUsePrepared());
        assertEquals(queryInfo.getPreparedQuery(), preparedQuery);
        assertEquals(queryInfo.getQueryString(), queryString);
    }    
}