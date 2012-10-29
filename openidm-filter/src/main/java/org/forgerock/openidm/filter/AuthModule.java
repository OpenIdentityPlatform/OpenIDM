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
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.http.security.Credential;
import org.eclipse.jetty.http.security.Password;

import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.filter.AuthFilter.AuthData;
import org.forgerock.openidm.http.ContextRegistrator;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.repo.QueryConstants;

import org.forgerock.json.fluent.JsonValue;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.eclipse.jetty.plus.jaas.spi.UserInfo;

// Deprecated
import org.forgerock.openidm.objset.JsonResourceObjectSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthModule {

    final static Logger logger = LoggerFactory.getLogger(AuthModule.class);

    // default properties set in config/system.properties
    static String queryId;
    static String queryOnResource;
    static String internalUserQueryId;
    static String queryOnInternalUserResource;
    static String userIdProperty;
    static String userCredentialProperty;
    static String userRolesProperty;
    static List<String> defaultRoles;

    // configuration conf/authentication.json

    public static void setConfig(JsonValue config) {
        defaultRoles = config.get("defaultUserRoles").asList(String.class);
        queryId = config.get("queryId").defaultTo("credential-query").asString();
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
     * @param authData The current authentication data to validate and augment, with the username supplied
     * @param password The supplied password to validate
     * @param resource
     * @return the authentication data augmented with role, id, status info. Whether authentication was successful is 
     * carried by the status property 
     */
    public static AuthData authenticate(AuthData authData, String password) {

        boolean authenticated = authPass(queryId, queryOnResource, authData.username, password, authData);
        if (!authenticated) {
            // Authenticate against the internal user table if authentication against managed users failed
            authenticated = authPass(internalUserQueryId, queryOnInternalUserResource, authData.username, password, authData);
            authData.resource = queryOnInternalUserResource;
        } else {
            authData.resource = queryOnResource;
        }
        authData.status = authenticated;
        
        return authData;
    }

    private static boolean authPass(String passQueryId, String passQueryOnResource,
            String login, String password, AuthData authData) {
        UserInfo userInfo = null;
        try {
            userInfo = getRepoUserInfo(passQueryId, passQueryOnResource, login, authData);
            if (userInfo != null && userInfo.checkCredential(password)) {
                List<String> roles = authData.roles;
                roles.clear();
                roles.addAll(userInfo.getRoleNames());
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

    private static UserInfo getRepoUserInfo (String repoQueryId, String repoResource, String username,
            AuthData authData) throws Exception {
        UserInfo user = null;
        Credential credential = null;
        List roleNames = new ArrayList();

        Map props = new HashMap();
        props.put(QueryConstants.QUERY_ID, repoQueryId);
        props.put("username", username);
        Map resultWrapper = getRepo().query(repoResource, props);
        JsonValue jsonView = new JsonValue(resultWrapper);
        if (jsonView.get(QueryConstants.QUERY_RESULT).size() > 1) {
            logger.warn("Query to match user credentials found more than one matching user for {}", username);
            for (JsonValue entry : jsonView.get(QueryConstants.QUERY_RESULT)) {
                logger.warn("Ambiguous matching username for {} found id: {}", username, entry.get("_id"));
            }
        } else if (jsonView.get(QueryConstants.QUERY_RESULT).size() > 0) {
            String retrId = null;
            String retrCred = null;
            String retrCredPropName = null;
            Object retrRoles = null;
            String retrRolesPropName = null;
            JsonValue entry = jsonView.get(QueryConstants.QUERY_RESULT).get(0);

            // If all of the required user parameters are defined
            // we can just fetch that info instead of iterating/requiring it in-order
            if (userIdProperty != null && userCredentialProperty != null) {
                logger.debug("AuthModule using explicit role query");
                retrId = entry.get(userIdProperty).asString();

                retrCredPropName = userCredentialProperty;
                retrCred = getCrypto().decryptIfNecessary(entry.get(userCredentialProperty)).asString();

                // Since userRoles are optional, check before we go to retrieve it
                if (userRolesProperty != null && entry.isDefined(userRolesProperty)) {
                    retrRolesPropName = userRolesProperty;
                    retrRoles = entry.get(userRolesProperty).getObject();
                }
            } else {
                logger.debug("AuthModule using default role query");
                int nonInternalCount = 0;
                // Repo supports returning the map entries in the order of the query,
                // even though JSON itself does not guarantee order.
                for (Map.Entry<String, Object> ordered : entry.asMap().entrySet()) {
                    String key = ordered.getKey();
                    if (key.equals("_id")) {
                        retrId = entry.get(key).asString();
                    } else if (!key.startsWith("_")) {
                        ++nonInternalCount;
                        if (nonInternalCount == 1) {
                            // By convention the first property is the cred
                            //decrypt if necessary
                            retrCred = getCrypto().decryptIfNecessary(entry.get(key)).asString();
                            retrCredPropName = key;
                        } else if (nonInternalCount == 2) {
                            // By convention the second property can define roles
                            retrRoles = entry.get(key).getObject();
                            retrRolesPropName = key;
                        }
                    }
                }
            }

            authData.userId = retrId; // The internal user id can be different than the login user name
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

    static Credential getCredential(Object retrCred, Object retrId, String username, String retrCredPropName,
            boolean allowStringifiedEncryption) {
        Credential credential = null;
        if (retrCred instanceof String) {
            if (allowStringifiedEncryption) {
                if (getCrypto().isEncrypted((String)retrCred)) {
                    JsonValue jsonRetrCred = getCrypto().decrypt((String)retrCred);
                    retrCred = jsonRetrCred == null ? null : jsonRetrCred.asString();
                }
            }
            credential = new Password((String) retrCred);
        } else if (retrCred != null) {
            if (retrCred instanceof Map) {
                JsonValue jsonRetrCred = new JsonValue(retrCred);
                if (getCrypto().isEncrypted(jsonRetrCred)) {
                    retrCred = getCrypto().decrypt(jsonRetrCred);
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

    static List addRoles(List existingRoleNames, Object retrRoles, String retrRolesPropName, List defaultRoles) {
        if (retrRoles instanceof Collection) {
            existingRoleNames.addAll((Collection) retrRoles);
        } else if (retrRoles instanceof String) {
            List parsedRoles = parseCommaDelimitedRoles((String)retrRoles);
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

    private static List parseCommaDelimitedRoles(String rawRoles) {
        List result = new ArrayList();
        if (rawRoles != null) {
            String[] split = rawRoles.split(",");
            result = Arrays.asList(split);
        }
        return result;
    }


    static JsonResourceObjectSet getRepo() {
        // TODO: switch to service trackers
        BundleContext ctx = ContextRegistrator.getBundleContext();
        ServiceReference repoRef = ctx.getServiceReference(RepositoryService.class.getName());
        return new JsonResourceObjectSet((RepositoryService)ctx.getService(repoRef));
    }

    static CryptoService getCrypto() {
        // TODO: switch to service trackers
        BundleContext ctx = ContextRegistrator.getBundleContext();
        ServiceReference cryptoRef = ctx.getServiceReference(CryptoService.class.getName());
        CryptoService crypto = (CryptoService) ctx.getService(cryptoRef);
        return crypto;
    }
}

