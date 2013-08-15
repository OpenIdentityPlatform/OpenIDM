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

import javax.security.auth.Subject;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;

/**
 * Commons Authentication Filter module to provide authentication for the anonymous user.
 *
 * @author Phill Cunnington
 */
public class InternalUserAuthModule extends IDMUserAuthModule {

    private static final String INTERNAL_USER_QUERY_ID = "credential-internaluser-query";
    private static final String QUERY_ON_INTERNAL_USER_RESOURCE = "internal/user";

    /**
     * Constructor used by the commons Authentication Filter framework to create an instance of this authentication
     * module.
     */
    public InternalUserAuthModule() {
        super(INTERNAL_USER_QUERY_ID, QUERY_ON_INTERNAL_USER_RESOURCE);
    }

    /**
     * Constructor used by tests to inject dependencies.
     *
     * @param authHelper A mock of the AuthHelper.
     */
    public InternalUserAuthModule(AuthHelper authHelper) {
        super(authHelper, INTERNAL_USER_QUERY_ID, QUERY_ON_INTERNAL_USER_RESOURCE);
    }

    /**
     * Always returns AuthStatus.SEND_SUCCESS but checks to see if the user that has been authenticated is the
     * anonymous user and if so sets skipSession to prevent the creation of a session by the Session Module,
     * (if it is configured in this case).
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) {

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        String xOpenIDMUsername = request.getHeader(IDMServerAuthModule.HEADER_USERNAME);

        if ("anonymous".equals(xOpenIDMUsername)) {
            messageInfo.getMap().put("skipSession", true);
        }

        return super.secureResponse(messageInfo, serviceSubject);
    }
}
