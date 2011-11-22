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
package org.forgerock.openidm.repo.jdbc.impl;

import java.sql.SQLException;

import org.forgerock.openidm.repo.jdbc.ErrorType;

/**
 * Maps SQL state with Open Group (X/Open) SQL Standard codes
 * to error constants known in OpenIDM
 * 
 * Use the error type facility sparingly, as it can take 
 * DB specific error code implementations, porting and maintenance
 * 
 * @author aegloff
 */
public class XOpenErrorMapping {
    public static boolean isErrorType(SQLException ex, ErrorType errorType) {
        boolean match = false;
        String sqlState = ex.getSQLState(); // ISO/ANSI or Open Group (X/Open) SQL Standard codes
        switch (errorType) {
            case CONNECTION_FAILURE: {
                // X/Open 08S01 is communication link failure
                // Known to be retryable for MySQL
                if ("08S01".equals(sqlState.trim())) {
                    match = true;
                }
                break;
            }
            case DUPLICATE_KEY: {
                // X/Open 23000 is Integrity constraint violation
                // Known to be used by Oracle, SQL Server, DB2, and MySQL
                if ("23000".equals(sqlState.trim())) {
                    match = true;
                }
                break;
            }
            case DEADLOCK_OR_TIMEOUT: {
                // X/Open 40001 is serialization failure such as timeout or deadlock
                // Known to be retryable for MySQL
                if ("40001".equals(sqlState.trim())) {
                    match = true;
                }
            }
        }
        return match;
    }
}