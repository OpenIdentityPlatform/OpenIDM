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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.modules;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.message.MessageInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides automatic role calculation based from the authentication configuration to provide support for common
 * auth modules out of the box.
 *
 * @since 3.0.0
 */
class RoleCalculator {

    private static final Logger logger = LoggerFactory.getLogger(RoleCalculator.class);

    private final ConnectionFactory connectionFactory;
    private final ServerContext context;
    private final String queryOnResource;
    private final String authenticationId;
    private final String groupMembership;
    private final List<String> defaultRoles;
    private final Map<String, List<String>> roleMapping;
    private final GroupComparison groupComparison;

    /**
     * Constructs a new RoleCalculator with the provided configuration.
     *
     * @param connectionFactory The ConnectionFactory for making an authenticate request on the router.
     * @param context The ServerContext to use when making requests on the router.
     * @param queryOnResource The resource to perform the role calculation query on.
     * @param authenticationId The object attribute that represents the authentication id.
     * @param groupMembership The object attribute representing the group membership.
     * @param defaultRoles The list of default roles.
     * @param roleMapping The mapping between OpenIDM roles and pass-through auth groups.
     * @param groupComparison The method of {@link GroupComparison} to use.
     */
    RoleCalculator(ConnectionFactory connectionFactory, ServerContext context, String queryOnResource,
            String authenticationId, String groupMembership, List<String> defaultRoles,
            Map<String, List<String>> roleMapping, GroupComparison groupComparison) {
        this.connectionFactory = connectionFactory;
        this.context = context;
        this.queryOnResource = queryOnResource;
        this.authenticationId = authenticationId;
        this.groupMembership = groupMembership;
        this.defaultRoles = defaultRoles;
        this.roleMapping = roleMapping;
        this.groupComparison = groupComparison;
    }

    /**
     * Performs the calculation of roles based on the provided configuration.
     *
     * @param principal The principal
     * @param messageInfo The message info instance
     * @return A SecurityContextMapper instance containing the authentication context information.
     * @throws ResourceException If the user's details could not be found.
     */
    SecurityContextMapper calculateRoles(String principal, MessageInfo messageInfo) throws ResourceException {

        SecurityContextMapper securityContextMapper = SecurityContextMapper.fromMessageInfo(principal, messageInfo);

        // user is authenticated; populate security context
        if (queryOnResource != null) {
            securityContextMapper.setResource(queryOnResource);
        }

        // First, set roles based on default assignment if provided
        if (!defaultRoles.isEmpty()) {
            securityContextMapper.setRoles(defaultRoles);
        }

        // Then, apply role mapping if available:
        // If the propertyMapping specifies a authenticationId property and the roleMapping has been provided,
        // attempt to read the user from the pass-through source and set roles based on the roleMapping
        if (authenticationId != null && roleMapping.size() > 0) {
            final List<Resource> resources = new ArrayList<Resource>();
            connectionFactory.getConnection().query(context,
                    Requests.newQueryRequest(queryOnResource)
                            .setQueryFilter(QueryFilter.equalTo(authenticationId, principal)),
                    resources);
            if (resources.isEmpty()) {
                throw ResourceException.getException(401, "Access denied, no user detail could be retrieved.");
            } else if (resources.size() > 1) {
                throw ResourceException.getException(401, "Access denied, user detail retrieved was ambiguous.");
            }
            final JsonValue userDetail = resources.get(0).getContent();

            if (!securityContextMapper.getAuthorizationId().containsKey("id")) {
                securityContextMapper.setUserId(userDetail.get("_id").asString());
            }

            if (!userDetail.get(groupMembership).isNull()) {
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

        return securityContextMapper;
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

    /**
     * Factory class for creating RoleCalculator instances.
     *
     * @since 3.0.0
     */
    static class RoleCalculatorFactory {

        /**
         * Creates a new RoleCalculator instance.
         *
         * @param connectionFactory The ConnectionFactory for making an authenticate request on the router.
         * @param context The ServerContext to use when making requests on the router.
         * @param queryOnResource The resource to perform the role calculation query on.
         * @param authenticationId The object attribute that represents the authentication id.
         * @param groupMembership The object attribute representing the group membership.
         * @param defaultRoles The list of default roles.
         * @param roleMapping The mapping between OpenIDM roles and pass-through auth groups.
         * @param groupComparison The method of {@link GroupComparison} to use.
         * @return A RoleCalculator instance.
         */
        RoleCalculator create(ConnectionFactory connectionFactory, ServerContext context, String queryOnResource,
                String authenticationId, String groupMembership, List<String> defaultRoles,
                Map<String, List<String>> roleMapping, GroupComparison groupComparison) {
            return new RoleCalculator(connectionFactory, context, queryOnResource, authenticationId, groupMembership,
                    defaultRoles, roleMapping, groupComparison);
        }
    }
}
