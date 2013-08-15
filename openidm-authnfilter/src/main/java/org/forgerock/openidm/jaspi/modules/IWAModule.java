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

import org.forgerock.json.fluent.JsonValue;
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
import java.util.Set;

/**
 * Commons Authentication Filter module to provide authentication using IWA.
 *
 * @author Phill Cunnington
 */
public class IWAModule extends IDMServerAuthModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(IWAModule.class);

    private final org.forgerock.jaspi.modules.iwa.IWAModule commonsIwaModule;

    /**
     * Constructor used by the commons Authentication Filter framework to create an instance of this authentication
     * module.
     */
    public IWAModule() {
        commonsIwaModule = new org.forgerock.jaspi.modules.iwa.IWAModule();
    }

    /**
     * Constructor used by tests to inject dependencies.
     *
     * @param commonsIwaModule A mock of the Commons IWAModule.
     */
    public IWAModule(org.forgerock.jaspi.modules.iwa.IWAModule commonsIwaModule) {
        this.commonsIwaModule = commonsIwaModule;
    }

    /**
     * Initialises the commons IWA authentication module.
     *
     * @param requestPolicy {@inheritDoc}
     * @param responsePolicy {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param options {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    protected void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
            JsonValue options) throws AuthException {
        commonsIwaModule.initialize(requestPolicy, responsePolicy, handler, options.asMap());
    }

    /**
     * Uses the IWA authentication to handle the authentication process.
     *
     * AuthStatus.SEND_CONTINUE is returned with the response Http code set to 401 unauthorized and a Negotiate header.
     * If IWA is enabled on the client then the subsequent request will contain the Kerberos token and
     * AuthStatus.SUCCESS will be returned for a successful authentication and AuthStatus.SEND_FAILURE if authentication
     * fails/
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @param securityContextMapper {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    protected AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject,
            SecurityContextMapper securityContextMapper) throws AuthException {

        LOGGER.debug("IWAModule: validateRequest START");

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();

        LOGGER.debug("IWAModule: Processing request {}", request.getRequestURL().toString());

        try {
            LOGGER.debug("IWAModule: Calling IWA modules");
            AuthStatus authStatus = commonsIwaModule.validateRequest(messageInfo, clientSubject, serviceSubject);

            if (!AuthStatus.SUCCESS.equals(authStatus)) {
                LOGGER.debug("IWAModule: IWA response to send to client, returning status, {}",
                        response.getStatus());
                return authStatus;
            }

            String username = null;

            Set<Principal> principals = clientSubject.getPrincipals(Principal.class);
            for (Principal principal : principals) {
                String principalName = principal.getName();
                if (principalName != null && !"".equals(principalName)) {
                    username = principalName;
                    break;
                }
            }
            if (username == null) {
                LOGGER.error("IWAModule: Username not found by IWA");
                throw new AuthException("Could not get username");
            }
            securityContextMapper.setUsername(username);
            // Need to set as much information as possible so it can be put in both the request and JWT for IDM
            // and later use
            securityContextMapper.setResource("system/AD/account");

            LOGGER.debug("IWAModule: Successful log in with user, {}", username);

            //Auth success will be logged in IDMServerAuthModule super type.
            return AuthStatus.SUCCESS;
        } catch (AuthException e) {
            // fallback to AD passthrough
            LOGGER.debug("IWAModule: IWA has failed");
            //Auth failure will be logged in IDMServerAuthModule super type.
            return AuthStatus.SEND_FAILURE;
        } finally {
            LOGGER.debug("IWAModule: validateRequest END");
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
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) {
        return AuthStatus.SEND_SUCCESS;
    }

    /**
     * Nothing to clean up.
     *
     * @param messageInfo {@inheritDoc}
     * @param subject {@inheritDoc}
     */
    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) {
    }
}
