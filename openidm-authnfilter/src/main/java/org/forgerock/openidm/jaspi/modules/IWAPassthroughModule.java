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

import org.apache.commons.lang3.StringUtils;
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

/**
 * This Authentication Module uses the IWA authentication module with fall through to the Passthrough authentication
 * module.
 *
 * @author Phill Cunnington
 */
public class IWAPassthroughModule extends IWAModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(IWAPassthroughModule.class);

    private final PassthroughModule passthroughModule;

    /**
     * Constructor used by the commons Authentication Filter framework to create an instance of this authentication
     * module.
     */
    public IWAPassthroughModule() {
        super();
        passthroughModule = new PassthroughModule();
    }

    /**
     * Constructor used by tests to inject dependencies.
     *
     * @param commonsIwaModule A mock of the Commons IWAModule.
     * @param passthroughModule A mock of the ADPassthroughMdoule.
     */
    public IWAPassthroughModule(org.forgerock.jaspi.modules.iwa.IWAModule commonsIwaModule,
            PassthroughModule passthroughModule) {
        super(commonsIwaModule);
        this.passthroughModule = passthroughModule;
    }

    /**
     * Initialises the super IWA authentication module and the Passthrough authentication module.
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
        super.initialize(requestPolicy, responsePolicy, handler, options);
        passthroughModule.initialize(requestPolicy, responsePolicy, handler, options);
    }

    /**
     * Uses the IWA authentication module with fall through to the Passthrough authentication module to validate the
     * request.
     *
     * If the OpenIDM username header has been set then Passthrough is used.
     * If the OpenIDM username header is not set the IWA is used.
     * If IWA fails then Passthrough is used.
     * If Passthrough fails then the method returns AuthStatus.SEND_FAILURE.
     * If either Passthrough or IWA succeeds then the method return AuthStatus.SUCCESS.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @param authData {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException If there is a problem performing the authentication.
     */
    @Override
    protected AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject,
            AuthData authData) throws AuthException {

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();

        String xOpenIDMUsername = request.getHeader("X-OpenIDM-Username");
        String xOpenIdmPassword = request.getHeader("X-OpenIDM-Password");
        if (!StringUtils.isEmpty(xOpenIDMUsername) && !StringUtils.isEmpty(xOpenIdmPassword)) {
            // skip straight to ad passthrough
            LOGGER.debug("IWAPassthroughModule: Have OpenIDM username, falling back to AD Passthrough");
            return passthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject, authData);
        }

        AuthStatus authStatus = super.validateRequest(messageInfo, clientSubject, serviceSubject, authData);

        if (AuthStatus.SEND_FAILURE.equals(authStatus)) {
            return passthroughModule.validateRequest(messageInfo, clientSubject, serviceSubject, authData);
        } else {
            return authStatus;
        }
    }
}
