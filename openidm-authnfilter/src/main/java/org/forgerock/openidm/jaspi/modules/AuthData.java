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

import java.util.ArrayList;
import java.util.List;

/**
 * Holds authentication information for a login.
 */
public class AuthData {

    private String username;
    private String userId;
    private List<String> roles = new ArrayList<String>();
    private String resource = "default";

    /**
     * Gets the username of the user.
     *
     * @return The username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username of the user.
     *
     * @param username The username.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the user id of the user.
     *
     * @return The user id.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user id of the user.
     *
     * @param userId The user id.
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the roles the user is allocated.
     *
     * @return The roles.
     */
    public List<String> getRoles() {
        return roles;
    }

    /**
     * Sets the roles the user is allocated.
     *
     * @param roles The roles.
     */
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    /**
     * Gets the resource used to authenticate the user.
     *
     * @return The resource.
     */
    public String getResource() {
        return resource;
    }

    /**
     * Sets the resource used to authenticate the user.
     *
     * @param resource The resource.
     */
    public void setResource(String resource) {
        this.resource = resource;
    }
}
