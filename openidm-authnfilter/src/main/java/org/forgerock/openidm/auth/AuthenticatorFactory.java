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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openidm.auth;

import javax.inject.Provider;

import org.forgerock.guava.common.base.Function;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.util.promise.NeverThrowsException;

import static org.forgerock.openidm.auth.modules.IDMAuthModuleWrapper.AUTHENTICATION_ID;
import static org.forgerock.openidm.auth.modules.IDMAuthModuleWrapper.PROPERTY_MAPPING;
import static org.forgerock.openidm.auth.modules.IDMAuthModuleWrapper.QUERY_ID;
import static org.forgerock.openidm.auth.modules.IDMAuthModuleWrapper.QUERY_ON_RESOURCE;
import static org.forgerock.openidm.auth.modules.IDMAuthModuleWrapper.USER_CREDENTIAL;
import static org.forgerock.openidm.auth.modules.IDMAuthModuleWrapper.USER_ROLES;

/**
 * A factory Function to build an Authenticator from an auth module config.
 */
public class AuthenticatorFactory implements Function<JsonValue, Authenticator> {

    /** property for the username if using static authentication */
    private static final String USERNAME_PROPERTY = "username";
    /** property for the password if using static authentication */
    private static final String PASSWORD_PROPERTY = "password";

    private final Provider<ConnectionFactory> connectionFactoryProvider;
    private final Provider<CryptoService> cryptoServiceProvider;

    public AuthenticatorFactory(final Provider<ConnectionFactory> connectionFactoryProvider,
            final Provider<CryptoService> cryptoServiceProvider) {
        this.connectionFactoryProvider = connectionFactoryProvider;
        this.cryptoServiceProvider = cryptoServiceProvider;
    }

    /**
     * Provided the auth module properties {@link JsonValue}, instantiate an appropriate {@link Authenticator}.
     *
     * @param jsonValue the auth module properties
     * @return an Authenticator
     * @throws NeverThrowsException
     */
    @Override
    public Authenticator apply(JsonValue jsonValue) {
        if (!jsonValue.get(QUERY_ID).isNull()) {
            return new ResourceQueryAuthenticator(cryptoServiceProvider, connectionFactoryProvider,
                    jsonValue.get(QUERY_ON_RESOURCE).required().asString(),
                    jsonValue.get(QUERY_ID).required().asString(),
                    jsonValue.get(PROPERTY_MAPPING).get(AUTHENTICATION_ID).required().asString(),
                    jsonValue.get(PROPERTY_MAPPING).get(USER_CREDENTIAL).required().asString(),
                    jsonValue.get(PROPERTY_MAPPING).get(USER_ROLES).asString());
        } else if (!jsonValue.get(USERNAME_PROPERTY).isNull()
                && !jsonValue.get(PASSWORD_PROPERTY).isNull()) {
            return new StaticAuthenticator(
                    jsonValue.get(USERNAME_PROPERTY).required().asString(),
                    jsonValue.get(PASSWORD_PROPERTY).required().asString());
        } else {
            return new PassthroughAuthenticator(connectionFactoryProvider,
                    jsonValue.get(QUERY_ON_RESOURCE).required().asString());
        }
    }
}
