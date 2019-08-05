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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openidm.auth.modules;

import static javax.security.auth.message.AuthStatus.SEND_FAILURE;
import static org.forgerock.caf.authentication.framework.AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.JsonValueFunctions.enumConstant;
import static org.forgerock.json.resource.ResourceResponse.*;
import static org.forgerock.openidm.auth.modules.MappingRoleCalculator.GroupComparison;
import static org.forgerock.openidm.servletregistration.ServletRegistration.SERVLET_FILTER_AUGMENT_SECURITY_CONTEXT;

import javax.script.ScriptException;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.caf.authentication.framework.AuditTrail;
import org.forgerock.http.protocol.Request;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.openidm.util.HeaderUtil;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.services.context.ClientContext;
import org.forgerock.util.Function;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.osgi.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CAF {@link AsyncServerAuthModule} that is designed to wrap any other AsyncServerAuthModule. This module provides
 * IDM specific authentication processing to the authentication mechanism of underlying auth module.
 * <br/>
 * This allows IDM to use any common auth module and still benefit from automatic role calculation
 * and augment security context scripts (providing the authentication.json contains the required configuration).
 *
 * @since 3.0.0
 */
public class IDMAuthModuleWrapper implements AsyncServerAuthModule {

    private static final Logger logger = LoggerFactory.getLogger(IDMAuthModuleWrapper.class);

    public static final String QUERY_ID = "queryId";
    public static final String QUERY_ON_RESOURCE = "queryOnResource";
    public static final String PROPERTY_MAPPING = "propertyMapping";
    public static final String AUTHENTICATION_ID = "authenticationId";
    public static final String USER_CREDENTIAL = "userCredential";
    public static final String USER_ROLES = "userRoles";

    static final String DEFAULT_USER_ROLES = "defaultUserRoles";
    private static final String GROUP_ROLE_MAPPING = "groupRoleMapping";
    private static final String GROUP_MEMBERSHIP = "groupMembership";
    private static final String GROUP_COMPARISON_METHOD = "groupComparisonMethod";

    /** Authentication without a session header. */
    public static final String NO_SESSION = "X-OpenIDM-NoSession";

    /** Key in Messages Map for the cached resource detail */
    public static final String AUTHENTICATED_RESOURCE = "org.forgerock.openidm.authentication.resource";

    private final ConnectionFactory connectionFactory;
    private final CryptoService cryptoService;
    private final ScriptRegistry scriptRegistry;
    private final AugmentationScriptExecutor augmentationScriptExecutor;

    /** an security context augmentation script, if configured */
    private ScriptEntry augmentScript = null;

    private final RoleCalculatorFactory roleCalculatorFactory;

    private final AsyncServerAuthModule authModule;

    private JsonValue properties = json(object());
    private String logClientIPHeader = null;
    private String queryOnResource;
    private Function<QueryRequest, ResourceResponse, NeverThrowsException> queryExecutor;
    private UserDetailQueryBuilder queryBuilder;
    private RoleCalculator roleCalculator;

    /**
     * Constructs a new instance of the IDMAuthModuleWrapper.
     *
     * @param authModule The auth module wrapped by this module.
     * @param connectionFactory
     * @param cryptoService
     * @param scriptRegistry
     */
    public IDMAuthModuleWrapper(AsyncServerAuthModule authModule,
            ConnectionFactory connectionFactory, CryptoService cryptoService, ScriptRegistry scriptRegistry) {
        this(authModule, connectionFactory, cryptoService, scriptRegistry,
                new RoleCalculatorFactory(), new AugmentationScriptExecutor());
    }

    /**
     * Constructs a new instance of the IDMAuthModuleWrapper with the provided parameters, for test use.
     *
     * @param authModule The auth module wrapped by this module.
     * @param roleCalculatorFactory An instance of the RoleCalculatorFactory.
     * @param augmentationScriptExecutor An instance of the AugmentationScriptExecutor.
     */
    IDMAuthModuleWrapper(
            AsyncServerAuthModule authModule,
            ConnectionFactory connectionFactory, CryptoService cryptoService, ScriptRegistry scriptRegistry,
            RoleCalculatorFactory roleCalculatorFactory,
            AugmentationScriptExecutor augmentationScriptExecutor) {
        this.authModule = authModule;
        this.connectionFactory = connectionFactory;
        this.cryptoService = cryptoService;
        this.scriptRegistry = scriptRegistry;
        this.roleCalculatorFactory = roleCalculatorFactory;
        this.augmentationScriptExecutor = augmentationScriptExecutor;
    }

    /**
     * Calls the underlying auth module's getSupportedMessageTypes method.
     *
     * @return {@inheritDoc}
     */
    @Override
    public Collection<Class<?>> getSupportedMessageTypes() {
        return authModule.getSupportedMessageTypes();
    }

    @Override
    public String getModuleId() {
        return authModule.getModuleId();
    }

    /**
     * Initialises the underlying auth module with the provided parameters and constructs an instance
     * of the RoleCalculator from the authentication configuration.
     * <br/>
     * Required configuration:
     * <ul>
     *     <li>connectionFactory - the ConnectionFactory for making an authenticate request on the router</li>
     *     <li>context - the Context to use when making requests on the router</li>
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
     * @return {@inheritDoc}
     */
    @Override
    public Promise<Void, AuthenticationException> initialize(MessagePolicy requestMessagePolicy,MessagePolicy responseMessagePolicy,
            CallbackHandler handler, Map<String, Object> options)  {

        properties = new JsonValue(options);
        authModule.initialize(requestMessagePolicy, responseMessagePolicy, handler, options);

        logClientIPHeader = properties.get("clientIPHeader").asString();

        queryOnResource = properties.get(QUERY_ON_RESOURCE).asString();

        String queryId = properties.get(QUERY_ID).asString();
        String authenticationId = properties.get(PROPERTY_MAPPING).get(AUTHENTICATION_ID).asString();

        final String userRoles = properties.get(PROPERTY_MAPPING).get(USER_ROLES).asString();
        String groupMembership = properties.get(PROPERTY_MAPPING).get(GROUP_MEMBERSHIP).asString();
        List<String> defaultRoles = properties.get(DEFAULT_USER_ROLES)
                .defaultTo(Collections.emptyList())
                .asList(String.class);
        Map<String, List<String>> roleMapping = properties.get(GROUP_ROLE_MAPPING)
                .defaultTo(Collections.emptyMap())
                .asMapOfList(String.class);
        MappingRoleCalculator.GroupComparison groupComparison = properties.get(GROUP_COMPARISON_METHOD)
                .defaultTo(MappingRoleCalculator.GroupComparison.equals.name())
                .as(enumConstant(MappingRoleCalculator.GroupComparison.class));

        // a function to perform the user detail query on the router
        queryExecutor =
                new Function<QueryRequest, ResourceResponse, NeverThrowsException>() {
                    @Override
                    public ResourceResponse apply(QueryRequest request) {
                        final List<ResourceResponse> resources = new ArrayList<>();
                        try {
                            connectionFactory.getConnection().query(
                                    ContextUtil.createInternalContext(), request, resources);

                            if (resources.isEmpty()) {
                                return null;
                            } else if (resources.size() > 1) {
                                logger.warn("Access denied, user detail for retrieved was ambiguous.");
                                return null;
                            }
                            return resources.get(0);
                        } catch (ResourceException e) {
                            // Unable to query resource; return none
                            return null;
                        }
                    }
                };

        queryBuilder = new UserDetailQueryBuilder(queryOnResource)
                .useQueryId(queryId)
                .withAuthenticationIdProperty(authenticationId)
                // ensure we request the roles field if the property is specified
                .withField(userRoles);

        roleCalculator = roleCalculatorFactory.create(defaultRoles, userRoles, groupMembership,
                roleMapping, groupComparison);

        JsonValue scriptConfig = properties.get(SERVLET_FILTER_AUGMENT_SECURITY_CONTEXT);
        try {
	        if (!scriptConfig.isNull()) {
	            augmentScript = getAugmentScript(scriptConfig);
	            logger.debug("Registered script {}", augmentScript);
	        }
        }catch (AuthenticationException e) {
			return org.forgerock.util.promise.Promises.newExceptionPromise(e);
		}
        return org.forgerock.util.promise.Promises.newResultPromise(null);
    }

    /**
     * Gets the ScriptEntry for the specified script config.
     *
     * @param scriptConfig The script config.
     * @return The ScriptEntry.
     * @throws AuthenticationException If there is a problem retrieving the ScriptEntry.
     */
    ScriptEntry getAugmentScript(JsonValue scriptConfig) throws AuthenticationException {
        try {
            return scriptRegistry.takeScript(scriptConfig);
        } catch (ScriptException e) {
            logger.error("{} when attempting to register script {}", e.toString(), scriptConfig, e);
            throw new AuthenticationException(e.toString(), e);
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
     */
    @SuppressWarnings("unchecked")
    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(final MessageInfoContext messageInfo,
            final Subject clientSubject, Subject serviceSubject) {

        // Add this properties so the AuditLogger knows whether to log the client IP in the header.
        setClientIPAddress(messageInfo);

        return authModule.validateRequest(messageInfo, clientSubject, serviceSubject)
                .then(new Function<AuthStatus, AuthStatus, AuthenticationException>() {
                    @Override
                    public AuthStatus apply(AuthStatus authStatus) throws AuthenticationException {
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
                            throw new AuthenticationException(
                                    "Underlying Server Auth Module has not set the client subject's principal!");
                        }

                        // user is authenticated; start populating security context

                        // ... with authenticationId
                        final SecurityContextMapper securityContextMapper =
                                SecurityContextMapper.fromMessageInfo(messageInfo)
                                        .setAuthenticationId(principalName);

                        // ... with successful authenticating module name
                        securityContextMapper.setModuleId(getModuleId());

                        // ... with user details

                        try {
                            // query the resource - could return null
                            final ResourceResponse resource = getAuthenticatedResource(principalName, messageInfo);

                            // Calculate (and set) roles if not already set
                            securityContextMapper.setRoles(roleCalculator.calculateRoles(principalName, resource));

                            // set "resource" (component) if not already set
                            securityContextMapper.setResource(queryOnResource);

                            // set "user id" (authorization.id) if not already set
                            if (resource != null) {
                                // assign authorization id from resource if present
                                securityContextMapper.setUserId(
                                        resource.getId() != null
                                                ? resource.getId()
                                                : resource.getContent().get(FIELD_CONTENT_ID).asString());
                            }

                            // run the augmentation script, if configured (will no-op if none specified)
                            augmentationScriptExecutor.executeAugmentationScript(augmentScript, messageInfo, properties,
                                    securityContextMapper);

                        } catch (ResourceException e) {
                            // store failure reason
                            messageInfo.getRequestContextMap().put(
                                    AuditTrail.AUDIT_FAILURE_REASON_KEY, e.toJsonValue().asMap());

                            logger.debug("Failed role calculation for {} on {}.", principalName, queryOnResource, e);
                            if (e.isServerError()) {
                                throw new AuthenticationException("Failed authentication of "
                                        + principalName + " on " + queryOnResource + ":" + e.getMessage(), e);
                            }
                            // role calculation failed
                            return SEND_FAILURE;
                        }

                        return authStatus;
                    }
                });
    }

    /**
     * Query for the authentication resource.
     *
     * @param principalName the principal that was authenticated
     * @param messageInfo
     * @return the resource, null if configuration prohibits searching for resource
     * @throws ResourceException if unable to query for the resource
     */
    private ResourceResponse getAuthenticatedResource(String principalName, MessageInfoContext messageInfo)
            throws ResourceException {
        // see if the resource was stored in the MessageInfo by the Authenticator
        if (messageInfo.getRequestContextMap().containsKey(AUTHENTICATED_RESOURCE)) {
            JsonValue resourceDetail = new JsonValue(messageInfo.getRequestContextMap().get(AUTHENTICATED_RESOURCE));
            if (resourceDetail.isMap()) {
                return Responses.newResourceResponse(resourceDetail.get(FIELD_CONTENT_ID).asString(),
                        resourceDetail.get(FIELD_CONTENT_REVISION).asString(),
                        resourceDetail.get(FIELD_CONTENT));
            }
        }

        // build a request - will return null if any of the pieces are null
        QueryRequest request = queryBuilder.build(principalName);
        if (request == null) {
            return null;
        }

        // attempt to read the user object
        return queryExecutor.apply(request);
    }

    private void setClientIPAddress(MessageInfoContext messageInfo) {
        Request request = messageInfo.getRequest();
        String ipAddress;
        if (logClientIPHeader == null) {
            ipAddress = messageInfo.asContext(ClientContext.class).getRemoteAddress();
        } else {
            ipAddress = request.getHeaders().getFirst(logClientIPHeader);
            if (ipAddress == null) {
                ipAddress = messageInfo.asContext(ClientContext.class).getRemoteAddress();
            }
        }
        getContextMap(messageInfo).put("ipAddress", ipAddress);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getContextMap(MessageInfoContext messageInfo) {
        return (Map<String, Object>) messageInfo.getRequestContextMap().get(ATTRIBUTE_AUTH_CONTEXT);
    }

    /**
     * If the request contains the X-OpenIDM-NoSession header, sets the skipSession property on the MessageInfo,
     * and then calls the underlying auth module's secureResponse method.
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageInfoContext messageInfo,
            Subject serviceSubject) {

        final Request request = messageInfo.getRequest();
        final String noSession = request.getHeaders().getFirst(NO_SESSION);

        if (Boolean.parseBoolean(noSession)) {
            messageInfo.getRequestContextMap().put("skipSession", true);
        }

        return authModule.secureResponse(messageInfo, serviceSubject);
    }

    /**
     * Calls the underlying auth module's cleanSubject method.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageInfoContext messageInfo, Subject clientSubject) {
        return authModule.cleanSubject(messageInfo, clientSubject);
    }

    /**
     * QueryRequest Builder class for querying the user object detail.
     * <p>
     * If queryId is provided, build() will set additional parameters for the authenticationId
     * property and principal name.  Otherwise a QueryFilter where "authenticationId property = principal name"
     * is used.
     *
     * @since 3.0.0
     */
    private static class UserDetailQueryBuilder {
        private final String queryOnResource;
        private String queryId = null;
        private String authenticationId = null;
        private List<String> fields = new ArrayList<>();

        private UserDetailQueryBuilder(final String queryOnResource) {
            this.queryOnResource = queryOnResource;
            fields.add(""); // request all default fields
        }

        UserDetailQueryBuilder useQueryId(String queryId) {
            this.queryId = queryId;
            return this;
        }

        UserDetailQueryBuilder withAuthenticationIdProperty(final String authenticationId) {
            this.authenticationId = authenticationId;
            return this;
        }

        UserDetailQueryBuilder withField(String field) {
            if (field != null) {
                fields.add(field);
            }
            return this;
        }

        QueryRequest build(final String principal) throws ResourceException {
            if (queryOnResource == null || authenticationId == null || principal == null) {
                return null;
            }
            QueryRequest request = Requests.newQueryRequest(queryOnResource);
            request.addField(fields.toArray(new String[fields.size()]));
            if (queryId != null) {
                // if we're using a queryId, provide the additional parameter mapping the authenticationId property
                // and its value (the principal)
                request.setQueryId(queryId);
                request.setAdditionalParameter(authenticationId, principal);
            } else {
                // otherwise, use a query filter mapping the authenticationId property to the principal
                request.setQueryFilter(QueryFilter.equalTo(new JsonPointer(authenticationId), principal));
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
    interface CredentialHelper {
        Credential getCredential(Request request);
    }

    /** CredentialHelper to get auth header creds from request */
    static final CredentialHelper HEADER_AUTH_CRED_HELPER = new CredentialHelper() {
        /** Authentication username header. */
        private static final String HEADER_USERNAME = "X-OpenIDM-Username";

        /** Authentication password header. */
        private static final String HEADER_PASSWORD = "X-OpenIDM-Password";

        @Override
        public Credential getCredential(Request request) {
            String username = request.getHeaders().getFirst(HEADER_USERNAME);
            String password = request.getHeaders().getFirst(HEADER_PASSWORD);
            try {
                username = HeaderUtil.decodeRfc5987(username);
            } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                logger.debug("Failed authentication, malformed RFC 5987 value for {} with username: {}",
                        HEADER_USERNAME, username, e);
            }
            try {
                password = HeaderUtil.decodeRfc5987(password);
            } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                logger.debug("Failed authentication, malformed RFC 5987 value for {} with username: {}",
                        HEADER_PASSWORD, username, e);
            }
            return new Credential(username, password);
        }
    };

    /** CredentialHelper to get Basic-Auth creds from request */
    static final CredentialHelper BASIC_AUTH_CRED_HELPER  = new CredentialHelper() {
        /** Basic auth header. */
        private static final String HEADER_AUTHORIZATION = "Authorization";
        private static final String AUTHORIZATION_HEADER_BASIC = "Basic";

        @Override
        public Credential getCredential(Request request) {
            final String authHeader = request.getHeaders().getFirst(HEADER_AUTHORIZATION);
            if (authHeader != null) {
                final String[] authValue = authHeader.split("\\s", 2);
                if (AUTHORIZATION_HEADER_BASIC.equalsIgnoreCase(authValue[0]) && authValue[1] != null) {
                    final byte[] decoded = Base64.decode(authValue[1].getBytes());
                    if (decoded != null) {
                        final String[] creds = new String(decoded).split(":", 2);
                        if (creds.length == 2) {
                            return new Credential(creds[0], creds[1]);
                        }
                    }
                }
            }
            return new Credential(null, null);
        }
    };
}
