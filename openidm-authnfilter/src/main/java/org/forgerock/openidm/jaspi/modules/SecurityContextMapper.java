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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openidm.jaspi.modules;

import org.forgerock.json.fluent.JsonValue;
import static org.forgerock.json.resource.SecurityContext.AUTHZID_ID;
import static org.forgerock.json.resource.SecurityContext.AUTHZID_COMPONENT;
import static org.forgerock.json.resource.SecurityContext.AUTHZID_ROLES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A JsonValue-wrapper to contain the security context information before the
 * SecurityContext proper is built.
 */
public class SecurityContextMapper {
    private static final String AUTHENTICATION_ID = "authenticationId";
    private static final String AUTHORIZATION_ID = "authorizationId";

    private final JsonValue authData;

    public SecurityContextMapper() {
        authData = new JsonValue(new HashMap<String, Object>());
        authData.put(AUTHORIZATION_ID, new HashMap<String, Object>());
    }

    public void setAuthenticationId(String authcId) {
        authData.put(AUTHENTICATION_ID, authcId);
    }

    public void setUserId(String userId) {
        authData.get(AUTHORIZATION_ID).put(AUTHZID_ID, userId);
    }

    public String getUserId() {
        return authData.get(AUTHORIZATION_ID).get(AUTHZID_ID).asString();
    }

    public void setResource(String resource) {
        authData.get(AUTHORIZATION_ID).put(AUTHZID_COMPONENT, resource);
    }

    public String getResource() {
        return authData.get(AUTHORIZATION_ID).get(AUTHZID_COMPONENT).asString();
    }

    public void setRoles(List<String> roles) {
        authData.get(AUTHORIZATION_ID).put(AUTHZID_ROLES, roles);
    }

    public void addRole(String role) {
        if (authData.get(AUTHORIZATION_ID).get(AUTHZID_ROLES).isNull()) {
            authData.get(AUTHORIZATION_ID).put(AUTHZID_ROLES, new ArrayList<String>());
        }
        authData.get(AUTHORIZATION_ID).get(AUTHZID_ROLES).add(role);
    }

    public List<String> getRoles() {
        return Collections.unmodifiableList(authData.get(AUTHORIZATION_ID).get(AUTHZID_ROLES).asList(String.class));
    }

    public String getAuthenticationId() {
        return authData.get(AUTHENTICATION_ID).asString();
    }

    public Map<String, Object> getAuthorizationId() {
        return Collections.unmodifiableMap(authData.get(AUTHORIZATION_ID).asMap());
    }

    protected JsonValue asJsonValue() {
        return authData;
    }
}
