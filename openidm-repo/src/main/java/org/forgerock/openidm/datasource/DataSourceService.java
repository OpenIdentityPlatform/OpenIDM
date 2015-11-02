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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.datasource;

import javax.sql.DataSource;

/**
 * Basic DataSource management interface.  Facilitates obtaining a DataSource from a variety of sources
 * and hides possible connection pool implementation and other implementation details.
 */
public interface DataSourceService {
    /**
     * Get the name of the database, or schema, within the database.
     *
     * @return the database name
     */
    String getDatabaseName();

    /**
     * Retrieve a DataSource from the service.
     *
     * @return a DataSource
     */
    DataSource getDataSource();

    /**
     * Shutdown the service - may close connection pools, etc.
     */
    void shutdown();
}
