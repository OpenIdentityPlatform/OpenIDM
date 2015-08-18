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

import org.forgerock.json.resource.ResourceResponse;

/**
 * Provides automatic role calculation based from the authentication configuration to provide support for common
 * auth modules out of the box.
 *
 * @since 3.0.0
 */
interface RoleCalculator {

    /**
     * Performs the calculation of roles based on the provided configuration.
     *
     * @param principal The principal.
     * @param securityContextMapper The message info instance.
     * @param resource the retrieved resource for the principal.
     */
    void calculateRoles(String principal, SecurityContextMapper securityContextMapper,
            ResourceResponse resource);
}
