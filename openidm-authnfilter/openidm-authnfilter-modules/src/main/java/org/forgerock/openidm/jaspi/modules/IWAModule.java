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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

public class IWAModule extends IDMServerAuthModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(IWAModule.class);

    private CallbackHandler handler;
    private Map options;

    private org.forgerock.jaspi.modules.iwa.IWAModule iwaModule;

    @Override
    protected void doInitialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
                           Map options) throws AuthException {
        this.handler = handler;
        this.options = options;
        this.iwaModule = new org.forgerock.jaspi.modules.iwa.IWAModule();
        iwaModule.initialize(requestPolicy, responsePolicy, handler, options);
    }

    @Override
    protected AuthStatus doValidateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject, AuthData authData)
            throws AuthException {

        LOGGER.debug("IWAADPassthroughModule: validateRequest START");

        HttpServletRequest request = (HttpServletRequest)messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse)messageInfo.getResponseMessage();

        LOGGER.debug("IWAADPassthroughModule: Processing request {}", request.getRequestURL().toString());


        AuthStatus authStatus;
        try {
            LOGGER.debug("IWAADPassthroughModule: Calling IWA modules");
            authStatus = iwaModule.validateRequest(messageInfo, clientSubject, serviceSubject);

            if (!AuthStatus.SUCCESS.equals(authStatus)) {
                LOGGER.debug("IWAADPassthroughModule: IWA response to send to client, returning status, {}",
                        response.getStatus());
                return authStatus;
            }

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
            authData.username = username;
            authData.resource = "system/AD/account";

            LOGGER.debug("IWAADPassthroughModule: Successful log in with user, {}", username);;

            return AuthStatus.SUCCESS;
        } catch (AuthException e) {
            // fallback to AD passthrough
            LOGGER.debug("IWAADPassthroughModule: IWA has failed, falling back to AD Passthrough");
            return AuthStatus.SEND_FAILURE;
        } finally {
            LOGGER.debug("IWAADPassthroughModule: validateRequest END");
        }
    }

    @Override
    protected AuthStatus doSecureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return AuthStatus.SEND_SUCCESS;
    }

    @Override
    protected void doCleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
    }
}
