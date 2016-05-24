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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.repo.jdbc.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * MSSQL handling of SQLExceptions
 */
public class MSSQLExceptionHandler extends DefaultSQLExceptionHandler {

    // MSSQL error codes which are retryable in addition to those handled
    // by the DefaultSQLExceptionHandler.
    private static final List<Integer> retryableErrors = Arrays.asList(
            701,  // Out of Memory
            1204, // Lock Issue
            1205, // Deadlock Victim
            1222, // Lock request time out period exceeded.
            3960, // Snapshot isolation transaction aborted due to update conflict.
            7214, // Remote procedure time out of %d seconds exceeded. Remote procedure '%.*ls' is canceled.
            7604, // Full-text operation failed due to a time out.
            7618, // %d is not a valid value for a full-text connection time out.
            8628, // A time out occurred while waiting to optimize the query. Rerun the query.
            8645, // A time out occurred while waiting for memory resources to execute the query. Rerun the query.
            8651  // Low memory condition
    );

    
    /**
     * @inheritDoc
     */
    @Override
    public boolean isRetryable(SQLException ex, Connection connection) {
        return super.isRetryable(ex, connection) || retryableErrors.contains(ex.getErrorCode());
    }
}
