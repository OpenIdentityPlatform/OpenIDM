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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.forgerock.jaspi.exceptions.JaspiAuthException;
import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.servlet.SecurityContextFactory;
import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterBuilder;
import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterHelper;
import org.forgerock.script.ScriptEntry;
import org.forgerock.util.promise.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.json.resource.Resource.FIELD_CONTENT_ID;
import static org.forgerock.openidm.jaspi.modules.MappingRoleCalculator.GroupComparison;
import static org.forgerock.openidm.servletregistration.ServletRegistration.SERVLET_FILTER_AUGMENT_SECURITY_CONTEXT;

/**
 * A Jaspi ServerAuthModule that is designed to wrap any other Jaspi ServerAuthModule. This module provides
 * IDM specific authentication processing to the authentication mechanism of underlying auth module.
 * <br/>
 * This allows IDM to use any common auth module and still benefit from automatic role calculation
 * and augment security context scripts (providing the authentication.json contains the required configuration).
 *
 * @since 3.0.0
 */
public class IDMJaspiModuleWrapper implements ServerAuthModule {

    private static final Logger logger = LoggerFactory.getLogger(IDMJaspiModuleWrapper.class);

    static final String AUTHENTICATION_ID = "authenticationId";
    static final String USER_ROLES = "userRoles";
    static final String DEFAULT_USER_ROLES = "defaultUserRoles";
    static final String PROPERTY_MAPPING = "propertyMapping";
    static final String QUERY_ID = "queryId";
    static final String QUERY_ON_RESOURCE = "queryOnResource";
    private static final String GROUP_ROLE_MAPPING = "groupRoleMapping";
    private static final String GROUP_MEMBERSHIP = "groupMembership";
    private static final String GROUP_COMPARISON_METHOD = "groupComparisonMethod";

    /** Authentication without a session header. */
    public static final String NO_SESSION = "X-OpenIDM-NoSession";

    private final OSGiAuthnFilterHelper authnFilterHelper;
    private final AuthModuleConstructor authModuleConstructor;
    private final AugmentationScriptExecutor augmentationScriptExecutor;

    /** an security context augmentation script, if configured */
    private ScriptEntry augmentScript = null;

    private final RoleCalculatorFactory roleCalculatorFactory;

    private JsonValue properties = json(object());
    private ServerAuthModule authModule;
    private String logClientIPHeader = null;
    private String queryOnResource;
    private String authenticationId;
    private Function<QueryRequest, Resource, ResourceException> queryExecutor;
    private UserDetailQueryBuilder queryBuilder;
    private RoleCalculator roleCalculator;

    /**
     * Constructs a new instance of the IDMJaspiModuleWrapper.
     */
    public IDMJaspiModuleWrapper() throws AuthException {
        this.authnFilterHelper = OSGiAuthnFilterBuilder.getInstance();
        if (authnFilterHelper == null) {
            throw new AuthException("OSGiAuthnFilterHelper is not ready.");
        }
        this.authModuleConstructor = new AuthModuleConstructor();
        this.roleCalculatorFactory = new RoleCalculatorFactory();
        this.augmentationScriptExecutor = new AugmentationScriptExecutor(authnFilterHelper);
    }

    /**
     * Constructs a new instance of the IDMJaspiModuleWrapper with the provided parameters, for test use.
     *
     * @param authnFilterHelper An instance of the OSGiAuthnFilterHelper.
     * @param authModuleConstructor An instance of the AuthModuleConstructor.
     * @param roleCalculatorFactory An instance of the RoleCalculatorFactory.
     * @param augmentationScriptExecutor An instance of the AugmentationScriptExecutor.
     */
    IDMJaspiModuleWrapper(OSGiAuthnFilterHelper authnFilterHelper,
            AuthModuleConstructor authModuleConstructor,
            RoleCalculatorFactory roleCalculatorFactory,
            AugmentationScriptExecutor augmentationScriptExecutor) {
        this.authnFilterHelper = authnFilterHelper;
        this.authModuleConstructor = authModuleConstructor;
        this.roleCalculatorFactory = roleCalculatorFactory;
        this.augmentationScriptExecutor = augmentationScriptExecutor;
    }

    /**
     * Calls the underlying auth module's getSupportedMessageTypes method.
     *
     * @return {@inheritDoc}
     */
    @Override
    public Class[] getSupportedMessageTypes() {
        return authModule.getSupportedMessageTypes();
    }

    /**
     * Initialises the underlying auth module with the provided parameters and constructs an instance
     * of the RoleCalculator from the authentication configuration.
     * <br/>
     * Required configuration:
     * <ul>
     *     <li>connectionFactory - the ConnectionFactory for making an authenticate request on the router</li>
     *     <li>context - the ServerContext to use when making requests on the router</li>
     *     <li>queryOnResource - the resource to perform the role calculation query on</li>
     *     <li>authenticationId - the object attribute that represents the authentication id</li>
     *     <li>groupMembership - the object attribute representing the group membership</li>
     *     <li>defaultRoles - the list of default roles</li>
     *     <li>roleMapping - the mapping between OpenIDM roles and pass-through auth groups</li>
     *     <li>groupComparison - the method of {@link GroupComparison} to use</li>
     * </ul>
     *
     * @param requestMessagePolicy {@inheritDoc}
     * @param responseMessagePolicy {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param options {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public void initialize(MessagePolicy requestMessagePolicy, MessagePolicy responseMessagePolicy,
            CallbackHandler handler, Map options) throws AuthException {

        properties = new JsonValue(options);
        authModule = authModuleConstructor.construct(properties.get("authModuleClassName").asString());
        authModule.initialize(requestMessagePolicy, responseMessagePolicy, handler, options);

        queryOnResource = properties.get("queryOnResource").asString();
        authenticationId = properties.get(PROPERTY_MAPPING).get(AUTHENTICATION_ID).asString();
        logClientIPHeader = properties.get("clientIPHeader").asString();

        String queryId = properties.get(QUERY_ID).asString();
        String userRoles = properties.get(PROPERTY_MAPPING).get(USER_ROLES).asString();

        String groupMembership = properties.get(PROPERTY_MAPPING).get(GROUP_MEMBERSHIP).asString();
        List<String> defaultRoles = properties.get(DEFAULT_USER_ROLES)
                .defaultTo(Collections.emptyList())
                .asList(String.class);
        Map<String, List<String>> roleMapping = properties.get(GROUP_ROLE_MAPPING)
                .defaultTo(Collections.emptyMap())
                .asMapOfList(String.class);
        MappingRoleCalculator.GroupComparison groupComparison = properties.get(GROUP_COMPARISON_METHOD)
                .defaultTo(MappingRoleCalculator.GroupComparison.equals.name())
                .asEnum(MappingRoleCalculator.GroupComparison.class);

        final ConnectionFactory connectionFactory = authnFilterHelper.getConnectionFactory();
        final ServerContext context = createServerContext();

        // a function to perform the user detail query on the router
        queryExecutor =
                new Function<QueryRequest, Resource, ResourceException>() {
                    @Override
                    public Resource apply(QueryRequest request) throws ResourceException {
                        final List<Resource> resources = new ArrayList<Resource>();
                        connectionFactory.getConnection().query(context, request, resources);

                        if (resources.isEmpty()) {
                            throw ResourceException.getException(401, "Access denied, no user detail could be retrieved.");
                        } else if (resources.size() > 1) {
                            throw ResourceException.getException(401, "Access denied, user detail retrieved was ambiguous.");
                        }
                        return resources.get(0);
                    }
                };

        queryBuilder = queryOnResource != null
                ? new UserDetailQueryBuilder(queryOnResource).useQueryId(queryId)
                : null;

        roleCalculator = roleCalculatorFactory.create(defaultRoles, userRoles, groupMembership, roleMapping, groupComparison);

        JsonValue scriptConfig =  properties.get(SERVLET_FILTER_AUGMENT_SECURITY_CONTEXT);
        if (!scriptConfig.isNull()) {
            augmentScript = getAugmentScript(scriptConfig);
            logger.debug("Registered script {}", augmentScript);
        }
    }

    /**
     * Creates a new ServerContext.
     *
     * @return A ServerContext instance.
     * @throws JaspiAuthException If there is a problem creating the ServerContext.
     */
    ServerContext createServerContext() throws JaspiAuthException {
        try {
            return authnFilterHelper.getRouter().createServerContext();
        } catch (ResourceException e) {
            logger.error("Could not create ServerContext", e);
            throw new JaspiAuthException("Could not create ServerContext", e);
        }
    }

    /**
     * Gets the ScriptEntry for the specified script config.
     *
     * @param scriptConfig The script config.
     * @return The ScriptEntry.
     * @throws JaspiAuthException If there is a problem retrieving the ScriptEntry.
     */
    ScriptEntry getAugmentScript(JsonValue scriptConfig) throws JaspiAuthException {
        try {
            return authnFilterHelper.getScriptRegistry().takeScript(scriptConfig);
        } catch (ScriptException e) {
            logger.error("{} when attempting to register script {}", e.toString(), scriptConfig, e);
            throw new JaspiAuthException(e.toString(), e);
        }
    }

    /**
     * Provides IDM specific authentication process handling, by setting whether to log the client's IP address,
     * and then calls the underlying auth module's validateRequest method. If the auth module returns
     * SUCCESS, based on the authentication configuration will perform role calculation and, if present, will run the
     * augment security context script.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {

        Map<String, Object> messageInfoParams = messageInfo.getMap();

        // Add this properties so the AuditLogger knows whether to log the client IP in the header.
        messageInfoParams.put(IDMAuthenticationAuditLogger.LOG_CLIENT_IP_HEADER_KEY, logClientIPHeader);

        final AuthStatus authStatus = authModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        if (!AuthStatus.SUCCESS.equals(authStatus)) {
            return authStatus;
        }

        String principalName = null;
        for (Principal principal : clientSubject.getPrincipals()) {
            if (principal.getName() != null) {
                principalName = principal.getName();
                break;
            }
        }

        if (principalName == null) {
            // As per Jaspi spec, the module developer MUST ensure that the client
            // subject's principal is set when the module returns SUCCESS.
            throw new JaspiAuthException("Underlying Server Auth Module has not set the client subject's principal!");
        }

        // user is authenticated; populate security context

        try {
            // Attempt to read the user object if we have been given an authenticationId property
            final Resource resource = (queryBuilder != null && authenticationId != null)
                    ? queryExecutor.apply(queryBuilder.assignAuthenticationId(authenticationId, principalName).build())
                    : null;

            final SecurityContextMapper securityContextMapper =
                    roleCalculator.calculateRoles(principalName, messageInfo, resource);

            // set "resource" (component)
            if (queryOnResource != null) {
                securityContextMapper.setResource(queryOnResource);
            }

            // set "user id" (authorization.id) if not already set
            if (securityContextMapper.getUserId() == null) {
                if (resource != null) {
                    // assign authorization id from resource if present
                    securityContextMapper.setUserId(
                            resource.getId() != null
                                    ? resource.getId()
                                    : resource.getContent().get(FIELD_CONTENT_ID).asString());
                } else {
                    // set to principal otherwise
                    securityContextMapper.setUserId(principalName);
                }
            }

            if (augmentScript != null) {
                augmentationScriptExecutor.executeAugmentationScript(augmentScript, properties, securityContextMapper);

                Map<String, Object> contextMap =
                        (Map<String, Object>) messageInfo.getMap().get(JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT);
                if (contextMap == null) {
                    contextMap = new HashMap<String, Object>();
                }
                contextMap.putAll(securityContextMapper.getAuthorizationId());
            }

            messageInfoParams.put(SecurityContextFactory.ATTRIBUTE_AUTHCID, securityContextMapper.getAuthenticationId());

            if (securityContextMapper.getRoles().isEmpty()) {
                throw new ForbiddenException("no roles assigned");
            }

        } catch (ResourceException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed role calculation for {} on {}.", principalName, queryOnResource, e);
            }
            if (e.isServerError()) { // HTTP server-side error; AuthException sadly does not accept cause
                throw new JaspiAuthException("Failed pass-through authentication of " + principalName + " on "
                        + queryOnResource + ":" + e.getMessage(), e);
            }
            // role calculation failed
            return AuthStatus.SEND_FAILURE;
        }

        return authStatus;
    }

    /**
     * If the request contains the X-OpenIDM-NoSession header, sets the skipSession property on the MessageInfo,
     * and then calls the underlying auth module's secureResponse method.
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {

        final HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        final String noSession = request.getHeader(NO_SESSION);

        if (Boolean.parseBoolean(noSession)) {
            messageInfo.getMap().put("skipSession", true);
        }

        return authModule.secureResponse(messageInfo, serviceSubject);
    }

    /**
     * Calls the underlying auth module's cleanSubject method.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject clientSubject) throws AuthException {
        authModule.cleanSubject(messageInfo, clientSubject);
    }

    /**
     * Constructs Server Auth Modules from a given class name, using the mandatory no-arg constructor.
     *
     * @since 3.0.0
     */
    static class AuthModuleConstructor {

        /**
         * Creates an instance of a Server Auth Module for the specified class name.
         * <br/>
         * Uses the spec mandated no-arg constructor.
         *
         * @param authModuleClassName The ServerAuthModule class name.
         * @return The ServerAuthModule instance.
         * @throws JaspiAuthException If there is any problem creating the ServerAuthModule instance.
         */
        ServerAuthModule construct(String authModuleClassName) throws JaspiAuthException {
             try {
                 return Class.forName(authModuleClassName).asSubclass(ServerAuthModule.class).newInstance();
             } catch (ClassNotFoundException e) {
                 logger.error("Failed to construct Auth Module instance", e);
                 throw new JaspiAuthException("Failed to construct Auth Module instance", e);
             } catch (InstantiationException e) {
                 logger.error("Failed to construct Auth Module instance", e);
                 throw new JaspiAuthException("Failed to construct Auth Module instance", e);
             } catch (IllegalAccessException e) {
                 logger.error("Failed to construct Auth Module instance", e);
                 throw new JaspiAuthException("Failed to construct Auth Module instance", e);
             }
        }
    }

    /**
     * QueryRequest Builder class for querying the user object detail.
     *
     * If queryId is provided, setAuthenticationId will set additional parameters for the authenticationId
     * property and principal name.  Otherwise a QueryFilter where "authenticationId property = principal name"
     * is used.
     *
     * @since 3.0.0
     */
    private static class UserDetailQueryBuilder {
        private final String queryOnResource;
        private String queryId = null;
        private String authenticationId = null;
        private String principal = null;

        private UserDetailQueryBuilder(final String queryOnResource) {
            this.queryOnResource = queryOnResource;
        }

        UserDetailQueryBuilder useQueryId(String queryId) {
            this.queryId = queryId;
            return this;
        }

        UserDetailQueryBuilder assignAuthenticationId(final String authenticationId, final String principal)
                throws BadRequestException {
            this.authenticationId = authenticationId;
            this.principal = principal;
            return this;
        }

        QueryRequest build() throws BadRequestException {
            QueryRequest request = Requests.newQueryRequest(queryOnResource);
            if (queryId != null) {
                // if we're using a queryId, provide the additional parameter mapping the authenticationId property
                // and its value (the principal)
                request.setQueryId(queryId);
                request.setAdditionalParameter(authenticationId, principal);
            } else {
                // otherwise, use a query filter mapping the authenticationId property to the principal
                request.setQueryFilter(QueryFilter.equalTo(authenticationId, principal));
            }
            return request;
        }
    }

    /**
     * Internal credential bean to hold username/password pair.
     *
     * @since 3.0.0
     */
    static class Credential {
        final String username;
        final String password;

        Credential(final String username, final String password) {
            this.username = username;
            this.password = password;
        }

        boolean isComplete() {
            return (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password));
        }
    }

    /**
     * Interface for a helper that returns user credentials from an HttpServletRequest
     *
     * @since 3.0.0
     */
    static interface CredentialHelper {
        public Credential getCredential(HttpServletRequest request);
    }

    /** CredentialHelper to get auth header creds from request */
    static final CredentialHelper HEADER_AUTH_CRED_HELPER = new CredentialHelper() {
        /** Authentication username header. */
        private static final String HEADER_USERNAME = "X-OpenIDM-Username";

        /** Authentication password header. */
        private static final String HEADER_PASSWORD = "X-OpenIDM-Password";

        @Override
        public Credential getCredential(HttpServletRequest request) {
            return new Credential(request.getHeader(HEADER_USERNAME), request.getHeader(HEADER_PASSWORD));
        }
    };

    /** CredentialHelper to get Basic-Auth creds from request */
    static final CredentialHelper BASIC_AUTH_CRED_HELPER  = new CredentialHelper() {
        /** Basic auth header. */
        private static final String HEADER_AUTHORIZATION = "Authorization";
        private static final String AUTHORIZATION_HEADER_BASIC = "Basic";

        @Override
        public Credential getCredential(HttpServletRequest request) {
            final String authHeader = request.getHeader(HEADER_AUTHORIZATION);
            if (authHeader != null) {
                final String[] authValue = authHeader.split("\\s", 2);
                if (AUTHORIZATION_HEADER_BASIC.equalsIgnoreCase(authValue[0]) && authValue[1] != null) {
                    final String[] creds = new String(Base64.decodeBase64(authValue[1].getBytes())).split(":");
                    if (creds.length == 2) {
                        return new Credential(creds[0], creds[1]);
                    }
                }
            }
            return new Credential(null, null);
        }
    };
}
