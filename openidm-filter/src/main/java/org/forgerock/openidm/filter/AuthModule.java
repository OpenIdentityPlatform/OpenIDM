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
import org.forgerock.openidm.http.ContextRegistrator;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.repo.QueryConstants;

import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.eclipse.jetty.plus.jaas.spi.UserInfo;
import org.eclipse.jetty.http.security.Credential;
import org.eclipse.jetty.http.security.Password;

// Deprecated
import org.forgerock.openidm.objset.JsonResourceObjectSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthModule {

    final static Logger logger = LoggerFactory.getLogger(AuthModule.class);

    // default properties set in config/system.properties
    static String queryId;
    static String queryOnResource;
    static String defaultUserRoles;
    static String defaultAdminRoles;
    static String adminUserName;
    static String adminPassword;
    static List defaultRoles;
    static List adminRoles;

    static {
        adminUserName = System.getProperty("openidm.admin.username", "admin");
        adminPassword = System.getProperty("openidm.admin.password", "admin");
        defaultAdminRoles = System.getProperty("openidm.admin.role", "openidm-admin");
        defaultUserRoles = System.getProperty("openidm.user.role", "openidm-authorized");
        queryId = System.getProperty("openidm.auth.queryId", "credential-query");
        queryOnResource = System.getProperty("openidm.auth.queryOnResource", "managed/user");
        defaultRoles = setDefaultRoles(defaultUserRoles);
        adminRoles = setDefaultRoles(defaultAdminRoles);
    }
    
    private static List setDefaultRoles(String rawDefaultRoles) {
        List result = new ArrayList();
        if (rawDefaultRoles != null) {
            String[] split = rawDefaultRoles.split(",");
            result = Arrays.asList(split);
        }
        return result;
    }

    public static boolean authenticate(String login, String password, List roles) {

        // file based check from admin
        // password set in config/system.properties
        if (login.equals(adminUserName) && password.equals(adminPassword)) {
            roles.addAll(adminRoles);
            return true;
        }
        UserInfo userInfo = null;
        try {
            userInfo = getRepoUserInfo(login);
            if (userInfo != null && userInfo.checkCredential(password)) {
                roles = userInfo.getRoleNames(); 
                return true;
            } else {
                logger.debug("Authentication failed for {} due to invalid credentials", login);
            }
        } catch (Exception ex) {
            logger.warn("Authentication failed to get user info for {}", login, ex);
            return false;
        }
        return false;
    }

    private static UserInfo getRepoUserInfo (String username) throws Exception {
        UserInfo user = null;
        Credential credential = null;
        List roleNames = new ArrayList();

        Map props = new HashMap();
        props.put(QueryConstants.QUERY_ID, queryId);
        props.put("username", username);
        Map resultWrapper = getRepo().query(queryOnResource, props);
        JsonValue jsonView = new JsonValue(resultWrapper);
        if (jsonView.get(QueryConstants.QUERY_RESULT).size() > 1) {
            logger.warn("Query to match user credentials found more than one matching user for {}", username);
            for (JsonValue entry : jsonView.get(QueryConstants.QUERY_RESULT)) {
                logger.warn("Ambiguous matching username for {} found id: {}", username, entry.get("_id"));
            }
        } else if (jsonView.get(QueryConstants.QUERY_RESULT).size() > 0) {
            String retrId = null;
            Object retrCred = null;
            String retrCredPropName = null;
            Object retrRoles = null;
            String retrRolesPropName = null;
            JsonValue entry = jsonView.get(QueryConstants.QUERY_RESULT).get(0);
            int nonInternalCount = 0;
            for (String key : entry.keys()) {
                if (key.equals("_id")) {
                    retrId = entry.get(key).asString(); // It is optional to include the record identifier
                } else if (!key.startsWith("_")) {
                    ++nonInternalCount;                    if (nonInternalCount == 1) {
                        retrCred = entry.get(key).asString(); // By convention the first property is the cred
                        retrCredPropName = key;
                    } else if (nonInternalCount == 2) {
                        retrRoles = entry.get(key); // By convention the second property can define roles
                        retrRolesPropName = key;
                    }
                }
            }
            if (retrCred == null && retrCredPropName == null) {
                logger.warn("Query for credentials did not contain expected result properties.");
            } else {
                credential = getCredential(retrCred, retrId, username, retrCredPropName);
                roleNames = addRoles(roleNames, retrRoles, retrRolesPropName, defaultRoles);
                logger.debug("User information for {}: id: {} credential available: {} roles from repo: {} total roles: {}",
                        new Object[] {username, retrId, (retrCred != null), retrRoles, roleNames});

                user = new UserInfo(username, credential, roleNames);
            }
        }

        return user;
    }

    static Credential getCredential(Object retrCred, Object retrId, String username, String retrCredPropName) {
        Credential credential = null;
        if (retrCred instanceof String) {
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
        } else if (retrRolesPropName != null) {
            logger.warn("Unknown roles type retrieved from query in property, expected Collection: {} type: {}",
                    retrRolesPropName, retrRoles.getClass());
        }
        // Default roles are additive
        existingRoleNames.addAll(defaultRoles);
        return existingRoleNames;
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

