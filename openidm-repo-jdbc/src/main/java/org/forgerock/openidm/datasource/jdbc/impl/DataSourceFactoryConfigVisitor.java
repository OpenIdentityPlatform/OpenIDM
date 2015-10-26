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

import org.osgi.framework.BundleContext;

/**
 * A DataSourceConfigVisitor to produce the appropriate {@link DataSourceFactory} from a {@link DataSourceConfig}
 * object.
 */
class DataSourceFactoryConfigVisitor implements DataSourceConfigVisitor<DataSourceFactory, Void> {
    private final BundleContext bundleContext;

    /**
     * Instantiate this visitor with a BundleContext.
     *
     * @param bundleContext the BundleContext
     */
    public DataSourceFactoryConfigVisitor(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public DataSourceFactory visit(JndiDataSourceConfig config, Void unused) {
        return new JndiDataSourceFactory(config);
    }

    @Override
    public DataSourceFactory visit(OsgiDataSourceConfig config, Void unused) {
        return new OsgiDataSourceFactory(config, bundleContext);
    }

    @Override
    public DataSourceFactory visit(NonPoolingDataSourceConfig config, Void unused) {
        return new NonPoolingDataSourceFactory(config);
    }

    @Override
    public DataSourceFactory visit(BoneCPDataSourceConfig config, Void unused) {
        return new BoneCPDataSourceFactory(config);
    }
}
