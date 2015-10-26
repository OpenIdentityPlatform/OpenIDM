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

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.osgi.OsgiName;
import org.forgerock.openidm.osgi.ServiceUtil;
import org.osgi.framework.BundleContext;

/**
 * A factory for a {@link DataSource} via OSGi,
 * e.g., "osgi:service/javax.sql.DataSource/(osgi.jndi.service.name=jdbc/openidm)"
 */
class OsgiDataSourceFactory implements DataSourceFactory {

    private final OsgiDataSourceConfig config;
    private final BundleContext bundleContext;

    OsgiDataSourceFactory(OsgiDataSourceConfig config, BundleContext bundleContext) {
        this.config = config;
        this.bundleContext = bundleContext;
    }

    @Override
    public DataSource newInstance() throws InvalidException {
        try {
            OsgiName lookupName = OsgiName.parse(config.getOsgiName());
            Object service = ServiceUtil.getService(bundleContext, lookupName, null, true);
            if (service instanceof DataSource) {
                return (DataSource) service;
            }

            throw new InvalidException("DataSource can not be retrieved for: " + config.getOsgiName());
        } catch (NamingException e) {
            throw new InvalidException("Unable to obtain OSGi service for " + config.getOsgiName(), e);
        }
    }

    @Override
    public void shutdown(DataSource dataSource) {
        // nothing to do
    }
}
