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

package org.forgerock.openidm.jaspi.modules;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provides automatic role calculation based from the authentication configuration to provide support for common
 * auth modules out of the box.
 *
 * @since 3.0.0
 */
class PropertyRoleCalculator implements RoleCalculator {

    private static final Logger logger = LoggerFactory.getLogger(MappingRoleCalculator.class);

    private final String userRoles;

    /**
     * Constructs a new MappingRoleCalculator with the provided configuration.
     *
     * @param userRoles the object attribute that represents the role definition in the retrieved object.
     */
    PropertyRoleCalculator(String userRoles) {
        this.userRoles = userRoles;
    }

    /**
     * Performs the calculation of roles based on the userRoles property in the configuration and the retrieved
     * user object.
     *
     *
     * @param principal The principal.
     * @param securityContextMapper The message info instance.
     * @param resource the retrieved resource for the principal.
     * @return A SecurityContextMapper instance containing the authentication context information.
     */
    public void calculateRoles(String principal, SecurityContextMapper securityContextMapper,
            ResourceResponse resource) {

        // Set roles from retrieved object:
        if (resource != null) {
            final JsonValue userDetail = resource.getContent();

            // support reading roles from property in object
            if (userRoles != null && !userDetail.get(userRoles).isNull()) {
                if (userDetail.get(userRoles).isString()) {
                    for (String role : userDetail.get(userRoles).asString().split(",")) {
                        securityContextMapper.addRole(role);
                    }
                } else  if (userDetail.get(userRoles).isList()) {
                    for (String role : userDetail.get(userRoles).asList(String.class)) {
                        securityContextMapper.addRole(role);
                    }
                } else {
                    logger.warn("Unknown roles type retrieved from user query, expected collection: {} type: {}",
                            userRoles, userDetail.get(userRoles).getObject().getClass());
                }
            }

            // Roles are now set.
            // Note: roles can be further augmented with a script if more complex behavior is desired

            logger.debug("Used {}object property to update context for {} with userid : {}, roles : {}",
                    userRoles != null ? (userRoles+" ") : "",
                    securityContextMapper.getAuthenticationId(),
                    securityContextMapper.getUserId(),
                    securityContextMapper.getRoles());
        }
    }

}
