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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * Helper in cleaning up JDBC resources
 *
 */
public class CleanupHelper {
    final static Logger logger = LoggerFactory.getLogger(CleanupHelper.class);

    /**
     * @param connection the connection to try to close if not null.
     * Failures to close are logged, no exception is propagated up
     */
    public static void loggedClose(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                logger.warn("Failure during connection close ", ex);
            }
        }
    }

    /**
     * @param statement the statement to try to close if not null.
     * Failures to close are logged, no exception is propagated up
     */
    public static void loggedClose(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ex) {
                logger.warn("Failure during statement close ", ex);
            }
        }
    }

    /**
     * @param rs the ResultSet to try to close if not null.
     * Failures to close are logged, no exception is propagated up
     */
    public static void loggedClose(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ex) {
                logger.warn("Failure during ResultSet close ", ex);
            }
        }
    }
}
