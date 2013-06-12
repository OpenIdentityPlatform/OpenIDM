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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openidm.jaspi.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public abstract class IDMServerAuthModule implements ServerAuthModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(IDMServerAuthModule.class);

    /** Attribute in session containing authenticated username. */
    private static final String USERNAME_ATTRIBUTE = "openidm.username";

    /** Attribute in session containing authenticated userid. */
    private static final String USERID_ATTRIBUTE = "openidm.userid";

    /** Attribute in session and request containing assigned roles. */
    private static final String ROLES_ATTRIBUTE = "openidm.roles";

    /** Attribute in session containing user's resource (managed_user or internal_user) */
    private static final String RESOURCE_ATTRIBUTE = "openidm.resource";

    /** Attribute in request to indicate to openidm down stream that an authentication filter has secured the request */
    private static final String OPENIDM_AUTHINVOKED = "openidm.authinvoked";


    @Override
    public final void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
                           Map options) throws AuthException {
        doInitialize(requestPolicy, responsePolicy, handler, options);
    }

    protected abstract void doInitialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
                           Map options) throws AuthException;

    @Override
    public final Class[] getSupportedMessageTypes() {
        return new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    }

    @Override
    public final AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {

        AuthData authData = new AuthData();

        AuthStatus authStatus = doValidateRequest(messageInfo, clientSubject, serviceSubject, authData);

        if (AuthStatus.SUCCESS.equals(authStatus)) {
            HttpServletRequest request = (HttpServletRequest)messageInfo.getRequestMessage();

            request.setAttribute(USERID_ATTRIBUTE, authData.userId);
            request.setAttribute(USERNAME_ATTRIBUTE, authData.username);
            request.setAttribute(ROLES_ATTRIBUTE, authData.roles);
            request.setAttribute(RESOURCE_ATTRIBUTE, authData.resource);
            request.setAttribute(OPENIDM_AUTHINVOKED, "openidmfilter");

            Map<String, String> messageInfoParams = messageInfo.getMap();
            messageInfoParams.put(USERNAME_ATTRIBUTE, authData.username);
            messageInfoParams.put(RESOURCE_ATTRIBUTE, authData.resource);
            messageInfoParams.put(OPENIDM_AUTHINVOKED, "authnfilter");
        }

        return authStatus;
    }

    protected abstract AuthStatus doValidateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject, AuthData authData)
            throws AuthException;

    @Override
    public final AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return doSecureResponse(messageInfo, serviceSubject);
    }

    protected abstract AuthStatus doSecureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException;

    @Override
    public final void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        doCleanSubject(messageInfo, subject);
    }

    protected abstract void doCleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException;
}
