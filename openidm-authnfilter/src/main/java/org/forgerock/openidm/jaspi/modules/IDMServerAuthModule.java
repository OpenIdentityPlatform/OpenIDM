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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.modules;

import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.servlet.SecurityContextFactory;
import org.forgerock.openidm.jaspi.config.OSGiAuthnFilterBuilder;
import static org.forgerock.openidm.servletregistration.ServletRegistration.SERVLET_FILTER_AUGMENT_SECURITY_CONTEXT;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.exception.ScriptThrownException;
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
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Map;

/**
 * Basis of all commons authentication filter modules. Ensures the required attributes/properties are set
 * after a request has been validated to ensure the attributes/properties needed by the rest of OpenIDM are present
 * so OpenIDM can operate properly.
 *
 * @author Phill Cunnington
 * @author brmiller
 */
public abstract class IDMServerAuthModule implements ServerAuthModule {

    private final static Logger logger = LoggerFactory.getLogger(IDMServerAuthModule.class);

    // common config property keys
    protected static final String AUTHENTICATION_ID = "authenticationId";
    protected static final String DEFAULT_USER_ROLES = "defaultUserRoles";
    protected static final String PROPERTY_MAPPING = "propertyMapping";

    /** Authentication username header. */
    public static final String HEADER_USERNAME = "X-OpenIDM-Username";

    /** Authentication password header. */
    public static final String HEADER_PASSWORD = "X-OpenIDM-Password";

    /** Authentication without a session header. */
    public static final String NO_SESSION = "X-OpenIDM-NoSession";

    private String logClientIPHeader = null;

    /** the auth module configuration */
    protected JsonValue properties = new JsonValue(null);

    /** an security context augmentation script, if configured */
    private ScriptEntry augmentScript = null;

    /**
     * Extracts the "clientIPHeader" value from the json configuration.
     *
     * @param requestPolicy {@inheritDoc}
     * @param responsePolicy {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param options {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public final void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
            Map options) throws AuthException {
        properties = new JsonValue(options);

        logClientIPHeader = properties.get("clientIPHeader").asString();

        initialize(requestPolicy, responsePolicy, handler);

        JsonValue scriptConfig =  properties.get(SERVLET_FILTER_AUGMENT_SECURITY_CONTEXT);
        if (!scriptConfig.isNull()) {
            try {
                augmentScript = OSGiAuthnFilterBuilder.getScriptRegistry().takeScript(scriptConfig);
                logger.debug("Registered script {}", augmentScript);
            } catch (ScriptException e) {
                logger.error("{} when attempting to register script {}", new Object[] { e.toString(), scriptConfig, e});
                throw new AuthException(e.toString()); // silly AuthException does not support chaining cause!!!
            }
        }
    }

    /**
     * Initialize this module with request and response message policies to enforce, a CallbackHandler, and any
     * module-specific configuration properties.
     *
     * @param requestPolicy The request policy this module must enforce, or null.
     * @param responsePolicy The response policy this module must enforce, or null.
     * @param handler CallbackHandler used to request information.
     * @throws AuthException If there is a problem initialising the Authentication module.
     */
    protected abstract void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler)
            throws AuthException;

    /**
     * {@inheritDoc}
     */
    @Override
    public final Class[] getSupportedMessageTypes() {
        return new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    }

    /**
     * Ensures the required attributes/properties are set after a request has been validated to ensure the
     * attributes/properties needed by the rest of OpenIDM are present so OpenIDM can operate properly.
     *
     * Attributes set on the HttpServletRequest and MessageInfo: openidm.userid, openidm.username, openidm.roles,
     * openidm.resource.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public final AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {

        Map<String, Object> messageInfoParams = messageInfo.getMap();
        Map<String, Object> contextMap =
                (Map<String, Object>) messageInfoParams.get(JaspiRuntime.ATTRIBUTE_AUTH_CONTEXT);

        // Add this properties so the AuditLogger knows whether to log the client IP in the header.
        messageInfoParams.put(IDMAuthenticationAuditLogger.LOG_CLIENT_IP_HEADER_KEY, logClientIPHeader);

        final SecurityContextMapper securityContextMapper = new SecurityContextMapper();
        AuthStatus authStatus = validateRequest(messageInfo, clientSubject, serviceSubject, securityContextMapper);
        clientSubject.getPrincipals().add(new Principal() {
            public String getName() {
                return securityContextMapper.getAuthenticationId();
            }
        });

        if (AuthStatus.SUCCESS.equals(authStatus)) {
            executeAugmentationScript(securityContextMapper);
        }

        contextMap.putAll(securityContextMapper.getAuthorizationId());
        messageInfoParams.put(SecurityContextFactory.ATTRIBUTE_AUTHCID, securityContextMapper.getAuthenticationId());

        return authStatus;
    }

    private void executeAugmentationScript(SecurityContextMapper securityContextMapper) throws AuthException {
        if (null != augmentScript) {
            try {
                if (!augmentScript.isActive()) {
                    throw new ServiceUnavailableException("Failed to execute inactive script: " + augmentScript.getName().toString());
                }

                // Create internal ServerContext chain for script-call
                ServerContext context = OSGiAuthnFilterBuilder.getRouter().createServerContext();
                final Script script = augmentScript.getScript(context);
                // Pass auth module properties and SecurityContextWrapper details to augmentation script
                script.put("properties", properties);
                script.put("security", securityContextMapper.asJsonValue());
                script.eval();
            } catch (ScriptThrownException e) {
                final ResourceException re = e.toResourceException(ResourceException.INTERNAL_ERROR, e.getMessage());
                logger.error("{} when attempting to execute script {}", new Object[] { re.toString(), augmentScript.getName(), re});
                throw new AuthException(re.getMessage()); // silly AuthException does not support chaining cause!!!
            } catch (ScriptException e) {
                logger.error("{} when attempting to execute script {}", new Object[] { e.toString(), augmentScript.getName(), e});
                throw new AuthException(e.getMessage()); // silly AuthException does not support chaining cause!!!
            } catch (ResourceException e) {
                logger.error("{} when attempting to create server context", e.toString(), e);
                throw new AuthException(e.getMessage()); // silly AuthException does not support chaining cause!!!
            }
        }
    }

    /**
     * Implementers of this method must implement the logic required to validate the incoming request.
     * @param messageInfo A contextual object that encapsulates the client request and server response objects, and
     *                    that may be used to save state across a sequence of calls made to the methods of this
     *                    interface for the purpose of completing a secure message exchange.
     * @param clientSubject A Subject that represents the source of the service request. It is used by the method
     *                      implementation to store Principals and credentials validated in the request.
     * @param serviceSubject A Subject that represents the recipient of the service request, or null. It may be used by
     *                       the method implementation as the source of Principals or credentials to be used to
     *                       validate the request. If the Subject is not null, the method implementation may add
     *                       additional Principals or credentials (pertaining to the recipient of the service request)
     *                       to the Subject.
     * @return An AuthStatus object representing the completion status of the processing performed by the method. The
     *          AuthStatus values that may be returned by this method are defined as follows:
     * <ul>
     *     <li>AuthStatus.SUCCESS when the application request message was successfully validated. The validated
     *     request message is available by calling getRequestMessage on messageInfo.</li>
     *     <li>AuthStatus.SEND_SUCCESS to indicate that validation/processing of the request message successfully
     *     produced the secured application response message (in messageInfo). The secured response message is available
     *     by calling getResponseMessage on messageInfo.</li>
     *     <li>AuthStatus.SEND_CONTINUE to indicate that message validation is incomplete, and that a preliminary
     *     response was returned as the response message in messageInfo. When this status value is returned to challenge
     *     an application request message, the challenged request must be saved by the authentication module such that
     *     it can be recovered when the module's validateRequest message is called to process the request returned for
     *     the challenge.</li>
     *     <li>AuthStatus.SEND_FAILURE to indicate that message validation failed and that an appropriate failure
     *     response message is available by calling getResponseMessage on messageInfo.</li>
     * </ul>
     * @throws AuthException When the message processing failed without establishing a failure response message
     * (in messageInfo).
     */
    protected abstract AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject,
            Subject serviceSubject, SecurityContextMapper securityContextMapper) throws AuthException;

    /**
     * Always returns AuthStatus.SEND_SUCCESS but checks to see if the request was made with the X-OpenIDM-NoSession
     * header and if so sets skipSession to prevent the creation of a session by the Session Module, (if it is
     * configured in this case).
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) {

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        String noSession = request.getHeader(IDMServerAuthModule.NO_SESSION);

        if (Boolean.parseBoolean(noSession)) {
            messageInfo.getMap().put("skipSession", true);
        }

        return AuthStatus.SEND_SUCCESS;
    }
}
