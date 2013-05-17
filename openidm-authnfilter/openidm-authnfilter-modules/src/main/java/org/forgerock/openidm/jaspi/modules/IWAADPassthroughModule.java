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

import org.forgerock.jaspi.modules.IWAModule;
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
import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

public class IWAADPassthroughModule implements ServerAuthModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(IWAADPassthroughModule.class);

    private CallbackHandler handler;
    private Map options;

    private IWAModule iwaModule;
    private ADPassthroughModule adPassthroughModule;

    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
                           Map options) throws AuthException {
        this.handler = handler;
        this.options = options;
        this.iwaModule = new IWAModule();
        this.adPassthroughModule = new ADPassthroughModule();
        iwaModule.initialize(requestPolicy, responsePolicy, handler, options);
        adPassthroughModule.initialize(requestPolicy, responsePolicy, handler, options);
    }

    @Override
    public Class[] getSupportedMessageTypes() {
        return new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {

        LOGGER.debug("IWAADPassthroughModule: validateRequest START");

        try {
            HttpServletRequest request = (HttpServletRequest)messageInfo.getRequestMessage();
            HttpServletResponse response = (HttpServletResponse)messageInfo.getResponseMessage();

            LOGGER.debug("IWAADPassthroughModule: Processing request {}", request.getRequestURL().toString());

            String xOpenIDMUsername = request.getHeader("X-OpenIDM-username");
            String xOpenIdmPassword = request.getHeader("X-OpenIDM-password");
            HttpSession session = request.getSession(false);
            boolean haveOpenIDMUsername = false;
            if (xOpenIDMUsername != null && xOpenIdmPassword != null) {
                LOGGER.debug("IWAADPassthroughModule: Request header contains OpenIDM username");
                haveOpenIDMUsername = xOpenIDMUsername != null;
            }
            if (!haveOpenIDMUsername && session != null) {
                LOGGER.debug("IWAADPassthroughModule: Session contains OpenIDM username");
                haveOpenIDMUsername = session.getAttribute("openidm.username") != null;
            }
            if (haveOpenIDMUsername) {
                // skip straight to ad passthrough
                LOGGER.debug("IWAADPassthroughModule: Have OpenIDM username, falling back to AD Passthrough");
                return adPassthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject);
            }

            AuthStatus authStatus;
            try {
                LOGGER.debug("IWAADPassthroughModule: Calling IWA modules");
                authStatus = iwaModule.validateRequest(messageInfo, clientSubject, serviceSubject);
            } catch (AuthException e) {
                // fallback to AD passthrough
                LOGGER.debug("IWAADPassthroughModule: IWA has failed, falling back to AD Passthrough");
                return adPassthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject);
            }

            if (!AuthStatus.SUCCESS.equals(authStatus)) {
                LOGGER.debug("IWAADPassthroughModule: IWA response to send to client, returning status, {}",
                        response.getStatus());
                return authStatus;
            }

            final String USERNAME_ATTRIBUTE = "openidm.username";
            final String RESOURCE_ATTRIBUTE = "openidm.resource";
            final String OPENIDM_AUTHINVOKED = "openidm.authinvoked";

            String username = null;

            Set<Principal> principals = clientSubject.getPrincipals(Principal.class);
            for (Principal principal : principals) {
                String principalName = principal.getName();
                if (principalName != null || !"".equals(principalName)) {
                    username = principalName;
                    break;
                }
            }
            if (username == null) {
                LOGGER.error("IWAADPassthroughModule: Username not found by IWA");
                throw new AuthException("Could not get username");
            }
            request.setAttribute(USERNAME_ATTRIBUTE, username);
            request.setAttribute(RESOURCE_ATTRIBUTE, "system/AD/account");
            request.setAttribute(OPENIDM_AUTHINVOKED, "authnfilter");

            LOGGER.debug("IWAADPassthroughModule: Successful log in with user, {}", username);;

            return AuthStatus.SUCCESS;
        } finally {
            LOGGER.debug("IWAADPassthroughModule: validateRequest END");
        }
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return AuthStatus.SEND_SUCCESS;
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
    }
}
