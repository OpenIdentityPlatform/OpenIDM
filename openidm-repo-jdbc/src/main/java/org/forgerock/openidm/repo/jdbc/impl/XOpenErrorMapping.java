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
 * Copyright 2011-2015 ForgeRock AS.
 */
package org.forgerock.openidm.repo.jdbc.impl;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.forgerock.openidm.repo.jdbc.ErrorType;


/**
 * Maps SQL state with Open Group (X/Open) SQL Standard codes to error constants known in OpenIDM
 *
 * Use the error type facility sparingly, as it can take DB specific error code implementations, porting and maintenance
 *
 */
public class XOpenErrorMapping {

    /** mapping of ErrorType constants to known X/Open SQL Standard SQL State codes */
    private static final Map<ErrorType, List<String>> errorTypeToSqlStates =
            new EnumMap<ErrorType, List<String>>(ErrorType.class);

    static {
        /* codes known to represent connection failure conditions */
        errorTypeToSqlStates.put(ErrorType.CONNECTION_FAILURE, Arrays.asList(
                // X/Open 08S01 is communication link failure
                // Known to be retryable for MySQL
                "08S01"
        ));

        /* codes known to represent duplicate key conditions */
        errorTypeToSqlStates.put(ErrorType.DUPLICATE_KEY, Arrays.asList(
                // X/Open 23000 is Integrity constraint violation
                // Known to be used by Oracle, SQL Server, DB2, and MySQL
                // Failure may be for a different constraint than duplicate though.
                "23000",
                // Postgres unique_violation per http://www.postgresql.org/docs/9.3/static/errcodes-appendix.html
                // also Oracle per http://docs.oracle.com/javadb/10.3.3.0/ref/rrefexcept71493.html
                "23505"
        ));

        /* codes known to represent deadlock/timeout conditions */
        errorTypeToSqlStates.put(ErrorType.DEADLOCK_OR_TIMEOUT, Arrays.asList(
                // X/Open 40001 is serialization failure such as timeout or deadlock
                // Known to be retryable for MySQL
                "40001"
        ));

        /* codes known to represent transaction isolation conditions */
        errorTypeToSqlStates.put(ErrorType.CANT_CHANGE_TX_ISOLATION, Arrays.asList(
                // Transaction isolation level can't be changed while a transaction is in progress.
                // Known to be used by PostgreSQL.
                "25001"
        ));
    }

    /**
     * Return whether the {@link SQLException} represents the given {@link ErrorType} using the
     * exceptions SQL State.
     *
     * @param ex an SQLException
     * @param errorType the error type to test
     * @return whether the exception is of the error type given
     */
    public static boolean isErrorType(SQLException ex, ErrorType errorType) {
        if (!errorTypeToSqlStates.containsKey(errorType)) {
            // we have no known mapping for this error type
            return false;
        }

        // ISO/ANSI or Open Group (X/Open) SQL Standard codes
        String sqlState = ex.getSQLState();
        if (sqlState != null) {
            sqlState = sqlState.trim();
        }

        return errorTypeToSqlStates.get(errorType).contains(sqlState);
    }
}
