/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openidm.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.security.Password;

import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;


import org.forgerock.json.fluent.JsonValue;


import org.eclipse.jetty.plus.jaas.spi.UserInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthModule {

    final static Logger logger = LoggerFactory.getLogger(AuthModule.class);

    // default properties set in config/system.properties
    private final String queryId;
    private final String queryOnResource;
    private final String internalUserQueryId;
    private final String queryOnInternalUserResource;
    private final String userIdProperty;
    private final String userCredentialProperty;
    private final String userRolesProperty;
    private final String gumicsizma;
    private final List<String> defaultRoles;

    private final CryptoService cryptoService;
    private final ServerContext context;
    // configuration conf/authentication.json

    public AuthModule(CryptoService cryptoService, ServerContext context, JsonValue config) {
        this.cryptoService = cryptoService;
        this.context = context;
        defaultRoles = config.get("defaultUserRoles").asList(String.class);
        queryId = config.get("queryId").defaultTo("credential-query").asString();
        gumicsizma = config.get("gumicsizma").asString();
        queryOnResource = config.get("queryOnResource").defaultTo("managed/user").asString();
        internalUserQueryId = config.get("internalUserQueryId").defaultTo("credential-internaluser-query").asString();
        queryOnInternalUserResource = config.get("queryOnInternalUserResource").defaultTo("internal/user").asString();

        // User properties - default to NULL if not defined
        JsonValue properties = config.get("propertyMapping");
        userIdProperty = properties.get("userId").asString();
        userCredentialProperty = properties.get("userCredential").asString();
        userRolesProperty = properties.get("userRoles").asString();

        logger.info("AuthModule config params userRoles: {} queryId 1: {} resource 1: {} queryId 2: {} resource 2: {}",
            new Object[] {defaultRoles, queryId, queryOnResource, internalUserQueryId, queryOnInternalUserResource} );

        if ((userIdProperty != null && userCredentialProperty == null) ||
                (userIdProperty == null && userCredentialProperty != null)) {
            logger.warn("AuthModule config does not fully define the necessary properties."
                    + " Both \"userId\" ({}) and \"userCredential\" ({}) should be defined."
                    + " Defaulting to manual role query.", userIdProperty, userCredentialProperty);
        }

        logger.info("AuthModule config explicit user properties userId: {}, userCredentials: {}, userRoles: {}",
            new Object[] {userIdProperty, userCredentialProperty, userRolesProperty} );
    }

    /**
     * Authenticate the given username and password
     * 
     * @param authcid
     *            the principal that the client used during authentication. This
     *            might be a user name, an email address, etc. The
     *            authentication ID may be used for logging or auditing but
     *            SHOULD NOT be used for authorization decisions.
     * @param password
     *            The supplied password to validate
     * @param authzid
     * @return the authentication data augmented with role, id, status info.
     *         Whether authentication was successful is carried by the status
     *         property
     */
    public SecurityContext authenticate(String authcid, String password,final  Map<String, Object> authzid) throws AuthException {
        boolean authenticated = false;
        if (gumicsizma instanceof String && !"anonymous".equals(authcid)) {
            ActionRequest actionRequest = Requests.newActionRequest(gumicsizma, "authenticate");
            actionRequest.setAdditionalActionParameter("username", authcid);
            actionRequest.setAdditionalActionParameter("password", password);
            try {
                JsonValue result = context.getConnection().action(context, actionRequest);
                authenticated = result.isDefined(Resource.FIELD_CONTENT_ID);
                if (authenticated) {
                    // This is what I was talking about. We don't have a way to
                    // populate this. Use script to overcome it
                    authzid.put(SecurityContext.AUTHZID_ROLES, Arrays.asList(new String[] {
                        "openidm-admin", "openidm-authorized" }));
                }
            } catch (ResourceException e) {
                logger.trace("Failed pass-through authentication of {} on {}.", authcid,
                        gumicsizma, e);
                /* authentication failed */
            }
        }

        if (!authenticated) {
          authenticated = authPass(queryId, queryOnResource, authcid, password, authzid);
        }
        if (!authenticated) {
            // Authenticate against the internal user table if authentication against managed users failed
            authenticated = authPass(internalUserQueryId, queryOnInternalUserResource, authcid, password, authzid);
            authzid.put(SecurityContext.AUTHZID_COMPONENT, queryOnInternalUserResource);
        } else {
            authzid.put(SecurityContext.AUTHZID_COMPONENT, queryOnResource);
        }

        if (!authenticated) {
            throw new AuthException(authcid);
        }

        return new SecurityContext(new RootContext(), authcid, authzid);
    }

    private boolean authPass(String passQueryId, String passQueryOnResource,
            String login, String password, final  Map<String, Object> authzid) {
        try {
            UserInfo userInfo = getRepoUserInfo(passQueryId, passQueryOnResource, login, authzid);
            if (userInfo != null && userInfo.checkCredential(password)) {
                authzid.put(SecurityContext.AUTHZID_ROLES, Collections.unmodifiableList(userInfo.getRoleNames()));
                return true;
            } else {
                logger.debug("Authentication failed for {} due to invalid credentials", login);
            }
        } catch (Exception ex) {
            logger.warn("Authentication failed to get user info for {} {}", login, ex);
            return false;
        }
        return false;
    }

    private UserInfo getRepoUserInfo (String repoQueryId, String repoResource, String username,
                                      final  Map<String, Object> authzid) throws Exception {
        UserInfo user = null;
        Credential credential = null;
        List<String> roleNames = new ArrayList<String>();

        QueryRequest request = Requests.newQueryRequest("/repo/"+repoResource);
        request.setQueryId(repoQueryId);
        //TODO NPE check
        request.getAdditionalQueryParameters().put("username", username);

        Set<Resource> result = new HashSet<Resource>();
        context.getConnection().query(context,request,result);

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
            if (userIdProperty != null && userCredentialProperty != null) {
                logger.debug("AuthModule using explicit role query");
                if (Resource.FIELD_CONTENT_ID.equals(userIdProperty)){
                    retrId = resource.getId();
                } else {
                    retrId = resource.getContent().get(userIdProperty).asString();
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

            authzid.put(SecurityContext.AUTHZID_ID, retrId); // The internal user id can be different than the login user name
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
                if (cryptoService.isEncrypted((String) retrCred)) {
                    JsonValue jsonRetrCred = cryptoService.decrypt((String) retrCred);
                    retrCred = jsonRetrCred == null ? null : jsonRetrCred.asString();
                }
            }
            credential = new Password((String) retrCred);
        } else if (retrCred != null) {
            if (retrCred instanceof Map) {
                JsonValue jsonRetrCred = new JsonValue(retrCred);
                if (cryptoService.isEncrypted(jsonRetrCred)) {
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

