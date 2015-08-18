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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.forgerock.json.resource.ResourceResponse;

/**
 * Factory class for creating PropertyRoleCalculator instances.
 *
 * @since 3.0.0
 */
class RoleCalculatorFactory {

    /**
     * Creates a new RoleCalculator instance.
     *
     * @param defaultRoles The list of default roles.
     * @param userRoles The object attribute representing the role assignment.
     * @param groupMembership The object attribute representing the group membership.
     * @param roleMapping The mapping between OpenIDM roles and pass-through auth groups.
     * @param groupComparison The method of {@link org.forgerock.openidm.jaspi.modules.MappingRoleCalculator.GroupComparison} to use.
     * @return A RoleCalculator instance.
     */
    RoleCalculator create(final List<String> defaultRoles, final String userRoles,
            final String groupMembership, final Map<String, List<String>> roleMapping,
            final MappingRoleCalculator.GroupComparison groupComparison) {

        // aggregate all role calculation per configuration
        return new RoleCalculator() {
            final List<RoleCalculator> calculators = new ArrayList<RoleCalculator>();

            {
                if (defaultRoles != null) {
                    // assign default roles
                    calculators.add(new DefaultRoleCalculator(defaultRoles));
                }

                if (userRoles != null) {
                    // use role-lookup from object property
                    calculators.add(new PropertyRoleCalculator(userRoles));
                }

                if (groupMembership != null && roleMapping != null && !roleMapping.isEmpty()) {
                    // use group membership and role-mapping role calculator
                    calculators.add(new MappingRoleCalculator(groupMembership, roleMapping, groupComparison));
                }
            }

            @Override
            public void calculateRoles(String principal, SecurityContextMapper securityContextMapper,
                    ResourceResponse resource) {
                // set roles
                for (RoleCalculator calculator : calculators) {
                    calculator.calculateRoles(principal, securityContextMapper, resource);
                }
            }
        };
    }

}
