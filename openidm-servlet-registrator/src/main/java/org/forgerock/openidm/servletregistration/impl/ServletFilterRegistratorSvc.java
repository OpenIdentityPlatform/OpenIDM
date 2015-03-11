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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openidm.servletregistration.impl;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.servletregistration.ServletFilterRegistrator;

/**
 * Holds the configuration used to register a servlet filter.
 *
 */
public class ServletFilterRegistratorSvc implements ServletFilterRegistrator {

    private final JsonValue config;

    /**
     * Constructs a new ServletFilterRegistratorSvc instance.
     *
     * @param config The servlet filter configuration.
     */
    public ServletFilterRegistratorSvc(JsonValue config) {
        this.config = config;
    }

    /**
     * Gets the configuration of the servlet filter.
     *
     * @return The servlet filter configuration.
     */
    @Override
    public JsonValue getConfiguration() {
        return config;
    }
}
