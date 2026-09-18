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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openidm.repo.jdbc.impl.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.resource.BadRequestException;
import org.testng.annotations.Test;

/**
 * Tests for the parameter binding in {@link TableQueries#resolveQuery}.
 */
public class TableQueriesTest {

    @Test
    public void nonNumericIntTokenIsRejectedAsBadRequest() throws Exception {
        TableQueries queries = new TableQueries(null, "objects", "objectproperties", "openidm", 100, null);
        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenReturn(mock(PreparedStatement.class));
        QueryInfo query = new QueryInfo("SELECT * FROM objects WHERE size = ?", Arrays.asList("int:size"));
        Map<String, Object> params = new HashMap<>();
        params.put("size", "large");

        try {
            queries.resolveQuery(query, connection, params);
            fail("expected BadRequestException");
        } catch (BadRequestException e) {
            assertThat(e.getMessage()).contains("size").contains("large");
        }
    }
}
