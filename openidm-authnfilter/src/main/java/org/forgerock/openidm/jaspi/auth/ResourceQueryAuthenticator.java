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
 * Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.auth;

import org.eclipse.jetty.jaas.spi.UserInfo;
import org.eclipse.jetty.util.security.Password;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.http.Context;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Authenticator class which performs authentication against managed/internal user tables using a queryId to fetch
 * the complete local user data and validates the password locally.
 */
public class ResourceQueryAuthenticator implements Authenticator {

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
     * @param context the Context to use
     * @return True if authentication is successful, otherwise false.
     */
    public AuthenticatorResult authenticate(String username, String password, Context context) throws ResourceException {

        Reject.ifNull(username, "Provided username was null");
        Reject.ifNull(context, "Router context was null");

        final ResourceResponse resource = getResource(username, context);
        final UserInfo userInfo = getRepoUserInfo(username, resource);

        if (userInfo == null) {
             // getResource already logged why
            return AuthenticatorResult.FAILED;
        } else if (userInfo.checkCredential(password)) {
            logger.debug("Authentication succeeded for {}", username);
            return AuthenticatorResult.authenticationSuccess(resource);
        } else {
            logger.debug("Authentication failed for {} due to invalid credentials", username);
            return AuthenticatorResult.FAILED;
        }
    }

    private ResourceResponse getResource(String username, Context context) throws ResourceException {
        QueryRequest request = Requests.newQueryRequest(queryOnResource)
                .setQueryId(queryId)
                .setAdditionalParameter(authenticationIdProperty, username);

        final Set<ResourceResponse> result = new HashSet<>();
        connectionFactory.getConnection().query(context, request, result);

        if (result.size() == 0) {
            logger.debug("Query to match user credentials found no user matching {}", username);
            return null;
        }

        if (result.size() > 1) {
            logger.debug("Query to match user credentials found more than one matching user for {}", username);
            for (ResourceResponse entry : result) {
                logger.debug("Ambiguous matching username for {} found id: {}", username, entry.getId());
            }
            throw ResourceException.getException(401, "Access denied, user detail retrieved was ambiguous.");
        }

        return result.iterator().next(); // the retrieved resource
    }

    private UserInfo getRepoUserInfo(String username, ResourceResponse resource) throws ResourceException {
        if (username == null || resource == null) {
            return null;
        }

        final String retrievedCred =
                cryptoService.decryptIfNecessary(resource.getContent().get(userCredentialProperty)).asString();

        return new UserInfo(username, new Password(retrievedCred), null);
    }
}

