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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2011-2014 ForgeRock Inc. All rights reserved.
 */

package org.forgerock.openidm.jaspi.modules;

import org.eclipse.jetty.plus.jaas.spi.UserInfo;
import org.eclipse.jetty.util.security.Password;
import org.forgerock.jaspi.exceptions.JaspiAuthException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.message.AuthException;
import java.util.HashSet;
import java.util.Set;

/**
 * Authenticator class which performs authentication against managed/internal user tables.
 */
public class ResourceQueryAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(ResourceQueryAuthenticator.class);

    private final CryptoService cryptoService;
    private final ConnectionFactory connectionFactory;
    private final String queryOnResource;
    private final String queryId;
    private final String authenticationIdProperty;
    private final String userCredentialProperty;

    /**
     * Constructs an instance of the ResourceQueryAuthenticator.
     *
     * @param cryptoService The CryptoService.
     * @param connectionFactory The ConnectionFactory.
     * @param queryOnResource The query resource.
     * @param queryId The query id.
     * @param authenticationIdProperty The user id property.
     * @param userCredentialProperty The user credential property.
     */
    public ResourceQueryAuthenticator(CryptoService cryptoService, ConnectionFactory connectionFactory,
            String queryOnResource, String queryId,  String authenticationIdProperty, String userCredentialProperty) {

        Reject.ifNull(cryptoService, "CryptoService is null");
        Reject.ifNull(connectionFactory, "ConnectionFactory is null");
        Reject.ifNull(queryOnResource, "User query resource was null");
        Reject.ifNull(queryId, "Credential query was null");
        Reject.ifNull(authenticationIdProperty, "authenticationId property is not defined");
        Reject.ifNull(userCredentialProperty, "userCredential property is not defined");

        this.cryptoService = cryptoService;
        this.connectionFactory = connectionFactory;
        this.queryOnResource = queryOnResource;
        this.queryId = queryId;
        this.authenticationIdProperty = authenticationIdProperty;
        this.userCredentialProperty = userCredentialProperty;
    }

    /**
     * Performs the authentication using the given query id, resource, username and password.
     *
     * @param username The username.
     * @param password The password.
     * @param context the ServerContext to use
     * @return True if authentication is successful, otherwise false.
     */
    public boolean authenticate(String username, String password, ServerContext context) throws AuthException {

        Reject.ifNull(username, "Provided username was null");
        Reject.ifNull(context, "Router context was null");

        try {
            UserInfo userInfo = getRepoUserInfo(username, context);
            if (userInfo == null) {
                return false; // getRepoUserInfo already logged why
            } else if (userInfo.checkCredential(password)) {
                logger.debug("Authentication succeeded for {}", username);
                return true;
            } else {
                logger.debug("Authentication failed for {} due to invalid credentials", username);
                return false;
            }
        } catch (ResourceException ex) {
            logger.debug("Authentication failed to get user info for {} {}", username, ex);
            throw new JaspiAuthException(ex.getMessage(), ex);
        }
    }

    private UserInfo getRepoUserInfo(String username, ServerContext context) throws ResourceException {
        QueryRequest request = Requests.newQueryRequest(queryOnResource)
                .setQueryId(queryId)
                .setAdditionalParameter(authenticationIdProperty, username);

        final Set<Resource> result = new HashSet<Resource>();
        connectionFactory.getConnection().query(context, request, result);

        if (result.size() == 0) {
            logger.debug("Query to match user credentials found no user matching {}", username);
            return null;
        }

        if (result.size() > 1) {
            logger.debug("Query to match user credentials found more than one matching user for {}", username);
            for (Resource entry : result) {
                logger.debug("Ambiguous matching username for {} found id: {}", username, entry.getId());
            }
            throw ResourceException.getException(401, "Access denied, user detail retrieved was ambiguous.");
        }

        final Resource resource = result.iterator().next();
        final String retrievedCred =
                cryptoService.decryptIfNecessary(resource.getContent().get(userCredentialProperty)).asString();

        return new UserInfo(username, new Password(retrievedCred), null);
    }
}

