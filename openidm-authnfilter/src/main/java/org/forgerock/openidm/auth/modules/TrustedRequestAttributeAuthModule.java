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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.auth.modules;

import static javax.security.auth.message.AuthStatus.*;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An AsyncServerAuthModule that trusts an attribute present in the Servlet Request as the authenticationId.
 * This attribute is likely set by a servlet filter that implements its own
 */
public class TrustedRequestAttributeAuthModule implements AsyncServerAuthModule {

    private final static Logger logger = LoggerFactory.getLogger(TrustedRequestAttributeAuthModule.class);

    private static final String AUTHENTICATION_ID = "authenticationIdAttribute";

    private String authenticationIdAttribute;

    @Override
    public String getModuleId() {
        return "TrustedAttribute";
    }

    /**
     * Initialises the TrustedServletFilterAuthModule.
     *
     * @param requestPolicy {@inheritDoc}
     * @param responsePolicy {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param options {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<Void, AuthenticationException> initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy,
            CallbackHandler handler, Map<String, Object> options) {

        JsonValue properties = json(options);
        authenticationIdAttribute = properties.get(AUTHENTICATION_ID).required().asString();

        return newResultPromise(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Collection<Class<?>> getSupportedMessageTypes() {
        return Arrays.asList(new Class<?>[]{Request.class, Response.class});
    }

    /**
     * Validates the request by checking for the presence of a pre-configured attribute in the ServletRequest.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(MessageInfoContext messageInfo,
            Subject clientSubject, Subject serviceSubject) {

        SecurityContextMapper securityContextMapper = SecurityContextMapper.fromMessageInfo(messageInfo);
        final JsonValue attributes = json(messageInfo.asContext(AttributesContext.class).getAttributes());

        if (attributes.isDefined(authenticationIdAttribute)
                && attributes.get(authenticationIdAttribute).isString()) {
            final String authenticationId = attributes.get(authenticationIdAttribute).asString();
            securityContextMapper.setAuthenticationId(authenticationId);
            clientSubject.getPrincipals().add(new Principal() {
                public String getName() {
                    return authenticationId;
                }
            });
            return newResultPromise(SUCCESS);
        } else {
            return newResultPromise(SEND_FAILURE);
        }
    }

    /**
     * No work to do here so always returns AuthStatus.SEND_SUCCESS.
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageInfoContext messageInfo,
            Subject serviceSubject) {
        return newResultPromise(SEND_SUCCESS);
    }

    /**
     * Nothing to clean up.
     *
     * @param messageInfo {@inheritDoc}
     * @param subject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageInfoContext messageInfo, Subject subject) {
        return newResultPromise(null);
    }
}
