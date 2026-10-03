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
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import java.sql.Connection;
import java.util.HashMap;

import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.openidm.repo.jdbc.TableHandler;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for the revision handling shared by the JDBC table handlers.
 */
public class GenericTableHandlerTest {

    @Test
    public void numericRevisionIsParsed() throws Exception {
        assertThat(GenericTableHandler.parseRevision("7")).isEqualTo(7);
    }

    @Test(expectedExceptions = PreconditionFailedException.class)
    public void nonNumericRevisionCannotMatchAndIsRejected() throws Exception {
        GenericTableHandler.parseRevision("\"7\"");
    }

    @DataProvider
    public Object[][] handlersWithOwnUpdate() {
        return new Object[][] {
            { GenericTableHandler.class },
            { MSSQLTableHandler.class },
            { MappedTableHandler.class },
        };
    }

    /** The revision is parsed before any database access, so a partial mock reaches it without a connection. */
    @Test(dataProvider = "handlersWithOwnUpdate", expectedExceptions = PreconditionFailedException.class)
    public void updateRejectsNonNumericRevision(Class<? extends TableHandler> handlerClass) throws Exception {
        TableHandler handler = mock(handlerClass, CALLS_REAL_METHODS);
        handler.update("managed/user/1", "managed/user", "1", "\"7\"",
                new HashMap<String, Object>(), mock(Connection.class));
    }
}
