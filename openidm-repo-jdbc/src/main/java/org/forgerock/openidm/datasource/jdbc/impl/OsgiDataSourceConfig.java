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

/**
 * A DataSource using OSGi simply consists of the OSGi string.
 */
class OsgiDataSourceConfig extends AbstractDataSourceConfig {

    private String osgiName;

    /**
     * Return the OSGi string, e.g., osgi:service/javax.sql.DataSource/(osgi.jndi.service.name=jdbc/openidm)
     *
     * @return the OSGi name to lookup
     */
    public String getOsgiName() {
        return osgiName;
    }

    @Override
    public <R, P> R accept(DataSourceConfigVisitor<R, P> visitor, P parameters) {
        return visitor.visit(this, parameters);
    }
}
