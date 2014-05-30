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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openidm.jaspi.auth;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;

/**
 * Authenticates a user given a username and password combination.
 *
 * @author brmiller
 */
public interface Authenticator {
    /**
     * Delegates authentication to the implemented endpoint, repository, or service.
     *
     * @param username The user's username
     * @param password The user's password.
     * @param context the ServerContext to use when making requests on the router
     * @return <code>true</code> if authentication is successful.
     * @throws ResourceException if there is a problem whilst attempting to authenticate the user.
     */
    boolean authenticate(String username, String password, ServerContext context) throws ResourceException;
}
