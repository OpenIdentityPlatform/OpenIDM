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

package org.forgerock.openidm.auth.modules;

import static org.forgerock.caf.authentication.framework.AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.services.context.SecurityContext.AUTHZID_COMPONENT;
import static org.forgerock.services.context.SecurityContext.AUTHZID_ID;
import static org.forgerock.services.context.SecurityContext.AUTHZID_ROLES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.http.SecurityContextFactory;

/**
 * A JsonValue-wrapper to contain the security context information before the SecurityContext proper is built.
 * <br/>
 * The authenticationId and authorizationId are backed by the CAF MessageInfo; setting the authenticationId
 * or any attribute on the authorizationId results in those properties being automatically set in the MessageInfo
 * authentication context map.  This map is added to the request automatically without any further action required.
 */
class SecurityContextMapper {

    public static final String AUTHENTICATION_ID = "authenticationId";
    public static final String AUTHORIZATION_ID = "authorizationId";

    /** the MessageInfo auth context-backing map */
    private Map messageInfoMap;

    /** a JsonValue view of the auth context data */
    private final JsonValue authData;

    private SecurityContextMapper(MessageInfoContext messageInfo) {
        messageInfoMap = messageInfo.getRequestContextMap();
        Map<String, Object> contextMap = (Map<String, Object>) messageInfoMap.get(ATTRIBUTE_AUTH_CONTEXT);
        if (contextMap == null) {
            contextMap = new HashMap<>();
            messageInfoMap.put(ATTRIBUTE_AUTH_CONTEXT, contextMap);
        }
        // create the JsonValue auth-data wrapper around the AUTHCID value
        authData = json(object(field(AUTHENTICATION_ID, messageInfoMap.get(SecurityContextFactory.ATTRIBUTE_AUTHCID))));
        // and the auth context map
        authData.put(AUTHORIZATION_ID, contextMap);
    }

    /**
     * Creates a new SecurityContextMapper instance backed by the provided MessageInfo.
     *
     * @param messageInfo The MessageInfo instance.
     * @return A new SecurityContextMapper.
     */
    static SecurityContextMapper fromMessageInfo(MessageInfoContext messageInfo) {
        return new SecurityContextMapper(messageInfo);

    }

    /**
     * Set/update the authenticationId.  Persist it in the MessageInfo backing-map.
     *
     * @param authcId the authenticationId
     * @return the SecurityContextMapper
     */
    SecurityContextMapper setAuthenticationId(String authcId) {
        authData.put(AUTHENTICATION_ID, authcId);
        // when setting the authenticationId, make sure to update it in the MessageInfo backing-map as well
        messageInfoMap.put(SecurityContextFactory.ATTRIBUTE_AUTHCID, authcId);
        return this;
    }

    /**
     * Set/update the authorizationId.  Persist it in the MessageInfo backing-map.
     *
     * @param authorizationId the authorization context data
     * @return the SecurityContextMapper
     */
    SecurityContextMapper setAuthorizationId(Map<String, Object> authorizationId) {
        authData.put(AUTHORIZATION_ID, authorizationId);
        messageInfoMap.put(ATTRIBUTE_AUTH_CONTEXT, authorizationId);
        return this;
    }

    SecurityContextMapper setUserId(String userId) {
        authData.get(AUTHORIZATION_ID).put(AUTHZID_ID, userId);
        return this;
    }

    String getUserId() {
        return authData.get(AUTHORIZATION_ID).get(AUTHZID_ID).asString();
    }

    SecurityContextMapper setResource(String resource) {
        authData.get(AUTHORIZATION_ID).put(AUTHZID_COMPONENT, resource);
        return this;
    }

    String getResource() {
        return authData.get(AUTHORIZATION_ID).get(AUTHZID_COMPONENT).asString();
    }

    SecurityContextMapper setRoles(List<String> roles) {
        authData.get(AUTHORIZATION_ID).put(AUTHZID_ROLES, new ArrayList<String>(roles));
        return this;
    }

    SecurityContextMapper addRole(String role) {
        if (authData.get(AUTHORIZATION_ID).get(AUTHZID_ROLES).isNull()) {
            authData.get(AUTHORIZATION_ID).put(AUTHZID_ROLES, new ArrayList<String>());
        }
        authData.get(AUTHORIZATION_ID).get(AUTHZID_ROLES).add(role);
        return this;
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
