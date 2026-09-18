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
package org.forgerock.openidm.repo.jdbc.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashSet;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.util.Accessor;
import org.testng.annotations.Test;

/**
 * Tests for {@link ExplicitResultSetMapper#mapToJsonValue}.
 */
public class ExplicitResultSetMapperTest {

    /** STRING columns require a crypto service to be reachable, even when nothing is encrypted. */
    private static final Accessor<CryptoService> CRYPTO = new Accessor<CryptoService>() {
        private final CryptoService cryptoService = mock(CryptoService.class);

        @Override
        public CryptoService access() {
            return cryptoService;
        }
    };

    @Test
    public void unmappedTotalColumnIsExposedAsRowCount() throws Exception {
        ExplicitResultSetMapper mapper = new ExplicitResultSetMapper("t",
                json(object(field("_id", "objectid"))), CRYPTO);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("objectid")).thenReturn("1");
        when(rs.getInt("total")).thenReturn(42);

        JsonValue result = mapper.mapToJsonValue(rs, new HashSet<>(Arrays.asList("objectid", "total")));

        assertThat(result.get("_id").asString()).isEqualTo("1");
        assertThat(result.get("total").asInteger()).isEqualTo(42);
    }

    @Test
    public void explicitlyMappedTotalColumnIsNotOverwrittenByRowCount() throws Exception {
        ExplicitResultSetMapper mapper = new ExplicitResultSetMapper("t",
                json(object(field("_id", "objectid"), field("total", array("total", "STRING")))), CRYPTO);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("objectid")).thenReturn("1");
        when(rs.getString("total")).thenReturn("mapped");

        JsonValue result = mapper.mapToJsonValue(rs, new HashSet<>(Arrays.asList("objectid", "total")));

        assertThat(result.get("total").asString()).isEqualTo("mapped");
        verify(rs, never()).getInt("total");
    }
}
