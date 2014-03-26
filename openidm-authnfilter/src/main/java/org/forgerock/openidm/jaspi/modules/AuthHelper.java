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
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.security.Password;
import org.forgerock.json.crypto.JsonCrypto;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class which performs authentication against manged internal user tables.
 */
public class AuthHelper {

    private static final Logger logger = LoggerFactory.getLogger(AuthHelper.class);

    private final CryptoService cryptoService;
    private final ConnectionFactory connectionFactory;

    private final String authenticationIdProperty;
    private final String userCredentialProperty;
    private final String userRolesProperty;
    private final List<String> defaultRoles;

    /**
     * Constructs an instance of the AuthHelper.
     *
     * @param authenticationIdProperty The user id property.
     * @param userCredentialProperty The user credential property.
     * @param userRolesProperty The user roles property.
     * @param defaultRoles The list of default roles.
     */
    public AuthHelper(CryptoService cryptoService, ConnectionFactory connectionFactory, String authenticationIdProperty,
                      String userCredentialProperty, String userRolesProperty, List<String> defaultRoles) {

        this.cryptoService = cryptoService;
        this.connectionFactory = connectionFactory;

        this.authenticationIdProperty = authenticationIdProperty;
        this.userCredentialProperty = userCredentialProperty;
        this.userRolesProperty = userRolesProperty;
        this.defaultRoles = defaultRoles;

        if ((authenticationIdProperty != null && userCredentialProperty == null)
                || (authenticationIdProperty == null && userCredentialProperty != null)) {
            logger.warn("AuthHelper config does not fully define the necessary properties."
                    + " Both \"authenticationId\" ({}) and \"userCredential\" ({}) should be defined."
                    + " Defaulting to manual role query.", authenticationIdProperty, userCredentialProperty);
        }

        logger.info("AuthHelper config explicit user properties authenticationId: {}, userCredentials: {}, userRoles: {}",
                authenticationIdProperty, userCredentialProperty, userRolesProperty);
    }

    /**
     * Performs the authentication using the given query id, resource, username and password.
     *
     * @param passQueryId The query id.
     * @param passQueryOnResource The query resource.
     * @param username The username.
     * @param password The password.
     * @param securityContextMapper The SecurityContextMapper object.
     * @param context the ServerContext to use
     * @return True if authentication is successful, otherwise false.
     */
    public boolean authenticate(String passQueryId, String passQueryOnResource, String username, String password,
            SecurityContextMapper securityContextMapper, ServerContext context) {

        try {
            UserInfo userInfo = getRepoUserInfo(passQueryId, passQueryOnResource, username, securityContextMapper, context);
            if (userInfo != null && userInfo.checkCredential(password)) {
                if (securityContextMapper != null) {
                    securityContextMapper.setRoles(userInfo.getRoleNames());
                }
                return true;
            } else {
                logger.debug("Authentication failed for {} due to invalid credentials", username);
            }
        } catch (Exception ex) {
            logger.warn("Authentication failed to get user info for {} {}", username, ex);
        }

        return false;
    }

    private UserInfo getRepoUserInfo(String repoQueryId, String repoResource, String username,
            SecurityContextMapper securityContextMapper, ServerContext context) throws Exception {

        UserInfo user = null;
        Credential credential = null;
        List<String> roleNames = new ArrayList<String>();

        QueryRequest request = Requests.newQueryRequest("/repo/" + repoResource);
        request.setQueryId(repoQueryId);
        //TODO NPE check
        request.getAdditionalParameters().put("username", username);

        Set<Resource> result = new HashSet<Resource>();
        connectionFactory.getConnection().query(context, request, result);

        if (result.size() > 1) {
            logger.warn("Query to match user credentials found more than one matching user for {}", username);
            for (Resource entry : result) {
                logger.warn("Ambiguous matching username for {} found id: {}", username, entry.getId());
            }
        } else if (result.size() > 0) {
            String retrId = null;
            String retrCred = null;
            String retrCredPropName = null;
            Object retrRoles = null;
            String retrRolesPropName = null;
            Resource resource = result.iterator().next();

            // If all of the required user parameters are defined
            // we can just fetch that info instead of iterating/requiring it in-order
            if (authenticationIdProperty != null && userCredentialProperty != null) {
                logger.debug("AuthModule using explicit role query");
                if (Resource.FIELD_CONTENT_ID.equals(authenticationIdProperty)){
                    retrId = resource.getId();
                } else {
                    retrId = resource.getContent().get(authenticationIdProperty).asString();
                }

                retrCredPropName = userCredentialProperty;
                retrCred = cryptoService.decryptIfNecessary(resource.getContent().get(userCredentialProperty)).asString();

                // Since userRoles are optional, check before we go to retrieve it
                if (userRolesProperty != null && resource.getContent().isDefined(userRolesProperty)) {
                    retrRolesPropName = userRolesProperty;
                    retrRoles = resource.getContent().get(userRolesProperty).getObject();
                }
            } else {
                logger.debug("AuthModule using default role query");
                int nonInternalCount = 0;
                // Repo supports returning the map entries in the order of the query,
                // even though JSON itself does not guarantee order.
                for (Map.Entry<String, Object> ordered : resource.getContent().asMap().entrySet()) {
                    String key = ordered.getKey();
                    if (key.equals(Resource.FIELD_CONTENT_ID)) {
                        retrId = resource.getContent().get(key).asString();
                    } else if (!key.startsWith("_")) {
                        ++nonInternalCount;
                        if (nonInternalCount == 1) {
                            // By convention the first property is the cred
                            //decrypt if necessary
                            retrCred = cryptoService.decryptIfNecessary(resource.getContent().get(key)).asString();
                            retrCredPropName = key;
                        } else if (nonInternalCount == 2) {
                            // By convention the second property can define roles
                            retrRoles = resource.getContent().get(key).getObject();
                            retrRolesPropName = key;
                        }
                    }
                }
            }

            if (securityContextMapper != null) {
                // The internal user id can be different than the login user name
                securityContextMapper.setUserId(retrId);
            }
            if (retrId == null) {
                logger.warn("Query for credentials did not contain expected result property defining the user id");
            } else if (retrCred == null && retrCredPropName == null) {
                logger.warn("Query for credentials did not contain expected result properties.");
            } else {
                credential = getCredential(retrCred, retrId, username, retrCredPropName, true);
                roleNames = addRoles(roleNames, retrRoles, retrRolesPropName, defaultRoles);
                logger.debug("User information for {}: id: {} credential available: {} " +
                        "roles from repo: {} total roles: {}",
                        new Object[] {username, retrId, (retrCred != null), retrRoles, roleNames});

                user = new UserInfo(username, credential, roleNames);
            }
        }

        return user;
    }

    Credential getCredential(Object retrCred, Object retrId, String username, String retrCredPropName,
            boolean allowStringifiedEncryption) {
        Credential credential = null;
        if (retrCred instanceof String) {
            if (allowStringifiedEncryption) {
                if (JsonUtil.isEncrypted((String) retrCred)) {
                    JsonValue jsonRetrCred = cryptoService.decrypt((String) retrCred);
                    retrCred = jsonRetrCred == null ? null : jsonRetrCred.asString();
                }
            }
            credential = new Password((String) retrCred);
        } else if (retrCred != null) {
            if (retrCred instanceof Map) {
                JsonValue jsonRetrCred = new JsonValue(retrCred);
                if (JsonCrypto.isJsonCrypto(jsonRetrCred)) {
                    retrCred = cryptoService.decrypt(jsonRetrCred);
                    credential = new Password((String) retrCred);
                } else {
                    logger.warn("Unknown credential type in id: {} for: {} credential used from: {}. "
                            + "The map does not represent an encrypted value.",
                            new Object[] {retrId, username, retrCredPropName});
                }
            } else {
                logger.warn("Unknown credential type in id: {} for: {} credential used from: {}. "
                        + "The data type is not supported: {}",
                        new Object[] {retrId, username, retrCredPropName, retrCred.getClass()});
            }
        }
        return credential;
    }

    List<String> addRoles(List<String> existingRoleNames, Object retrRoles, String retrRolesPropName, List<String> defaultRoles) {
        if (retrRoles instanceof Collection) {
            existingRoleNames.addAll((Collection) retrRoles);
        } else if (retrRoles instanceof String) {
            List<String> parsedRoles = parseCommaDelimitedRoles((String)retrRoles);
            existingRoleNames.addAll(parsedRoles);
        } else if (retrRolesPropName != null) {
            logger.warn("Unknown roles type retrieved from query in property, expected Collection: {} type: {}",
                    retrRolesPropName, retrRoles.getClass());
        } else {
            // Default roles are only applied if no explicit roles are getting queried
            existingRoleNames.addAll(defaultRoles);
        }
        return existingRoleNames;
    }

    private List<String> parseCommaDelimitedRoles(String rawRoles) {
        List<String> result = new ArrayList<String>();
        if (rawRoles instanceof String) {
            String[] split = rawRoles.split(",");
            result = Arrays.asList(split);
        }
        return result;
    }
}

