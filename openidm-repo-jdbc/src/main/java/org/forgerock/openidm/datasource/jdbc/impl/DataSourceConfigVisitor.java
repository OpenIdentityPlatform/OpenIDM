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
 * A visitor of {@code DataSourceConfig}s.
 * <p>
 * Classes implementing this interface can perform actions based on the type of
 * a config in a type-safe manner. When a visitor is passed to a config's
 * accept method, the corresponding visit method associated with the type of the
 * config is invoked.
 *
 * @param <R> The return type of this visitor's methods. Use
 *            {@link java.lang.Void} for visitors that do not need to return
 *            results.
 * @param <P> The type of the additional parameter to this visitor's methods.
 *            Use {@link java.lang.Void} for visitors that do not need an
 *            additional parameter.
 */
interface DataSourceConfigVisitor<R, P> {

    /**
     * Visit a {@link JndiDataSourceConfig}.
     *
     * @param config the datasource config
     * @return the result of the visitor
     */
    R visit(JndiDataSourceConfig config, P parameters);

    /**
     * Visit a {@link OsgiDataSourceConfig}.
     *
     * @param config the datasource config
     * @return the result of the visitor
     */
    R visit(OsgiDataSourceConfig config, P parameters);

    /**
     * Visit a {@link NonPoolingDataSourceConfig}.
     *
     * @param config the datasource config
     * @return the result of the visitor
     */
    R visit(NonPoolingDataSourceConfig config, P parameters);

    /**
     * Visit a {@link BoneCPDataSourceConfig}.
     *
     * @param config the datasource config
     * @return the result of the visitor
     */
    R visit(BoneCPDataSourceConfig config, P parameters);
}
