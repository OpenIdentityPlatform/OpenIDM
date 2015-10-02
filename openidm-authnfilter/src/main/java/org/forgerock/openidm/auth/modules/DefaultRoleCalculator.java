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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openidm.auth.modules;

import org.forgerock.json.resource.ResourceResponse;

import java.util.List;

/**
 * Default role calculator that simply sets the authorizationId to the principal name and
 * the roles to the default role set.
 */
class DefaultRoleCalculator implements RoleCalculator {
    private final List<String> defaultRoles;

    public DefaultRoleCalculator(List<String> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void calculateRoles(String principal, SecurityContextMapper securityContextMapper,
            ResourceResponse resource) {
        if (defaultRoles != null) {
            securityContextMapper.setRoles(defaultRoles);
        }
    }

}
