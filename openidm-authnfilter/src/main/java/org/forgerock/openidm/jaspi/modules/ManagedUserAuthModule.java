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
 * Copyright 2013-2014 ForgeRock Inc.
 */

package org.forgerock.openidm.jaspi.modules;

import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.util.Accessor;

/**
 * Authentication Module for authenticating users against a managed users table.
 *
 * @author Phill Cunnington
 */
public class ManagedUserAuthModule extends IDMUserAuthModule {

    private static final String QUERY_ID = "credential-query";
    private static final String QUERY_ON_RESOURCE = "managed/user";

    /**
     * Constructor used by the commons Authentication Filter framework to create an instance of this authentication
     * module.
     */
    public ManagedUserAuthModule() {
        super(QUERY_ID, QUERY_ON_RESOURCE);
    }

    /**
     * Constructor used by tests to inject dependencies.
     *
     * @param authHelper A mock of an AuthHelper instance.
     */
    public ManagedUserAuthModule(AuthHelper authHelper, Accessor<ServerContext> accessor) {
        super(authHelper, accessor, QUERY_ID, QUERY_ON_RESOURCE);
    }
}
