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

package org.forgerock.openidm.jaspi.modules;

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.jaspi.modules.iwa.IWAModule;
import org.forgerock.jaspi.modules.openid.OpenIdConnectModule;
import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.jaspi.modules.session.openam.OpenAMSessionModule;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.jaspi.auth.AuthenticatorFactory;

/**
 * Enum that represents all the core IDM Authentication modules.
 */
public enum IDMAuthModule {

    /** JWT Session Auth Module. */
    JWT_SESSION {
        @Override
        public AsyncServerAuthModule newInstance(ConnectionFactory connectionFactory, CryptoService cryptoService) {
            return new JwtSessionModule();
        }
    },
    /** OpenAM Session Auth Module. */
    OPENAM_SESSION {
        @Override
        public AsyncServerAuthModule newInstance(ConnectionFactory connectionFactory, CryptoService cryptoService) {
            return new OpenAMSessionModule();
        }
    },
    /** Client-cert Auth Module. */
    CLIENT_CERT {
        @Override
        public AsyncServerAuthModule newInstance(ConnectionFactory connectionFactory, CryptoService cryptoService) {
            return new ClientCertAuthModule();
        }
    },
    /** Delegated auth module using an {@link org.forgerock.openidm.jaspi.auth.Authenticator} */
    DELEGATED {
        @Override
        public AsyncServerAuthModule newInstance(ConnectionFactory connectionFactory, CryptoService cryptoService) {
            return new DelegatedAuthModule(
                    new AuthenticatorFactory(connectionFactory, cryptoService));
        }
    },
    /** Managed User Auth Module. */
    MANAGED_USER {
        @Override
        public AsyncServerAuthModule newInstance(ConnectionFactory connectionFactory, CryptoService cryptoService) {
            return DELEGATED.newInstance(connectionFactory, cryptoService);
        }
    },
    /** Internal User Auth Module. */
    INTERNAL_USER {
        @Override
        public AsyncServerAuthModule newInstance(ConnectionFactory connectionFactory, CryptoService cryptoService) {
            return DELEGATED.newInstance(connectionFactory, cryptoService);
        }
    },
    /** Static User Auth Module. */
    STATIC_USER {
        @Override
        public AsyncServerAuthModule newInstance(ConnectionFactory connectionFactory, CryptoService cryptoService) {
            return DELEGATED.newInstance(connectionFactory, cryptoService);
        }
    },
    /** Passthrough to OpenICF connector Auth Module. */
    PASSTHROUGH {
        @Override
        public AsyncServerAuthModule newInstance(ConnectionFactory connectionFactory, CryptoService cryptoService) {
            return DELEGATED.newInstance(connectionFactory, cryptoService);
        }
    },
    /** IWA Auth Module. */
    IWA {
        @Override
        public AsyncServerAuthModule newInstance(ConnectionFactory connectionFactory, CryptoService cryptoService) {
            return new IWAModule();
        }
    },
    /** OpenID Connect Auth Module. */
    OPENID_CONNECT {
        @Override
        public AsyncServerAuthModule newInstance(ConnectionFactory connectionFactory, CryptoService cryptoService) {
            return new OpenIdConnectModule();
        }
    };

    public abstract AsyncServerAuthModule newInstance(ConnectionFactory connectionFactory, CryptoService cryptoService);
}
