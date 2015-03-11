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

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DB2 handling of SQLExceptions
 *
 */
public class DB2SQLExceptionHandler extends DefaultSQLExceptionHandler {

    /**
     * @InheritDoc
     */
    @Override
    public boolean isRetryable(SQLException ex, Connection connection) {
        // Re-tryable DB2 error codes
        // -911 indicates DB2 rolled back already and expects a retry
        // -912 indicates deadlock or timeout.
        // -904 indicates resource limit was exceeded.
        if (-911 == ex.getErrorCode() || -912 == ex.getErrorCode() || -904 == ex.getErrorCode()) {
            return true;
        } else {
            return false;
        }
    }
}