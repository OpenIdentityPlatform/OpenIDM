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
package org.forgerock.openidm.datasource.jdbc.impl;

import javax.sql.DataSource;

import org.forgerock.openidm.config.enhanced.InvalidException;

/**
 * Defines an interface to instantiate and shutdown a {@link DataSource}.
 */
interface DataSourceFactory {

    /**
     * Provide a new instance of the DataSource
     *
     * @return an instance of a DataSource
     * @throws InvalidException on failure to create a DataSource
     */
    DataSource newInstance() throws InvalidException;

    /**
     * Shutdown the DataSource, if necessary.
     *
     * @param dataSource the DataSource in use
     */
    void shutdown(DataSource dataSource);
}
