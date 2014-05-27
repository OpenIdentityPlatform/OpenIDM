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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.modules;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.message.AuthException;

/**
 * Contains logic to perform authentication by passing the request through to be authenticated against a OpenICF
 * connector.
 *
 * @author Phill Cunnington
 * @author brmiller
 */
public class PassthroughAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(PassthroughAuthenticator.class);

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
     * @param context the ServerContext to use when making requests on the router
     * @return <code>true</code> if authentication is successful.
     * @throws AuthException if there is a problem whilst attempting to authenticate the user.
     */
    public boolean authenticate(String username, String password, ServerContext context) throws AuthException {

        try {
            final JsonValue result = connectionFactory.getConnection().action(context,
                    Requests.newActionRequest(passThroughAuth, "authenticate")
                            .setAdditionalParameter("username", username)
                            .setAdditionalParameter("password", password));

            // pass-through authentication is successful if _id exists in result
            return result.isDefined(Resource.FIELD_CONTENT_ID);
        } catch (ResourceException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed pass-through authentication of {} on {}.", username, passThroughAuth, e);
            }
            if (e.isServerError()) { // HTTP server-side error; AuthException sadly does not accept cause
                throw new AuthException("Failed pass-through authentication of " + username + " on "
                        + passThroughAuth + ":" + e.getMessage());
            }
            // authentication failed
            return false;
        }
    }
}
