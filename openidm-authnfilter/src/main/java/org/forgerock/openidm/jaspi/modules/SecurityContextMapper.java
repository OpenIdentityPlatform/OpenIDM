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

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.SecurityContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecurityContextMapper {

    private String authcid;
    private final Map<String, Object> authzid;

    public SecurityContextMapper() {
        authzid = new HashMap<String, Object>();
    }

    public void setUsername(String username) {
        authcid = username;
    }

    public void setUserId(String userId) {
        authzid.put(SecurityContext.AUTHZID_ID, userId);
    }

    public void setResource(String resource) {
        authzid.put(SecurityContext.AUTHZID_COMPONENT, resource);
    }

    public void setRoles(List<String> roles) {
        authzid.put(SecurityContext.AUTHZID_ROLES, roles);
    }

    public List<String> getRoles() {
        return Collections.unmodifiableList((List<? extends String>) authzid.get(SecurityContext.AUTHZID_ROLES));
    }

    public String getAuthcid() {
        return authcid;
    }

    public Map<String, Object> getAuthzid() {
        return Collections.unmodifiableMap(authzid);
    }
}
