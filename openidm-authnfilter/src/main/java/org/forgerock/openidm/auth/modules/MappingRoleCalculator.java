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

import java.util.List;
import java.util.Map;

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
class MappingRoleCalculator implements RoleCalculator {

    private static final Logger logger = LoggerFactory.getLogger(MappingRoleCalculator.class);

    private final String groupMembership;
    private final Map<String, List<String>> roleMapping;
    private final GroupComparison groupComparison;

    /**
     * Constructs a new MappingRoleCalculator with the provided configuration.
     *
     * @param groupMembership The object attribute representing the group membership.
     * @param roleMapping The mapping between OpenIDM roles and pass-through auth groups.
     * @param groupComparison The method of {@link GroupComparison} to use.
     */
    MappingRoleCalculator(String groupMembership, Map<String, List<String>> roleMapping, GroupComparison groupComparison) {
        this.groupMembership = groupMembership;
        this.roleMapping = roleMapping;
        this.groupComparison = groupComparison;
    }

    /**
     * Performs the calculation of roles based on the provided configuration.
     *
     * @param principal The principal.
     * @param securityContextMapper The message info instance.
     * @param resource the retrieved resource for the principal.
     */
    public void calculateRoles(String principal, SecurityContextMapper securityContextMapper,
            ResourceResponse resource) {

        // Apply role mapping if available:
        if (resource != null) {
            final JsonValue userDetail = resource.getContent();

            // support setting roles from the provided roleMapping and groupMembership
            if (groupMembership != null
                    && !userDetail.get(groupMembership).isNull()
                    && roleMapping.size() > 0) {

                final List<String> userGroups = userDetail.get(groupMembership).asList(String.class);
                for (final Map.Entry<String, List<String>> entry : roleMapping.entrySet()) {
                    final String role = entry.getKey();
                    final List<String> groups = entry.getValue();
                    if (isMemberOfRoleGroups(groups, userGroups)) {
                        securityContextMapper.addRole(role);
                    }
                }
            }

            // Roles are now set.
            // Note: roles can be further augmented with a script if more complex behavior is desired

            logger.debug("Used pass-through details to update context for {} with userid : {}, roles : {}",
                    securityContextMapper.getAuthenticationId(),
                    securityContextMapper.getUserId(),
                    securityContextMapper.getRoles());
        }
    }

    private boolean isMemberOfRoleGroups(List<String> groups, List<String> groupMembership) {
        for (String group : groups) {
            for (String membership : groupMembership) {
                if (groupComparison.compare(group, membership)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The modes of group-comparison supported to see if a user's membership in a given
     * group matches one of the groups that have been specified to map to a particular
     * OpenIDM role.
     */
    static enum GroupComparison {
        /* case-sensitive equality */
        equals {
            public boolean compare(final String groupA, final String groupB) {
                return groupA.equals(groupB);
            }
        },

        /* case-insensitive equality */
        caseInsensitive {
            public boolean compare(final String groupA, final String groupB) {
                return groupA.equalsIgnoreCase(groupB);
            }
        },

        /* LDAP case- and whitespace-insensitive matching */
        ldap {
            public boolean compare(final String groupA, final String groupB) {
                // ldap is case (and to some degree whitespace) insensitive, so we have to be too:
                return groupA.replaceAll(LDAP_WHITESPACE, "$1").equalsIgnoreCase(groupB.replaceAll(LDAP_WHITESPACE, "$1"));
            }
        };

        private static final String LDAP_WHITESPACE = "\\s*(^|$|,|=)\\s*";

        /**
         * Compare two groups for fuzzy equality.
         *
         * @param groupA a membership group
         * @param groupB a membership group
         * @return whether the comparison considers them the same group
         */
        public abstract boolean compare(final String groupA, final String groupB);
    }

}
