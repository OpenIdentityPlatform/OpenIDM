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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.filter.AuthFilter;
import org.forgerock.openidm.filter.UserWrapper;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Authentication Filter modules for the JASPI common Authentication Filter. Validates client requests by passing though
 * to Active Directory.
 */
//@Component(name = "org.forgerock.openidm.jaspi.ADPassthroughModule", immediate = true, policy = ConfigurationPolicy.IGNORE)      //TODO is this needed?? I don't think so
//@Properties({
//        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
//        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM JASPI Authentication Filter AD Passthrough Module")
//})
public class ADPassthroughModule implements ServerAuthModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(ADPassthroughModule.class);

    private AuthFilter authFilter;

    /**
     * Required default constructor of OSGi to instantiate.
     */
    public ADPassthroughModule() {
    }

    /**
     * For tests purposes.
     *
     * @param authFilter A mock of an AuthFilter instance.
     */
    public ADPassthroughModule(AuthFilter authFilter) {
        this.authFilter = authFilter;
    }

    /**
     * Required by OSGi to create an instance of the ADPassthroughModule.
     */
    @Activate
    public void activate() {
    }

    /**
     * Required to return HttpServletRequest and HttpServletResponse.
     *
     * {@inheritDoc}
     */
    @Override
    public Class[] getSupportedMessageTypes() {
        return new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    }

    /**
     * Initialises the ADPassthroughModule with the OSGi json configuration.
     *
     * @param requestPolicy {@inheritDoc}
     * @param responsePolicy {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param options {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler, Map options) throws AuthException {
        JsonValue jsonConfig = new JsonValue(options);
        authFilter = new AuthFilter(jsonConfig);
    }

    /**
     * Validates the client's request by calling through to the existing AuthFilter.authenticate() method.
     * If the authenticate method return null, this indicates a logout and AuthStatus.SEND_SUCCESS will be returned.
     * If the authenticate method returns a valid UserWrapper object, this indicates a successful authentication and
     * AuthStatus.SUCCESS will be returned.
     * If the authenticate method throws an org.forgerock.openidm.filter.AuthException, this indicates an unsuccessful
     * authentication and AuthStatus.SEND_FAILURE will be returned.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {

        LOGGER.debug("ADPassthroughModule: validateRequest START");

        HttpServletRequest request = (HttpServletRequest)messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse)messageInfo.getResponseMessage();

        try {
            LOGGER.debug("ADPassthroughModule: Delegating call to internal AuthFilter");
            UserWrapper result = authFilter.authenticate(request, response);
            if (result == null) {
                LOGGER.debug("ADPassthroughModule: Logout successful");
                return AuthStatus.SEND_SUCCESS;
            } else {
                LOGGER.debug("ADPassthroughModule: Authentication successful");
                messageInfo.setRequestMessage(result);
                return AuthStatus.SUCCESS;
            }
        } catch (IOException e) {
            LOGGER.error("ADPassthroughModule: Authentication failed");
            return AuthStatus.SEND_FAILURE;
        } catch (ServletException e) {
            LOGGER.error("ADPassthroughModule: Authentication failed");
            return AuthStatus.SEND_FAILURE;
        } catch (org.forgerock.openidm.filter.AuthException e) {
            LOGGER.error("ADPassthroughModule: Authentication failed");
            return AuthStatus.SEND_FAILURE;
        } finally {
            LOGGER.debug("ADPassthroughModule: validateRequest END");
        }
    }

    /**
     * No work to do here so always returns AuthStatus.SEND_SUCCESS.
     *
     * @param messageInfo {@inheritDoc}
     * @param subject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject subject) throws AuthException {
        return AuthStatus.SEND_SUCCESS;
    }

    /**
     * Nothing to clean up.
     *
     * @param messageInfo {@inheritDoc}
     * @param subject {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
    }
}
