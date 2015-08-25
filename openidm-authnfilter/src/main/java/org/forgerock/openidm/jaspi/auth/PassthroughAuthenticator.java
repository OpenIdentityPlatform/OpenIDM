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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.auth;

import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.http.Context;

/**
 * Contains logic to perform authentication by passing the request through to be authenticated against a OpenICF
 * connector, or an endpoint accepting an "authenticate" action with supplied username and password parameters.
 */
public class PassthroughAuthenticator implements Authenticator {

    private final ConnectionFactory connectionFactory;
    private final String passThroughAuth;

    /**
     * Constructs an instance of the PassthroughAuthenticator.
     *
     * @param connectionFactory the ConnectionFactory for making an authenticate request on the router
     * @param passThroughAuth the passThroughAuth resource
     */
    public PassthroughAuthenticator(ConnectionFactory connectionFactory,
            String passThroughAuth) {
        this.connectionFactory = connectionFactory;
        this.passThroughAuth = passThroughAuth;
    }

    /**
     * Performs the pass-through authentication to an external system endpoint, such as an OpenICF provisioner at
     * "system/AD/account".
     *
     * @param username The user's username
     * @param password The user's password.
     * @param context the Context to use when making requests on the router
     * @return <code>true</code> if authentication is successful.
     * @throws ResourceException if there is a problem whilst attempting to authenticate the user.
     */
    public AuthenticatorResult authenticate(String username, String password, Context context) throws ResourceException {

        final ActionResponse result = connectionFactory.getConnection().action(context,
                Requests.newActionRequest(passThroughAuth, "authenticate")
                        .setAdditionalParameter("username", username)
                        .setAdditionalParameter("password", password));

        // pass-through authentication is successful if _id exists in result; no resource is provided
        return result.getJsonContent().isDefined(ResourceResponse.FIELD_CONTENT_ID)
                ? AuthenticatorResult.SUCCESS
                : AuthenticatorResult.FAILED;

    }
}
