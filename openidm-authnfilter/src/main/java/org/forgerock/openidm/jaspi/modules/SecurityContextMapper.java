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

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.json.resource.SecurityContext.AUTHZID_COMPONENT;
import static org.forgerock.json.resource.SecurityContext.AUTHZID_ID;
import static org.forgerock.json.resource.SecurityContext.AUTHZID_ROLES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.message.MessageInfo;

import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.json.fluent.JsonValue;

/**
 * A JsonValue-wrapper to contain the security context information before the
 * SecurityContext proper is built.
 * <br/>
 * The authorizationId is backed by the Jaspi MessageInfo, resulting in any attribute set on the authorizationId is
 * automatically added to the MessageInfo authentication context map which will be added to the request automatically
 * without any further action required.
 */
class SecurityContextMapper {

    private static final String AUTHENTICATION_ID = "authenticationId";
    private static final String AUTHORIZATION_ID = "authorizationId";

    private final JsonValue authData;

    private SecurityContextMapper(String authenticationId, MessageInfo messageInfo) {
        authData = json(object(field(AUTHENTICATION_ID, authenticationId)));
        authData.put(AUTHORIZATION_ID, getContextMap(messageInfo));
    }

    private Map<String, Object> getContextMap(MessageInfo messageInfo) {
        Map<String, Object> contextMap =
                (Map<String, Object>) messageInfo.getMap().get(JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT);
        if (contextMap == null) {
            contextMap = new HashMap<String, Object>();
            messageInfo.getMap().put(JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT, contextMap);
        }

        return contextMap;
    }

    /**
     * Creates a new SecurityContextMapper instance backed by the provided MessageInfo.
     *
     * @param authenticationId The authenticationId, i.e. principal.
     * @param messageInfo The MessageInfo instance.
     * @return A new SecurityContextMapper.
     */
    static SecurityContextMapper fromMessageInfo(String authenticationId, MessageInfo messageInfo)  {
        return new SecurityContextMapper(authenticationId, messageInfo);
    }

    void setAuthenticationId(String authcId) {
        authData.put(AUTHENTICATION_ID, authcId);
    }

    void setUserId(String userId) {
        authData.get(AUTHORIZATION_ID).put(AUTHZID_ID, userId);
    }

    String getUserId() {
        return authData.get(AUTHORIZATION_ID).get(AUTHZID_ID).asString();
    }

    void setResource(String resource) {
        authData.get(AUTHORIZATION_ID).put(AUTHZID_COMPONENT, resource);
    }

    String getResource() {
        return authData.get(AUTHORIZATION_ID).get(AUTHZID_COMPONENT).asString();
    }

    void setRoles(List<String> roles) {
        authData.get(AUTHORIZATION_ID).put(AUTHZID_ROLES, new ArrayList<String>(roles));
    }

    void addRole(String role) {
        if (authData.get(AUTHORIZATION_ID).get(AUTHZID_ROLES).isNull()) {
            authData.get(AUTHORIZATION_ID).put(AUTHZID_ROLES, new ArrayList<String>());
        }
        authData.get(AUTHORIZATION_ID).get(AUTHZID_ROLES).add(role);
    }

    List<String> getRoles() {
        return Collections.unmodifiableList(authData.get(AUTHORIZATION_ID).get(AUTHZID_ROLES)
                .defaultTo(new ArrayList<String>()).asList(String.class));
    }

    String getAuthenticationId() {
        return authData.get(AUTHENTICATION_ID).asString();
    }

    Map<String, Object> getAuthorizationId() {
        return Collections.unmodifiableMap(authData.get(AUTHORIZATION_ID).asMap());
    }

    JsonValue asJsonValue() {
        return authData;
    }
}
