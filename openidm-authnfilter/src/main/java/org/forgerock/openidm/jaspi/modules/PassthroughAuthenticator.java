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
 * Copyright 2013-2014 ForgeRock Inc.
 */

package org.forgerock.openidm.jaspi.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.security.auth.message.AuthException;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains logic to perform authentication by passing the request through to be authenticated against a OpenICF
 * connector.
 *
 * @author Phill Cunnington
 * @author brmiller
 */
public class PassthroughAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(PassthroughAuthenticator.class);

    /** anonymous user */
    private static final String ANONYMOUS = "anonymous";

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

    private final ConnectionFactory connectionFactory;
    private final ServerContext context;
    private final String passThroughAuth;
    private final String authenticationId;
    private final String groupMembership;
    private final List<String> defaultRoles;
    private final Map<String, List<String>> roleMapping;
    private final GroupComparison groupComparison;

    /**
     * Constructs an instance of the PassthroughAuthenticator.
     *
     * @param connectionFactory the ConnectionFactory for making an authenticate request on the router
     * @param context the ServerContext to use when making requests on the router
     * @param passThroughAuth the passThroughAuth resource
     * @param authenticationId the object attribute that represents the authentication id
     * @param groupMembership the object attribute representing the group membership
     * @param defaultRoles the list of default roles
     * @param roleMapping the mapping between OpenIDM roles and pass-through auth groups
     * @param groupComparison the method of {@link GroupComparison} to use
     */
    public PassthroughAuthenticator(ConnectionFactory connectionFactory, ServerContext context,
            String passThroughAuth,  String authenticationId,  String groupMembership,
            List<String> defaultRoles, Map<String, List<String>> roleMapping, GroupComparison groupComparison) {
        this.connectionFactory = connectionFactory;
        this.context = context;
        this.passThroughAuth = passThroughAuth;
        this.authenticationId = authenticationId;
        this.groupMembership = groupMembership;
        this.defaultRoles = defaultRoles;
        this.roleMapping = roleMapping;
        this.groupComparison = groupComparison;
    }

    /**
     * Performs the pass-through authentication to an external system endpoint, such as an OpenICF provisioner at
     * "system/AD/account".
     *
     * @param username The user's username
     * @param password The user's password.
     * @param securityContextMapper The SecurityContextMapper object.
     * @return <code>true</code> if authentication is successful.
     * @throws AuthException if there is a problem whilst attempting to authenticate the user.
     */
    public boolean authenticate(String username, String password, SecurityContextMapper securityContextMapper)
            throws AuthException {

        if (StringUtils.isEmpty(passThroughAuth) || ANONYMOUS.equals(username)) {
            return false;
        }

        try {
            final JsonValue result = connectionFactory.getConnection().action(context,
                    Requests.newActionRequest(passThroughAuth, "authenticate")
                            .setAdditionalParameter("username", username)
                            .setAdditionalParameter("password", password));

            // pass-through authentication is successful if _id exists in result; bail here early if not the case
            if (!result.isDefined(Resource.FIELD_CONTENT_ID)) {
                return false;
            }

            // user is authenticated; populate security context
            securityContextMapper.setResource(passThroughAuth);
            securityContextMapper.setUserId(result.get(Resource.FIELD_CONTENT_ID).asString());
            securityContextMapper.setAuthenticationId(username);

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
                        Requests.newQueryRequest(passThroughAuth)
                                .setQueryFilter(QueryFilter.equalTo(authenticationId, securityContextMapper.getAuthenticationId())),
                        resources);
                if (resources.isEmpty()) {
                    throw ResourceException.getException(401, "Access denied, no user detail could be retrieved.");
                } else if (resources.size() > 1) {
                    throw ResourceException.getException(401, "Access denied, user detail retrieved was ambiguous.");
                }
                final JsonValue userDetail = resources.get(0).getContent();

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
            return true;
        } catch (ResourceException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed pass-through authentication of {} on {}.", username, passThroughAuth, e);
            }
            if (e.isServerError()) { // HTTP server-side error; AuthException sadly does not accept cause
                throw new AuthException("Failed pass-through authentication of " + username + " on "
                        + passThroughAuth + ":" + e.getMessage());
            }
            // authentication failed
            return false;
        }

        //TODO need to look at setting resource on authz and setting roles on authz, as well as what uses this to ensure security context is populated, like the old Servlet class used to do!
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
}
